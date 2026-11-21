package cn.pocket.toolbox.notify

import android.content.Context
import cn.pocket.toolbox.bodycast.NutritionBridge
import cn.pocket.toolbox.bodycast.WeatherRecord
import cn.pocket.toolbox.i18n.BodyCastText

/**
 * 天气变化提醒（22.32.0）：**事件驱动，不是闹钟**。
 *
 * 固定时间的「到点看一眼」会被用户关掉；而「今天的天气和昨天不一样」
 * 是真正值得说话的时刻。规则刻意保守：
 * - 只在**今天 ≠ 昨天**时说话，天气连着一样就整周安静；
 * - **每天最多一条**（本机记着「哪天发过了」，重启不重发）；
 * - 昨天没有记录就不发——没有对照就不算「变化」；
 * - 文案只播报差异 + 一条温和建议，不评价好坏、不制造焦虑。
 *
 * 全程本地：比较的是本机已有的天气记录，不联网、不上传。
 */
object WeatherChangeNotifier {

    private const val PREFS = "bodycast_weather_change"
    private const val KEY_NOTIFIED_DATE = "notified_date"
    private const val NOTIFY_ID = 1003

    /**
     * 今天和昨天的天气不一样就发一条；当天发过、或昨天没记录，都静默。
     *
     * 22.48.0：带上今天的运动读数——通知里的「今天先补」与应用首页必须是同一个
     * 结论，否则用户会觉得这两处各说各话。
     */
    fun maybeNotify(
        context: Context,
        todayRecord: WeatherRecord?,
        yesterdayRecord: WeatherRecord?,
        steps: Int? = null,
        activityMinutes: Int? = null
    ) {
        if (!shouldNotify(todayRecord, yesterdayRecord)) return
        val today = todayRecord!!
        val yesterday = yesterdayRecord!!

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_NOTIFIED_DATE, null) == today.date) return

        val focus = NutritionBridge.primary(today.kind, steps, activityMinutes)
        // 整句走 BodyCastText 的模板（「昨天是X，今天是Y。」），槽位是已登记的天气词。
        val title = BodyCastText.text("今天的天气变了")
        val body = BodyCastText.text("昨天是${yesterday.kind.label}，今天是${today.kind.label}。") + " " +
            today.kind.motto + BodyCastText.text("今天先补：${focus.label}。")

        Notifier.post(context, NOTIFY_ID, title, body, tab = 0)
        prefs.edit().putString(KEY_NOTIFIED_DATE, today.date).apply()
    }

    /**
     * 变化提醒的判定（纯函数，给单测锁口径）。
     *
     * 返回 true 的三个条件：今天有记录、昨天有记录、两者天气不同。
     * 「当天已发过」在 [maybeNotify] 里另查——那是发送侧的节流，不是判定。
     */
    fun shouldNotify(today: WeatherRecord?, yesterday: WeatherRecord?): Boolean =
        today != null && yesterday != null && today.kind != yesterday.kind
}
