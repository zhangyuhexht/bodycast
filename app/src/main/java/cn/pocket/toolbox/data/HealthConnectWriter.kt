package cn.pocket.toolbox.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Percentage
import cn.pocket.toolbox.bodycast.EntryValues
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 「把读数写入 Health Connect」桥（22.42.0）。
 *
 * 为什么存在：小米云这条路通了之后，手上**有**读数，但系统健康服务里还是空的——
 * 厂商 App 不写、华为走不了 Health Connect，于是「Health Connect 这条路」永远是死的，
 * 其他健康应用也看不到数据。本模块把**已同步到本机的聚合读数**写进 Health Connect：
 * 通道从此是活的，第三方应用也能看到同一份数据。
 *
 * 纪律：
 * - **只在用户打开开关后写**，写的是聚合读数（今天走了 X 步、静息心率 X），
 *   不编造时间窗——睡眠/运动只有拿到真实起止时间才写；
 * - 写入来源标注为手动录入（recordingMethod = MANUAL_ENTRY），Health Connect
 *   的「数据来源」里看得见、可管理；
 * - 调用方负责先拿写入权限（[writePermissions]）。
 */
object HealthConnectWriter {

    val writePermissions: Set<String> = buildSet {
        add(HealthPermission.getWritePermission(StepsRecord::class))
        add(HealthPermission.getWritePermission(RestingHeartRateRecord::class))
        add(HealthPermission.getWritePermission(OxygenSaturationRecord::class))
        add(HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class))
        add(HealthPermission.getWritePermission(SleepSessionRecord::class))
        add(HealthPermission.getWritePermission(ExerciseSessionRecord::class))
    }

    /** 睡眠/运动的真实时间窗（没有就不写这两类）。 */
    data class Windows(
        val sleepStart: Instant? = null,
        val sleepEnd: Instant? = null,
        val exerciseStart: Instant? = null,
        val exerciseEnd: Instant? = null
    )

    data class Wrote(val count: Int, val skipped: List<String>)

    private fun meta(): Metadata = Metadata(recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY)

    private fun zone(): ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())

    suspend fun write(
        context: Context,
        values: EntryValues,
        windows: Windows = Windows()
    ): Wrote = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val client = runCatching { HealthConnectClient.getOrCreate(context) }
            .getOrNull() ?: return@withContext Wrote(0, listOf("no-client"))
        val now = Instant.now()
        val zone = zone()
        val todayStart = now.atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault()).toInstant()
        val records = mutableListOf<Record>()
        val skipped = mutableListOf<String>()

        values.steps?.let {
            records += StepsRecord(
                startTime = todayStart,
                startZoneOffset = zone,
                endTime = now,
                endZoneOffset = zone,
                count = it.toLong(),
                metadata = meta()
            )
        }
        values.restingHeartRate?.let {
            records += RestingHeartRateRecord(
                time = now,
                zoneOffset = zone,
                beatsPerMinute = it.toLong(),
                metadata = meta()
            )
        }
        values.bloodOxygen?.let {
            records += OxygenSaturationRecord(
                time = now,
                zoneOffset = zone,
                percentage = Percentage(it.toDouble()),
                metadata = meta()
            )
        }
        values.hrv?.let {
            records += HeartRateVariabilityRmssdRecord(
                time = now,
                zoneOffset = zone,
                heartRateVariabilityMillis = it.toDouble(),
                metadata = meta()
            )
        }
        val sleepStart = windows.sleepStart
        val sleepEnd = windows.sleepEnd
        if (sleepStart != null && sleepEnd != null && sleepEnd > sleepStart) {
            records += SleepSessionRecord(
                startTime = sleepStart,
                startZoneOffset = zone,
                endTime = sleepEnd,
                endZoneOffset = zone,
                title = "Sleep",
                metadata = meta()
            )
        } else if (values.sleepMinutes != null) {
            skipped += "sleep-no-window"
        }
        val exStart = windows.exerciseStart
        val exEnd = windows.exerciseEnd
        if (exStart != null && exEnd != null && exEnd > exStart) {
            records += ExerciseSessionRecord(
                startTime = exStart,
                startZoneOffset = zone,
                endTime = exEnd,
                endZoneOffset = zone,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                title = "Workout",
                metadata = meta()
            )
        } else if (values.activityMinutes != null) {
            skipped += "exercise-no-window"
        }

        if (records.isEmpty()) return@withContext Wrote(0, skipped)
        runCatching {
            client.insertRecords(records)
        }.onFailure {
            return@withContext Wrote(0, skipped + "write-failed")
        }
        Wrote(records.size, skipped)
    }

    /**
     * 从本机偏好里取**合并读数**写进 Health Connect（22.42.0，写入桥的主入口）。
     *
     * - 优先级与界面一致：手动填写 > 小米云 > 文件导入，逐字段补；
     * - 睡眠 / 运动只有真实起止窗口才写（[BodyCastPrefs.miCloudSleepStartAt] 等）；
     * - [force] = false（自动写）时，内容签名与上次相同就跳过，不刷重复记录；
     * - 成功后把结果记回偏好（状态行显示「上次写入」）。
     */
    suspend fun writeFromStore(
        context: Context,
        store: cn.pocket.toolbox.bodycast.BodyCastStore,
        force: Boolean = false
    ): Wrote {
        val prefs = store.prefs
        val manual = prefs.manualEntries.takeIf { prefs.manualEntryFresh && !it.isEmpty }
        val cloud = prefs.miCloudEntries.takeIf { prefs.miCloudFresh && !it.isEmpty }
        val imported = prefs.importedEntries.takeIf { prefs.importedFresh && !it.isEmpty }
        val values = EntryValues(
            restingHeartRate = manual?.restingHeartRate ?: cloud?.restingHeartRate ?: imported?.restingHeartRate,
            hrv = manual?.hrv ?: cloud?.hrv ?: imported?.hrv,
            sleepMinutes = manual?.sleepMinutes ?: cloud?.sleepMinutes ?: imported?.sleepMinutes,
            steps = manual?.steps ?: cloud?.steps ?: imported?.steps,
            bloodOxygen = manual?.bloodOxygen ?: cloud?.bloodOxygen ?: imported?.bloodOxygen,
            activityMinutes = manual?.activityMinutes ?: cloud?.activityMinutes ?: imported?.activityMinutes
        )
        // 睡眠 / 运动窗口只跟小米云这条路走（手动填写 / 导入没有明细时间）。
        val windows = if (prefs.miCloudFresh) {
            Windows(
                sleepStart = prefs.miCloudSleepStartAt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) },
                sleepEnd = prefs.miCloudSleepEndAt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) },
                exerciseStart = prefs.miCloudExerciseStartAt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) },
                exerciseEnd = prefs.miCloudExerciseEndAt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
            )
        } else Windows()
        val sig = listOf(
            values.restingHeartRate, values.hrv, values.sleepMinutes, values.steps,
            values.bloodOxygen, values.activityMinutes,
            windows.sleepStart?.toEpochMilli(), windows.sleepEnd?.toEpochMilli(),
            windows.exerciseStart?.toEpochMilli(), windows.exerciseEnd?.toEpochMilli()
        ).joinToString(",")
        if (!force && sig == prefs.hcWriteSig && prefs.hcWriteAt > 0L) {
            return Wrote(0, listOf("unchanged"))
        }
        if (values.isEmpty && windows.sleepStart == null && windows.exerciseStart == null) {
            return Wrote(0, listOf("nothing-to-write"))
        }
        val wrote = write(context, values, windows)
        if (wrote.count > 0) store.recordHcWrite(wrote.count, sig)
        return wrote
    }
}
