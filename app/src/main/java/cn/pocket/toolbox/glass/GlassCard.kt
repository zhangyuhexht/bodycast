package cn.pocket.toolbox.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.ui.bodycast.LocalSheetTextBoost
import cn.pocket.toolbox.ui.bodycast.sheetBoost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 玻璃卡片的弹性手感，写进 `drawBackdrop` 的 `layerBlock`。
 *
 * 按下时整块玻璃轻微鼓起，同时跟着手指位移；位移用 tanh 饱和，
 * 所以怎么拉都拉不出自身尺寸之外，松手后被 spring 弹回。
 *
 * **这段数学逐字来自官方 `LiquidButton` 的 `layerBlock`，不要改系数。**
 *
 * 之前这里用的是自己放大的版本（振幅 0.18 * 宽、tanh 参数除以 0.45），
 * 手指一动玻璃就瞬间跳到最大位移，观感是「轻微浮动」——用户明确否掉了。
 * 官方的 `initialDerivative = 0.05f` 饱和得很慢，小幅移动几乎不动，
 * 只有真正拖动才跟手，这才是要的手感。
 *
 * 只形变玻璃本身、不动内容：玻璃像一层液体透镜盖在内容上，
 * 既符合原版 Backdrop 的行为，也保证文字始终清晰可读。
 */
internal fun GraphicsLayerScope.applyElasticLayer(
    highlight: InteractiveHighlight
) {
    val width = size.width
    val height = size.height

    val progress = highlight.pressProgress
    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

    val maxOffset = size.minDimension
    val initialDerivative = 0.05f
    val offset = highlight.offset
    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

    val maxDragScale = 4f.dp.toPx() / size.height
    val offsetAngle = atan2(offset.y, offset.x)
    scaleX =
        scale +
                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                (width / height).fastCoerceAtMost(1f)
    scaleY =
        scale +
                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                (height / width).fastCoerceAtMost(1f)
}

/**
 * 可交互玻璃卡片。
 *
 * 整块卡片就是点击区，按下有鼓起 + 跟手弹性拖拽，松手回弹。
 * 需要「按得动、有反馈」的地方都用它，不要用裸 [clickable]——
 * 裸 clickable 只有涟漪没有玻璃形变，手感是平的。
 */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTheme.cardRadius),
    tint: Color = GlassTheme.glassContainer,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) { InteractiveHighlight(animationScope) }

    Column(
        modifier
            .fillMaxWidth()
            .glassSurface(
                backdrop = backdrop,
                shape = shape,
                blurRadius = 10.dp,
                lensHorizontal = 16.dp,
                lensVertical = 24.dp,
                shadowAlpha = 0.30f,
                layerBlock = if (enabled) {
                    { applyElasticLayer(highlight) }
                } else {
                    null
                },
                onDrawSurface = { drawRect(tint) }
            )
            .clip(shape)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(if (enabled) highlight.modifier else Modifier)
            .then(if (enabled) highlight.gestureModifier else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 纯展示玻璃面板。不响应点击，也没有按压形变。
 * 用于已经不可点的容器，例如统计块、说明块。
 */
@Composable
fun GlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTheme.cardRadius),
    tint: Color = GlassTheme.glassContainer,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .glassSurface(
                backdrop = backdrop,
                shape = shape,
                blurRadius = 10.dp,
                lensHorizontal = 14.dp,
                lensVertical = 22.dp,
                shadowAlpha = 0.25f,
                onDrawSurface = { drawRect(tint) }
            )
            // 22.33.0：补 clip——玻璃面画了圆角，但内容没跟着裁：
            // 行的按压高亮是直角矩形，会从圆角里「溢出」，看起来就是
            // R 角重叠。GlassCard 早有这一句，这里一直漏着。
            .clip(shape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 玻璃列表行。左图标 + 标题/副标题 + 右侧值或箭头，整行可点。
 * 设置页、记录页大量使用，所以统一在这里实现一次。
 */
@Composable
fun GlassRow(
    backdrop: Backdrop,
    title: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconTint: Color = GlassTheme.accent,
    subtitle: String? = null,
    value: String? = null,
    chevron: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    surface: Boolean = true,
    shape: Shape? = null
) {
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    val shape = shape ?: RoundedCornerShape(GlassTheme.secondaryRadius)
    val interactive = onClick != null

    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (surface) {
                    Modifier
                        .glassSurface(
                            backdrop = backdrop,
                            shape = shape,
                            blurRadius = 8.dp,
                            lensHorizontal = 12.dp,
                            lensVertical = 20.dp,
                            layerBlock = if (interactive) {
                                { applyElasticLayer(highlight) }
                            } else {
                                null
                            },
                            onDrawSurface = { drawRect(GlassTheme.glassContainer) }
                        )
                        .clip(shape)
                } else {
                    Modifier
                }
            )
            .then(
                if (interactive) {
                    // onClick 是 val 参数，且 interactive 就是「onClick != null」，
                    // 编译器能把它智能转换掉，不需要 !!。
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .then(if (interactive) highlight.modifier else Modifier)
            .then(if (interactive) highlight.gestureModifier else Modifier)
            // 22.43.0：块高回到「标题 1 行 + 副标题 2 行」的原大小（22.42.0 曾把
            // 行撑大到 111dp，块变得很高）。两行内容的行**居中放在三行块里**——
            // 居中做在下面的文字盒上，不是靠撑大外框。
            .heightIn(min = GlassTheme.minTouch)
            .padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            // 22.7.0：行首图标色块的圆角随外框同步——外框圆角达到
            // cardRadius（32dp）时，图标用 32-14=18dp；外框圆角更小的
            // 独立行维持 13dp（已经比几何值更圆，观感与外框同一套曲率语言）。
            val density = LocalDensity.current
            val frameCorner = (shape as? RoundedCornerShape)?.topStart
            val frameRadius = frameCorner?.toPx(Size.Zero, density)
            val roundedFrame = frameRadius != null &&
                frameRadius >= with(density) { GlassTheme.cardRadius.toPx() }
            val iconShape = RoundedCornerShape(
                if (surface) {
                    if (roundedFrame) GlassTheme.cardRadius - 14.dp else 13.dp
                } else {
                    GlassTheme.cardRadius - 14.dp
                }
            )
            Box(
                Modifier
                    .size(if (surface) 40.dp else 38.dp)
                    .clip(iconShape)
                    .then(
                        if (surface) {
                            Modifier.glassSurface(
                                backdrop = backdrop,
                                shape = iconShape,
                                vibrancy = false,
                                blurRadius = null,
                                lensHorizontal = 8.dp,
                                lensVertical = 12.dp,
                                frostedTint = iconTint.copy(alpha = 0.28f),
                                onDrawSurface = { drawRect(iconTint.copy(alpha = 0.18f)) }
                            )
                        } else {
                            // 分组卡片内部：图标不再各自做一层玻璃，
                            // 否则小圆角方块的折射/高光会和大卡片外框打架。
                            // 改成同一色系的平涂小色块，圆角与卡片体系一致。
                            Modifier.background(iconTint.copy(alpha = 0.16f))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                GlassIcon(icon, iconTint, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
        }

        // 22.43.0：带副标题的行，文字盒固定「标题 1 行 + 副标题 2 行」高，
        // 标题 + 副标题按**真实行数**排、整块垂直居中——内容只有两行时
        // 两行落在三行块的中间（上下各半行空隙），块的大小不变，
        // 不是把块撑大，也不是让文字贴在三行的上两行。
        Box(
            Modifier
                .weight(1f)
                .heightIn(
                    min = if (subtitle != null) {
                        (GlassTheme.body.value * 1.2f + GlassTheme.caption.value * 2.4f).dp
                    } else {
                        0.dp
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = BodyCastText.text(title),
                    fontSize = GlassTheme.body,
                    fontWeight = FontWeight.Medium,
                    color = GlassTheme.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = BodyCastText.text(subtitle),
                        fontSize = GlassTheme.caption,
                        color = sheetBoost(GlassTheme.muted, LocalSheetTextBoost.current),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = BodyCastText.text(value),
                fontSize = GlassTheme.body,
                fontWeight = FontWeight.SemiBold,
                color = sheetBoost(GlassTheme.muted, LocalSheetTextBoost.current)
            )
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }

        if (chevron) {
            Spacer(Modifier.width(10.dp))
            GlassIcon("next", GlassTheme.faint, Modifier.size(20.dp))
        }
    }
}

/** 区块小标题。 */
@Composable
fun GlassLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = BodyCastText.text(text),
        modifier = modifier,
        fontSize = GlassTheme.caption,
        fontWeight = FontWeight.SemiBold,
        color = sheetBoost(GlassTheme.muted, LocalSheetTextBoost.current)
    )
}

/** 页脚说明。统一小字灰色，用来交代隐私边界与限制。 */
@Composable
fun GlassNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = BodyCastText.text(text),
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        fontSize = GlassTheme.caption,
        lineHeight = 22.sp,
        color = sheetBoost(GlassTheme.muted, LocalSheetTextBoost.current)
    )
}

/** 空状态占位。 */
@Composable
fun GlassEmpty(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassIcon("leaf", GlassTheme.faint, Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(text = BodyCastText.text(text), fontSize = GlassTheme.caption, color = sheetBoost(GlassTheme.muted, LocalSheetTextBoost.current))
    }
}
