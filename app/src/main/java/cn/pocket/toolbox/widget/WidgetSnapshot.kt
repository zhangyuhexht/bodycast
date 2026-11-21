package cn.pocket.toolbox.widget

import android.content.Context

/**
 * 桌面小组件的数据快照（22.0.0 新增；22.35.0 扩四信号）。
 *
 * 为什么不直接读 BodyCastStore：应用里的信号（signals）是内存态、不落盘，
 * 小组件进程里读不到；而小组件只需要「今天是什么天气 + 一句话 + 今日小事 + 四信号水平词」，
 * 所以应用每次算出天气时顺手把这几项写进一个独立的 SharedPreferences，
 * 小组件只管渲染。两个方向都是只读自己那点数据，互不依赖。
 *
 * 存的都是**按应用语言翻好的显示串**（保存时过一遍 BodyCastText），
 * 小组件进程不需要知道词条表；换语言后应用下次结算时会重写快照。
 */
object WidgetSnapshot {

    private const val PREFS = "bodycast_widget"
    private const val KEY_KIND = "kind"
    private const val KEY_MOTTO = "motto"
    private const val KEY_HEADLINE = "headline"
    private const val KEY_ACTION = "action"
    private const val KEY_FOCUS_LABEL = "focus_label"
    private const val KEY_FOCUS_ID = "focus_id"
    private const val KEY_FOCUS_REASON = "focus_reason"
    private const val KEY_DARK = "dark"
    private const val KEY_ACTION_MINUTES = "action_minutes"
    private const val KEY_COLOR = "color_hex"
    private const val KEY_UPDATED = "updated"
    private const val KEY_DEMO = "demo"
    private const val KEY_METRIC_TITLES = "metric_titles"
    private const val KEY_METRIC_LEVELS = "metric_levels"
    private const val SEP = ""

    data class Snapshot(
        val kind: String,
        val motto: String,
        val headline: String,
        val action: String,
        /** 今天先补什么（营养素的显示名）。 */
        val focusLabel: String,
        /** 同一元素的枚举名，点「今天吃什么」带回食谱页做筛选用。 */
        val focusId: String,
        /** 今天为什么先补它（带数字的理由，22.49.0；已按应用语言翻好）。 */
        val focusReason: String = "",
        /** 今日处方的总时长（分钟，22.49.0）；0 = 还没开出处方。 */
        val actionMinutes: Int = 0,
        /** 应用内的深浅色（22.52.0）：小组件跟着它走，不再只看系统。 */
        val dark: Boolean = false,
        /** 天气配色（0xAARRGGBB），强调条与格言随它染色。 */
        val colorHex: Long,
        val updatedAt: Long,
        val demo: Boolean,
        /** 四信号标题（0=恢复 1=压力 2=能量 3=睡眠），保存时已翻译。 */
        val metricTitles: List<String> = emptyList(),
        /** 四信号水平词，缺的位是「暂时没读到」。 */
        val metricLevels: List<String> = emptyList()
    )

    fun save(
        context: Context,
        kind: String,
        motto: String,
        headline: String,
        action: String,
        focusLabel: String,
        focusId: String,
        focusReason: String = "",
        actionMinutes: Int = 0,
        dark: Boolean = false,
        colorHex: Long,
        demo: Boolean,
        metricTitles: List<String> = emptyList(),
        metricLevels: List<String> = emptyList()
    ) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_KIND, kind)
                .putString(KEY_MOTTO, motto)
                .putString(KEY_HEADLINE, headline)
                .putString(KEY_ACTION, action)
                .putString(KEY_FOCUS_LABEL, focusLabel)
                .putString(KEY_FOCUS_ID, focusId)
                .putString(KEY_FOCUS_REASON, focusReason)
                .putInt(KEY_ACTION_MINUTES, actionMinutes)
                .putBoolean(KEY_DARK, dark)
                .putLong(KEY_COLOR, colorHex)
                .putLong(KEY_UPDATED, System.currentTimeMillis())
                .putBoolean(KEY_DEMO, demo)
                .putString(KEY_METRIC_TITLES, metricTitles.joinToString(SEP))
                .putString(KEY_METRIC_LEVELS, metricLevels.joinToString(SEP))
                .apply()
        }
    }

    /** 读不到就返回 null —— 小组件会显示「还没有读到数据」，绝不编造。 */
    fun load(context: Context): Snapshot? = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val kind = prefs.getString(KEY_KIND, null) ?: return null
        fun list(key: String): List<String> =
            prefs.getString(key, "")?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
        Snapshot(
            kind = kind,
            motto = prefs.getString(KEY_MOTTO, "") ?: "",
            headline = prefs.getString(KEY_HEADLINE, "") ?: "",
            action = prefs.getString(KEY_ACTION, "") ?: "",
            focusLabel = prefs.getString(KEY_FOCUS_LABEL, "") ?: "",
            focusId = prefs.getString(KEY_FOCUS_ID, "") ?: "",
            focusReason = prefs.getString(KEY_FOCUS_REASON, "") ?: "",
            actionMinutes = prefs.getInt(KEY_ACTION_MINUTES, 0),
            dark = prefs.getBoolean(KEY_DARK, false),
            colorHex = prefs.getLong(KEY_COLOR, 0xFF3ED598L),
            updatedAt = prefs.getLong(KEY_UPDATED, 0L),
            demo = prefs.getBoolean(KEY_DEMO, false),
            metricTitles = list(KEY_METRIC_TITLES),
            metricLevels = list(KEY_METRIC_LEVELS)
        )
    }.getOrNull()

    /**
     * 只更新「应用内深浅色」这一项（22.52.0）。
     *
     * 用户在设置里换深浅色时，天气与处方都没变，不该为了一个标记去重写整份快照；
     * 调用方接一次 [cn.pocket.toolbox.widget.BodyCastWidgetProvider.updateAll] 就够。
     */
    fun setDark(context: Context, dark: Boolean) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_DARK, dark)
                .apply()
        }
    }

    fun clear(context: Context) {
        runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply() }
    }
}
