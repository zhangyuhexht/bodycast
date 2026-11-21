package cn.pocket.toolbox.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `BodyCastText.select()` 的兜底链（22.27.0）。
 *
 * 契约：**缺译回退英语**（ja / la 槽位空 → en），绝不能把日语/拉丁界面
 * 漏成中文或空白；中文界面永远用源串。运行期拼接的模板走 `patterns()`，
 * 数字与占位符在各语言里必须原样保留。
 */
class BodyCastTextFallbackTest {

    private val full = "重试" // 四槽全填
    private val jaGap = "CastOverlayHost 尚未绑定背景层，浮层必须在 CastOverlayHost 内部使用" // ja 空、en 有

    @Test
    fun chineseUsesSourceText() {
        assertEquals(full, BodyCastText.text(full, AppLanguage.SIMPLIFIED_CHINESE))
    }

    @Test
    fun japaneseSlotIsUsedWhenPresent() {
        val out = BodyCastText.text(full, AppLanguage.JAPANESE)
        assertEquals("再試行", out)
    }

    @Test
    fun missingJapaneseFallsBackToEnglishNotChinese() {
        val out = BodyCastText.text(jaGap, AppLanguage.JAPANESE)
        assertEquals("CastOverlayHost has no backdrop bound; overlays must be used inside CastOverlayHost", out)
        // 兜底不能漏出中文源串。
        assertFalse(out.contains("尚未绑定"))
    }

    @Test
    fun missingLatinFallsBackToEnglish() {
        // 「日志开关」la 槽是空的：拉丁界面应显示英语，不是中文。
        val out = BodyCastText.text("日志开关", AppLanguage.LATIN)
        assertEquals("Logging", out)
    }

    @Test
    fun coverageTemplateKeepsNumbersInEveryLanguage() {
        val source = "今天是按 2/4 个信号算的，缺的那 2 个显示「暂时没读到」。"
        assertEquals(source, BodyCastText.text(source, AppLanguage.SIMPLIFIED_CHINESE))
        val en = BodyCastText.text(source, AppLanguage.ENGLISH)
        assertTrue(en, en.contains("2 of the 4") && en.contains("the other 2"))
        val ja = BodyCastText.text(source, AppLanguage.JAPANESE)
        assertTrue(ja, ja.contains("2 つ") && ja.contains("4 つ"))
        val la = BodyCastText.text(source, AppLanguage.LATIN)
        assertTrue(la, la.contains("2") && la.contains("4"))
    }

    @Test
    fun lastReadTemplateTranslatesTimestampSlot() {
        val source = "上次读取：今天 08:12"
        val en = BodyCastText.text(source, AppLanguage.ENGLISH)
        assertEquals("Last read: Today 08:12", en)
    }

    @Test
    fun miCloudPipelineLineTranslatesAllSlots() {
        val source = "小米账号（云同步）· 上次同步 昨天 20:30 · 六类里读到 3 类"
        val en = BodyCastText.text(source, AppLanguage.ENGLISH)
        assertEquals("Mi account (cloud sync) · last synced Yesterday 20:30 · 3 of 6 types read", en)
    }

    @Test
    fun loginPageStringsAreRegisteredInAllLanguages() {
        // 登录页整页文案的四语登记（防再漏）：缺一槽就回退英语并记审计缺口。
        TranslationAudit.clear()
        val page = listOf(
            "连接小米账号",
            "在你的小米官方页面里登录。",
            "登录在小米自己的页面上完成",
            "登录完成，正在取回会话…",
            "打不开小米登录页",
            "可能是网络不通，或者页面被拦住了。检查网络之后点「重试」。",
            "重试",
            "正在重新打开…",
            "我已经登录完了",
            "正在确认登录结果…",
            "登录只在小米的页面上完成；BodyCast 不会看到你的密码，也不会把它存下来。"
        )
        listOf(AppLanguage.ENGLISH, AppLanguage.JAPANESE, AppLanguage.LATIN).forEach { lang ->
            page.forEach { source ->
                val out = BodyCastText.text(source, lang)
                // 必须真的走了译文槽：不许原样返回中文源串。
                assertFalse("$lang returned source for: $source", out == source)
                // 英/拉产出里不许出现汉字（日语本来就用汉字，另判）。
                if (lang != AppLanguage.JAPANESE) {
                    assertFalse("$lang leaked Chinese for: $source -> $out", containsChinese(out))
                }
            }
        }
        assertEquals(emptySet<String>(), TranslationAudit.sheetGapSnapshot())
        assertEquals(emptySet<String>(), TranslationAudit.fallbackSnapshot())
    }

    @Test
    fun halfTranslatedTemplateIsNotEmitted() {
        // 「今天是(.+)。」这个笼统模板不能吞掉没登记的拼接句：
        // 槽位里的中文没翻走时，宁可整句回退，也不翻一半混排
        //（登录页「中文说明卡 + 日语错误卡」就是这类混排的成因，22.27.0 修）。
        val composed = "今天是暂时没读到的一句拼接。"
        listOf(AppLanguage.ENGLISH, AppLanguage.JAPANESE, AppLanguage.LATIN).forEach { lang ->
            val out = BodyCastText.text(composed, lang)
            // 不许出现「外壳翻译 + 中文槽位」的混排句。
            assertFalse("$lang mixed: $out", out.contains("Today is a ") || out.contains("今日は"))
            assertTrue("$lang lost source: $out", out.contains("暂时没读到"))
        }
    }

    @Test
    fun templateStillTranslatesWhenSlotIsRegistered() {
        // 槽位是登记过的词条时模板照常工作（不许把好句也拒掉）。
        val out = BodyCastText.text("今天是平衡天。", AppLanguage.ENGLISH)
        assertTrue(out, out.startsWith("Today is a "))
        assertFalse(out, containsChinese(out))
    }

    private fun containsChinese(s: String): Boolean = s.any { it.code in 0x4E00..0x9FFF }
}
