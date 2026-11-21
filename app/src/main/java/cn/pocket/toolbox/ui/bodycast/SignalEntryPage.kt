package cn.pocket.toolbox.ui.bodycast

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import cn.pocket.toolbox.bodycast.BodyCastPrefs
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.EntryRanges
import cn.pocket.toolbox.bodycast.EntryValues
import cn.pocket.toolbox.data.ImportParser
import cn.pocket.toolbox.glass.GlassRow
import cn.pocket.toolbox.ui.skin.SkinTokens
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * 手动填写读数 / 文件导入（22.24.0，方案 B §3.1–§3.3）。
 *
 * 这两条路是「零权限、零依赖、任何手机都能用」的兜底：
 * - **手动填写**：用户打开厂商 App 看到数字，照着抄进来。不需要 Health Connect、
 *   不需要任何权限、不需要厂商 SDK——所有自动路径都失败时，这条一定可行；
 * - **文件导入**：厂商 App 导出的 CSV / JSON，经系统分享面板或设置页选文件交给本应用，
 *   应用只按表头认列、只存当天的聚合读数。
 *
 * 两条共用同一条纪律（[EntryValues] / [EntryRanges]）：
 * 1. **只填认识的，其余留空**。空 = 没读到，绝不写成 0；
 * 2. **24 小时后过期**，过期就不参与今天的天气判定，界面显示「已过期」；
 * 3. **不碰 Health Connect 的数据**：这里存的是本机偏好里的聚合值，
 *    HC 那一路既不覆盖也不清除，缺项继续显示「暂时没读到」。
 */

/**
 * 跨页的一次性请求。
 *
 * 「现在去首页看看」与「去手动填写读数」都要跳出设置页（换 tab / 进二级页），
 * 而设置页的路由是它自己的局部状态。这里用和 `OnboardingState` 同一套办法：
 * 一个全局计数，主界面看到它变了就切页，设置页看到它变了就进二级页。
 */
object BodyCastNav {

    /** 请求切到首页。设置页里的浮层按一次，主界面消费。 */
    var requestHome by mutableStateOf(false)
        private set

    /** 请求打开「手动填写读数」那一页：主界面先切到设置，设置页再进二级页并消费。 */
    var requestManualEntry by mutableStateOf(false)
        private set

    /**
     * 请求切到食谱页并按元素筛选（22.30.0「天气 → 元素 → 食物」联动）。
     *
     * 值 = 要预选的元素；null = 没有待处理请求。主界面看到非空就切页，
     * 食谱页组合时消费并清空——和 [requestHome] 同一套一次性请求纪律。
     */
    var requestRecipeFocus by mutableStateOf<cn.pocket.toolbox.bodycast.Nutrient?>(null)
        private set

    fun goHome() {
        requestHome = true
    }

    fun openManualEntry() {
        requestManualEntry = true
    }

    /** 首页「今天吃什么」按下：跳食谱页，顺手把今天首推的元素筛上。 */
    fun openRecipe(focus: cn.pocket.toolbox.bodycast.Nutrient) {
        requestRecipeFocus = focus
    }

    /** 食谱页把筛选应用掉之后清请求，免得下次回到食谱又被筛一次。 */
    fun consumeRecipeFocus() {
        requestRecipeFocus = null
    }

    /** 主界面切完页面之后清掉首页请求。 */
    fun consumeHome() {
        requestHome = false
    }

    /** 设置页打开二级页之后清掉这一条，免得下次回到设置又被弹进去一次。 */
    fun consumeManualEntry() {
        requestManualEntry = false
    }

    /**
     * 请求打开「数据连接指导」（22.48.0）。
     *
     * 启动弹窗是第一次遇到它的地方，但用户点了「忽略」之后还得有个**随时能回去**的入口：
     * 设置 → 数据与同步里那一行就发这个请求，主界面把全屏指导页打开。
     * 指导页是主界面之上的一层，不切 tab，所以只清请求、不改 selected。
     */
    var requestDataGuide by mutableStateOf(false)
        private set

    fun openDataGuide() {
        requestDataGuide = true
    }

    fun consumeDataGuide() {
        requestDataGuide = false
    }
}

/**
 * 分享面板送进来的文件（方案 B §3.1）。
 *
 * 厂商 App 的「导出 / 分享」把文件交给系统分享面板，用户在里面选 BodyCast，
 * 系统就把 `ACTION_SEND` 连同文件 uri 发到这里。uri 只是一个「待处理」标记：
 * 由 [ImportInboxHost] 消费一次，解析完就丢掉，**文件内容不落盘**。
 */
object ImportInbox {

    /**
     * 待处理的文件。
     *
     * **刻意不是 Compose 状态**：状态一变就会让宿主那边的 `LaunchedEffect` 重跑，
     * 而没有读完的那一次会被取消掉（实测踩到过：读完之前清状态 = 读了个空）。
     * 触发交给 [ticket]，这里只当一只「取一次就空」的盒子。
     */
    private var pendingUri: Uri? = null

    /** 每来一个文件 +1。宿主按它触发一次解析。 */
    var ticket by mutableStateOf(0)
        private set

    /**
     * 待用户确认的解析结果。
     *
     * **刻意跨组合存活**：分享进来时系统可能新建一个任务 / 重建界面（实测如此），
     * 解析已经跑完、确认卡却随旧组合一起消失是最坏的一种表现——让用户以为「分享了没反应」。
     * 放在这里，组合重建之后卡片自己回来；用户点「写入」或「先不要」时才清掉。
     */
    var preview by mutableStateOf<ImportParser.Result?>(null)
        private set

    /** 主界面收到 Intent 时调一次。不是分享 / 打开文件就什么都不做。 */
    fun accept(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_VIEW) return
        /*
         * 三条路都要试，因为 uri 到底出现在哪一处由发送方和系统决定：
         * 1. EXTRA_STREAM —— 分享面板的标准做法（实测也是这一条）；
         * 2. ClipData —— 系统在跨进程投递前会把流式文件搬进 clipData，部分厂商 App 只留这一份；
         * 3. data —— 「用其他应用打开」给的就是它。
         */
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.getItemAt(0)?.uri
            ?: intent.data
            ?: return
        pendingUri = uri
        ticket++
    }

    /** 取走待处理的文件。取一次就没了，取完不会触发重组。 */
    internal fun take(): Uri? = pendingUri.also { pendingUri = null }

    internal fun offer(result: ImportParser.Result) {
        preview = result
    }

    /** 用户已经处理这张卡（写入 / 先不要）。 */
    fun clearPreview() {
        preview = null
    }
}

/** 一次最多读这么多字符：接到一个巨大的文件也不会把内存吃满。 */
private const val MAX_IMPORT_CHARS = 512 * 1024

/** 把用户交来的文件读成文本。读不到就是 null（界面说「这个文件读不了」）。 */
private fun readImportText(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val buffer = CharArray(8192)
        val out = StringBuilder()
        while (out.length < MAX_IMPORT_CHARS) {
            val read = reader.read(buffer)
            if (read < 0) break
            out.append(buffer, 0, read)
        }
        out.toString()
    }
}.getOrNull()

/**
 * 分享面板那条路的宿主：每来一个文件，解析一次、弹一张确认卡。
 *
 * 放在应用根部（和浮层同级），这样用户在哪个页面收到分享都能看到结果。
 * 两段状态分开：**解析**按 [ImportInbox.ticket] 触发一次；**确认卡的显示**
 * 按「待确认的结果」触发——组合被系统重建（分享常常会新建一个任务）时，
 * 卡片会自己回来，不会出现「分享了没反应」。
 */
@Composable
fun ImportInboxHost(
    backdrop: Backdrop,
    store: BodyCastStore,
    overlay: CastOverlayController
) {
    val context = LocalContext.current
    val ticket = ImportInbox.ticket
    LaunchedEffect(ticket) {
        if (ticket == 0) return@LaunchedEffect
        val uri = ImportInbox.take() ?: return@LaunchedEffect
        val text = withContext(Dispatchers.IO) { readImportText(context, uri) }
        showImportResult(overlay, backdrop, store, text)
    }

    val preview = ImportInbox.preview
    LaunchedEffect(preview) {
        if (preview != null) showImportPreview(overlay, backdrop, store, preview)
    }
}

/**
 * 设置页那条路：`OpenDocument()` 选一个文件。
 *
 * 照抄 `MyDataPage` 里已有的 SAF 写法（`rememberLauncherForActivityResult` +
 * `contentResolver.openInputStream`）：**不申请任何存储权限**，SAF 自己不需要。
 */
@Composable
fun rememberImportPicker(
    backdrop: Backdrop,
    store: BodyCastStore
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current
    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        showImportResult(overlay, backdrop, store, readImportText(context, uri))
    }
}

/**
 * 解析结果 → 确认卡。
 *
 * 四个分支各有话说，且都不猜：
 * - 读不了（不是文本 / 空文件）→「这个文件读不了」；
 * - 一列都不认识 → 也读不了，但**把识别到的表头列出来**，方便下次加映射；
 * - 日期不是今天 / 昨夜 → 拒绝写入，说清楚是几天前的；
 * - 认得一部分 → 「识别 N 项 / 忽略 M 列」，用户点「写入」才落盘。
 */
fun showImportResult(
    overlay: CastOverlayController,
    backdrop: Backdrop,
    store: BodyCastStore,
    text: String?
) {
    val result = ImportParser.parse(text)
    if (result == null) {
        overlay.show(
            title = "这个文件读不了",
            subtitle = "它不是表格文本，或者里面是空的。"
        ) {
            CastHint("在厂商 App 里用「导出」生成 CSV，再从分享面板发给本应用，或者用设置里的「导入数据文件」。")
            CastHint("Excel 文件请先另存为 CSV 再导入。")
        }
        return
    }

    if (result.staleDays != null) {
        overlay.show(
            title = "这份数据是 ${result.staleDays} 天前的",
            subtitle = "只接受今天或昨夜导出的数据，所以这次没有写入。"
        ) {
            CastHint("旧数据不参与今天的天气判定——读不到就说没读到，不拿旧数字顶上。")
            CastHint("在厂商 App 里选今天（或昨夜）重新导出一次就好。")
        }
        return
    }

    if (result.values.isEmpty) {
        overlay.show(
            title = "这个文件读不了",
            subtitle = "表头里没有一列是它认识的读数。"
        ) {
            CastHint("识别到的表头：")
            result.allHeaders.take(12).forEach { header -> RawHint("· $header") }
            CastHint("只按表头认列，不按列序猜：表头里有「静息心率 / 心率变异性 HRV / 睡眠时长 / 深睡 / 步数 / 血氧 / 运动时长」这些字样的列才会被读。")
            CastHint("没有一列被写进本机，也没有填 0。")
        }
        return
    }

    // 认得出来：把结果放到跨组合存活的那一格，由 [showImportPreview] 显示确认卡。
    // （分享进来时系统常常新建一个任务，界面会重建一次——卡片不能跟着旧组合消失。）
    ImportInbox.offer(result)
}

/**
 * 确认卡：**识别 N 项 / 忽略 M 列**，用户点「写入」才落盘。
 *
 * 显示由 [ImportInbox.preview] 驱动，组合重建之后会自己回来；用户点
 * 「写入」或「先不要」时由这里清掉那一格。
 */
fun showImportPreview(
    overlay: CastOverlayController,
    backdrop: Backdrop,
    store: BodyCastStore,
    result: ImportParser.Result
) {
    val recognized = result.recognized
    overlay.show(
        title = "识别 ${recognized.size} 项 / 忽略 ${result.ignoredHeaders.size} 列",
        subtitle = "这是从你的文件里读到的内容，确认之后再写入本机。",
        tall = true,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    CastSecondaryButton(
                        text = "先不要",
                        onClick = {
                            ImportInbox.clearPreview()
                            overlay.dismiss()
                        },
                        backdrop = backdrop
                    )
                }
                Box(Modifier.weight(1f)) {
                    CastPrimaryButton(
                        text = "写入",
                        onClick = {
                            ImportInbox.clearPreview()
                            store.saveImportedEntry(result.values)
                            overlay.show(
                                title = "已导入：${entryStamp(store.prefs.importedAt)} · 识别 ${recognized.size} 项",
                                subtitle = "这一份读数 24 小时内有效。"
                            ) {
                                CastHint("没识别到的项一律留空，界面继续显示「暂时没读到」。")
                                CastHint("设备读到的数据没有被覆盖，也没有被清掉。")
                                CastHint("原始文件只在内存里解析过，不会留在手机上。")
                                Spacer(Modifier.height(4.dp))
                                CastPrimaryButton(
                                    text = "现在去首页看看",
                                    onClick = {
                                        overlay.dismiss()
                                        BodyCastNav.goHome()
                                    },
                                    backdrop = backdrop
                                )
                            }
                        },
                        backdrop = backdrop
                    )
                }
            }
        }
    ) {
        // 行右那一列是文件里读到的原值（数字 / 百分比），走 [RawText]：
        // 它是数据不是文案，过翻译只会在漏翻审计里记假缺口。
        recognized.forEach { field ->
            GlassRow(
                backdrop = backdrop,
                title = field.label,
                chevron = false,
                onClick = null,
                surface = false,
                trailing = {
                    RawText(
                        text = field.display(result.values).orEmpty(),
                        fontSize = SkinTokens.body,
                        fontWeight = FontWeight.SemiBold,
                        color = SkinTokens.muted
                    )
                }
            )
        }
        if (result.ignoredHeaders.isNotEmpty()) {
            CastHint("忽略的列：")
            result.ignoredHeaders.take(12).forEach { header -> RawHint("· $header") }
        }
        CastHint("只按表头认列；认不出的列不会被写进来，也不会按列序猜。")
        CastHint("写入的是当天的聚合读数，明细不会留在这台手机上。")
    }
}

// ---------------------------------------------------------------------------
// 手动填写读数
// ---------------------------------------------------------------------------

/** 一个输入项：标题、厂商 App 里在哪看、取值范围。 */
private data class ManualField(
    val title: String,
    val hint: String,
    val range: IntRange,
    val raw: String,
    val onValueChange: (String) -> Unit
)

/** 空的 = 这次没填（合法）；填了但不是数字 / 超范围 = 不合法，不硬存。 */
private fun accepts(raw: String, range: IntRange): Boolean {
    val text = raw.trim()
    if (text.isEmpty()) return true
    val value = text.toIntOrNull() ?: return false
    return value in range
}

/** 不合法时那行小字（中文源串，翻译走 `BodyCastText` 的模板）。 */
private fun problemSource(raw: String, title: String, range: IntRange): String =
    if (raw.trim().toIntOrNull() == null) {
        "$title 只能填数字。"
    } else {
        "$title 只能填 ${range.first}-${range.last}。"
    }

/** 「上次填写：今天 08:12」「…（已过期）」「还没有填过」。 */
fun entryStatusText(prefix: String, emptyText: String, at: Long, fresh: Boolean): String {
    if (at <= 0L) return emptyText
    val stamp = entryStamp(at)
    return if (fresh) "$prefix：$stamp" else "$prefix：$stamp（已过期）"
}

/**
 * 时间戳 → 「今天 08:12」。
 *
 * **必须是中文源串**，翻译交给 `BodyCastText` 的模板（`今天 (\d{2}:\d{2})`）。
 * 直接拼 `HealthConnectBridge.writtenAt()` 那种已翻译的串会让整句再进一次翻译，
 * 那段英文在表里查不到，就会在「漏翻审计」里记一条假缺口。
 */
internal fun entryStamp(at: Long): String {
    val zone = ZoneId.systemDefault()
    val local = Instant.ofEpochMilli(at).atZone(zone)
    val time = "%02d:%02d".format(local.hour, local.minute)
    val date = local.toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "今天 $time"
        today.minusDays(1) -> "昨天 $time"
        // 更早的日期用纯数字，不依赖语言（和自检卡片那一列同一套写法）。
        else -> "%02d-%02d".format(date.monthValue, date.dayOfMonth) + " " + time
    }
}

/**
 * 与 `CastHint` 同一个观感的**原样文字**。
 *
 * 文件里的表头、数值都是用户的数据，不是界面文案：走翻译会在
 * 「漏翻审计」里记一堆假缺口（那些串本来就永远查不到）。
 */
@Composable
private fun RawHint(text: String) {
    RawText(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        fontSize = SkinTokens.caption,
        lineHeight = 22.sp,
        color = SkinTokens.muted
    )
}

@Composable
fun ManualEntryPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs
    val last = prefs.manualEntries

    // 输入框一律从空开始：空 = 这次没填。想沿用上次的数字有「和上次一样」那一个按钮，
    // 免得用户没注意就把昨天抄的数字当成今天又存了一遍。
    var restingHeartRate by remember { mutableStateOf("") }
    var hrv by remember { mutableStateOf("") }
    var sleepMinutes by remember { mutableStateOf("") }
    var deepSleepPercent by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var bloodOxygen by remember { mutableStateOf("") }
    var activityMinutes by remember { mutableStateOf("") }

    val fields = listOf(
        ManualField(
            title = "静息心率",
            hint = "小米运动健康 → 心率 → 静息心率；华为运动健康 → 心脏健康。单位：次/分",
            range = EntryRanges.restingHeartRate,
            raw = restingHeartRate,
            onValueChange = { restingHeartRate = it }
        ),
        ManualField(
            title = "心率变异性 HRV",
            hint = "小米运动健康 → 心率 → 心率变异性；华为运动健康 → 心脏健康。单位：毫秒",
            range = EntryRanges.hrv,
            raw = hrv,
            onValueChange = { hrv = it }
        ),
        ManualField(
            title = "睡眠时长",
            hint = "厂商 App 首页的「睡眠」卡片：昨晚一共睡了多久。单位：分钟（7 小时 32 分 = 452）",
            range = EntryRanges.sleepMinutes,
            raw = sleepMinutes,
            onValueChange = { sleepMinutes = it }
        ),
        ManualField(
            title = "深睡占比",
            hint = "同一张睡眠卡片：深睡时长 ÷ 总睡眠时长。填百分数（21% 就填 21）",
            range = 0..100,
            raw = deepSleepPercent,
            onValueChange = { deepSleepPercent = it }
        ),
        ManualField(
            title = "步数",
            hint = "厂商 App 首页的「步数」卡片：到现在的今日步数",
            range = EntryRanges.steps,
            raw = steps,
            onValueChange = { steps = it }
        ),
        ManualField(
            title = "血氧",
            hint = "小米运动健康 → 血氧；华为运动健康 → 血氧。填百分数（97% 就填 97）",
            range = EntryRanges.bloodOxygen,
            raw = bloodOxygen,
            onValueChange = { bloodOxygen = it }
        ),
        ManualField(
            title = "运动时长",
            hint = "厂商 App 的「运动 / 锻炼」卡片：今天的中高强度时长。单位：分钟",
            range = EntryRanges.activityMinutes,
            raw = activityMinutes,
            onValueChange = { activityMinutes = it }
        )
    )

    val problems = fields.filterNot { accepts(it.raw, it.range) }
    val filledCategories = listOf(
        restingHeartRate, hrv,
        if (sleepMinutes.trim().isEmpty()) deepSleepPercent else sleepMinutes,
        steps, bloodOxygen, activityMinutes
    ).count { it.trim().isNotEmpty() }

    fun fillLast() {
        restingHeartRate = last.restingHeartRate?.toString().orEmpty()
        hrv = last.hrv?.toString().orEmpty()
        sleepMinutes = last.sleepMinutes?.toString().orEmpty()
        deepSleepPercent = last.deepSleepRatio?.let { (it * 100).roundToInt().toString() }.orEmpty()
        steps = last.steps?.toString().orEmpty()
        bloodOxygen = last.bloodOxygen?.toString().orEmpty()
        activityMinutes = last.activityMinutes?.toString().orEmpty()
    }

    fun save() {
        if (problems.isNotEmpty() || filledCategories == 0) return
        val values = EntryValues(
            restingHeartRate = restingHeartRate.trim().toIntOrNull(),
            hrv = hrv.trim().toIntOrNull(),
            sleepMinutes = sleepMinutes.trim().toIntOrNull(),
            deepSleepRatio = deepSleepPercent.trim().toIntOrNull()?.let { (it / 100f).coerceIn(0f, 1f) },
            steps = steps.trim().toIntOrNull(),
            bloodOxygen = bloodOxygen.trim().toIntOrNull(),
            activityMinutes = activityMinutes.trim().toIntOrNull()
        )
        store.saveManualEntry(values)
        // 存完把输入框清空：这份数字已经记下了，留着容易让人以为还没保存。
        restingHeartRate = ""
        hrv = ""
        sleepMinutes = ""
        deepSleepPercent = ""
        steps = ""
        bloodOxygen = ""
        activityMinutes = ""
        overlay.show(
            title = "已记录：${entryStamp(store.prefs.manualEntryAt)} · 6 类里填了 ${values.categoryCount} 类",
            subtitle = "这份读数 24 小时内有效，之后就不再参与今天的天气判定。"
        ) {
            CastHint("没填的项一律留空，界面继续显示「暂时没读到」。")
            CastHint("设备读到的数据没有被覆盖，也没有被清掉。")
            Spacer(Modifier.height(4.dp))
            CastPrimaryButton(
                text = "现在去首页看看",
                onClick = {
                    overlay.dismiss()
                    BodyCastNav.goHome()
                },
                backdrop = backdrop
            )
        }
    }

    CastPage {
        item {
            CastPageHeader(
                title = "手动填写读数",
                subtitle = "打开厂商 App 看到数字，照着抄进来。不认识的项留空就行，留空不会当成 0。",
                trailing = {
                    CastIconButton(
                        icon = "back",
                        label = "返回",
                        onClick = onBack,
                        backdrop = backdrop,
                        tint = SkinTokens.muted
                    )
                }
            )
        }

        item {
            CastCard(backdrop = backdrop) {
                CastHint(
                    entryStatusText(
                        prefix = "上次填写",
                        emptyText = "还没有填过。填过一次之后，下次可以用「和上次一样」。",
                        at = prefs.manualEntryAt,
                        fresh = prefs.manualEntryFresh
                    )
                )
                CastHint("填进来的数字只用于今天的身体天气，不参与诊断；只存这几项聚合值，明细不落盘。")
            }
        }

        item {
            Box(Modifier.fillMaxWidth()) {
                CastSecondaryButton(
                    text = "和上次一样",
                    icon = "reset",
                    enabled = !last.isEmpty,
                    onClick = { fillLast() },
                    backdrop = backdrop
                )
            }
        }

        fields.forEach { field ->
            item {
                CastCard(backdrop = backdrop) {
                    Text(
                        text = field.title,
                        fontSize = SkinTokens.body,
                        fontWeight = FontWeight.SemiBold,
                        color = SkinTokens.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassTextField(
                        value = field.raw,
                        onValueChange = { input -> field.onValueChange(input.filter { it.isDigit() }.take(6)) },
                        placeholder = "填数字，不认识就留空",
                        backdrop = backdrop
                    )
                    CastHint(field.hint)
                    if (!accepts(field.raw, field.range)) {
                        CastHint(problemSource(field.raw, field.title, field.range))
                    }
                }
            }
        }

        item {
            CastCard(backdrop = backdrop) {
                CastPrimaryButton(
                    text = "保存这几项读数",
                    icon = "check",
                    enabled = problems.isEmpty() && filledCategories > 0,
                    onClick = { save() },
                    backdrop = backdrop
                )
                when {
                    problems.isNotEmpty() -> CastHint(
                        "有 ${problems.size} 项填得不对：超出范围的数字不会被保存，改好再点保存。"
                    )
                    filledCategories == 0 -> CastHint("六类里至少填一项再保存。只填你认识的那几项就够了。")
                    else -> CastHint("现在会记录 6 类里的 ${filledCategories} 类。")
                }
            }
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "清空手动填写的读数",
                icon = "trash",
                iconTint = SkinTokens.muted,
                subtitle = "只清这一路；设备读到的数据不受影响。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.confirm(
                        title = "清空手动填写的读数",
                        message = "会清掉你抄进来的这几项数字。设备读到的数据不受影响，这一步不能撤销。",
                        confirmLabel = "清空",
                        cancelLabel = "先不要"
                    ) {
                        store.clearManualEntry()
                    }
                }
            )
        }
    }
}

/**
 * 设置页那一行右侧的状态字（短：这一列宽，长文案会把标题挤掉）。
 *
 * 过期时只写「已过期」——具体时间写在那一页顶上（「上次填写：昨天 04:25（已过期）」），
 * 列表行里塞一整个时间戳会把标题挤成「Manua…」。
 */
fun manualEntryStatus(prefs: BodyCastPrefs): String = when {
    prefs.manualEntryAt <= 0L -> "还没有填过"
    prefs.manualEntryFresh -> entryStamp(prefs.manualEntryAt)
    else -> "已过期"
}

/** 导入那一行右侧的状态字。 */
fun importStatus(prefs: BodyCastPrefs): String = when {
    prefs.importedAt <= 0L -> "还没有导入过"
    prefs.importedFresh -> entryStamp(prefs.importedAt)
    else -> "已过期"
}
