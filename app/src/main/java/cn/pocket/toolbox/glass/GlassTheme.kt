package cn.pocket.toolbox.glass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计规范。
 *
 * **默认深色**：玻璃要在深底上才看得清折射，浅色底上玻璃几乎只是一层白雾，
 * 所以深色不是「顺便支持一下」，而是这套 UI 的默认形态。
 * 浅色作为可选项保留，由设置页的开关通过 [setDark] 切换。
 *
 * 色板围绕「健康」选定：深墨绿底 + 薄荷绿强调（默认档，可在「实验性功能」里换色），
 * 其余数据色各自区分度足够。
 * 字号整体偏大、点击区不小于 [minTouch]，保证单手也能准确点中。
 */
object GlassTheme {

    /** 一套配色。深浅两套结构完全相同，只是取值不同。 */
    private class Scheme(
        val bg: Color,
        val bgElevated: Color,
        val ink: Color,
        val white: Color,
        val muted: Color,
        val faint: Color,
        val line: Color,
        val onAccent: Color,
        val sage: Color,
        val peach: Color,
        val blue: Color,
        val lavender: Color,
        val cWater: Color,
        val cSteps: Color,
        val cSleep: Color,
        val cHeart: Color,
        val cMood: Color,
        val cWarning: Color,
        val glassContainer: Color,
        val glassStrong: Color,
        val glassDivider: Color,
        val glassTrack: Color,
        /**
         * 浮层面板的实底。
         *
         * 浮层必须能盖住下层页面正文——而 [glassStrong] 只有一两成不透明度，
         * 一旦背景层里没有正文可供模糊（例如内容层只剩环境渐变），
         * 面板就等于一块透明玻璃片，下面的字会直接透上来。
         * 所以浮层需要一块自己的、近乎不透明的底色。
         */
        val sheetBase: Color,
        /**
         * 安卓 13 以下磨砂降级模式的玻璃实底。
         *
         * 低版本没有折射着色器，官方库会把效果静默跳过、玻璃退化成透明片。
         * 降级模式不给玻璃采样背景的机会，直接垫一块近实底色，保住可读性。
         */
        val frostedBase: Color
    )

    private val Dark = Scheme(
        bg = Color(0xFF0A0F0D),
        bgElevated = Color(0xFF121A17),
        ink = Color(0xFFF1F5F3),
        white = Color(0xFFFFFFFF),
        muted = Color(0xFF93A19C),
        faint = Color(0xFF5F6C67),
        line = Color(0x1FFFFFFF),
        onAccent = Color(0xFF06231A),
        sage = Color(0xFF2C6E54),
        peach = Color(0xFFFF9F6B),
        blue = Color(0xFF56B8F0),
        lavender = Color(0xFF9B8CFF),
        cWater = Color(0xFF56B8F0),
        cSteps = Color(0xFF3ED598),
        cSleep = Color(0xFF9B8CFF),
        cHeart = Color(0xFFFF6B81),
        cMood = Color(0xFFFFC46B),
        cWarning = Color(0xFFFFAB5C),
        glassContainer = Color(0x1FFFFFFF),
        glassStrong = Color(0x2BFFFFFF),
        glassDivider = Color(0x1FFFFFFF),
        glassTrack = Color(0x24FFFFFF),
        sheetBase = Color(0xF20E1613),
        frostedBase = Color(0xD1121A17)
    )

    private val Light = Scheme(
        bg = Color(0xFFF3F7F5),
        bgElevated = Color(0xFFFFFFFF),
        ink = Color(0xFF14211C),
        white = Color(0xFFFFFFFF),
        muted = Color(0xFF6A7A74),
        faint = Color(0xFF9AA8A3),
        line = Color(0x14000000),
        onAccent = Color(0xFFFFFFFF),
        sage = Color(0xFF7FBFA4),
        peach = Color(0xFFE08D5A),
        blue = Color(0xFF2E8FCB),
        lavender = Color(0xFF6C5CE0),
        cWater = Color(0xFF2E8FCB),
        cSteps = Color(0xFF1FA97A),
        cSleep = Color(0xFF6C5CE0),
        cHeart = Color(0xFFE05068),
        cMood = Color(0xFFD98A28),
        cWarning = Color(0xFFD97A1E),
        // 22.59.0：浅色档的「条条框框」提清楚一档——
        // 用户反馈浅色下卡片边界与分隔线看不清（白底 + 白玻璃 + 8% 黑线）。
        // 卡片更实、描边更深、轨道更明显；深色档一个字没动。
        glassContainer = Color(0x8CFFFFFF),
        glassStrong = Color(0xA8FFFFFF),
        glassDivider = Color(0x24000000),
        glassTrack = Color(0x24000000),
        sheetBase = Color(0xF7FFFFFF),
        frostedBase = Color(0xE0FFFFFF)
    )

    // 用 snapshot state 承载当前配色，切换时整棵树自动重组。
    private var scheme by mutableStateOf(Dark)

    /** 当前是否深色。 */
    val isDark: Boolean get() = scheme === Dark

    /** 切换深浅色。设置页的「深色模式」开关调它。 */
    fun setDark(dark: Boolean) {
        val next = if (dark) Dark else Light
        if (scheme !== next) scheme = next
    }

    // ---- 底色 ----
    val bg: Color get() = scheme.bg
    val bgElevated: Color get() = scheme.bgElevated

    // ---- 文字 ----
    val ink: Color get() = scheme.ink
    val white: Color get() = scheme.white
    val muted: Color get() = scheme.muted
    val faint: Color get() = scheme.faint
    val line: Color get() = scheme.line

    // ---- 强调与数据色 ----

    /**
     * 「强调色」实验档位：0 绿（默认）/ 1 蓝 / 2 紫 / 3 橙。
     *
     * 和 [setFontScale] 一样，这里只持有偏好的一份静态快照——
     * [cn.pocket.toolbox.bodycast.BodyCastStore] 在偏好变化时调 [setAccentStyle] 推进来，
     * 不新增第二份可写状态。浅色主题用同一档位的深一档取值，
     * 保证白字/深字在各自底色上都够对比度。
     */
    private var accentStyleIndex by mutableStateOf(0)

    fun setAccentStyle(index: Int) {
        val next = index.coerceIn(0, 3)
        if (accentStyleIndex != next) accentStyleIndex = next
    }

    private val darkAccents = listOf(
        Color(0xFF3ED598), Color(0xFF56B8F0), Color(0xFF9B8CFF), Color(0xFFFFA76B)
    )
    private val darkAccentDeeps = listOf(
        Color(0xFF1F8A5E), Color(0xFF2B7FB5), Color(0xFF6B5BD6), Color(0xFFD97A3C)
    )
    private val lightAccents = listOf(
        Color(0xFF1FA97A), Color(0xFF2E8FCB), Color(0xFF6C5CE0), Color(0xFFE08D5A)
    )
    private val lightAccentDeeps = listOf(
        Color(0xFF14724F), Color(0xFF1F6A9B), Color(0xFF4B3EB0), Color(0xFFB26A3C)
    )

    val accent: Color
        get() = (if (isDark) darkAccents else lightAccents)[accentStyleIndex]

    /**
     * 供「主题色」色卡用：返回某个档位在当前深浅下的取值。
     * 开箱引导与设置页画色卡时调它，显示的就是真正会生效的那个颜色。
     */
    fun accentPreview(index: Int): Color =
        (if (isDark) darkAccents else lightAccents)[index.coerceIn(0, 3)]

    val accentDeep: Color
        get() = (if (isDark) darkAccentDeeps else lightAccentDeeps)[accentStyleIndex]

    /** 铺在 [accent] 底色上的文字色。深色主题下是近黑，浅色主题下是白。 */
    val onAccent: Color get() = scheme.onAccent

    /** 兼容旧命名 */
    val green: Color get() = accent
    val sage: Color get() = scheme.sage
    val peach: Color get() = scheme.peach
    val blue: Color get() = scheme.blue
    val lavender: Color get() = scheme.lavender

    val cWater: Color get() = scheme.cWater
    val cSteps: Color get() = scheme.cSteps
    val cSleep: Color get() = scheme.cSleep
    val cHeart: Color get() = scheme.cHeart
    val cMood: Color get() = scheme.cMood
    val cWarning: Color get() = scheme.cWarning

    // ---- 玻璃 ----
    val glassContainer: Color get() = scheme.glassContainer
    val glassStrong: Color get() = scheme.glassStrong
    val glassDivider: Color get() = scheme.glassDivider
    val glassTrack: Color get() = scheme.glassTrack

    /** 浮层面板的实底，见 [Scheme.sheetBase]。 */
    val sheetBase: Color get() = scheme.sheetBase

    /** 安卓 13 以下磨砂降级的玻璃实底，见 [Scheme.frostedBase]。 */
    val frostedBase: Color get() = scheme.frostedBase

    // ---- 字号（不随主题变化，但随设置里的「字体大小」整体缩放） ----
    // 22.0.0 起这个设置真的生效了：在此之前 fontScale 只是存了个偏好，
    // 代码里没有任何地方读它——等于一个空开关。
    private var fontScaleFactor by mutableStateOf(1f)

    /**
     * 设置页的「字体大小」档位：0 小 / 1 标准 / 2 大 / 3 特大。
     *
     * 只缩放字号，不动间距、圆角与触摸区——特大档按排版逐页看过，
     * 卡片不会被挤爆（指标块会自然换行）。
     */
    fun setFontScale(index: Int) {
        val next = when (index) {
            0 -> 0.9f
            2 -> 1.15f
            3 -> 1.3f
            else -> 1f
        }
        if (fontScaleFactor != next) fontScaleFactor = next
    }

    /**
     * 当前字号档位的缩放系数（0.9 / 1.0 / 1.15 / 1.3）。
     *
     * 只读出口，给谷歌皮肤的 Typography 用（那边每一档字号都要乘同一个系数），
     * 保证两套皮肤的「字体大小」档位是同一个来源。
     */
    val fontScale: Float get() = fontScaleFactor

    val display: TextUnit get() = (44f * fontScaleFactor).sp
    val pageTitle: TextUnit get() = (28f * fontScaleFactor).sp
    val sectionTitle: TextUnit get() = (21f * fontScaleFactor).sp
    val cardTitle: TextUnit get() = (20f * fontScaleFactor).sp
    val body: TextUnit get() = (17f * fontScaleFactor).sp
    val caption: TextUnit get() = (15f * fontScaleFactor).sp
    val tabLabel: TextUnit get() = (13f * fontScaleFactor).sp

    // ---- 尺寸与间距 ----
    val pagePadding: Dp = 20.dp

    /** 卡片之间的间距。8.0.0 从 14 收到 10：一屏能多看两组内容。 */
    val cardGap: Dp = 10.dp

    /** 最小点击区 */
    val minTouch: Dp = 56.dp

    /**
     * 主容器圆角。32dp 与底部导航栏胶囊（64dp 高 = 32dp 半径）完全对齐，
     * 卡片从底栏下滚过时视觉连续，不会出现「框体比底栏更尖」的错位感。
     *
     * 「圆角风格」实验档位（0 标准 / 1 更圆 / 2 更方）只改这一个值；
     * [secondaryRadius] 与 [contentBottomInset] 等派生值保持不动，
     * 免得连锁改动把整套间距打破。
     */
    private var cornerStyleIndex by mutableStateOf(0)

    fun setCornerStyle(index: Int) {
        val next = index.coerceIn(0, 2)
        if (cornerStyleIndex != next) cornerStyleIndex = next
    }

    val cardRadius: Dp
        get() = when (cornerStyleIndex) {
            1 -> 40.dp
            2 -> 24.dp
            else -> 32.dp
        }

    // ---- 玻璃强度（实验档位：0 标准 / 1 更透 / 2 更磨砂） ----
    // 只影响 glassSurface（在 GlassSupport.kt 里）的 blur 半径与表面色浓度。
    private var glassStrengthIndex by mutableStateOf(0)

    fun setGlassStrength(index: Int) {
        val next = index.coerceIn(0, 2)
        if (glassStrengthIndex != next) glassStrengthIndex = next
    }

    /** 传给 [glassSurface] 的 blur 半径系数。 */
    val glassBlurScale: Float
        get() = when (glassStrengthIndex) {
            1 -> 0.5f
            2 -> 1.8f
            else -> 1f
        }

    /** [glassSurface] 表面色的 alpha 系数；乘完之后按上限 1 截断。 */
    val glassSurfaceAlphaScale: Float
        get() = when (glassStrengthIndex) {
            1 -> 0.7f
            2 -> 1.25f
            else -> 1f
        }

    /** 次级行、指标与提示条使用的圆角，避免与主卡片层级混淆。 */
    val secondaryRadius: Dp = 20.dp

    /**
     * 行首小图标色块的圆角。分组卡片内部的行图标用它——
     * 小方块自己在做玻璃折射时会和大卡片外框打架，统一成平涂色块。
     */
    val iconRadius: Dp = 12.dp

    val iconSize: Dp = 30.dp
    val cardIconSize: Dp = 34.dp

    /** 底部栏高度 */
    val bottomBarHeight: Dp = 68.dp

    /** 底部栏在屏幕底部留出的安全距离 */
    val bottomBarBottomMargin: Dp = 12.dp

    /**
     * 可滚动内容底部需要预留的空白。
     *
     * 内容要能从玻璃栏底下穿过去，所以滚动容器不能整体上移，
     * 只能靠这段 padding 保证最后一项不被挡住。
     */
    val contentBottomInset: Dp = bottomBarHeight + bottomBarBottomMargin + 24.dp
}
