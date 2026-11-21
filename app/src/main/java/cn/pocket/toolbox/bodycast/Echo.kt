package cn.pocket.toolbox.bodycast

import java.time.LocalDate

/** 每天一条天气记录。只记天气类型和校准标签，不存任何原始数值。 */
data class WeatherRecord(
    val date: String,
    val kind: WeatherKind,
    val calibrated: Set<CalibrationTag> = emptySet()
)

/**
 * 周天气日历。
 *
 * 用天气图标铺满七天，看起来像天气预报——
 * 这正是重点：它不会显示「你完成了几天」，因为没有可完成的东西。
 */
data class WeekCalendar(val days: List<DaySlot>) {
    data class DaySlot(val date: LocalDate, val kind: WeatherKind?, val label: String)

    val recordedCount: Int get() = days.count { it.kind != null }
}

/** 月视图的四个周摘要：只给词和方向，不显示数字指标。 */
data class MonthWeekSummary(
    val label: String,
    val recordedCount: Int,
    val dominant: WeatherKind?
) {
    val fraction: Float get() = (recordedCount / 7f).coerceIn(0f, 1f)
}

/** 身体来信里的一句。 */
data class LetterLine(val text: String, val emphasis: Boolean = false)

/** 身体来信。 */
data class BodyLetter(
    val weekLabel: String,
    val title: String,
    val lines: List<LetterLine>,
    val signature: String
)

/**
 * 回声图表的一条曲线。
 *
 * [points] 是「日期 → 档位词」，**不是数值**。
 * 页面把它画成一条起伏的带子而不是折线，避免被读成涨跌。
 */
data class EchoSeries(
    val metricKey: String,
    val title: String,
    val points: List<Pair<String, String>>,
    /** 整周的总体走向描述 */
    val trend: String
)

/** 分享卡片的内容。 */
data class ShareCard(
    val title: String,
    val kind: WeatherKind,
    val lines: List<String>,
    val poem: String,
    val footer: String
)

/**
 * 回声生成器。
 *
 * 全部基于本地记录推导，不联网、不做用户间比较。
 * 没有帖子、没有别人——这一页只回放用户自己的记录。
 */
object EchoGenerator {

    // ---------------------------------------------------------------------
    // 周天气日历
    // ---------------------------------------------------------------------

    fun weekCalendar(records: Map<String, WeatherRecord>, today: LocalDate): WeekCalendar {
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            val record = records[key]
            WeekCalendar.DaySlot(
                date = date,
                kind = record?.kind,
                label = weekday(date)
            )
        }
        return WeekCalendar(days)
    }

    /** 最近 28 天按四周聚合：用于回声页的「这个月」视图。 */
    fun monthCalendar(records: Map<String, WeatherRecord>, today: LocalDate): List<MonthWeekSummary> =
        (3 downTo 0).map { week ->
            val end = today.minusDays((week * 7).toLong())
            val kinds = (0..6).mapNotNull { offset -> records[end.minusDays(offset.toLong()).toString()]?.kind }
            val dominant = kinds.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            MonthWeekSummary(
                label = "${end.minusDays(6).monthValue}/${end.minusDays(6).dayOfMonth}–${end.monthValue}/${end.dayOfMonth}",
                recordedCount = kinds.size,
                dominant = dominant
            )
        }

    private fun weekday(date: LocalDate): String =
        listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]

    // ---------------------------------------------------------------------
    // 身体来信
    // ---------------------------------------------------------------------

    /**
     * 把一周的天气写成一封短信。
     *
     * 语气刻意平淡：不表扬、不惋惜、不催。
     * 记录得少的时候也不会出现「你只记了 2 天」这种话。
     */
    fun bodyLetter(records: Map<String, WeatherRecord>, today: LocalDate, nickname: String): BodyLetter {
        val weekKeys = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
        val weekRecords = weekKeys.mapNotNull { records[it] }

        val counts = weekRecords.groupingBy { it.kind }.eachCount()
        val dominant = counts.maxByOrNull { it.value }?.key

        val lines = mutableListOf<LetterLine>()

        if (weekRecords.isEmpty()) {
            lines += LetterLine("这一周还没有留下天气记录。")
            lines += LetterLine("什么时候想记，再来就好。")
            return BodyLetter(
                weekLabel = weekLabel(today),
                title = "这周还没有回声",
                lines = lines,
                signature = signature(nickname)
            )
        }

        lines += LetterLine("这周你留下了 ${weekRecords.size} 天的天气。")

        dominant?.let { kind ->
            lines += LetterLine("其中最多的是${kind.label}。", emphasis = true)
            lines += LetterLine(
                when (kind) {
                    WeatherKind.RECOVERING -> "你有一段日子在收着过。这不叫落后。"
                    WeatherKind.ENERGY_HIGH -> "有几天你是有余量的，希望那几天被用在了想做的事上。"
                    WeatherKind.PRESSURE -> "紧的时候比较多。你已经处理得比你以为的好。"
                    WeatherKind.BALANCED -> "大部分时候是稳的。稳这件事，本身就不容易。"
                    WeatherKind.SENSITIVE -> "有几天你选择了听身体的。那需要一点勇气。"
                }
            )
        }

        val calibratedDays = weekRecords.count { it.calibrated.isNotEmpty() }
        if (calibratedDays > 0) {
            val tags = weekRecords.flatMap { it.calibrated }.groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key
            if (tags != null) {
                lines += LetterLine("「${tags.label}」出现了 ${weekRecords.count { tags in it.calibrated }} 次，是你这周最常说的感受。")
            }
        }

        val kinds = weekRecords.map { it.kind }.distinct()
        lines += LetterLine(
            when {
                kinds.size >= 4 -> "天气换了好几种，这很常见，不用从中找规律。"
                kinds.size <= 1 -> "天气几乎没变。不变也是信息。"
                else -> "有起伏，但都在你能走过去的范围里。"
            }
        )

        return BodyLetter(
            weekLabel = weekLabel(today),
            title = "这周的身体来信",
            lines = lines,
            signature = signature(nickname)
        )
    }

    private fun weekLabel(today: LocalDate): String {
        val start = today.minusDays(6)
        return "${start.monthValue}月${start.dayOfMonth}日 — ${today.monthValue}月${today.dayOfMonth}日"
    }

    private fun signature(nickname: String): String =
        if (nickname.isBlank()) "BodyCast" else "BodyCast · $nickname"

    // ---------------------------------------------------------------------
    // 回声图表
    // ---------------------------------------------------------------------

    /**
     * 把一周的天气折成四条定性曲线。
     *
     * 注意这里没有「进步/退步」的判断——[trend] 只描述形态。
     */
    fun echoSeries(records: Map<String, WeatherRecord>, today: LocalDate): List<EchoSeries> {
        val keys = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
        val labels = keys.map { key ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull()
            date?.let { "${it.monthValue}/${it.dayOfMonth}" } ?: key
        }

        // 记录里只有天气类型，没有分项数值，所以这里按天气类型反推一组定性档位。
        // 这是有意的：回声图表要看起来像天气带，不能像体检报告。
        fun seriesFor(key: String, title: String, levelOf: (WeatherKind) -> String): EchoSeries {
            val points = keys.mapIndexed { index, dateKey ->
                val kind = records[dateKey]?.kind
                labels[index] to (kind?.let(levelOf) ?: "—")
            }
            val known = points.mapNotNull { it.second.takeIf { s -> s != "—" } }
            val trend = when {
                known.isEmpty() -> "这周还没有足够记录"
                known.distinct().size == 1 -> "一周里基本没变"
                else -> "中间有起伏"
            }
            return EchoSeries(key, title, points, trend)
        }

        return listOf(
            seriesFor(BodyWeather.KEY_RECOVERY, "恢复云量") { kind ->
                when (kind) {
                    WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED -> "晴"
                    WeatherKind.SENSITIVE -> "少云"
                    WeatherKind.PRESSURE -> "多云"
                    WeatherKind.RECOVERING -> "阴"
                }
            },
            seriesFor(BodyWeather.KEY_PRESSURE, "压力风速") { kind ->
                when (kind) {
                    WeatherKind.BALANCED, WeatherKind.ENERGY_HIGH -> "无风"
                    WeatherKind.SENSITIVE -> "微风"
                    WeatherKind.RECOVERING -> "有风"
                    WeatherKind.PRESSURE -> "风大"
                }
            },
            seriesFor(BodyWeather.KEY_ENERGY, "能量湿度") { kind ->
                when (kind) {
                    WeatherKind.ENERGY_HIGH -> "偏湿"
                    WeatherKind.BALANCED -> "适中"
                    WeatherKind.SENSITIVE, WeatherKind.PRESSURE -> "偏干"
                    WeatherKind.RECOVERING -> "很干"
                }
            },
            seriesFor(BodyWeather.KEY_SLEEP, "睡眠云层") { kind ->
                when (kind) {
                    WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED -> "晴"
                    WeatherKind.SENSITIVE -> "薄雾"
                    WeatherKind.PRESSURE -> "有雾"
                    WeatherKind.RECOVERING -> "厚雾"
                }
            }
        )
    }

    // ---------------------------------------------------------------------
    // 分享卡片
    // ---------------------------------------------------------------------

    fun shareCard(weather: BodyWeather, nickname: String): ShareCard {
        val lines = buildList {
            add(weather.broadcast.headline)
            add(weather.broadcast.body)
            weather.calibrated.takeIf { it.isNotEmpty() }?.let {
                add("我手动校准了「${it.joinToString("、") { tag -> tag.label }}」")
            }
        }
        return ShareCard(
            title = if (nickname.isBlank()) "我的身体天气" else "$nickname 的身体天气",
            kind = weather.kind,
            lines = lines,
            poem = weather.broadcast.poem,
            // 卡片底部不出现任何数值、评分或日期统计
            footer = "BodyCast · 不评分，只播报"
        )
    }
}

/**
 * 模式洞察（22.32.0）：从本地记录里说几句「它注意到什么」。
 *
 * 产品口径（这是一组文案纪律，改文案前先读）：
 * - **只说观察，不下诊断**：说「你这周有 3 个压力天」，不说「你压力过大」；
 * - **数据不够就不说**：样本太少的洞察宁可不出现，也不硬凑；
 * - **没有分数、没有排名、没有与他人比较**——洞察只用用户自己的记录；
 * - 每条都是一句人话，最多三条，宁缺毋滥。
 *
 * 纯函数、无副作用：输入记录、输出句子，方便单测锁口径。
 */
object EchoInsights {

    /** 最少要有这么多天的记录才开始说洞察（否则什么都不说）。 */
    private const val MIN_DAYS = 5

    /**
     * 生成 0–3 条洞察（中文源串，走 [cn.pocket.toolbox.i18n.BodyCastText] 翻译）。
     *
     * 生成顺序即优先级：先说「这一周的主旋律」，再说「星期规律」，最后说
     * 「感觉和天气的关系」。每条各自有门槛，不满足就跳过。
     */
    fun generate(records: Map<String, WeatherRecord>, today: java.time.LocalDate): List<String> {
        val weekKeys = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
        val week = weekKeys.mapNotNull { records[it] }
        if (records.size < MIN_DAYS) return emptyList()

        val out = mutableListOf<String>()

        // 1) 这一周的主旋律：出现最多的天气 ≥3 天才说。
        val dominant = week.groupingBy { it.kind }.eachCount()
            .filterValues { it >= 3 }
            .maxByOrNull { it.value }
        if (dominant != null) {
            val n = dominant.value.toString()
            out += when (dominant.key) {
                WeatherKind.PRESSURE -> "这一周，你有 $n 天是压力天——风大的日子里，先把肩膀放下来。"
                WeatherKind.RECOVERING -> "这一周，你有 $n 天是恢复天——身体在慢慢补，不用急着赶路。"
                WeatherKind.SENSITIVE -> "这一周，你有 $n 天是敏感天——听它说完，比问它为什么更重要。"
                WeatherKind.ENERGY_HIGH -> "这一周，你有 $n 天是高能天——阳光好的日子，记得也留一点给明天。"
                WeatherKind.BALANCED -> "这一周，你有 $n 天是平衡天——稳，本身就是一件好事。"
            }
        }

        // 2) 星期规律：同一天（星期几）≥2 次且全是同一类天气才说。
        val byWeekday = records.entries
            .mapNotNull { (date, rec) ->
                val day = runCatching { java.time.LocalDate.parse(date) }.getOrNull()?.dayOfWeek
                    ?: return@mapNotNull null
                day to rec.kind
            }
            .groupBy({ it.first }, { it.second })
        val pattern = byWeekday.entries.firstOrNull { (_, kinds) ->
            kinds.size >= 2 && kinds.distinct().size == 1
        }
        if (pattern != null) {
            val weekday = weekdayName(pattern.key)
            val kind = pattern.value.first().label
            out += "看起来${weekday}常常是$kind——只是你自己的规律，不是规定。"
        }

        // 3) 感觉与天气：某个校准标签出现 ≥2 次，且都在同一类天气下才说。
        val tagKinds = mutableMapOf<String, MutableList<WeatherKind>>()
        records.values.forEach { rec ->
            rec.calibrated.forEach { tag ->
                tagKinds.getOrPut(tag.name) { mutableListOf() }.add(rec.kind)
            }
        }
        val tagPattern = tagKinds.entries.firstOrNull { (_, kinds) ->
            kinds.size >= 2 && kinds.distinct().size == 1
        }
        if (tagPattern != null) {
            val label = runCatching {
                CalibrationTag.fromId(tagPattern.key)?.label
            }.getOrNull() ?: tagPattern.key
            out += "你每次说「$label」，天气都是${tagPattern.value.first().label}——身体的信号，常常是连着的。"
        }

        return out.take(3)
    }

    private fun weekdayName(day: java.time.DayOfWeek): String = when (day) {
        java.time.DayOfWeek.MONDAY -> "周一"
        java.time.DayOfWeek.TUESDAY -> "周二"
        java.time.DayOfWeek.WEDNESDAY -> "周三"
        java.time.DayOfWeek.THURSDAY -> "周四"
        java.time.DayOfWeek.FRIDAY -> "周五"
        java.time.DayOfWeek.SATURDAY -> "周六"
        java.time.DayOfWeek.SUNDAY -> "周日"
    }
}
