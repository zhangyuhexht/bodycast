package cn.pocket.toolbox.ui.bodycast

import android.speech.tts.TextToSpeech
import cn.pocket.toolbox.i18n.AppLanguage
import java.util.Locale

/**
 * 语音播报的语言选择（22.49.0）。
 *
 * 以前这里只做一件事：把应用语言直接当 Locale 丢给引擎，
 * `isLanguageAvailable(...) >= LANG_AVAILABLE` 不成立就把入口整行藏起来。
 * 真机反馈是「语音播报用不了」——藏起来之后用户既看不到原因、也无从下手。
 *
 * 现在的口径：
 * 1. **按候选表依次试**。应用语言的 tag 是「zh-Hans」这种带脚本的形式，
 *    而语音引擎认的是地区码（zh_CN / zh_TW），直接拿 tag 去问经常被判成
 *    不支持——所以 zh-Hans → [zh_CN, zh, …] 这样逐个问，谁可用就用谁。
 * 2. **拉丁文没有语音**（没有任何引擎有 `la`）：退回英语，用英语音色念拉丁文，
 *    这是这一套六语体系里唯一说得通的兜底。
 * 3. 全都不行时返回 null，由界面**如实说明**并给一个「打开系统语音设置」的入口，
 *    而不是把整行藏掉。
 */
object Speech {

    /** 某种界面语言下，按优先级排好的候选音色。 */
    fun candidates(language: AppLanguage): List<Locale> = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> listOf(
            Locale.SIMPLIFIED_CHINESE,   // zh_CN
            Locale("zh", "CN"),
            Locale("zh")
        )
        AppLanguage.TRADITIONAL_CHINESE -> listOf(
            Locale.TRADITIONAL_CHINESE,  // zh_TW
            Locale("zh", "TW"),
            Locale("zh")
        )
        AppLanguage.ENGLISH -> listOf(Locale.US, Locale.ENGLISH)
        AppLanguage.SPANISH -> listOf(Locale("es", "ES"), Locale("es"))
        AppLanguage.GERMAN -> listOf(Locale.GERMANY, Locale.GERMAN)
        AppLanguage.JAPANESE -> listOf(Locale.JAPAN, Locale.JAPANESE)
        // 拉丁文没有音色，退回英语（脚本里最接近的读法）。
        AppLanguage.LATIN -> listOf(Locale.US, Locale.ENGLISH)
    }

    /**
     * 从引擎里挑一个能用的音色并装上。返回挑中的 Locale；一个都用不了返回 null。
     */
    fun pick(engine: TextToSpeech, language: AppLanguage): Locale? {
        candidates(language).forEach { candidate ->
            val available = runCatching { engine.isLanguageAvailable(candidate) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (available >= TextToSpeech.LANG_AVAILABLE) {
                val result = runCatching { engine.setLanguage(candidate) }
                    .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (result >= TextToSpeech.LANG_AVAILABLE) return candidate
            }
        }
        return null
    }

    /** 系统「文字转语音」设置页。没装引擎时它会引导用户去装一个。 */
    const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"
}

/**
 * 念之前把阿拉伯数字转成中文读法（22.53.0）。
 *
 * 为什么需要：TTS 念「12000 步」时会按数字位读，听感是「一二〇〇〇」；
 * 中文里该念「一万二千」。转写只改**朗读用的那一份文本**，界面上的数字一个不动。
 *
 * 口径：
 * - 只处理 0–99999 的整数（读数、分钟数、步数都在这个量级），超过就原样留着；
 * - 只对简体中文与日语生效（日语的汉字数字与中文同源）；英/西/德/拉一律不转——
 *   那些语言本来就念阿拉伯数字；
 * - 逐段替换，不碰日期与时间格式（`HH:mm` 里的数字是横杠/冒号分隔的，照样转成
 *   「十四时三十分」这种读法反而是错的，所以**跳过带冒号的片段**）。
 */
object SpeechText {

    private val CH_DIGITS = "零一二三四五六七八九"

    /** 带冒号的（时间、比例、URL）不转。 */
    private val TIME_LIKE = Regex("\\d{1,2}:\\d{2}")

    fun forSpeech(text: String, language: AppLanguage = AppLanguage.current()): String {
        if (language != AppLanguage.SIMPLIFIED_CHINESE && language != AppLanguage.JAPANESE) return text
        // 占位符**不能用数字**：下面那条正则会把占位符里的序号也转成汉字（踩过一次）。
        val protected = ArrayList<String>()
        var working = TIME_LIKE.replace(text) { m ->
            protected += m.value
            // A、B、C… 当占位符，字母不会被 \d+ 匹配。
            "\u0001" + ('A'.code + protected.size - 1).toChar() + "\u0001"
        }
        working = Regex("\\d+").replace(working) { m ->
            val value = m.value.toIntOrNull()
            if (value == null || value > 99_999) m.value else chineseNumber(value)
        }
        protected.forEachIndexed { i, original ->
            val token = "\u0001" + ('A'.code + i).toChar() + "\u0001"
            working = working.replace(token, original)
        }
        return working
    }

    /** 0–99999 的中文读法；超出范围由调用方原样返回。 */
    internal fun chineseNumber(value: Int): String = when {
        value < 10 -> CH_DIGITS[value].toString()
        value < 20 -> "十" + if (value % 10 == 0) "" else CH_DIGITS[value % 10].toString()
        value < 100 -> {
            val tens = CH_DIGITS[value / 10] + "十"
            if (value % 10 == 0) tens else tens + CH_DIGITS[value % 10]
        }
        value < 1000 -> {
            val hundreds = CH_DIGITS[value / 100] + "百"
            val rest = value % 100
            when {
                rest == 0 -> hundreds
                rest < 10 -> hundreds + "零" + CH_DIGITS[rest]
                else -> hundreds + chineseNumber(rest)
            }
        }
        value < 10_000 -> {
            val thousands = CH_DIGITS[value / 1000] + "千"
            val rest = value % 1000
            when {
                rest == 0 -> thousands
                rest < 100 -> thousands + "零" + chineseNumber(rest)
                else -> thousands + chineseNumber(rest)
            }
        }
        else -> {
            val head = chineseNumber(value / 10_000) + "万"
            val rest = value % 10_000
            when {
                rest == 0 -> head
                rest < 1000 -> head + "零" + chineseNumber(rest)
                else -> head + chineseNumber(rest)
            }
        }
    }
}
