package cn.pocket.toolbox.ui.bodycast

import cn.pocket.toolbox.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 朗读用的数字转写（22.53.0）。
 *
 * 锁住三件事：中文读法正确、时间样式不被动、其它语言原样返回。
 */
class SpeechTextTest {

    @Test
    fun chineseReadsNumbersAsWords() {
        assertEquals("一万二千步", SpeechText.forSpeech("12000步", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("四十八分钟", SpeechText.forSpeech("48分钟", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("六千零五十步", SpeechText.forSpeech("6050步", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("一百零五", SpeechText.forSpeech("105", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("二千零二十", SpeechText.forSpeech("2020", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("九百九十九", SpeechText.forSpeech("999", AppLanguage.SIMPLIFIED_CHINESE))
    }

    @Test
    fun timeLikeFragmentsStayUntouched() {
        // 「今天 08:31」这种时间戳不该被念成汉字数字。
        assertEquals("上次读取：今天 08:31", SpeechText.forSpeech("上次读取：今天 08:31", AppLanguage.SIMPLIFIED_CHINESE))
    }

    @Test
    fun otherLanguagesKeepArabicDigits() {
        assertEquals("12000 steps", SpeechText.forSpeech("12000 steps", AppLanguage.ENGLISH))
        assertEquals("48 Minuten", SpeechText.forSpeech("48 Minuten", AppLanguage.GERMAN))
        assertEquals("12000 gradus", SpeechText.forSpeech("12000 gradus", AppLanguage.LATIN))
    }

    @Test
    fun hugeNumbersAreLeftAlone() {
        // 超过 99999 原样留着：那多半不是读数，而是别的东西（年份、编码）。
        assertEquals("20261016", SpeechText.forSpeech("20261016", AppLanguage.SIMPLIFIED_CHINESE))
    }
}
