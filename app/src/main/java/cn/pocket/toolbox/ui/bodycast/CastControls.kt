package cn.pocket.toolbox.ui.bodycast

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.i18n.BodyCastText
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import cn.pocket.toolbox.glass.GlassCard
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.glass.GlassRow
import cn.pocket.toolbox.glass.GlassSurface
import cn.pocket.toolbox.glass.LiquidButton
import cn.pocket.toolbox.glass.LiquidToggle
import cn.pocket.toolbox.glass.glassSurface
import cn.pocket.toolbox.ui.skin.SkinTokens

/**
 * 身体气象台共用控件。
 *
 * 页面只负责拼装，不重复实现皮肤外观。所有可点元素一律走本文件里的控件：
 * 玻璃皮肤下它们都是玻璃控件（[GlassCard] / [LiquidButton] / …），按下有同样的
 * 鼓起 + 跟手弹性形变，不会出现「有的按钮按得动、有的没反应」。
 *
 *
 * **本文件里没有任何评分、排名、卡路里、体重的呈现控件，这是刻意的。**
 * 也没有「未达标」「差一点」这类句式的载体——提示只有 [CastBanner] 一种，
 * 而且它不接受红色。
 */

// ---------------------------------------------------------------------------
// 页面骨架
// ---------------------------------------------------------------------------

/**
 * 「界面正在滚动」的全局信号（22.27.0）。
 *
 * 回前台自动刷新读回来的结果要**等滚动停下来再写状态**——
 * 列表滑到一半整页重排是最容易被用户感觉到的卡顿。
 * 计数器而不是布尔：多个 [CastPage] 同时在组合里时，谁开始滚动谁 +1。
 */
object UiMotion {
    private val holding = androidx.compose.runtime.mutableIntStateOf(0)

    val busy: Boolean get() = holding.intValue > 0

    fun acquire() {
        holding.intValue += 1
    }

    fun release() {
        holding.intValue = (holding.intValue - 1).coerceAtLeast(0)
    }
}

/**
 * 页面骨架：可滚动列表 + 底部留白。
 *
 * 底部留出 [SkinTokens.contentBottomInset]，这样内容能从玻璃底栏底下穿过去。
 * 内容整体上移会让玻璃永远只盖在背景色上，折射就白做了。
 */
@Composable
fun CastPage(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    horizontalPadding: Dp = SkinTokens.pagePadding,
    topPadding: Dp = 6.dp,
    content: LazyListScope.() -> Unit
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 滚动状态进 [UiMotion]：回前台自动刷新会等它空下来再写读数（22.27.0）。
    LaunchedEffect(state) {
        var holding = false
        try {
            androidx.compose.runtime.snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
                if (scrolling && !holding) {
                    holding = true
                    UiMotion.acquire()
                } else if (!scrolling && holding) {
                    holding = false
                    UiMotion.release()
                }
            }
        } finally {
            // 页面被回收时不能把计数器留在 +1 上，否则自动刷新会一直等下去。
            if (holding) UiMotion.release()
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = topPadding,
            bottom = SkinTokens.contentBottomInset + navBottom
        ),
        verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap),
        content = content
    )
}

/** 页面大标题。左侧标题 + 可选副标题，右侧可放一个操作（例如「重新扫描」）。
 *  [label] 是谷歌皮肤头部的小标签（M3 overline）；玻璃皮肤不显示，玻璃观感零变化。 */
@Composable
fun CastPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    label: String? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = SkinTokens.pageTitle,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                color = SkinTokens.ink
            )
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = SkinTokens.body,
                    lineHeight = 26.sp,
                    color = SkinTokens.muted
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** 区块小标题。用于「快速校准」「环境建议」这类分组。 */
@Composable
fun CastSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    color = SkinTokens.muted
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** 分组标签（设置页的「数据源管理」这类）。 */
@Composable
fun CastGroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 14.dp, bottom = 2.dp),
        fontSize = SkinTokens.caption,
        fontWeight = FontWeight.SemiBold,
        color = SkinTokens.muted
    )
}

/** 极细分隔线，用于玻璃卡内部。 */
@Composable
fun CastDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SkinTokens.glassDivider)
    )
}

// ---------------------------------------------------------------------------
// 标签（可多选可取消）
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 小说明按钮（ⓘ）
// ---------------------------------------------------------------------------

/**
 * 小「ⓘ」按钮（22.49.0）：**页面上只留结论与按钮，解释收在这里**。
 *
 * 用户的原话是「解释加太多了，在旁边有个小 i，点一下才弹出来」。所以
 * 授权指导、数据相关的页面一律这样摆：一行标题 + 一个 ⓘ，点开才是完整说明
 * （复用底部玻璃浮层，和首页「四个信号」的说明同一个形态）。
 *
 * 传进来的 [lines] 就是原来排在页面上的那些提示，一条一行；空列表不显示按钮。
 */
@Composable
fun CastInfoDot(
    title: String,
    lines: List<String>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    if (lines.isEmpty()) return
    val overlay = LocalCastOverlay.current
    CastIconButton(
        icon = "info",
        label = null,
        onClick = {
            overlay.show(title = title) {
                lines.forEach { CastHint(it) }
            }
        },
        backdrop = backdrop,
        modifier = modifier,
        tint = SkinTokens.muted
    )
}

/**
 * 玻璃标签。用于「我现在的感觉是——」这类可多选可取消的标记。
 *
 * 选中态是实心强调色，未选中是浅玻璃。两个状态高度一致，点选时不跳动。
 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    accent: Color = SkinTokens.accent,
    enabled: Boolean = true,
    localizeLabel: Boolean = true
) {
    val shape: Shape = Capsule()
    Box(
        modifier
            .glassSurface(
                backdrop = backdrop,
                shape = shape,
                blurRadius = 6.dp,
                lensHorizontal = 9.dp,
                lensVertical = 16.dp,
                shadowAlpha = if (selected) 0.22f else 0.10f,
                frostedTint = if (selected) accent else SkinTokens.frostedBase,
                onDrawSurface = {
                    drawRect(if (selected) accent.copy(alpha = 0.92f) else SkinTokens.glassContainer)
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick
            )
            .semantics { this.selected = selected }
            .heightIn(min = 44.dp)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        val labelColor = when {
            !enabled -> SkinTokens.faint
            selected -> SkinTokens.onAccent
            else -> SkinTokens.ink
        }
        if (localizeLabel) {
            Text(
                text = label,
                fontSize = SkinTokens.body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = labelColor,
                maxLines = 1
            )
        } else {
            RawText(
                text = label,
                fontSize = SkinTokens.body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = labelColor,
                maxLines = 1
            )
        }
    }
}

/** 自动换行的标签组。 */
@Composable
fun <T> GlassChipFlow(
    items: List<T>,
    backdrop: Backdrop,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SkinTokens.accent,
    localizeLabels: Boolean = true
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            GlassChip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onToggle(item) },
                backdrop = backdrop,
                accent = accent,
                localizeLabel = localizeLabels
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 分段控制器
// ---------------------------------------------------------------------------

/** 玻璃分段控制器。选项等宽，选中项是实心强调色胶囊。 */
@Composable
fun GlassSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    accent: Color = SkinTokens.accent
) {
    val outer: Shape = Capsule()
    Row(
        modifier
            .fillMaxWidth()
            .glassSurface(
                backdrop = backdrop,
                shape = outer,
                blurRadius = 8.dp,
                lensHorizontal = 10.dp,
                lensVertical = 18.dp,
                onDrawSurface = { drawRect(SkinTokens.glassTrack) }
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val isOn = index == selectedIndex
            val itemShape: Shape = Capsule()
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .glassSurface(
                        backdrop = backdrop,
                        shape = itemShape,
                        blurRadius = 5.dp,
                        lensHorizontal = 7.dp,
                        lensVertical = 12.dp,
                        frostedTint = if (isOn) accent else SkinTokens.frostedBase,
                        onDrawSurface = {
                            if (isOn) drawRect(accent.copy(alpha = 0.92f))
                        }
                    )
                    .clip(itemShape)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    )
                    .semantics { selected = isOn }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = SkinTokens.body,
                    fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isOn) SkinTokens.onAccent else SkinTokens.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 按钮
// ---------------------------------------------------------------------------

/** 主按钮：实心强调色胶囊。用于「领取今日微行动」「开始 X 分钟微行动」。
 *  [loading] 是进行中态（谷歌皮肤换成 M3 转圈；玻璃皮肤保持原样，只靠调用点禁用来表达）。 */
@Composable
fun CastPrimaryButton(
    text: String,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        isInteractive = enabled,
        surfaceColor = if (enabled) SkinTokens.accent else SkinTokens.glassTrack,
        height = 58.dp
    ) {
        if (icon != null) {
            GlassIcon(icon, if (enabled) SkinTokens.onAccent else SkinTokens.faint, Modifier.size(21.dp))
        }
        Text(
            text = text,
            fontSize = SkinTokens.body,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) SkinTokens.onAccent else SkinTokens.faint
        )
    }
}

/** 次按钮：透明玻璃胶囊。用于「只听身体播报」「换一张处方」。 */
@Composable
fun CastSecondaryButton(
    text: String,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true
) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        isInteractive = enabled,
        surfaceColor = SkinTokens.glassContainer,
        height = 56.dp
    ) {
        if (icon != null) {
            GlassIcon(icon, if (enabled) SkinTokens.accent else SkinTokens.faint, Modifier.size(20.dp))
        }
        Text(
            text = text,
            fontSize = SkinTokens.body,
            fontWeight = FontWeight.Medium,
            color = if (enabled) SkinTokens.ink else SkinTokens.faint
        )
    }
}

/** 进行中指示：谷歌皮肤用 M3 的 [circular progress][androidx.compose.material3.CircularProgressIndicator]，
 *  玻璃皮肤什么都不画（玻璃侧保持「只显示文字」的现有观感）。 */
@Composable
fun CastBusyIndicator(modifier: Modifier = Modifier) {
}

/** 小尺寸玻璃图标按钮。例如右上角「重新扫描」。 */
@Composable
fun CastIconButton(
    icon: String,
    label: String?,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = SkinTokens.accent
) {
    val shape: Shape = Capsule()
    Row(
        modifier
            .glassSurface(
                backdrop = backdrop,
                shape = shape,
                blurRadius = 7.dp,
                lensHorizontal = 10.dp,
                lensVertical = 18.dp,
                shadowAlpha = 0.18f,
                onDrawSurface = { drawRect(SkinTokens.glassContainer) }
            )
            .clip(shape)
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = if (label == null) 12.dp else 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        GlassIcon(icon, tint, Modifier.size(19.dp))
        if (label != null) {
            Text(label, fontSize = SkinTokens.caption, fontWeight = FontWeight.Medium, color = SkinTokens.ink)
        }
    }
}

// ---------------------------------------------------------------------------
// 列表行（设置页大量使用）
// ---------------------------------------------------------------------------

/** 开关行。整行可点，右侧是 [LiquidToggle]。 */
@Composable
fun GlassToggleRow(
    backdrop: Backdrop,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    shape: Shape? = null
) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        shape = shape,
        trailing = {
            // selected 的 lambda 身份必须稳定：LiquidToggle 内部以它为
            // LaunchedEffect 的 key，每次重组都传新 lambda 会导致效果反复
            // 重启、把进行中的弹簧动画中途取消（表现为「开关卡一下、
            // 动画没了」）。只在值真的变化时才换新 lambda。
            val selectedState = remember(checked) { { checked } }
            LiquidToggle(
                selected = selectedState,
                onSelect = { onCheckedChange(it) },
                backdrop = backdrop
            )
        }
    )
}

/** 选择行。右侧显示当前值，点击弹出选项浮层。 */
@Composable
fun GlassSelectRow(
    backdrop: Backdrop,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    shape: Shape? = null
) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        subtitle = subtitle,
        value = value,
        chevron = true,
        modifier = modifier,
        onClick = onClick,
        shape = shape
    )
}

/** 时间行。右侧显示 HH:mm，点击弹出时间选择浮层。 */
@Composable
fun GlassTimeRow(
    backdrop: Backdrop,
    title: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    shape: Shape? = null
) {
    val overlay = LocalCastOverlay.current
    GlassRow(
        backdrop = backdrop,
        title = title,
        subtitle = subtitle,
        value = "%02d:%02d".format(hour, minute),
        chevron = true,
        modifier = modifier,
        onClick = { overlay.pickHourMinute(title, hour, minute, onChange) },
        shape = shape
    )
}

/** 纯操作行。 */
@Composable
fun GlassActionRow(
    backdrop: Backdrop,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconTint: Color = SkinTokens.accent,
    subtitle: String? = null,
    value: String? = null,
    chevron: Boolean = true,
    surface: Boolean = true,
    shape: Shape? = null
) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        subtitle = subtitle,
        value = value,
        icon = icon,
        iconTint = iconTint,
        chevron = chevron,
        modifier = modifier,
        onClick = onClick,
        surface = surface,
        shape = shape
    )
}

// ---------------------------------------------------------------------------
// 内容块
// ---------------------------------------------------------------------------

/** 步骤列表。处方步骤、微行动引导都用它。序号是小玻璃圆点。 */
@Composable
fun CastStepList(
    steps: List<String>,
    modifier: Modifier = Modifier,
    accent: Color = SkinTokens.accent
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = SkinTokens.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = step,
                    modifier = Modifier.weight(1f),
                    fontSize = SkinTokens.body,
                    lineHeight = 26.sp,
                    color = SkinTokens.ink
                )
            }
        }
    }
}

/**
 * 温和提示条。**不用红色**——这套界面里没有警告，只有说明。
 *
 * 默认用次级圆角（20dp）；放在页面最底部、紧贴底部导航栏的提示条
 * 应传 `shape = RoundedCornerShape(SkinTokens.cardRadius)`，与导航栏
 * 的 32dp 胶囊圆角对齐（22.5.0）。
 */
@Composable
fun CastBanner(
    text: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icon: String = "info",
    tint: Color = SkinTokens.muted,
    shape: Shape = RoundedCornerShape(SkinTokens.secondaryRadius)
) {
    Row(
        modifier
            .fillMaxWidth()
            .glassSurface(
                backdrop = backdrop,
                shape = shape,
                vibrancy = false,
                blurRadius = null,
                lensHorizontal = 8.dp,
                lensVertical = 14.dp,
                frostedTint = tint.copy(alpha = 0.28f),
                onDrawSurface = { drawRect(tint.copy(alpha = 0.10f)) }
            )
            .clip(shape)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        GlassIcon(icon, tint, Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
    }
}

/** 说明文字。统一小字灰色。 */
@Composable
fun CastHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        fontSize = SkinTokens.caption,
        lineHeight = 22.sp,
        color = SkinTokens.muted
    )
}

/** 统一的可点/不可点卡片外壳。 */
@Composable
fun CastCard(
    backdrop: Backdrop,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    tint: Color = SkinTokens.glassContainer,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick == null) {
        GlassSurface(
            backdrop = backdrop,
            modifier = modifier,
            tint = tint,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        GlassCard(
            backdrop = backdrop,
            onClick = onClick,
            modifier = modifier,
            tint = tint,
            contentPadding = contentPadding,
            content = content
        )
    }
}

/** 空状态玻璃面板。 */
@Composable
fun CastEmptyPanel(
    text: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    GlassSurface(backdrop = backdrop, modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp),
            fontSize = SkinTokens.caption,
            color = SkinTokens.muted,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// 浮层（底部玻璃面板）
// ---------------------------------------------------------------------------

/**
 * 浮层内容。
 *
 * @param title 面板标题
 * @param subtitle 可选副标题
 * @param tall 是否撑到 92% 屏高（回声面板、食谱详情这类长内容）
 * @param footer 固定在底部的操作区（确认框的按钮）
 */
class CastOverlaySpec(
    val title: String,
    val subtitle: String?,
    val tall: Boolean,
    val footer: (@Composable ColumnScope.() -> Unit)?,
    val content: @Composable ColumnScope.() -> Unit,
    // 22.12.0：居中对话框形态（官方 Dialog 的做法：大折射圆角面板 + 说明
    // + 两个胶囊按钮）。false = 原来的底部玻璃面板。
    val dialog: Boolean = false,
    // 22.13.0：对话框按钮文案改用调用方传入的 label（此前硬编码「确认/取消」）。
    val confirmLabel: String = "确认",
    val cancelLabel: String = "取消",
    // 22.48.0：对话框里的一条勾选框（「不再提示」这类）。null = 不画，
    // 现有调用方的观感一字不变。
    val checkboxLabel: String? = null
)

/**
 * 浮层控制器。
 *
 * 页面里用 `LocalCastOverlay.current.show(...)` 打开浮层。
 *
 * **为什么浮层放在应用根部而不是页面内部**：玻璃要采样背景层才有折射，
 * 而背景层是由 `layerBackdrop` 标记的内容层。浮层如果画在那一层里面，
 * 就会采样到自己。所以浮层统一由 [CastOverlayHost] 渲染在内容层之外。
 */
class CastOverlayController {

    private var current by mutableStateOf<CastOverlaySpec?>(null)

    val spec: CastOverlaySpec? get() = current

    /** 由 [CastOverlayHost] 在渲染时注入背景层。 */
    internal var backdropRef: Backdrop? = null

    internal fun requireBackdrop(): Backdrop = requireNotNull(backdropRef) {
        "CastOverlayHost 尚未绑定背景层，浮层必须在 CastOverlayHost 内部使用"
    }

    fun show(
        title: String,
        subtitle: String? = null,
        tall: Boolean = false,
        footer: (@Composable ColumnScope.() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        current = CastOverlaySpec(title, subtitle, tall, footer, content)
    }

    fun dismiss() {
        current = null
    }

    /**
     * 居中对话框（22.12.0）。参照官方 Dialog 的做法：屏幕中间一块大圆角
     * 液态玻璃面板（强折射 + 完整高光），标题 + 说明 + 取消/确认两个胶囊。
     * 用于「打开开发者模式」这类需要严肃确认的场合。
     */
    fun showDialog(
        title: String,
        message: String,
        confirmLabel: String = "确认",
        cancelLabel: String = "取消",
        onConfirm: () -> Unit
    ) {
        current = CastOverlaySpec(
            title = title,
            subtitle = message,
            tall = false,
            footer = null,
            content = {},
            dialog = true,
            confirmLabel = confirmLabel,
            cancelLabel = cancelLabel
        )
        pendingDialogAction = { onConfirm() }
        pendingDialogCancelAction = null
        dialogChecked = false
    }

    /**
     * 带勾选框的居中对话框（22.48.0）。
     *
     * 用在「要不要连运动数据」这类**反复问会烦人**的地方：勾上「不再提示」，
     * 两个按钮里的任意一个都会把勾选状态回调出来，由调用方决定怎么持久化。
     * 勾选态存在控制器上（[dialogChecked]），所以按钮回调读到的永远是点下去那一刻的值。
     */
    fun showDialogWithCheckbox(
        title: String,
        message: String,
        checkboxLabel: String,
        confirmLabel: String = "确认",
        cancelLabel: String = "取消",
        initiallyChecked: Boolean = false,
        onConfirm: (checked: Boolean) -> Unit,
        onCancel: ((checked: Boolean) -> Unit)? = null
    ) {
        current = CastOverlaySpec(
            title = title,
            subtitle = message,
            tall = false,
            footer = null,
            content = {},
            dialog = true,
            confirmLabel = confirmLabel,
            cancelLabel = cancelLabel,
            checkboxLabel = checkboxLabel
        )
        pendingDialogAction = onConfirm
        pendingDialogCancelAction = onCancel
        dialogChecked = initiallyChecked
    }

    /** 勾选框当前状态（仅 [showDialogWithCheckbox] 打开时有意义）。 */
    var dialogChecked by mutableStateOf(false)
        internal set

    /** 对话框「确认」按钮的回调；由宿主在对话框渲染时读取。 */
    internal var pendingDialogAction: ((Boolean) -> Unit)? = null
        private set

    /** 对话框「取消」按钮的回调（可选）。默认取消什么都不做，只是关掉。 */
    internal var pendingDialogCancelAction: ((Boolean) -> Unit)? = null
        private set

    /** 主机在关闭对话框时清理回调。 */
    internal fun consumeDialogAction(): ((Boolean) -> Unit)? {
        val action = pendingDialogAction
        pendingDialogAction = null
        return action
    }

    /** 主机在关闭对话框时清理取消回调。 */
    internal fun consumeDialogCancelAction(): ((Boolean) -> Unit)? {
        val action = pendingDialogCancelAction
        pendingDialogCancelAction = null
        return action
    }

    /** 二次确认。用于「删除所有数据」这类不可逆操作。 */
    fun confirm(
        title: String,
        message: String,
        confirmLabel: String = "确定",
        cancelLabel: String = "先不要",
        onConfirm: () -> Unit
    ) {
        show(
            title = title,
            subtitle = message,
            footer = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        CastSecondaryButton(
                            text = cancelLabel,
                            onClick = { dismiss() },
                            backdrop = requireBackdrop()
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        CastPrimaryButton(
                            text = confirmLabel,
                            onClick = {
                                dismiss()
                                onConfirm()
                            },
                            backdrop = requireBackdrop()
                        )
                    }
                }
            }
        ) {
            Spacer(Modifier.height(6.dp))
        }
    }

    /** 单选浮层。用于「敏感度」「主题」「字体大小」这类。 */
    fun pickOne(
        title: String,
        options: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit
    ) {
        show(title = title) {
            options.forEachIndexed { index, option ->
                GlassRow(
                    backdrop = requireBackdrop(),
                    title = option,
                    value = if (index == selectedIndex) "当前" else null,
                    chevron = false,
                    onClick = {
                        onSelect(index)
                        dismiss()
                    }
                )
            }
        }
    }

    /** 时间选择浮层。用于通知提醒时间。 */
    fun pickHourMinute(
        title: String,
        hour: Int,
        minute: Int,
        onPicked: (Int, Int) -> Unit
    ) {
        // 选择状态放在浮层外持有，这样 footer 的按钮读到的是最新值，
        // 而不是打开浮层那一刻的旧值。
        val holder = HourMinuteHolder(hour, minute)
        show(
            title = title,
            subtitle = "选一个你通常还没睡、但也不在忙的时间",
            footer = {
                CastPrimaryButton(
                    text = "就用这个时间",
                    onClick = {
                        onPicked(holder.hour, holder.minute)
                        dismiss()
                    },
                    backdrop = requireBackdrop()
                )
            }
        ) {
            HourMinutePicker(holder)
        }
    }
}

/** 小时 / 分钟的选择状态。 */
internal class HourMinuteHolder(hour: Int, minute: Int) {
    var hour by mutableStateOf(hour)
    var minute by mutableStateOf(minute)
}

/** 当前浮层控制器。由 `BodyCastApp` 通过 CompositionLocalProvider 提供。 */
val LocalCastOverlay = compositionLocalOf<CastOverlayController> {
    error("LocalCastOverlay 未提供，请检查 MainActivity 里的 CompositionLocalProvider")
}

/** 浮层宿主。放在内容层之外，由应用根部渲染。 */
@Composable
fun CastOverlayHost(
    backdrop: Backdrop,
    controller: CastOverlayController
) {
    controller.backdropRef = backdrop

    // 谷歌皮肤：换成 ModalBottomSheet / AlertDialog（见 ui/google/GoogleOverlay.kt）。
    // backdrop 仍然要绑上——浮层里的按钮会调 requireBackdrop() 拿它（谷歌分支不用，
    // 但那几个 footer lambda 是共享的）。

    AnimatedContent(
        targetState = controller.spec,
        contentKey = { it != null },
        transitionSpec = {
            // 22.12.0：对话框用淡入 + 缩放入场；底部面板保持原来的上滑。
            // 22.28.0 动画批：时长/曲线统一进 [CastMotion]——进场中档 220ms
            // （emphasized decelerate，快进缓落），退场快档 120ms（accelerate，
            // 起步即走）；「动画效果」关掉时全部 0ms 硬切。
            if (targetState?.dialog == true || initialState?.dialog == true) {
                (
                    fadeIn(CastMotion.enter(160)) + scaleIn(CastMotion.enter(CastMotion.MEDIUM), initialScale = 0.92f)
                    ).togetherWith(
                    fadeOut(CastMotion.exit()) + scaleOut(CastMotion.exit(160), targetScale = 0.95f)
                )
            } else {
                (
                    fadeIn(CastMotion.enter(160)) + slideInVertically(CastMotion.enter(CastMotion.MEDIUM)) { it / 7 }
                    ).togetherWith(
                    fadeOut(CastMotion.exit()) + slideOutVertically(CastMotion.exit(180)) { it / 9 }
                )
            }
        },
        label = "cast-overlay"
    ) { spec ->
        if (spec == null) {
            Box(Modifier.fillMaxSize())
        } else if (spec.dialog) {
            CastGlassDialog(
                spec = spec,
                backdrop = backdrop,
                controller = controller,
                onDismiss = { controller.dismiss() }
            )
        } else {
            CastOverlayPanel(spec = spec, backdrop = backdrop, onDismiss = { controller.dismiss() })
        }
    }
}

/**
 * 居中液态玻璃对话框（22.12.0）。
 *
 * 参照官方 Dialog 的画法：整屏压暗，中间一块大圆角玻璃面板——
 * 类官方参数（强折射 lens + 完整高光 + 半透明容器色），
 * 标题 + 说明 + 取消/确认两个同宽胶囊按钮。
 */
@Composable
private fun CastGlassDialog(
    spec: CastOverlaySpec,
    backdrop: Backdrop,
    controller: CastOverlayController,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val shape = RoundedCornerShape(40.dp)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 压暗整屏；点空白处 = 取消。
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = if (SkinTokens.isDark) 0.5f else 0.32f)
                )
                .clickable(interactionSource = null, indication = null) { onDismiss() }
        )

        Column(
            Modifier
                .padding(horizontal = 34.dp)
                .fillMaxWidth()
                .clip(shape)
                .glassSurface(
                    backdrop = backdrop,
                    shape = shape,
                    // 官方面板：亮一点 / 饱和一点 + 轻模糊 + 大折射（带纵深）。
                    blurRadius = 10.dp,
                    lensHorizontal = 24.dp,
                    lensVertical = 48.dp,
                    lensDepthEffect = true,
                    shadowAlpha = 0.42f,
                    onDrawSurface = { drawRect(SkinTokens.glassStrong) }
                )
                .clickable(interactionSource = null, indication = null) { /* 吞掉点击，防止点到遮罩 */ }
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = BodyCastText.text(spec.title),
                fontSize = SkinTokens.cardTitle,
                fontWeight = FontWeight.Bold,
                color = SkinTokens.ink,
                textAlign = TextAlign.Center
            )
            if (spec.subtitle != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = BodyCastText.text(spec.subtitle),
                    fontSize = SkinTokens.body,
                    lineHeight = 26.sp,
                    color = SkinTokens.muted
                )
            }
            // 22.48.0：可选勾选框（「不再提示」），勾选态由控制器持有。
            spec.checkboxLabel?.let { label ->
                Spacer(Modifier.height(14.dp))
                DialogCheckLine(
                    label = BodyCastText.text(label),
                    checked = controller.dialogChecked,
                    onCheckedChange = { controller.dialogChecked = it },
                    backdrop = backdrop
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    CastSecondaryButton(
                        text = BodyCastText.text(spec.cancelLabel),
                        onClick = {
                            val cancelAction = controller.consumeDialogCancelAction()
                            controller.consumeDialogAction()
                            val checked = controller.dialogChecked
                            onDismiss()
                            cancelAction?.invoke(checked)
                        },
                        backdrop = backdrop
                    )
                }
                Box(Modifier.weight(1f)) {
                    CastPrimaryButton(
                        text = BodyCastText.text(spec.confirmLabel),
                        onClick = {
                            val action = controller.consumeDialogAction()
                            controller.consumeDialogCancelAction()
                            val checked = controller.dialogChecked
                            onDismiss()
                            action?.invoke(checked)
                        },
                        backdrop = backdrop
                    )
                }
            }
        }
    }
}

/**
 * 对话框里的勾选行（22.48.0）。
 *
 * 圆角方框 + 一道勾，未选中是浅玻璃底、选中是实心强调色——和 [GlassChip] 同一套
 * 明暗关系，所以在一屏玻璃里不会显得像是外来的控件。整行都是点击区（≥44dp）。
 */
@Composable
private fun DialogCheckLine(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop
) {
    val boxShape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, role = Role.Checkbox) {
                onCheckedChange(!checked)
            }
            .semantics { this.toggleableState = if (checked) ToggleableState.On else ToggleableState.Off }
            .heightIn(min = 44.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(boxShape)
                .background(if (checked) SkinTokens.accent else SkinTokens.glassContainer)
                .border(1.dp, if (checked) SkinTokens.accent else SkinTokens.glassDivider, boxShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                GlassIcon(kind = "check", tint = SkinTokens.onAccent, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = SkinTokens.caption,
            color = SkinTokens.ink
        )
    }
}

@Composable
private fun CastOverlayPanel(
    spec: CastOverlaySpec,
    backdrop: Backdrop,
    onDismiss: () -> Unit
) {
    // 返回键/侧滑返回优先关掉浮层，而不是把整个应用退到桌面。
// BackHandler 只在这层组合时注册，浮层一关自动让位给下层。
    BackHandler { onDismiss() }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 22.5.0：下拉跟手关闭。面板位移 1:1 跟手指走，松手后要么弹回、
    // 要么顺着惯性滑出屏幕再真正关闭（所有底部浮层共用这一套）。
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var panelHeight by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = with(density) { 150.dp.toPx() }
    val overshootPx = with(density) { 60.dp.toPx() }

    fun settleOrDismiss() {
        scope.launch {
            if (dragOffset >= dismissThreshold) {
                // 跟手滑到底：继续把它送出屏幕，出屏后再真正关掉，
                // 这样 AnimatedContent 的退场动画不会把面板「拽回原位」闪一下。
                // 22.28.0：送出屏走退场曲线（accelerate），220ms 起步即走。
                animate(
                    initialValue = dragOffset,
                    targetValue = panelHeight + overshootPx,
                    animationSpec = CastMotion.exit(CastMotion.MEDIUM)
                ) { value, _ -> dragOffset = value }
                onDismiss()
            } else {
                animate(
                    initialValue = dragOffset,
                    targetValue = 0f,
                    animationSpec = CastMotion.settleSpec()
                ) { value, _ -> dragOffset = value }
            }
        }
    }

    val sheetDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = { settleOrDismiss() },
            onDragCancel = {
                scope.launch {
                    animate(
                        initialValue = dragOffset,
                        targetValue = 0f,
                        animationSpec = CastMotion.settleSpec()
                    ) { value, _ -> dragOffset = value }
                }
            }
        ) { _, dragAmount ->
            // 只允许往下拖（面板本来就在屏幕底边）；拖起来之后也允许往回推。
            // detectVerticalDragGestures 的 dragAmount 是标量（向下为正）。
            if (dragAmount > 0f || dragOffset > 0f) {
                dragOffset = (dragOffset + dragAmount).coerceIn(0f, panelHeight)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 遮罩：点空白处收起；下拉时跟着变淡，玻璃面板被「拉走」的感觉更真。
        val scrimAlpha =
            (if (SkinTokens.isDark) 0.42f else 0.26f) *
                    (1f - (dragOffset / panelHeight).coerceIn(0f, 1f))
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(interactionSource = null, indication = null) { onDismiss() }
        )

        val panelShape: Shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (spec.tall) {
                        Modifier.fillMaxHeight(0.92f)
                    } else {
                        Modifier.heightIn(max = 560.dp)
                    }
                )
                // 面板的实底。没有它，下层页面正文会直接透上来和浮层文字重叠——
                // 背景层里不一定有正文可供 blur 采样，所以这里必须自己给一块底色。
                .background(SkinTokens.sheetBase)
                .glassSurface(
                    backdrop = backdrop,
                    shape = panelShape,
                    blurRadius = 18.dp,
                    lensHorizontal = 20.dp,
                    lensVertical = 34.dp,
                    shadowAlpha = 0.45f,
                    // 降级模式下实底 sheetBase 已经在更下层垫好，这里不再叠色。
                    frostedTint = Color.Transparent,
                    onDrawSurface = { drawRect(SkinTokens.glassStrong) }
                )
                .clip(panelShape)
                .graphicsLayer { translationY = dragOffset }
                .onSizeChanged { panelHeight = it.height.toFloat() }
                .padding(bottom = navBottom)
        ) {
            // 抓手。整条连同周边都是下拉手势区：按住往下拖，面板跟手走。
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
                    .then(sheetDragModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(SkinTokens.faint.copy(alpha = 0.5f))
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .then(sheetDragModifier)
                    .padding(start = 22.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = spec.title,
                        fontSize = SkinTokens.cardTitle,
                        fontWeight = FontWeight.Bold,
                        color = SkinTokens.ink
                    )
                    if (spec.subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        // 副标题在浮层里也提到白色（22.33.0）。
                        CompositionLocalProvider(LocalSheetTextBoost provides true) {
                            Text(
                                text = spec.subtitle,
                                fontSize = SkinTokens.caption,
                                lineHeight = 22.sp,
                                color = SkinTokens.muted
                            )
                        }
                    }
                }
                CastIconButton(
                    icon = "next",
                    label = null,
                    onClick = onDismiss,
                    backdrop = backdrop,
                    tint = SkinTokens.muted
                )
            }

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 22.33.0：浮层里灰字发闷看不清——内容区统一把 muted/faint
                // 提到白色（见 LocalSheetTextBoost）。页面正文不受影响。
                CompositionLocalProvider(LocalSheetTextBoost provides true) {
                    spec.content(this@Column)
                }
            }

            val footer = spec.footer
            if (footer != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    footer(this)
                }
            } else {
                Spacer(Modifier.height(22.dp))
            }
        }
    }
}

/** 小时 / 分钟选择器。整点 + 5 分钟步进，列表不会太长。 */
@Composable
private fun HourMinutePicker(holder: HourMinuteHolder) {
    var tab by remember { mutableStateOf(0) }
    val backdrop = LocalCastOverlay.current.requireBackdrop()
    Column(Modifier.fillMaxWidth()) {
        GlassSegmented(
            options = listOf("小时", "分钟"),
            selectedIndex = tab,
            onSelect = { tab = it },
            backdrop = backdrop
        )
        Spacer(Modifier.height(12.dp))
        if (tab == 0) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0..23).forEach { h ->
                    GlassChip(
                        label = "%02d".format(h),
                        selected = h == holder.hour,
                        onClick = { holder.hour = h },
                        backdrop = backdrop
                    )
                }
            }
        } else {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0..55 step 5).forEach { m ->
                    GlassChip(
                        label = "%02d".format(m),
                        selected = m == holder.minute,
                        onClick = { holder.minute = m },
                        backdrop = backdrop
                    )
                }
            }
        }
    }
}

/** 便捷函数：在页面里拿到浮层控制器。 */
@Composable
fun rememberCastOverlay(): CastOverlayController = LocalCastOverlay.current

/** 把内容包进指定浮层控制器的作用域（预览与测试用）。 */
@Composable
fun ProvideCastOverlay(
    controller: CastOverlayController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalCastOverlay provides controller, content = content)
}
