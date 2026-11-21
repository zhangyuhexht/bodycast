package cn.pocket.toolbox.data

import android.content.Context
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.BodySignals
import cn.pocket.toolbox.bodycast.EntryValidity
import cn.pocket.toolbox.bodycast.SignalSource

/**
 * 导入数据（22.24.0，方案 B §3.3）。
 *
 * 用户把厂商 App 导出的 CSV / JSON 交给本应用（系统分享面板或设置页选文件），
 * 应用只取认识的列、只存当天的聚合读数 + 导入时间戳。原始文件只在内存里解析，
 * 不落盘、不上传；应用继续没有 INTERNET、不申请存储权限。
 *
 * 和手动填写同一条纪律：**24 小时后过期**（[EntryValidity]），过期就不参与
 * 今天的天气判定，界面显示「上次导入：昨天 21:03（已过期）」。
 * 导入优先于 Health Connect，但低于手动填读数——三者按字段合并，谁也不清空谁。
 */
object ImportSource : HealthSource {

    override val id = "import"
    override val label = "导入数据"

    override suspend fun isAvailable(context: Context) = true

    override suspend fun hasPermission(context: Context) = true

    override suspend fun read(context: Context): BodySignals {
        val prefs = BodyCastStore.loadPrefs(context)
        val values = prefs.importedEntries
        if (!prefs.importedFresh || values.isEmpty) return BodySignals(source = SignalSource.NONE)
        return BodySignals(
            source = SignalSource.IMPORT,
            updatedAt = prefs.importedAt,
            restingHeartRate = values.restingHeartRate,
            hrv = values.hrv,
            sleepMinutes = values.sleepMinutes,
            deepSleepRatio = values.deepSleepRatio,
            steps = values.steps,
            stressLevel = values.stressLevel,
            bloodOxygen = values.bloodOxygen,
            activityMinutes = values.activityMinutes
        )
    }
}
