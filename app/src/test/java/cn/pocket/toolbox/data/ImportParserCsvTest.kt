package cn.pocket.toolbox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 文件导入的表头映射（22.27.0 补的回归防线）。
 *
 * 只按表头认列、绝不按列序猜：表头不认识就留 null 并如实列回界面。
 * 空值的各种写法都算「没读到」，不是 0（null ≠ 0）。
 */
class ImportParserCsvTest {

    /** 数据行的日期必须是「今天 / 昨夜」才会被采信（ImportParser 只认近一天），
     *  所以测试里的日期一律现算，写死日期会随日历过期（22.34.0 修的时间炸弹）。 */
    private val today: String get() = java.time.LocalDate.now().toString()

    @Test
    fun chineseHeadersMapToFields() {
        val csv = """
            日期,静息心率,心率变异性,睡眠时长,深睡占比,步数,血氧,运动时长
            $today,62,48,445,0.21,3480,97,26
        """.trimIndent()
        val r = ImportParser.parse(csv)!!
        assertEquals(7, r.recognized.size)
        assertEquals(62, r.values.restingHeartRate)
        assertEquals(48, r.values.hrv)
        assertEquals(445, r.values.sleepMinutes)
        assertEquals(3480, r.values.steps)
        assertEquals(97, r.values.bloodOxygen)
        assertEquals(26, r.values.activityMinutes)
    }

    @Test
    fun englishAliasesAreCaseAndSpaceInsensitive() {
        val csv = """
            Date, Resting Heart Rate , HRV,Sleep,DeepSleep,Step Count,Spo2,Active Minutes
            $today,55,60,400,0.25,12000,98,45
        """.trimIndent()
        val r = ImportParser.parse(csv)!!
        assertEquals(55, r.values.restingHeartRate)
        assertEquals(60, r.values.hrv)
        assertEquals(400, r.values.sleepMinutes)
        assertEquals(12000, r.values.steps)
        assertEquals(98, r.values.bloodOxygen)
        assertEquals(45, r.values.activityMinutes)
    }

    @Test
    fun unknownColumnsAreListedAsIgnoredNotGuessed() {
        val csv = """
            步数,微信步数排名,天气
            1000,1,晴
        """.trimIndent()
        val r = ImportParser.parse(csv)!!
        assertEquals(1, r.recognized.size)
        assertEquals(1000, r.values.steps)
        assertTrue(r.ignoredHeaders.contains("微信步数排名"))
        assertTrue(r.ignoredHeaders.contains("天气"))
        // 不认识的列绝不猜成读数。
        assertNull(r.values.restingHeartRate)
    }

    @Test
    fun blankSpellingsMeanMissingNotZero() {
        val csv = """
            步数,静息心率,血氧
            -,--,未知
        """.trimIndent()
        val r = ImportParser.parse(csv)!!
        assertNull(r.values.steps)
        assertNull(r.values.restingHeartRate)
        assertNull(r.values.bloodOxygen)
    }

    @Test
    fun stressColumnRecognizedByAllAliases() {
        listOf("压力", "压力值", "Stress Level").forEach { header ->
            val r = ImportParser.parse("$header\n40\n")!!
            assertEquals("header=$header", 40, r.values.stressLevel)
        }
    }

    @Test
    fun headerRowIsFoundBelowPreambleLines() {
        val csv = """
            小米运动健康导出
            生成时间：$today

            日期,步数
            $today,4321
        """.trimIndent()
        val r = ImportParser.parse(csv)!!
        assertEquals(4321, r.values.steps)
    }

    @Test
    fun blankTextReturnsNull() {
        assertNull(ImportParser.parse(null))
        assertNull(ImportParser.parse("   "))
    }

    @Test
    fun recognizedHeaderWithoutRowsReportsHeadersAndNoValues() {
        val csv = "步数,静息心率\n"
        val r = ImportParser.parse(csv)!!
        // 表头认出来了，但没有数据行：值一律 null，不猜、不填 0。
        assertNull(r.values.steps)
        assertNull(r.values.restingHeartRate)
        assertTrue(r.allHeaders.containsAll(listOf("步数", "静息心率")))
    }
}
