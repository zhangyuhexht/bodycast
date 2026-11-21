package cn.pocket.toolbox.bodycast

/**
 * 把一轮微行动的总时长按**每一步自己的动作量**分下去（22.55.0）。
 *
 * 用户报的问题是：「有些步骤（比如'站起来走 20 步'）看一眼、听一句就做完了，
 * 可应用给它留了和别的步骤一样长的时间，完全是浪费。」
 *
 * 做法：每一步先估一个「动作量」（秒），再按比例把总时长分下去——
 * 估法只认文本里写明的量，不猜：
 * - `N 秒` → N 秒；`N 分钟` → N×60 秒；
 * - `N 次 / 轮 / 遍 / 组` → N×3 秒（一次一到两秒的动作量，取一个不紧不慢的中间值）；
 * - 都没有：短句（≤ 14 字）按 8 秒（一句话就读完的交代），长句按 25 秒（要照着做的）；
 * - 每一步最少 5 秒，避免除零与「一闪而过根本来不及看」。
 *
 * 分配结果保证**总和等于总时长**（用最大余额法补齐差值），
 * 这样倒计时与步骤推进始终是一条线，不会各说各话。
 */
object StepPacing {

    private const val MIN_STEP_SECONDS = 5
    private const val SHORT_LINE_SECONDS = 8
    private const val LONG_LINE_SECONDS = 25
    private const val PER_REPEAT_SECONDS = 3

    private val SECONDS = Regex("(\\d+)\\s*秒")
    private val MINUTES = Regex("(\\d+)\\s*分钟")
    private val REPEATS = Regex("(\\d+)\\s*(次|轮|遍|组|个)")

    /** 估算一步的动作量（秒）。 */
    internal fun weightOf(line: String): Int {
        MINUTES.find(line)?.let { return it.groupValues[1].toInt() * 60 }
        SECONDS.find(line)?.let { return it.groupValues[1].toInt() }
        REPEATS.find(line)?.let { return it.groupValues[1].toInt() * PER_REPEAT_SECONDS }
        return if (line.length <= 14) SHORT_LINE_SECONDS else LONG_LINE_SECONDS
    }

    /**
     * 每一步分到多少秒。返回的列表长度与 [lines] 相同，总和 = [totalSeconds]。
     */
    fun plan(lines: List<String>, totalSeconds: Int): List<Int> {
        if (lines.isEmpty()) return emptyList()
        val total = totalSeconds.coerceAtLeast(lines.size * MIN_STEP_SECONDS)
        val weights = lines.map { weightOf(it).coerceAtLeast(1) }
        val weightSum = weights.sum().coerceAtLeast(1)

        // 先按比例取整（向下），再用最大余额把剩下的秒数补回给「余数最大」的几步。
        val raw = weights.map { it.toDouble() * total / weightSum }
        val base = raw.map { it.toInt().coerceAtLeast(MIN_STEP_SECONDS) }.toMutableList()
        var assigned = base.sum()
        // 超出总量时从最长的一步往下削（仍不小于下限）
        while (assigned > total) {
            val idx = base.indices.maxByOrNull { base[it] } ?: break
            if (base[idx] <= MIN_STEP_SECONDS) break
            base[idx] -= 1
            assigned -= 1
        }
        // 还有富余就按余数从大到小补给各步
        val order = raw.indices.sortedByDescending { raw[it] - raw[it].toInt() }
        var cursor = 0
        while (assigned < total && order.isNotEmpty()) {
            base[order[cursor % order.size]] += 1
            assigned += 1
            cursor += 1
        }
        return base
    }
}
