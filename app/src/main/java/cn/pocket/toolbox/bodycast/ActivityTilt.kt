package cn.pocket.toolbox.bodycast

/**
 * 今天的运动量把处方往哪边推（22.49.0）。
 *
 * 之前处方只看天气与时段，跟「今天到底动没动」无关：一个走了 12000 步的人
 * 和躺了一天的人，拿到的是同一套「起来走一走」。这一层把运动读数接进
 * 微行动的选择，规则只有两条、阈值也只有两个：
 *
 * - [ACTIVE]：中高强度 ≥30 分钟或步数 ≥8000 —— 今天动得够多了，
 *   处方偏向**放松/呼吸/光照**这一类，不再劝人「再出去走走」；
 * - [SEDENTARY]：步数 <2000（且确实读到了步数）—— 今天几乎没动，
 *   处方偏向**活动类**，先让人站起来;
 * - [NEUTRAL]：其余情况（含读数缺失）—— 保持原来的口径，不做任何倾斜。
 *
 * 读数缺失时**不算久坐**：没读到不等于没动过，这是这一版反复守的那条线。
 */
enum class ActivityTilt {
    ACTIVE,
    SEDENTARY,
    NEUTRAL;

    companion object {
        private const val ACTIVE_STEPS = 8_000
        private const val ACTIVE_MINUTES = 30
        private const val SEDENTARY_STEPS = 2_000

        fun of(steps: Int?, activityMinutes: Int?): ActivityTilt = when {
            (activityMinutes ?: 0) >= ACTIVE_MINUTES || (steps ?: 0) >= ACTIVE_STEPS -> ACTIVE
            steps != null && steps < SEDENTARY_STEPS -> SEDENTARY
            else -> NEUTRAL
        }
    }
}
