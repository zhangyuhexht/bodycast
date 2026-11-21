package cn.pocket.toolbox.ui.bodycast

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.ui.skin.SkinTokens

/**
 * 倒计时呼吸圆环。
 *
 * 两件事同时发生，但走的是两套完全独立的机制：
 *
 * 1. **剩余进度**：`drawArc` 画一段弧，弧长 = 剩余时间 / 总时间。
 *    它由调用方每秒喂进来的 [remainingSeconds] 驱动，是**真实**的剩余量。
 * 2. **呼吸缩放**：整环用 [rememberInfiniteTransition] 做 4 秒一个来回的
 *    1.0 ↔ 1.08 缩放，节奏对齐「吸气—呼气」。
 *
 * 第二条**不表示任何进度**，它只是让等待这件事有个身体感。
 * 把呼吸当成进度是这类界面上最常见的谎言，这里刻意不做。
 *
 * 数据永远是「还剩多少」，不显示「已完成百分之几」——
 * 前者是陪伴，后者是考核。
 */
@Composable
fun CountdownRing(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
    accent: Color = SkinTokens.accent,
    diameter: Dp = 214.dp,
    running: Boolean = true,
    label: String? = null
) {
    val transition = rememberInfiniteTransition(label = "breath")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            // 2000ms 去 + 2000ms 回 = 4 秒一个完整呼吸周期
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath-scale"
    )
    // 暂停时停下来。停着的圆环比继续假装呼吸更诚实。
    // 「动画效果」关掉或系统「减少动画」时（[CastMotion.enabled]）同样静止。
    val scale = if (running && CastMotion.enabled.value) breath else 1f

    val safeTotal = totalSeconds.coerceAtLeast(1)
    val progress = (remainingSeconds.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 13.dp.toPx()
            val inset = strokeWidth / 2f + 3.dp.toPx()
            val arcSize = Size(
                width = (size.width - inset * 2f).coerceAtLeast(0f),
                height = (size.height - inset * 2f).coerceAtLeast(0f)
            )
            val topLeft = Offset(inset, inset)

            // 轨道
            drawArc(
                color = SkinTokens.glassTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 剩余进度：从 12 点方向顺时针收回去
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatMmSs(remainingSeconds),
                fontSize = SkinTokens.display,
                fontWeight = FontWeight.Bold,
                color = SkinTokens.ink
            )
            if (label != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted
                )
            }
        }
    }
}

/** 秒 → `mm:ss`。负数按 0 处理，不会出现 `-01:30` 这种吓人的东西。 */
fun formatMmSs(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
