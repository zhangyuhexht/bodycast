package cn.pocket.toolbox.ui.bodycast

import cn.pocket.toolbox.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 语音音色候选表（22.49.0）。
 *
 * 锁住两条：带脚本的语言标签要还原成引擎认的地区码（zh-Hans → zh_CN），
 * 拉丁文没有音色、退回英语。这两条是「语音播报用不了」的根因所在。
 */
class SpeechTest {

    @Test
    fun simplifiedChineseAsksForTheRegionCodeFirst() {
        val first = Speech.candidates(AppLanguage.SIMPLIFIED_CHINESE).first()
        assertEquals("zh", first.language)
        assertEquals("CN", first.country)
    }

    @Test
    fun traditionalChineseAsksForTaiwanFirst() {
        val first = Speech.candidates(AppLanguage.TRADITIONAL_CHINESE).first()
        assertEquals("zh", first.language)
        assertEquals("TW", first.country)
    }

    @Test
    fun latinFallsBackToEnglishBecauseNoEngineHasLatin() {
        val candidates = Speech.candidates(AppLanguage.LATIN)
        assertTrue(candidates.isNotEmpty())
        assertEquals(Locale.US, candidates.first())
    }

    @Test
    fun everyLanguageHasAtLeastOneCandidate() {
        AppLanguage.entries.forEach { language ->
            assertTrue("no candidates for $language", Speech.candidates(language).isNotEmpty())
        }
    }
}
