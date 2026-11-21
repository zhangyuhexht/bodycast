package cn.pocket.toolbox.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.EchoGenerator
import cn.pocket.toolbox.i18n.BodyCastText

/**
 * 提醒到点时的接收器（22.0.0 新增）。
 *
 * 三种提醒：
 * - 身体来信：每周日，用本周的记录生成一封（读不到记录就如实说，不编造）；
 * - 微行动：随口一句，具体做什么留给用户打开应用再挑；
 * - 握个手：提醒看一眼今天的身体天气。
 *
 * 响过之后**自己排下一次**（一次性闹钟 + 自我重排，见 ReminderScheduler 的注释）。
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "cn.pocket.toolbox.REMINDER"
        const val EXTRA_KIND = "kind"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        @Suppress("UNCHECKED_CAST")
        val goAsyncResult = runCatching { goAsync() }.getOrNull()

        val store = runCatching { BodyCastStore(context) }.getOrNull()
        val prefs = store?.prefs

        runCatching {
            when (kind) {
                ReminderScheduler.KIND_LETTER -> {
                    val letter = store?.let {
                        EchoGenerator.bodyLetter(it.records, it.today, it.prefs.nickname)
                    }
                    val title = BodyCastText.text("这周的身体来信")
                    val body = when {
                        letter == null -> BodyCastText.text("这周还没有足够记录。今天记一天，周日就有信了。")
                        // 运行期拼串：标题和正文先各自翻译，再拼起来，否则英文通知会漏出中文。
                        letter.lines.isEmpty() -> BodyCastText.text(letter.title)
                        else -> {
                            // 22.13.0：分隔符跟语言走——英文通知里不出现中文句号。
                            // 日语和中文同用「。」，所以只有非中文、非日语才换成 ASCII 的 ". "。
                            val language = cn.pocket.toolbox.i18n.AppLanguage.current()
                            val fullWidthStop = language == cn.pocket.toolbox.i18n.AppLanguage.SIMPLIFIED_CHINESE ||
                                language == cn.pocket.toolbox.i18n.AppLanguage.TRADITIONAL_CHINESE ||
                                language == cn.pocket.toolbox.i18n.AppLanguage.JAPANESE
                            val sep = if (fullWidthStop) "。" else ". "
                            BodyCastText.text(letter.title) + sep + BodyCastText.text(letter.lines.first().text)
                        }
                    }
                    Notifier.post(context, id = 4101, title = title, body = body, tab = 2)
                }

                ReminderScheduler.KIND_ACTION -> {
                    Notifier.post(
                        context,
                        id = 4102,
                        title = BodyCastText.text("有件几分钟的小事在等你"),
                        body = BodyCastText.text("打开 BodyCast，从今天可以做什么里挑一件——随便挑一件做就行，不用每件都做。"),
                        tab = 0
                    )
                }

                ReminderScheduler.KIND_HANDSHAKE -> {
                    Notifier.post(
                        context,
                        id = 4103,
                        title = BodyCastText.text("今天身体是什么天气？"),
                        body = BodyCastText.text("看一眼就好。它不评价，也不催你。"),
                        tab = 0
                    )
                }
            }
        }

        // 排下一次（偏好可能已经被改过，以当前值为准）。
        prefs?.let { ReminderScheduler.rescheduleOne(context, kind, it) }
        runCatching { goAsyncResult?.finish() }
    }
}
