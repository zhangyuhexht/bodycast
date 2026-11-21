package cn.pocket.toolbox.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as GColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.i18n.BodyCastText
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * 「今日天气明信片」（22.38.0）：一键生成、发得出去的分享图。
 *
 * 设计目标不是「信息截图」而是「愿意发朋友圈的画」：
 * 天气色的深底 + 会发光的气象球 + 一句话 + 日期 + 一行品牌小字，
 * **不含任何数值、体重、热量**（产品哲学一路跟到底）。
 *
 * 用原生 Canvas 绘制（而不是截屏 Compose 视图）：
 * 像素级可控、不依赖 Compose 渲染管线，出图稳定。
 */
object WeatherPostcard {

    /** 明信片尺寸：2:3 竖版，朋友圈/聊天里都上相。 */
    private const val WIDTH = 1080
    private const val HEIGHT = 1620

    private const val INK = 0xFFF1F5F3.toInt()
    private const val INK_SOFT = 0xB3E6EEEA.toInt()

    /** 渲染一张明信片。[line] 是「今天的一句话」（调用方给已定的文案）。 */
    fun render(kind: WeatherKind, line: String, date: LocalDate): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val surface = kind.colorHex.toInt()
        val deep = kind.deepHex.toInt()

        // ---- 底：深色纵向渐变（上深色带天气色、下近黑）----
        canvas.drawPaint(
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, HEIGHT.toFloat(),
                    blend(deep, GColor.BLACK, 0.25f),
                    GColor.rgb(10, 12, 16),
                    Shader.TileMode.CLAMP
                )
            }
        )
        // 大面积柔光晕：天气色从上三分之一洒下来
        canvas.drawPaint(
            Paint().apply {
                shader = RadialGradient(
                    WIDTH / 2f, HEIGHT * 0.30f, WIDTH * 0.95f,
                    intArrayOf(alpha(surface, 0.34f), alpha(surface, 0.0f)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )

        // ---- 气象球：外晕 + 双层径向渐变体 ----
        val cx = WIDTH / 2f
        val cy = HEIGHT * 0.325f
        val r = WIDTH * 0.21f
        canvas.drawCircle(
            cx, cy, r * 1.85f,
            Paint().apply {
                shader = RadialGradient(
                    cx, cy, r * 1.85f,
                    intArrayOf(alpha(surface, 0.40f), alpha(surface, 0.0f)),
                    floatArrayOf(0.35f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )
        canvas.drawCircle(
            cx, cy, r,
            Paint().apply {
                shader = RadialGradient(
                    cx - r * 0.38f, cy - r * 0.42f, r * 1.65f,
                    intArrayOf(blend(surface, GColor.WHITE, 0.32f), surface, deep),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )
        // 顶部高光弧：让球看起来是玻璃而不是圆片
        canvas.drawCircle(
            cx - r * 0.30f, cy - r * 0.36f, r * 0.42f,
            Paint().apply {
                shader = RadialGradient(
                    cx - r * 0.30f, cy - r * 0.36f, r * 0.42f,
                    intArrayOf(alpha(GColor.WHITE, 0.32f), alpha(GColor.WHITE, 0.0f)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )

        // ---- 文案 ----
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 108f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mottoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_SOFT
            textSize = 40f
        }
        val linePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 46f
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_SOFT
            textSize = 34f
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = alpha(GColor.WHITE, 0.55f)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 排版：每段按实际布局高度推进（22.38.0 二修——写死行距会叠字）。
        var y = cy + r * 1.38f
        y = drawCentered(canvas, BodyCastText.text(kind.label), labelPaint, y, WIDTH - 160f) + 18f
        y = drawCentered(canvas, BodyCastText.text(kind.motto), mottoPaint, y, WIDTH - 260f) + 64f
        y = drawCentered(canvas, BodyCastText.text(line), linePaint, y, WIDTH - 220f)

        // 底部：日期 + 品牌（贴底锚定）。
        drawCentered(canvas, BodyCastText.formatDate(date), metaPaint, HEIGHT - 190f, WIDTH - 160f)
        drawCentered(canvas, BodyCastText.text("身体气象台 · BodyCast"), brandPaint, HEIGHT - 120f, WIDTH - 160f)

        return bitmap
    }

    /** 存到缓存目录并通过系统分享面板发出去。 */
    fun share(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "postcards").apply { mkdirs() }
        val file = File(dir, "bodycast-postcard-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, BodyCastText.text("今日天气明信片"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, BodyCastText.text("把明信片发出去")))
    }

    // ---- 绘制小工具 ----

    /** 画一段居中文本，返回它底部的 y（供上层继续排版）。 */
    private fun drawCentered(canvas: Canvas, text: String, paint: TextPaint, top: Float, maxWidth: Float): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.18f)
            .build()
        canvas.withTranslation((WIDTH - layout.width) / 2f, top) { layout.draw(this) }
        return top + layout.height
    }

    private fun alpha(color: Int, a: Float): Int =
        (color and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    private fun blend(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        val r = (GColor.red(a) * (1 - k) + GColor.red(b) * k).toInt()
        val g = (GColor.green(a) * (1 - k) + GColor.green(b) * k).toInt()
        val bl = (GColor.blue(a) * (1 - k) + GColor.blue(b) * k).toInt()
        return GColor.rgb(r, g, bl)
    }

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        save()
        translate(x, y)
        block()
        restore()
    }
}
