package cn.pocket.toolbox.bodycast

import cn.pocket.toolbox.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 部分信号可用时的判定（22.27.0）。
 *
 * 产品铁律不许破：不评分、不排名、读不到就说读不到（null 不补 0）；
 * 四信号全空才走「敏感天 / 默认天气」老路径；手动校准永远最高优先。
 * 这里锁的是「有几路就按几路给结论，缺的那几路写暂时没读到」。
 */
class PartialSignalTest {

    private val engine = LocalRuleEngine()

    @org.junit.Before
    fun forceChineseSourceText() {
        // 断言的是中文源串（「暂时没读到」「1/4」）；测试 JVM 的 locale 不做假设。
        AppLanguage.apply(AppLanguage.SIMPLIFIED_CHINESE)
    }

    private fun weather(
        signals: BodySignals,
        calibrated: Set<CalibrationTag> = emptySet(),
        defaultKind: WeatherKind? = null
    ): BodyWeather = engine.weather(signals, calibrated, seed = 42L, defaultKind = defaultKind)

    private fun BodyWeather.levelOf(key: String): String = metric(key)!!.level

    private fun BodyWeather.uncertainOf(key: String): Boolean = metric(key)!!.uncertain

    // ------------------------------------------------------------------
    // 部分信号 → 部分结论
    // ------------------------------------------------------------------

    @Test
    fun stepsAndActivityGiveEnergyButNotOthers() {
        val signals = BodySignals(steps = 8000, activityMinutes = 30)
        assertEquals(1, engine.signalCount(signals))
        val w = weather(signals)
        assertFalse(w.uncertainOf(WeatherBodyWeatherKeys.ENERGY))
        assertTrue(w.uncertainOf(WeatherBodyWeatherKeys.RECOVERY))
        assertTrue(w.uncertainOf(WeatherBodyWeatherKeys.PRESSURE))
        assertTrue(w.uncertainOf(WeatherBodyWeatherKeys.SLEEP))
        // 缺的三路写「暂时没读到」（源串；界面经 i18n 翻译），不编数。
        assertEquals("暂时没读到", w.levelOf(WeatherBodyWeatherKeys.RECOVERY))
    }

    @Test
    fun hrvAndRestingHeartRateGiveRecovery() {
        val signals = BodySignals(hrv = 50, restingHeartRate = 60)
        assertEquals(1, engine.signalCount(signals))
        val w = weather(signals)
        assertFalse(w.uncertainOf(WeatherBodyWeatherKeys.RECOVERY))
        assertTrue(w.uncertainOf(WeatherBodyWeatherKeys.ENERGY))
    }

    @Test
    fun sleepAloneGivesSleepLayer() {
        val signals = BodySignals(sleepMinutes = 420, deepSleepRatio = 0.2f)
        assertEquals(1, engine.signalCount(signals))
        val w = weather(signals)
        assertFalse(w.uncertainOf(WeatherBodyWeatherKeys.SLEEP))
        assertTrue(w.uncertainOf(WeatherBodyWeatherKeys.PRESSURE))
    }

    @Test
    fun stressOnlyFromImportGivesPressure() {
        val signals = BodySignals(stressLevel = 20)
        assertEquals(1, engine.signalCount(signals))
        val w = weather(signals)
        assertFalse(w.uncertainOf(WeatherBodyWeatherKeys.PRESSURE))
    }

    @Test
    fun bloodOxygenAloneCountsAsNoSignal() {
        // 血氧不参与四路信号的任何一路：只读到血氧 = 依然无从判断。
        val signals = BodySignals(bloodOxygen = 98)
        assertEquals(0, engine.signalCount(signals))
    }

    // ------------------------------------------------------------------
    // 「按 N/4 个信号算的」这句如实说明
    // ------------------------------------------------------------------

    @Test
    fun coverageNoteSpellsOutPartialCount() {
        val w = weather(BodySignals(steps = 8000))
        val coverage = w.broadcast.coverage
        assertNotNull(coverage)
        // 数字各语言一致：按 1/4 个信号算的，缺 3 个。
        assertTrue(coverage!!.contains("1/4"))
    }

    @Test
    fun noCoverageNoteWhenAllFourSignalsPresent() {
        val signals = BodySignals(
            hrv = 50, restingHeartRate = 60, stressLevel = 20,
            steps = 8000, activityMinutes = 30, sleepMinutes = 420
        )
        assertEquals(4, engine.signalCount(signals))
        assertNull(weather(signals).broadcast.coverage)
    }

    @Test
    fun noCoverageNoteWhenCalibrated() {
        // 手动校准是最高优先级：结论按用户说的算，不按信号数算，也不加这句。
        val w = weather(BodySignals(steps = 8000), calibrated = setOf(CalibrationTag.TIRED))
        assertEquals(WeatherKind.SENSITIVE, w.kind)
        assertNull(w.broadcast.coverage)
    }

    // ------------------------------------------------------------------
    // 四信号全空才走老路径（敏感天 / 默认天气）
    // ------------------------------------------------------------------

    @Test
    fun emptySignalsUseDefaultKindWhenGiven() {
        val w = weather(BodySignals(), defaultKind = WeatherKind.BALANCED)
        assertEquals(WeatherKind.BALANCED, w.kind)
    }

    @Test
    fun emptySignalsWithoutDefaultAreSensitive() {
        val w = weather(BodySignals())
        assertEquals(WeatherKind.SENSITIVE, w.kind)
    }

    @Test
    fun anySignalIgnoresDefaultKind() {
        // 有一路信号就交回真实数据定档：默认天气不许顶掉它。
        val w = weather(BodySignals(steps = 8000, activityMinutes = 30), defaultKind = WeatherKind.RECOVERING)
        assertEquals(WeatherKind.ENERGY_HIGH, w.kind)
    }

    @Test
    fun partialLowRecoveryStillClassifiesConservatively() {
        // 只有恢复一路且很差 → 恢复天（保守优先）。
        val w = weather(BodySignals(hrv = 20, restingHeartRate = 80))
        assertEquals(WeatherKind.RECOVERING, w.kind)
    }

    // ------------------------------------------------------------------
    // 铁律底线
    // ------------------------------------------------------------------

    @Test
    fun metricsNeverInventNumbersForMissingSignals() {
        val w = weather(BodySignals(steps = 100))
        // 缺失的那三路：uncertain=true，词是「暂时没读到」，不是任何档位词。
        listOf(
            WeatherBodyWeatherKeys.RECOVERY,
            WeatherBodyWeatherKeys.PRESSURE,
            WeatherBodyWeatherKeys.SLEEP
        ).forEach { key ->
            assertTrue(w.uncertainOf(key))
            assertEquals("暂时没读到", w.levelOf(key))
        }
    }

    @Test
    fun calibrationOutranksAllSignals() {
        val w = weather(
            BodySignals(hrv = 60, restingHeartRate = 55, steps = 9000, activityMinutes = 40, sleepMinutes = 450),
            calibrated = setOf(CalibrationTag.UNSPEAKABLE)
        )
        assertEquals(WeatherKind.SENSITIVE, w.kind)
    }

    @Test
    fun demoFlagIsCarriedThrough() {
        val w = weather(BodySignals.demo())
        assertTrue(w.demo)
        assertEquals(SignalSource.DEMO, w.source)
    }
}
