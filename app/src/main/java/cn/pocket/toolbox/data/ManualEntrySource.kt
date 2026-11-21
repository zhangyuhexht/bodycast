package cn.pocket.toolbox.data

import android.content.Context
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.BodySignals
import cn.pocket.toolbox.bodycast.SignalSource
import cn.pocket.toolbox.bodycast.EntryValidity

/**
 * 手动填写读数（22.24.0，方案 B §3.3）。
 *
 * 用户打开小米运动健康 / 华为运动健康看到数字，照着抄进本应用——这是所有自动路径
 * 都失败时**唯一一定可行**的一条：不需要 Health Connect、不需要任何权限、
 * 不需要厂商 SDK，任何手机都能用。
 *
 * 三条纪律：
 * 1. **只填认识的，其余留空**。没填的项是 null，界面照旧写「暂时没读到」；
 * 2. **24 小时后过期**（[EntryValidity]）。过期的读数不再参与今天的天气判定，
 *    界面显示「已过期」，判定退回设备 / 默认天气；
 * 3. **不碰 Health Connect 的数据**。这里读的是本机偏好里存的聚合值，
 *    HC 那一路的读数既不覆盖也不清除。
 *
 * 优先级：手动校准标签（改结论）> 手动填读数 > 导入 > Health Connect。
 * 合并发生在 `BodyCastStore.signals` 里，这一层只负责「把本机存的这一路读出来」。
 */
object ManualEntrySource : HealthSource {

    override val id = "manual_entry"
    override val label = "手动填写读数"

    override suspend fun isAvailable(context: Context) = true

    override suspend fun hasPermission(context: Context) = true

    override suspend fun read(context: Context): BodySignals {
        val prefs = BodyCastStore.loadPrefs(context)
        val values = prefs.manualEntries
        if (!prefs.manualEntryFresh || values.isEmpty) return BodySignals(source = SignalSource.NONE)
        return BodySignals(
            source = SignalSource.MANUAL_ENTRY,
            updatedAt = prefs.manualEntryAt,
            restingHeartRate = values.restingHeartRate,
            hrv = values.hrv,
            sleepMinutes = values.sleepMinutes,
            deepSleepRatio = values.deepSleepRatio,
            steps = values.steps,
            bloodOxygen = values.bloodOxygen,
            activityMinutes = values.activityMinutes
        )
    }
}
