package cn.pocket.toolbox.bodycast

import cn.pocket.toolbox.i18n.BodyCastText

/**
 * 数据来源。
 *
 * 界面必须能一眼看出「这个天气是从哪来的」——演示数据要明确标注，
 * 不能让人以为是自己身体的真实读数。
 */
enum class SignalSource(private val sourceLabel: String) {
    HEALTH_CONNECT("Health Connect"),
    /** 用户照着手环 App 里的数字抄进来的读数（22.24.0）。 */
    MANUAL_ENTRY("手动填写读数"),
    /** 用户从厂商 App 导出、再交给本应用的文件（22.24.0）。 */
    IMPORT("导入数据"),
    /**
     * 用户自己账号里、设备同步到小米云的快照（22.24.0 起的可选通道）。
     *
     * 这条路的读数**不是实时的**：界面上必须显示上次同步时间，
     * 超过 24 小时的快照不参与今天的判定。
     */
    MI_CLOUD("小米账号（云同步）"),
    MANUAL("手动校准"),
    DEMO("演示数据"),
    NONE("还没有数据");

    val label: String get() = BodyCastText.text(sourceLabel)
}

/**
 * 一份「用户亲手给的」读数（手动填写 / 文件导入共用）。
 *
 * 只有聚合值，且**每个字段可空**：null = 没填 / 没读到，绝不写 0。
 * 存进本机的也只有这个形状的值 + 一个填写时间戳，原始明细一律不落盘。
 */
data class EntryValues(
    /** 静息心率，次/分 */
    val restingHeartRate: Int? = null,
    /** 心率变异性，毫秒 */
    val hrv: Int? = null,
    /** 昨夜睡眠总时长，分钟 */
    val sleepMinutes: Int? = null,
    /** 深睡占比，0f..1f */
    val deepSleepRatio: Float? = null,
    /** 今日步数 */
    val steps: Int? = null,
    /** 压力值 0..100（只有文件导入会给这一项，Health Connect 也没有） */
    val stressLevel: Int? = null,
    /** 血氧，% */
    val bloodOxygen: Int? = null,
    /** 今日中高强度活动时长，分钟 */
    val activityMinutes: Int? = null
) {
    /** 有值的字段数量。 */
    val count: Int
        get() = listOf(
            restingHeartRate, hrv, sleepMinutes, deepSleepRatio,
            steps, stressLevel, bloodOxygen, activityMinutes
        ).count { it != null }

    val isEmpty: Boolean get() = count == 0

    /**
     * 「六类」里有几类填了。
     *
     * 六类 = 静息心率 / 心率变异性 / 睡眠（时长与深睡算一类）/ 步数 / 血氧 / 运动时长。
     * 压力值不计入——它不是六类读数之一（界面上的说明跟着这一条走）。
     */
    val categoryCount: Int
        get() = listOf(
            restingHeartRate, hrv,
            sleepMinutes ?: deepSleepRatio,
            steps, bloodOxygen, activityMinutes
        ).count { it != null }

    companion object {
        /** 六类读数的类别数。 */
        const val CATEGORY_TOTAL = 6
    }
}

/**
 * 用户亲手给的读数（手动填写 / 导入）的有效期。
 *
 * 24 小时一过就不再参与今天的天气判定，界面显示「已过期」——
 * 理由和「读不到就说没读到」是同一条：不能让昨天抄的数字冒充今天的状态。
 */
object EntryValidity {

    const val TTL_MS: Long = 24L * 60 * 60 * 1000

    /** 填写/导入时间是不是还在有效期内。0 或未填写 = 没有。 */
    fun isFresh(at: Long, now: Long = System.currentTimeMillis()): Boolean =
        at > 0L && now - at <= TTL_MS
}

/**
 * 手动填写的字段在预填 / 校验时用的取值范围。
 *
 * 这是**输入框的接受范围**，不是判定阈值：超出范围的输入一律拒绝并提示，
 * 不做「聪明换算」，也不硬存。
 */
object EntryRanges {
    val restingHeartRate = 25..200
    val hrv = 5..250
    val sleepMinutes = 0..(24 * 60)
    val steps = 0..100_000
    val bloodOxygen = 50..100
    val activityMinutes = 0..(24 * 60)
}

/**
 * 「上次读取」的时长纪律（22.27.0）。
 *
 * 两条规则：
 * - 回前台时隔 [AUTO_REFRESH_MS] 以上才自动轻量重读一次（演示数据模式下不自动读）；
 * - 上次读取超过 [STALE_MS] 就在首页 / 数据源页提示「数据可能过期，点此刷新」。
 *
 * `now` 一律由调用方传进来（有默认值）：这样单测可以注入时间戳，
 * 不用真的等 30 分钟来验证提示会不会出现。
 */
object FetchFreshness {

    /** 回前台自动刷新的节流窗口。 */
    const val AUTO_REFRESH_MS: Long = 5L * 60 * 1000

    /** 超过这个时长就提示数据可能过期。 */
    const val STALE_MS: Long = 30L * 60 * 1000

    /** 该不该自动重读：从没读过（0）也算该读。 */
    fun shouldAutoRefresh(lastFetchAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        now - lastFetchAt >= AUTO_REFRESH_MS

    /** 是不是已经「可能过期」。没读过（0）不算过期——那是「还没有读取过」。 */
    fun isStale(lastFetchAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        lastFetchAt > 0L && now - lastFetchAt >= STALE_MS
}

/**
 * 统一内部模型：设备与手动输入都先落到这里，页面不直接接触任何厂商 SDK。
 *
 * **所有字段可空，null 表示「未知」，不表示 0。**
 * 这是本产品的底线：缺数据就说不确定，绝不用 0 冒充真实读数。
 */
data class BodySignals(
    val source: SignalSource = SignalSource.NONE,
    val updatedAt: Long = 0L,
    /** 静息心率，次/分 */
    val restingHeartRate: Int? = null,
    /**
     * 静息心率是不是估算出来的。
     *
     * 厂商没写 RestingHeartRateRecord 时，会用 48 小时里的最低十分位动态心率兜底；
     * 那是个估算值，界面上必须标出来，不能冒充厂商写的记录。
     */
    val restingHeartRateEstimated: Boolean = false,
    /** 心率变异性，毫秒 */
    val hrv: Int? = null,
    /** 昨夜睡眠总时长，分钟 */
    val sleepMinutes: Int? = null,
    /** 深睡占比，0f..1f */
    val deepSleepRatio: Float? = null,
    /** 今日步数 */
    val steps: Int? = null,
    /** 压力值 0..100，越低越放松 */
    val stressLevel: Int? = null,
    /** 血氧，% */
    val bloodOxygen: Int? = null,
    /** 今日中高强度活动时长，分钟 */
    val activityMinutes: Int? = null,
    /**
     * 这次读到的记录是哪几个 App 写进系统健康服务的（包名）。
     *
     * 用来回答「数据到底从哪来的」——用户装了小米又装华为，
     * 或者数据其实是别的 App 写的，光看「已授权」是看不出来的。
     * 空集 = 这次什么都没读到。
     */
    val origins: Set<String> = emptySet(),
    /**
     * 按类型分开记的来源（类型键 → 包名），自检卡片第 ④ 步用。
     *
     * 键是 `HealthConnectBridge.DataType.key`（steps / sleep / resting_heart_rate /
     * hrv / blood_oxygen / exercise）。这里用字符串而不是枚举，是为了让
     * [BodySignals] 不反向依赖数据层。
     */
    val originByType: Map<String, String> = emptyMap()
) {
    /** 有值的字段数量。 */
    val availableCount: Int
        get() = listOf(
            restingHeartRate, hrv, sleepMinutes, deepSleepRatio,
            steps, stressLevel, bloodOxygen, activityMinutes
        ).count { it != null }

    val hasAnything: Boolean get() = availableCount > 0

    /**
     * 缺失字段的中文名。
     *
     * 用「暂时没有读到」而不是「缺失」——后者像在指责用户。
     */
    val missing: List<String>
        get() = buildList {
            if (restingHeartRate == null) add("静息心率")
            if (hrv == null) add("心率变异性")
            if (sleepMinutes == null) add("睡眠时长")
            if (deepSleepRatio == null) add("深睡占比")
            if (steps == null) add("步数")
            if (stressLevel == null) add("压力值")
            if (bloodOxygen == null) add("血氧")
            if (activityMinutes == null) add("活动时长")
        }

    companion object {
        /** 演示数据。用于没有设备、没有授权时也能看到完整效果，界面会标明来源。 */
        fun demo(): BodySignals = BodySignals(
            source = SignalSource.DEMO,
            updatedAt = System.currentTimeMillis(),
            restingHeartRate = 62,
            hrv = 48,
            sleepMinutes = 445,
            deepSleepRatio = 0.21f,
            steps = 3480,
            stressLevel = 38,
            bloodOxygen = 97,
            activityMinutes = 26
        )
    }
}

/**
 * 多源合并的纯函数（22.27.0 从 [BodyCastStore] 里抽出来，方便单测锁住优先级）。
 *
 * 优先级是产品铁律的延伸：**手动填读数 > 小米云 > 文件导入 > 设备（HC）**，
 * 且**只按字段补**——高优先级没填的字段继续往下找，一路都没有才是 null，
 * 界面照旧写「暂时没读到」，绝不用 0 或默认值补齐（null ≠ 0）。
 *
 * 手动校准标签（改结论）不参与这一层：它比所有读数都高一层，直接改写结论。
 * 各路读数的 24 小时有效期也由调用方判断（[EntryValidity]），这里只做合并。
 */
object SignalMerge {

    fun merge(
        base: BodySignals,
        manual: EntryValues? = null,
        manualAt: Long = 0L,
        cloud: EntryValues? = null,
        cloudAt: Long = 0L,
        imported: EntryValues? = null,
        importedAt: Long = 0L
    ): BodySignals {
        // 演示模式直接返回演示读数：那整份都是假数据，横幅必须如实标注，
        // 往上叠真实读数会让「这是演示」这句话变得不成立。
        if (base.source == SignalSource.DEMO) return base
        if (manual == null && cloud == null && imported == null) return base

        // 同层里小米云优先于导入：它是用户自己账号里设备同步上来的原始快照。
        fun <T> pick(m: T?, c: T?, i: T?, d: T?): T? = m ?: c ?: i ?: d
        val restingHeartRate = pick(
            manual?.restingHeartRate, cloud?.restingHeartRate,
            imported?.restingHeartRate, base.restingHeartRate
        )
        val source = when {
            manual != null -> SignalSource.MANUAL_ENTRY
            cloud != null -> SignalSource.MI_CLOUD
            imported != null -> SignalSource.IMPORT
            else -> base.source
        }
        return base.copy(
            source = source,
            updatedAt = maxOf(
                base.updatedAt,
                if (manual != null) manualAt else 0L,
                if (cloud != null) cloudAt else 0L,
                if (imported != null) importedAt else 0L
            ),
            restingHeartRate = restingHeartRate,
            // 手动填 / 导入 / 云端的心率都不是本应用估的，所以「估算」标记
            // 只在仍然用设备估的那个值时才留着。
            restingHeartRateEstimated = base.restingHeartRateEstimated &&
                restingHeartRate == base.restingHeartRate,
            hrv = pick(manual?.hrv, cloud?.hrv, imported?.hrv, base.hrv),
            sleepMinutes = pick(manual?.sleepMinutes, cloud?.sleepMinutes, imported?.sleepMinutes, base.sleepMinutes),
            deepSleepRatio = pick(
                manual?.deepSleepRatio, cloud?.deepSleepRatio,
                imported?.deepSleepRatio, base.deepSleepRatio
            ),
            steps = pick(manual?.steps, cloud?.steps, imported?.steps, base.steps),
            stressLevel = imported?.stressLevel ?: base.stressLevel,
            bloodOxygen = pick(manual?.bloodOxygen, cloud?.bloodOxygen, imported?.bloodOxygen, base.bloodOxygen),
            activityMinutes = pick(
                manual?.activityMinutes, cloud?.activityMinutes,
                imported?.activityMinutes, base.activityMinutes
            )
        )
    }
}

/**
 * 手动校准标签。
 *
 * 用户说什么就是什么——校准一旦发生，它的优先级高于所有设备读数。
 * 这是「手动校准永远可用且优先」的落地方式：不是加个权重，是直接改写结论。
 */
enum class CalibrationTag(private val sourceLabel: String) {
    TIRED("累"),
    TENSE("紧"),
    BLOATED("胀"),
    SLEEPY("困"),
    ANNOYED("烦"),
    LIGHT("轻"),
    EMPTY("空"),
    HUNGRY("饿"),
    UNSPEAKABLE("说不清");

    val label: String get() = BodyCastText.text(sourceLabel)

    companion object {
        /** 界面上的展示顺序：先身体感受，再情绪，最后「说不清」。 */
        val DISPLAY_ORDER: List<CalibrationTag> = listOf(
            TIRED, TENSE, BLOATED, SLEEPY, ANNOYED, LIGHT, EMPTY, HUNGRY, UNSPEAKABLE
        )

        fun fromId(id: String): CalibrationTag? = entries.firstOrNull { it.name == id }
    }
}
