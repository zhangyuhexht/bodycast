package cn.pocket.toolbox.glass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 手绘图标库。由原 View 版 `Ui.java` 的 `Icon` 逐条移植而来，
 * 保持同一套 24x24 坐标系与 1.7 描边，视觉与旧版一致。
 *
 * 未知 [kind] 会走兜底图形（圆 + 十字），不会崩溃。
 */
@Composable
fun GlassIcon(kind: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        scale(unit, unit, pivot = Offset.Zero) {
            drawGlassIcon(kind, tint)
        }
    }
}

private const val W = 1.7f

private fun DrawScope.ln(x1: Float, y1: Float, x2: Float, y2: Float, c: Color, w: Float = W) {
    drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = w, cap = StrokeCap.Round)
}

private fun DrawScope.rr(
    l: Float, t: Float, r: Float, b: Float, radius: Float, c: Color, w: Float = W
) {
    drawRoundRect(
        color = c,
        topLeft = Offset(l, t),
        size = Size(r - l, b - t),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.dot(x: Float, y: Float, radius: Float, c: Color) {
    drawCircle(c, radius, Offset(x, y), style = Fill)
}

private fun DrawScope.path(c: Color, w: Float = W, block: Path.() -> Unit) {
    val p = Path().apply(block)
    drawPath(p, c, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawGlassIcon(kind: String, c: Color) {
    when (kind) {
        "home" -> {
            rr(4f, 10f, 20f, 21f, 2f, c)
            ln(3f, 10f, 12f, 3f, c); ln(12f, 3f, 21f, 10f, c)
            rr(9f, 14f, 15f, 21f, 1f, c)
        }
        "grid" -> {
            var x = 4f
            while (x < 20f) {
                var y = 4f
                while (y < 20f) { rr(x, y, x + 6f, y + 6f, 1.4f, c); y += 10f }
                x += 10f
            }
        }
        "star" -> path(c) {
            for (i in 0 until 10) {
                val a = -PI / 2 + i * PI / 5
                val rad = if (i % 2 == 0) 9f else 4.2f
                val px = 12f + cos(a).toFloat() * rad
                val py = 12f + sin(a).toFloat() * rad
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        "search" -> {
            drawCircle(c, 6f, Offset(10f, 10f), style = Stroke(W))
            ln(14.5f, 14.5f, 21f, 21f, c)
        }
        "back" -> { ln(14f, 5f, 7f, 12f, c); ln(7f, 12f, 14f, 19f, c) }
        "next" -> { ln(9f, 6f, 15f, 12f, c); ln(15f, 12f, 9f, 18f, c) }
        "battery" -> {
            rr(3f, 7f, 19f, 17f, 2f, c)
            ln(21f, 10f, 21f, 14f, c)
            ln(6f, 10f, 13f, 10f, c); ln(6f, 14f, 13f, 14f, c)
        }
        "folder" -> path(c) {
            moveTo(3f, 7f); lineTo(3f, 5f); lineTo(9f, 5f); lineTo(12f, 8f)
            lineTo(21f, 8f); lineTo(21f, 20f); lineTo(3f, 20f); close()
        }
        "network" -> {
            drawArc(c, 225f, 90f, false, Offset(2f, 4f), Size(20f, 20f), style = Stroke(W))
            drawArc(c, 225f, 90f, false, Offset(6f, 9f), Size(12f, 14f), style = Stroke(W))
            dot(12f, 19f, 1f, c)
        }
        "screen" -> {
            rr(3f, 4f, 21f, 17f, 2f, c)
            ln(8f, 21f, 16f, 21f, c); ln(12f, 17f, 12f, 21f, c)
        }
        "phone" -> {
            rr(6f, 2f, 18f, 22f, 3f, c)
            ln(10f, 5f, 14f, 5f, c); ln(10f, 19f, 14f, 19f, c)
        }
        "cpu" -> {
            rr(6f, 6f, 18f, 18f, 2f, c)
            rr(9f, 9f, 15f, 15f, 1f, c)
            var i = 8f
            while (i <= 16f) {
                ln(i, 2f, i, 6f, c); ln(i, 18f, i, 22f, c)
                ln(2f, i, 6f, i, c); ln(18f, i, 22f, i, c)
                i += 4f
            }
        }
        "speed" -> {
            drawArc(c, 145f, 250f, false, Offset(3f, 3f), Size(18f, 18f), style = Stroke(W))
            ln(12f, 12f, 17f, 7f, c)
            drawCircle(c, 2f, Offset(12f, 12f), style = Stroke(W))
            ln(5f, 19f, 19f, 19f, c)
        }
        "shield" -> {
            path(c) {
                moveTo(12f, 2f); lineTo(21f, 6f); lineTo(20f, 14f)
                quadraticTo(18f, 20f, 12f, 22f)
                quadraticTo(6f, 20f, 4f, 14f)
                lineTo(3f, 6f); close()
            }
            ln(8f, 12f, 11f, 15f, c); ln(11f, 15f, 16f, 9f, c)
        }
        "clean" -> {
            ln(15f, 3f, 10f, 12f, c); ln(7f, 10f, 17f, 16f, c); ln(7f, 10f, 3f, 18f, c)
            ln(3f, 18f, 12f, 22f, c); ln(12f, 22f, 17f, 16f, c); ln(6f, 17f, 10f, 19f, c)
        }
        "code" -> {
            ln(8f, 6f, 2f, 12f, c); ln(2f, 12f, 8f, 18f, c)
            ln(16f, 6f, 22f, 12f, c); ln(22f, 12f, 16f, 18f, c)
            ln(14f, 3f, 10f, 21f, c)
        }
        "settings" -> {
            drawCircle(c, 4f, Offset(12f, 12f), style = Stroke(W))
            drawCircle(c, 9f, Offset(12f, 12f), style = Stroke(W))
            for (i in 0 until 8) {
                val a = i * PI / 4
                val cx = cos(a).toFloat(); val cy = sin(a).toFloat()
                ln(12f + cx * 8f, 12f + cy * 8f, 12f + cx * 11f, 12f + cy * 11f, c)
            }
        }
        "clock" -> {
            drawCircle(c, 9f, Offset(12f, 13f), style = Stroke(W))
            ln(12f, 13f, 12f, 7f, c); ln(12f, 13f, 16f, 15f, c); ln(9f, 1f, 15f, 1f, c)
        }
        "info" -> {
            drawCircle(c, 9f, Offset(12f, 12f), style = Stroke(W))
            ln(12f, 11f, 12f, 17f, c); dot(12f, 7f, 0.9f, c)
        }
        "bolt" -> path(c) {
            moveTo(14f, 2f); lineTo(5f, 14f); lineTo(11f, 14f)
            lineTo(10f, 22f); lineTo(20f, 9f); lineTo(13f, 9f); close()
        }
        "net" -> {
            ln(12f, 2f, 12f, 8f, c)
            drawCircle(c, 4f, Offset(12f, 12f), style = Stroke(W))
            ln(12f, 16f, 12f, 22f, c); ln(2f, 12f, 8f, 12f, c); ln(16f, 12f, 22f, 12f, c)
        }
        "globe" -> {
            drawCircle(c, 9f, Offset(12f, 12f), style = Stroke(W))
            ln(3f, 12f, 21f, 12f, c)
            drawOval(c, Offset(7f, 3f), Size(10f, 18f), style = Stroke(W))
        }
        "calc" -> {
            rr(4f, 2f, 20f, 22f, 2f, c)
            ln(7f, 7f, 17f, 7f, c)
            var x = 7f
            while (x <= 17f) {
                var y = 12f
                while (y <= 18f) { dot(x, y, 0.9f, c); y += 3f }
                x += 5f
            }
        }
        "type" -> { ln(4f, 5f, 20f, 5f, c); ln(12f, 5f, 12f, 20f, c); ln(8f, 20f, 16f, 20f, c) }
        "hash" -> {
            ln(8f, 3f, 6f, 21f, c); ln(18f, 3f, 16f, 21f, c)
            ln(3f, 9f, 21f, 9f, c); ln(3f, 15f, 21f, 15f, c)
        }
        "id" -> {
            rr(2f, 5f, 22f, 19f, 2f, c)
            drawCircle(c, 2.4f, Offset(7.5f, 10.5f), style = Stroke(W))
            drawArc(c, 180f, 180f, false, Offset(4f, 13f), Size(7f, 7f), style = Stroke(W))
            ln(14f, 10f, 19f, 10f, c); ln(14f, 14f, 19f, 14f, c)
        }
        "scan" -> {
            ln(3f, 8f, 3f, 3f, c); ln(3f, 3f, 8f, 3f, c)
            ln(16f, 3f, 21f, 3f, c); ln(21f, 3f, 21f, 8f, c)
            ln(21f, 16f, 21f, 21f, c); ln(21f, 21f, 16f, 21f, c)
            ln(8f, 21f, 3f, 21f, c); ln(3f, 21f, 3f, 16f, c)
            ln(7f, 12f, 17f, 12f, c)
        }
        "calendar" -> {
            rr(3f, 5f, 21f, 21f, 2f, c)
            ln(3f, 10f, 21f, 10f, c)
            ln(8f, 2f, 8f, 6f, c); ln(16f, 2f, 16f, 6f, c)
            var x = 7f
            while (x <= 17f) {
                var y = 13f
                while (y <= 17f) { dot(x, y, 0.9f, c); y += 4f }
                x += 5f
            }
        }
        "qr" -> {
            rr(3f, 3f, 9f, 9f, 1f, c); rr(15f, 3f, 21f, 9f, 1f, c); rr(3f, 15f, 9f, 21f, 1f, c)
            rr(6f, 6f, 8f, 8f, 0.4f, c); rr(17f, 17f, 20f, 20f, 0.4f, c)
            ln(15f, 15f, 15f, 18f, c); ln(18f, 15f, 21f, 15f, c)
        }
        "cube" -> {
            path(c) {
                moveTo(12f, 2f); lineTo(21f, 7f); lineTo(21f, 17f)
                lineTo(12f, 22f); lineTo(3f, 17f); lineTo(3f, 7f); close()
            }
            ln(12f, 2f, 12f, 12f, c); ln(3f, 7f, 12f, 12f, c); ln(21f, 7f, 12f, 12f, c)
        }
        "moon" -> path(c) {
            moveTo(15f, 2f)
            quadraticTo(8f, 4f, 8f, 12f)
            quadraticTo(8f, 20f, 15f, 22f)
            quadraticTo(6f, 21f, 4f, 12f)
            quadraticTo(6f, 3f, 15f, 2f)
            close()
        }
        "dice" -> {
            rr(3f, 3f, 13f, 13f, 3f, c); rr(11f, 11f, 21f, 21f, 3f, c)
            dot(6f, 6f, 0.9f, c); dot(10f, 10f, 0.9f, c)
            dot(14f, 14f, 0.9f, c); dot(18f, 18f, 0.9f, c)
        }
        "bulb" -> {
            drawCircle(c, 6f, Offset(12f, 9f), style = Stroke(W))
            ln(9f, 15f, 15f, 15f, c); ln(10f, 18f, 14f, 18f, c); ln(12f, 2f, 12f, 3f, c)
        }
        "level" -> {
            rr(3f, 11f, 21f, 13f, 1f, c)
            ln(12f, 11f, 12f, 5f, c)
            drawCircle(c, 1f, Offset(10f, 2f), style = Fill)
        }
        "compass2" -> {
            drawCircle(c, 9f, Offset(12f, 12f), style = Stroke(W))
            path(c) {
                moveTo(9f, 15f); lineTo(12f, 8f); lineTo(15f, 15f); lineTo(12f, 13f); close()
            }
        }
        "timer" -> {
            drawCircle(c, 9f, Offset(12f, 13f), style = Stroke(W))
            ln(12f, 13f, 12f, 8f, c)
            ln(9f, 2f, 15f, 2f, c); ln(12f, 2f, 12f, 4f, c); ln(19f, 6f, 20f, 7f, c)
        }
        "shake" -> {
            rr(8f, 3f, 16f, 21f, 2f, c)
            ln(3f, 8f, 3f, 16f, c); ln(5f, 6f, 5f, 18f, c)
            ln(19f, 6f, 19f, 18f, c); ln(21f, 8f, 21f, 16f, c)
        }
        "swap" -> {
            ln(4f, 8f, 19f, 8f, c); ln(16f, 5f, 19f, 8f, c); ln(19f, 11f, 16f, 14f, c)
            ln(20f, 16f, 5f, 16f, c); ln(8f, 13f, 5f, 16f, c); ln(5f, 19f, 8f, 22f, c)
        }
        // ---- 健康相关 ----
        "heart" -> {
            path(c) {
                moveTo(12f, 20.5f)
                cubicTo(12f, 20.5f, 3f, 15f, 3f, 9.5f)
                cubicTo(3f, 6.5f, 5.4f, 4.5f, 8f, 4.5f)
                cubicTo(10f, 4.5f, 11.4f, 5.7f, 12f, 7.2f)
                cubicTo(12.6f, 5.7f, 14f, 4.5f, 16f, 4.5f)
                cubicTo(18.6f, 4.5f, 21f, 6.5f, 21f, 9.5f)
                cubicTo(21f, 15f, 12f, 20.5f, 12f, 20.5f)
                close()
            }
        }
        "water" -> {
            path(c) {
                moveTo(12f, 3f)
                cubicTo(12f, 3f, 6f, 10f, 6f, 14.5f)
                cubicTo(6f, 17.9f, 8.7f, 20.6f, 12f, 20.6f)
                cubicTo(15.3f, 20.6f, 18f, 17.9f, 18f, 14.5f)
                cubicTo(18f, 10f, 12f, 3f, 12f, 3f)
                close()
            }
        }
        "walk" -> {
            drawCircle(c, 2.2f, Offset(13f, 4.5f), style = Stroke(W))
            ln(13f, 7.2f, 11f, 13f, c)
            ln(11f, 13f, 7f, 21f, c)
            ln(11f, 13f, 16.5f, 21f, c)
            ln(12.6f, 8.6f, 18f, 10.5f, c)
            ln(12.6f, 8.6f, 7.5f, 11f, c)
        }
        "flame" -> path(c) {
            moveTo(12f, 2f)
            cubicTo(12f, 7f, 6f, 8.5f, 6f, 14f)
            cubicTo(6f, 18.4f, 8.7f, 22f, 12f, 22f)
            cubicTo(15.3f, 22f, 18f, 18.4f, 18f, 14f)
            cubicTo(18f, 11f, 16.2f, 9f, 15.2f, 7f)
            cubicTo(15.2f, 10f, 13.6f, 11f, 13.6f, 11f)
            cubicTo(13.6f, 8f, 12f, 5f, 12f, 2f)
            close()
        }
        "smile" -> {
            drawCircle(c, 9f, Offset(12f, 12f), style = Stroke(W))
            dot(9f, 10f, 1.1f, c); dot(15f, 10f, 1.1f, c)
            drawArc(c, 25f, 130f, false, Offset(7.4f, 9f), Size(9.2f, 8.5f), style = Stroke(W))
        }
        "scale" -> {
            rr(4f, 4f, 20f, 20f, 4f, c)
            drawArc(c, 200f, 140f, false, Offset(8.4f, 7.5f), Size(7.2f, 7.2f), style = Stroke(W))
            ln(12f, 11.5f, 12f, 8.5f, c)
        }
        "lung" -> {
            ln(12f, 3f, 12f, 12f, c)
            path(c) {
                moveTo(12f, 8.5f)
                cubicTo(9.5f, 6.5f, 5f, 8f, 5f, 13f)
                cubicTo(5f, 17f, 6.5f, 20.5f, 9.5f, 20.5f)
                cubicTo(11.5f, 20.5f, 12f, 18f, 12f, 15f)
            }
            path(c) {
                moveTo(12f, 8.5f)
                cubicTo(14.5f, 6.5f, 19f, 8f, 19f, 13f)
                cubicTo(19f, 17f, 17.5f, 20.5f, 14.5f, 20.5f)
                cubicTo(12.5f, 20.5f, 12f, 18f, 12f, 15f)
            }
        }
        "leaf" -> {
            path(c) {
                moveTo(20.5f, 3.5f)
                cubicTo(11f, 3.5f, 3.5f, 8f, 3.5f, 15f)
                cubicTo(3.5f, 18.2f, 5.8f, 20.5f, 9f, 20.5f)
                cubicTo(16f, 20.5f, 20.5f, 13f, 20.5f, 3.5f)
                close()
            }
            ln(19f, 5f, 6f, 19f, c)
        }
        "bell" -> {
            path(c) {
                moveTo(6f, 17f); lineTo(6f, 11f)
                cubicTo(6f, 7f, 8.6f, 4f, 12f, 4f)
                cubicTo(15.4f, 4f, 18f, 7f, 18f, 11f)
                lineTo(18f, 17f)
            }
            ln(4f, 17f, 20f, 17f, c)
            drawArc(c, 0f, 180f, false, Offset(9.6f, 17f), Size(4.8f, 4.8f), style = Stroke(W))
        }
        "user" -> {
            drawCircle(c, 4.2f, Offset(12f, 8f), style = Stroke(W))
            drawArc(c, 180f, 180f, false, Offset(4.5f, 13f), Size(15f, 15f), style = Stroke(W))
        }
        "chart" -> {
            ln(3.5f, 20.5f, 20.5f, 20.5f, c)
            ln(7f, 20f, 7f, 13f, c, 3.2f)
            ln(12f, 20f, 12f, 7f, c, 3.2f)
            ln(17f, 20f, 17f, 10.5f, c, 3.2f)
        }
        "trend" -> {
            ln(3.5f, 20.5f, 20.5f, 20.5f, c)
            ln(4f, 16f, 9f, 11f, c)
            ln(9f, 11f, 13f, 14f, c)
            ln(13f, 14f, 20f, 6f, c)
            ln(20f, 6f, 15f, 6f, c); ln(20f, 6f, 20f, 11f, c)
        }
        "check" -> { ln(4.5f, 12.5f, 9.5f, 18f, c); ln(9.5f, 18f, 19.5f, 6f, c) }
        "plus" -> { ln(12f, 5f, 12f, 19f, c); ln(5f, 12f, 19f, 12f, c) }
        "minus" -> ln(5f, 12f, 19f, 12f, c)
        "target" -> {
            drawCircle(c, 9f, Offset(12f, 12f), style = Stroke(W))
            drawCircle(c, 4.5f, Offset(12f, 12f), style = Stroke(W))
            dot(12f, 12f, 1.2f, c)
        }
        "book" -> {
            rr(3f, 4.5f, 12f, 20f, 2f, c); rr(12f, 4.5f, 21f, 20f, 2f, c)
            ln(12f, 4.5f, 12f, 20f, c)
        }
        "play" -> path(c) { moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close() }
        "pause" -> { rr(8f, 5f, 10.6f, 19f, 1f, c); rr(13.4f, 5f, 16f, 19f, 1f, c) }
        "reset" -> {
            drawArc(c, 40f, 290f, false, Offset(4f, 4f), Size(16f, 16f), style = Stroke(W))
            ln(17.5f, 2.5f, 20.5f, 6.5f, c); ln(20.5f, 6.5f, 16.5f, 7.5f, c)
        }
        "edit" -> {
            ln(4f, 20f, 4f, 16f, c)
            ln(4f, 16f, 16f, 4f, c); ln(16f, 4f, 20f, 8f, c)
            ln(20f, 8f, 8f, 20f, c); ln(4f, 20f, 8f, 20f, c)
        }
        "trash" -> {
            ln(4f, 6.5f, 20f, 6.5f, c)
            ln(9.5f, 6.5f, 9.5f, 3.5f, c); ln(14.5f, 6.5f, 14.5f, 3.5f, c)
            rr(6f, 6.5f, 18f, 20.5f, 2f, c)
        }
        "sun" -> {
            drawCircle(c, 4.6f, Offset(12f, 12f), style = Stroke(W))
            for (i in 0 until 8) {
                val a = i * PI / 4
                val cx = cos(a).toFloat(); val cy = sin(a).toFloat()
                ln(12f + cx * 6.6f, 12f + cy * 6.6f, 12f + cx * 9.6f, 12f + cy * 9.6f, c)
            }
        }
        "up" -> { ln(12f, 19f, 12f, 6f, c); ln(7f, 11f, 12f, 6f, c); ln(17f, 11f, 12f, 6f, c) }
        "down" -> { ln(12f, 5f, 12f, 18f, c); ln(7f, 13f, 12f, 18f, c); ln(17f, 13f, 12f, 18f, c) }
        else -> {
            drawCircle(c, 8f, Offset(12f, 12f), style = Stroke(W))
            ln(8f, 12f, 16f, 12f, c); ln(12f, 8f, 12f, 16f, c)
        }
    }
}
