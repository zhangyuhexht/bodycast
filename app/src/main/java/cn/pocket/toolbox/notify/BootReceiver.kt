package cn.pocket.toolbox.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cn.pocket.toolbox.bodycast.BodyCastStore

/**
 * 开机 / 应用更新后把提醒重新排上（22.0.0 新增）。
 *
 * AlarmManager 里的闹钟在重启后会丢，所以必须开机重排一次。
 * 这里只读偏好，不做任何数据读取与网络动作。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val store = runCatching { BodyCastStore(context) }.getOrNull() ?: return
        runCatching { ReminderScheduler.scheduleAll(context, store.prefs) }
        runCatching { Notifier.ensureChannel(context) }
        // 22.35.0：重启后桌面卡片会带着旧数据发呆（系统 30 分钟才刷一次），
        // 这里顺手把三种小组件刷成最新快照。
        runCatching { cn.pocket.toolbox.widget.WidgetRender.updateAll(context) }
    }
}
