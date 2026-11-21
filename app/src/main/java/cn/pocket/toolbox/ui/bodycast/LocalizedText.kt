package cn.pocket.toolbox.ui.bodycast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.ui.skin.SkinTokens

/**
 * 是否在半屏浮层（底部面板）里（22.33.0）。
 *
 * 浮层底是 `sheetBase` 实底，比页面底深——同样的 muted 灰在浮层里
 * 明显发闷、看不清。设为 true 时 [Text] / [RawText] 把 muted / faint
 * 一律提到白色（ink）；页面正文不受影响（不在浮层里就是 false）。
 */
val LocalSheetTextBoost = compositionLocalOf { false }

@Composable
fun RawText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    val boost = LocalSheetTextBoost.current
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = sheetBoost(color, boost),
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines
    )
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null,
    style: TextStyle = TextStyle.Default
) {
    val boost = LocalSheetTextBoost.current
    androidx.compose.material3.Text(
        text = BodyCastText.text(text),
        modifier = modifier,
        color = sheetBoost(color, boost),
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * 浮层内的灰字提到白色（22.33.0）。
 *
 * 传进来的 muted / faint 在浮层里一律换成 ink；accent / ink 等其它色原样保留
 * （强调色、链接色不动，不然整屏只剩一种颜色反而更难扫读）。
 * 不在浮层里（[boost] = false）原样返回，页面一个像素不变。
 *
 * internal：[cn.pocket.toolbox.glass.GlassCard] 的行/标签/页脚也走同一套换色。
 */
internal fun sheetBoost(color: Color, boost: Boolean): Color =
    if (boost && (color == SkinTokens.muted || color == SkinTokens.faint)) SkinTokens.ink else color

