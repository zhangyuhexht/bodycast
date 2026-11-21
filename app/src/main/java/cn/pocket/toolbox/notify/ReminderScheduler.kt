package cn.pocket.toolbox.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import cn.pocket.toolbox.bodycast.BodyCastPrefs
import java.time.ZonedDateTime

/**
 * 提醒调度（22.0.0 新增）。
 *
 * 用 AlarmManager 的一次性闹钟 + 每次响过之后自己排下一次：
 * - 不用 setExactAndAllowWhileIdle：那需要 SCHEDULE_EXACT_ALARM 权限，
 *   而这个应用的提醒是「差不多到点就行」的温柔提醒，不需要精确到分钟；
 * - 用 setAndAllowWhileIdle：不需要额外权限，Doze 下也能醒，代价是可能晚几分钟——
 *   这个取舍写在这里，别以后被误认为 bug。
 *
 * 免打扰时段（quietStartHour → quietEndHour，可跨零点）：到点时如果落在里面，
 * 就顺延到免打扰结束的那一刻，不硬发。
 */
object ReminderScheduler {

    const val KIND_LETTER = "letter"      // 身体来信（每周日）
    const val KIND_ACTION = "action"      // 微行动提醒（每天）
    const val KIND_HANDSHAKE = "handshake" // 看一眼今天的天气（每天）

    private const val REQ_LETTER = 4001
    private const val REQ_ACTION = 4002
    private const val REQ_HANDSHAKE = 4003

    /** 按当前偏好重排全部提醒。关掉的会被取消。 */
    fun scheduleAll(context: Context, prefs: BodyCastPrefs) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return

        if (prefs.calendarReminder) {
            // 每周日 20:00
            val at = nextWeekly(dayOfWeek = 7, hour = 20, minute = 0)
            setAlarm(context, manager, REQ_LETTER, KIND_LETTER, shiftOutOfQuiet(at, prefs))
        } else {
            cancel(context, manager, REQ_LETTER, KIND_LETTER)
        }

        if (prefs.actionReminder) {
            val at = nextDaily(prefs.actionHour, prefs.actionMinute)
            setAlarm(context, manager, REQ_ACTION, KIND_ACTION, shiftOutOfQuiet(at, prefs))
        } else {
            cancel(context, manager, REQ_ACTION, KIND_ACTION)
        }

        if (prefs.handshakeReminder) {
            val at = nextDaily(prefs.handshakeHour, prefs.handshakeMinute)
            setAlarm(context, manager, REQ_HANDSHAKE, KIND_HANDSHAKE, shiftOutOfQuiet(at, prefs))
        } else {
            cancel(context, manager, REQ_HANDSHAKE, KIND_HANDSHAKE)
        }
    }

    /** 响过一次之后，由接收器调用，把下一次排上。 */
    fun rescheduleOne(context: Context, kind: String, prefs: BodyCastPrefs) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        when (kind) {
            KIND_LETTER -> if (prefs.calendarReminder) {
                val at = nextWeekly(dayOfWeek = 7, hour = 20, minute = 0)
                setAlarm(context, manager, REQ_LETTER, KIND_LETTER, shiftOutOfQuiet(at, prefs))
            }
            KIND_ACTION -> if (prefs.actionReminder) {
                val at = nextDaily(prefs.actionHour, prefs.actionMinute)
                setAlarm(context, manager, REQ_ACTION, KIND_ACTION, shiftOutOfQuiet(at, prefs))
            }
            KIND_HANDSHAKE -> if (prefs.handshakeReminder) {
                val at = nextDaily(prefs.handshakeHour, prefs.handshakeMinute)
                setAlarm(context, manager, REQ_HANDSHAKE, KIND_HANDSHAKE, shiftOutOfQuiet(at, prefs))
            }
        }
    }

    // ---------------------------------------------------------------------

    private fun setAlarm(
        context: Context,
        manager: AlarmManager,
        requestCode: Int,
        kind: String,
        atMillis: Long
    ) {
        val pending = pendingIntent(context, requestCode, kind, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, atMillis, pending)
            }
        }
    }

    private fun cancel(context: Context, manager: AlarmManager, requestCode: Int, kind: String) {
        val pending = pendingIntent(context, requestCode, kind, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching { manager.cancel(pending) }
        pending.cancel()
    }

    private fun pendingIntent(
        context: Context,
        requestCode: Int,
        kind: String,
        extraFlags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMINDER)
            .putExtra(ReminderReceiver.EXTRA_KIND, kind)
        val flags = extraFlags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /** 下一个「每周某天的 hh:mm」：今天还没到就是今天，否则下周。 */
    private fun nextWeekly(dayOfWeek: Int, hour: Int, minute: Int): Long {
        var target = ZonedDateTime.now()
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val daysAhead = ((dayOfWeek - target.dayOfWeek.value) + 7) % 7
        target = target.plusDays(daysAhead.toLong())
        if (target.toInstant().toEpochMilli() <= System.currentTimeMillis()) {
            target = target.plusWeeks(1)
        }
        return target.toInstant().toEpochMilli()
    }

    private fun nextDaily(hour: Int, minute: Int): Long {
        var target = ZonedDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (target.toInstant().toEpochMilli() <= System.currentTimeMillis()) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    /**
     * 落在免打扰时段里就顺延到免打扰结束。
     * 免打扰是「从 quietStartHour 到 quietEndHour」的区间，可以跨零点（默认 23 → 7）。
     */
    private fun shiftOutOfQuiet(atMillis: Long, prefs: BodyCastPrefs): Long {
        // 免打扰总开关关掉时就不过滤（用户在设置里明确选了「不必守免打扰」）。
        if (!prefs.quietMode) return atMillis
        val start = prefs.quietStartHour
        val end = prefs.quietEndHour
        if (start == end) return atMillis

        val at = java.time.Instant.ofEpochMilli(atMillis)
            .atZone(java.time.ZoneId.systemDefault())
        val hour = at.hour
        val inside = if (start < end) hour in start until end else (hour >= start || hour < end)
        if (!inside) return atMillis

        var resume = at.withMinute(0).withSecond(0).withNano(0).withHour(end)
        if (resume.toInstant().toEpochMilli() <= atMillis) resume = resume.plusDays(1)
        return resume.toInstant().toEpochMilli()
    }
}
