package cn.pocket.toolbox.data

import cn.pocket.toolbox.bodycast.EntryValues
import cn.pocket.toolbox.i18n.BodyCastText
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 导入文件的解析器（22.24.0，方案 B §3.2）。
 *
 * 目标：**只按表头认列，绝不按列序猜**。厂商导出的文件长什么样我们管不了，
 * 但「哪一列是什么」必须由表头说了算；表头不认识就留 null，界面继续写
 * 「暂时没读到」——**绝不填 0**，也绝不因为一个坏文件把应用带崩。
 *
 * 三条解析纪律（照抄 `Backup.kt` 与 `HealthConnectSource` 的精神）：
 * 1. 只认表头：认不出的列如实列回界面（「忽略 M 列」），方便下次加映射；
 * 2. 读不到就 null：某一列缺失、格式不对、超出合理范围，一律丢该字段；
 * 3. 不做聪明换算：单位认不出就丢，不给「大概齐」的猜测值。
 *
 * 只用 [org.json]（项目已在用，见 `Backup.kt`）与手写的 CSV 切分，**不引任何新依赖**。
 */
object ImportParser {

    /** 内部字段：键用来给界面取值，中文名登记在 [BodyCastText] 里。 */
    enum class Field(val key: String, private val sourceLabel: String) {
        RESTING_HEART_RATE("restingHeartRate", "静息心率"),
        HRV("hrv", "心率变异性"),
        SLEEP_MINUTES("sleepMinutes", "睡眠时长"),
        DEEP_SLEEP_RATIO("deepSleepRatio", "深睡占比"),
        STEPS("steps", "步数"),
        STRESS_LEVEL("stressLevel", "压力值"),
        BLOOD_OXYGEN("bloodOxygen", "血氧"),
        ACTIVITY_MINUTES("activityMinutes", "活动时长");

        val label: String get() = BodyCastText.text(sourceLabel)

        /** 预览卡右侧那一列：读到的原值（纯数字，不带单位）。 */
        fun display(values: EntryValues): String? = when (this) {
            RESTING_HEART_RATE -> values.restingHeartRate?.toString()
            HRV -> values.hrv?.toString()
            SLEEP_MINUTES -> values.sleepMinutes?.toString()
            DEEP_SLEEP_RATIO -> values.deepSleepRatio?.let { (it * 100).toInt().toString() + "%" }
            STEPS -> values.steps?.toString()
            STRESS_LEVEL -> values.stressLevel?.toString()
            BLOOD_OXYGEN -> values.bloodOxygen?.toString()
            ACTIVITY_MINUTES -> values.activityMinutes?.toString()
        }
    }

    /** 解析结果。识别不出任何一列时 [recognized] 为空，由界面走「这个文件读不了」分支。 */
    data class Result(
        val values: EntryValues,
        val recognized: List<Field>,
        /** 没用上的列（表头原文，含日期列）。 */
        val ignoredHeaders: List<String>,
        /** 文件里认出来的全部表头，读不了时列给用户看。 */
        val allHeaders: List<String>,
        /** 文件里的日期比昨天还早时是几天前；null = 没有日期列，或者就是今天 / 昨天。 */
        val staleDays: Int?,
        /** 解析出来的行数。 */
        val rowCount: Int
    )

    /** 表头别名（大小写、空格、中英文都要认）。归一化之后做全等比较。 */
    private val ALIASES: Map<Field, Set<String>> = mapOf(
        Field.RESTING_HEART_RATE to setOf("静息心率", "心率", "restingheartrate", "rhr"),
        Field.HRV to setOf("心率变异性", "hrv", "rmssd", "心率变异性hrv"),
        Field.SLEEP_MINUTES to setOf("睡眠时长", "总睡眠", "睡眠", "睡眠时间", "sleep", "sleeptime", "totalsleep"),
        Field.DEEP_SLEEP_RATIO to setOf("深睡", "深睡时长", "深睡占比", "deepsleep", "deepsleeptime", "deepsleepratio"),
        Field.STEPS to setOf("步数", "总步数", "steps", "stepcount"),
        Field.STRESS_LEVEL to setOf("压力", "压力值", "压力得分", "stress", "stresslevel"),
        Field.BLOOD_OXYGEN to setOf("血氧", "血氧饱和度", "spo2", "bloodoxygen", "oxygensaturation"),
        Field.ACTIVITY_MINUTES to setOf(
            "中高强度", "活动时长", "运动时长", "锻炼时长", "运动时间",
            "activityminutes", "activeminutes", "exerciseminutes"
        )
    )

    /** 日期列别名。这一列不产出读数，但用来判断「这份数据是不是今天 / 昨夜的」。 */
    private val DATE_ALIASES = setOf(
        "日期", "时间", "测量时间", "记录时间", "开始时间", "测量日期", "数据日期",
        "date", "time", "starttime", "datetime", "timestamp"
    )

    /** 空值的各种写法：都当「没读到」，不是 0。 */
    private val BLANKS = setOf("", "-", "--", "—", "－", "无", "null", "n/a", "na", "/", "未知")

    // -----------------------------------------------------------------------
    // 入口
    // -----------------------------------------------------------------------

    /**
     * 解析一段文本。返回 null = 这个文件读不了（空文件 / 二进制 / 连表头都没有）。
     *
     * 返回非 null 但 [Result.recognized] 为空，也是「读不了」的一种：
     * 文件是文本、表头能列出来，只是没有一列认识——界面要把表头显示出来。
     */
    fun parse(text: String?): Result? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        val table = parseJson(trimmed) ?: parseCsv(trimmed) ?: return null
        return extract(table)
    }

    // -----------------------------------------------------------------------
    // 表格（表头 + 行）
    // -----------------------------------------------------------------------

    private data class Table(val headers: List<String>, val rows: List<Map<String, String>>, val rowCount: Int)

    /**
     * CSV：先找表头行，再按分隔符切片。
     *
     * 表头行不是无脑取第一行——厂商导出的文件常常先来一两行标题 / 空行。
     * 这里在前 8 个非空行里挑「认出的别名最多」的那一行当表头；一行都认不出时
     * 退回第一个非空行（这样界面能把原始表头列出来）。
     */
    private fun parseCsv(text: String): Table? {
        val lines = text.split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null
        val head = lines.take(8).map { it to splitCsvLine(it, delimiterOf(it)) }
        val headerIndex = head.indices
            .maxByOrNull { index -> head[index].second.count { cell -> fieldOf(cell) != null || isDateHeader(cell) } }
            ?.takeIf { index -> head[index].second.any { cell -> fieldOf(cell) != null } }
            ?: 0
        val (headerLine, rawHeaders) = head[headerIndex]
        val delimiter = delimiterOf(headerLine)
        val headers = rawHeaders.map { it.trim() }
        // 表头行本身可能因为分隔符判断错而只有一格：换一个分隔符再切一次。
        val cells = if (headers.size >= 2) headers else splitCsvLine(headerLine, alternativeDelimiter(delimiter)).map { it.trim() }
        val body = lines.drop(headerIndex + 1).map { splitCsvLine(it, delimiter) }
        val rows = body.map { line ->
            cells.indices.associate { i -> cells[i] to (line.getOrNull(i)?.trim() ?: "") }
        }
        return Table(cells, rows, body.size)
    }

    /** 分隔符：逗号 / 制表符 / 分号里，出现在表头行里最多的那个（引号内的不算）。 */
    private fun delimiterOf(line: String): Char {
        val counts = listOf(',', '\t', ';', '|').associateWith { countOutsideQuotes(line, it) }
        val best = counts.maxByOrNull { it.value }
        return if (best != null && best.value > 0) best.key else ','
    }

    private fun alternativeDelimiter(current: Char): Char =
        listOf(',', '\t', ';', '|').first { it != current }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        line.forEach { c ->
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> count++
            }
        }
        return count
    }

    /** 手写切分：支持双引号包裹、`""` 转义。**不引任何 CSV 库。** */
    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> {
                    out += cell.toString()
                    cell.setLength(0)
                }
                else -> cell.append(c)
            }
            i++
        }
        out += cell.toString()
        return out
    }

    /**
     * JSON：数组对象、带数组字段的对象、单个对象三种形状都认。
     *
     * 用项目已在用的 [org.json]；坏文件一律返回 null，不抛异常到界面。
     */
    private fun parseJson(text: String): Table? {
        if (!text.startsWith("{") && !text.startsWith("[")) return null
        val elements: List<Any> = runCatching {
            when {
                text.startsWith("[") -> JSONArray(text).let { array -> (0 until array.length()).map { array.get(it) } }
                else -> {
                    val root = JSONObject(text)
                    val nested = listOf("data", "records", "samples", "items", "list", "values", "result", "datas")
                        .firstNotNullOfOrNull { key -> root.optJSONArray(key) }
                    if (nested != null) {
                        (0 until nested.length()).map { nested.get(it) }
                    } else {
                        listOf(root)
                    }
                }
            }
        }.getOrNull() ?: return null

        val objects = elements.mapNotNull { it as? JSONObject }
        if (objects.isEmpty()) return null
        // 表头 = 所有对象键的并集，顺序按第一次出现的顺序。
        val headers = linkedSetOf<String>()
        objects.forEach { obj -> obj.keys().forEach { headers += it } }
        val headerList = headers.toList()
        val rows = objects.map { obj ->
            headerList.associateWith { key -> scalarOf(obj.opt(key)) }
        }
        return Table(headerList, rows, objects.size)
    }

    /** JSON 值 → 单元格文本。对象 / 数组不参与取值。 */
    private fun scalarOf(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is JSONObject, is JSONArray -> ""
        else -> value.toString()
    }

    // -----------------------------------------------------------------------
    // 取值
    // -----------------------------------------------------------------------

    private fun extract(table: Table): Result {
        val columns = table.headers.map { header -> header to fieldOf(header) }
        val recognized = columns.mapNotNull { it.second }.distinct()

        // 日期：认出来的第一列。用来判断「这份数据是不是今天 / 昨夜的」。
        val dateHeader = table.headers.firstOrNull { normalize(it) in DATE_ALIASES }
        val datedRows = if (dateHeader == null) {
            emptyList()
        } else {
            table.rows.mapNotNull { row -> parseDate(row[dateHeader] ?: "") }
        }
        val today = LocalDate.now()
        val freshRows = if (dateHeader == null || datedRows.isEmpty()) {
            table.rows
        } else {
            // 只认「今天 / 昨夜」那几行；一条都没有就说明整份文件都是旧的。
            val kept = table.rows.filter { row ->
                val date = parseDate(row[dateHeader] ?: "")
                date != null && ChronoUnit.DAYS.between(date, today) <= 1
            }
            kept
        }
        val staleDays = if (dateHeader != null && datedRows.isNotEmpty() && freshRows.isEmpty()) {
            datedRows.maxByOrNull { it }?.let { newest -> ChronoUnit.DAYS.between(newest, today).toInt() }
        } else {
            null
        }

        fun cells(field: Field): List<String> {
            val header = columns.firstOrNull { it.second == field }?.first ?: return emptyList()
            return freshRows.map { it[header] ?: "" }
        }

        val sleepHeader = columns.firstOrNull { it.second == Field.SLEEP_MINUTES }?.first
        val deepHeader = columns.firstOrNull { it.second == Field.DEEP_SLEEP_RATIO }?.first
        // 表头自己说了是「占比 / 比例」时按比例读；说「时长」时按时长读，再除同一行的总睡眠。
        val deepIsRatio = deepHeader?.let { looksLikeRatioHeader(it) } ?: false

        val restingHeartRate = cells(Field.RESTING_HEART_RATE)
            .asSequence().mapNotNull { cell -> intOf(cell) }
            .firstOrNull { it in 25..200 }
        // HRV：毫秒。合理范围之外的值丢掉，不硬存（单位认不出时同理）。
        val hrv = cells(Field.HRV).asSequence().mapNotNull { intOf(it) }.firstOrNull { it in 1..1000 }
        val sleepMinutes = cells(Field.SLEEP_MINUTES)
            .asSequence().mapNotNull { minutesOf(it) }.filter { it > 0 }.maxOrNull()
            ?.takeIf { it <= 24 * 60 }
        val deepSleepRatio = if (deepHeader == null) {
            null
        } else {
            freshRows.asSequence().mapNotNull { row ->
                val cell = row[deepHeader] ?: ""
                if (isBlank(cell)) {
                    null
                } else if (deepIsRatio || cell.trim().endsWith("%") || cell.trim().endsWith("％")) {
                    ratioOf(cell)
                } else {
                    // 深睡给的是时长：除**同一行**的总睡眠时长；拿不到就丢，不猜。
                    val deep = minutesOf(cell)
                    val total = sleepHeader?.let { minutesOf(row[it] ?: "") }
                    if (deep != null && total != null && total > 0) {
                        (deep.toFloat() / total).coerceIn(0f, 1f)
                    } else {
                        // `0.21` 这种「已经是比例」的写法仍然认。
                        cell.trim().toFloatOrNull()?.takeIf { it in 0f..1f }
                    }
                }
            }.lastOrNull()
        }
        // 步数：当天最大行，**不求和**（同一份文件里有多行时求和会翻倍）。
        val steps = cells(Field.STEPS).asSequence().mapNotNull { intOf(it) }
            .filter { it in 0..1_000_000 }.maxOrNull()
        val stressLevel = cells(Field.STRESS_LEVEL).asSequence().mapNotNull { intOf(it) }
            .firstOrNull { it in 0..100 }
        // 血氧：只认单次测量值，不做平均（平均值会把「最低血氧」也揉进去）。
        val bloodOxygen = cells(Field.BLOOD_OXYGEN).asSequence().mapNotNull { percentIntOf(it) }
            .firstOrNull { it in 50..100 }
        // 活动时长：当天多行**求和**（一份文件里可能有几段运动）。
        val activityMinutes = cells(Field.ACTIVITY_MINUTES).asSequence().mapNotNull { minutesOf(it) }
            .filter { it > 0 }.sum().takeIf { it in 1..(24 * 60) }

        val values = EntryValues(
            restingHeartRate = restingHeartRate,
            hrv = hrv,
            sleepMinutes = sleepMinutes,
            deepSleepRatio = deepSleepRatio,
            steps = steps,
            stressLevel = stressLevel,
            bloodOxygen = bloodOxygen,
            activityMinutes = activityMinutes
        )

        val usedHeaders = columns.filter { it.second != null }.map { it.first } + listOfNotNull(dateHeader)
        val ignored = table.headers.filter { it !in usedHeaders }
        return Result(
            values = values,
            recognized = recognized.filter { field -> field.display(values) != null },
            ignoredHeaders = ignored,
            allHeaders = table.headers,
            staleDays = staleDays,
            rowCount = table.rowCount
        )
    }

    // -----------------------------------------------------------------------
    // 单元格取值
    // -----------------------------------------------------------------------

    private fun isBlank(cell: String): Boolean = cell.trim().lowercase() in BLANKS

    /** 表头 → 字段。归一化之后全等比较（含单位后缀的表头先剥掉括号）。 */
    private fun fieldOf(header: String): Field? {
        val key = normalize(header)
        if (key.isEmpty()) return null
        return ALIASES.entries.firstOrNull { (_, aliases) -> key in aliases }?.key
    }

    private fun isDateHeader(header: String): Boolean = normalize(header) in DATE_ALIASES

    /**
     * 表头归一化：去掉空白、下划线、连字符，剥掉括号里的单位，转小写。
     *
     * `步数 (步)` / `steps_count` / `静息心率（次/分）` 都能对上别名表。
     */
    private fun normalize(header: String): String {
        val noSpace = header.trim().lowercase()
            .replace(" ", "").replace("\u3000", "")
            .replace("_", "").replace("-", "")
        val cut = noSpace.indexOfFirst { it == '(' || it == '（' || it == '[' || it == '【' }
        val stripped = if (cut > 0) noSpace.substring(0, cut) else noSpace
        return stripped.trim('*', '：', ':', '"')
    }

    /** 整数。`452.0` 认，`「62 次/分」` 取前导数字，其余一律 null。 */
    private fun intOf(cell: String): Int? {
        if (isBlank(cell)) return null
        val text = cell.trim().replace(",", "").replace("，", "")
        val match = Regex("^([+-]?\\d+(?:\\.\\d+)?)").find(text) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        return value.takeIf { it % 1.0 == 0.0 }?.toInt()
    }

    /**
     * 时长 → 分钟。认「452」「7小时32分」「7:32」「7h32m」「90分钟」这几种写法。
     * 认不出就 null，**不做「大概几小时」的换算**。
     */
    private fun minutesOf(cell: String): Int? {
        if (isBlank(cell)) return null
        val text = cell.trim().lowercase().replace(" ", "")
        // 7:32 / 07:32:00
        Regex("^(\\d{1,2}):(\\d{2})(?::(\\d{2}))?$").matchEntire(text)?.let { match ->
            val hours = match.groupValues[1].toIntOrNull() ?: return null
            val minutes = match.groupValues[2].toIntOrNull() ?: return null
            if (hours > 24 || minutes > 59) return null
            return hours * 60 + minutes
        }
        // 7小时32分 / 7小时 / 32分 / 7h32m / 1h5min / 90分钟
        val chinese = Regex("^(?:(\\d+)小时|(\\d+)h(?:our)?s?)?(?:(\\d+)分(?:钟)?|(\\d+)m(?:in)?s?)?$").matchEntire(text)
        if (chinese != null) {
            val hours = chinese.groupValues[1].ifEmpty { chinese.groupValues[2] }.toIntOrNull()
            val minutes = chinese.groupValues[3].ifEmpty { chinese.groupValues[4] }.toIntOrNull()
            if (hours != null || minutes != null) {
                val total = (hours ?: 0) * 60 + (minutes ?: 0)
                if (total > 24 * 60) return null
                return total
            }
        }
        // 纯数字 = 分钟（方案 §3.2：「452」这种写法）
        val plain = text.toIntOrNull() ?: return null
        return plain.takeIf { it in 0..(24 * 60) }
    }

    /** 百分比 → 0f..1f。`21%` → 0.21，`0.21` → 0.21，`21` → 0.21。 */
    private fun ratioOf(cell: String): Float? {
        val text = cell.trim()
        if (isBlank(text)) return null
        val percentSign = text.endsWith("%") || text.endsWith("％")
        val value = text.removeSuffix("%").removeSuffix("％").trim().toFloatOrNull() ?: return null
        if (value < 0f) return null
        return if (!percentSign && value <= 1f) value.coerceIn(0f, 1f) else (value / 100f).coerceIn(0f, 1f)
    }

    /** 血氧：`97` / `97%` / `0.97` 都认，取整数百分比。 */
    private fun percentIntOf(cell: String): Int? {
        val text = cell.trim()
        if (isBlank(text)) return null
        val percentSign = text.endsWith("%") || text.endsWith("％")
        val number = text.removeSuffix("%").removeSuffix("％").trim().toFloatOrNull() ?: return null
        if (number <= 0f) return null
        // `0.97` = 比例；`97` 与 `97%` = 百分数。
        val percent = if (!percentSign && number <= 1f) number * 100f else number
        return percent.toInt()
    }

    /** 表头自己在说「这是比例 / 占比」吗。 */
    private fun looksLikeRatioHeader(header: String): Boolean {
        val text = header.lowercase()
        return text.contains("占比") || text.contains("比例") || text.contains("percent") ||
            text.contains("ratio") || header.contains("%") || header.contains("％")
    }

    // -----------------------------------------------------------------------
    // 日期
    // -----------------------------------------------------------------------

    /**
     * 日期单元格 → LocalDate。
     *
     * 认 `2026-10-11`、`2026/10/11`、`2026年10月11日`、`10-11`、`10月11日`、`10/11`，
     * 后面可以跟时间（`2026-10-11 08:12` / `2026-10-11T08:12:00`）。
     * 认不出就 null —— 后面的判断会当成「没有日期信息」而不是当成旧的。
     */
    private fun parseDate(cell: String): LocalDate? {
        if (isBlank(cell)) return null
        val text = cell.trim().replace("T", " ").substringBefore(' ')
        val today = LocalDate.now()
        Regex("^(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})日?$").matchEntire(text)?.let { match ->
            return runCatching {
                LocalDate.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt()
                )
            }.getOrNull()
        }
        Regex("^(\\d{1,2})[-/.月](\\d{1,2})日?$").matchEntire(text)?.let { match ->
            return runCatching {
                LocalDate.of(
                    today.year,
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt()
                )
            }.getOrNull()
        }
        return null
    }
}
