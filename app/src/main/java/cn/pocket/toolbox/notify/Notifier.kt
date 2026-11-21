package cn.pocket.toolbox.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.pocket.toolbox.R
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.ui.MainActivity

/**
 * 通知的收发口（22.0.0 新增）。
 *
 * 产品规矩同样适用于通知：不出现分数、卡路里、体重，不用红色警告，
 * 不制造焦虑——到点轻轻说一句，点开就走。
 */
object Notifier {

    const val CHANNEL_ID = "bodycast_reminder"

    /** 建通知渠道。多次调用无副作用，应用启动时调一次即可。 */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val name = BodyCastText.text("身体天气提醒")
        val description = BodyCastText.text("到点时提醒你看一眼今天的身体天气，或者做一件几分钟的小事。")
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        // 渠道名与描述在创建那一刻就定格在系统里，之后不再跟随应用内语言。
        // 语言切换后走到这里时文本对不上，就删掉重建（22.28.0）——
        // 按任务约定用 deleteNotificationChannel + create 重建，
        // 代价是用户对这条渠道单独改过的通知设置会复位。
        if (existing != null &&
            existing.name?.toString() == name &&
            existing.description?.toString() == description
        ) {
            return
        }
        if (existing != null) {
            runCatching { manager.deleteNotificationChannel(CHANNEL_ID) }
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            this.description = description
            enableLights(false)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** 用户是否允许通知（Android 13+ 还要看运行时权限）。 */
    fun canNotify(context: Context): Boolean =
        runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }
            .getOrDefault(false)

    /**
     * 发一条提醒。同一个 id 覆盖上一条，不会堆一串。
     * 点击打开主界面（落在指定 tab 上，0 首页 / 2 回声）。
     */
    fun post(context: Context, id: Int, title: String, body: String, tab: Int = 0) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_TAB, tab)
        val pending = PendingIntent.getActivity(
            context,
            id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
