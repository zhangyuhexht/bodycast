package cn.pocket.toolbox.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * 液态玻璃的运行时能力与降级路径。
 *
 * 官方 Backdrop 库内部对不支持的 API 是静默跳过的（反编译确认）：
 * blur 需要 API 31+（RenderEffect），lens/折射着色器需要 API 33+（RuntimeShader），
 * 库自己的 minSdk 是 21。所以安装包可以安全地下探到安卓 8（API 26，
 * 同时也是 Health Connect 库的 minSdk）。
 *
 * 但「静默跳过」意味着低版本上玻璃只是一层发灰的透明片——
 * 文字直接和背景混在一起。这个文件把「这台设备跑不跑液态玻璃」
 * 收敛成一个开关，并给所有自写玻璃控件提供统一的降级表面。
 */
object GlassSupport {

    /**
     * 「强制磨砂降级」开关（设置 → 实验性功能 → 底层开关）。
     *
     * 打开后即使设备支持折射着色器也一律走磨砂降级，用来对比两种观感。
     * 和 [GlassTheme] 的其他档位一样，这里只是偏好的一份静态快照：
     * `BodyCastStore` 在偏好变化时调 [setForceFrost] 推进来。
     *
     * 注意：切换后返回其他页面即生效；当前页需要离开再回来
     * （正在显示的玻璃节点已经按旧值组合过一遍）。
     */
    private var forceFrostEnabled by mutableStateOf(false)

    fun setForceFrost(on: Boolean) {
        if (forceFrostEnabled != on) forceFrostEnabled = on
    }

    /**
     * 这台设备是否启用液态玻璃（含折射着色器）。
     *
     * 用 SDK_INT >= 33 而不用库自己的 isRuntimeShaderSupported()，
     * 是因为产品要求「安卓 13 以下一律磨砂降级」：API 31/32 虽然
     * blur 可用，但没有折射的玻璃和整套液态玻璃观感差太多，
     * 统一走降级反而整齐。用户打开「强制磨砂降级」时同样走降级。
     */
    val isLiquidGlassAvailable: Boolean
        get() = !forceFrostEnabled && Build.VERSION.SDK_INT >= 33
}

/**
 * 自写玻璃控件的统一表面。**新增自定义玻璃控件必须走这里，不允许直接调 drawBackdrop。**
 *
 * 液态玻璃（API 33+）：参数原样转交给官方 [drawBackdrop]，观感与降级方案引入之前
 * 完全一致——各调用点原先传什么（blur/lens 半径、阴影 alpha、表面色），这里照样传什么。
 * 唯一例外是「玻璃强度」实验档位：blur 半径与表面色被统一乘一个系数，
 * 见 [GlassTheme.glassBlurScale] / [GlassTheme.glassSurfaceAlphaScale]。
 *
 * 降级（API 26–32）：不调用 drawBackdrop（连带避免一切着色器和 RenderEffect），
 * 换成一块磨砂实底 [frostedTint]。按压弹性若调用方给了 [layerBlock]，
 * 改用 graphicsLayer 近似——降级模式下内容和玻璃一起轻微位移，
 * 不像液态玻璃那样只形变玻璃层，这是低版本上已知的取舍。
 *
 * @param vibrancy 是否加官方 vibrancy 效果（图标盒、提示条这类薄元素原本没有）
 * @param blurRadius 官方 blur 半径；null 表示这个元素原本就不加 blur
 * @param lensHorizontal/lensVertical 官方 lens 折射半径；null 表示原本不加 lens
 * @param shadowAlpha 表面阴影 alpha；null 保持官方默认阴影（Shadow.Default）
 * @param frostedTint 降级模式下的磨砂实底色
 */
fun Modifier.glassSurface(
    backdrop: Backdrop,
    shape: Shape,
    vibrancy: Boolean = true,
    blurRadius: Dp? = 10.dp,
    lensHorizontal: Dp? = null,
    lensVertical: Dp? = null,
    // 22.12.0：官方 Dialog 用 depthEffect 让折射带一点纵深；默认关，行为不变。
    lensDepthEffect: Boolean = false,
    shadowAlpha: Float? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    frostedTint: Color = GlassTheme.frostedBase,
    onDrawSurface: DrawScope.() -> Unit
): Modifier {
    // 「玻璃强度」实验档位只在这里生效：调用点传什么还是什么，
    // 由这两条系数统一缩放 blur 半径与表面色浓度，不改任何调用点的参数语义。
    val blurScale = GlassTheme.glassBlurScale
    val surfaceAlphaScale = GlassTheme.glassSurfaceAlphaScale

    if (!GlassSupport.isLiquidGlassAvailable) {
        // 降级路径没有 blur，只按同一个系数调磨砂实底的浓度。
        val tint = if (surfaceAlphaScale == 1f) {
            frostedTint
        } else {
            frostedTint.copy(alpha = (frostedTint.alpha * surfaceAlphaScale).fastCoerceAtMost(1f))
        }
        val base = background(tint, shape)
        return if (layerBlock != null) {
            base.graphicsLayer(block = layerBlock)
        } else {
            base
        }
    }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (vibrancy) vibrancy()
            blurRadius?.let { blur(it.toPx() * blurScale) }
            if (lensHorizontal != null && lensVertical != null) {
                lens(
                    lensHorizontal.toPx(),
                    lensVertical.toPx(),
                    depthEffect = lensDepthEffect
                )
            }
        },
        // 降级前所有调用点要么显式传 Highlight.Default，要么用库默认（也是 Highlight.Default），
        // 所以这里统一等价。
        highlight = { Highlight.Default },
        shadow = {
            if (shadowAlpha != null) Shadow(alpha = shadowAlpha) else Shadow.Default
        },
        layerBlock = layerBlock,
        onDrawSurface = {
            if (surfaceAlphaScale == 1f) {
                onDrawSurface()
            } else {
                // 表面色是调用点自己 drawRect 出来的（同一个玻璃面可能画不止一层），
                // 所以在整层上乘一个 alpha 系数，而不是去改调用点的颜色值。
                // 用颜色矩阵的 alpha 项（而不是 saveLayer 的 paint alpha）：
                // 后者上限就是 1，没法表达「更磨砂」那一档的 ×1.25。
                // 只有非标准档位才会走到这里：默认档位不多付一次 saveLayer。
                val paint = Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { this[3, 3] = surfaceAlphaScale }
                    )
                }
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), paint)
                onDrawSurface()
                drawContext.canvas.restore()
            }
        }
    )
}
