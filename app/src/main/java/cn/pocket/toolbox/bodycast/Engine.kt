package cn.pocket.toolbox.bodycast

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 身体气象台的推理层。
 *
 * 这一版是本地规则引擎，不联网、不调用任何大模型。
 * 接口之所以单独抽出来，是因为「播报」这件事最终一定要交给语言模型才自然，
 * 到时候只需要换一个实现，页面和存储一行都不用动。
 *
 * 实现方必须遵守的三条：
 * 1. 不输出数字。所有档位都用天气词表达。
 * 2. 数据缺失时返回「暂时没读到」，不允许用 0 或默认值补齐。
 * 3. 手动校准非空时，结论必须是 [WeatherKind.SENSITIVE]，设备读数不再参与定档。
 */
interface BodyCastEngine {

    /** 引擎名，设置页里会显示，方便判断当前用的是规则还是模型。 */
    val name: String

    /**
     * @param defaultKind 用户在设置里指定的「默认天气类型」。
     *
     * 只在**四个信号都算不出来且没有任何校准**时才允许使用——也就是引擎确实
     * 无从判断的时候（22.27.0 起按「四信号全空」判，不再看零散读数的条数：
     * 只填了血氧这类不参与四信号的读数时，依然属于无从判断）。
     * 只要有一路信号能算，或者用户做了任何校准，这个参数一律被忽略，
     * 交给真实数据定档。
     *
     * 注意方向：默认值是由**调用方**传进来的用户偏好，不是引擎自己的假设。
     * 引擎**自己绝不发明默认值**——缺失读数一律保持缺失。
     * 传 null 表示用户选了「跟随读数」，此时没有任何默认值可用。
     */
    fun weather(
        signals: BodySignals,
        calibrated: Set<CalibrationTag>,
        seed: Long,
        defaultKind: WeatherKind? = null
    ): BodyWeather

    /**
     * 四个信号（恢复云量 / 压力风速 / 能量湿度 / 睡眠云层）里有几个算得出来。
     *
     * 22.27.0 起天气判定**允许部分信号**：有几路就按几路给结论，
     * 缺的那几路照旧写「暂时没读到」。返回 0 = 确实一项都无从判断，
     * 只有这时才允许退回用户设置的默认天气（老路径）。
     */
    fun signalCount(signals: BodySignals): Int

    fun prescription(
        weather: BodyWeather,
        calibrated: Set<CalibrationTag>,
        prefs: PrescriptionPrefs,
        seed: Long,
        /** 22.49.0：今天的运动量把处方往哪边推（见 [ActivityTilt]）。 */
        activityTilt: ActivityTilt = ActivityTilt.NEUTRAL,
        /** 22.53.0：排除这些动作 id——「再来一件」时不要把刚做完的又端上来。 */
        excludeIds: Set<String> = emptySet()
    ): Prescription
}

/**
 * 本地规则引擎。
 *
 * 阈值都写在这里，且**只在内部使用**——它们的作用是决定用哪个天气词，
 * 不是拿来给用户看进度条。任何把阈值暴露成「完成度」的改动都会违背产品定位。
 */
class LocalRuleEngine : BodyCastEngine {

    override val name: String = "本地规则引擎"

    private companion object {
        /**
         * 卡片上至少要列出几条动作。
         *
         * 上限由 [PrescriptionPrefs.maxMinutes] 的总时长预算决定，
         * 这个下限只是「别只给一两件就交差」——总时长装不下时以总时长为准。
         */
        const val MIN_STEPS = 3
    }

    // ---------------------------------------------------------------------
    // 天气
    // ---------------------------------------------------------------------

    override fun signalCount(signals: BodySignals): Int = listOf(
        recoveryLevel(signals), pressureLevel(signals),
        energyLevel(signals), sleepLevel(signals)
    ).count { it != null }

    override fun weather(
        signals: BodySignals,
        calibrated: Set<CalibrationTag>,
        seed: Long,
        defaultKind: WeatherKind?
    ): BodyWeather {
        val recovery = recoveryLevel(signals)
        val pressure = pressureLevel(signals)
        val energy = energyLevel(signals)
        val sleep = sleepLevel(signals)

        val metrics = listOf(
            recoveryMetric(recovery),
            pressureMetric(pressure),
            energyMetric(energy),
            sleepMetric(sleep)
        )

        val kind = if (calibrated.isNotEmpty()) {
            // 用户说了算。设备读数不参与定档，但四项指标仍然展示——
            // 让用户看到「设备读到的」和「我感受到的」可以不一样。
            WeatherKind.SENSITIVE
        } else {
            val fromSignals = classify(recovery, pressure, energy, sleep)
            // 只有在「四路信号都算不出来」这种确实无从判断的情况下，
            // 才退回用户设置的默认天气类型。有几路就按几路定档（22.27.0）：
            // 一路信号也能给出部分结论，缺的那几路照旧写「暂时没读到」。
            // 四档全为 null 时 classify 会给 SENSITIVE，但那是引擎的兜底，
            // 不是用户的意愿，所以这里要单独判 signalCount，而不是看 classify 的结果。
            if (signalCount(signals) == 0 && defaultKind != null) defaultKind else fromSignals
        }

        val broadcast = buildBroadcast(kind, metrics, signals, calibrated, recovery, pressure, energy, sleep, seed)

        return BodyWeather(
            kind = kind,
            metrics = metrics,
            broadcast = broadcast,
            source = signals.source,
            updatedAt = if (signals.updatedAt > 0L) signals.updatedAt else System.currentTimeMillis(),
            calibrated = calibrated,
            missing = signals.missing,
            demo = signals.source == SignalSource.DEMO
        )
    }

    /**
     * 定档顺序是刻意的：先看「恢复」再看「压力」，最后才轮到「高能」。
     *
     * 因为把疲惫误判成高能，比把高能误判成疲惫的代价大得多——
     * 前者会推着人继续消耗，后者最多让人多休息一会儿。
     * 所有健康类产品都应该往保守的一侧偏。
     */
    private fun classify(
        recovery: Int?,
        pressure: Int?,
        energy: Int?,
        sleep: Int?
    ): WeatherKind = when {
        recovery != null && recovery == 0 -> WeatherKind.RECOVERING
        sleep != null && sleep == 0 && (recovery == null || recovery <= 1) -> WeatherKind.RECOVERING
        pressure != null && pressure == 0 -> WeatherKind.PRESSURE
        energy != null && energy == 3 && (pressure == null || pressure >= 2) -> WeatherKind.ENERGY_HIGH
        recovery == null && pressure == null && energy == null && sleep == null -> WeatherKind.SENSITIVE
        else -> WeatherKind.BALANCED
    }

    // ---- 各指标的内部档位：3 最好，0 最差，null 表示读不到 ----

    private fun recoveryLevel(s: BodySignals): Int? {
        val votes = mutableListOf<Int>()
        s.hrv?.let { votes += when { it >= 55 -> 3; it >= 45 -> 2; it >= 35 -> 1; else -> 0 } }
        s.restingHeartRate?.let { votes += when { it <= 58 -> 3; it <= 66 -> 2; it <= 74 -> 1; else -> 0 } }
        return votes.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    }

    private fun pressureLevel(s: BodySignals): Int? = s.stressLevel?.let {
        when {
            it <= 30 -> 3
            it <= 50 -> 2
            it <= 70 -> 1
            else -> 0
        }
    }

    private fun energyLevel(s: BodySignals): Int? {
        val votes = mutableListOf<Int>()
        s.activityMinutes?.let { votes += when { it >= 40 -> 3; it >= 20 -> 2; it >= 5 -> 1; else -> 0 } }
        s.steps?.let { votes += when { it >= 8000 -> 3; it >= 4000 -> 2; it >= 1000 -> 1; else -> 0 } }
        return votes.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    }

    private fun sleepLevel(s: BodySignals): Int? {
        val votes = mutableListOf<Int>()
        s.sleepMinutes?.let { votes += when { it >= 420 -> 3; it >= 360 -> 2; it >= 300 -> 1; else -> 0 } }
        s.deepSleepRatio?.let { votes += when { it >= 0.22f -> 3; it >= 0.16f -> 2; it >= 0.10f -> 1; else -> 0 } }
        return votes.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    }

    // ---- 档位 → 天气词 ----

    private fun recoveryMetric(level: Int?): WeatherMetric {
        if (level == null) {
            return WeatherMetric(
                WeatherBodyWeatherKeys.RECOVERY, "恢复云量", "暂时没读到",
                "接上设备或手动校准后，这里会有结果", uncertain = true
            )
        }
        val (word, hint) = when (level) {
            3 -> "晴" to "身体有余量，可以正常安排"
            2 -> "少云" to "还算稳，别排太满"
            1 -> "多云" to "余量不多，留点空档"
            else -> "阴" to "先别加量，把该停的停下来"
        }
        return WeatherMetric(WeatherBodyWeatherKeys.RECOVERY, "恢复云量", word, hint)
    }

    private fun pressureMetric(level: Int?): WeatherMetric {
        if (level == null) {
            return WeatherMetric(
                WeatherBodyWeatherKeys.PRESSURE, "压力风速", "暂时没读到",
                "设备没有提供压力数据", uncertain = true
            )
        }
        val (word, hint) = when (level) {
            3 -> "无风" to "现在是松的"
            2 -> "微风" to "有一点紧，属于正常"
            1 -> "有风" to "紧的时候先松肩膀"
            else -> "风大" to "今天适合少做决定"
        }
        return WeatherMetric(WeatherBodyWeatherKeys.PRESSURE, "压力风速", word, hint)
    }

    private fun energyMetric(level: Int?): WeatherMetric {
        if (level == null) {
            return WeatherMetric(
                WeatherBodyWeatherKeys.ENERGY, "能量湿度", "暂时没读到",
                "没有活动数据，判断不了", uncertain = true
            )
        }
        val (word, hint) = when (level) {
            3 -> "偏湿" to "身体想动，顺着它"
            2 -> "适中" to "不多不少"
            1 -> "偏干" to "能量偏低，别硬撑"
            else -> "很干" to "今天先把力气省下来"
        }
        return WeatherMetric(WeatherBodyWeatherKeys.ENERGY, "能量湿度", word, hint)
    }

    private fun sleepMetric(level: Int?): WeatherMetric {
        if (level == null) {
            return WeatherMetric(
                WeatherBodyWeatherKeys.SLEEP, "睡眠云层", "暂时没读到",
                "没有睡眠数据", uncertain = true
            )
        }
        val (word, hint) = when (level) {
            3 -> "晴" to "昨晚睡透了"
            2 -> "薄雾" to "睡得还行，差一点点"
            1 -> "有雾" to "昨晚不太够"
            else -> "厚雾" to "缺得比较多，今天补一补"
        }
        return WeatherMetric(WeatherBodyWeatherKeys.SLEEP, "睡眠云层", word, hint)
    }

    // ---- 播报 ----

    private fun buildBroadcast(
        kind: WeatherKind,
        metrics: List<WeatherMetric>,
        signals: BodySignals,
        calibrated: Set<CalibrationTag>,
        recovery: Int?,
        pressure: Int?,
        energy: Int?,
        sleep: Int?,
        seed: Long
    ): Broadcast {
        val headline = if (calibrated.isNotEmpty()) {
            "你说「${calibrated.joinToString("、") { it.label }}」，所以今天是${kind.label}。"
        } else {
            "今天是${kind.label}。"
        }

        val body = when (kind) {
            WeatherKind.RECOVERING ->
                "云还没散完。今天不用把日程填满，能推掉的就推掉一件。"
            WeatherKind.ENERGY_HIGH ->
                "身体有余量。趁这个窗口做点一直想做的事，但别一次用光。"
            WeatherKind.PRESSURE ->
                "风比平时大。今天不适合做重大决定，先处理手头最简单的。"
            WeatherKind.BALANCED ->
                "没有特别要提醒的。按你原本的节奏来就好。"
            WeatherKind.SENSITIVE ->
                "设备读到的和你的感受不完全一致。以你说的为准。"
        }

        val reason = buildReason(kind, signals, calibrated, recovery, pressure, energy, sleep)

        // 部分信号可用时如实说明「按几路算的」（22.27.0）。结论是手动校准定的时候
        // 不加这句——那时结论根本不按信号算，加了反而像是在给校准打折扣。
        val signalCount = listOf(recovery, pressure, energy, sleep).count { it != null }
        val coverage = if (calibrated.isEmpty() && signalCount in 1..3) {
            "今天是按 $signalCount/4 个信号算的，缺的那 ${4 - signalCount} 个显示「暂时没读到」。"
        } else {
            null
        }

        return Broadcast(
            headline,
            body,
            reason,
            WeatherPoems.forKind(kind, seed),
            coverage
        )
    }

    /**
     * 「为什么这样播报」。
     *
     * 只说方向（偏低 / 正常 / 偏高），不说数值，也不给百分比——
     * 一旦出现数字，用户就会开始盯着它涨跌。
     */
    private fun buildReason(
        kind: WeatherKind,
        signals: BodySignals,
        calibrated: Set<CalibrationTag>,
        recovery: Int?,
        pressure: Int?,
        energy: Int?,
        sleep: Int?
    ): String {
        val parts = mutableListOf<String>()

        // 22.34.0：组合句在拼装时就翻好——整句再进 Text() 只会掉回原文，
        // 所以每一段（含分隔符）各自走 BodyCastText，槽位用 compose 模板。
        if (calibrated.isNotEmpty()) {
            parts += cn.pocket.toolbox.i18n.BodyCastText.compose(
                "你手动校准了「{0}」，这一条优先于设备读数",
                calibrated.joinToString("、") { it.label }
            )
        }
        recovery?.let {
            parts += cn.pocket.toolbox.i18n.BodyCastText.text(
                when {
                    it >= 3 -> "恢复指标比平时好"
                    it == 2 -> "恢复指标在常见范围"
                    it == 1 -> "恢复指标偏低"
                    else -> "恢复指标明显偏低"
                }
            )
        }
        pressure?.let {
            parts += cn.pocket.toolbox.i18n.BodyCastText.text(
                when {
                    it >= 3 -> "压力读数很松"
                    it == 2 -> "压力读数正常"
                    it == 1 -> "压力读数偏高"
                    else -> "压力读数明显偏高"
                }
            )
        }
        energy?.let {
            parts += cn.pocket.toolbox.i18n.BodyCastText.text(
                when {
                    it >= 3 -> "活动量比平时多"
                    it == 2 -> "活动量正常"
                    it == 1 -> "活动量偏少"
                    else -> "几乎没怎么活动"
                }
            )
        }
        sleep?.let {
            parts += cn.pocket.toolbox.i18n.BodyCastText.text(
                when {
                    it >= 3 -> "昨夜睡眠充足"
                    it == 2 -> "昨夜睡眠勉强够"
                    it == 1 -> "昨夜睡眠偏少"
                    else -> "昨夜睡眠明显不足"
                }
            )
        }

        if (parts.isEmpty()) {
            return cn.pocket.toolbox.i18n.BodyCastText.text("目前没有任何可用数据，所以这里的判断只是默认值，不代表你的身体状况。")
        }

        val tail = if (signals.missing.isEmpty()) {
            cn.pocket.toolbox.i18n.BodyCastText.text("这些只是趋势，不是诊断。")
        } else {
            cn.pocket.toolbox.i18n.BodyCastText.compose(
                "另外，{0}暂时没读到，这部分没有参与判断。",
                signals.missing.joinToString("、")
            )
        }
        return cn.pocket.toolbox.i18n.BodyCastText.text("依据：") +
            parts.joinToString(cn.pocket.toolbox.i18n.BodyCastText.text("；")) +
            cn.pocket.toolbox.i18n.BodyCastText.text("。") +
            tail
    }

    // ---------------------------------------------------------------------
    // 处方
    // ---------------------------------------------------------------------

    override fun prescription(
        weather: BodyWeather,
        calibrated: Set<CalibrationTag>,
        prefs: PrescriptionPrefs,
        seed: Long,
        activityTilt: ActivityTilt,
        excludeIds: Set<String>
    ): Prescription {
        val slot = Slot.of(currentHour())

        // 候选池：先按天气和时段筛，再按偏好过滤。
        var pool = ActionLibrary.forWeather(weather.kind, slot)
            .filter { it.minutes <= prefs.maxMinutes }
            .filter { it.category !in prefs.avoid }
            // 22.53.0：「再来一件」时排掉刚做完的那几件。排完一个不剩就放弃排除——
            // 宁可能重复，也不能给一张空处方。
            .let { base ->
                val filtered = base.filter { it.id !in excludeIds }
                if (filtered.isNotEmpty()) filtered else base
            }

        if (pool.isEmpty()) {
            // 退一步：放宽时段限制，但不放宽时长和偏好——那两条是用户的明确意愿。
            pool = ActionLibrary.forWeather(weather.kind)
                .filter { it.minutes <= prefs.maxMinutes }
                .filter { it.category !in prefs.avoid }
        }
        if (pool.isEmpty()) {
            pool = ActionLibrary.ALL.filter { it.minutes <= prefs.maxMinutes }
        }

        // 22.49.0：按今天的运动量排一下优先次序（[ActivityTilt] 里写清了规则）。
        // 关键在**分组后各自打乱**：只在排序前拆开不够——pickWithinBudget 会
        // 按种子整体洗牌，倾斜立刻被冲掉（实测过）。
        pool = orderByTilt(pool, activityTilt, seed)

        val ordered = if (prefs.preferQuiet) {
            pool.sortedBy { it.category.ordinal }
        } else {
            pool
        }

        val picked = pickWithinBudget(ordered, want = 5, budget = prefs.maxMinutes, seed = seed)

        return Prescription(
            ticketNo = ticketNo(seed),
            issuedAt = System.currentTimeMillis(),
            slot = slot,
            items = picked,
            note = note(weather, calibrated, activityTilt),
            weatherKind = weather.kind
        )
    }

    /**
     * 按运动量倾斜把池子排成「先挑的那组 + 其余」。
     *
     * 组内仍按种子乱序（同一天同一时段稳定、换一天才换一批），
     * 只有这样 [pickWithinBudget] 的顺序才是我们想要的。
     */
    internal fun orderByTilt(
        pool: List<MicroAction>,
        tilt: ActivityTilt,
        seed: Long
    ): List<MicroAction> {
        val preferred: Set<ActionCategory> = when (tilt) {
            ActivityTilt.ACTIVE -> setOf(ActionCategory.REST, ActionCategory.BREATHE, ActionCategory.LIGHT)
            ActivityTilt.SEDENTARY -> setOf(ActionCategory.MOVE, ActionCategory.LIGHT)
            ActivityTilt.NEUTRAL -> emptySet()
        }
        if (preferred.isEmpty()) return seededOrder(pool, seed)
        val (head, tail) = pool.partition { it.category in preferred }
        return seededOrder(head, seed) + seededOrder(tail, seed + 1)
    }

    /** 动得够多的一天：放松 / 呼吸 / 光照 排在前面，活动类排最后。 */
    private fun activeRank(category: ActionCategory): Int = when (category) {
        ActionCategory.REST -> 0
        ActionCategory.BREATHE -> 1
        ActionCategory.LIGHT -> 2
        ActionCategory.EAT -> 3
        ActionCategory.CONNECT -> 4
        ActionCategory.MOVE -> 5
    }

    /** 几乎没动的一天：活动 / 光照 排在前面。 */
    private fun sedentaryRank(category: ActionCategory): Int = when (category) {
        ActionCategory.MOVE -> 0
        ActionCategory.LIGHT -> 1
        ActionCategory.BREATHE -> 2
        ActionCategory.REST -> 3
        ActionCategory.EAT -> 4
        ActionCategory.CONNECT -> 5
    }

    private fun note(
        weather: BodyWeather,
        calibrated: Set<CalibrationTag>,
        activityTilt: ActivityTilt = ActivityTilt.NEUTRAL
    ): String = when {
        calibrated.isNotEmpty() ->
            "按你说的来。这几件都不费力，做不完也没关系。"
        // 运动读数优先解释：今天动得多 / 几乎没动，都比天气更直接。
        activityTilt == ActivityTilt.ACTIVE ->
            "今天已经动得够多了，这几件都偏向放松。"
        activityTilt == ActivityTilt.SEDENTARY ->
            "今天几乎没怎么动，先挑要起身的那一件。"
        weather.kind == WeatherKind.RECOVERING ->
            "今天的目标是少消耗一点，不是多做一点。"
        weather.kind == WeatherKind.PRESSURE ->
            "风大的时候，把动作放小一点反而更容易完成。"
        weather.kind == WeatherKind.ENERGY_HIGH ->
            "有余量的时候做点小事，比等到没力气再硬撑更好。"
        else ->
            "随便挑一件做就行，不用每件都做。"
    }

    /**
     * 用种子做稳定洗牌。
     *
     * 同一天同一时段，处方不应该每次刷新都换一批——
     * 那样用户会觉得这些建议是随机生成的，也就不会认真对待。
     */
    private fun <T> seededOrder(list: List<T>, seed: Long): List<T> {
        if (list.size <= 1) return list
        val indexed = list.withIndex().toMutableList()
        var state = seed
        // 线性同余，够用且不引入依赖
        fun next(): Int {
            state = state * 6364136223846793005L + 1442695040888963407L
            return ((state ushr 33).toInt() and 0x7FFFFFFF)
        }
        for (i in indexed.indices) {
            val j = next() % indexed.size
            val tmp = indexed[i]
            indexed[i] = indexed[j]
            indexed[j] = tmp
        }
        return indexed.map { it.value }
    }

    /**
     * 按**总时长预算**挑动作。
     *
     * 必须按总量卡，不能按单个动作卡：界面上「建议时长 约 N 分钟」
     * 和按钮「开始 N 分钟微行动」用的都是 [Prescription.totalMinutes]，
     * 也就是各动作之和。按单动作过滤会让三件各 2 分钟的动作凑出 6 分钟，
     * 和用户在设置里选的「5 分钟」对不上。
     *
     * 想要 want 件，装不下就少给——宁可给 2 件真能做完的，
     * 也不给 3 件做不完的。但少于 [MIN_STEPS] 件时卡片太空，
     * 会按「短的优先」重挑一遍，尽量在同样的预算里塞进更多件。
     * 重挑**不放松总时长**，只是换一批更省时间的组合。
     */
    private fun pickWithinBudget(
        pool: List<MicroAction>,
        want: Int,
        budget: Int,
        seed: Long
    ): List<MicroAction> {
        if (pool.isEmpty()) return emptyList()
        // 22.49.0：这里不再自己洗牌——池子已由 [orderByTilt] 按「倾斜分组 + 组内种子乱序」排好，
        // 再洗一遍会把倾斜冲掉。
        val shuffled = pool
        val out = ArrayList<MicroAction>(want)
        var total = 0
        for (action in shuffled) {
            if (out.size >= want) break
            if (total + action.minutes <= budget) {
                out += action
                total += action.minutes
            }
        }
        // 22.53.0：尽量用满用户设的时长预算。
        // 以前按种子随机取，经常只取到预算的一半就收手（设了 10 分钟只给 5 分钟），
        // 用户感觉「这不像我设的」。现在剩下的一半预算还有候选时，按「时长从大到小」
        // 往里塞，塞到下一次就超预算为止——**不超预算**这条没变。
        if (budget > 0 && total < budget) {
            val rest = shuffled.filter { candidate -> out.none { it.id == candidate.id } }
            rest.sortedByDescending { it.minutes }.forEach { candidate ->
                if (out.size >= want) return@forEach
                if (total + candidate.minutes <= budget) {
                    out += candidate
                    total += candidate.minutes
                }
            }
        }

        // 补齐：随机序可能先挑走几个长动作，只剩一两件。卡片要列 3–5 条，
        // 所以条数不够时改用「短的优先」重挑——预算一分不放松，
        // 只是换一批更省时间的组合，让条数尽量够。
        if (out.size < MIN_STEPS) {
            val byMinutes = shuffled.sortedBy { it.minutes }
            val refill = ArrayList<MicroAction>(want)
            var sum = 0
            for (action in byMinutes) {
                if (refill.size >= want) break
                if (sum + action.minutes <= budget) {
                    refill += action
                    sum += action.minutes
                }
            }
            // 只有确实变多了才替换，否则保持随机序的结果（同一天稳定）。
            if (refill.size > out.size) return refill
        }
        // 兜底：预算比库里最短的动作还小，也至少给一条，绝不返回空处方。
        if (out.isEmpty()) out += shuffled.minBy { it.minutes }
        return out
    }

    private fun ticketNo(seed: Long): String {
        val s = abs(seed).toString().padStart(6, '0').takeLast(6)
        return "BC-$s"
    }

    private fun currentHour(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
}

/** 指标 key 的集中定义，避免页面里写裸字符串。 */
object WeatherBodyWeatherKeys {
    const val RECOVERY = "recovery"
    const val PRESSURE = "pressure"
    const val ENERGY = "energy"
    const val SLEEP = "sleep"
}
