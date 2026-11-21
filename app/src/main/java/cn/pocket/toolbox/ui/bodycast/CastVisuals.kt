package cn.pocket.toolbox.ui.bodycast

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.bodycast.EchoSeries
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.bodycast.WeekCalendar
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.ui.skin.SkinTokens
import kotlin.math.abs

/**
 * 身体气象台的「画」。
 *
 * 这个文件只负责把状态画出来，不含任何业务判断、不读写仓库、不发请求。
 * 四个图形：
 *
 * 2. [WeatherOrb]        —— 跟着天气类型走的动态气象球
 * 3. [WeekWeatherStrip]  —— 一周的身体天气日历
 * 4. [EchoTrendBand]     —— 回声图表（定性带，没有数值坐标轴）
 *
 * **这里没有任何一处会画出数字**：没有刻度、没有百分比、没有进度数值。
 */

// ---------------------------------------------------------------------------
// 1 冷启动 Logo
// ---------------------------------------------------------------------------

/**
 * 今日气象球。
 *
 * 颜色完全跟着 [kind] 走，边缘用同色系的深色收口，所以五种天气一眼可辨。
 * 球一直在缓慢呼吸——它表达的是「身体是活的」，不是「进度在走」。
 *
 * 球内文字按 [ORB_TEXT_WIDTH_RATIO] 限宽，字号从 [ORB_LABEL_TIERS] 里按长度挑：
 * 中文词仍是 36sp、单行；外文长标签逐档下探（下限 13sp），必要时居中折行，
 * 都不顶出球外、也不截断（见 [orbTextFit]）。
 */
@Composable
fun WeatherOrb(
    kind: WeatherKind,
    modifier: Modifier = Modifier,
    diameter: Dp = 216.dp
) {
    val surface = Color(kind.colorHex)
    val deep = Color(kind.deepHex)

    // 球内文字：中文三个字仍然落在 36sp、单行；英文 / 俄文标签长得多，按长度逐档缩到
    // 放得下为止（下限 13sp），整块（标题 + 小字）都落在球体里——不出圈，也不截断。
    val textWidth = diameter * ORB_TEXT_WIDTH_RATIO
    val orbBodyRadius = diameter * ORB_BODY_RADIUS_RATIO
    val dpPerSp = with(LocalDensity.current) { 1.sp.toDp().value }
    val label = kind.label
    val motto = kind.motto
    val fit = remember(label, motto, textWidth, orbBodyRadius, dpPerSp) {
        orbTextFit(
            label = label,
            motto = motto,
            maxWidthDp = textWidth.value,
            orbRadiusDp = orbBodyRadius.value,
            dpPerSp = dpPerSp
        )
    }

    val transition = rememberInfiniteTransition(label = "weather-orb")
    // 22.28.0 动画批：呼吸/色晕游走是**循环**动画，不吃系统时长缩放，
    // 必须自己听 [CastMotion.enabled]——「动画效果」关掉或系统「减少动画」时
    // 完全静止（breath 钉在 1f、drift 钉在中点），不偷偷继续跑。
    val motionOn = CastMotion.enabled.value
    val breath by if (motionOn) {
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb-breath"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    // 色晕在球体里缓慢游走，避免它看起来是一张贴上去的静态图
    val drift by if (motionOn) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb-drift"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    Box(
        modifier
            .size(diameter)
            .scale(breath)
            .drawBehind {
                val r = size.minDimension / 2f
                val c = center

                // 外层光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(surface.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(c.x + (drift - 0.5f) * r * 0.5f, c.y - r * 0.22f),
                        radius = r
                    ),
                    radius = r
                )

                // 球体本体
                val orbRadius = r * 0.80f
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(surface, deep),
                        start = Offset(c.x - orbRadius * 0.75f, c.y - orbRadius * 0.85f),
                        end = Offset(c.x + orbRadius * 0.85f, c.y + orbRadius * 0.95f)
                    ),
                    radius = orbRadius
                )

                // 流动的内层色晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(deep.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(
                            c.x - orbRadius * 0.40f + orbRadius * 0.85f * drift,
                            c.y + orbRadius * 0.30f - orbRadius * 0.55f * drift
                        ),
                        radius = orbRadius * 0.95f
                    ),
                    radius = orbRadius
                )

                // 高光
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(c.x - orbRadius * 0.34f, c.y - orbRadius * 0.40f),
                        radius = orbRadius * 0.85f
                    ),
                    radius = orbRadius
                )

                // 边缘收口
                drawCircle(
                    color = deep.copy(alpha = 0.55f),
                    radius = orbRadius,
                    style = Stroke(width = orbRadius * 0.035f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(textWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                maxLines = fit.labelLines,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fit.labelSizeSp.sp,
                    lineHeight = (fit.labelSizeSp * ORB_LABEL_LINE_HEIGHT).sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.35f),
                        offset = Offset(0f, 2f),
                        blurRadius = 10f
                    )
                )
            )
            Spacer(Modifier.height(ORB_LABEL_MOTTO_GAP.dp))
            Text(
                text = motto,
                textAlign = TextAlign.Center,
                maxLines = fit.mottoLines,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = fit.mottoSizeSp.sp,
                    lineHeight = (fit.mottoSizeSp * ORB_MOTTO_LINE_HEIGHT).sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.30f),
                        offset = Offset(0f, 1f),
                        blurRadius = 8f
                    )
                )
            )
        }
    }
}

/**
 * 球内文字可排的宽度比例。
 *
 * 球体本体只画到外框的 0.80（见上面的 drawBehind），文字再往里收一点，
 * 折行的两端才不会顶到球边。
 */
private const val ORB_TEXT_WIDTH_RATIO = 0.62f

/** 球体本体的半径占外框直径的比例。和绘制里的 `r * 0.80f` 是同一个尺寸。 */
private const val ORB_BODY_RADIUS_RATIO = 0.40f

/** 主标题的行高系数。 */
private const val ORB_LABEL_LINE_HEIGHT = 1.25f

/** 小字的行高系数。 */
private const val ORB_MOTTO_LINE_HEIGHT = 1.4f

/** 主标题与小字之间的空隙（dp）。判断行数时也要算上它。 */
private const val ORB_LABEL_MOTTO_GAP = 8f

/** 主标题优先排两行。 */
private const val ORB_PREFERRED_LINES = 2

/**
 * 单行优先的下限。
 *
 * 字号掉到这个数以下就不再为了硬撑单行继续缩，改成折行：
 * 一个 13sp 的单行标题比两行 20sp 更难读。
 */
private const val ORB_ONE_LINE_MIN_SP = 24f

/**
 * 球内文字的行数上限。
 *
 * 只作为兜底：真排到四行说明这句文案在球里已经很长了，再多就该改文案而不是继续压字号。
 */
private const val ORB_MAX_LINES = 4

/**
 * 球内主标题的候选字号，从大到小。
 *
 * 中文三四个字永远落在第一档（36sp），英文 / 俄文这类长标签按长度逐档下探，
 * 下限 13sp——再小就不如不显示了。
 */
private val ORB_LABEL_TIERS = listOf(36f, 33f, 30f, 27f, 24f, 21f, 19f, 17f, 15f, 13f)

/** 球内小字的候选字号：中文停在 caption（15sp），外文放不下时才让到 14 / 13sp。 */
private val ORB_MOTTO_TIERS = listOf(15f, 14f, 13f)

/**
 * 一个字符的经验宽度（em）。
 *
 * 全角字（汉字、假名、全角标点）算一个字宽，拉丁 / 西里尔字母算六成，
 * 空格三成。都往宽了估：宁可让标签小一档，也不让它顶出球外。
 */
private fun glyphEm(ch: Char): Float = when {
    ch == ' ' || ch == '\u00A0' -> 0.30f
    ch.code < 0x2E80 -> 0.62f
    else -> 1f
}

/** 可以断行的最小单位：一个全角字，或者一个拉丁词。 */
private class OrbUnit(val widthEm: Float, val spaceBefore: Boolean)

/**
 * 把一段文字切成断行单位。
 *
 * 汉字逐字断（和系统排版一致），拉丁词整词断——所以「Sensitive」这种长词
 * 不会被从中间劈开，而是先看它自己放不放得下。
 */
private fun orbUnits(text: String): List<OrbUnit> {
    val units = mutableListOf<OrbUnit>()
    var wordEm = 0f
    var space = false

    fun flushWord() {
        if (wordEm > 0f) {
            units += OrbUnit(wordEm, space)
            wordEm = 0f
            space = false
        }
    }

    text.forEach { ch ->
        when {
            ch == ' ' || ch == '\u00A0' -> {
                flushWord()
                space = true
            }

            glyphEm(ch) >= 1f -> {
                flushWord()
                units += OrbUnit(1f, space)
                space = false
            }

            else -> wordEm += glyphEm(ch)
        }
    }
    flushWord()
    return units
}

/**
 * 逐行估宽：把 [text] 按 [fontSizeSp] 排进 [maxWidthDp]，返回每一行的宽度（dp）。
 *
 * 断行单位见 [orbUnits]：汉字逐字断，拉丁词整词断。留 3% 余量：估宽宁可多算一点，
 * 也不让最后一行的字贴着边。
 */
private fun orbLineWidths(
    text: String,
    fontSizeSp: Float,
    maxWidthDp: Float,
    dpPerSp: Float
): List<Float> {
    if (text.isBlank() || fontSizeSp <= 0f || maxWidthDp <= 0f) return emptyList()

    val size = fontSizeSp * dpPerSp
    val limit = maxWidthDp * 0.97f
    val spaceDp = 0.30f * size
    val widths = mutableListOf<Float>()
    var used = 0f

    orbUnits(text).forEach { unit ->
        val width = unit.widthEm * size
        val gap = if (unit.spaceBefore && used > 0f) spaceDp else 0f
        when {
            used == 0f -> used = width
            used + gap + width <= limit -> used += gap + width
            else -> {
                widths += used
                used = width
            }
        }
    }
    if (used > 0f) widths += used
    return widths
}

/** 这段文字里最宽的一个断行单位（em）。它比整行还宽时只能横向溢出。 */
private fun orbWidestUnitEm(text: String): Float = orbUnits(text).maxOfOrNull { it.widthEm } ?: 0f

/** 球内一次排版的结论：主标题字号 / 行数 + 小字号 / 行数。 */
private class OrbTextFit(
    val labelSizeSp: Float,
    val labelLines: Int,
    val mottoSizeSp: Float,
    val mottoLines: Int
)

/** 球内文字的一行：离整块中心的距离、半宽、墨迹半高。 */
private class OrbRow(val centerY: Float, val halfWidth: Float, val inkHalfHeight: Float)

/**
 * 给球内文字挑一套「整块都落在球体里」的排版。
 *
 * 规则只有一条：从大字号往小试，第一组（标题优先、小字其次）逐行墨迹都在球里的
 * 就是答案。中文三个字在第一档（36sp、单行）就成立，所以中文的字号一点没变；
 * 英文 / 俄文标签会自动往下让几档，必要时折行——字号下限 13sp，行数上限
 * [ORB_MAX_LINES]。所以「缩小字号」和「折行」都是按实际宽度算出来的，不靠猜。
 *
 * 所有档位都不成立时取最小档：宁可字小一点，也不把最后一句截掉。
 */
private fun orbTextFit(
    label: String,
    motto: String,
    maxWidthDp: Float,
    orbRadiusDp: Float,
    dpPerSp: Float
): OrbTextFit {
    val limit = maxWidthDp * 0.97f
    fun widths(text: String, sizeSp: Float) = orbLineWidths(text, sizeSp, maxWidthDp, dpPerSp)
    fun unitFits(text: String, sizeSp: Float) = orbWidestUnitEm(text) * sizeSp * dpPerSp <= limit

    // 主标题候选：中文这类短语优先保持单行——中日韩文字可以在任意位置断行，
    // 硬折出来的两行会把球撑满。只有单行要缩到 [ORB_ONE_LINE_MIN_SP] 以下时才折行。
    val singleLine = ORB_LABEL_TIERS.firstOrNull { unitFits(label, it) && widths(label, it).size == 1 }
    val labelTiers = if (singleLine != null && singleLine >= ORB_ONE_LINE_MIN_SP) {
        listOf(singleLine)
    } else {
        ORB_LABEL_TIERS.filter { unitFits(label, it) && widths(label, it).size <= ORB_PREFERRED_LINES }
    }
    val mottoTiers = ORB_MOTTO_TIERS.filter { unitFits(motto, it) && widths(motto, it).size <= ORB_MAX_LINES }

    labelTiers.forEach { labelSp ->
        mottoTiers.forEach { mottoSp ->
            val fit = OrbTextFit(
                labelSizeSp = labelSp,
                labelLines = widths(label, labelSp).size,
                mottoSizeSp = mottoSp,
                mottoLines = widths(motto, mottoSp).size
            )
            if (orbBlockFits(label, motto, fit, maxWidthDp, orbRadiusDp, dpPerSp)) return fit
        }
    }

    val labelSp = labelTiers.lastOrNull() ?: ORB_LABEL_TIERS.last()
    val mottoSp = mottoTiers.lastOrNull() ?: ORB_MOTTO_TIERS.last()
    return OrbTextFit(
        labelSizeSp = labelSp,
        labelLines = widths(label, labelSp).size.coerceIn(1, ORB_MAX_LINES),
        mottoSizeSp = mottoSp,
        mottoLines = widths(motto, mottoSp).size.coerceIn(1, ORB_MAX_LINES)
    )
}

/**
 * 整块文字（标题 + 小字）逐行检查：每一行的墨迹两端都要落在球体里。
 *
 * 用「行的中心 + 墨迹半高」判断，而不是拿整块的外框去比圆——文字实际占的是几条
 * 横带，不是四个角；按外框算会把本来就放得下的中文判成出圈。
 */
private fun orbBlockFits(
    label: String,
    motto: String,
    fit: OrbTextFit,
    maxWidthDp: Float,
    orbRadiusDp: Float,
    dpPerSp: Float
): Boolean {
    if (orbRadiusDp <= 0f) return true

    val labelLineHeight = fit.labelSizeSp * dpPerSp * ORB_LABEL_LINE_HEIGHT
    val mottoLineHeight = fit.mottoSizeSp * dpPerSp * ORB_MOTTO_LINE_HEIGHT
    val total = fit.labelLines * labelLineHeight + ORB_LABEL_MOTTO_GAP + fit.mottoLines * mottoLineHeight

    val rows = mutableListOf<OrbRow>()
    var y = -total / 2f
    orbLineWidths(label, fit.labelSizeSp, maxWidthDp, dpPerSp).forEach { width ->
        rows += OrbRow(y + labelLineHeight / 2f, width / 2f, fit.labelSizeSp * dpPerSp / 2f)
        y += labelLineHeight
    }
    y += ORB_LABEL_MOTTO_GAP
    orbLineWidths(motto, fit.mottoSizeSp, maxWidthDp, dpPerSp).forEach { width ->
        rows += OrbRow(y + mottoLineHeight / 2f, width / 2f, fit.mottoSizeSp * dpPerSp / 2f)
        y += mottoLineHeight
    }

    val r2 = orbRadiusDp * orbRadiusDp
    return rows.all { row ->
        val dy = abs(row.centerY) + row.inkHalfHeight
        row.halfWidth * row.halfWidth + dy * dy <= r2
    }
}

// ---------------------------------------------------------------------------
// 4 周天气日历
// ---------------------------------------------------------------------------

/** 日历格里的两字简称。这里不出现「好 / 差」这类判断词。 */
internal fun weatherShortLabel(kind: WeatherKind): String = when (kind) {
    WeatherKind.RECOVERING -> "恢复"
    WeatherKind.ENERGY_HIGH -> "高能"
    WeatherKind.PRESSURE -> "压力"
    WeatherKind.BALANCED -> "平衡"
    WeatherKind.SENSITIVE -> "敏感"
}

/**
 * 日历格里的拉丁文简称。
 *
 * 拉丁文的天气全名（`Recuperatio` / `Sensibilitas`…）在 1/7 屏宽的格子里必然放不下，
 * 所以另给一套 ≤3 字符的简称——**这几个串不参与翻译**（见 [WeekWeatherStrip] 用 [RawText]），
 * 免得再走一遍词条表、还往漏翻审计里塞假阳性。
 */
internal val LATIN_WEATHER_SHORT = mapOf(
    WeatherKind.RECOVERING to "REC",
    WeatherKind.ENERGY_HIGH to "VIS",
    WeatherKind.PRESSURE to "PRE",
    WeatherKind.BALANCED to "AEQ",
    WeatherKind.SENSITIVE to "SEN"
)

/**
 * 星期几的拉丁文三字简称。
 *
 * [EchoGenerator.weekday] 返回的是中文单字「一…日」——这些单字在词条表里登记过
 * （`一` → `Mon`），**走翻译层会被翻成英语/日语的星期名**。拉丁文这里直接按单字索引取
 * 拉丁简称，用 [RawText] 渲染，不再经过翻译层，避免「日」「一」这种单字键在别处被误翻。
 */
internal val LATIN_WEEKDAY_SHORT = mapOf(
    "一" to "Lun", "二" to "Mar", "三" to "Mer", "四" to "Iov",
    "五" to "Ven", "六" to "Sat", "日" to "Sol"
)

/**
 * 日历格最多放得下几个字符。中文两字简称在这个宽度里刚好，
 * 英文的 "Sensitive" / "High-energy" 放不下——放不下就不显示，
 * 由下方图例把颜色对应回名字，绝不显示截断的半截词。
 */
private const val WEATHER_LABEL_MAX_CHARS = 3

/**
 * 一周的身体天气日历。
 *
 * 没记录的那天画成空槽、写一个破折号——**不补、不猜、不留白给想象**。
 */
@Composable
fun WeekWeatherStrip(
    days: List<WeekCalendar.DaySlot>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 拉丁文的星期名要绕开翻译层：`一…日` 这些单字键在词条表里是
                // 「Monday…」那一套，翻出来是英文。见 [LATIN_WEEKDAY_SHORT]。
                val latin = AppLanguage.current() == AppLanguage.LATIN
                if (latin) {
                    RawText(
                        text = LATIN_WEEKDAY_SHORT[day.label] ?: day.label,
                        fontSize = SkinTokens.tabLabel,
                        color = SkinTokens.muted,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = day.label,
                        fontSize = SkinTokens.tabLabel,
                        color = SkinTokens.muted,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (day.kind == null) {
                                SkinTokens.glassTrack
                            } else {
                                Color(day.kind.colorHex).copy(alpha = 0.88f)
                            }
                        )
                )
                Spacer(Modifier.height(5.dp))
                // 日历格只有 1/7 屏宽：中文两字简称放得下，英文的
                // "Sensitive"/"High-energy" 一定会被截断成 "Sensiti"。
                // 所以只在文字确实放得下时才显示，放不下就只留圆点，
                // 由下方「颜色说明」负责把颜色对应回名字。
                // 拉丁文单给一套 ≤3 字符的简称，见 [LATIN_WEATHER_SHORT]。
                val short = day.kind?.let { if (latin) LATIN_WEATHER_SHORT[it] else weatherShortLabel(it) }
                val shown = when {
                    day.kind == null -> "—"
                    short != null && short.length <= WEATHER_LABEL_MAX_CHARS -> short
                    else -> ""
                }
                if (latin) {
                    RawText(
                        text = shown,
                        fontSize = SkinTokens.tabLabel,
                        color = if (day.kind == null) SkinTokens.faint else SkinTokens.ink,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = shown,
                        fontSize = SkinTokens.tabLabel,
                        color = if (day.kind == null) SkinTokens.faint else SkinTokens.ink,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5 回声图表
// ---------------------------------------------------------------------------

/**
 * 定性档位 → 颜色。
 *
 * 刻意用**同一个色相的深浅**，而不是红黄绿：
 * 一旦用了交通灯配色，「阴」就会被读成「坏」，用户就开始想把它变绿。
 *
 * ⚠️ **这里的 `word` 是中文词当键用，从不渲染上屏**（只有 `EchoGenerator.echoSeries`
 * 内部按天气类型反推的那一组定性词会传进来）。**绝对不要跟着语言翻译它**：
 * 一旦翻译，所有词都会落到 `else` 分支，整张图会变成一条 alpha=0.10 的平线。
 * 相关的渲染在 [EchoTrendBand]，那里也只用它算透明度。
 */
private fun levelAlpha(word: String): Float = when (word) {
    "晴", "无风", "偏湿", "适中" -> 0.88f
    "少云", "微风", "薄雾" -> 0.60f
    "多云", "有风", "偏干", "有雾" -> 0.40f
    "阴", "风大", "很干", "厚雾" -> 0.24f
    else -> 0.10f
}

/**
 * 回声图表。
 *
 * 每行是一条定性带，没有纵轴、没有刻度、没有数值标签——
 * 它只回答「这周大概长什么样」，不回答「哪天比哪天好」。
 */
@Composable
fun EchoTrendBand(
    series: List<EchoSeries>,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty()) return

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        series.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    modifier = Modifier.width(66.dp),
                    fontSize = SkinTokens.tabLabel,
                    color = SkinTokens.muted,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.points.forEach { (_, word) ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    SkinTokens.accent.copy(alpha = levelAlpha(word))
                                )
                        )
                    }
                }
            }
        }
    }
}
