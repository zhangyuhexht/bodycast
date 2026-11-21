/*
 * Copyright 2026 Kyant0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 来源：Kyant0/AndroidLiquidGlass（master）
 *       app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt
 * Apache License 2.0。
 *
 * 22.49.0：**整体回到官方实现**。
 *
 * 21.0.0 起这一块被反复改造（每次都是为了让某个局部更像苹果），到 22.48.0 已经
 * 积累了 12 处偏离，用户最终定调：不要再自己发明，直接照抄官方。
 * 这一版把函数体逐行改回官方版本，只保留两处**应用适配**与一处**总开关**：
 *
 *   适配 1：`isLightTheme = !GlassTheme.isDark`（官方读系统深浅，本应用读自己的档位）。
 *   适配 2：`accentColor = GlassTheme.accent`（官方是蓝，本应用是绿，跟随「强调色」档位）。
 *   开关 3：拖动结束的偏移回弹在「动画效果」关掉时走 snap()，否则用官方 spring。
 *
 * 官方结构（三块，别再动）：
 *   ① Row 64dp 底座：vibrancy + blur(8dp) + lens(24dp, 24dp)，按压时整块
 *      按 16dp/宽度 等比放大（layerBlock）；
 *   ② Row 56dp 染色副本层：alpha=0、layerBackdrop(tabsBackdrop) 录一层，
 *      供滑块采样；它自己也带 vibrancy + blur + lens(24dp×按压)；
 *   ③ Box 滑块（宽 1/tabsCount）：采样「原始背景层 + 染色副本层」的合成，
 *      lens(10dp, 14dp) × 按压 + 色散，外圈 highlight / shadow / innerShadow
 *      都随按压浮现，layerBlock 里按速度做横向拉伸、纵向压缩。
 *      静止时 lens 为 0、表面只画一遍淡色（alpha = 1 - 按压），所以静止的
 *      滑块与底座是同一块玻璃、只是淡一点；拖动才变成跟手的透镜。
 */
package cn.pocket.toolbox.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import cn.pocket.toolbox.anim.CastMotion
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    // 适配 1 / 适配 2：主题与强调色跟应用走。
    val isLightTheme = !GlassTheme.isDark
    val accentColor = GlassTheme.accent
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    // 22.54.0：底栏也走「液态玻璃」开关。关掉（或安卓 13 以下）时用磨砂实底，
    // 布局、滑动动画、按压反馈全部保留，只是没有折射与色散。
    val liquid = GlassSupport.isLiquidGlassAvailable
    // 22.57.0 修根因：22.54.0 那版浅色写成了 Color(0xF0F6F3)——六位十六进制在 Compose 里
    // 等于 alpha=0x00，也就是**全透明**，于是「安卓 13 以下 + 浅色模式」时底板整个看不见。
    // 现在两种主题都用应用统一的磨砂实底色（浅色 0xD1FFFFFF / 深色 0xD1121A17），
    // 和页面里其他磨砂表面是同一档。
    val frostedPlate = GlassTheme.frostedBase
    val frostedIndicator =
        if (isLightTheme) Color(0x14000000) else Color(0x14FFFFFF)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember { mutableIntStateOf(selectedTabIndex()) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        // 开关 3：「动画效果」关掉时偏移直接归位，不弹。
                        offsetAnimation.animateTo(
                            0f,
                            if (CastMotion.enabled.value) spring(1f, 300f, 0.5f) else snap()
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        val plateModifier = if (liquid) {
            Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        // 22.49.0 偏离官方（唯一两处观感参数）：底板模糊 8dp → 1.5dp。
                        // 用户定调「静止时几乎没有磨砂，只微微一点点」；官方 8dp 在
                        // 深色底上糊成一团，连后面的边缘拉伸都被糊掉了。
                        blur(1.5f.dp.toPx())
                        // 22.49.0：底板边缘拉伸 24dp → 14dp（官方 24dp 是配 8dp 模糊的，
                        // 我们模糊只有 1.5dp，同样的 24dp 会显得夸张）。
                        // 22.51.0：用户看过实机后说 14dp 又偏小，回到 18dp——
                        // 介于官方与上一版之间；滑块（下面那个 Box）不动。
                        lens(18f.dp.toPx(), 18f.dp.toPx())
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
        } else {
            Modifier
                .clip(Capsule())
                .background(frostedPlate)
        }
        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .then(plateModifier)
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            }
        ) {
            // 磨砂路不录副本层：没有折射就没有采样源（这一层只服务液态玻璃）。
            if (!liquid) return@CompositionLocalProvider
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            // 22.49.0：副本层（滑块里看到的那一份）静止时留一点点
                            // 磨砂，按压/拖动过程中平滑淡出到零——苹果的动态模糊，
                            // 也是用户定的「滑块拖动时完全没有模糊」。
                            blur(2f.dp.toPx() * (1f - progress))
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(56f.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .then(
                    if (liquid) {
                        Modifier.drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        // 22.61.0 回退 22.60.0 的「静止也上底板透镜」：那会让透镜作用到滑块里
                        // 的**染色副本**（图标 + 文字）上——真机截图里「回声」被拉糊成一团绿。
                        // 官方的分工是对的：静止的滑块只做「同一块玻璃里颜色淡一点」，
                        // 边缘拉伸属于底板本身；按下/拖动才切成滑块自己的透镜与色散。
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(frostedIndicator)
                    }
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}
