package cn.pocket.toolbox.bodycast

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cn.pocket.toolbox.data.micloud.MiSession
import cn.pocket.toolbox.data.micloud.MiSnapshot
import cn.pocket.toolbox.data.micloud.MiTokenVault
import cn.pocket.toolbox.glass.GlassSupport
import cn.pocket.toolbox.glass.GlassTheme
import cn.pocket.toolbox.i18n.AppLanguage
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 设置页里的全部偏好。默认值都偏向「安静、少打扰」。 */
data class BodyCastPrefs(
    val nickname: String = "",
    /** 连接状态：是否已同意隐私说明 */
    val privacyAgreed: Boolean = false,
    /** 首次握手流程是否走完 */
    val onboardingDone: Boolean = false,

    // 页面 2：微行动处方
    val voiceGuide: Boolean = true,
    val backgroundSound: Boolean = true,
    val vibrate: Boolean = true,

    // 页面 4：提醒
    val quietMode: Boolean = false,
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 7,
    /** 免打扰时段内是否仍然允许手动校准 */
    val allowCalibrationInQuiet: Boolean = true,

    // 外观
    val darkMode: Boolean = true,
    /** 0 浅色 / 1 深色 / 2 跟随系统。[darkMode] 是它的派生结果。 */
    val themeMode: Int = 1,
    val animationEnabled: Boolean = true,
    /** 0 小 / 1 标准 / 2 大 */
    val fontScale: Int = 1,
    /** 用户显式选定的语言；auto = 跟随系统（中文→简中，其他→英语）。 */
    val language: String = "auto",

    // 外观实验（设置 → 实验性功能）。都是给开发者与好奇者的档位，
    // 默认值 = 现有观感，不动它就跟以前完全一样。
    /** 强调色：0 绿（默认）/ 1 蓝 / 2 紫 / 3 橙 */
    val accentStyle: Int = 0,
    /** 圆角风格：0 标准（32dp）/ 1 更圆（40dp）/ 2 更方（24dp） */
    val cornerStyle: Int = 0,
    /** 玻璃强度：0 标准 / 1 更透 / 2 更磨砂 */
    val glassStrength: Int = 0,
    /** 强制磨砂降级：忽略折射着色器，一律走低版本磨砂外观 */
    val forceFrost: Boolean = false,
    /** 开发者模式是否已解锁（关于页红块确认后置位，重启保留） */
    val developerUnlocked: Boolean = false,
    /**
     * 数据连接引导「不再提示」（22.48.0）。
     *
     * 没连上数据时每次打开都会问一次「要不要连运动数据」——问多了就是骚扰。
     * 用户在弹窗里勾了「不再提示」就永久闭嘴；设置里的「授权向导」照旧随时能进，
     * 这条偏好只关掉**启动时的提示**，不关掉功能。
     */
    val dataGuideNever: Boolean = false,

    // 数据与隐私
    val autoFetch: Boolean = true,
    val lastFetchAt: Long = 0L,
    /** 身体来信提醒（每周日晚） */
    val calendarReminder: Boolean = false,
    val exportFormat: String = "JSON",

    // 数据源管理
    val dataSourcePriority: String = "health_connect",
    /** 模拟数据模式。比赛演示用，开了之后界面会明确标注「演示数据」。 */
    val demoMode: Boolean = false,

    // 手动填写读数 / 文件导入（22.24.0，方案 B §3.1–§3.3）。
    // 与 HC 的读数分开存：**只存聚合值 + 一个时间戳**，不存明细，也绝不覆盖 HC 的数据。
    val manualEntryAt: Long = 0L,
    val manualRestingHeartRate: Int? = null,
    val manualHrv: Int? = null,
    val manualSleepMinutes: Int? = null,
    val manualDeepSleepRatio: Float? = null,
    val manualSteps: Int? = null,
    val manualBloodOxygen: Int? = null,
    val manualActivityMinutes: Int? = null,
    val importedAt: Long = 0L,
    val importedRestingHeartRate: Int? = null,
    val importedHrv: Int? = null,
    val importedSleepMinutes: Int? = null,
    val importedDeepSleepRatio: Float? = null,
    val importedSteps: Int? = null,
    /** 压力值只有文件导入会给（Health Connect 没有这一项）。 */
    val importedStressLevel: Int? = null,
    val importedBloodOxygen: Int? = null,
    val importedActivityMinutes: Int? = null,

    // 小米账号（云同步，22.24.0 起的**可选**通道）。默认关闭：没连过账号时一个字节都不外发。
    // 令牌是 Android Keystore 加密之后的密文（Base64），明文令牌不落盘；
    // 其余只有六类聚合值 + 上次同步时间，与手动填写 / 导入同一套纪律。
    val miCloudToken: String = "",
    /** 设备标识（非机密，用来让小米认出「还是同一台手机」）。 */
    val miCloudDeviceId: String = "",
    /** 账号线索（健康云昵称，或 userId 掩码）——只在本机显示。 */
    val miCloudAccount: String = "",
    val miCloudRegion: String = "",
    val miCloudSyncedAt: Long = 0L,
    val miCloudRestingHeartRate: Int? = null,
    val miCloudHrv: Int? = null,
    val miCloudSleepMinutes: Int? = null,
    val miCloudDeepSleepRatio: Float? = null,
    val miCloudSteps: Int? = null,
    val miCloudStressLevel: Int? = null,
    val miCloudBloodOxygen: Int? = null,
    val miCloudActivityMinutes: Int? = null,
    /** 上一次同步六类里读到了哪几类（界面按这个如实显示）。 */
    val miCloudReadKeys: List<String> = emptyList(),
    /**
     * 昨夜睡眠 / 今日运动的**真实起止时间**（epoch 毫秒；0 = 没拿到窗口）。
     * 写入 Health Connect 的睡眠 / 运动段只在有窗口时才写，不编造时间。
     */
    val miCloudSleepStartAt: Long = 0L,
    val miCloudSleepEndAt: Long = 0L,
    val miCloudExerciseStartAt: Long = 0L,
    val miCloudExerciseEndAt: Long = 0L,

    // 把读数写入系统健康服务（Health Connect 写入桥，22.42.0）。
    // 只在用户打开开关后写；写的是聚合读数，来源标注为手动录入。
    val bridgeToHc: Boolean = false,
    /** 上次写入成功的时间与条数（状态行用）。 */
    val hcWriteAt: Long = 0L,
    val hcWriteCount: Int = 0,
    /** 上次写入内容的签名：值没变自动写就跳过（「立即写入」不受限）。 */
    val hcWriteSig: String = "",

    // 身体天气校准
    val defaultWeather: String = "auto",
    val customCalibrationLabels: List<String> = emptyList(),
    /** 被用户在设置里隐藏掉的默认校准标签 */
    val hiddenCalibration: Set<CalibrationTag> = emptySet(),
    /** 0 低 / 1 中 / 2 高 */
    val sensitivity: Int = 1,

    // 处方偏好
    val maxMinutes: Int = 5,
    val preferQuiet: Boolean = false,
    val avoid: Set<ActionCategory> = emptySet(),
    /** 可用场景稳定 ID：desk / transit / bed / balcony / outdoors */
    val preferredScenes: Set<String> = setOf("desk", "bed", "balcony"),
    /** 完成反馈后自动调整下次处方 */
    val autoAdjustAfterFeedback: Boolean = true,
    val defaultSound: String = "silent",

    // 食谱指南
    val dietPreferences: Set<String> = emptySet(),
    val allergenAlert: Boolean = true,
    val recipeScope: String = "all",

    // 通知
    val handshakeReminder: Boolean = false,
    val handshakeHour: Int = 9,
    val handshakeMinute: Int = 0,
    val actionReminder: Boolean = false,
    val actionHour: Int = 20,
    val actionMinute: Int = 0
) {
    /** 手动填写的读数。没有填过的项是 null。 */
    val manualEntries: EntryValues
        get() = EntryValues(
            restingHeartRate = manualRestingHeartRate,
            hrv = manualHrv,
            sleepMinutes = manualSleepMinutes,
            deepSleepRatio = manualDeepSleepRatio,
            steps = manualSteps,
            bloodOxygen = manualBloodOxygen,
            activityMinutes = manualActivityMinutes
        )

    /** 从文件导入的读数。 */
    val importedEntries: EntryValues
        get() = EntryValues(
            restingHeartRate = importedRestingHeartRate,
            hrv = importedHrv,
            sleepMinutes = importedSleepMinutes,
            deepSleepRatio = importedDeepSleepRatio,
            steps = importedSteps,
            stressLevel = importedStressLevel,
            bloodOxygen = importedBloodOxygen,
            activityMinutes = importedActivityMinutes
        )

    /** 手动填写的读数还在有效期内吗（24 小时）。 */
    val manualEntryFresh: Boolean get() = EntryValidity.isFresh(manualEntryAt)

    /** 导入的读数还在有效期内吗（24 小时）。 */
    val importedFresh: Boolean get() = EntryValidity.isFresh(importedAt)

    /** 小米云快照里的读数（六类）。 */
    val miCloudEntries: EntryValues
        get() = EntryValues(
            restingHeartRate = miCloudRestingHeartRate,
            hrv = miCloudHrv,
            sleepMinutes = miCloudSleepMinutes,
            deepSleepRatio = miCloudDeepSleepRatio,
            steps = miCloudSteps,
            stressLevel = miCloudStressLevel,
            bloodOxygen = miCloudBloodOxygen,
            activityMinutes = miCloudActivityMinutes
        )

    /** 已连接小米账号（本机有加密令牌）。 */
    val miCloudConnected: Boolean get() = miCloudToken.isNotBlank()

    /**
     * 小米云的快照还在有效期内吗（24 小时）。
     *
     * 云端数据是「设备同步到云」的**快照**，不是实时读数：过了 24 小时就不再参与
     * 今天的判定，界面如实显示「已过期」与上次同步时间。
     */
    val miCloudFresh: Boolean get() = EntryValidity.isFresh(miCloudSyncedAt)

    fun encode(): String = buildString {
        append("name=").append(nickname)
        append(";pa=").append(if (privacyAgreed) 1 else 0)
        append(";ob=").append(if (onboardingDone) 1 else 0)
        append(";vg=").append(if (voiceGuide) 1 else 0)
        append(";bs=").append(if (backgroundSound) 1 else 0)
        append(";vb=").append(if (vibrate) 1 else 0)
        append(";qm=").append(if (quietMode) 1 else 0)
        append(";qs=").append(quietStartHour)
        append(";qe=").append(quietEndHour)
        append(";qc=").append(if (allowCalibrationInQuiet) 1 else 0)
        append(";dark=").append(if (darkMode) 1 else 0)
        append(";tm=").append(themeMode)
        append(";anim=").append(if (animationEnabled) 1 else 0)
        append(";fs=").append(fontScale)
        append(";lang=").append(language)
        append(";af=").append(if (autoFetch) 1 else 0)
        append(";lf=").append(lastFetchAt)
        append(";cr=").append(if (calendarReminder) 1 else 0)
        append(";exp=").append(exportFormat)
        append(";dsp=").append(dataSourcePriority)
        append(";dm=").append(if (demoMode) 1 else 0)
        append(";dw=").append(defaultWeather)
        append(";ccl=").append(customCalibrationLabels.joinToString("|"))
        append(";hc=").append(hiddenCalibration.joinToString("|") { it.name })
        append(";sens=").append(sensitivity)
        append(";mm=").append(maxMinutes)
        append(";pq=").append(if (preferQuiet) 1 else 0)
        append(";av=").append(avoid.joinToString("|") { it.name })
        append(";ps=").append(preferredScenes.joinToString("|"))
        append(";aaf=").append(if (autoAdjustAfterFeedback) 1 else 0)
        append(";dsnd=").append(defaultSound)
        append(";diet=").append(dietPreferences.joinToString("|"))
        append(";alg=").append(if (allergenAlert) 1 else 0)
        append(";rsc=").append(recipeScope)
        append(";hr=").append(if (handshakeReminder) 1 else 0)
        append(";hh=").append(handshakeHour)
        append(";hm=").append(handshakeMinute)
        append(";ar=").append(if (actionReminder) 1 else 0)
        append(";ah=").append(actionHour)
        append(";am=").append(actionMinute)
        // 外观实验偏好是后加的：旧串里没有这几个键，解码时按默认值兜底。
        append(";acs=").append(accentStyle)
        append(";cor=").append(cornerStyle)
        append(";gst=").append(glassStrength)
        append(";ffr=").append(if (forceFrost) 1 else 0)
        append(";dvu=").append(if (developerUnlocked) 1 else 0)
        append(";dgn=").append(if (dataGuideNever) 1 else 0)
        // 手动填写读数 / 文件导入（22.24.0）：只写聚合值，没填的项写空串。
        append(";mea=").append(manualEntryAt)
        append(";mhr=").append(manualRestingHeartRate ?: "")
        append(";mhv=").append(manualHrv ?: "")
        append(";msl=").append(manualSleepMinutes ?: "")
        append(";mds=").append(manualDeepSleepRatio ?: "")
        append(";mst=").append(manualSteps ?: "")
        append(";mbo=").append(manualBloodOxygen ?: "")
        append(";mam=").append(manualActivityMinutes ?: "")
        append(";ima=").append(importedAt)
        append(";ihr=").append(importedRestingHeartRate ?: "")
        append(";ihv=").append(importedHrv ?: "")
        append(";isl=").append(importedSleepMinutes ?: "")
        append(";ids=").append(importedDeepSleepRatio ?: "")
        append(";ist=").append(importedSteps ?: "")
        append(";isp=").append(importedStressLevel ?: "")
        append(";ibo=").append(importedBloodOxygen ?: "")
        append(";iam=").append(importedActivityMinutes ?: "")
        // 小米账号（云同步）：令牌是密文（Base64），其余是聚合值 + 时间戳。
        append(";mct=").append(miCloudToken)
        append(";mcdid=").append(miCloudDeviceId)
        append(";mca=").append(miCloudAccount)
        append(";mcr=").append(miCloudRegion)
        append(";mcsa=").append(miCloudSyncedAt)
        append(";mchr=").append(miCloudRestingHeartRate ?: "")
        append(";mchv=").append(miCloudHrv ?: "")
        append(";mcsl=").append(miCloudSleepMinutes ?: "")
        append(";mcds=").append(miCloudDeepSleepRatio ?: "")
        append(";mcst=").append(miCloudSteps ?: "")
        append(";mcps=").append(miCloudStressLevel ?: "")
        append(";mcbo=").append(miCloudBloodOxygen ?: "")
        append(";mcam=").append(miCloudActivityMinutes ?: "")
        append(";mck=").append(miCloudReadKeys.joinToString("|"))
        // 睡眠 / 运动的真实起止窗口（22.42.0）：HC 写入桥用，不编造时间。
        append(";mcss=").append(miCloudSleepStartAt)
        append(";mcse=").append(miCloudSleepEndAt)
        append(";mces=").append(miCloudExerciseStartAt)
        append(";mcee=").append(miCloudExerciseEndAt)
        // HC 写入桥（22.42.0）：开关 + 上次写入状态 + 自动写去重签名。
        append(";bhc=").append(if (bridgeToHc) 1 else 0)
        append(";hcwa=").append(hcWriteAt)
        append(";hcwc=").append(hcWriteCount)
        append(";hcws=").append(hcWriteSig)
    }

    companion object {
        fun decode(raw: String?): BodyCastPrefs {
            if (raw.isNullOrBlank()) return BodyCastPrefs()
            val map = raw.split(';').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
            }.toMap()
            fun i(k: String, d: Int) = map[k]?.toIntOrNull() ?: d
            fun l(k: String, d: Long) = map[k]?.toLongOrNull() ?: d
            fun b(k: String, d: Boolean) = map[k]?.toIntOrNull()?.let { it == 1 } ?: d
            fun s(k: String, d: String) = map[k]?.takeIf { it.isNotEmpty() } ?: d
            // 手动填写 / 导入的读数：空串 = 这一项没填（不是 0）。
            fun iNull(k: String) = map[k]?.toIntOrNull()
            fun fNull(k: String) = map[k]?.toFloatOrNull()
            fun list(k: String) = map[k]?.split('|')?.filter { it.isNotEmpty() } ?: emptyList()
            fun set(k: String) = list(k).toSet()
            fun stable(value: String, aliases: Map<String, String>, fallback: String): String =
                aliases[value] ?: value.takeIf { it in aliases.values } ?: fallback
            val sceneAliases = mapOf(
                "工位" to "desk", "地铁" to "transit", "床上" to "bed",
                "阳台" to "balcony", "户外" to "outdoors"
            )
            val dietAliases = mapOf(
                "素食" to "vegetarian", "不吃乳制品" to "dairy_free",
                "不吃海鲜" to "seafood_free", "低糖" to "low_sugar"
            )
            return BodyCastPrefs(
                nickname = map["name"] ?: "",
                privacyAgreed = b("pa", false),
                onboardingDone = b("ob", false),
                voiceGuide = b("vg", true),
                backgroundSound = b("bs", true),
                vibrate = b("vb", true),
                quietMode = b("qm", false),
                quietStartHour = i("qs", 23),
                quietEndHour = i("qe", 7),
                allowCalibrationInQuiet = b("qc", true),
                darkMode = b("dark", true),
                themeMode = i("tm", 1),
                animationEnabled = b("anim", true),
                fontScale = i("fs", 1),
                language = AppLanguage.fromStored(map["lang"]).tag,
                autoFetch = b("af", true),
                lastFetchAt = l("lf", 0L),
                calendarReminder = b("cr", false),
                exportFormat = s("exp", "JSON"),
                dataSourcePriority = stable(
                    s("dsp", "health_connect"),
                    mapOf(
                        "Health Connect" to "health_connect",
                        "手动填写读数" to "manual_entry",
                        "导入数据" to "import",
                        "手动校准" to "manual",
                        "演示数据" to "demo"
                    ),
                    "health_connect"
                ),
                demoMode = b("dm", false),
                defaultWeather = stable(
                    s("dw", "auto"),
                    mapOf("跟随读数" to "auto") + WeatherKind.entries.associate { it.label to it.name },
                    "auto"
                ),
                customCalibrationLabels = list("ccl"),
                hiddenCalibration = set("hc")
                    .mapNotNull { name -> CalibrationTag.entries.firstOrNull { it.name == name } }
                    .toSet(),
                sensitivity = i("sens", 1),
                maxMinutes = i("mm", 5),
                preferQuiet = b("pq", false),
                avoid = set("av")
                    .mapNotNull { name -> ActionCategory.entries.firstOrNull { it.name == name } }
                    .toSet(),
                preferredScenes = set("ps")
                    .mapNotNull { sceneAliases[it] ?: it.takeIf { value -> value in sceneAliases.values } }
                    .toSet()
                    .ifEmpty { setOf("desk", "bed", "balcony") },
                autoAdjustAfterFeedback = b("aaf", true),
                defaultSound = stable(
                    s("dsnd", "silent"),
                    mapOf("雨声" to "rain", "风声" to "wind", "咖啡馆" to "cafe", "纯静音" to "silent"),
                    "silent"
                ),
                dietPreferences = set("diet")
                    .mapNotNull { dietAliases[it] ?: it.takeIf { value -> value in dietAliases.values } }
                    .toSet(),
                allergenAlert = b("alg", true),
                recipeScope = stable(
                    s("rsc", "all"),
                    mapOf("全部食物" to "all", "只按营养素" to "nutrient", "只按功效" to "benefit"),
                    "all"
                ),
                handshakeReminder = b("hr", false),
                handshakeHour = i("hh", 9),
                handshakeMinute = i("hm", 0),
                actionReminder = b("ar", false),
                actionHour = i("ah", 20),
                actionMinute = i("am", 0),
                accentStyle = i("acs", 0).coerceIn(0, 3),
                cornerStyle = i("cor", 0).coerceIn(0, 2),
                glassStrength = i("gst", 0).coerceIn(0, 2),
                forceFrost = b("ffr", false),
                // 旧串里的 ;sk=（皮肤）键直接忽略：22.31.0 起只有液态玻璃一套界面。
                developerUnlocked = b("dvu", false),
                dataGuideNever = b("dgn", false),
                // 手动填写读数 / 文件导入（22.24.0）：旧串里没有这些键，一律按「没填」。
                manualEntryAt = l("mea", 0L),
                manualRestingHeartRate = iNull("mhr"),
                manualHrv = iNull("mhv"),
                manualSleepMinutes = iNull("msl"),
                manualDeepSleepRatio = fNull("mds"),
                manualSteps = iNull("mst"),
                manualBloodOxygen = iNull("mbo"),
                manualActivityMinutes = iNull("mam"),
                importedAt = l("ima", 0L),
                importedRestingHeartRate = iNull("ihr"),
                importedHrv = iNull("ihv"),
                importedSleepMinutes = iNull("isl"),
                importedDeepSleepRatio = fNull("ids"),
                importedSteps = iNull("ist"),
                importedStressLevel = iNull("isp"),
                importedBloodOxygen = iNull("ibo"),
                importedActivityMinutes = iNull("iam"),
                // 小米账号（云同步）：旧串里没有这些键 → 没连过账号，一切照旧。
                miCloudToken = s("mct", ""),
                miCloudDeviceId = s("mcdid", ""),
                miCloudAccount = s("mca", ""),
                miCloudRegion = s("mcr", ""),
                miCloudSyncedAt = l("mcsa", 0L),
                miCloudRestingHeartRate = iNull("mchr"),
                miCloudHrv = iNull("mchv"),
                miCloudSleepMinutes = iNull("mcsl"),
                miCloudDeepSleepRatio = fNull("mcds"),
                miCloudSteps = iNull("mcst"),
                miCloudStressLevel = iNull("mcps"),
                miCloudBloodOxygen = iNull("mcbo"),
                miCloudActivityMinutes = iNull("mcam"),
                miCloudReadKeys = list("mck"),
                // 睡眠 / 运动真实窗口 + HC 写入桥：旧串里没有这些键，按默认值兜底。
                miCloudSleepStartAt = l("mcss", 0L),
                miCloudSleepEndAt = l("mcse", 0L),
                miCloudExerciseStartAt = l("mces", 0L),
                miCloudExerciseEndAt = l("mcee", 0L),
                bridgeToHc = b("bhc", false),
                hcWriteAt = l("hcwa", 0L),
                hcWriteCount = i("hcwc", 0),
                hcWriteSig = s("hcws", "")
            )
        }
    }
}

/**
 * 身体气象台的数据仓库。
 *
 * 只存三类东西：偏好、天气记录、动作库收藏。
 * **不存任何原始生理数值**——读数进来是为了算天气，算完就丢掉，
 * 这样即使有人拿到本机的存储文件，也看不到历史心率或睡眠时长。
 */
class BodyCastStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefsStore = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** HC 写入桥的后台协程（22.42.0）：只在开关打开后写，写失败不影响保存读数本身。 */
    private val ioScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    val engine: BodyCastEngine = LocalRuleEngine()

    var prefs by mutableStateOf(BodyCastPrefs.decode(prefsStore.getString(KEY_PREFS, null)))
        private set

    /**
     * 冷启动时把外观实验偏好推给 [GlassTheme] / [GlassSupport]。
     *
     * 那两处是静态快照（和 setDark / setFontScale 同一套机制），
     * 只在偏好真正变化时推进去；这里不引入第二份可写状态。
     */
    init {
        syncAppearance()
    }

    /**
     * 设备那一路读到的原始读数（Health Connect 等）。
     *
     * 外部只读 [signals]；这里单独留一份是为了「手动填读数 / 导入」能叠在它上面，
     * 而不是覆盖它——两边缺的项互相补，谁也不清空谁。
     */
    private var deviceSignals by mutableStateOf(BodySignals())

    /**
     * 这一轮生效的读数。
     *
     * 合并顺序是产品铁律的延伸：**手动填读数 > 导入 > 设备（HC）**，且**只按字段补**——
     * 手动只填了步数，心率仍然是设备读到的；两边都没有才是 null，界面照旧写
     * 「暂时没读到」。手动校准（改结论）比这些都高一层，不在这里参与。
     * 超过 24 小时的手动填写 / 导入不参与合并（[EntryValidity]）。
     */
    val signals: BodySignals
        get() = mergeSignals(deviceSignals)

    /** 今天的手动校准标签。 */
    var calibrated by mutableStateOf<Set<CalibrationTag>>(emptySet())
        private set

    /** 历史天气记录，键是 `yyyy-MM-dd`。 */
    var records by mutableStateOf(loadRecords())
        private set

    /** 当前这张处方。 */
    var prescription by mutableStateOf<Prescription?>(null)
        private set

    /** 本轮已完成的动作 id。 */
    var doneActions by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 完成后的反馈选择。 */
    var feedback by mutableStateOf<String?>(null)
        private set

    /**
     * **长期**反馈，会持久化。
     *
     * 和 [feedback] 的区别：[feedback] 是「这一轮选了什么」，开下一张处方就清空；
     * [lastFeedback] 是「用户上次说过什么」，一直留着，直到他改口。
     * 「说更累了下次就更温和」这句承诺靠它兑现——不然换一张处方就忘了。
     */
    var lastFeedback by mutableStateOf(migrateFeedback(prefsStore.getString(KEY_LAST_FEEDBACK, null)))
        private set

    /** 已经点开过多少次「今天，身体说了什么」——设置页的「倾听次数」。 */
    var listenCount by mutableStateOf(prefsStore.getInt(KEY_LISTEN, 0))
        private set

    /**
     * 「照顾自己」记录：日期 → 当天完成的微行动数（22.30.0）。
     *
     * 只做**正向计数**——回声页用它显示「本周你照顾自己 N 次」，
     * 没有目标值、没有连击、没有完成率（产品铁律不变）。
     * 每做完一件微行动 +1；不做减法，也不因为漏做而扣。
     */
    var careLog by mutableStateOf(loadCareLog())
        private set

    /**
     * 「今天真的吃了什么」：日期 → 当天记下的食物名（22.32.0 食谱闭环）。
     *
     * 在食谱页按「就吃这个」记一笔——回声页按周汇总成「这周你吃了什么」。
     * 只记用户自己按下的，不猜、不推、不评价吃得好不好。
     */
    var mealLog by mutableStateOf(loadMealLog())
        private set

    /**
     * 「这周做过的小事」：日期 → 当天做完的微行动 id（22.49.0）。
     *
     * 和 [mealLog] 同一套纪律：只记用户自己点完的，不猜、不补、不评价。
     * 在此之前回声页只有「照顾自己 N 次」这一个数字——数字涨了却想不起
     * 做过什么。这一份让回声页能如实列出「哪天做了哪几件」，
     * 也让「微行动」这件事真正出现在回声页里（用户提的）。
     */
    var actionLog by mutableStateOf(loadActionLog())
        private set

    /**
     * 「想吃的」清单：以后想吃 / 想买的食物名（22.32.0）。
     *
     * 在食物详情里点「收进想吃的」收进来，做饭前打开看一眼就是购物清单。
     * 有序插入（文件里按加入顺序存），可以逐条移除。
     */
    var wishList by mutableStateOf(loadWishList())
        private set

    /** 动作库收藏。 */
    var favorites by mutableStateOf(loadFavorites())
        private set

    val today: LocalDate get() = LocalDate.now()

    val todayKey: String get() = today.toString()

    /** 当天的稳定种子，保证同一天里天气诗和处方不会变来变去。 */
    val daySeed: Long get() = today.toEpochDay() * 1000003L

    /**
     * 当前天气。每次都现算——纯函数，开销可以忽略，
     * 但能保证「改了校准，天气立刻跟着变」，不会出现状态不同步。
     *
     * 自定义标签（设置里自己加的）不参与定档，只要选中任意一个，
     * 就等价于勾了「说不清」——因为身体已经明确在说话了。
     */
    val weather: BodyWeather
        get() = engine.weather(
            signals,
            if (customCalibrated.isEmpty()) {
                calibrated
            } else {
                calibrated + CalibrationTag.UNSPEAKABLE
            },
            daySeed,
            defaultWeatherKind()
        )

    /**
     * 设置页里「默认天气类型」对应的档位。
     *
     * 只有**四路信号都算不出来且没有任何校准**时才生效——也就是引擎确实
     * 无从判断的时候（22.27.0：按信号数判，不看零散读数的条数；有一路信号
     * 就交回引擎按真实数据定档，缺的那几路如实写「暂时没读到」）。
     * 这条边界是产品底线：数据缺失标记为未知，不允许在规则引擎里用默认值替代。
     *
     * 用户选「跟随读数」时存稳定值 `auto`，具体天气存 [WeatherKind.name]。
     */
    private fun defaultWeatherKind(): WeatherKind? {
        if (engine.signalCount(signals) > 0) return null
        if (hasAnyCalibration) return null
        return WeatherKind.fromId(prefs.defaultWeather)
    }

    /** 自定义校准标签的选中态。 */
    var customCalibrated by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 页面上要显示的默认校准标签（设置里隐藏掉的不显示）。 */
    val visibleCalibrationTags: List<CalibrationTag>
        get() = CalibrationTag.DISPLAY_ORDER.filter { it !in prefs.hiddenCalibration }

    /** 页面上要显示的额外自定义标签。 */
    val visibleCustomLabels: List<String>
        get() = prefs.customCalibrationLabels

    /** 切换一个自定义标签。 */
    fun toggleCustomCalibration(label: String) {
        customCalibrated = if (customCalibrated.contains(label)) {
            customCalibrated - label
        } else {
            customCalibrated + label
        }
        recordToday()
    }

    /** 是否有任何形式的校准（默认标签或自定义标签）。 */
    val hasAnyCalibration: Boolean
        get() = calibrated.isNotEmpty() || customCalibrated.isNotEmpty()

    fun clearAllCalibration() {
        calibrated = emptySet()
        customCalibrated = emptySet()
        recordToday()
    }

    // ---------------------------------------------------------------------
    // 校准
    // ---------------------------------------------------------------------

    /** 切换一个校准标签，并立刻把今天的天气记录更新掉。 */
    fun toggleCalibration(tag: CalibrationTag) {
        val next = calibrated.toMutableSet()
        if (!next.add(tag)) next.remove(tag)
        calibrated = next
        recordToday()
    }

    fun clearCalibration() {
        if (calibrated.isEmpty()) return
        calibrated = emptySet()
        recordToday()
    }

    /** 记录「今天，身体说了什么」被点开。 */
    fun markListened() {
        listenCount += 1
        prefsStore.edit().putInt(KEY_LISTEN, listenCount).apply()
        recordToday()
    }

    // ---------------------------------------------------------------------
    // 信号
    // ---------------------------------------------------------------------

    fun updateSignals(next: BodySignals) {
        deviceSignals = next
        prefs = prefs.copy(lastFetchAt = System.currentTimeMillis())
        persistPrefs()
        recordToday()
    }

    /** 用演示数据填充，用于没有设备或没授权时也能看到完整效果。 */
    fun useDemoData() = updateSignals(BodySignals.demo())

    fun clearSignals() {
        deviceSignals = BodySignals()
        recordToday()
    }

    // ---------------------------------------------------------------------
    // 手动填写读数 / 文件导入（22.24.0）
    //
    // 两条都不申请任何权限、都不依赖厂商 SDK。存进本机的只有聚合值 + 一个时间戳，
    // 24 小时之后自动失效；HC 那一路的读数既不覆盖也不清除。
    // ---------------------------------------------------------------------

    /** 保存一次手动填写的读数（null = 这一项没填）。 */
    fun saveManualEntry(values: EntryValues) {
        if (values.isEmpty) return
        prefs = prefs.copy(
            manualEntryAt = System.currentTimeMillis(),
            manualRestingHeartRate = values.restingHeartRate,
            manualHrv = values.hrv,
            manualSleepMinutes = values.sleepMinutes,
            manualDeepSleepRatio = values.deepSleepRatio,
            manualSteps = values.steps,
            manualBloodOxygen = values.bloodOxygen,
            manualActivityMinutes = values.activityMinutes
        )
        persistPrefs()
        recordToday()
        maybeBridgeToHc()
    }

    /** 清掉手动填的读数。清的是这一路，设备读数不受影响。 */
    fun clearManualEntry() {
        prefs = prefs.copy(
            manualEntryAt = 0L,
            manualRestingHeartRate = null,
            manualHrv = null,
            manualSleepMinutes = null,
            manualDeepSleepRatio = null,
            manualSteps = null,
            manualBloodOxygen = null,
            manualActivityMinutes = null
        )
        persistPrefs()
        recordToday()
    }

    /** 记下一次文件导入的读数（用户确认「写入」之后才调到这里）。 */
    fun saveImportedEntry(values: EntryValues) {
        if (values.isEmpty) return
        prefs = prefs.copy(
            importedAt = System.currentTimeMillis(),
            importedRestingHeartRate = values.restingHeartRate,
            importedHrv = values.hrv,
            importedSleepMinutes = values.sleepMinutes,
            importedDeepSleepRatio = values.deepSleepRatio,
            importedSteps = values.steps,
            importedStressLevel = values.stressLevel,
            importedBloodOxygen = values.bloodOxygen,
            importedActivityMinutes = values.activityMinutes
        )
        persistPrefs()
        recordToday()
        maybeBridgeToHc()
    }

    // ---------------------------------------------------------------------
    // 小米账号（云同步，22.24.0 起）
    //
    // 这条路是**可选**的：没连过账号时一个字节都不外发。连上之后：
    // - 令牌用 Android Keystore 加密后存进偏好（明文不落盘），随时可以断开删除；
    // - 存下来的只有六类聚合值 + 上次同步时间，与手动填写 / 导入同一套纪律；
    // - 「断开并删除令牌」把密钥和密文一起清掉，同时清掉云端那一份读数。
    // ---------------------------------------------------------------------

    /** 保存一次登录结果：加密令牌 + 账号线索 + 区域。*/
    fun saveMiCloudSession(sealedToken: String, session: MiSession) {
        if (sealedToken.isBlank()) return
        prefs = prefs.copy(
            miCloudToken = sealedToken,
            miCloudDeviceId = session.deviceId,
            miCloudAccount = session.accountHint,
            miCloudRegion = session.region
        )
        persistPrefs()
    }

    /** 保存一次同步读到的六类读数（读不到的项是 null，不填 0）。 */
    fun saveMiCloudSnapshot(snapshot: MiSnapshot, syncedAt: Long = System.currentTimeMillis()) {
        prefs = prefs.copy(
            miCloudSyncedAt = syncedAt,
            miCloudRestingHeartRate = snapshot.restingHeartRate,
            miCloudHrv = snapshot.hrv,
            miCloudSleepMinutes = snapshot.sleepMinutes,
            miCloudDeepSleepRatio = snapshot.deepSleepRatio,
            miCloudSteps = snapshot.steps,
            miCloudStressLevel = snapshot.stressLevel,
            miCloudBloodOxygen = snapshot.bloodOxygen,
            miCloudActivityMinutes = snapshot.activityMinutes,
            miCloudReadKeys = snapshot.readKeys.toList(),
            // 真实起止窗口（22.42.0）：HC 写入桥只在有窗口时写睡眠 / 运动段。
            miCloudSleepStartAt = snapshot.sleepStartAt,
            miCloudSleepEndAt = snapshot.sleepEndAt,
            miCloudExerciseStartAt = snapshot.exerciseStartAt,
            miCloudExerciseEndAt = snapshot.exerciseEndAt
        )
        persistPrefs()
        recordToday()
        maybeBridgeToHc()
    }

    /**
     * 记一次 HC 写入桥的结果（22.42.0）。
     *
     * [sig] 是本次写入内容的签名：自动写靠它去重（值没变不重复写），
     * 「立即写入」按钮不受这个限制。
     */
    fun recordHcWrite(count: Int, sig: String) {
        prefs = prefs.copy(
            hcWriteAt = System.currentTimeMillis(),
            hcWriteCount = count,
            hcWriteSig = sig
        )
        persistPrefs()
    }

    /**
     * HC 写入桥的自动写钩子（22.42.0）：手动填写 / 导入 / 小米云同步保存之后调。
     *
     * 只在开关（[BodyCastPrefs.bridgeToHc]）打开后写；内容签名没变就跳过，
     * 不给健康服务刷重复记录。写失败静默——保存读数本身已经成功了。
     */
    fun maybeBridgeToHc() {
        if (!prefs.bridgeToHc) return
        ioScope.launch {
            runCatching {
                cn.pocket.toolbox.data.HealthConnectWriter.writeFromStore(appContext, this@BodyCastStore)
            }
        }
    }

    /**
     * 断开并删除令牌。
     *
     * 三件事一起做：删 Keystore 里的密钥（密文从此解不开）、清掉密文与账号线索、
     * 清掉这一路存下来的读数——断开之后界面上不能还留着「小米账号」来的数字。
     */
    fun disconnectMiCloud() {
        MiTokenVault.deleteKey()
        prefs = prefs.copy(
            miCloudToken = "",
            miCloudAccount = "",
            miCloudRegion = "",
            miCloudSyncedAt = 0L,
            miCloudRestingHeartRate = null,
            miCloudHrv = null,
            miCloudSleepMinutes = null,
            miCloudDeepSleepRatio = null,
            miCloudSteps = null,
            miCloudStressLevel = null,
            miCloudBloodOxygen = null,
            miCloudActivityMinutes = null,
            miCloudReadKeys = emptyList()
        )
        persistPrefs()
    }

    /**
     * 合并四路读数。
     *
     * 真正的合并逻辑在 [SignalMerge]（纯函数，单测锁住优先级）：
     * **手动填读数 > 小米云 > 文件导入 > 设备（HC）**，逐字段补；
     * 演示模式整份返回演示读数，不往上叠真实数据。
     * 这里只负责把「还在 24 小时有效期内」的那几路挑出来交给它。
     */
    private fun mergeSignals(base: BodySignals): BodySignals {
        if (base.source == SignalSource.DEMO) return base
        val manual = prefs.manualEntries.takeIf { prefs.manualEntryFresh && !it.isEmpty }
        val cloud = prefs.miCloudEntries.takeIf { prefs.miCloudFresh && !it.isEmpty }
        val imported = prefs.importedEntries.takeIf { prefs.importedFresh && !it.isEmpty }
        return SignalMerge.merge(
            base = base,
            manual = manual, manualAt = if (manual != null) prefs.manualEntryAt else 0L,
            cloud = cloud, cloudAt = if (cloud != null) prefs.miCloudSyncedAt else 0L,
            imported = imported, importedAt = if (imported != null) prefs.importedAt else 0L
        )
    }

    // ---------------------------------------------------------------------
    // 天气记录
    // ---------------------------------------------------------------------

    private fun recordToday() {
        val map = records.toMutableMap()
        map[todayKey] = WeatherRecord(todayKey, weather.kind, calibrated)
        records = map
        prefsStore.edit().putString(KEY_RECORDS, serializeRecords(map)).apply()
    }

    /** 记录 → 落盘文本（date|KIND|TAG,TAG）。导入导出共用同一套格式。 */
    private fun serializeRecords(map: Map<String, WeatherRecord>): String =
        map.values.joinToString("\n") { "${it.date}|${it.kind.name}|${it.calibrated.joinToString(",") { c -> c.name }}" }

    /** 落盘文本 → 记录。坏行直接跳过，不抛异常。 */
    private fun parseRecords(raw: String?): Map<String, WeatherRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split('|')
            val date = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val kind = WeatherKind.fromId(parts.getOrNull(1)) ?: return@mapNotNull null
            val tags = parts.getOrNull(2)
                ?.split(',')
                ?.mapNotNull { CalibrationTag.fromId(it.trim()) }
                ?.toSet()
                ?: emptySet()
            date to WeatherRecord(date, kind, tags)
        }.toMap()
    }

    /**
     * 从备份恢复（22.0.0 新增，供「备份与换机」用）。
     * 合并策略：备份里的日期覆盖本机同一天，备份里没有的日期原样保留。
     */
    fun restoreFromBackup(recordLines: String?, encodedPrefs: String?): Int {
        if (!encodedPrefs.isNullOrBlank()) {
            runCatching {
                val next = BodyCastPrefs.decode(encodedPrefs)
                prefs = next
                prefsStore.edit().putString(KEY_PREFS, next.encode()).apply()
                syncAppearance()
            }
        }
        val incoming = parseRecords(recordLines)
        if (incoming.isEmpty()) return 0
        val merged = records.toMutableMap().apply { putAll(incoming) }
        records = merged
        prefsStore.edit().putString(KEY_RECORDS, serializeRecords(merged)).apply()
        return incoming.size
    }

    private fun loadRecords(): Map<String, WeatherRecord> =
        parseRecords(prefsStore.getString(KEY_RECORDS, null))

    // ---------------------------------------------------------------------
    // 处方
    // ---------------------------------------------------------------------

    /** 生成一张新处方。 */
    fun issuePrescription(seedOffset: Long = 0L, excludeIds: Set<String> = emptySet()) {
        val p = engine.prescription(
            weather = weather,
            calibrated = calibrated,
            prefs = adjustedPrefs(),
            seed = daySeed + seedOffset,
            // 22.49.0：今天的运动量决定「先给哪一类」——动够了就先给放松的，
            // 几乎没动就先给要起身的（规则在 [ActivityTilt] 里）。
            activityTilt = ActivityTilt.of(signals.steps, signals.activityMinutes),
            excludeIds = excludeIds
        )
        prescription = p
        doneActions = emptySet()
        feedback = null
    }

    /**
     * 把上一次的反馈翻译成处方偏好。
     *
     * 这一步是「说「更累了」下次就会更短更温和」这句承诺的实现处。
     * 引擎本身不认识反馈——它只认识时长上限和要避开的类别，
     * 所以翻译放在仓库层，引擎保持纯粹。
     *
     * 两条纪律：
     *
     * 1. **每次都以 [prefs] 为基准重新算，不做累加。** 这样用户改口之后，
     *    上一次反馈留下的痕迹会自动消失，不会越积越多。
     *
     * 2. **只往「更保守」的方向调，永远不做减法。** 用户在设置页里明确勾过的
     *    「安静优先」和自定义避开的类别是他的显式意愿，反馈不该把它悄悄摘掉——
     *    那等于用一句随手点的反馈覆盖了用户认真设过的偏好。
     *    所以「身体变轻了」这一档**什么都不改**，保持原样。
     *
     * 3. **用户在设置里关掉「完成反馈后自动调整」，这里必须原样返回。**
     *    这是「显式设置优先于自动调整」：开关关掉之后反馈仍然被记下来，
     *    但不再改变处方的任何一项参数。之前只在页面层拦了「选完立刻换票」，
     *    而换票 / 拖时段 / 左右滑动这些入口同样会走到这里，等于开关没关掉。
     */
    private fun adjustedPrefs(): PrescriptionPrefs {
        val base = prefs
        /** 用户原样设置，不做任何调整。 */
        fun asIs() = PrescriptionPrefs(
            maxMinutes = base.maxMinutes,
            preferQuiet = base.preferQuiet,
            avoid = base.avoid
        )
        // 用户显式关掉了自动调整：一句随手点的反馈不能覆盖这个设置。
        if (!base.autoAdjustAfterFeedback) return asIs()
        return when (lastFeedback) {
            FEEDBACK_HEAVIER -> PrescriptionPrefs(
                // 更短：压到 2 分钟以内
                maxMinutes = minOf(base.maxMinutes, 2),
                preferQuiet = true,
                // 更温和：在用户已有的避开项上再加两项。只加不减。
                avoid = base.avoid + ActionCategory.MOVE + ActionCategory.CONNECT
            )
            FEEDBACK_NOTHING -> PrescriptionPrefs(
                // 没感觉：说明强度偏低，放宽一档
                maxMinutes = (base.maxMinutes + 2).coerceAtMost(10),
                preferQuiet = base.preferQuiet,
                avoid = base.avoid
            )
            // 变轻了 → 现在这套就是合适的，原样保持。
            // 想再来一次 → 强度不变，只是换一张。
            FEEDBACK_LIGHTER, FEEDBACK_AGAIN -> asIs()
            else -> asIs()
        }
    }

    /**
     * 上一次反馈带来的调整，用一句人话说明，给界面显示。
     *
     * 关掉自动调整之后返回 null——不能说「这次排得更短更温和」，
     * 因为那时并没有真的调整过。界面会退回中性提示。
     */
    val feedbackAdjustmentNote: String?
        get() = if (!prefs.autoAdjustAfterFeedback) {
            null
        } else when (lastFeedback) {
            FEEDBACK_HEAVIER -> "按上次说的「更累了」，这次排得更短更温和。"
            FEEDBACK_NOTHING -> "按上次说的「没感觉」，这次稍微放开了一点。"
            FEEDBACK_LIGHTER -> "上次做完说变轻了，这次保持。"
            FEEDBACK_AGAIN -> "上次说想再来一次，这次换一张同强度的。"
            else -> null
        }

    fun markActionDone(actionId: String) {
        doneActions = doneActions + actionId
        recordCare()
        recordAction(actionId)
    }

    /**
     * 做完一轮处方（22.49.0）：**一次记齐**「照顾自己一次」与「做过哪几件」。
     *
     * 以前调用点各自 `recordCare()`，动作名没地方记；现在统一走这里，
     * 首页处方（倒计时走完 / 点完成）都只调这一个方法，避免两套写法漂移。
     */
    fun completeActions(actionIds: List<String>) {
        recordCare()
        actionIds.forEach { recordAction(it) }
    }

    /**
     * 记一件做完的微行动（22.49.0）：按天存 id，回声页照它列出「这周做过什么」。
     * 同一个动作一天内重复完成只记一次（它不是计步器，是记录）。
     */
    fun recordAction(actionId: String) {
        if (actionId.isBlank()) return
        val map = actionLog.toMutableMap()
        val todayList = map[todayKey].orEmpty()
        if (actionId in todayList) return
        map[todayKey] = todayList + actionId
        actionLog = map
        prefsStore.edit().putString(KEY_ACTIONS, serializeActionLog(map)).apply()
    }

    /** 最近 [days] 天（含今天）按天取动作记录，旧的在前。 */
    fun actionsInLastDays(days: Int): List<Pair<String, List<String>>> =
        (days - 1 downTo 0)
            .map { offset -> today.minusDays(offset.toLong()).toString() }
            .mapNotNull { date -> actionLog[date]?.let { date to it } }

    private fun serializeActionLog(map: Map<String, List<String>>): String =
        map.entries.sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value.joinToString("|")}" }

    private fun loadActionLog(): Map<String, List<String>> {
        val raw = prefsStore.getString(KEY_ACTIONS, null)
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('\n').mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) return@mapNotNull null
            val date = line.substring(0, i).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val ids = line.substring(i + 1).split('|').filter { it.isNotBlank() }
            if (ids.isEmpty()) null else date to ids
        }.toMap()
    }

    /**
     * 记一次「照顾自己」（22.30.0）。
     *
     * 完成一件微行动就 +1，键是当天日期——回声页按周求和显示
     * 「本周你照顾自己 N 次」。持久化成 `yyyy-MM-dd=n` 每行一条，
     * 和天气记录同一套落盘纪律：坏行跳过，不抛异常。
     */
    fun recordCare() {
        val map = careLog.toMutableMap()
        map[todayKey] = (map[todayKey] ?: 0) + 1
        careLog = map
        prefsStore.edit().putString(KEY_CARE, serializeCareLog(map)).apply()
    }

    /** 最近 [days] 天（含今天）的照顾次数之和。 */
    fun careCountInLastDays(days: Int): Int =
        (0 until days).sumOf { offset -> careLog[today.minusDays(offset.toLong()).toString()] ?: 0 }

    private fun serializeCareLog(map: Map<String, Int>): String =
        map.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}=${it.value}" }

    private fun loadCareLog(): Map<String, Int> {
        val raw = prefsStore.getString(KEY_CARE, null)
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split('=')
            val date = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val count = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            date to count
        }.toMap()
    }

    // ---------------------------------------------------------------------
    // 食谱闭环（22.32.0）：今天真的吃了什么 / 想吃的清单
    // ---------------------------------------------------------------------

    /**
     * 记下「今天吃了它」。同一天同一道菜重复按不重复记（幂等），
     * 回声页按周汇总时不会把一道菜数成两遍。
     */
    fun recordMeal(foodName: String) {
        if (foodName.isBlank()) return
        val map = mealLog.toMutableMap()
        val today = map[todayKey]?.toMutableList() ?: mutableListOf()
        if (foodName in today) return
        today.add(foodName)
        map[todayKey] = today
        mealLog = map
        prefsStore.edit().putString(KEY_MEALS, serializeMealLog(map)).apply()
    }

    /** 最近 [days] 天（含今天）按天取吃的记录，旧的在前。 */
    fun mealsInLastDays(days: Int): List<Pair<String, List<String>>> =
        (days - 1 downTo 0)
            .map { offset -> today.minusDays(offset.toLong()).toString() }
            .mapNotNull { date -> mealLog[date]?.let { date to it } }

    private fun serializeMealLog(map: Map<String, List<String>>): String =
        map.entries.sortedBy { it.key }
            .joinToString("\n") { "${it.key}=${it.value.joinToString("|")}" }

    private fun loadMealLog(): Map<String, List<String>> {
        val raw = prefsStore.getString(KEY_MEALS, null)
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('\n').mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) return@mapNotNull null
            val date = line.substring(0, i).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val names = line.substring(i + 1).split('|').filter { it.isNotBlank() }
            if (names.isEmpty()) null else date to names
        }.toMap()
    }

    /** 「想吃的」里有没有这道食物。 */
    fun isWished(foodName: String): Boolean = foodName in wishList

    /** 收进 / 移出「想吃的」。返回现在是否在清单里。 */
    fun toggleWish(foodName: String): Boolean {
        val next = if (foodName in wishList) wishList - foodName else wishList + foodName
        wishList = next
        prefsStore.edit().putString(KEY_WISH, next.joinToString("|")).apply()
        return foodName in next
    }

    /** 从「想吃的」移除一条。 */
    fun removeWish(foodName: String) {
        if (foodName !in wishList) return
        wishList = wishList - foodName
        prefsStore.edit().putString(KEY_WISH, wishList.joinToString("|")).apply()
    }

    private fun loadWishList(): List<String> =
        prefsStore.getString(KEY_WISH, null)
            ?.split('|')
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()

    /**
     * 记下完成后的反馈：身体变轻了 / 没感觉 / 更累了 / 想再来一次。
     *
     * 名字不叫 setFeedback 是为了避开与 [feedback] 属性 setter 的 JVM 签名冲突。
     * 这里同时写 [lastFeedback]（持久化），下一次开处方时会读它来调整——
     * 「更累了」下次就更短更温和。
     */
    fun recordFeedback(text: String) {
        feedback = text
        lastFeedback = text
        prefsStore.edit().putString(KEY_LAST_FEEDBACK, text).apply()
    }

    // ---------------------------------------------------------------------
    // 收藏
    // ---------------------------------------------------------------------

    fun toggleFavorite(actionId: String) {
        val next = favorites.toMutableSet()
        if (!next.add(actionId)) next.remove(actionId)
        favorites = next
        prefsStore.edit().putString(KEY_FAVORITES, next.joinToString(",")).apply()
    }

    private fun loadFavorites(): Set<String> =
        prefsStore.getString(KEY_FAVORITES, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    // ---------------------------------------------------------------------
    // 偏好
    // ---------------------------------------------------------------------

    fun updatePrefs(block: (BodyCastPrefs) -> BodyCastPrefs) {
        prefs = block(prefs)
        persistPrefs()
    }

    private fun persistPrefs() {
        prefsStore.edit().putString(KEY_PREFS, prefs.encode()).apply()
        syncAppearance()
    }

    /**
     * 把「外观实验」几项推给玻璃层。
     *
     * [GlassTheme] / [GlassSupport] 不感知仓库，只持有偏好的一份静态快照，
     * 所以每次偏好变化后由这里推一次；推之前先比一次值，值没变就不写状态。
     * 冷启动也走这里（init 块），旧串缺键时各键按默认值兜底。
     */
    private fun syncAppearance() {
        GlassTheme.setAccentStyle(prefs.accentStyle)
        GlassTheme.setCornerStyle(prefs.cornerStyle)
        GlassTheme.setGlassStrength(prefs.glassStrength)
        GlassSupport.setForceFrost(prefs.forceFrost)
    }

    /** 设置页的「清除全部本地数据」。 */
    fun clearAll() {
        prefsStore.edit().clear().apply()
        prefs = BodyCastPrefs()
        syncAppearance()
        deviceSignals = BodySignals()
        calibrated = emptySet()
        customCalibrated = emptySet()
        records = emptyMap()
        prescription = null
        doneActions = emptySet()
        feedback = null
        lastFeedback = null
        listenCount = 0
        careLog = emptyMap()
        mealLog = emptyMap()
        wishList = emptyList()
        favorites = emptySet()
    }

    companion object {
        private const val PREFS = "bodycast_v3"
        private const val KEY_PREFS = "prefs"
        private const val KEY_RECORDS = "records"
        private const val KEY_LISTEN = "listen"
        private const val KEY_CARE = "care_log"
        private const val KEY_MEALS = "meal_log"
        private const val KEY_ACTIONS = "action_log"
        private const val KEY_WISH = "wish_list"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_LAST_FEEDBACK = "last_feedback"

        const val FEEDBACK_LIGHTER = "lighter"
        const val FEEDBACK_NOTHING = "nothing"
        const val FEEDBACK_HEAVIER = "heavier"
        const val FEEDBACK_AGAIN = "again"

        val FEEDBACK_OPTIONS = listOf(FEEDBACK_LIGHTER, FEEDBACK_NOTHING, FEEDBACK_HEAVIER, FEEDBACK_AGAIN)

        /**
         * 直接读一次本机偏好，不建仓库。
         *
         * 给数据源实现用（手动填写读数 / 导入数据这两路只读本机已经存下来的聚合值，
         * 不需要一个完整的仓库实例）。读不到就是默认值，不抛异常。
         */
        fun loadPrefs(context: Context): BodyCastPrefs = BodyCastPrefs.decode(
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PREFS, null)
        )

        fun migrateFeedback(value: String?): String? = when (value) {
            "身体变轻了", FEEDBACK_LIGHTER -> FEEDBACK_LIGHTER
            "没感觉", FEEDBACK_NOTHING -> FEEDBACK_NOTHING
            "更累了", FEEDBACK_HEAVIER -> FEEDBACK_HEAVIER
            "想再来一次", FEEDBACK_AGAIN -> FEEDBACK_AGAIN
            else -> null
        }
    }
}

@Composable
fun rememberBodyCastStore(): BodyCastStore {
    val context = LocalContext.current
    return remember(context) { BodyCastStore(context) }
}
