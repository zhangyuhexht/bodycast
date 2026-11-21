package cn.pocket.toolbox.bodycast

import cn.pocket.toolbox.notify.WeatherChangeNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 22.32.0 的两条新口径：
 * - [EchoInsights] 模式洞察——只说观察不下诊断、数据不够就不说；
 * - [WeatherChangeNotifier.shouldNotify] 天气变化提醒——今天≠昨天才说话。
 */
class InsightsAndChangeTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 13) // 周二

    private fun record(date: LocalDate, kind: WeatherKind, vararg tags: CalibrationTag): WeatherRecord =
        WeatherRecord(date.toString(), kind, tags.toSet())

    private fun weekOf(kind: WeatherKind): Map<String, WeatherRecord> =
        (6 downTo 0).associate { offset ->
            val d = today.minusDays(offset.toLong())
            d.toString() to record(d, kind)
        }

    // ---- 模式洞察 ----

    @Test
    fun tooFewDaysSaysNothing() {
        val records = (0..3).associate { offset ->
            val d = today.minusDays(offset.toLong())
            d.toString() to record(d, WeatherKind.PRESSURE)
        }
        assertTrue(EchoInsights.generate(records, today).isEmpty())
    }

    @Test
    fun dominantWeekKindIsReported() {
        val out = EchoInsights.generate(weekOf(WeatherKind.PRESSURE), today)
        assertTrue(out.isNotEmpty())
        assertTrue(out.first().contains("压力天"))
    }

    @Test
    fun insightNeverGivesDiagnosis() {
        val out = EchoInsights.generate(weekOf(WeatherKind.SENSITIVE), today)
        // 纪律：不出现「你压力过大」「你该……」这类下诊断的句式。
        out.forEach { line ->
            assertFalse(line.contains("你压力"))
            assertFalse(line.contains("不正常"))
            assertFalse(line.contains("你该"))
        }
    }

    @Test
    fun weekdayPatternIsObserved() {
        // 两个周六都是恢复天 → 「周六常常是恢复天」。
        val base = weekOf(WeatherKind.BALANCED).toMutableMap()
        (0..1L).forEach { weeksBack ->
            val sat = today.minusDays((today.dayOfWeek.value - 6).toLong() + weeksBack * 7)
            base[sat.toString()] = record(sat, WeatherKind.RECOVERING)
        }
        val out = EchoInsights.generate(base, today)
        assertTrue(out.any { it.contains("周六") })
    }

    @Test
    fun atMostThreeInsights() {
        val out = EchoInsights.generate(weekOf(WeatherKind.PRESSURE), today)
        assertTrue(out.size <= 3)
    }

    // ---- 天气变化提醒 ----

    @Test
    fun notifyOnlyWhenKindChanges() {
        val todayRec = record(today, WeatherKind.PRESSURE)
        val yesterdaySame = record(today.minusDays(1), WeatherKind.PRESSURE)
        val yesterdayDiff = record(today.minusDays(1), WeatherKind.BALANCED)
        assertFalse(WeatherChangeNotifier.shouldNotify(todayRec, yesterdaySame))
        assertTrue(WeatherChangeNotifier.shouldNotify(todayRec, yesterdayDiff))
    }

    @Test
    fun noRecordNoNotification() {
        val todayRec = record(today, WeatherKind.PRESSURE)
        assertFalse(WeatherChangeNotifier.shouldNotify(todayRec, null))
        assertFalse(WeatherChangeNotifier.shouldNotify(null, record(today.minusDays(1), WeatherKind.PRESSURE)))
        assertFalse(WeatherChangeNotifier.shouldNotify(null, null))
    }
}
