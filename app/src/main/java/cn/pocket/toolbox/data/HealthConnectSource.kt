package cn.pocket.toolbox.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import cn.pocket.toolbox.bodycast.BodySignals
import cn.pocket.toolbox.bodycast.SignalSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.reflect.KClass

/**
 * Health Connect 数据源。
 *
 * 小米、华为等厂商的手环数据，只要用户允许同步到 Health Connect，
 * 就能通过这一条通道读到——所以这里不需要为每个品牌单独写适配。
 *
 * 两条纪律：
 * 1. **每一项独立 try**。某一项读失败（没授权、没有该类型数据）只让那一项留空，
 *    不能让整次读取失败。
 * 2. **读不到就 null**。没有睡眠数据时 sleepMinutes 是 null 而不是 0，
 *    上层会显示「暂时没读到」，而不是「你昨晚睡了 0 分钟」。
 *
 * 22.26.0 起**权限也按类各自算账**：某一类没授权只让那一类留空，别的照读——
 * 以前要求七项权限全齐才肯读，少勾一项就整页「暂时没读到」。
 *
 * 22.22.0 起每类都记下 `dataOrigin.packageName`（写进 [BodySignals.originByType]），
 * 自检卡片第 ④ 步要按类型显示「这条是谁写的」；步数改成按来源分组再取最大者，
 * 避免系统本机步数与手环步数相加。
 */
object HealthConnectSource : HealthSource {

    override val id = "health_connect"
    override val label = "Health Connect"

    /** 只申请真正用到的读取权限。不申请任何写入权限。 */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        // 动态心率：给「静息心率」做兜底估算用（厂商常常只写瞬时心率序列）。
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    override suspend fun isAvailable(context: Context): Boolean = runCatching {
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }.getOrDefault(false)

    /**
     * 有没有拿到**任何一类**的读取权限。
     *
     * 以前这里是「七项全齐才算数」，结果是：只差动态心率那一项没勾时，
     * 六类全跟着读不到（22.26.0 真机反馈「授权完数据还是断的」的直接原因）。
     * 现在按类各自算账，见 [read]。
     */
    override suspend fun hasPermission(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            HealthConnectBridge.DataType.entries.any { granted.contains(it.permission) }
        }.getOrDefault(false)
    }

    override suspend fun read(context: Context): BodySignals {
        val client = runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
            ?: return BodySignals(source = SignalSource.NONE)

        // 逐类按各自的权限读：某一类没授权只让那一类留空，别的照读。
        // 以前这里要求七项权限全齐（含只给静息心率兜底用的动态心率）才肯读任何东西，
        // 少勾一项就整页「暂时没读到」，而自检第 ② 步还在说「六类都授权了」。
        val granted = runCatching {
            client.permissionController.getGrantedPermissions()
        }.getOrDefault(emptySet())
        fun permitted(type: HealthConnectBridge.DataType): Boolean =
            granted.contains(type.permission)

        // 一类都没授权：这条通道整个没开，按老规矩返回空实例（source = NONE）。
        if (HealthConnectBridge.DataType.entries.none { permitted(it) }) {
            return BodySignals(source = SignalSource.NONE)
        }

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val todayStart = LocalDate.now().atStartOfDay(zone).toInstant()
        // 静息心率 / HRV / 血氧：厂商常常一天只写一两条，只读「今天 00:00 之后」
        // 几乎必然读不到，所以窗口放宽到 48 小时（方案 §3.4）。
        val recent48h = now.minus(Duration.ofHours(48))
        // 睡眠：最近 36 小时内的最长一段——跨零点、午后补睡都能覆盖。
        val sleepFrom = now.minus(Duration.ofHours(36))

        // 这次读到的每条记录是哪个 App 写进来的。origins 是合集，
        // originByType 按类型分开记，自检卡片第 ④ 步用后者。
        val origins = linkedSetOf<String>()
        val originByType = linkedMapOf<String, String>()

        fun note(key: String, origin: String?) {
            if (origin.isNullOrBlank()) return
            origins += origin
            originByType.putIfAbsent(key, origin)
        }

        val steps = if (permitted(HealthConnectBridge.DataType.STEPS)) {
            runCatching { readStepsByOrigin(client, todayStart, now) }.getOrNull()
        } else null
        val sleep = if (permitted(HealthConnectBridge.DataType.SLEEP)) {
            runCatching { readSleep(client, sleepFrom, now) }.getOrNull()
        } else null
        val resting = if (permitted(HealthConnectBridge.DataType.RESTING_HEART_RATE)) {
            // 兜底估算只在动态心率也授权了的时候做：那是拿别人的记录推的，
            // 没授权就只认厂商写好的静息心率记录。
            val allowEstimate = granted.contains(
                HealthPermission.getReadPermission(HeartRateRecord::class)
            )
            runCatching { readRestingHeartRate(client, recent48h, now, allowEstimate) }.getOrNull()
        } else null
        val hrv = if (permitted(HealthConnectBridge.DataType.HRV)) {
            runCatching {
                latest(client, HeartRateVariabilityRmssdRecord::class, recent48h, now,
                    timeOf = { it.time }) { it.heartRateVariabilityMillis.toInt() }
            }.getOrNull()
        } else null
        val spo2 = if (permitted(HealthConnectBridge.DataType.BLOOD_OXYGEN)) {
            runCatching {
                latest(client, OxygenSaturationRecord::class, recent48h, now,
                    timeOf = { it.time }) { it.percentage.value.toInt() }
            }.getOrNull()
        } else null
        val exercise = if (permitted(HealthConnectBridge.DataType.EXERCISE)) {
            runCatching { readExerciseMinutes(client, todayStart, now) }.getOrNull()
        } else null

        note(HealthConnectBridge.DataType.STEPS.key, steps?.origin)
        note(HealthConnectBridge.DataType.SLEEP.key, sleep?.origin)
        note(HealthConnectBridge.DataType.RESTING_HEART_RATE.key, resting?.origin)
        note(HealthConnectBridge.DataType.HRV.key, hrv?.origin)
        note(HealthConnectBridge.DataType.BLOOD_OXYGEN.key, spo2?.origin)
        note(HealthConnectBridge.DataType.EXERCISE.key, exercise?.origin)

        return BodySignals(
            source = SignalSource.HEALTH_CONNECT,
            updatedAt = System.currentTimeMillis(),
            restingHeartRate = resting?.value,
            restingHeartRateEstimated = resting?.estimated == true,
            hrv = hrv?.value,
            sleepMinutes = sleep?.value?.minutes,
            deepSleepRatio = sleep?.value?.deepRatio,
            steps = steps?.value,
            // Health Connect 没有标准的「压力值」类型。
            // 与其用别的字段硬凑一个，不如留空让界面说「暂时没读到」。
            stressLevel = null,
            bloodOxygen = spo2?.value,
            activityMinutes = exercise?.value,
            origins = origins,
            originByType = originByType
        )
    }

    // ---------------------------------------------------------------------
    // 22.37.0 Health Connect 内容透视
    // ---------------------------------------------------------------------

    /**
     * Health Connect 里**实际存了什么**（最近 30 天）：每类的条数、最新一条时间与来源。
     *
     * 这是给「HC 这条路到底断在哪」的诊断用的：
     * - 0 条 = **厂商根本没写**（去厂商 App 开 Health Connect 同步）；
     * - 有条但我们读不到 = **授权或读取侧**的问题（回自检第②步）。
     * 只读不做写入。
     */
    data class Probe(
        val type: HealthConnectBridge.DataType,
        val count: Int,
        val latestAt: Long?,
        val origin: String?
    )

    suspend fun probe(context: Context): List<Probe> = withContext(Dispatchers.IO) {
        val client = runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
            ?: return@withContext emptyList()
        val from = Instant.now().minus(Duration.ofDays(30))

        suspend fun <T : androidx.health.connect.client.records.Record> countOf(
            type: HealthConnectBridge.DataType,
            clazz: kotlin.reflect.KClass<T>,
            timeOf: (T) -> Instant
        ): Probe {
            val rows = runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = clazz,
                        timeRangeFilter = TimeRangeFilter.after(from)
                    )
                ).records
            }.getOrDefault(emptyList())
            val latest = rows.maxByOrNull { timeOf(it) }
            return Probe(
                type = type,
                count = rows.size,
                latestAt = latest?.let { timeOf(it).toEpochMilli() },
                origin = latest?.metadata?.dataOrigin?.packageName
            )
        }

        listOf(
            countOf(HealthConnectBridge.DataType.STEPS, StepsRecord::class) { it.endTime },
            countOf(HealthConnectBridge.DataType.SLEEP, SleepSessionRecord::class) { it.endTime },
            countOf(HealthConnectBridge.DataType.RESTING_HEART_RATE, RestingHeartRateRecord::class) { it.time },
            countOf(HealthConnectBridge.DataType.HRV, HeartRateVariabilityRmssdRecord::class) { it.time },
            countOf(HealthConnectBridge.DataType.BLOOD_OXYGEN, OxygenSaturationRecord::class) { it.time },
            countOf(HealthConnectBridge.DataType.EXERCISE, ExerciseSessionRecord::class) { it.endTime }
        )
    }

    // ---------------------------------------------------------------------

    /** 一次读取的结果：值 + 写这条记录的包名 + 是不是估算出来的。 */
    private data class Reading<T>(
        val value: T,
        val origin: String?,
        val estimated: Boolean = false
    )

    /** 睡眠一次读取的结果。 */
    private data class SleepReading(val minutes: Int, val deepRatio: Float?)

    /**
     * 步数：**按来源分组求和，取最大的那一路**。
     *
     * 不用不带来源的 `COUNT_TOTAL` 聚合：Android 14 起系统健康服务自己也会记录
     * 本机步数，直接聚合会把「手机步数 + 手环步数」加在一起、结果偏大。
     * 分组后取最大者 = 谁记得多就以谁为准，绝不叠加。
     */
    private suspend fun readStepsByOrigin(
        client: HealthConnectClient,
        from: Instant,
        to: Instant
    ): Reading<Int>? {
        val byOrigin = linkedMapOf<String, Long>()
        var token: String? = null
        var pages = 0
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                    pageSize = 1000,
                    pageToken = token
                )
            )
            response.records.forEach { record ->
                val pkg = record.metadata.dataOrigin.packageName
                byOrigin[pkg] = (byOrigin[pkg] ?: 0L) + record.count
            }
            token = response.pageToken
            pages++
        } while (token != null && pages < 3)

        val best = byOrigin.maxByOrNull { it.value } ?: return null
        if (best.value <= 0L) return null
        return Reading(best.value.toInt(), best.key)
    }

    /** 返回 (总时长分钟, 深睡占比) + 来源。 */
    private suspend fun readSleep(
        client: HealthConnectClient,
        from: Instant,
        to: Instant
    ): Reading<SleepReading>? {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to)
            )
        )
        val session = response.records.maxByOrNull {
            Duration.between(it.startTime, it.endTime).toMinutes()
        } ?: return null

        val totalMinutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt()
        if (totalMinutes <= 0) return null

        // 深睡占比只有设备上报了睡眠分期才算得出来，否则留 null。
        // 注意：分期常量挂在 SleepSessionRecord 上（STAGE_TYPE_DEEP），
        // 并不存在独立的 SleepStageRecord 类型。
        val stages = session.stages
        val deepRatio = if (stages.isEmpty()) {
            null
        } else {
            val deep = stages.filter { it.stage == SleepSessionRecord.STAGE_TYPE_DEEP }
                .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            if (deep <= 0L) null else (deep.toFloat() / totalMinutes).coerceIn(0f, 1f)
        }
        return Reading(
            SleepReading(totalMinutes, deepRatio),
            session.metadata.dataOrigin.packageName
        )
    }

    /**
     * 静息心率：先读厂商写好的静息心率记录；没有就用动态心率兜底估一个。
     *
     * 兜底算法：48 小时内所有动态心率样本里取**最低十分位**的平均值——
     * 那是「安静时」的那一档，比拿全体平均值合理。估算值会被标出来
     * （[BodySignals.restingHeartRateEstimated]），绝不冒充厂商写的静息心率。
     */
    private suspend fun readRestingHeartRate(
        client: HealthConnectClient,
        from: Instant,
        to: Instant,
        allowEstimate: Boolean
    ): Reading<Int>? {
        latest(client, RestingHeartRateRecord::class, from, to,
            timeOf = { it.time }) { it.beatsPerMinute.toInt() }?.let { return it }
        if (!allowEstimate) return null
        return runCatching { estimateRestingHeartRate(client, from, to) }.getOrNull()
    }

    private suspend fun estimateRestingHeartRate(
        client: HealthConnectClient,
        from: Instant,
        to: Instant
    ): Reading<Int>? {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to)
            )
        )
        if (response.records.isEmpty()) return null
        val beats = response.records.flatMap { it.samples }.map { it.beatsPerMinute }.sorted()
        // 样本太少时估出来的数不可信，直接说读不到。
        if (beats.size < 20) return null
        val lowCount = (beats.size / 10).coerceAtLeast(1)
        val average = beats.take(lowCount).average().roundToInt()
        if (average <= 0) return null
        val origin = response.records.maxByOrNull { it.endTime }?.metadata?.dataOrigin?.packageName
        return Reading(average, origin, estimated = true)
    }

    private suspend fun readExerciseMinutes(
        client: HealthConnectClient,
        from: Instant,
        to: Instant
    ): Reading<Int>? {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to)
            )
        )
        if (response.records.isEmpty()) return null
        val minutes = response.records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }.toInt()
        val origin = response.records.maxByOrNull { it.endTime }?.metadata?.dataOrigin?.packageName
        return Reading(minutes, origin)
    }

    /**
     * 取时间窗内最后一条记录，并映射成需要的值。
     *
     * 时间字段只能由调用方通过 [timeOf] 传进来：`Record` 接口本身只有
     * `metadata`，`time` 是子类才有的（而且 `InstantaneousRecord` 在库里是
     * internal，没法拿来当泛型上界）。显式传一个取值函数最稳妥。
     */
    private suspend fun <T : Record> latest(
        client: HealthConnectClient,
        type: KClass<T>,
        from: Instant,
        to: Instant,
        timeOf: (T) -> Instant,
        map: (T) -> Int
    ): Reading<Int>? {
        val response = client.readRecords(
            ReadRecordsRequest(recordType = type, timeRangeFilter = TimeRangeFilter.between(from, to))
        )
        val record = response.records.maxByOrNull(timeOf) ?: return null
        return Reading(map(record), record.metadata.dataOrigin.packageName)
    }
}
