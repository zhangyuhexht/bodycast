package cn.pocket.toolbox.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import cn.pocket.toolbox.bodycast.BodySignals
import cn.pocket.toolbox.i18n.BodyCastText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

/**
 * Health Connect 桥接层。
 *
 * 手环接入**不走蓝牙**。统一链路是：
 *
 * ```
 * 小米手环 → 小米运动健康 ─┐
 *                         ├→ Health Connect → BodyCast
 * 华为手环 → 华为运动健康 ─┘
 * ```
 *
 * 应用这边只做四件事：
 * 1. 检测系统健康平台是否可用；
 * 2. 不可用时给出跳转（应用商店 / 系统设置）；
 * 3. 按需最小化申请读取权限，用户拒绝就降级，绝不反复骚扰；
 * 4. 读取最近 24 小时与最近 7 天的数据，转成内部统一模型 [BodySignals]。
 *
 * 小米 SDK、华为 Health Service Kit 只作为可选补充适配器，**不进入业务层**。
 */
object HealthConnectBridge {

    /** 系统健康平台的可用状态。 */
    enum class Availability(private val sourceLabel: String) {
        AVAILABLE("已就绪"),
        NEED_INSTALL("需要安装系统健康服务"),
        NEED_UPDATE("需要更新系统健康服务"),
        UNSUPPORTED("这台设备暂不支持");

        val label: String get() = BodyCastText.text(sourceLabel)
    }

    /**
     * 需要申请的全部**读取**权限。
     *
     * 读取这条路只读，不申请写入；写入桥（22.42.0）走 [HealthConnectWriter.writePermissions]，
     * 只在用户打开「把读数写入系统健康服务」开关时单独申请。
     */
    val permissions: Set<String> get() = HealthConnectSource.permissions

    /** 权限类型 → 中文名。设置页用来列出「已授权类型」。 */
    val permissionLabels: Map<String, String>
        get() = DataType.entries.associate { it.permission to it.label }

    suspend fun availability(context: Context): Availability = runCatching {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEED_UPDATE
            else -> Availability.NEED_INSTALL
        }
    }.getOrDefault(Availability.UNSUPPORTED)

    suspend fun isAvailable(context: Context): Boolean = availability(context) == Availability.AVAILABLE

    /** 当前已经拿到的权限。读不到就返回空集，不抛异常。 */
    suspend fun grantedPermissions(context: Context): Set<String> = runCatching {
        if (!isAvailable(context)) return emptySet()
        HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
    }.getOrDefault(emptySet())

    /** 已授权的类型（中文名），按 [permissionLabels] 顺序。 */
    suspend fun grantedTypeLabels(context: Context): List<String> {
        val granted = grantedPermissions(context)
        return permissionLabels.filterKeys { granted.contains(it) }.values.toList()
    }

    /** 是否六项全部授权。 */
    suspend fun allGranted(context: Context): Boolean =
        grantedPermissions(context).containsAll(permissions)

    // -----------------------------------------------------------------------
    // 自检：四步流水线
    //
    // 「读不到」有四种完全不同的原因：没装健康服务 / 没授权 / 厂商没往里写 /
    // 写了但应用读不到。只给一句「暂时没读到」，用户没法判断该做什么。
    // 下面这一组结构把四步各自的结果分开返回，界面才能对断掉的那一步给一条出路。
    // 所有判断都是**应用可自证**的：只看系统状态、授权列表、记录本身。
    // -----------------------------------------------------------------------

    /** 一步的三态。 */
    enum class StepState(private val sourceLabel: String) {
        OK("通"),
        BROKEN("断"),
        UNKNOWN("未知");

        val label: String get() = BodyCastText.text(sourceLabel)
    }

    /**
     * 六类数据的类型表：中文名、对应权限、以及「这次读到没有」怎么取值。
     *
     * 顺序 = 设置页与自检卡片的显示顺序。
     */
    enum class DataType(
        val key: String,
        private val sourceLabel: String,
        val permission: String,
        private val extract: (BodySignals) -> Int?
    ) {
        STEPS("steps", "步数", "android.permission.health.READ_STEPS", { it.steps }),
        SLEEP("sleep", "睡眠", "android.permission.health.READ_SLEEP", { it.sleepMinutes }),
        RESTING_HEART_RATE(
            "resting_heart_rate", "静息心率",
            "android.permission.health.READ_RESTING_HEART_RATE", { it.restingHeartRate }
        ),
        HRV(
            "hrv", "心率变异性 HRV",
            "android.permission.health.READ_HEART_RATE_VARIABILITY", { it.hrv }
        ),
        BLOOD_OXYGEN(
            "blood_oxygen", "血氧",
            "android.permission.health.READ_OXYGEN_SATURATION", { it.bloodOxygen }
        ),
        EXERCISE(
            "exercise", "运动记录",
            "android.permission.health.READ_EXERCISE", { it.activityMinutes }
        );

        val label: String get() = BodyCastText.text(sourceLabel)

        fun valueOf(signals: BodySignals): Int? = extract(signals)
    }

    /** 第 ③ 步：某一类数据「最后一次写入」的时间与来源。 */
    data class WriteInfo(
        val type: DataType,
        val permitted: Boolean,
        val at: Instant?,
        val origin: String?
    ) {
        val written: Boolean get() = at != null

        /** 「最后写入」那一列要显示的字。 */
        val timeLabel: String
            get() = when {
                at != null -> writtenAt(at)
                !permitted -> BodyCastText.text("未授权")
                else -> BodyCastText.text("无记录")
            }

        /** 来源的中文名（没写过时为 null）。 */
        val originName: String? get() = origin?.let { originLabel(it) }
    }

    /** 第 ④ 步：本次读取里某一类的结果。 */
    data class ReadInfo(
        val type: DataType,
        val value: Int?,
        val origin: String?,
        val estimated: Boolean
    ) {
        val ok: Boolean get() = value != null
        val originName: String? get() = origin?.let { originLabel(it) }
    }

    /**
     * 四步自检的结果。界面直接拿它渲染，不再自己拼判断——
     * 「哪一步断了」必须是同一份结论。
     */
    data class Diagnosis(
        val availability: Availability,
        val granted: Set<String>,
        val writes: List<WriteInfo>,
        val reads: List<ReadInfo>,
        val signals: BodySignals
    ) {
        val grantedCount: Int get() = DataType.entries.count { granted.contains(it.permission) }
        val missing: List<DataType>
            get() = DataType.entries.filterNot { granted.contains(it.permission) }
        val allGranted: Boolean get() = missing.isEmpty()

        /** ① 系统健康服务。 */
        val systemState: StepState
            get() = if (availability == Availability.AVAILABLE) StepState.OK else StepState.BROKEN

        /** ② 授权。 */
        val grantState: StepState get() = if (allGranted) StepState.OK else StepState.BROKEN

        /** ③ 厂商有没有往 Health Connect 写过东西。六类全无 = 断。 */
        val writeState: StepState
            get() = if (writes.any { it.written }) StepState.OK else StepState.BROKEN

        /** ④ 本次读数。 */
        val readState: StepState get() = if (reads.any { it.ok }) StepState.OK else StepState.BROKEN

        val readCount: Int get() = reads.count { it.ok }
        val wroteAny: Boolean get() = writes.any { it.written }

        /** ③ 有、④ 无 —— 这是应用侧读取的问题，不是厂商没写。 */
        val writtenButNotRead: Boolean get() = wroteAny && readCount == 0

        /** 最近一次写入（六类里最新的那一条）。 */
        val latestWrite: WriteInfo? get() = writes.filter { it.written }.maxByOrNull { it.at!! }
    }

    /**
     * 第 ③ 步：六类各自的最后写入时间 + 来源包名。
     *
     * 看厂商 App 里有没有开关，不如直接看记录——Health Connect 里存的是记录本身。
     * 每类只读一页（倒序，pageSize = 1），窗口 7 天：既能看出「最近有没有写」，
     * 又不会碰到 30 天读取窗口的限制。
     */
    suspend fun lastWriteTimes(context: Context, granted: Set<String>): List<WriteInfo> =
        runCatching {
            if (!isAvailable(context)) return emptyList()
            val client = HealthConnectClient.getOrCreate(context)
            val now = Instant.now()
            val from = now.minus(Duration.ofDays(7))
            DataType.entries.map { type ->
                if (!granted.contains(type.permission)) {
                    WriteInfo(type, permitted = false, at = null, origin = null)
                } else {
                    val hit = runCatching { lastRecordOf(client, type, from, now) }.getOrNull()
                    WriteInfo(type, permitted = true, at = hit?.first, origin = hit?.second)
                }
            }
        }.getOrDefault(emptyList())

    private suspend fun lastRecordOf(
        client: HealthConnectClient,
        type: DataType,
        from: Instant,
        to: Instant
    ): Pair<Instant, String>? = when (type) {
        DataType.STEPS -> lastRecordOf(client, StepsRecord::class, from, to) { it.startTime }
        DataType.SLEEP -> lastRecordOf(client, SleepSessionRecord::class, from, to) { it.endTime }
        DataType.RESTING_HEART_RATE ->
            lastRecordOf(client, RestingHeartRateRecord::class, from, to) { it.time }
        DataType.HRV ->
            lastRecordOf(client, HeartRateVariabilityRmssdRecord::class, from, to) { it.time }
        DataType.BLOOD_OXYGEN ->
            lastRecordOf(client, OxygenSaturationRecord::class, from, to) { it.time }
        DataType.EXERCISE -> lastRecordOf(client, ExerciseSessionRecord::class, from, to) { it.endTime }
    }

    /** 读一页记录，取时间最晚的那条 →（写入时间, 来源包名）。 */
    private suspend fun <T : Record> lastRecordOf(
        client: HealthConnectClient,
        type: KClass<T>,
        from: Instant,
        to: Instant,
        timeOf: (T) -> Instant
    ): Pair<Instant, String>? {
        // 优先倒序读一页：直接就是最后一条。旧版健康服务不支持倒序时，
        // 退回正序读一页再自己取最大——两条路都不翻页，开销可控。
        val descending = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                    ascendingOrder = false,
                    pageSize = 1
                )
            ).records.firstOrNull()?.let { timeOf(it) to it.metadata.dataOrigin.packageName }
        }.getOrNull()
        if (descending != null) return descending

        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                    pageSize = 100
                )
            ).records.maxByOrNull(timeOf)?.let { timeOf(it) to it.metadata.dataOrigin.packageName }
        }.getOrNull()
    }

    /**
     * 四步一次跑完。自检卡片与手动同步都从这里取结果。
     *
     * 会真的读一次数据（第 ④ 步），所以调用方顺手就能拿到 [Diagnosis.signals]
     * 更新界面，不用读两遍。
     *
     * 整段跑在 IO 线程上：这七八次跨进程调用加起来可能有十几秒（宿主忙的时候），
     * 放在界面线程上就是一次 ANR。返回值回到调用方原来的线程。
     */
    suspend fun diagnose(context: Context): Diagnosis = withContext(Dispatchers.IO) {
        val availability = availability(context)
        val granted = grantedPermissions(context)
        val writes = lastWriteTimes(context, granted)
        val signals = read(context)
        val reads = DataType.entries.map { type ->
            ReadInfo(
                type = type,
                value = type.valueOf(signals),
                origin = signals.originByType[type.key],
                estimated = type == DataType.RESTING_HEART_RATE && signals.restingHeartRateEstimated
            )
        }
        Diagnosis(availability, granted, writes, reads, signals)
    }

    /** 「最后写入」那一列的时间写法：今天 08:12 / 昨天 21:04 / 10月5日 07:30。 */
    fun writtenAt(at: Instant): String {
        val zone = ZoneId.systemDefault()
        val local = at.atZone(zone)
        val time = "%02d:%02d".format(local.hour, local.minute)
        val date = local.toLocalDate()
        val today = LocalDate.now(zone)
        return when (date) {
            today -> BodyCastText.text("今天") + " " + time
            today.minusDays(1) -> BodyCastText.text("昨天") + " " + time
            // 其余日期用纯数字，不依赖语言——两位月份/日期保证排序稳定。
            else -> "%02d-%02d".format(date.monthValue, date.dayOfMonth) + " " + time
        }
    }

    /**
     * 读取最近数据。读不到就是 null，**绝不用 0 冒充真实读数**。
     * 未授权时返回 [BodySignals] 的空实例（source = NONE）。
     *
     * 走 IO 线程：Health Connect 是跨进程调用，宿主慢的时候能等上一会儿，
     * 在界面线程上等就会 ANR（22.22.0 在模拟器上实测踩到过）。
     */
    suspend fun read(context: Context): BodySignals = withContext(Dispatchers.IO) {
        runCatching { HealthConnectSource.read(context) }.getOrElse { BodySignals() }
    }

    // -----------------------------------------------------------------------
    // 跳转
    //
    // 手环厂商的包名体系比较乱，同一家有好几个 App（小米就有「小米运动健康」
    // 「小米穿戴」「小米运动」三个历史包名）。这里一律**多候选 + 逐级回退**：
    //   1. 已安装的厂商 App 启动 Intent（按候选包名依次探测）；
    //   2. 应用商店的「详情页」（按候选包名依次尝试）；
    //   3. 应用商店的「搜索页」（不依赖具体包名，避免商店里查不到而卡死）；
    //   4. 浏览器打开商店网页。
    // 这样在自带商店没有该 App 的机器上，也不会出现「点了没反应」。
    // -----------------------------------------------------------------------

    /**
     * 小米系候选包名。顺序按「**国行 → 国际 → 老版**」排：
     * 1. com.mi.health —— 小米运动健康（**国行版**）。21.x 起「小米点了打不开」的
     *    直接原因就是这张表里没有它（Manifest 里也没声明，包可见性过滤后连
     *    扫描都扫不到）；
     * 2. com.xiaomi.wearable —— 小米运动健康 / Mi Fitness（国际版）；
     * 3. com.xiaomi.hm.health —— Zepp Life / 小米运动（老 App，仍有人在用）；
     * 4. com.xiaomi.wearable2 —— 少数版本用过的包名，放最后兜底。
     *
     * 注意：**这张表只是兜底**。真正优先的是「按应用名扫出来的已安装 App」
     * （见 [xiaomiApps]），因为厂商的包名换过好几次，写死一定会漏。
     */
    private val XIAOMI_PACKAGES = listOf(
        "com.mi.health",
        "com.xiaomi.wearable",
        "com.xiaomi.hm.health",
        "com.xiaomi.wearable2"
    )

    /** 华为系：华为运动健康 + 荣耀运动健康（荣耀分家后有独立包名）。 */
    private val HUAWEI_PACKAGES = listOf(
        "com.huawei.health",
        "com.hihonor.health"
    )

    private val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

    /** 打开 Health Connect 的授权 / 设置页。 */
    fun settingsIntent(): Intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

    /**
     * 已安装的厂商 App。
     *
     * 不写死包名：厂商的包名体系很乱（小米就同时存在小米运动健康 / 小米穿戴 /
     * 小米运动三个包名，而且互相替代过），所以这里直接扫已安装应用，
     * 用「名字里有没有这些关键词」来判断——用户在设置页看到的也是应用名，
     * 这样才不会出现「跳过去装了个别的 App」。
     */
    data class VendorApp(val packageName: String, val label: String, val launch: Intent)

    /**
     * 「包名 → 应用名」一次扫描的结果。
     *
     * 这几个查询都在**界面线程**上跑（设置页 `remember { }` 与浮层打开时），
     * 所以只能扫一次：以前每个厂商各扫一遍 `getInstalledApplications`，
     * 六个厂商就是六遍，还要逐包取应用名，慢机器上直接把主线程压住。
     *
     * 再加一层 5 秒的短缓存：进入数据源页那一瞬间会连着问好几次
     * （小米、华为、全部厂商、自检），短缓存把它们并成一次扫描。
     * 只缓存包名和名字这类字符串，不持有 Context，不会漏。
     */
    private var labelCache: Pair<Long, Map<String, String>>? = null

    private fun installedAppLabels(context: Context): Map<String, String> {
        val now = System.currentTimeMillis()
        labelCache?.let { (at, cached) -> if (now - at < 5_000L) return cached }
        val fresh = runCatching {
            val pm = context.packageManager
            pm.getInstalledApplications(0).associate { info ->
                info.packageName to runCatching { pm.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName)
            }
        }.getOrDefault(emptyMap())
        labelCache = now to fresh
        return fresh
    }

    fun findVendorApps(context: Context, keywords: List<String>): List<VendorApp> = runCatching {
        val pm = context.packageManager
        installedAppLabels(context).mapNotNull { (packageName, label) ->
            val hit = keywords.any {
                label.contains(it, ignoreCase = true) || packageName.contains(it, ignoreCase = true)
            }
            if (!hit) return@mapNotNull null
            val launch = pm.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return@mapNotNull null
            VendorApp(packageName, label, launch)
        }
    }.getOrDefault(emptyList())

    private val XIAOMI_KEYWORDS = listOf(
        "小米运动健康", "小米穿戴", "小米运动", "Zepp Life",
        "Mi Fitness", "Mi Wear", "mi.health", "xiaomi.wearable", "xiaomi.hm.health"
    )
    private val HUAWEI_KEYWORDS = listOf(
        "华为运动健康", "华为健康", "荣耀运动健康", "运动健康",
        "Huawei Health", "Honor Health", "huawei.health", "hihonor.health"
    )
    private val SAMSUNG_KEYWORDS = listOf(
        "三星健康", "Samsung Health", "shealth"
    )
    private val ZEPP_KEYWORDS = listOf(
        "Zepp", "Amazfit", "huami"
    )

    fun xiaomiApps(context: Context): List<VendorApp> = findVendorApps(context, XIAOMI_KEYWORDS)
    fun huaweiApps(context: Context): List<VendorApp> = findVendorApps(context, HUAWEI_KEYWORDS)
    fun samsungApps(context: Context): List<VendorApp> = findVendorApps(context, SAMSUNG_KEYWORDS)
    fun zeppApps(context: Context): List<VendorApp> = findVendorApps(context, ZEPP_KEYWORDS)

    /**
     * 一个厂商 App 的「同步路径」。
     *
     * 路径写成「找什么」（在括号里给准确菜单），不写死第几层菜单——厂商改版频繁，
     * 写死一定会过期。**没说清的宁可写「未证实」**：宁可让用户少抱期待，
     * 也不要写一句「华为手环也能读通」然后让他白折腾。
     */
    data class VendorProfile(
        val id: String,
        /** 界面上显示的厂商名（可翻译）。 */
        private val sourceName: String,
        /** 候选包名，首位最常见。 */
        val packages: List<String>,
        /** 扫描关键词（应用名 / 包名都能命中）。 */
        val keywords: List<String>,
        /** 菜单路径：先说要找什么，再给准确路径。 */
        private val sourcePath: String,
        /** 这个 App 到底会不会往 Health Connect 写。null = 未证实。 */
        val writesHealthConnect: Boolean?,
        /** 限制 / 备注。 */
        private val sourceNote: String
    ) {
        val name: String get() = BodyCastText.text(sourceName)
        val path: String get() = BodyCastText.text(sourcePath)
        val note: String get() = BodyCastText.text(sourceNote)

        /** 归到哪一家，用来分「连接小米手环」「连接华为手环」两个入口。 */
        val family: String
            get() = when {
                packages.any { it.contains("mi.health") || it.contains("xiaomi") || it.contains("huami") } -> "xiaomi"
                packages.any { it.contains("huawei") || it.contains("hihonor") } -> "huawei"
                else -> "other"
            }
    }

    /**
     * 厂商同步路径表（按方案 §3.8 的表格写）。
     *
     * 顺序 = 界面上列出的顺序：先小米国行（用户最多），再国际版、老版，
     * 然后华为 / 荣耀（这两家**不写 Health Connect**，只能走桥接或手动），
     * 最后三星（作为「国行 App 普遍区域化裁剪」的对照）。
     */
    val VENDOR_PROFILES: List<VendorProfile> = listOf(
        VendorProfile(
            id = "mi_cn",
            sourceName = "小米运动健康（国行）",
            packages = listOf("com.mi.health"),
            keywords = XIAOMI_KEYWORDS,
            sourcePath = "打开小米运动健康（国行），在它的设置里找「三方数据管理」：我的 → 设置 → 三方数据管理 → 授权 Health Connect",
            writesHealthConnect = true,
            sourceNote = "这一项在大陆区域可能被隐藏；看不到就等于这条路在本机走不通。官方的健康云开放接口目前也只对合作企业开放。授权前产生的历史不会补写。"
        ),
        VendorProfile(
            id = "mi_intl",
            sourceName = "小米运动健康（国际版 / Mi Fitness）",
            packages = listOf("com.xiaomi.wearable", "com.xiaomi.wearable2"),
            keywords = XIAOMI_KEYWORDS,
            sourcePath = "打开小米运动健康（国际版），在它的设置里找「三方数据管理」：我的 → 三方数据管理 → 授权 Health Connect",
            writesHealthConnect = true,
            sourceNote = "第三方教程都指明要用国际版（没有支付宝的那个）；国行手机能不能登录国际版需要你自己验证。"
        ),
        VendorProfile(
            id = "zepp_life",
            sourceName = "Zepp Life / 小米运动（老版）",
            packages = listOf("com.xiaomi.hm.health", "com.huami.watch.hmwatchmanager"),
            keywords = ZEPP_KEYWORDS,
            sourcePath = "打开 Zepp Life（老版小米运动），找「第三方接入」：我的 → 第三方接入",
            writesHealthConnect = null,
            sourceNote = "官方资料里只提到微信这类第三方绑定，有没有 Health Connect 开关未证实。"
        ),
        VendorProfile(
            id = "huawei",
            sourceName = "华为运动健康",
            packages = listOf("com.huawei.health"),
            keywords = HUAWEI_KEYWORDS,
            sourcePath = "打开华为运动健康，找「数据分享与授权」：我的 → 隐私管理 → 数据分享与授权",
            writesHealthConnect = false,
            sourceNote = "华为不把数据写进系统健康服务：官方接口（Health Service Kit）要企业开发者资质，个人开发者拿不到。这是厂商限制，本应用修不了，走下面的替代路。"
        ),
        VendorProfile(
            id = "honor",
            sourceName = "荣耀运动健康",
            packages = listOf("com.hihonor.health"),
            keywords = HUAWEI_KEYWORDS,
            sourcePath = "打开荣耀运动健康，找「数据分享、授权与导入」：我的 → 隐私管理 → 数据分享、授权与导入",
            writesHealthConnect = false,
            sourceNote = "没找到任何官方证据说它写 Health Connect，能确认的只有它自己的分享入口。"
        ),
        VendorProfile(
            id = "samsung",
            sourceName = "三星健康",
            packages = listOf("com.sec.android.app.shealth"),
            keywords = SAMSUNG_KEYWORDS,
            sourcePath = "打开三星健康 → 设置 →「Health Connect」并打开同步（v6.22.5 起与系统健康服务双向同步）",
            writesHealthConnect = true,
            sourceNote = "国行版 App 普遍有区域化裁剪，找不到开关不是本应用的问题。"
        )
    )

    /** 这台机器上真实装到的厂商 App + 它的同步路径。 */
    data class InstalledVendor(val profile: VendorProfile, val app: VendorApp)

    /**
     * 每个厂商自己的识别关键词。
     *
     * 华为和荣耀必须分开：两家的应用名里都有「运动健康」，
     * 共用一个表的话，装了华为 App 会把荣耀的路径也列出来（实测踩到过）。
     */
    private val VENDOR_KEYWORDS: Map<String, List<String>> = mapOf(
        "mi_cn" to listOf("小米运动健康", "Mi Fitness", "mi.health"),
        "mi_intl" to listOf("Mi Fitness", "Mi Wear", "xiaomi.wearable"),
        "zepp_life" to listOf("Zepp Life", "小米运动", "hm.health", "huami", "Amazfit"),
        "huawei" to listOf("华为运动健康", "华为健康", "Huawei Health", "huawei.health"),
        "honor" to listOf("荣耀运动健康", "荣耀健康", "Honor Health", "hihonor.health"),
        "samsung" to listOf("三星健康", "Samsung Health", "shealth")
    )

    /**
     * 只返回**这台机器上真实装到**的厂商 App + 它的同步路径。
     *
     * 两轮，顺序很重要：
     * 1. 先按候选**包名精确匹配**——包名是唯一可靠的判据；
     * 2. 再按**应用名扫描**补漏（厂商换过包名、表里没有的情况），
     *    但**一个包只归给一个厂商**：华为和荣耀的关键词本来就互相包含，
     *    不设这道闸，装一个华为 App 会把荣耀的路径也一起列出来。
     */
    fun installedVendors(context: Context): List<InstalledVendor> {
        val pm = context.packageManager
        val labels = installedAppLabels(context)
        val used = linkedSetOf<String>()
        val result = mutableListOf<InstalledVendor>()

        fun take(profile: VendorProfile, packageName: String?) {
            if (packageName == null || !used.add(packageName)) return
            val label = labels[packageName] ?: return
            val launch = pm.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
            result += InstalledVendor(profile, VendorApp(packageName, label, launch))
        }

        // 1) 候选包名精确匹配（最可靠）；
        VENDOR_PROFILES.forEach { profile ->
            take(profile, profile.packages.firstOrNull { labels.containsKey(it) })
        }
        // 2) 剩下的按应用名补漏（厂商换过包名的情况），仍是一个包只归一个厂商。
        VENDOR_PROFILES.forEach { profile ->
            val keywords = VENDOR_KEYWORDS[profile.id] ?: profile.keywords
            val hit = labels.entries.firstOrNull { (packageName, label) ->
                packageName !in used && keywords.any {
                    label.contains(it, ignoreCase = true) || packageName.contains(it, ignoreCase = true)
                }
            }
            take(profile, hit?.key)
        }
        return result
    }

    /**
     * 这台机器上装了「不写 Health Connect（或未证实会写）」的厂商 App 吗。
     * 华为 / 荣耀是桥接引导的触发条件之一。
     */
    fun needsBridge(context: Context): Boolean =
        installedVendors(context).any { it.profile.family == "huawei" && it.profile.writesHealthConnect != true }

    /**
     * 六类数据的读取结果：类型名 → 这次到底读到没有。
     *
     * 全都没读到，几乎一定是厂商 App 还没把数据同步进 Health Connect，
     * 而不是本应用的问题——这份报告就是让用户能自己判断卡在哪一步。
     */
    fun typeReport(signals: BodySignals): List<Pair<String, Boolean>> =
        DataType.entries.map { it.label to (it.valueOf(signals) != null) }

    /**
     * 小米：**先用扫出来的已安装 App**（按应用名匹配，不看包名），再退回包名表，
     * 最后才是商店链路。
     *
     * 21.0.0 及以前是先按写死的包名表找，用户报告「小米运动健康点了打不开」——
     * 因为装的是 com.xiaomi.wearable，而表里把另一个包名排在了前面，跳过去失败
     * 后就一路掉到商店。改成「先看装了什么」之后就不会再出现这种情况。
     */
    fun xiaomiIntents(context: Context): List<Intent> =
        vendorIntents(context, XIAOMI_PACKAGES, "小米运动健康", xiaomiApps(context))

    /** 华为 / 荣耀：同上。 */
    fun huaweiIntents(context: Context): List<Intent> =
        vendorIntents(context, HUAWEI_PACKAGES, "华为运动健康", huaweiApps(context))

    /** Health Connect：商店详情 → 商店搜索 → 网页。 */
    fun healthConnectIntents(context: Context, appName: String): List<Intent> = buildList {
        launchIntentFor(context, HEALTH_CONNECT_PACKAGE)?.let { add(it) }
        add(storeDetailsIntent(HEALTH_CONNECT_PACKAGE))
        add(storeSearchIntent(appName))
        add(webIntent(HEALTH_CONNECT_PACKAGE))
    }

    /**
     * 「这台机器上没装厂商 App」时的商店入口：**只按名字搜，不绑定包名**。
     *
     * 数据源页的 ③ 在没有检测到任何厂商 App 时用这一条。此时不知道用户戴的是哪家的表，
     * 按某个品牌写死包名会把小米用户送去华为的页面；搜索页 + 网页兜底两条就够。
     * 搜索词由调用方按界面语言给出（见 `BodyCastText`），这里不做翻译。
     */
    fun storeSearchIntents(appName: String): List<Intent> = listOf(
        storeSearchIntent(appName),
        webSearchIntent(appName)
    )

    private fun vendorIntents(
        context: Context,
        packages: List<String>,
        displayName: String,
        detected: List<VendorApp> = emptyList()
    ): List<Intent> = buildList {
        // 1. 扫出来（按应用名匹配）的已安装 App 最可靠，先试它；
        detected.forEach { add(it.launch) }
        // 2. 再按候选包名试；
        packages.forEach { pkg -> launchIntentFor(context, pkg)?.let { add(it) } }
        // 3. 商店详情 → 搜索页 → 网页。
        packages.forEach { pkg -> add(storeDetailsIntent(pkg)) }
        add(storeSearchIntent(displayName))
        packages.firstOrNull()?.let { add(webIntent(it)) }
    }

    /** 已安装且可启动时返回启动 Intent，否则 null。 */
    private fun launchIntentFor(context: Context, packageName: String): Intent? =
        runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.getOrNull()

    private fun storeDetailsIntent(packageName: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))

    /** 商店搜索：不依赖具体包名，在只认本地商店的机器上更靠谱。 */
    private fun storeSearchIntent(query: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}"))

    private fun webIntent(packageName: String): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        )

    /** Play 网页版搜索：本地商店完全用不了时的最后一跳。 */
    private fun webSearchIntent(query: String): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps")
        )

    /**
     * 依次尝试一组 Intent，返回是否有一个真的打开了。
     * 全部失败时返回 false，由界面提示用户手动去商店搜索。
     */
    fun openFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        intents.forEach { intent ->
            val opened = runCatching { context.startActivity(intent) }.isSuccess
            if (opened) return true
        }
        return false
    }

    /**
     * 手环同步引导文案。**统一走系统健康平台，不教用户开蓝牙。**
     *
     * 22.22.0 起只给「这台机器上真实装到的 App」的路径（方案 §3.8）：以前无论装没装
     * 都写两家，用户照着找不到菜单，还以为是自己的问题。一个都没装时给的是
     * 「先装厂商 App」这三步，而不是路径。
     *
     * [family] 用来分开两个入口：`xiaomi` / `huawei` / `any`。
     */
    fun vendorGuide(context: Context, family: String = "any"): List<String> =
        vendorGuideFor(installedVendors(context), family)

    /**
     * 同上，但「这台机器上装了什么」由调用方传进来。
     *
     * 浮层里通常已经扫过一遍（要画「打开 X」按钮），这里再扫一次纯属浪费——
     * 那个扫描是在界面线程上做的。
     */
    fun vendorGuideFor(vendors: List<InstalledVendor>, family: String = "any"): List<String> {
        val installed = vendors.filter { family == "any" || it.profile.family == family }
        if (installed.isEmpty()) return listOf(
            BodyCastText.text("这台机器上没找到手环厂商的运动健康 App"),
            BodyCastText.text("先装上厂商的运动健康 App，登录手环账号，确认数据在它里面能看到"),
            BodyCastText.text("然后在它的设置里找「数据共享 / 第三方接入 / Health Connect」这类开关")
        )
        return buildList {
            installed.forEach { vendor ->
                add(vendor.profile.path)
                add(vendor.profile.note)
            }
            // 每个厂商末尾固定两条：打开 App ≠ 数据进来了；历史不补写。
            add(BodyCastText.text("厂商 App 打开不等于数据已经写进 Health Connect——回本页看自检第 ③ 步的最后写入时间才算数"))
            add(BodyCastText.text("授权前产生的历史通常不会补写：先在厂商 App 里同步一次，再回来点「手动同步」"))
        }
    }

    /**
     * 桥接引导（华为 / 荣耀用户的关键一步）。
     *
     * 华为运动健康把数据存在自己的健康服务里，不往 Health Connect 写——这是厂商
     * 限制，应用侧补不了。现实中能走通的是「第三方桥接 App」，所以把那条路如实
     * 讲清楚。**应用不集成任何第三方 SDK、不做任何代理**，只是把路径写出来；
     * 也不承诺桥接一定能读出全部类型。
     */
    fun bridgeGuide(): List<String> = listOf(
        BodyCastText.text("装一个第三方桥接 App：Health Sync 这类，官网说明支持把 HUAWEI Health 的数据转写到 Health Connect，中国大陆有 APK 下载说明"),
        BodyCastText.text("在桥接 App 里把数据源设成 HUAWEI Health（华为运动健康），目标设成 Health Connect"),
        BodyCastText.text("回到本页点「去授权」，重新授权一次——只有桥接写进来的数据才算数"),
        BodyCastText.text("之后是自动的：桥接在后台同步，本页第 ③ 步会开始出现最后写入时间"),
        BodyCastText.text("我们只给这条路径，不集成任何第三方 SDK，也不替它们的可用性做承诺"),
        BodyCastText.text("不想装桥接也有一条明路：在华为运动健康 App 里「我的 → 设置 → 隐私管理 → 数据下载/导出」拿一份数据文件（不同版本入口略有差异），发到本应用的「导入数据文件」，今天的数据就能进来"),
        BodyCastText.text("实在都不方便：手动填写读数永远可用——照着华为运动健康里的数字抄进来，24 小时内有效")
    )

    /**
     * 厂商 App 的写入能力探测（22.30.0）。
     *
     * 用户在自检第 ③ 步卡住时，最难回答的问题是「到底是没开开关，还是这 App 根本
     * 不写 Health Connect？」——两者修法完全不同。答案就写在厂商 APK 的
     * 清单里：**声明了 `android.permission.health.WRITE_*` 的才会写**。
     * 读法零成本（一次 `getPackageInfo`，不需要别的权限），也不碰厂商数据。
     *
     * 三种结果：
     * - true  —— App 声明了写入权限，数据进不来是「开关没开 / 还没同步」；
     * - false —— 清单里一项写入权限都没有，这条路在它身上走不通（华为 / 荣耀
     *   的国行版就是这种），引导用户去桥接或手动；
     * - null  —— 查不到（没装 / 被包可见性挡住），不要下结论。
     */
    fun declaresHealthConnectWrite(context: Context, packageName: String): Boolean? =
        runCatching {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            val perms = info.requestedPermissions ?: return null
            perms.any { it.startsWith("android.permission.health.WRITE_") }
        }.getOrNull()

    /**
     * 把记录来源的包名翻译成人能看懂的名字。
     *
     * 用处：用户同时装了小米和华为两个 App，或者数据其实是别的 App 写的，
     * 只看「已授权」是看不出来的——这一行直接告诉他数据从哪来。
     */
    fun originLabel(packageName: String): String = when {
        // 国行小米运动健康的包名是 com.mi.health，里面**没有** xiaomi，
        // 所以这一条必须放最前面（21.x 起「数据来源」直接显示原始包名就是漏了它）。
        packageName.contains("mi.health") -> BodyCastText.text("小米运动健康（国行）")
        packageName.contains("hm.health") -> BodyCastText.text("Zepp Life / 小米运动（老版）")
        packageName.contains("xiaomi") || packageName.contains("mifitness") ->
            BodyCastText.text("小米运动健康")
        packageName.contains("huami") || packageName.contains("zepp") ||
            packageName.contains("amazfit") -> BodyCastText.text("Zepp / Amazfit")
        packageName.contains("hihonor") -> BodyCastText.text("荣耀运动健康")
        packageName.contains("huawei") -> BodyCastText.text("华为运动健康")
        packageName.contains("heytap") || packageName.contains("oppo") ->
            BodyCastText.text("OPPO 健康")
        packageName.contains("samsung") || packageName.contains("shealth") ->
            BodyCastText.text("三星健康")
        packageName.contains("google") || packageName.contains("healthdata") ->
            BodyCastText.text("系统健康服务")
        else -> packageName
    }
}
