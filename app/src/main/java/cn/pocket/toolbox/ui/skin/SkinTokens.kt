package cn.pocket.toolbox.ui.skin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import cn.pocket.toolbox.glass.GlassTheme

/**
 * 皮肤取值的**唯一门面**。
 *
 * 页面层（`ui/bodycast/` 下的各页、`ui/MainActivity.kt`、`ui/PrivacyPolicyActivity.kt`）
 * 一律读这里的属性，不再直接读 `GlassTheme`。属性名与 `GlassTheme` 同名，逐字转发，
 * 取值一个像素都没变。
 *
 * 22.31.0 起只有一套界面（液态玻璃）：此前的「谷歌 Material 3」皮肤连同
 * `ui/google/` 包与 [cn.pocket.toolbox.ui.skin.UiSkin] 一起删除，这里保留门面
 * 是为了让页面层的读点一个都不用动。
 *
 * 本文件里没有任何评分、排名、卡路里、体重的呈现取值，这是刻意的。
 */
object SkinTokens {

    // ---- 底色 ----
    val bg: Color get() = GlassTheme.bg
    val bgElevated: Color get() = GlassTheme.bgElevated

    // ---- 文字 ----
    val ink: Color get() = GlassTheme.ink
    val white: Color get() = GlassTheme.white
    val muted: Color get() = GlassTheme.muted
    val faint: Color get() = GlassTheme.faint
    val line: Color get() = GlassTheme.line

    // ---- 强调与数据色 ----
    val accent: Color get() = GlassTheme.accent
    val onAccent: Color get() = GlassTheme.onAccent

    val sage: Color get() = GlassTheme.sage
    val peach: Color get() = GlassTheme.peach
    val blue: Color get() = GlassTheme.blue
    val lavender: Color get() = GlassTheme.lavender

    val cWater: Color get() = GlassTheme.cWater
    val cSteps: Color get() = GlassTheme.cSteps
    val cSleep: Color get() = GlassTheme.cSleep
    val cHeart: Color get() = GlassTheme.cHeart
    val cMood: Color get() = GlassTheme.cMood
    val cWarning: Color get() = GlassTheme.cWarning

    // ---- 容器 ----
    val glassContainer: Color get() = GlassTheme.glassContainer
    val glassStrong: Color get() = GlassTheme.glassStrong
    val glassDivider: Color get() = GlassTheme.glassDivider
    val glassTrack: Color get() = GlassTheme.glassTrack
    val sheetBase: Color get() = GlassTheme.sheetBase
    val frostedBase: Color get() = GlassTheme.frostedBase

    // ---- 字号 ----
    val display: TextUnit get() = GlassTheme.display
    val pageTitle: TextUnit get() = GlassTheme.pageTitle
    val sectionTitle: TextUnit get() = GlassTheme.sectionTitle
    val cardTitle: TextUnit get() = GlassTheme.cardTitle
    val body: TextUnit get() = GlassTheme.body
    val caption: TextUnit get() = GlassTheme.caption
    val tabLabel: TextUnit get() = GlassTheme.tabLabel

    /** 当前字号档位的缩放系数（0.9 / 1.0 / 1.15 / 1.3）。 */
    val fontScale: Float get() = GlassTheme.fontScale

    // ---- 尺寸与间距 ----
    val pagePadding: Dp = GlassTheme.pagePadding
    val cardGap: Dp = GlassTheme.cardGap
    val minTouch: Dp = GlassTheme.minTouch

    /** 主容器圆角（「圆角风格」实验档位在这里生效）。 */
    val cardRadius: Dp get() = GlassTheme.cardRadius

    /** 次级行、指标与提示条的圆角。 */
    val secondaryRadius: Dp get() = GlassTheme.secondaryRadius

    val iconRadius: Dp get() = GlassTheme.iconRadius

    /** 底部栏高度。 */
    val bottomBarHeight: Dp get() = GlassTheme.bottomBarHeight

    val bottomBarBottomMargin: Dp get() = GlassTheme.bottomBarBottomMargin

    /** 可滚动内容底部预留的空白，由底栏高度派生（各页面自己不用算）。 */
    val contentBottomInset: Dp get() = GlassTheme.contentBottomInset

    /** 当前是否深色。 */
    val isDark: Boolean get() = GlassTheme.isDark
}
