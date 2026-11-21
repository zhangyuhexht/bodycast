package cn.pocket.toolbox.ui.bodycast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.bodycast.ActionLibrary
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.CalibrationTag
import cn.pocket.toolbox.bodycast.EchoGenerator
import cn.pocket.toolbox.bodycast.EchoInsights
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.bodycast.WeatherRecord
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.ui.skin.SkinTokens
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import java.time.LocalDate
import kotlin.math.abs

/**
 * 页面三：身体回声（8.0.0 起是独立 tab，不再是浮层）。
 *
 * 放在「后面查看」的位置：它不是行动入口，是回放——这一周身体经历过什么。
 * 板块按回放顺序排：
 *
 * 1. 周天气日历 —— 这一周身体是什么天气
 * 2. 身体来信   —— 一周一封，替身体说话
 * 3. 天气诗     —— 一句短诗，随天气变
 * 4. 回声图表   —— 定性趋势，没有数值坐标轴
 * 5. 分享卡片   —— 截图就能发出去
 *
 * 「回声统计」「本周对照」两张卡排在日历之后、来信之前，用已经记下的日子数出
 * 天数：只描述，不评分，也不设达标线。
 *
 * 21.0.0 移除了匿名天气墙（发帖、点赞、帖子列表整块去掉）：
 * 这一页只回放用户自己的记录，不再有「像社交」的东西。
 *
 * 底部放设置入口：这一页都是「看」的内容，改偏好属于低频操作，收到最后一行。
 */

@Composable
fun EchoTab(
    backdrop: Backdrop,
    store: BodyCastStore,
    onOpenSettings: () -> Unit
) {
    var period by remember { mutableIntStateOf(0) }

    CastPage {
        item {
            CastPageHeader(
                title = "身体回声",
                subtitle = if (period == 0) "这一周，身体经历过什么。它不评价，只是回放。"
                else "这个月，身体经历过什么。它不评价，只是回放。"
            )
        }
        // ---- 今日微行动（22.49.0 从首页搬到这里）----
        // 用户连着三次说「微行动不在回声页面」，所以它就是这一页最上面的一张卡：
        // 不再折叠、点开才看，进来直接是处方与「开始」按钮。
        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = "今日微行动",
                    fontSize = SkinTokens.sectionTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = SkinTokens.ink
                )
                val suggestion = store.prescription?.items?.firstOrNull()?.title
                val doneToday = store.actionLog[store.todayKey].orEmpty()
                    .count { ActionLibrary.byId(it) != null }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        doneToday > 0 && suggestion != null ->
                            "今天已经做完 $doneToday 件；还想做的话，可以先做这一件：$suggestion"
                        suggestion != null -> "今天可以先做这一件：$suggestion"
                        else -> "几分钟能做完的一件小事。"
                    },
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    color = SkinTokens.muted
                )
                Spacer(Modifier.height(8.dp))
                PrescriptionSection(backdrop = backdrop, store = store)
                store.prescription?.note?.takeIf { it.isNotBlank() }?.let { note ->
                    CastHint(note)
                }
            }
        }

        item {
            GlassSegmented(
                options = listOf("这一周", "这个月"),
                selectedIndex = period,
                onSelect = { period = it },
                backdrop = backdrop
            )
        }

        if (period == 0) {
            item { EchoCalendarSection(backdrop, store) }
        } else {
            item { EchoMonthCalendarSection(backdrop, store) }
        }

        // 两张统计卡排在日历之后：分段控件的切换目标始终是日历本身，
        // 卡片插在中间会让「这一周 / 这个月」看起来也在管它们。
        // 22.30.0：「本周你照顾自己 N 次」排在统计之前——这是回声页唯一的
        // 正向计数（没有目标值、没有连击），放在最前面给这一页定调。
        // 22.51.0（老师意见「有点复杂」）：这一页原来是一长串卡，
        // 现在按三组分：这一周的样子 / 它注意到 / 留着或发出去。
        item { CastGroupLabel("这一周的样子") }
        item { EchoCareSection(backdrop, store) }
        item { EchoActionSection(backdrop, store) }
        item { EchoMealSection(backdrop, store) }
        item { EchoStatsSection(backdrop, store) }
        item { EchoWeekCompareSection(backdrop, store) }
        item { CastGroupLabel("它注意到") }
        item { EchoInsightSection(backdrop, store) }

        item { EchoLetterSection(backdrop, store) }
        item { EchoPoemSection(backdrop, store) }
        item { CastGroupLabel("留着或发出去") }
        item { EchoChartSection(backdrop, store) }
        item { EchoShareSection(backdrop, store) }

        // ---- 设置入口：低频操作放最后 ----
        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "设置与偏好",
                subtitle = "数据源、校准、提醒、外观，都在设置页。",
                icon = "settings",
                // 22.5.0：页面最底部的行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = onOpenSettings
            )
        }

        item {
            CastHint("身体回声只存在这台手机上。它不排名，也不和任何人比较。")
        }
    }
}

// ---------------------------------------------------------------------------
// 月天气回放
// ---------------------------------------------------------------------------

@Composable
private fun EchoMonthCalendarSection(backdrop: Backdrop, store: BodyCastStore) {
    val weeks = remember(store.records, store.today) {
        EchoGenerator.monthCalendar(store.records, store.today)
    }
    CastCard(backdrop = backdrop) {
        Text("这个月的身体天气", fontSize = SkinTokens.sectionTitle, fontWeight = FontWeight.SemiBold, color = SkinTokens.ink)
        Spacer(Modifier.height(6.dp))
        Text("按四周回放，只看方向，不做排名。", fontSize = SkinTokens.caption, color = SkinTokens.muted)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            weeks.forEach { week ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(week.label, modifier = Modifier.weight(1f), fontSize = SkinTokens.caption, color = SkinTokens.muted)
                        Text(
                            week.dominant?.label ?: "暂时没读到",
                            fontSize = SkinTokens.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = week.dominant?.let { Color(it.colorHex) } ?: SkinTokens.faint
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SkinTokens.glassTrack)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(week.fraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background((week.dominant?.let { Color(it.colorHex) } ?: SkinTokens.faint).copy(alpha = 0.65f))
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 回声统计 / 本周对照
// ---------------------------------------------------------------------------

/**
 * 本周你照顾自己 N 次（22.30.0）。
 *
 * 回声页唯一的**正向计数**：每做完一件微行动就 +1，按周求和。
 * 没有目标值、没有连击、漏做不扣——铁律不变，这里只是把
 * 「你这周照顾过自己几次」如实说一句，给这一页定一个温和的调子。
 */
@Composable
private fun EchoCareSection(backdrop: Backdrop, store: BodyCastStore) {
    // careLog 变了要跟着变，所以 key 里带上它。
    val count = remember(store.careLog, store.today) { store.careCountInLastDays(7) }

    CastCard(backdrop = backdrop) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SkinTokens.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                GlassIcon("heart", SkinTokens.accent, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "本周你照顾自己 $count 次。",
                    fontSize = SkinTokens.cardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "做完一件微行动就记一次。没有目标，漏做也不扣——只是让你看见。",
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    color = SkinTokens.muted
                )
            }
        }
    }
}

/**
 * 模式洞察（22.32.0）：本地记录里说几句「它注意到什么」。
 *
 * 句子全部来自 [EchoInsights.generate]（纯函数，口径单测锁着）：
 * 只说观察、不下诊断，数据不够就整卡不出现。
 */
@Composable
private fun EchoInsightSection(backdrop: Backdrop, store: BodyCastStore) {
    val insights = remember(store.records, store.today) {
        EchoInsights.generate(store.records, store.today)
    }
    if (insights.isEmpty()) return

    CastCard(backdrop = backdrop) {
        Text(
            text = "它注意到",
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "只说观察，不下诊断。记录多了，这里会更懂你。",
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            insights.forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SkinTokens.accent)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = line,
                        fontSize = SkinTokens.body,
                        lineHeight = 26.sp,
                        color = SkinTokens.ink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 这周吃了什么（22.32.0 食谱闭环）。
 * 食谱页按「就吃这个」记下来的东西在这里按天回放——只列名字，
 * 不评价吃得好不好、不算营养分。空记录不摆卡片，只给一行提示。
 */
@Composable
private fun EchoMealSection(backdrop: Backdrop, store: BodyCastStore) {
    val days = remember(store.mealLog, store.today) { store.mealsInLastDays(7) }

    if (days.isEmpty()) {
        CastHint("这周还没有记过「就吃这个」。在食谱页的三餐卡片上按一下，这里就能看到。")
        return
    }

    CastCard(backdrop = backdrop) {
        Text(
            text = "这周吃了什么",
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "只是把按过的记下来，不评价，也不打分。",
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            days.forEach { (date, names) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = BodyCastText.text("今天").let { todayWord ->
                            if (date == store.todayKey) todayWord else date.takeLast(5)
                        },
                        fontSize = SkinTokens.caption,
                        fontWeight = FontWeight.Medium,
                        color = SkinTokens.accent,
                        modifier = Modifier.width(52.dp)
                    )
                    Text(
                        text = names.joinToString("、"),
                        fontSize = SkinTokens.caption,
                        lineHeight = 23.sp,
                        color = SkinTokens.ink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 这周做过的小事（22.49.0）：微行动在回声页里的落点。
 *
 * 22.30.0 起回声页只有一个「本周你照顾自己 N 次」——数字涨了，却想不起
 * 做过什么。这一块按天列出这一周点完的动作（动作名、几分钟），
 * 仍然是回声页的那条纪律：**只记录，不评价，没有目标、没有连击**。
 */
@Composable
private fun EchoActionSection(backdrop: Backdrop, store: BodyCastStore) {
    val days = remember(store.actionLog, store.today) { store.actionsInLastDays(7) }

    if (days.isEmpty()) {
        CastHint("这周还没有做完过微行动。首页那张「今日微行动」卡点开、做完一件，这里就会留下记录。")
        return
    }

    CastCard(backdrop = backdrop) {
        Text(
            text = "这周做过的小事",
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "只是把点完的记下来：没有目标，也没有连击。",
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 新的在前：最近做过什么，一眼看见。
            days.reversed().forEach { (date, ids) ->
                val names = ids.mapNotNull { id -> ActionLibrary.byId(id)?.title }
                if (names.isEmpty()) return@forEach
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = if (date == store.todayKey) BodyCastText.text("今天") else date.takeLast(5),
                        fontSize = SkinTokens.caption,
                        fontWeight = FontWeight.Medium,
                        color = SkinTokens.accent,
                        modifier = Modifier.width(52.dp)
                    )
                    Text(
                        text = names.joinToString("、"),
                        fontSize = SkinTokens.caption,
                        lineHeight = 23.sp,
                        color = SkinTokens.ink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 回声统计。
 *
 * 只把已经记下的日子数出来：记了多少天、连着记了多久、最多的天气和最多的感觉。
 * 没有目标值，也没有达标线——数字在这里只是把日子说清楚。
 * 记录少于三天时不摆空行，换一句引导。
 */
@Composable
private fun EchoStatsSection(backdrop: Backdrop, store: BodyCastStore) {
    val rows = remember(store.records, store.today) { echoStats(store.records, store.today) }

    CastCard(backdrop = backdrop) {
        Text(
            text = "回声统计",
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "把记下的日子数出来。没有目标值，也没有达标线。",
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(16.dp))

        if (rows.isEmpty()) {
            Text(
                text = "记录还很少——回声会在你多记几天之后慢慢清楚起来。",
                fontSize = SkinTokens.body,
                lineHeight = 28.sp,
                color = SkinTokens.muted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEach { row -> EchoStatLine(row) }
            }
        }
    }
}

/** 统计行：左边大字号数字，右边小字说明。 */
@Composable
private fun EchoStatLine(row: EchoStatRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.value,
            // 跟着设置里的「字体大小」一起缩放，所以从 display 那一档派生。
            fontSize = SkinTokens.display * 0.68f,
            fontWeight = FontWeight.Bold,
            color = SkinTokens.ink,
            maxLines = 1
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = row.title,
                fontSize = SkinTokens.caption,
                fontWeight = FontWeight.Medium,
                color = SkinTokens.ink
            )
            row.detail?.let { detail ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detail,
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    color = SkinTokens.muted
                )
            }
        }
    }
}

/** 「回声统计」里的一行。 */
private data class EchoStatRow(
    /** 大字号那一格，例如「12 天」。 */
    val value: String,
    /** 小字说明，例如「记录天数」。 */
    val title: String,
    /** 补充说明，没有就不占这一行。 */
    val detail: String? = null
)

/**
 * 用已有的天气记录算出统计行。
 *
 * 总天数不到三天时返回空列表——界面会换一句引导，而不是摆一张空卡片。
 */
private fun echoStats(records: Map<String, WeatherRecord>, today: LocalDate): List<EchoStatRow> {
    if (records.size < 3) return emptyList()

    fun countsOf(pick: (WeatherRecord) -> Boolean): Map<WeatherKind, Int> =
        records.values.filter(pick).groupingBy { it.kind }.eachCount()

    fun dateOf(record: WeatherRecord): LocalDate? =
        runCatching { LocalDate.parse(record.date) }.getOrNull()

    val thisMonth = countsOf { record ->
        val date = dateOf(record)
        date != null && date.year == today.year && date.monthValue == today.monthValue
    }

    // 本月还没有记录时退回全部记录，标题也跟着换——不把「本月」硬安在全量数据上。
    val scoped = if (thisMonth.isNotEmpty()) thisMonth else countsOf { true }
    val scopeLabel = if (thisMonth.isNotEmpty()) "本月" else "记录里"
    val top = scoped.entries
        .sortedWith(
            compareByDescending<Map.Entry<WeatherKind, Int>> { it.value }
                .thenBy { it.key.ordinal }
        )
        .take(2)

    val rows = mutableListOf(
        EchoStatRow(value = "${records.size} 天", title = "记录天数"),
        EchoStatRow(value = "${streakDays(records, today)} 天", title = "连续记录", detail = "从今天往回数")
    )

    top.firstOrNull()?.let { first ->
        rows += EchoStatRow(
            value = "${first.value} 天",
            title = "${scopeLabel}最多的天气 · ${first.key.label}",
            detail = top.getOrNull(1)?.let { "其次是 ${it.key.label}，${it.value} 天" }
        )
    }

    topCalibrationTag(records)?.let { (tag, days) ->
        rows += EchoStatRow(value = "$days 天", title = "最常说的感觉 · ${tag.label}")
    }

    return rows
}

/** 从今天往回数，连续有记录的天数；断一天就停。 */
private fun streakDays(records: Map<String, WeatherRecord>, today: LocalDate): Int {
    var days = 0
    var date = today
    while (records.containsKey(date.toString())) {
        days += 1
        date = date.minusDays(1)
    }
    return days
}

/** 出现天数最多的校准标签；一份标签都没有时返回 null，界面省略这一行。 */
private fun topCalibrationTag(records: Map<String, WeatherRecord>): Pair<CalibrationTag, Int>? {
    val counts = records.values.flatMap { it.calibrated }.groupingBy { it }.eachCount()
    // 按展示顺序取最大：次数打平时，先后和校准标签区保持一致。
    return CalibrationTag.DISPLAY_ORDER
        .mapNotNull { tag -> counts[tag]?.let { count -> tag to count } }
        .maxByOrNull { it.second }
}

/**
 * 本周对照。
 *
 * 最近七天和上一个七天比天数，只挑变化最明显的两类。
 * 句式是「上周 N 天 → 本周 M 天」：不换算百分比，也不给好坏。
 */
@Composable
private fun EchoWeekCompareSection(backdrop: Backdrop, store: BodyCastStore) {
    val compare = remember(store.records, store.today) { weekCompare(store.records, store.today) }

    CastCard(backdrop = backdrop) {
        Text(
            text = "本周对照",
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "最近七天和上一个七天比一比，只数天数，不做评价。",
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(16.dp))

        if (compare.lines.isEmpty()) {
            Text(
                text = compare.note,
                fontSize = SkinTokens.body,
                lineHeight = 28.sp,
                color = SkinTokens.muted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                compare.lines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = SkinTokens.body,
                        lineHeight = 26.sp,
                        color = SkinTokens.ink
                    )
                }
            }
        }
    }
}

/** 「本周对照」卡的结论：最多两行对照，或者一句说明。 */
private class WeekCompare(
    /** 变化最明显的两类，最多两行；空表示这两周没有可比的变化。 */
    val lines: List<String>,
    /** [lines] 为空时要说的一句话。 */
    val note: String = ""
)

/**
 * 最近七天和上一个七天各自数一遍天气种类，按次数差排出变化最大的两类。
 *
 * 只数天数：没有百分比，也没有评分。两侧都还没记录时给一句如实说明，
 * 而不是硬摆一个「0 → 0」的对照。
 */
private fun weekCompare(records: Map<String, WeatherRecord>, today: LocalDate): WeekCompare {
    fun countsOf(from: LocalDate, to: LocalDate): Map<WeatherKind, Int> {
        val counts = mutableMapOf<WeatherKind, Int>()
        var date = from
        while (!date.isAfter(to)) {
            records[date.toString()]?.let { counts[it.kind] = (counts[it.kind] ?: 0) + 1 }
            date = date.plusDays(1)
        }
        return counts
    }

    val lastWeek = countsOf(today.minusDays(13), today.minusDays(7))
    val thisWeek = countsOf(today.minusDays(6), today)

    if (lastWeek.isEmpty() && thisWeek.isEmpty()) {
        return WeekCompare(emptyList(), "这两周记录还太少，对比不出来。")
    }

    val lines = WeatherKind.entries
        .map { kind -> Triple(kind, lastWeek[kind] ?: 0, thisWeek[kind] ?: 0) }
        .filter { (_, before, now) -> before != now }
        .sortedWith(
            compareByDescending<Triple<WeatherKind, Int, Int>> { abs(it.third - it.second) }
                .thenByDescending { it.third }
        )
        .take(2)
        .map { (kind, before, now) -> "${kind.label}：上周 $before 天 → 本周 $now 天" }

    return if (lines.isEmpty()) {
        WeekCompare(emptyList(), "这两周几乎一样，没有哪一类明显变多或变少。")
    } else {
        WeekCompare(lines)
    }
}

// ---------------------------------------------------------------------------
// 1 周天气日历
// ---------------------------------------------------------------------------

@Composable
private fun EchoCalendarSection(backdrop: Backdrop, store: BodyCastStore) {
    val calendar = remember(store.records, store.today) {
        EchoGenerator.weekCalendar(store.records, store.today)
    }

    // 两张卡片都挂在这一个 LazyColumn item 里。页面级的 cardGap 只在 item 之间生效，
    // item 内部不给间距，所以这里自己补一列卡片间距，和页面其它区块保持一致。
    Column(verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)) {
        CastCard(backdrop = backdrop) {
            Text(
                text = "这一周的身体天气",
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "记了 ${calendar.recordedCount} 天。没记的日子就是没记，不补、不猜。",
                fontSize = SkinTokens.caption,
                lineHeight = 22.sp,
                color = SkinTokens.muted
            )
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                calendar.days.forEach { day ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 拉丁文绕开翻译层：`一…日` 这些单字在词条表里是英文的星期名。
                        // 与 [cn.pocket.toolbox.ui.bodycast.WeekWeatherStrip] 同一套简称。
                        val latin = AppLanguage.current() == AppLanguage.LATIN
                        if (latin) {
                            RawText(
                                text = LATIN_WEEKDAY_SHORT[day.label] ?: day.label,
                                fontSize = SkinTokens.caption,
                                color = SkinTokens.muted,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = day.label,
                                fontSize = SkinTokens.caption,
                                color = SkinTokens.muted,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val kind = day.kind
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (kind == null) {
                                        SkinTokens.glassTrack
                                    } else {
                                        kind.color().copy(alpha = 0.85f)
                                    }
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        val short = kind?.let { if (latin) LATIN_WEATHER_SHORT[it] else shortLabel(it) }
                        if (latin) {
                            RawText(
                                text = short ?: "—",
                                fontSize = SkinTokens.caption,
                                color = if (kind == null) SkinTokens.faint else SkinTokens.ink,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = short ?: "—",
                                fontSize = SkinTokens.caption,
                                color = if (kind == null) SkinTokens.faint else SkinTokens.ink,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 图例：让用户知道颜色都代表什么，而不是自己去猜
        CastCard(backdrop = backdrop) {
            CastSectionHeader(title = "颜色说明")
            Column(
                Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeatherKind.entries.forEach { kind ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(kind.color())
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${kind.label} · ${kind.motto}",
                            modifier = Modifier.weight(1f),
                            fontSize = SkinTokens.caption,
                            lineHeight = 22.sp,
                            color = SkinTokens.muted
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2 身体来信
// ---------------------------------------------------------------------------

@Composable
private fun EchoLetterSection(backdrop: Backdrop, store: BodyCastStore) {
    val letter = remember(store.records, store.today, store.prefs.nickname) {
        EchoGenerator.bodyLetter(store.records, store.today, store.prefs.nickname)
    }

    CastCard(backdrop = backdrop) {
        Text(
            text = letter.weekLabel,
            fontSize = SkinTokens.caption,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = letter.title,
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.Bold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            letter.lines.forEach { line ->
                Text(
                    text = line.text,
                    fontSize = SkinTokens.body,
                    lineHeight = 28.sp,
                    fontWeight = if (line.emphasis) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (line.emphasis) SkinTokens.ink else SkinTokens.muted
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = letter.signature,
            modifier = Modifier.fillMaxWidth(),
            fontSize = SkinTokens.caption,
            fontStyle = FontStyle.Italic,
            color = SkinTokens.faint,
            textAlign = TextAlign.End
        )
    }
}

// ---------------------------------------------------------------------------
// 3 天气诗
// ---------------------------------------------------------------------------

@Composable
private fun EchoPoemSection(backdrop: Backdrop, store: BodyCastStore) {
    val weather = store.weather

    // 说明文字和卡片一样要隔开：贴着卡片的说明会被读成卡片的一部分（21.0.0）。
    Column(verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)) {
        CastCard(backdrop = backdrop) {
            Text(
                text = weather.kind.label,
                fontSize = SkinTokens.caption,
                color = SkinTokens.muted
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = weather.broadcast.poem,
                fontSize = 24.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Medium,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "—— BodyCast",
                modifier = Modifier.fillMaxWidth(),
                fontSize = SkinTokens.caption,
                color = SkinTokens.faint,
                textAlign = TextAlign.End
            )
        }

        CastHint("诗是本地生成的，每天只换一次。刷新页面它不会变——这样它才像今天的东西。")
    }
}

// ---------------------------------------------------------------------------
// 4 回声图表
// ---------------------------------------------------------------------------

@Composable
private fun EchoChartSection(backdrop: Backdrop, store: BodyCastStore) {
    val series = remember(store.records, store.today) {
        EchoGenerator.echoSeries(store.records, store.today)
    }

    if (series.isEmpty()) {
        CastEmptyPanel("还没有足够的记录，攒两天就能看到趋势了。", backdrop)
        return
    }

    // 显式给间距：卡片之间、卡片与说明文字之间都按页面节奏拉开 10dp，
    // 不依赖「同一个 LazyColumn item 内部会不会自动带间距」这种隐式行为
    // （21.0.0：用户反馈这一段的几块贴在一起，看不出间隔）。
    Column(verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)) {
        series.forEach { item ->
            CastCard(backdrop = backdrop) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = SkinTokens.cardTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = SkinTokens.ink
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.trend,
                            fontSize = SkinTokens.caption,
                            color = SkinTokens.muted
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    item.points.forEach { (label, value) ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = value,
                                fontSize = SkinTokens.caption,
                                fontWeight = FontWeight.Medium,
                                color = SkinTokens.ink,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(SkinTokens.accent.copy(alpha = 0.35f))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = SkinTokens.caption,
                                color = SkinTokens.muted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        CastHint("这里只有词和方向，没有数值坐标轴。一旦有刻度，人就会开始追求更高的那条线。")
    }
}

// ---------------------------------------------------------------------------
// 5 分享卡片
// ---------------------------------------------------------------------------

@Composable
private fun EchoShareSection(backdrop: Backdrop, store: BodyCastStore) {
    val card = remember(store.weather, store.prefs.nickname) {
        EchoGenerator.shareCard(store.weather, store.prefs.nickname)
    }

    // 说明文字和卡片之间也要留出页面级的间距：贴着卡片的说明会被读成
    // 卡片的一部分（21.0.0 用户反馈「一块一块都连在一起了」）。
    Column(verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)) {
        // 分享卡本身是一块「实心」的玻璃面板，颜色跟着天气走，
        // 这样截图出去能一眼看出今天是什么天气。
        CastCard(backdrop = backdrop, tint = card.kind.color().copy(alpha = 0.22f)) {
            Text(
                text = card.title,
                fontSize = SkinTokens.caption,
                color = SkinTokens.muted
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = card.kind.label,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = card.kind.motto,
                fontSize = SkinTokens.body,
                lineHeight = 26.sp,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                card.lines.forEach { line ->
                    // 先翻译再拼前缀：BodyCastText.text() 是整串匹配，带「· 」前缀的串匹配不上。
                    RawText(
                        text = "· " + BodyCastText.text(line),
                        fontSize = SkinTokens.caption,
                        lineHeight = 24.sp,
                        color = SkinTokens.muted
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = card.poem,
                fontSize = SkinTokens.body,
                fontStyle = FontStyle.Italic,
                lineHeight = 28.sp,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = card.footer,
                fontSize = SkinTokens.caption,
                color = SkinTokens.faint
            )
        }

        // 22.38.0：明信片——一键出图、走系统分享面板。截屏仍然可行，但
        // 「生成一张好看的画」才是让人愿意发出去的东西。
        val shareContext = androidx.compose.ui.platform.LocalContext.current
        CastPrimaryButton(
            text = BodyCastText.text("生成天气明信片并分享"),
            icon = "share",
            onClick = {
                runCatching {
                    val bmp = cn.pocket.toolbox.ui.share.WeatherPostcard.render(
                        kind = store.weather.kind,
                        line = store.weather.broadcast.headline,
                        date = store.today
                    )
                    cn.pocket.toolbox.ui.share.WeatherPostcard.share(shareContext, bmp)
                }
            },
            backdrop = backdrop
        )
        CastHint(BodyCastText.text("明信片是画出来的：天气球、一句话、日期和落款，不含任何数值、体重或热量。"))
    }
}

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

/** 天气色 → Compose 颜色。 */
private fun WeatherKind.color(): Color = Color(colorHex)

/** 日历格里的两字简称。 */
private fun shortLabel(kind: WeatherKind): String = when (kind) {
    WeatherKind.RECOVERING -> "恢复"
    WeatherKind.ENERGY_HIGH -> "高能"
    WeatherKind.PRESSURE -> "压力"
    WeatherKind.BALANCED -> "平衡"
    WeatherKind.SENSITIVE -> "敏感"
}

/**
 * 输入框。
 *
 * 玻璃盒子 + 无装饰的 BasicTextField。
 */
@Composable
internal fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    // 22.59.0：形状可传（设置页的搜索框要胶囊形，两头全圆）。
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(SkinTokens.secondaryRadius)
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SkinTokens.glassTrack)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = SkinTokens.body,
                // 用 muted 而不是 faint：faint 压在玻璃轨道上对比度太低，
                // 页面滚到顶部（玻璃后面是亮色环境色块）时基本看不清。
                color = SkinTokens.muted
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = SkinTokens.body,
                color = SkinTokens.ink
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(SkinTokens.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
