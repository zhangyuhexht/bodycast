package cn.pocket.toolbox.ui

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.pocket.toolbox.ui.skin.SkinTokens
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.ui.bodycast.Text

/**
 * 首次打开应用的入场动画（22.44.0）：软件图标 **BC** 丝滑变形成 **BodyCast**。
 *
 * 只在开箱引导之前播一次（重看引导不播）。三段一气呵成：
 * 1. 落定：BC（图标同款蓝→绿渐变）从轻微缩放中浮出；
 * 2. 展开：B 向左、C 向右轻轻让位，中间淡入 "ody"——BC 就地长成 BodyCast；
 * 3. 让位：整词轻微放大并淡出，交棒给开箱引导。
 *
 * **驱动用 Handler + SystemClock**（22.44.0）：冷启动最早的一批组合里
 * Compose 帧时钟与 Choreographer 的动画都实测不推进（画面静止在第一帧），
 * Handler 是最底层的时间源，什么时候起跑都能完整走完。
 * 另挂一个兜底：即使驱动异常，2.6 秒后也一定进开箱，不会卡在入场页。
 */
@Composable
fun IntroOverlay(onDone: () -> Unit) {
    val p = remember { mutableFloatStateOf(0f) }
    val done = remember { object {
        var fired = false
        fun fire(callback: () -> Unit) {
            if (!fired) {
                fired = true
                callback()
            }
        }
    } }
    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val duration = 3200L
        val tick = object : Runnable {
            override fun run() {
                val t = ((SystemClock.uptimeMillis() - start).toFloat() / duration)
                    .coerceIn(0f, 1f)
                p.floatValue = t
                if (t < 1f) {
                    handler.postDelayed(this, 16)
                } else {
                    done.fire(onDone)
                }
            }
        }
        // 兜底：驱动异常也保证 3.8 秒后进开箱。
        handler.postDelayed({ done.fire(onDone) }, 3800)
        if (CastMotion.enabled.value) {
            handler.post(tick)
        } else {
            done.fire(onDone)
        }
        onDispose { handler.removeCallbacksAndMessages(null) }
    }

    // 五段进度（22.47.0 按用户定调中和）：BC 定格拉长、BodyCast 段收短——
    // 0-16% BC 上浮落定 ｜ 16-40% BC 定格（长） ｜ 40-60% 变形为 BodyCast（快）
    // ｜ 60-76% BodyCast 定格（短） ｜ 76-92% 淡出。
    val t = p.floatValue
    val appear = FastOutSlowInEasing.transform((t / 0.16f).coerceIn(0f, 1f))
    val spread = CastMotion.Emphasized.transform(((t - 0.40f) / 0.20f).coerceIn(0f, 1f))
    val fadeOut = FastOutSlowInEasing.transform(((t - 0.76f) / 0.16f).coerceIn(0f, 1f))
    // 浮出时的轻微上浮：从下方 12dp 升到落点。
    val rise = 12.dp * (1f - appear)

    // 字距：BodyCast 成型时从松到正（与交叉淡化同相位）。

    // 图标同款渐变：左偏蓝、右偏绿，倾斜约 25°（与启动图标一致的语言）。
    val brand = Brush.linearGradient(
        colors = listOf(Color(0xFF4EA8FF), Color(0xFF3ED598))
    )

    Box(
        Modifier
            .fillMaxSize()
            // 图标底色：**跟应用主题走**（22.58.0 修）。
            // 以前写死深色 0xFF0B0F0D——浅色模式下冷启动会先黑一下再进浅色界面，
            // 看着像闪了一下。现在用应用自己的底色，Brand 渐变在两个底上都清楚。
            .background(SkinTokens.bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { done.fire(onDone) },
        contentAlignment = Alignment.Center
    ) {
        // 22.45.1：修「B␣␣␣C」开场。此前 ody 用 alpha 淡入——透明度不撤布局
        // 占位，初始画面是 B 与 C 之间隔着一段空档，观感是裂开的。
        // 改成 BC ↔ BodyCast 整词交叉淡化：BC 在中心落定，BodyCast 带着收紧的
        // 字距在原地长出来，两词同轴交叠，任何一帧都不会出现空档。
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = rise)
                .alpha((1f - fadeOut).coerceIn(0f, 1f))
                .scale(0.92f + 0.08f * appear + 0.04f * fadeOut)
        ) {
            Text(
                text = "BC",
                style = TextStyle(
                    brush = brand,
                    fontSize = 56.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier.alpha((appear * (1f - spread)).coerceIn(0f, 1f))
            )
            Text(
                text = "BodyCast",
                style = TextStyle(
                    brush = brand,
                    fontSize = 42.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = lerp(10.sp, 0.sp, spread)
                ),
                modifier = Modifier.alpha(spread)
            )
        }
    }
}
