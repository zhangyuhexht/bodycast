package cn.pocket.toolbox.data

import cn.pocket.toolbox.bodycast.BodyCastPrefs
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.CalibrationTag
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.bodycast.WeatherRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份与换机（22.0.0 新增）。
 *
 * 备份文件是一个 `.bodycast`（内部就是 JSON）。里面只有两样东西：
 * 1. 每天的天气记录（日期 + 天气类型 + 那天勾过的标签）；
 * 2. 偏好里那些「本机选择」（语言、主题、字号、提醒时间、敏感度……）。
 *
 * **不包含**任何原始生理数值——本机本来就不存心率/睡眠时长的原始值，
 * 备份里自然也不会有。这一点和「导出到剪贴板」保持同一口径。
 *
 * 用 Android 自带的 org.json（不用引依赖），解析失败一律返回 null，
 * 绝不因为一个坏文件把应用带崩。
 */
object Backup {

    const val FORMAT = "bodycast-backup"
    const val CURRENT_VERSION = 1

    data class RecordLine(val date: String, val weatherId: String, val tags: List<String>)

    data class Parsed(
        val version: Int,
        val exportedAt: Long,
        val records: List<RecordLine>,
        val encodedPrefs: String?
    ) {
        val dayCount: Int get() = records.size
    }

    /** 生成备份文本。records 直接用仓库里的那份（日期 → 记录）。 */
    fun build(records: Map<String, WeatherRecord>, prefs: BodyCastPrefs): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", CURRENT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        records.values.sortedBy { it.date }.forEach { record ->
            val item = JSONObject()
            item.put("date", record.date)
            item.put("weather", record.kind.name)
            val tags = JSONArray()
            record.calibrated.forEach { tags.put(it.name) }
            item.put("tags", tags)
            array.put(item)
        }
        root.put("records", array)
        root.put("prefs", prefs.encode())
        return root.toString(2)
    }

    /** 解析备份文本。任何异常都吃掉，返回 null 由界面提示「这个文件读不了」。 */
    fun parse(text: String): Parsed? = runCatching {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) return null
        val version = root.optInt("version", 0)
        val exportedAt = root.optLong("exportedAt", 0L)
        val array = root.optJSONArray("records") ?: JSONArray()
        val records = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val date = item.optString("date").takeIf { it.isNotBlank() } ?: continue
                val weatherId = item.optString("weather")
                if (WeatherKind.fromId(weatherId) == null) continue
                val tagsJson = item.optJSONArray("tags") ?: JSONArray()
                val tags = buildList {
                    for (j in 0 until tagsJson.length()) {
                        val id = tagsJson.optString(j)
                        if (CalibrationTag.fromId(id) != null) add(id)
                    }
                }
                add(RecordLine(date, weatherId, tags))
            }
        }
        Parsed(
            version = version,
            exportedAt = exportedAt,
            records = records,
            encodedPrefs = root.optString("prefs").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    /** 把解析出来的记录转成仓库认识的文本格式（date|KIND|TAG,TAG）。 */
    fun toRecordText(parsed: Parsed): String =
        parsed.records.joinToString("\n") { "${it.date}|${it.weatherId}|${it.tags.joinToString(",")}" }

    /**
     * 导入。交给仓库去合并与落盘；返回值是这次写进去的天数。
     * 这里只做转换，不碰持久化（持久化在 [BodyCastStore.restoreFromBackup]）。
     */
    fun restore(store: BodyCastStore, parsed: Parsed): Int =
        store.restoreFromBackup(toRecordText(parsed), parsed.encodedPrefs)
}
