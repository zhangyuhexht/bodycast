package cn.pocket.toolbox.bodycast

import cn.pocket.toolbox.i18n.BodyCastText

/**
 * 五种身体天气。
 *
 * 只有五种，不是巧合：分类越多，用户越容易开始比较「哪种更好」。
 * 五种里面没有好坏，只有状态——[RECOVERING] 和 [ENERGY_HIGH] 一样正当。
 */
enum class WeatherKind(
    private val sourceLabel: String,
    /** 气象球的配色（雾蓝 / 橙黄 / 灰蓝 / 绿 / 紫） */
    val colorHex: Long,
    /** 同色系的深色，用于渐变边缘 */
    val deepHex: Long,
    /** 一句话概括，用在分享卡和颜色说明里 */
    private val sourceMotto: String
) {
    RECOVERING("恢复天", 0xFF7FA8C9, 0xFF41637F, "云在慢慢散，不用急着赶路"),
    ENERGY_HIGH("高能天", 0xFFE8A33D, 0xFF9C6414, "阳光正好，可以往外走一点"),
    PRESSURE("压力天", 0xFF6E7C8C, 0xFF3A4451, "风有点大，先找个避风的地方"),
    BALANCED("平衡天", 0xFF5FAE8A, 0xFF2C6A52, "今天的身体，是稳的"),
    SENSITIVE("敏感天", 0xFF9B8CC4, 0xFF5A4A85, "身体在说话，先听它说完");

    val label: String get() = BodyCastText.text(sourceLabel)
    val motto: String get() = BodyCastText.text(sourceMotto)

    companion object {
        fun fromId(id: String?): WeatherKind? = entries.firstOrNull { it.name == id }
    }
}

/**
 * 一项指标的档位。
 *
 * [level] 只允许用天气词，**永远不出现数字**。
 * 「恢复云量：多云」和「恢复指数 62」的区别在于：后者会变成一个新的比较维度。
 */
data class WeatherMetric(
    val key: String,
    private val sourceTitle: String,
    private val sourceLevel: String,
    private val sourceHint: String,
    /** 是否因为数据缺失而只能给模糊结论 */
    val uncertain: Boolean = false
) {
    val title: String get() = BodyCastText.text(sourceTitle)
    val level: String get() = BodyCastText.text(sourceLevel)
    val hint: String get() = BodyCastText.text(sourceHint)
}

/** 播报文案。 */
data class Broadcast(
    private val sourceHeadline: String,
    private val sourceBody: String,
    /** 「为什么这样播报」——只讲定性依据，不给数值。 */
    private val sourceReason: String,
    /** 天气诗 */
    private val sourcePoem: String,
    /**
     * 部分信号可用时的如实说明（22.27.0）：「今天是按 N/4 个信号算的，
     * 缺的那 M 个显示『暂时没读到』」。null = 不需要这句——四个信号都有值，
     * 或者结论是手动校准定的（校准优先级最高，不按信号数算）。
     */
    private val sourceCoverage: String? = null
) {
    val headline: String get() = BodyCastText.text(sourceHeadline)
    val body: String get() = BodyCastText.text(sourceBody)
    val reason: String get() = BodyCastText.text(sourceReason)
    val poem: String get() = BodyCastText.text(sourcePoem)
    val coverage: String? get() = sourceCoverage?.let { BodyCastText.text(it) }
}

/**
 * 一次完整的身体天气。
 *
 * [calibrated] 非空时，[kind] 一定是 [WeatherKind.SENSITIVE]：
 * 用户说了「累」，天气就必须是敏感天，设备读数不再参与定档。
 */
data class BodyWeather(
    val kind: WeatherKind,
    val metrics: List<WeatherMetric>,
    val broadcast: Broadcast,
    val source: SignalSource,
    val updatedAt: Long,
    val calibrated: Set<CalibrationTag> = emptySet(),
    /** 没读到的数据项，界面用「暂时没读到」列出，不补零。 */
    val missing: List<String> = emptyList(),
    /** 是否来自演示数据（界面要打标） */
    val demo: Boolean = false
) {
    val hasCalibration: Boolean get() = calibrated.isNotEmpty()

    /** 指标 key 与顺序固定为四项，便于页面直接取。 */
    fun metric(key: String): WeatherMetric? = metrics.firstOrNull { it.key == key }

    companion object {
        const val KEY_RECOVERY = "recovery"
        const val KEY_PRESSURE = "pressure"
        const val KEY_ENERGY = "energy"
        const val KEY_SLEEP = "sleep"
    }
}

/** 天气诗。按天气类型给几句，取哪一句由引擎按日期决定，保证同一天稳定。 */
internal object WeatherPoems {
    private val poems: Map<WeatherKind, List<String>> = mapOf(
        WeatherKind.RECOVERING to listOf(
            "潮水退下去的时候，沙滩才有机会被看见。",
            "云在慢慢散，不用急着赶路。",
            "不是所有的低，都需要被修好。"
        ),
        WeatherKind.ENERGY_HIGH to listOf(
            "阳光正好，可以往外走一点。",
            "风从背后推了一把，那就走两步。",
            "今天的光是多的，用一点就好。"
        ),
        WeatherKind.PRESSURE to listOf(
            "风有点大，先找个避风的地方。",
            "树在风里摇，根还牢牢地站着。",
            "紧的时候，先把肩膀放下来。"
        ),
        WeatherKind.BALANCED to listOf(
            "今天的身体，是稳的。",
            "没有风，也没有雾，就这样走。",
            "平稳不是没有起伏，是起伏刚好在能承受的范围里。"
        ),
        WeatherKind.SENSITIVE to listOf(
            "身体在说话，先听它说完。",
            "有些天气不适合解释，只适合待着。",
            "敏感不是弱点，是天线更长。"
        )
    )

    fun forKind(kind: WeatherKind, seed: Long): String {
        val list = poems[kind] ?: return ""
        val index = ((seed % list.size) + list.size).toInt() % list.size
        return list[index]
    }
}
