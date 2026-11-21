package cn.pocket.toolbox.ui.bodycast

import android.os.Build
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.BuildConfig
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.data.Backup
import cn.pocket.toolbox.notify.Notifier
import cn.pocket.toolbox.notify.ReminderScheduler
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.CalibrationTag
import cn.pocket.toolbox.bodycast.FetchFreshness
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.data.HealthConnectBridge
import cn.pocket.toolbox.data.HealthConnectWriter
import cn.pocket.toolbox.data.MiCloudSource
import cn.pocket.toolbox.data.micloud.MiCloudApi
import cn.pocket.toolbox.data.micloud.MiLicense
import cn.pocket.toolbox.data.micloud.MiSession
import cn.pocket.toolbox.data.micloud.MiTokenVault
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.i18n.TranslationAudit
import cn.pocket.toolbox.glass.GlassRow
import cn.pocket.toolbox.glass.GlassSupport
import cn.pocket.toolbox.ui.skin.SkinTokens
import cn.pocket.toolbox.glass.GlassIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 页面四：设置（10.0.0 起改为二级菜单结构）。
 *
 * 主菜单按「你此刻想改什么」分成六组（数据 / 身体 / 内容 / 提醒 / 数据与隐私 /
 * 外观与关于），每组一张卡、卡内一到两行；二级页共九页：
 * 数据与同步 / 身体天气校准 / 微行动与处方 / 食谱指南 / 通知与提醒 /
 * 我的数据 / 权限与说明 / 外观与体验 / 关于。
 * 每组的详细设置收进各自的二级页，不再把几十行设置平摊在一页里。
 * 路由是页面内的局部状态：进二级页、返回，都不出这个 tab。
 *
 * 三条贯穿全页的规矩：
 * 1. **没有红色。** 连「删除所有数据」这种不可逆操作，也只是措辞明确，不是警告。
 * 2. **读不到就说读不到。** Health Connect 未授权、设备不支持、权限为空，
 *    一律显示「暂时没读到」或真实状态，绝不补一个看起来正常的数字。
 * 3. **只用这套玻璃控件。** 页面里不出现 Material3 的 Button / Switch / Card /
 *    Slider / TextField / AlertDialog；浮层一律走 [LocalCastOverlay]。
 *
 * 另外：手环接入统一经过系统健康服务，**引导文案里不出现手环直连的说法**。
 */

/** 搜索命中项 → 二级页路由（22.55.0）。 */
private fun SettingsSearch.SettingsSearchRoute.toSettingsRoute(): SettingsRoute = when (this) {
    SettingsSearch.SettingsSearchRoute.DATA_SOURCE -> SettingsRoute.DATA_SOURCE
    SettingsSearch.SettingsSearchRoute.CALIBRATION -> SettingsRoute.CALIBRATION
    SettingsSearch.SettingsSearchRoute.PRESCRIPTION -> SettingsRoute.PRESCRIPTION
    SettingsSearch.SettingsSearchRoute.RECIPE -> SettingsRoute.RECIPE
    SettingsSearch.SettingsSearchRoute.NOTIFY -> SettingsRoute.NOTIFY
    SettingsSearch.SettingsSearchRoute.MY_DATA -> SettingsRoute.MY_DATA
    SettingsSearch.SettingsSearchRoute.PERMISSIONS -> SettingsRoute.PERMISSIONS
    SettingsSearch.SettingsSearchRoute.APPEARANCE -> SettingsRoute.APPEARANCE
    SettingsSearch.SettingsSearchRoute.ABOUT -> SettingsRoute.ABOUT
    SettingsSearch.SettingsSearchRoute.DEVELOPER -> SettingsRoute.DEVELOPER
    SettingsSearch.SettingsSearchRoute.MANUAL_ENTRY -> SettingsRoute.MANUAL_ENTRY
    SettingsSearch.SettingsSearchRoute.MI_CLOUD -> SettingsRoute.MI_CLOUD
    SettingsSearch.SettingsSearchRoute.SETUP_GUIDE -> SettingsRoute.SETUP_GUIDE
    SettingsSearch.SettingsSearchRoute.IMPORT_FILE -> SettingsRoute.DATA_SOURCE
    SettingsSearch.SettingsSearchRoute.DELETE_ALL -> SettingsRoute.MY_DATA
    SettingsSearch.SettingsSearchRoute.LANGUAGE -> SettingsRoute.APPEARANCE
    SettingsSearch.SettingsSearchRoute.THEME -> SettingsRoute.APPEARANCE
}

/** 二级页路由。null = 主菜单。 */
private enum class SettingsRoute {
    DATA_SOURCE, CALIBRATION, PRESCRIPTION, RECIPE, NOTIFY,
    MY_DATA, PERMISSIONS, APPEARANCE, ABOUT, DEVELOPER, MANUAL_ENTRY, MI_CLOUD, SETUP_GUIDE
}

@Composable
fun SettingsTab(
    backdrop: Backdrop,
    store: BodyCastStore
) {
    val context = LocalContext.current
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    // 22.36.0：单槽 route 换成**导航栈**——以前二级页（关于）→ 三级页（开发者模式）
    // 是同一个变量被覆盖，返回时直接置空，一步跳回设置一级页。
    // 现在三级→二级→一级逐级退，侧滑与返回键同一套语义。
    val stack = remember { androidx.compose.runtime.mutableStateListOf<SettingsRoute>() }
    val route = stack.lastOrNull()
    fun push(r: SettingsRoute) {
        if (stack.lastOrNull() != r) stack.add(r)
    }
    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    // 系统返回键 / 侧滑返回：逐级退回，一级菜单不拦截（由系统退出应用）。
    // 浮层打开时浮层自己的 BackHandler 优先（它组合在更外层、后注册）。
    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    // 首页那条「去手动填写读数」的引导：主界面切到设置之后，这里把二级页直接打开。
    // 消费一次就清掉，免得下次回到设置又被弹进去。
    LaunchedEffect(BodyCastNav.requestManualEntry) {
        if (BodyCastNav.requestManualEntry) {
            stack.clear()
            push(SettingsRoute.MANUAL_ENTRY)
            BodyCastNav.consumeManualEntry()
        }
    }

    /*
     * 22.48.0：从「数据与同步」里点「连接运动数据」= 请主界面把全屏指导页打开。
     * 指导页是主界面之上的一层（不是设置里的二级页），所以这里只负责把设置这一层
     * 退回一级菜单——不然关掉指导页之后，用户还停在设置二级页上，会觉得没退干净。
     */
    LaunchedEffect(BodyCastNav.requestDataGuide) {
        if (BodyCastNav.requestDataGuide) {
            stack.clear()
        }
    }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            // 22.36.0 动画批：转场改成**单页运动**——同一时刻只有一页在动，
            // 另一页只做快速淡出/淡入。以前两页同时滑，两棵玻璃树逐帧合成，
            // 中端机掉帧明显；现在运动页保持 220ms 快进缓落，静止页 120ms 淡，
            // 体感更利落，合成量也减半。「动画效果」关掉时经 [CastMotion] 归零硬切。
            if (targetState != null) {
                // 进入二级菜单：新页从右向左滑入，旧页原地快淡出
                (
                    slideInHorizontally(CastMotion.enter(CastMotion.MEDIUM)) { it / 6 } + fadeIn(CastMotion.enter(160))
                    ).togetherWith(
                    fadeOut(CastMotion.exit(120))
                )
            } else {
                // 退回上级菜单：旧页向右滑出，新页原地淡入
                (
                    fadeIn(CastMotion.enter(160))
                    ).togetherWith(
                    slideOutHorizontally(CastMotion.exit(180)) { it / 6 } + fadeOut(CastMotion.exit(120))
                )
            }
        },
        label = "settings-route"
    ) { currentRoute ->
        when (currentRoute) {
            null -> SettingsMainMenu(
                backdrop = backdrop,
                store = store,
                debuggable = debuggable,
                onNavigate = { push(it) }
            )
            SettingsRoute.DATA_SOURCE -> DataSourcePage(
                backdrop = backdrop,
                store = store,
                onBack = { pop() },
                onOpenManualEntry = { push(SettingsRoute.MANUAL_ENTRY) },
                onOpenMiCloud = { push(SettingsRoute.MI_CLOUD) },
                onOpenSetupGuide = { push(SettingsRoute.SETUP_GUIDE) }
            )
            SettingsRoute.CALIBRATION -> CalibrationPage(backdrop, store) { pop() }
            SettingsRoute.PRESCRIPTION -> PrescriptionPrefsPage(backdrop, store) { pop() }
            SettingsRoute.RECIPE -> RecipePrefsPage(backdrop, store) { pop() }
            SettingsRoute.NOTIFY -> NotifyPage(backdrop, store) { pop() }
            SettingsRoute.MY_DATA -> MyDataPage(backdrop, store) { pop() }
            SettingsRoute.PERMISSIONS -> PermissionsPage(backdrop, store) { pop() }
            SettingsRoute.APPEARANCE -> AppearancePage(backdrop, store) { pop() }
            SettingsRoute.ABOUT -> AboutPage(
                backdrop = backdrop,
                store = store,
                onOpenDeveloper = { push(SettingsRoute.DEVELOPER) }
            ) { pop() }
            SettingsRoute.DEVELOPER -> DeveloperPage(backdrop, store) { pop() }
            SettingsRoute.MANUAL_ENTRY -> ManualEntryPage(backdrop, store) { pop() }
            SettingsRoute.MI_CLOUD -> MiCloudPage(backdrop, store) { pop() }
            SettingsRoute.SETUP_GUIDE -> SetupGuidePage(backdrop, store) { pop() }
        }
    }
}

// ---------------------------------------------------------------------------
// 主菜单
// ---------------------------------------------------------------------------

@Composable
private fun SettingsMainMenu(
    backdrop: Backdrop,
    store: BodyCastStore,
    debuggable: Boolean,
    onNavigate: (SettingsRoute) -> Unit
) {
    // 22.55.0：搜索。设置的二/三级菜单加起来几十项，找一个开关要一层层翻；
    // 首页顶部一个框，搜到的条目直接落到对应那一页。
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { SettingsSearch.search(query) }
    val prefs = store.prefs
    val themeLabel = listOf("浅色", "深色", "跟随系统")[prefs.themeMode.coerceIn(0, 2)]
    val notifyCount = listOf(
        prefs.handshakeReminder, prefs.actionReminder, prefs.calendarReminder
    ).count { it } + if (prefs.quietMode) 1 else 0

    CastPage {
        item {
            CastPageHeader(
                title = "设置",
                subtitle = "数据从哪来、身体天气怎么定、提醒怎么发，都分门别类放在这里。"
            )
        }

        // 演示数据必须在最显眼的位置说明白，不能让人以为这是自己的真实状态。
        if (prefs.demoMode) {
            item {
                CastBanner(
                    text = "当前是演示数据，不是你的真实身体状态。",
                    backdrop = backdrop,
                    icon = "info",
                    tint = SkinTokens.peach
                )
            }
        }

        // 一级菜单按「你此刻想改什么」分组，每组一张卡；组名在上、行在下，
        // 一眼能看出「数据的事」和「身体的事」不是一回事（22.1.0 重排）。
        // ---- 搜索（22.55.0；22.56.0 人性化）----
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    GlassTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "搜设置：深色、过敏原、语音、备份…",
                        backdrop = backdrop,
                        // 22.59.0：搜索框两头全圆（胶囊），和按钮同族。
                        shape = com.kyant.shapes.Capsule()
                    )
                }
                // 有词的时候给一个「清空」——不用一个字一个字删回去。
                if (query.isNotEmpty()) {
                    CastIconButton(
                        icon = "close",
                        label = "清空",
                        onClick = { query = "" },
                        backdrop = backdrop,
                        tint = SkinTokens.muted
                    )
                }
            }
        }
        if (query.isNotBlank()) {
            if (hits.isEmpty()) {
                item {
                    CastCard(backdrop = backdrop) {
                        CastHint("没有找到和「" + query + "」有关的设置项。")
                        CastHint("换个说法试试——「深色」「夜间」「暗色」都能找到同一个开关；")
                        CastHint("也可以直接说你想干什么，比如「备份」「导出」「语音」。")
                    }
                }
                item {
                    CastSecondaryButton(
                        text = "清空重搜",
                        icon = "close",
                        onClick = { query = "" },
                        backdrop = backdrop
                    )
                }
            } else {
                item { CastHint("找到 " + hits.size + " 项，点一下直接进去。") }
                hits.forEach { hit ->
                    val why = hit.hitOf(query)
                    item {
                        GlassActionRow(
                            backdrop = backdrop,
                            title = BodyCastText.text(hit.title),
                            icon = "settings",
                            // 为什么命中：同义词命中时用户才知道「原来它叫这个」。
                            subtitle = BodyCastText.text(hit.group) +
                                if (why != null && why != hit.title) {
                                    " · " + BodyCastText.text("匹配：") + why
                                } else {
                                    ""
                                },
                            shape = RoundedCornerShape(SkinTokens.cardRadius),
                            onClick = {
                                query = ""
                                onNavigate(hit.route.toSettingsRoute())
                            }
                        )
                    }
                }
            }
            // 有查询词时菜单让位：这一屏只放结果。
            return@CastPage
        }

        item { CastGroupLabel("数据") }
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "数据与同步",
                    icon = "network",
                    subtitle = "手环接入、Health Connect 授权、数据来源、手动同步。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.DATA_SOURCE) }
                )
            }
        }

        item { CastGroupLabel("身体") }
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "身体天气校准",
                    icon = "target",
                    subtitle = "默认天气、校准标签、敏感度。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.CALIBRATION) }
                )
                CastDivider()
                GlassActionRow(
                    backdrop = backdrop,
                    title = "微行动与处方",
                    icon = "clock",
                    subtitle = "时长预算、可用场景、语音引导。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.PRESCRIPTION) }
                )
            }
        }

        item { CastGroupLabel("内容") }
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "食谱指南",
                    icon = "leaf",
                    subtitle = "饮食偏好、过敏原提醒、搜索范围。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.RECIPE) }
                )
            }
        }

        item { CastGroupLabel("提醒") }
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "通知与提醒",
                    icon = "bell",
                    subtitle = if (notifyCount == 0) "现在全部关着。" else "已开启 $notifyCount 项。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.NOTIFY) }
                )
            }
        }

        item { CastGroupLabel("数据与隐私") }
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "我的数据",
                    icon = "up",
                    subtitle = "导出到剪贴板、备份与恢复、删除所有数据。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.MY_DATA) }
                )
                CastDivider()
                GlassActionRow(
                    backdrop = backdrop,
                    title = "权限与说明",
                    icon = "shield",
                    subtitle = "授权状态、隐私说明、系统权限。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.PERMISSIONS) }
                )
            }
        }

        item { CastGroupLabel("外观与关于") }
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "外观与体验",
                    icon = "sun",
                    subtitle = "主题（$themeLabel）、动画、字体大小。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.APPEARANCE) }
                )
                CastDivider()
                GlassActionRow(
                    backdrop = backdrop,
                    title = "关于",
                    icon = "info",
                    subtitle = "开发者、版本、开源许可。",
                    surface = false,
                    onClick = { onNavigate(SettingsRoute.ABOUT) }
                )
            }
        }

        // 开发者选项只在可调试构建里出现，正式包里整段都不存在。
        if (debuggable) {
            item { CastGroupLabel("开发者选项") }

            item {
                var verboseLog by remember { mutableStateOf(false) }
                GlassToggleRow(
                    backdrop = backdrop,
                    title = "日志开关",
                    checked = verboseLog,
                    subtitle = "只影响本机日志输出。",
                    onCheckedChange = { verboseLog = it }
                )
            }

            item {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "模拟数据快捷入口",
                    icon = "bolt",
                    subtitle = "一键把演示读数填进来。",
                    onClick = { store.useDemoData() }
                )
            }
        }
    }
}

/** 二级页统一的返回按钮。 */
@Composable
private fun BackButton(onBack: () -> Unit, backdrop: Backdrop) {
    CastIconButton(
        icon = "back",
        label = "返回",
        onClick = onBack,
        backdrop = backdrop,
        tint = SkinTokens.muted
    )
}

/**
 * 自检卡片里的一步（22.22.0）：标题 + 三态 + 说明。
 *
 * 三态写成文字（通 / 断 / 未知）而不是靠颜色：这套界面里没有红色，
 * 而且色弱用户也得一眼分得出「断」。
 */
@Composable
private fun PipelineStepRow(
    backdrop: Backdrop,
    title: String,
    state: HealthConnectBridge.StepState,
    detail: String
) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        subtitle = detail,
        value = state.label,
        chevron = false,
        onClick = null,
        // 卡片本身已经是玻璃面了，这里不再叠一层。
        surface = false,
        shape = RoundedCornerShape(SkinTokens.secondaryRadius)
    )
}

/**
 * 自检卡片顶部的那个主按钮是什么（22.23.0）。
 *
 * 四步里断在哪一步，主按钮就是那一步的修法；四步全通时它变成「手动同步」。
 * 每一步自己的按钮只在「主按钮不是这件事」时才出现——同一个动作不在同一张卡上出现两次。
 */
private enum class PipelineAction {
    /** 自检还没跑完，没有可推荐的动作。 */
    WAIT,
    INSTALL_HEALTH,
    GRANT,
    OPEN_VENDOR,
    INSTALL_VENDOR,
    SYNC
}

/** 自检时刻的 HH:mm（设备时区）。 */
private fun hhmm(at: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))

// ---------------------------------------------------------------------------
// 二级页 1：数据源管理
// ---------------------------------------------------------------------------

@Composable
private fun DataSourcePage(
    backdrop: Backdrop,
    store: BodyCastStore,
    onBack: () -> Unit,
    onOpenManualEntry: () -> Unit,
    onOpenMiCloud: () -> Unit,
    onOpenSetupGuide: () -> Unit
) {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current
    val scope = rememberCoroutineScope()
    val prefs = store.prefs

    // ---- Health Connect 的异步状态：null 表示「正在检查」，不是「没有」。 ----
    var refreshKey by remember { mutableIntStateOf(0) }
    // 四步自检的结果：①系统健康服务 ②授权 ③厂商最后写入时间 ④本次读数。
    // 界面只认这一份结论，不再自己拼判断——页面里也不再有第二份
    // 「可用状态 / 授权列表」拷贝（22.23.0 起那两块独立行已并进卡片）。
    var diagnosis by remember { mutableStateOf<HealthConnectBridge.Diagnosis?>(null) }
    // 这一份结论是什么时候跑出来的：卡里显示「刚刚检查于 HH:MM」，刷新后跟着变。
    var checkedAt by remember { mutableStateOf<Long?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var syncNote by remember { mutableStateOf<String?>(null) }
    // 自检正在跑（防止 ON_RESUME 与授权框回调叠跑两遍）。
    var diagnosing by remember { mutableStateOf(false) }
    // 有没有真的离开过这一页（只有「离开过再回来」才重跑自检）。
    var pausedOnce by remember { mutableStateOf(false) }
    // 桥接引导默认收起——它是给华为 / 荣耀用户的一条岔路，不该常驻版面。
    var bridgeOpen by remember { mutableStateOf(false) }

    // 「导入数据文件」的 SAF 选择器。**必须放在页面顶部**：LazyColumn 的 item
    // 会被回收，启动器的 remember 放在 item 里会在滚动之后丢掉回调。
    val importPicker = rememberImportPicker(backdrop, store)

    // 只给「这台机器上真实装到的 App」的同步路径（方案 §3.8）。
    // 桥接引导的触发条件也从这份名单里算，省掉第二次「已安装应用」全表扫描。
    // 22.26.0：**挪到 IO 线程**。以前这里 `remember { installedVendors(context) }`
    // 在首次组合里同步做整包扫描（`getInstalledApplications` + 逐包取应用名），
    // 就是「进数据与同步会卡一下」的直接原因；现在先出页面，名单随后填上
    //（null = 还没扫完，界面不提前说「没找到厂商 App」）。
    var vendors by remember { mutableStateOf<List<HealthConnectBridge.InstalledVendor>?>(null) }
    LaunchedEffect(Unit) {
        vendors = withContext(Dispatchers.IO) { HealthConnectBridge.installedVendors(context) }
    }
    val installedVendors = vendors
    val bridgeNeeded = installedVendors?.any {
        it.profile.family == "huawei" && it.profile.writesHealthConnect != true
    } == true

    /*
     * 真正申请读取权限的地方。
     *
     * 之前只有「打开 Health Connect 设置」，用户到了那边也未必会给本应用勾上；
     * 手环厂商的 App 更是没法替本应用授权——只有 Health Connect 自己弹出的
     * 那个授权框才算数。所以这里用官方契约直接把系统授权框拉起来，
     * 这是数据能不能读进来的关键一步。
     */
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        // 授权框一回来就重跑自检：②的状态、③的六类写入时间会跟着一起刷新，
        // 用户不需要再手动点一次「重新检查」。
        pausedOnce = false
        checkedAt = null
        refreshKey++
    }

    // ---- HC 写入桥（22.42.0）：把读数写入系统健康服务 ----
    // 开关打开时才弹系统授权框拿**写入**权限；拿到才置位开关，拿不到什么也不写。
    var bridgeNote by remember { mutableStateOf<String?>(null) }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val ok = HealthConnectWriter.writePermissions.all { it in granted }
        if (ok) {
            store.updatePrefs { p -> p.copy(bridgeToHc = true) }
            scope.launch {
                val wrote = HealthConnectWriter.writeFromStore(context, store, force = true)
                bridgeNote = when {
                    wrote.count > 0 -> "已写入 ${wrote.count} 条。"
                    wrote.skipped.contains("write-failed") -> "写入失败：先确认系统健康服务可用。"
                    else -> "没有可写的内容：先去同步一次读数。"
                }
            }
        } else {
            overlay.show(
                title = "没有拿到写入权限",
                subtitle = "开关没有打开，什么都没写。"
            ) {
                CastHint("系统授权框里要勾选允许写入的类型；也可以在「在系统设置里改」里补授权后再打开开关。")
            }
        }
    }

    /*
     * 自检的生命周期点（22.26.0）。
     *
     * 真机反馈：授权完回到应用，自检还是旧结论，刷新、杀进程都像是没生效。
     * 两个原因叠在一起：
     * 1. 只靠授权框的 result 回调触发重跑——回调在进程被杀重建后会丢，
     *    而部分国产 ROM 在 Health Connect 授权之后会顺手回收应用进程；
     * 2. 从「在系统设置里改」授权回来时**根本没有 result 回调**，什么都不重跑。
     * 所以这里再挂一个 ON_RESUME：从任何系统页面回来（授权框、系统设置）都重跑一次；
     * 首次进入不重复跑（LaunchedEffect 已经跑过了），正在跑也不叠跑。
     */
    DisposableEffect(Unit) {
        val owner = context as? android.app.Activity
        val observer = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(
                source: androidx.lifecycle.LifecycleOwner,
                event: androidx.lifecycle.Lifecycle.Event
            ) {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> pausedOnce = true
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                        if (pausedOnce && !diagnosing) {
                            pausedOnce = false
                            checkedAt = null
                            refreshKey++
                        }
                    else -> Unit
                }
            }
        }
        (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.addObserver(observer)
        onDispose {
            (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshKey) {
        // 一次自检就把「系统健康服务是否可用 / 授权到了哪几项 / 六类读数」
        // 全拿回来：页面进来只跑一趟 Health Connect，省掉两次重复调用。
        // 这些调用都在 IO 线程上跑（见 HealthConnectBridge.diagnose），
        // 不会卡住界面。
        diagnosing = true
        val d = HealthConnectBridge.diagnose(context)
        diagnosis = d
        checkedAt = System.currentTimeMillis()
        // ④ 的读数直接进首页用的 store：授权回来 / 杀进程重进之后数据自己接上，
        // 不用再点一次「手动同步」（原来只有手动同步会写这一笔）。
        store.updateSignals(d.signals)
        diagnosing = false
    }

    /*
     * 三个跳转各只有一条路径：主按钮、卡片里的按钮走的都是同一段代码。
     * - 「去授权」：拉系统授权框（只有它才算数）；
     * - 「手动同步」：跑一次四步自检 + 更新首页读数；
     * - 商店 / 厂商 App：跳不过去也要给一句人话，不能点了没反应。
     */
    fun openHealthServiceStore() {
        val opened = HealthConnectBridge.openFirstAvailable(
            context,
            HealthConnectBridge.healthConnectIntents(context, "Health Connect")
        )
        if (!opened) {
            overlay.show(
                title = "商店里没找到",
                subtitle = "可以换一个应用商店再试。"
            ) {
                CastHint("有些手机自带商店里没有 Health Connect，换 Play 商店试试。")
                CastHint("安卓 14 及以上系统一般自带系统健康服务，不需要另外装。")
            }
        }
    }

    /**
     * 主按钮在 ① 断时的动作：**先看再装**。
     *
     * 用户可能刚从商店回来、或者系统悄悄更新好了，直接再跳一次商店很蠢；
     * 所以先重跑一次可用性检查，确实还没有才去商店。
     */
    fun checkAndInstallHealthService() {
        scope.launch {
            val state = HealthConnectBridge.availability(context)
            if (state == HealthConnectBridge.Availability.AVAILABLE) refreshKey++ else openHealthServiceStore()
        }
    }

    /** ③ 没检测到厂商 App 时的商店入口：按应用名搜索，不绑定具体包名。 */
    fun openVendorStore() {
        val opened = HealthConnectBridge.openFirstAvailable(
            context,
            HealthConnectBridge.storeSearchIntents(BodyCastText.text("运动健康"))
        )
        if (!opened) {
            overlay.show(
                title = "商店里没搜到",
                subtitle = "可以换一个应用商店再试。"
            ) {
                CastHint("有些手机自带商店里没有这个应用，换 Play 商店或厂商商店试试。")
            }
        }
    }

    fun requestPermissions() {
        val state = diagnosis?.availability
        if (state == HealthConnectBridge.Availability.AVAILABLE) {
            // 直接拉起系统授权框：这才是真正能拿到数据的一步。
            runCatching { permissionLauncher.launch(HealthConnectBridge.permissions) }
                .onFailure {
                    overlay.show(
                        title = "打不开授权框",
                        subtitle = "先在系统里确认健康服务可用。"
                    ) {
                        CastHint("可以点下面的「在系统设置里改」，在那里手动勾选本应用。")
                    }
                }
        } else {
            overlay.show(
                title = "先装好系统健康服务",
                subtitle = "没有它就没有授权框可以弹。"
            ) {
                CastHint("安卓 14 及以上一般自带；安卓 13 及以下需要单独安装 Health Connect。")
                CastHint("装完回到这一页，点一次「重新检查」。")
            }
        }
    }

    /*
     * 手动同步：结果**拆成四种失败**，因为「没读到」有四种完全不同的原因，
     * 修法也完全不同（没装 / 没授权 / 厂商没写 / 应用读不到）。
     * 这一步会把自检结果整份刷新（含卡里那个「刚刚检查于」的时间），
     * 并把本次读数写进首页用的 store——用户点一次就够，不用再手动刷新。
     */
    fun runManualSync() {
        if (syncing) return
        syncing = true
        scope.launch {
            val d = HealthConnectBridge.diagnose(context)
            diagnosis = d
            checkedAt = System.currentTimeMillis()
            store.updateSignals(d.signals)
            syncing = false
            syncNote = when {
                d.signals.hasAnything -> "已经读到了最新数据。"
                d.availability != HealthConnectBridge.Availability.AVAILABLE ->
                    "这台机器上没有系统健康服务，这条路走不通。"
                d.grantedCount == 0 -> "还没有授权，先点「去授权」。"
                else -> "暂时没读到，可能是厂商还没把数据同步进系统。"
            }
            when {
                // ① 没装系统健康服务
                d.availability != HealthConnectBridge.Availability.AVAILABLE -> overlay.show(
                    title = "还没有系统健康服务",
                    subtitle = "手环数据要先汇总到它，BodyCast 才能读到。"
                ) {
                    CastHint("安卓 14 及以上一般自带系统健康服务；这台机器上没有，说明这条路在本机走不通。")
                    CastHint("不用一直试，下面三条都还能用：")
                    CastHint("· 手动校准：读完数自己看一眼，随时可用，优先级永远最高。")
                    CastHint("· 厂商 App：数据留在厂商自己家里，不往外写，应用拿不到。")
                    CastHint("· 第三方桥接：装一个桥接 App 把厂商数据转写进系统健康服务，见「让华为数据进得来」。")
                }
                // ② 没授权（含只授权了一部分）
                d.grantedCount == 0 -> overlay.show(
                    title = "还没有授权",
                    subtitle = "系统健康服务里没有给本应用任何读取权限。"
                ) {
                    CastHint("点「一键授权」，在弹出的系统框里勾上要允许的类型。")
                    CastHint("授权前产生的历史通常不会补写：授权完先在厂商 App 里同步一次。")
                }
                // ③ 本次读到了：列出每一条的来源与最后写入时间
                d.signals.hasAnything -> overlay.show(
                    title = "读到了 ${d.readCount} 类数据",
                    subtitle = "来源和最后写入时间都在下面。"
                ) {
                    d.reads.filter { it.ok }.forEach { read ->
                        GlassRow(
                            backdrop = backdrop,
                            title = read.type.label,
                            subtitle = if (read.estimated) "估算" else read.originName,
                            value = d.writes.firstOrNull { it.type == read.type }?.timeLabel
                                ?: "无记录",
                            chevron = false,
                            onClick = null,
                            surface = false
                        )
                    }
                    if (d.reads.any { it.estimated }) {
                        CastHint("标「估算」的那一条：厂商没写静息心率记录，应用用这 48 小时里最低一档的动态心率推的，不是厂商的原始数据。")
                    }
                    if (!d.allGranted) {
                        CastHint("还没授权：${d.missing.joinToString("、") { it.label }}")
                    }
                    CastHint("手环继续戴着，厂商 App 同步进新数据之后这里会自己更新。")
                }
                // ④ 有数据但这次读不到
                d.wroteAny -> overlay.show(
                    title = "有数据，但这次没读到",
                    subtitle = "系统健康服务里有记录（看自检第 ③ 步），应用这一次没读上来。"
                ) {
                    CastHint("先点一次「去授权」重新确认权限——有些系统会在重启后把健康权限清掉。")
                    CastHint("还不行就是应用侧的读取问题：麻烦把「数据流水线自检」这四步的状态记下来反馈。")
                }
                // ⑤ 已授权，但厂商没往系统里写数据
                else -> overlay.show(
                    title = "授权了，但厂商还没写数据",
                    subtitle = "权限没问题，是厂商 App 还没有把记录写进系统健康服务。"
                ) {
                    if (!d.allGranted) {
                        CastHint("还没授权：${d.missing.joinToString("、") { it.label }}")
                    }
                    CastHint("厂商 App 打开不等于数据已经写进 Health Connect。")
                    CastHint("厂商 App 通常只写新产生的数据，授权前的历史不会补写；先在厂商 App 里同步一次。")
                    if (!installedVendors.isNullOrEmpty()) {
                        CastHint("这台机器上装到的：")
                        CastStepList(HealthConnectBridge.vendorGuideFor(installedVendors, "any"))
                    } else if (installedVendors != null) {
                        CastHint("这台机器上没找到手环厂商的运动健康 App。")
                    }
                    CastHint("实在写不进来（华为 / 荣耀是已知情况），可以用第三方桥接，或直接用手动校准。")
                }
            }
        }
    }

    /** 主按钮的动作。四个跳转函数都在上面，这里只做分派。 */
    fun runMainAction(action: PipelineAction) {
        when (action) {
            PipelineAction.WAIT -> Unit
            PipelineAction.INSTALL_HEALTH -> checkAndInstallHealthService()
            PipelineAction.GRANT -> requestPermissions()
            PipelineAction.OPEN_VENDOR ->
                installedVendors?.firstOrNull()?.let { launchSafely(context, it.app.launch) }
            PipelineAction.INSTALL_VENDOR -> openVendorStore()
            PipelineAction.SYNC -> runManualSync()
        }
    }

    CastPage {
        item {
            CastPageHeader(
                title = "数据源管理",
                subtitle = "手环数据统一经过系统健康服务汇总到这里。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        // ---- 数据流水线自检（22.22.0；22.23.0 把整页收进这一张卡）----
        // 「读不到」有四种完全不同的原因。这张卡把四步分开显示，哪一步断了就在那一步
        // 给一条出路；判断全部来自 HealthConnectBridge.diagnose()，界面不自己拼结论。
        //
        // 卡顶三件事，从上到下就是用户的动线：一句结论（不用自己拼四步）→
        // 一个主按钮（下一步做什么，全页只有这一个）→ 次要的「重新检查」。
        // 原来并排摆着的「连接状态」「Health Connect 授权状态」「已授权数据类型」
        // 「去授权」「系统健康服务设置」「撤销授权」「手动同步」「连接小米/华为手环」
        // 九块独立行已经全部并进这张卡：说的是同一件事，分开摆只会把页面拉长、
        // 把「该点哪个」变成一道选择题。
        item {
            val d = diagnosis
            val unknown = HealthConnectBridge.StepState.UNKNOWN
            val ok = HealthConnectBridge.StepState.OK
            val vendorApp = installedVendors?.firstOrNull()

            // 结论：断在哪一步、还是四步全通（全通时把最后一次写入时间一并说出来）。
            val conclusion = when {
                d == null -> "正在检查…"
                d.systemState != ok -> "断在第 ① 步：这台机器上没有系统健康服务"
                !d.allGranted -> "断在第 ② 步：还没给应用授权"
                !d.wroteAny -> "断在第 ③ 步：厂商还没写数据"
                d.readCount == 0 -> "断在第 ④ 步：这次没读到数据"
                else -> "四步都通 · 数据会自动更新，最后一次 ${d.latestWrite?.timeLabel ?: ""}"
            }

            // 主按钮：按当前断在哪一步决定它是谁，全页只有这一个主操作。
            val mainAction = when {
                d == null -> PipelineAction.WAIT
                d.systemState != ok -> PipelineAction.INSTALL_HEALTH
                !d.allGranted -> PipelineAction.GRANT
                !d.wroteAny ->
                    if (vendorApp != null) PipelineAction.OPEN_VENDOR else PipelineAction.INSTALL_VENDOR
                else -> PipelineAction.SYNC
            }
            val mainLabel = when (mainAction) {
                PipelineAction.WAIT -> "正在检查…"
                PipelineAction.INSTALL_HEALTH -> "检查并安装健康服务"
                PipelineAction.GRANT -> "一键授权"
                PipelineAction.OPEN_VENDOR -> "打开${vendorApp?.app?.label.orEmpty()}"
                PipelineAction.INSTALL_VENDOR -> "安装厂商运动健康 App"
                PipelineAction.SYNC -> if (syncing) "正在读…" else "手动同步"
            }
            val mainIcon = when (mainAction) {
                PipelineAction.WAIT -> "scan"
                PipelineAction.INSTALL_HEALTH -> "down"
                PipelineAction.GRANT -> "shield"
                PipelineAction.OPEN_VENDOR -> "walk"
                PipelineAction.INSTALL_VENDOR -> "down"
                PipelineAction.SYNC -> "swap"
            }
            // 需要用户「跳出应用再回来」的那几种：把回来的第一步一并说清楚。
            val mainHint = when (mainAction) {
                PipelineAction.INSTALL_HEALTH -> "先装系统健康服务；装完回来点「重新检查」。"
                PipelineAction.OPEN_VENDOR -> "同步之后回来点「重新检查」。"
                PipelineAction.INSTALL_VENDOR -> "装上厂商 App 并登录手环账号，再回来点「重新检查」。"
                else -> null
            }
            val checkedLabel = checkedAt?.let { "刚刚检查于 " + hhmm(it) } ?: "正在检查…"

            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "数据流水线自检")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = conclusion,
                    fontSize = SkinTokens.body,
                    lineHeight = 26.sp,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(12.dp))

                CastPrimaryButton(
                    text = mainLabel,
                    icon = mainIcon,
                    enabled = mainAction != PipelineAction.WAIT && !(syncing && mainAction == PipelineAction.SYNC),
                    // 谷歌皮肤下「正在检查 / 正在读 / 重新检查跑着」都用 M3 转圈表达进度
                    //（玻璃皮肤按原样只靠文字，参数在玻璃分支被忽略）。
                    loading = mainAction == PipelineAction.WAIT || syncing || checkedAt == null,
                    onClick = { runMainAction(mainAction) },
                    backdrop = backdrop
                )
                if (mainHint != null) CastHint(mainHint)
                Spacer(Modifier.height(8.dp))
                CastSecondaryButton(
                    text = "重新检查",
                    icon = "reset",
                    // 把「刚刚检查于」清掉，检查过程中这里就是「正在检查…」——
                    // 点了有反应，用户不用猜是不是没点到。
                    onClick = {
                        checkedAt = null
                        refreshKey++
                    },
                    backdrop = backdrop
                )
                CastHint(checkedLabel)

                // ① 系统健康服务
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "① 系统健康服务",
                    state = d?.systemState ?: unknown,
                    detail = d?.availability?.label ?: "正在检查…"
                )
                if (d != null && d.systemState != ok) {
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        CastSecondaryButton(
                            text = "去应用商店装 / 更新",
                            onClick = { openHealthServiceStore() },
                            backdrop = backdrop
                        )
                    }
                    CastHint("安卓 14 及以上：系统一般自带「系统健康服务 / Health Connect」，不用额外安装。")
                }

                // ② 授权
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "② 授权",
                    state = d?.grantState ?: unknown,
                    detail = when {
                        d == null -> "正在检查…"
                        d.allGranted -> "六类都授权了"
                        d.grantedCount == 0 -> "还没有授权任何类型"
                        else -> "已授权 ${d.grantedCount} / ${HealthConnectBridge.DataType.entries.size} 项"
                    }
                )
                if (d != null && !d.allGranted) {
                    CastHint("还没授权：${d.missing.joinToString("、") { it.label }}")
                    // 主按钮在 ② 断时就是「一键授权」，这里不重复放第二个「去授权」；
                    // 只补一条主按钮做不到的次要入口（去系统里看 / 改授权）。
                    if (mainAction != PipelineAction.GRANT) {
                        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            CastSecondaryButton(
                                text = "去授权",
                                onClick = { requestPermissions() },
                                backdrop = backdrop
                            )
                        }
                    }
                }
                // 原来是「系统健康服务设置」「撤销授权」两行，并成这一条：
                // 撤销本来就只能在系统里做，两条入口说的是同一件事。
                if (d != null) {
                    GlassActionRow(
                        backdrop = backdrop,
                        title = "在系统设置里改",
                        icon = "settings",
                        subtitle = "看 / 改本应用的授权；取消授权也只能在这里做。",
                        surface = false,
                        shape = RoundedCornerShape(SkinTokens.secondaryRadius),
                        onClick = { launchSafely(context, HealthConnectBridge.settingsIntent()) }
                    )
                }

                // ③ 厂商有没有真的把记录写进系统健康服务（这一条最关键）
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "③ 厂商数据写入",
                    state = d?.writeState ?: unknown,
                    detail = when {
                        d == null -> "正在检查…"
                        // 一类都没授权时查不了写入记录——不能说成「厂商没写」。
                        d.grantedCount == 0 -> "还没授权，看不到写入时间"
                        d.wroteAny -> "最近 7 天有厂商写入的记录"
                        else -> "最近 7 天六类都没有记录"
                    }
                )

                // ---- 小米账号（云同步）：第 ③ 步里的第二条通路（22.27.0 起常驻）----
                // 这条路不经过系统健康服务：厂商不往 HC 写时它照样可能有数据。
                // 状态分四种如实显示：未连接 / 令牌失效要重新登录 / 连了还没同步 /
                // 已同步（新鲜或已过期），下一步按钮跟着状态走。
                val miCount = if (prefs.miCloudReadKeys.isNotEmpty()) {
                    prefs.miCloudReadKeys.size
                } else {
                    prefs.miCloudEntries.categoryCount
                }
                val miBroken = prefs.miCloudConnected && MiCloudSource.sessionOf(prefs) == null
                GlassRow(
                    backdrop = backdrop,
                    title = "小米账号（云同步）",
                    subtitle = when {
                        !prefs.miCloudConnected ->
                            "还没有连接小米账号。连上之后这里会显示上次同步时间和读到几类。"
                        miBroken -> "本机的令牌已失效，请重新登录。"
                        prefs.miCloudSyncedAt <= 0L -> "已连接，还没有同步过。"
                        prefs.miCloudFresh ->
                            "小米账号（云同步）· 上次同步 ${entryStamp(prefs.miCloudSyncedAt)} · 六类里读到 $miCount 类"
                        else ->
                            "小米账号（云同步）· 上次同步 ${entryStamp(prefs.miCloudSyncedAt)} · 已过期（超过 24 小时），不参与今天的判定。"
                    },
                    value = when {
                        !prefs.miCloudConnected -> "未连接"
                        miBroken -> "需要重新登录"
                        prefs.miCloudSyncedAt <= 0L -> "还没有同步过"
                        prefs.miCloudFresh -> "已连接"
                        else -> "已过期"
                    },
                    chevron = false,
                    onClick = null,
                    surface = false,
                    shape = RoundedCornerShape(SkinTokens.secondaryRadius)
                )
                if (prefs.miCloudConnected && !miBroken && prefs.miCloudFresh) {
                    CastHint("走的不是系统健康服务，所以第 ③ 步显示「没写入」时它照样可能有数据。")
                } else {
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        CastSecondaryButton(
                            text = when {
                                !prefs.miCloudConnected -> "去连接"
                                miBroken -> "重新登录"
                                else -> "去同步"
                            },
                            onClick = onOpenMiCloud,
                            backdrop = backdrop
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (d != null && !d.wroteAny) {
                    // 厂商快捷入口：显示这台机器上**真实检测到**的 App（按应用名扫出来的），
                    // 不是写死的名字；点「打开 X」就打开它。原来独立的「连接小米手环」
                    // 「连接华为手环」两行就并在这里。
                    if (vendorApp != null) {
                        GlassRow(
                            backdrop = backdrop,
                            title = "检测到：${installedVendors.orEmpty().joinToString("、") { it.app.label }}",
                            subtitle = "打开它同步一次；同步之后回来点「重新检查」。",
                            chevron = false,
                            onClick = null,
                            surface = false,
                            shape = RoundedCornerShape(SkinTokens.secondaryRadius)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    if (vendorApp != null && mainAction != PipelineAction.OPEN_VENDOR) {
                        Box(Modifier.fillMaxWidth()) {
                            CastSecondaryButton(
                                text = "打开${vendorApp.app.label}",
                                onClick = { launchSafely(context, vendorApp.app.launch) },
                                backdrop = backdrop
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // 名单还没扫完（null）时不说「没找到」——那是异步扫描在跑。
                    if (installedVendors != null && vendorApp == null) {
                        CastHint("这台机器上没找到手环厂商的运动健康 App。")
                        if (mainAction != PipelineAction.INSTALL_VENDOR) {
                            Box(Modifier.fillMaxWidth()) {
                                CastSecondaryButton(
                                    text = "去商店找厂商 App",
                                    onClick = { openVendorStore() },
                                    backdrop = backdrop
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth()) {
                        CastSecondaryButton(
                            text = "看厂商同步路径",
                            onClick = {
                                showVendorGuide(
                                    overlay = overlay,
                                    backdrop = backdrop,
                                    context = context,
                                    title = "厂商同步路径",
                                    family = "any",
                                    vendors = installedVendors.orEmpty()
                                )
                            },
                            backdrop = backdrop
                        )
                    }
                    if (bridgeNeeded) CastHint("华为 / 荣耀不往系统健康服务写数据，是已知情况——下面那张卡有办法。")
                }

                // ③ 的明细：已授权的每一类各自的最后写入时间与来源。
                // 就是原来那张独立的「已授权数据类型」列表，搬进 ③ 里。
                // 22.26.0 起**授权了几项就列几项**（没授权的行不占版面，② 里已经点了名），
                // 不再要求六类全齐才显示——授权完就能看到厂商最后写入时间。
                if (d != null && d.grantedCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    CastHint("六类各自的最后写入时间与来源：")
                    d.writes.filter { it.permitted }.forEach { write ->
                        GlassRow(
                            backdrop = backdrop,
                            title = write.type.label,
                            subtitle = write.originName,
                            value = write.timeLabel,
                            chevron = false,
                            onClick = null,
                            surface = false
                        )
                    }
                    CastHint("右边是这一类最后一次写进系统健康服务的时间；「无记录」= 最近 7 天厂商没有写。")
                }

                // ④ 本次读数
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "④ 本次读数",
                    state = d?.readState ?: unknown,
                    detail = when {
                        d == null -> "正在检查…"
                        d.readCount > 0 -> "读到了 ${d.readCount} 类数据"
                        else -> "这次一类都没读到"
                    }
                )
                // 主按钮在 ④ 断时就是「手动同步」，这里不重复放第二个。
                if (d != null && d.readCount == 0 && mainAction != PipelineAction.SYNC) {
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        CastSecondaryButton(
                            text = "手动同步",
                            onClick = { runManualSync() },
                            backdrop = backdrop
                        )
                    }
                }
                // 手动同步的结果留一句在这个位置（原来挂在「手动同步」那一行上）。
                syncNote?.let { CastHint(it) }
            }
        }

        // ---- 数据连接指导（22.48.0）：把「第一步该做什么」单独讲一遍 ----
        // 与「授权向导」互补：向导讲手环 ↔ 厂商 App ↔ 系统这一整条链怎么接；
        // 这一页只讲三步——授权系统健康服务、连接小米账号、确认数据真的进来了，
        // 而且页内直接内嵌小米账号那一整块与同步数据明细，不用来回跳。
        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "连接运动数据",
                icon = "swap",
                subtitle = "三步指导：授权健康服务、连接小米账号、看数据有没有真的进来。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { BodyCastNav.openDataGuide() }
            )
        }

        // ---- 授权向导（22.30.0）----
        // 自检卡告诉你「断在哪」，向导告诉你「一步步怎么接」：厂商 App 里开写入开关、
        // 授权、验证，每一步带跳转按钮。第一次接入手环的用户从这里走最顺。
        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "授权向导",
                icon = "shield",
                subtitle = "第一次接入手环数据：一步步带你从厂商 App 接到本应用。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = onOpenSetupGuide
            )
        }

        // ----「上次读取」+ 过期提示（22.27.0）----
        // 和首页同一条纪律：超过 30 分钟没读就提示「数据可能过期」，
        // 点这一行走「重新检查」同一条路（重跑自检 + 刷新首页读数）。
        item {
            val at = store.prefs.lastFetchAt
            val stale = FetchFreshness.isStale(at)
            GlassRow(
                backdrop = backdrop,
                title = if (at <= 0L) "还没有读取过" else "上次读取：" + entryStamp(at),
                icon = "clock",
                value = if (stale) "数据可能过期，点此刷新" else "点一下可以重新检查。",
                chevron = false,
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    checkedAt = null
                    refreshKey++
                }
            )
        }

        // ---- 手动填写读数 / 导入数据文件（22.24.0，方案 B §3.1–§3.3）----
        // 这两条是零权限、零依赖的兜底：Health Connect 那条路走不通（厂商不往里写、
        // 没装健康服务、在海外机器上）时，用户仍然能自己把读数交给应用。
        item {
            CastCard(
                backdrop = backdrop,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                GlassActionRow(
                    backdrop = backdrop,
                    title = "手动填写读数",
                    icon = "type",
                    subtitle = "照着手环 App 里的数字抄进来，24 小时内有效。",
                    value = manualEntryStatus(prefs),
                    surface = false,
                    onClick = onOpenManualEntry
                )
                CastDivider()
                GlassActionRow(
                    backdrop = backdrop,
                    title = "导入数据文件",
                    icon = "down",
                    subtitle = "厂商 App 导出的 CSV / JSON；也可以直接从分享面板发给本应用。",
                    value = importStatus(prefs),
                    surface = false,
                    onClick = { importPicker.launch(arrayOf("text/*", "application/*")) }
                )
                CastDivider()
                // 小米账号（云同步）：默认关闭的可选通道。只有用户主动连接之后才会联网，
                // 所以这一行走的是「手动填写 / 导入」旁边，而不是任何默认路径上。
                GlassActionRow(
                    backdrop = backdrop,
                    title = "小米账号（云同步）",
                    icon = "shield",
                    subtitle = "可选：连接你自己的小米账号，把云端数据取回来。默认不联网。",
                    value = miCloudStatus(prefs),
                    surface = false,
                    onClick = { onOpenMiCloud() }
                )
            }
        }

        item {
            CastHint("这两条都不需要授权、不需要装任何东西，任何手机都能用。")
            CastHint("只按表头认列；手动填写、导入与小米云快照的读数 24 小时后过期。有效期内手动填的读数优先于小米云 / 导入 / 设备读数。")
        }

        // ---- 把读数写入系统健康服务（HC 写入桥，22.42.0）----
        // 方向和上面相反：读数已经同步到本机之后，把它写进系统健康服务——
        // 这条通道从此是活的，其他健康应用也能看到同一份数据。
        // 默认关闭；打开开关会先弹系统授权框，拿到写入权限才开始写。
        item {
            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "写入系统健康服务")
                Spacer(Modifier.height(4.dp))
                GlassToggleRow(
                    backdrop = backdrop,
                    title = "把读数写入系统健康服务",
                    checked = prefs.bridgeToHc,
                    subtitle = "打开后，本机已同步的读数会写进 Health Connect，来源标注为本应用。睡眠与运动只在有真实起止时间时写入。",
                    shape = RoundedCornerShape(SkinTokens.secondaryRadius),
                    onCheckedChange = { on ->
                        if (on) {
                            runCatching {
                                writePermissionLauncher.launch(HealthConnectWriter.writePermissions)
                            }.onFailure {
                                overlay.show(
                                    title = "打不开授权框",
                                    subtitle = "先在系统里确认健康服务可用。"
                                ) {
                                    CastHint("可以在「在系统设置里改」里手动勾选写入权限后再打开。")
                                }
                            }
                        } else {
                            store.updatePrefs { p -> p.copy(bridgeToHc = false) }
                            bridgeNote = "已关闭，不会再写入。"
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                CastSecondaryButton(
                    text = "立即写入一次",
                    icon = "swap",
                    onClick = {
                        scope.launch {
                            val wrote = HealthConnectWriter.writeFromStore(context, store, force = true)
                            bridgeNote = when {
                                wrote.count > 0 -> "已写入 ${wrote.count} 条。"
                                wrote.skipped.contains("write-failed") -> "写入失败：先打开上面的开关并授权。"
                                else -> "没有可写的内容：先去同步一次读数。"
                            }
                        }
                    },
                    backdrop = backdrop
                )
                bridgeNote?.let { CastHint(it) }
                if (prefs.hcWriteAt > 0L) {
                    CastHint("上次写入：" + entryStamp(prefs.hcWriteAt) + " · " + prefs.hcWriteCount + " 条")
                }
                CastHint("写的是本机已同步的聚合读数；睡眠与运动没有真实起止时间就跳过，不编造。")
            }
        }

        // ---- 桥接引导：只在「健康服务就绪 + 六类已授权 + 第③步六类全无 + 装了华为/荣耀」时露出。
        // 条件与 22.22.0 一致，位置挪到自检卡**之后**（③ 里那句话指的就是这张卡）、
        // 默认收起：它是给华为 / 荣耀用户的一条岔路，不该常驻版面。
        // 应用不集成任何第三方 SDK，只把这条现实存在的路讲清楚。
        val needsBridge = diagnosis?.let { d ->
            d.systemState == HealthConnectBridge.StepState.OK &&
                d.allGranted && !d.wroteAny && bridgeNeeded
        } ?: false
        if (needsBridge) {
            item {
                CastCard(backdrop = backdrop) {
                    GlassActionRow(
                        backdrop = backdrop,
                        title = "让华为数据进得来",
                        icon = "swap",
                        subtitle = if (bridgeOpen) {
                            "点一下收起。"
                        } else {
                            "华为运动健康不往系统健康服务写数据，这里有一条现实的路。"
                        },
                        shape = RoundedCornerShape(SkinTokens.cardRadius),
                        onClick = { bridgeOpen = !bridgeOpen }
                    )
                    if (bridgeOpen) {
                        Spacer(Modifier.height(6.dp))
                        CastStepList(HealthConnectBridge.bridgeGuide())
                    }
                }
            }
        }

        // 「连接状态」「连接小米手环」「连接华为手环」「Health Connect 授权状态」
        // 「已授权数据类型」「去授权」「系统健康服务设置」「撤销授权」「手动同步」
        // 九块独立行在这里**整段撤掉**（22.23.0）：信息与操作全部并进上面的自检卡。
        item {
            val sourceKeys = listOf("health_connect", "manual_entry", "import", "manual", "demo")
            val sourceOptions = listOf("Health Connect", "手动填写读数", "导入数据", "手动校准", "演示数据")
            GlassSelectRow(
                backdrop = backdrop,
                title = "数据来源优先级",
                value = BodyCastText.text(sourceOptions[sourceKeys.indexOf(prefs.dataSourcePriority).coerceAtLeast(0)]),
                subtitle = "优先用哪一路数据来定今天的天气。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = "数据来源优先级",
                        options = sourceOptions.map(BodyCastText::text),
                        selectedIndex = indexOrFirst(sourceKeys, prefs.dataSourcePriority)
                    ) { index ->
                        store.updatePrefs { it.copy(dataSourcePriority = sourceKeys[index]) }
                    }
                }
            )
        }

        item { CastHint("手动校准的优先级永远高于系统自动判断。") }
        item { CastHint("手动填写的读数也一样：只要没超过 24 小时，它填的那几项就以它为准。") }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "模拟数据模式",
                checked = prefs.demoMode,
                subtitle = "用来演示界面效果，不是你的真实状态。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on ->
                    store.updatePrefs { it.copy(demoMode = on) }
                    if (on) store.useDemoData() else store.clearSignals()
                }
            )
        }

        // 同步状态跟数据走，所以放在数据源管理里（原本在「账户」组，
        // 那组整体已删除——本应用没有服务器，不存在账号体系）。
        item {
            val syncLabel = if (prefs.lastFetchAt <= 0L) {
                "还没有同步过"
            } else {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(prefs.lastFetchAt))
            }
            GlassRow(
                backdrop = backdrop,
                title = "同步状态",
                icon = "clock",
                value = syncLabel,
                chevron = false,
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = null
            )
        }

        // 数据到底是谁写进系统健康服务的——这一行让用户能自己核对。
        // 同时装了小米和华为、或者数据其实是别的 App 写的，光看「已授权」看不出来。
        item {
            val origins = store.signals.origins
            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "数据来源")
                if (origins.isEmpty()) {
                    CastHint("这次什么都没读到。授权之后点上面的「手动同步」，这里会显示出数据是哪个 App 写进系统健康服务的。")
                } else {
                    CastHint("本次读到的记录来自：")
                    origins.forEach { origin ->
                        CastHint("· " + HealthConnectBridge.originLabel(origin) + "（" + origin + "）")
                    }
                    CastHint("如果这里显示的不是你手环对应的那个 App，说明系统里还有别的应用在写同类数据——以你手动校准的结论为准。")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 1.5：授权向导（22.30.0）
// ---------------------------------------------------------------------------

/**
 * 授权向导：手环数据从零接进来的分步指南。
 *
 * 自检卡回答「现在断在哪」，这里回答「一步步怎么接」。四步对应数据流水线：
 * ① 系统健康服务 → ② 给应用授权 → ③ 厂商 App 写数据（**最常被漏掉的一步**：
 * 打开厂商 App 不等于数据会写进 Health Connect，要在它自己的设置里打开
 * 「数据共享 / Health Connect」开关）→ ④ 回来验证。
 *
 * 第 ③ 步还会探测厂商 APK 的写入能力（[HealthConnectBridge.declaresHealthConnectWrite]）：
 * 声明了写入权限 = 开关问题；一项没声明 = 这条路在它身上走不通（华为/荣耀国行），
 * 直接指去桥接或手动，不让人在死路上反复试。
 *
 * 每步都带跳转按钮（打开厂商 App / 拉授权框 / 同步），跳出去之前把回来的第一步
 * 一并说清楚——这是真机反馈里「授权完不知道该干嘛」的直接解法。
 */
@Composable
private fun SetupGuidePage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current
    val scope = rememberCoroutineScope()

    // 自检结论（异步；null = 正在检查）。页面用它把「已通的步骤」标绿。
    var diagnosis by remember { mutableStateOf<HealthConnectBridge.Diagnosis?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var pausedOnce by remember { mutableStateOf(false) }

    // 厂商 App 名单（IO 扫描）与各自的写入能力探测结果。
    var vendors by remember { mutableStateOf<List<HealthConnectBridge.InstalledVendor>?>(null) }
    var writeSupport by remember { mutableStateOf<Map<String, Boolean?>>(emptyMap()) }
    LaunchedEffect(Unit) {
        val found = withContext(Dispatchers.IO) { HealthConnectBridge.installedVendors(context) }
        vendors = found
        // 探测每个已装厂商 App 的清单：声明了 WRITE_* 才会往 Health Connect 写。
        writeSupport = withContext(Dispatchers.IO) {
            found.associate { it.app.packageName to HealthConnectBridge.declaresHealthConnectWrite(context, it.app.packageName) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        pausedOnce = false
        refreshKey++
    }

    // 从系统页面回来（授权框、厂商 App、系统设置）自动重跑——和数据源页同一套生命周期纪律。
    DisposableEffect(Unit) {
        val owner = context as? android.app.Activity
        val observer = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(
                source: androidx.lifecycle.LifecycleOwner,
                event: androidx.lifecycle.Lifecycle.Event
            ) {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> pausedOnce = true
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                        if (pausedOnce) {
                            pausedOnce = false
                            refreshKey++
                        }
                    else -> Unit
                }
            }
        }
        (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.addObserver(observer)
        onDispose {
            (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshKey) {
        diagnosis = HealthConnectBridge.diagnose(context)
        store.updateSignals(diagnosis!!.signals)
    }

    fun requestPermissions() {
        runCatching { permissionLauncher.launch(HealthConnectBridge.permissions) }
            .onFailure {
                overlay.show(
                    title = "打不开授权框",
                    subtitle = "先在系统里确认健康服务可用。"
                ) {
                    CastHint("可以先做第 ① 步，装好系统健康服务再回来。")
                }
            }
    }

    val d = diagnosis
    val ok = HealthConnectBridge.StepState.OK
    val vendorApps = vendors.orEmpty()

    CastPage {
        item {
            CastPageHeader(
                title = "授权向导",
                subtitle = "手环数据的完整接法：厂商 App → 系统健康服务 → BodyCast。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        // ---- 总览：四步进度 ----
        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = when {
                        d == null -> "正在检查你走到哪一步了…"
                        d.readCount > 0 -> "已经通了：数据在自动进来。"
                        d.systemState != ok -> "从第 ① 步开始。"
                        d.grantState != ok -> "第 ① 步已通，做第 ② 步。"
                        d.writeState != ok -> "前两步已通，做第 ③ 步（最容易漏的一步）。"
                        else -> "前三步已通，做第 ④ 步验证。"
                    },
                    fontSize = SkinTokens.body,
                    lineHeight = 26.sp,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(10.dp))
                CastHint("链路是：手环 → 厂商运动健康 App → 系统健康服务（Health Connect）→ BodyCast。任何一环断了，数据都进不来。")
            }
        }

        // ---- ⓪ 配对手环（一条龙的起点，22.34.0）----
        // 用户反馈「不知道如何配对」：以前的向导从厂商 App 开始讲，
        // 但真正的起点是手环 ↔ 厂商 App 的蓝牙配对。这里按厂商家族给具体步骤。
        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = "⓪ 先把手环连到厂商 App",
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(6.dp))
                CastHint("这是整条链的起点：手环的数据先到厂商的运动健康 App，才轮得到后面几步。手环和 BodyCast 之间没有直连。")
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "小米 / 红米手环",
                    fontSize = SkinTokens.caption,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(4.dp))
                CastStepList(
                    listOf(
                        "装好「小米运动健康」，登录你的小米账号。",
                        "在「我的」页或首页顶部找到「添加设备」。",
                        "扫包装盒上的二维码，或在列表里选你的手环型号。",
                        "手环屏幕会弹出配对请求，在手环上点确认。",
                        "回到 App 首页，能看到今天的步数或睡眠就是配对成功。"
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "华为 / 荣耀手环",
                    fontSize = SkinTokens.caption,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(4.dp))
                CastStepList(
                    listOf(
                        "装好「华为运动健康」或「荣耀运动健康」，登录账号。",
                        "底部「设备」页 →「添加设备」。",
                        "扫一扫或选型号，在手环上点确认。",
                        "同步一次，首页能看到今天的数据就是配对成功。"
                    )
                )
                Spacer(Modifier.height(10.dp))
                CastHint("判断标准只有一个：打开厂商 App，能看到今天的数据。")
                if (vendorApps.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    CastHint("这台机器上找到了：" + vendorApps.joinToString("、") { it.app.label } + "。")
                }
            }
        }

        // ---- ⓪+ 小米手环的省事路（云同步不依赖厂商写入）----
        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = "用小米手环的话，还有条更省事的路",
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(6.dp))
                CastHint("「小米账号（云同步）」直接从云端取数，不依赖厂商 App 写系统健康服务。只要手环用小米运动健康同步过一次，数据就到了小米的云端。")
                Spacer(Modifier.height(8.dp))
                CastSecondaryButton(
                    text = "去看小米账号（云同步）",
                    icon = "swap",
                    onClick = {
                        overlay.show(
                            title = "小米账号（云同步）",
                            subtitle = "在「设置 → 数据与同步」里，就在这一页下面。"
                        ) {
                            CastHint("连接后点「立即同步」，读到的六类数据和 Health Connect 那条路是一样的。两条路可以并存，谁的数据新用谁。")
                        }
                    },
                    backdrop = backdrop
                )
            }
        }

        // ---- ① 系统健康服务 ----
        item {
            CastCard(backdrop = backdrop) {
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "① 系统健康服务",
                    state = d?.systemState ?: HealthConnectBridge.StepState.UNKNOWN,
                    detail = d?.availability?.label ?: "正在检查…"
                )
                Spacer(Modifier.height(8.dp))
                CastHint("安卓 14 及以上系统自带；安卓 13 及以下要在应用商店装一个 Health Connect。")
                if (d?.systemState != ok) {
                    Spacer(Modifier.height(8.dp))
                    CastPrimaryButton(
                        text = "去应用商店装 / 更新",
                        icon = "down",
                        onClick = {
                            val opened = HealthConnectBridge.openFirstAvailable(
                                context,
                                HealthConnectBridge.healthConnectIntents(context, "Health Connect")
                            )
                            if (!opened) {
                                overlay.show(
                                    title = "商店里没找到",
                                    subtitle = "可以换一个应用商店再试。"
                                ) {
                                    CastHint("有些手机自带商店里没有 Health Connect，换 Play 商店试试。")
                                }
                            }
                        },
                        backdrop = backdrop
                    )
                    CastHint("装完回到这一页，下一步会自动亮起来。")
                }
            }
        }

        // ---- ② 给应用授权 ----
        item {
            CastCard(backdrop = backdrop) {
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "② 给 BodyCast 授权",
                    state = d?.grantState ?: HealthConnectBridge.StepState.UNKNOWN,
                    detail = when {
                        d == null -> "正在检查…"
                        d.allGranted -> "六类都授权了"
                        d.grantedCount == 0 -> "还没有授权任何类型"
                        else -> "已授权 ${d.grantedCount} / ${HealthConnectBridge.DataType.entries.size} 项"
                    }
                )
                Spacer(Modifier.height(8.dp))
                CastHint("在弹出的系统框里把要读的类型都勾上。只读取，不写入。")
                if (d != null && !d.allGranted) {
                    CastHint("还没授权：" + d.missing.joinToString("、") { it.label })
                    Spacer(Modifier.height(8.dp))
                    CastPrimaryButton(
                        text = "一键授权",
                        icon = "shield",
                        onClick = { requestPermissions() },
                        backdrop = backdrop
                    )
                    CastHint("授权框只有系统健康服务能弹出来——厂商 App 替不了这一步。")
                }
            }
        }

        // ---- ③ 厂商 App 写数据（最容易漏的一步）----
        item {
            CastCard(backdrop = backdrop) {
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "③ 厂商 App 写数据",
                    state = d?.writeState ?: HealthConnectBridge.StepState.UNKNOWN,
                    detail = when {
                        d == null -> "正在检查…"
                        d.wroteAny -> "最近 7 天有厂商写入的记录"
                        else -> "最近 7 天没有厂商写入"
                    }
                )
                Spacer(Modifier.height(8.dp))
                CastHint("打开厂商 App 不等于数据进来了——要去它自己的设置里打开「数据共享 / Health Connect」开关，数据才会写进系统健康服务。")

                if (vendorApps.isEmpty() && vendors != null) {
                    CastHint("这台机器上没找到手环厂商的运动健康 App。先装上它、登录手环账号、确认数据在它里面能看到，再回来。")
                    Spacer(Modifier.height(8.dp))
                    CastSecondaryButton(
                        text = "去商店找厂商 App",
                        icon = "down",
                        onClick = {
                            HealthConnectBridge.openFirstAvailable(
                                context,
                                HealthConnectBridge.storeSearchIntents(BodyCastText.text("运动健康"))
                            )
                        },
                        backdrop = backdrop
                    )
                }

                // 每个已装厂商 App 一张小卡：写入能力结论 + 它自己的开关路径 + 跳转按钮。
                vendorApps.forEach { vendor ->
                    val support = writeSupport[vendor.app.packageName]
                    Spacer(Modifier.height(10.dp))
                    GlassRow(
                        backdrop = backdrop,
                        title = vendor.app.label,
                        subtitle = when (support) {
                            true -> "支持写入系统健康服务——去它设置里把开关打开"
                            false -> "这个版本不写系统健康服务——看下面的替代方案"
                            null -> "没查到它的写入能力（不影响使用）"
                        },
                        chevron = false,
                        onClick = null,
                        surface = false,
                        shape = RoundedCornerShape(SkinTokens.secondaryRadius)
                    )
                    CastHint(vendor.profile.path)
                    if (support == false) CastHint(vendor.profile.note)
                    Spacer(Modifier.height(6.dp))
                    CastSecondaryButton(
                        text = "打开${vendor.app.label}",
                        icon = "walk",
                        onClick = { launchSafely(context, vendor.app.launch) },
                        backdrop = backdrop
                    )
                    CastHint("同步一次手环再回来——厂商一般只写新产生的数据，授权前的历史不补写。")
                }

                if (vendorApps.any { writeSupport[it.app.packageName] == false }) {
                    Spacer(Modifier.height(8.dp))
                    CastHint("不写系统健康服务的厂商（华为 / 荣耀是已知情况），有两条替代路：")
                    CastStepList(HealthConnectBridge.bridgeGuide())
                }
            }
        }

        // ---- ④ 回来验证 ----
        item {
            CastCard(backdrop = backdrop) {
                PipelineStepRow(
                    backdrop = backdrop,
                    title = "④ 回来验证",
                    state = d?.readState ?: HealthConnectBridge.StepState.UNKNOWN,
                    detail = when {
                        d == null -> "正在检查…"
                        d.readCount > 0 -> "读到了 ${d.readCount} 类数据"
                        else -> "这次一类都没读到"
                    }
                )
                Spacer(Modifier.height(8.dp))
                CastHint("从厂商 App 同步完回来，点下面的按钮看结果。每类读数的来源和最后写入时间都会列出来。")
                Spacer(Modifier.height(8.dp))
                CastPrimaryButton(
                    text = "同步并查看结果",
                    icon = "swap",
                    onClick = {
                        scope.launch {
                            val result = HealthConnectBridge.diagnose(context)
                            diagnosis = result
                            store.updateSignals(result.signals)
                            val title = when {
                                result.signals.hasAnything -> BodyCastText.text("读到了 ${result.readCount} 类数据")
                                result.availability != HealthConnectBridge.Availability.AVAILABLE ->
                                    BodyCastText.text("这台机器上没有系统健康服务")
                                result.grantedCount == 0 -> BodyCastText.text("还没有授权")
                                !result.wroteAny -> BodyCastText.text("厂商还没把数据写进来")
                                else -> BodyCastText.text("有数据，但这次没读到")
                            }
                            overlay.show(
                                title = title,
                                subtitle = BodyCastText.text("详细状态都在下面，也写回了自检卡。")
                            ) {
                                if (result.signals.hasAnything) {
                                    result.reads.filter { it.ok }.forEach { read ->
                                        CastHint(
                                            "· " + read.type.label + "：" +
                                                (if (read.estimated) "估算" else (read.originName ?: "")) +
                                                "（" + (result.writes.firstOrNull { it.type == read.type }?.timeLabel ?: "无记录") + "）"
                                        )
                                    }
                                    CastHint("数据会随厂商写入自动更新，不用再来回点。")
                                } else if (!result.wroteAny && result.grantedCount > 0) {
                                    CastHint("第 ③ 步还没通：回上面看看厂商 App 的开关路径。")
                                } else {
                                    CastHint("按上面四步走一遍，每一步的状态都会自动刷新。")
                                }
                            }
                        }
                    },
                    backdrop = backdrop
                )
            }
        }

        // ---- 兜底：任何一步走不通都还能用的三条路 ----
        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = "走不通也没关系",
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(6.dp))
                CastHint("厂商不配合、机器太老、不想授权——任何一步卡住，下面三条路都不需要 Health Connect：")
                CastHint("· 手动填写读数：照手环 App 里的数字抄进来，24 小时内有效。")
                CastHint("· 导入数据文件：厂商导出的 CSV / JSON，或从分享面板发给本应用。")
                CastHint("· 手动校准：勾选现在的感觉就够了，优先级永远高于设备读数。")
            }
        }

        item {
            CastHint("这条链路的每一步都在本机完成。应用不上传任何数据，也不保存原始生理数值。")
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 2：身体天气校准
// ---------------------------------------------------------------------------

@Composable
private fun CalibrationPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs


    CastPage {
        item {
            CastPageHeader(
                title = "身体天气校准",
                subtitle = "读不到数据时怎么办、你能手动告诉它什么。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            val weatherKeys = listOf("auto") + WeatherKind.entries.map { it.name }
            val weatherOptions = listOf(BodyCastText.text("跟随读数")) + WeatherKind.entries.map { it.label }
            val weatherIndex = indexOrFirst(weatherKeys, prefs.defaultWeather)
            GlassSelectRow(
                backdrop = backdrop,
                title = "默认天气类型",
                value = weatherOptions[weatherIndex],
                subtitle = "读不到数据时先按这个显示。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = "默认天气类型",
                        options = weatherOptions,
                        selectedIndex = weatherIndex
                    ) { index ->
                        store.updatePrefs { it.copy(defaultWeather = weatherKeys[index]) }
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "校准标签自定义",
                icon = "type",
                subtitle = "关掉的标签不会出现在首页上。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { showCalibrationEditor(overlay, backdrop, store) }
            )
        }

        item {
            SettingBlock(title = "敏感度", backdrop = backdrop) {
                GlassSegmented(
                    options = listOf("低", "中", "高"),
                    selectedIndex = prefs.sensitivity.coerceIn(0, 2),
                    onSelect = { index -> store.updatePrefs { it.copy(sensitivity = index) } },
                    backdrop = backdrop
                )
            }
        }

        item { CastHint("敏感度越高，天气越容易跟着读数变化。") }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "重置校准",
                icon = "reset",
                subtitle = "把今天的校准标签和自定义标签都清掉。",
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.confirm(
                        title = "重置校准",
                        message = "会清掉今天勾过的感觉标签，也会清掉你自己加的标签。天气会回到按读数判断。",
                        confirmLabel = "重置",
                        cancelLabel = "先不要"
                    ) {
                        store.clearAllCalibration()
                        store.updatePrefs {
                            it.copy(hiddenCalibration = emptySet(), customCalibrationLabels = emptyList())
                        }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 3：微行动处方偏好
// ---------------------------------------------------------------------------

@Composable
private fun PrescriptionPrefsPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val prefs = store.prefs


    CastPage {
        item {
            CastPageHeader(
                title = "微行动处方偏好",
                subtitle = "处方多长、在什么场景做、要不要声音。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            val minuteLabels = listOf("1 分钟", "3 分钟", "5 分钟", "不限")
            val minuteValues = listOf(1, 3, 5, 10)
            SettingBlock(title = "偏好时长", backdrop = backdrop) {
                GlassSegmented(
                    options = minuteLabels,
                    selectedIndex = minuteValues.indexOf(prefs.maxMinutes).let { if (it < 0) 2 else it },
                    onSelect = { index -> store.updatePrefs { it.copy(maxMinutes = minuteValues[index]) } },
                    backdrop = backdrop
                )
            }
        }

        item {
            val scenes = listOf(
                "desk" to "工位", "transit" to "地铁", "bed" to "床上",
                "balcony" to "阳台", "outdoors" to "户外"
            )
            SettingBlock(title = "可用场景", backdrop = backdrop) {
                GlassChipFlow(
                    items = scenes,
                    backdrop = backdrop,
                    label = { BodyCastText.text(it.second) },
                    isSelected = { it.first in prefs.preferredScenes },
                    onToggle = { scene ->
                        store.updatePrefs { p ->
                            p.copy(
                                preferredScenes = if (scene.first in p.preferredScenes) {
                                    p.preferredScenes - scene.first
                                } else {
                                    p.preferredScenes + scene.first
                                }
                            )
                        }
                    }
                )
                if (prefs.preferredScenes.isEmpty()) {
                    CastHint("至少留一个场景，不然处方没法生成。")
                }
            }
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "语音引导",
                checked = prefs.voiceGuide,
                subtitle = "做微行动的时候有声音带着你。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(voiceGuide = on) } }
            )
        }

        // 22.28.0：「背景音默认」选择行已隐藏——本版本没有任何音频资源文件，
        // 「能点但不响」是半成品。偏好字段（defaultSound / backgroundSound）原样
        // 保留，将来接上音频资源后把这一行加回来即可（README 已注明）。

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "完成反馈后自动调整",
                checked = prefs.autoAdjustAfterFeedback,
                subtitle = "说「更累了」下次就会更短更温和。",
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(autoAdjustAfterFeedback = on) } }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 4：食谱指南
// ---------------------------------------------------------------------------

@Composable
private fun RecipePrefsPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs


    CastPage {
        item {
            CastPageHeader(
                title = "食谱指南",
                subtitle = "吃什么、不吃什么，由你定。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            val diets = listOf(
                "vegetarian" to "素食", "dairy_free" to "不吃乳制品",
                "seafood_free" to "不吃海鲜", "low_sugar" to "低糖"
            )
            SettingBlock(title = "饮食偏好", backdrop = backdrop) {
                GlassChipFlow(
                    items = diets,
                    backdrop = backdrop,
                    label = { BodyCastText.text(it.second) },
                    isSelected = { it.first in prefs.dietPreferences },
                    onToggle = { diet ->
                        store.updatePrefs { p ->
                            p.copy(
                                dietPreferences = if (diet.first in p.dietPreferences) {
                                    p.dietPreferences - diet.first
                                } else {
                                    p.dietPreferences + diet.first
                                }
                            )
                        }
                    }
                )
            }
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "过敏原提醒",
                checked = prefs.allergenAlert,
                subtitle = "在食谱里把可能踩雷的食材标出来。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(allergenAlert = on) } }
            )
        }

        item {
            val scopeKeys = listOf("all", "nutrient", "benefit")
            val scopeOptions = listOf("全部食物", "只按营养素", "只按功效")
            val scopeIndex = indexOrFirst(scopeKeys, prefs.recipeScope)
            GlassSelectRow(
                backdrop = backdrop,
                title = "默认搜索范围",
                value = BodyCastText.text(scopeOptions[scopeIndex]),
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = "默认搜索范围",
                        options = scopeOptions.map(BodyCastText::text),
                        selectedIndex = scopeIndex
                    ) { index ->
                        store.updatePrefs { it.copy(recipeScope = scopeKeys[index]) }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 5：通知与提醒
// ---------------------------------------------------------------------------

@Composable
private fun NotifyPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = store.prefs


    CastPage {
        item {
            CastPageHeader(
                title = "通知与提醒",
                subtitle = "到点时轻轻提醒一次，不催促、不带数字。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "每日倾听提醒",
                checked = prefs.handshakeReminder,
                subtitle = "每天提醒你回来看看今天的身体天气。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on ->
                    store.updatePrefs { it.copy(handshakeReminder = on) }
                    reschedule(context, store)
                }
            )
        }

        if (prefs.handshakeReminder) {
            item {
                GlassTimeRow(
                    backdrop = backdrop,
                    title = "提醒时间",
                    hour = prefs.handshakeHour,
                    minute = prefs.handshakeMinute,
                    shape = RoundedCornerShape(SkinTokens.cardRadius),
                    onChange = { hour, minute ->
                        store.updatePrefs { it.copy(handshakeHour = hour, handshakeMinute = minute) }
                    }
                )
            }
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "微行动提醒",
                checked = prefs.actionReminder,
                subtitle = "到了点提醒你今天的那张处方。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on ->
                    store.updatePrefs { it.copy(actionReminder = on) }
                    reschedule(context, store)
                }
            )
        }

        if (prefs.actionReminder) {
            item {
                GlassTimeRow(
                    backdrop = backdrop,
                    title = "提醒时间",
                    hour = prefs.actionHour,
                    minute = prefs.actionMinute,
                    shape = RoundedCornerShape(SkinTokens.cardRadius),
                    onChange = { hour, minute ->
                        store.updatePrefs { it.copy(actionHour = hour, actionMinute = minute) }
                    }
                )
            }
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "身体来信提醒",
                checked = prefs.calendarReminder,
                subtitle = "每周日晚上",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on ->
                    store.updatePrefs { it.copy(calendarReminder = on) }
                    reschedule(context, store)
                }
            )
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "免打扰时段",
                checked = prefs.quietMode,
                subtitle = "这段时间里不打扰你。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(quietMode = on) } }
            )
        }

        if (prefs.quietMode) {
            item {
                GlassTimeRow(
                    backdrop = backdrop,
                    title = "免打扰时段",
                    hour = prefs.quietStartHour,
                    minute = 0,
                    shape = RoundedCornerShape(SkinTokens.cardRadius),
                    onChange = { hour, _ -> store.updatePrefs { it.copy(quietStartHour = hour) } }
                )
            }
            item {
                GlassTimeRow(
                    backdrop = backdrop,
                    title = "结束时间",
                    hour = prefs.quietEndHour,
                    minute = 0,
                    shape = RoundedCornerShape(SkinTokens.cardRadius),
                    onChange = { hour, _ -> store.updatePrefs { it.copy(quietEndHour = hour) } }
                )
            }
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "免打扰时仍允许手动校准",
                checked = prefs.allowCalibrationInQuiet,
                subtitle = "免打扰只挡提醒，不挡你自己想说的话。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(allowCalibrationInQuiet = on) } }
            )
        }

        item {
            // Android 13+ 需要运行时权限；已经允许的话这一行只做状态展示。
            var notifyAllowed by remember { mutableStateOf(Notifier.canNotify(context)) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                notifyAllowed = granted
                if (granted) reschedule(context, store)
            }
            GlassActionRow(
                backdrop = backdrop,
                title = "通知权限状态",
                icon = "bell",
                subtitle = if (notifyAllowed) {
                    "已经允许。提醒会在到点时发出来。"
                } else {
                    "点一下申请；系统里关掉过的话，会带你去系统设置。"
                },
                value = if (notifyAllowed) "已允许" else "未申请",
                chevron = false,
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    if (notifyAllowed) {
                        launchSafely(context, notificationSettingsIntent(context))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runCatching { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            .onFailure { launchSafely(context, notificationSettingsIntent(context)) }
                    } else {
                        launchSafely(context, notificationSettingsIntent(context))
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "去系统设置里打开通知",
                icon = "settings",
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { launchSafely(context, notificationSettingsIntent(context)) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 6：隐私与安全
// ---------------------------------------------------------------------------

@Composable
private fun MyDataPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out ->
                    out.write(Backup.build(store.records, store.prefs))
                }
            }.onFailure {
                overlay.show(title = "备份失败", subtitle = "文件没有写进去，请换个位置再试。") { }
            }
        }
    }
    // 22.41.0：导出的文件给两条出路——「另存为」（系统文档创建器）与
    // 「分享」（系统分享面板）。内容同一份，去向用户自己选。
    val exportDataLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out ->
                    out.write(exportText(store, prefs.exportFormat))
                }
            }.onFailure {
                overlay.show(title = "保存失败", subtitle = "文件没有写进去，请换个位置再试。") { }
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val parsed = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { Backup.parse(it.readText()) }
            }.getOrNull()
            if (parsed == null) {
                overlay.show(title = "这个备份读不了", subtitle = "文件格式不对，或文件已经损坏。") { }
            } else {
                overlay.confirm(
                    title = "导入本机备份？",
                    message = "将合并 " + parsed.dayCount + " 天记录和偏好。同一天以备份里的为准，这一步不能撤销。",
                    confirmLabel = "导入",
                    cancelLabel = "先不要"
                ) {
                    val count = Backup.restore(store, parsed)
                    overlay.show(title = "导入完成", subtitle = "合并了 " + count + " 天记录。本机没有导入任何原始心率、睡眠或血氧数值。") { }
                }
            }
        }
    }

    CastPage {
        item {
            CastPageHeader(
                title = "我的数据",
                subtitle = "导出、备份、恢复与删除，都在这一页。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            CastCard(backdrop = backdrop) {
                CastHint("数据只存在这台手机上。应用默认不联网、没有服务器、没有账号。本机保存的只有「今天是什么天气」和「你手动勾选的感觉标签」，不保存原始心率、睡眠、血氧数值。")
                CastHint("例外只有一条、而且默认关闭：你自己连接「小米账号（云同步）」之后，应用才会去小米的服务器取你自己账号里的数据；详情见设置 → 隐私与安全里的「数据边界」。")
            }
        }

        item {
            val formatOptions = listOf("JSON", "CSV")
            GlassSelectRow(
                backdrop = backdrop,
                title = "导出格式",
                value = prefs.exportFormat,
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = "导出格式",
                        options = formatOptions,
                        selectedIndex = indexOrFirst(formatOptions, prefs.exportFormat)
                    ) { index ->
                        store.updatePrefs { it.copy(exportFormat = formatOptions[index]) }
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "导出数据",
                icon = "up",
                subtitle = "复制到剪贴板、另存为文件，或直接分享出去。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    val isCsv = prefs.exportFormat.equals("CSV", ignoreCase = true)
                    overlay.pickOne(
                        title = "导出数据",
                        options = listOf(
                            BodyCastText.text("复制到剪贴板"),
                            BodyCastText.text("另存为…"),
                            BodyCastText.text("分享…")
                        ),
                        selectedIndex = 0
                    ) { which ->
                        when (which) {
                            0 -> {
                                copyToClipboard(context, exportText(store, prefs.exportFormat))
                                overlay.show(
                                    title = "已经复制到剪贴板",
                                    subtitle = "粘贴到任何地方都能存下来。"
                                ) {
                                    CastHint("导出内容只有「日期 + 天气 + 你勾过的标签」，不含任何原始心率、睡眠或血氧数值。")
                                }
                            }
                            1 -> exportDataLauncher.launch(
                                if (isCsv) "bodycast-export.csv" else "bodycast-export.json"
                            )
                            else -> runCatching {
                                cn.pocket.toolbox.ui.share.ShareFile.share(
                                    context,
                                    exportText(store, prefs.exportFormat),
                                    if (isCsv) "bodycast-export.csv" else "bodycast-export.json",
                                    if (isCsv) "text/csv" else "application/json"
                                )
                            }
                        }
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "导出本机备份",
                icon = "up",
                subtitle = "生成 .bodycast 文件，换手机时可以导入。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = "导出本机备份",
                        options = listOf(
                            BodyCastText.text("另存为…"),
                            BodyCastText.text("分享…")
                        ),
                        selectedIndex = 0
                    ) { which ->
                        if (which == 0) {
                            exportBackupLauncher.launch("bodycast-backup.bodycast")
                        } else {
                            runCatching {
                                cn.pocket.toolbox.ui.share.ShareFile.share(
                                    context,
                                    Backup.build(store.records, store.prefs),
                                    "bodycast-backup.bodycast"
                                )
                            }
                        }
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "从备份恢复",
                icon = "down",
                subtitle = "先看将导入多少天，再确认写入本机。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "删除所有数据",
                icon = "trash",
                iconTint = SkinTokens.muted,
                subtitle = "清空本机的记录、标签和偏好。",
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.confirm(
                        title = "删除所有数据",
                        message = "会清空本机的天气记录、校准标签和偏好。这个操作没法撤销。",
                        confirmLabel = "删除",
                        cancelLabel = "先不要"
                    ) {
                        store.clearAll()
                    }
                }
            )
        }


    }
}

// ---------------------------------------------------------------------------
// 二级页 7：权限与说明（22.1.0 从「隐私与安全」拆出来）
// ---------------------------------------------------------------------------

/**
 * 权限与说明。
 *
 * 与「我的数据」分开的原因：一个管「东西放哪、怎么带走」，
 * 一个管「我允许它做什么」。混在一起时，用户想找「撤销授权」要翻很久。
 */
@Composable
private fun PermissionsPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val context = LocalContext.current

    CastPage {
        item {
            CastPageHeader(
                title = "权限与说明",
                subtitle = "这台机器允许了它什么，以及它承诺不做什么。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            CastCard(backdrop = backdrop) {
                CastHint("数据只存在这台手机上。应用默认不联网、没有服务器、没有账号。")
                Spacer(Modifier.height(4.dp))
                CastHint("本机保存的只有「今天是什么天气」和「你手动勾选的感觉标签」，不保存原始心率、睡眠、血氧数值。")
                CastHint("唯一会联网的是小米账号（云同步）那条可选通道，默认关闭；连接与断开都在「数据与同步」里。")
            }
        }

        item {
            var notifyAllowed by remember { mutableStateOf(Notifier.canNotify(context)) }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> notifyAllowed = granted }
            GlassActionRow(
                backdrop = backdrop,
                title = "通知权限",
                icon = "bell",
                subtitle = if (notifyAllowed) "已允许。提醒会在到点时发出来。" else "点一下申请；系统里关掉过的话，会带你去系统设置。",
                value = if (notifyAllowed) "已允许" else "未申请",
                chevron = false,
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    if (notifyAllowed) {
                        launchSafely(context, notificationSettingsIntent(context))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runCatching { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            .onFailure { launchSafely(context, notificationSettingsIntent(context)) }
                    } else {
                        launchSafely(context, notificationSettingsIntent(context))
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "健康数据授权",
                icon = "shield",
                subtitle = "在系统的健康服务里看 / 改本应用的读取授权。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { launchSafely(context, HealthConnectBridge.settingsIntent()) }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "撤销授权",
                icon = "minus",
                subtitle = "只能去系统里取消，应用自己撤不了。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { launchSafely(context, HealthConnectBridge.settingsIntent()) }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "隐私说明页",
                icon = "info",
                subtitle = "写清「读什么、不读什么、存在哪」的那一页。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    launchSafely(
                        context,
                        Intent(context, cn.pocket.toolbox.ui.PrivacyPolicyActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "系统权限管理",
                icon = "settings",
                subtitle = "去系统的应用详情页看这台机器给了什么权限。",
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { launchSafely(context, appDetailsIntent(context)) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 7：外观与体验
// ---------------------------------------------------------------------------

@Composable
private fun AppearancePage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs


    CastPage {
        item {
            CastPageHeader(
                title = "外观与体验",
                subtitle = "看着舒服，比功能多更重要。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            val themeOptions = listOf("浅色", "深色", "跟随系统")
            SettingBlock(title = "主题", backdrop = backdrop) {
                GlassSegmented(
                    options = themeOptions,
                    selectedIndex = prefs.themeMode.coerceIn(0, 2),
                    onSelect = { index ->
                        val night = (context.resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        store.updatePrefs {
                            it.copy(
                                themeMode = index,
                                darkMode = when (index) {
                                    0 -> false
                                    1 -> true
                                    else -> night
                                }
                            )
                        }
                    },
                    backdrop = backdrop
                )
            }
        }

        item {
            GlassSelectRow(
                backdrop = backdrop,
                title = BodyCastText.text("主题色"),
                subtitle = BodyCastText.text("按钮、图标与选中态的底色。绿色是原来的样子。"),
                value = BodyCastText.text(ACCENT_STYLE_LABELS[prefs.accentStyle.coerceIn(0, 3)]),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = BodyCastText.text("主题色"),
                        options = ACCENT_STYLE_LABELS.map { BodyCastText.text(it) },
                        selectedIndex = prefs.accentStyle.coerceIn(0, 3)
                    ) { index -> store.updatePrefs { it.copy(accentStyle = index) } }
                }
            )
        }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = "动画效果",
                checked = prefs.animationEnabled,
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(animationEnabled = on) } }
            )
        }

        item { CastHint("关掉之后页面切换和按压动画会变简单，省电一些。") }

        item {
            val fontOptions = listOf("小", "标准", "大", "特大")
            SettingBlock(title = "字体大小", backdrop = backdrop) {
                GlassSegmented(
                    options = fontOptions,
                    selectedIndex = prefs.fontScale.coerceIn(0, 3),
                    onSelect = { index -> store.updatePrefs { it.copy(fontScale = index) } },
                    backdrop = backdrop
                )
            }
        }

        item {
            val languages = AppLanguage.entries
            val autoLabel = when (AppLanguage.fromSystem().tag) {
                "zh-Hans" -> "跟随系统（简体中文）"
                "ja" -> "跟随系统（日本語）"
                else -> "跟随系统（English）"
            }
            val currentLanguage = AppLanguage.fromStored(prefs.language)
            GlassSelectRow(
                backdrop = backdrop,
                title = "语言",
                value = if (prefs.language == "auto" || prefs.language.isBlank()) autoLabel else currentLanguage.selfName,
                subtitle = "跟随系统：中文→简体中文，日语→日本語，其他→英语；拉丁文需手动选择。",
                // 22.5.0：页面末行圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlayLanguagePicker(
                        overlay = overlay,
                        languages = languages,
                        selected = currentLanguage
                    ) { language ->
                        store.updatePrefs { it.copy(language = language.tag) }
                        AppLanguage.apply(language)
                        // 通知渠道名与桌面小组件的静态串跟应用语言（22.28.0）：
                        // 渠道文本在创建时定格 → 删掉重建；小组件重取字符串并刷新。
                        runCatching { Notifier.ensureChannel(context) }
                        runCatching { cn.pocket.toolbox.widget.BodyCastWidgetProvider.updateAll(context) }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 二级页 8：关于
// ---------------------------------------------------------------------------

@Composable
private fun AboutPage(
    backdrop: Backdrop,
    store: BodyCastStore,
    onOpenDeveloper: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current

    CastPage {
        item {
            CastPageHeader(
                title = "关于",
                subtitle = "这个应用是谁做的、站在谁的肩膀上。",
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item {
            // 22.30.0：产品定位写进关于页——答辩口径与应用内口径必须是同一句。
            CastCard(backdrop = backdrop) {
                Text(
                    text = "BodyCast 是什么",
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "把身体状态翻译成天气，告诉你今天该怎么用力。",
                    fontSize = SkinTokens.body,
                    lineHeight = 28.sp,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "不打分、不排名、不制造焦虑；读不到就说读不到。" +
                        "从天气到营养元素再到食物——今天压力大，就去食谱页找含镁的食物。",
                    fontSize = SkinTokens.caption,
                    lineHeight = 23.sp,
                    color = SkinTokens.muted
                )
            }
        }

        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = "开发者",
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(10.dp))
                // 三个名字：居中对齐，宽度够就并排，不够自动换行往下排。
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("zhangyuhexht", "__xiaocai__").forEach { name ->
                        Text(
                            text = name,
                            fontSize = SkinTokens.body,
                            color = SkinTokens.ink
                        )
                    }
                }
            }
        }

        item {
            GlassRow(
                backdrop = backdrop,
                title = "版本号",
                icon = "info",
                value = BuildConfig.VERSION_NAME,
                chevron = false,
                shape = RoundedCornerShape(SkinTokens.cardRadius)
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "开源许可",
                icon = "book",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.show(
                        title = "开源许可",
                        subtitle = "这个应用站在这些开源项目上面。"
                    ) {
                        CastHint("Jetpack Compose、AndroidX / AppCompat —— Apache License 2.0")
                        CastHint("Kotlin 语言与标准库 —— Apache License 2.0")
                        CastHint("Kyant Backdrop / Shapes（液态玻璃与图形）—— Apache License 2.0")
                        CastHint("AndroidX Health Connect 客户端 —— Apache License 2.0")
                        CastHint("Material Components（仅用于窗口主题底色）—— Apache License 2.0")
                        CastHint(MiLicense.SUMMARY)
                        CastHint("以上项目均以原样提供，不含任何担保。完整许可文本可在各项目仓库查看。")
                        Spacer(Modifier.height(6.dp))
                        // 署名本身是英文法律文本：原样显示，不进翻译表（否则会记成假缺口）。
                        RawText(
                            text = MiLicense.SUMMARY,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = SkinTokens.caption,
                            lineHeight = 22.sp,
                            color = SkinTokens.muted
                        )
                        Spacer(Modifier.height(8.dp))
                        CastSecondaryButton(
                            text = "查看 MIT 许可原文",
                            onClick = { showMiLicense(overlay, backdrop) },
                            backdrop = backdrop
                        )
                    }
                }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "重看新手引导",
                icon = "info",
                subtitle = "第一次打开时的那三页，随时可以再看。",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { reopenOnboarding() }
            )
        }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "反馈与建议",
                icon = "edit",
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.show(
                        title = "反馈与建议",
                        subtitle = "这个应用不收集任何数据。",
                        footer = {
                            CastSecondaryButton(
                                text = "复制版本信息",
                                onClick = {
                                    copyToClipboard(
                                        context,
                                        "BodyCast ${BuildConfig.VERSION_NAME} (versionCode ${BuildConfig.VERSION_CODE})"
                                    )
                                    overlay.show(
                                        title = "已经复制",
                                        subtitle = "报问题时附上这行版本信息会更方便定位。"
                                    ) {
                                        CastHint("可以在应用商店留言，或直接告诉开发者。")
                                    }
                                },
                                backdrop = backdrop
                            )
                        }
                    ) {
                        CastHint("应用内没有反馈上传入口。应用不收集数据，平时也不联网——唯一会联网的是你自己开启的小米账号（云同步）通道。")
                        CastHint("如果想提意见，可以在应用商店的评价里留言，或直接把想法告诉开发者。")
                        CastHint("报问题时附上版本号，定位会快很多。")
                    }
                }
            )
        }

        // 22.12.0：开发者模式门控。正常不显示任何开发者入口；
        // 这块红色描边的块是唯一的入口——先确认，再进入。
        item {
            DeveloperGateBlock(
                unlocked = store.prefs.developerUnlocked,
                onRequestOpen = {
                    overlay.showDialog(
                        title = "打开开发者模式",
                        message = "你将要打开开发者模式。里面的功能都是一些测试未完善、" +
                            "可能有 bug、甚至可能损坏数据的功能，正常用户请不要打开。",
                        confirmLabel = "确认打开",
                        cancelLabel = "取消"
                    ) {
                        store.updatePrefs { it.copy(developerUnlocked = true) }
                    }
                },
                onEnter = onOpenDeveloper
            )
        }
    }
}

/**
 * 开发者模式门控块（22.12.0）。
 *
 * 是一块**红描边的独立块**，不是普通列表行：未解锁时写「打开开发者模式」，
 * 点击弹确认对话框；确认解锁后变成可进入的「开发者模式」菜单，点击进入三级页。
 * 解锁状态持久化在 prefs 里，重启后仍显示为菜单形态。
 */
@Composable
private fun DeveloperGateBlock(
    unlocked: Boolean,
    onRequestOpen: () -> Unit,
    onEnter: () -> Unit
) {
    val gateColor = Color(0xFFFF5A5F)
    val shape = RoundedCornerShape(SkinTokens.cardRadius)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(gateColor.copy(alpha = 0.08f))
            .border(1.5.dp, gateColor.copy(alpha = 0.85f), shape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = if (unlocked) onEnter else onRequestOpen
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            Column {
                Text(
                    text = BodyCastText.text(if (unlocked) "开发者模式" else "打开开发者模式"),
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.SemiBold,
                    color = gateColor
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = BodyCastText.text(
                        if (unlocked) "测试信息与实验性开关；改动即时生效。"
                        else "测试未完善、可能有 bug，正常用户请不要打开。"
                    ),
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted
                )
            }
        }
        GlassIcon(
            "next",
            gateColor.copy(alpha = 0.9f),
            Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// 三级页：开发者模式（实验性功能）
// ---------------------------------------------------------------------------

/** 强调色档位的显示名，下标与 `BodyCastPrefs.accentStyle` 一一对应。 */
private val ACCENT_STYLE_LABELS = listOf("绿", "蓝", "紫", "橙")

/** 圆角档位的显示名，下标与 `BodyCastPrefs.cornerStyle` 一一对应。 */
private val CORNER_STYLE_LABELS = listOf("标准（32dp）", "更圆（40dp）", "更方（24dp）")

/** 玻璃强度档位的显示名，下标与 `BodyCastPrefs.glassStrength` 一一对应。 */
private val GLASS_STRENGTH_LABELS = listOf("标准", "更透", "更磨砂")

/**
 * 实验性功能页。
 *
 * 这里的开关都是「改一下看看效果」的性质：不改数据、不上传、随时可以改回来。
 * 三项外观档位写进 pref 后由 [cn.pocket.toolbox.bodycast.BodyCastStore]
 * 推给 [cn.pocket.toolbox.glass.GlassTheme] / [cn.pocket.toolbox.glass.GlassSupport]，
 * 改动即时生效；「强制磨砂降级」切换后返回其他页面才刷新（当前页的玻璃节点
 * 已经组合过一遍）。
 */
/**
 * 同步数据明细（22.36.0）：七类读数 × 三通道的对照表。
 *
 * 立意很朴素——「同步成功」四个字不算证据，**数值摆出来才算**：
 * 小米云同步回来的、手动填的、文件导入的各占一列，
 * 取到就填值，没取到写「没读到」，不猜也不补 0。
 * 表头给这条通道的状态（上次同步时间 / 账号 / 区域 / 会话在不在）。
 */
@Composable
fun SyncedDataCard(
    backdrop: Backdrop,
    store: BodyCastStore,
    /** 22.49.0：这张表怎么看——有内容时收进卡内标题旁的 ⓘ，不占版面。 */
    infoLines: List<String> = emptyList()
) {
    val context = LocalContext.current
    val prefs = store.prefs

    data class Row(
        val label: String,
        val cloud: String,
        val manual: String,
        val imported: String
    )

    fun fmtInt(v: Int?): String = v?.toString() ?: BodyCastText.text("没读到")
    fun fmtSleep(v: Int?): String = if (v == null) BodyCastText.text("没读到") else "${v / 60}h${v % 60}m"
    fun fmtRatio(v: Float?): String = if (v == null) BodyCastText.text("没读到") else "${(v * 100).toInt()}%"

    val rows = listOf(
        Row(BodyCastText.text("步数"),
            fmtInt(prefs.miCloudSteps), fmtInt(prefs.manualSteps), fmtInt(prefs.importedSteps)),
        Row(BodyCastText.text("睡眠时长"),
            fmtSleep(prefs.miCloudSleepMinutes), fmtSleep(prefs.manualSleepMinutes), fmtSleep(prefs.importedSleepMinutes)),
        Row(BodyCastText.text("深睡占比"),
            fmtRatio(prefs.miCloudDeepSleepRatio), fmtRatio(prefs.manualDeepSleepRatio), fmtRatio(prefs.importedDeepSleepRatio)),
        Row(BodyCastText.text("静息心率"),
            fmtInt(prefs.miCloudRestingHeartRate), fmtInt(prefs.manualRestingHeartRate), fmtInt(prefs.importedRestingHeartRate)),
        Row(BodyCastText.text("心率变异性"),
            fmtInt(prefs.miCloudHrv), fmtInt(prefs.manualHrv), fmtInt(prefs.importedHrv)),
        Row(BodyCastText.text("压力值"),
            fmtInt(prefs.miCloudStressLevel), BodyCastText.text("没读到"), fmtInt(prefs.importedStressLevel)),
        Row(BodyCastText.text("血氧"),
            fmtInt(prefs.miCloudBloodOxygen), fmtInt(prefs.manualBloodOxygen), fmtInt(prefs.importedBloodOxygen)),
        Row(BodyCastText.text("运动时长"),
            fmtInt(prefs.miCloudActivityMinutes), fmtInt(prefs.manualActivityMinutes), fmtInt(prefs.importedActivityMinutes))
    )

    val syncedAt = prefs.miCloudSyncedAt
    val syncLine = if (prefs.miCloudToken.isBlank()) {
        BodyCastText.text("小米云未连接")
    } else if (syncedAt <= 0L) {
        BodyCastText.text("已连接，还没有同步过")
    } else {
        BodyCastText.text("上次同步：") + entryStamp(syncedAt) +
            if (prefs.miCloudFresh) "" else "（" + BodyCastText.text("已过期") + "）"
    }

    CastCard(backdrop = backdrop) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = BodyCastText.text("这就是应用当前手里的全部读数。"),
                modifier = Modifier.weight(1f),
                fontSize = SkinTokens.caption,
                color = SkinTokens.muted
            )
            CastInfoDot(
                title = BodyCastText.text("这张表怎么看"),
                lines = infoLines,
                backdrop = backdrop
            )
        }
        Spacer(Modifier.height(6.dp))
        CastHint(syncLine)
        if (prefs.miCloudToken.isNotBlank()) {
            CastHint(
                BodyCastText.text("账号：") + prefs.miCloudAccount.ifBlank { "-" } +
                    " · " + BodyCastText.text("区域：") + prefs.miCloudRegion.ifBlank { "-" }
            )
        }
        Spacer(Modifier.height(8.dp))

        // 表头
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = "",
                modifier = Modifier.weight(1.4f),
                fontSize = SkinTokens.caption,
                color = SkinTokens.muted
            )
            for (h in listOf(BodyCastText.text("小米云"), BodyCastText.text("手动"), BodyCastText.text("导入"))) {
                Text(
                    text = h,
                    modifier = Modifier.weight(1f),
                    fontSize = SkinTokens.caption,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.muted
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Text(
                    text = row.label,
                    modifier = Modifier.weight(1.4f),
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.ink
                )
                for (v in listOf(row.cloud, row.manual, row.imported)) {
                    Text(
                        text = v,
                        modifier = Modifier.weight(1f),
                        fontSize = SkinTokens.caption,
                        color = if (v == BodyCastText.text("没读到")) SkinTokens.muted else SkinTokens.ink
                    )
                }
            }
        }

        // ---- 22.37.0 Health Connect 内容透视：HC 这条路断在哪，一眼看清 ----
        Spacer(Modifier.height(10.dp))
        Text(
            text = BodyCastText.text("Health Connect 里实际有什么（最近 30 天）"),
            fontSize = SkinTokens.caption,
            fontWeight = FontWeight.Medium,
            color = SkinTokens.ink
        )
        val probes = remember { mutableStateOf<List<cn.pocket.toolbox.data.HealthConnectSource.Probe>?>(null) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            probes.value = runCatching { cn.pocket.toolbox.data.HealthConnectSource.probe(context) }.getOrDefault(emptyList())
        }
        val rows2 = probes.value
        when {
            rows2 == null -> CastHint(BodyCastText.text("正在读 Health Connect…"))
            rows2.isEmpty() -> CastHint(BodyCastText.text("读不到 Health Connect（先在自检里把第①②步的服务和授权弄好）。"))
            else -> {
                Spacer(Modifier.height(4.dp))
                rows2.forEach { probe ->
                    val line = if (probe.count == 0) {
                        probe.type.label + "：" + BodyCastText.text("0 条（厂商没写入）")
                    } else {
                        probe.type.label + "：" + BodyCastText.text("${probe.count} 条 · 最新 ") +
                            entryStamp(probe.latestAt ?: 0L) +
                            (probe.origin?.let { " · $it" } ?: "")
                    }
                    Text(
                        text = line,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = SkinTokens.caption,
                        color = if (probe.count == 0) SkinTokens.muted else SkinTokens.ink
                    )
                }
                Spacer(Modifier.height(4.dp))
                CastHint(BodyCastText.text("0 条 = 厂商 App 没往 Health Connect 写；有条但上面读不到 = 授权或读取侧的问题。"))
            }
        }
    }
}

@Composable
private fun DeveloperPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs
    // 自检要在 IO 上跑网络请求，这里借这个 scope 起协程。
    val scope = rememberCoroutineScope()

    CastPage {
        item {
            CastPageHeader(
                title = BodyCastText.text("实验性功能"),
                subtitle = BodyCastText.text("给开发者与好奇者的东西；改动即时生效。"),
                trailing = { BackButton(onBack, backdrop) }
            )
        }

        item { CastGroupLabel(BodyCastText.text("外观实验")) }

        item {
            GlassSelectRow(
                backdrop = backdrop,
                title = BodyCastText.text("强调色"),
                subtitle = BodyCastText.text("按钮、图标与选中态的底色。绿色是原来的样子。"),
                value = BodyCastText.text(ACCENT_STYLE_LABELS[prefs.accentStyle.coerceIn(0, 3)]),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = BodyCastText.text("强调色"),
                        options = ACCENT_STYLE_LABELS.map { BodyCastText.text(it) },
                        selectedIndex = prefs.accentStyle.coerceIn(0, 3)
                    ) { index -> store.updatePrefs { it.copy(accentStyle = index) } }
                }
            )
        }

        item {
            GlassSelectRow(
                backdrop = backdrop,
                title = BodyCastText.text("圆角风格"),
                subtitle = BodyCastText.text("只改主卡片的圆角，次级行与间距保持不动。"),
                value = BodyCastText.text(CORNER_STYLE_LABELS[prefs.cornerStyle.coerceIn(0, 2)]),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = BodyCastText.text("圆角风格"),
                        options = CORNER_STYLE_LABELS.map { BodyCastText.text(it) },
                        selectedIndex = prefs.cornerStyle.coerceIn(0, 2)
                    ) { index -> store.updatePrefs { it.copy(cornerStyle = index) } }
                }
            )
        }

        item {
            GlassSelectRow(
                backdrop = backdrop,
                title = BodyCastText.text("玻璃强度"),
                subtitle = BodyCastText.text("模糊与挂色的浓淡。调过头会牺牲可读性，看个新鲜就够。"),
                value = BodyCastText.text(GLASS_STRENGTH_LABELS[prefs.glassStrength.coerceIn(0, 2)]),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    overlay.pickOne(
                        title = BodyCastText.text("玻璃强度"),
                        options = GLASS_STRENGTH_LABELS.map { BodyCastText.text(it) },
                        selectedIndex = prefs.glassStrength.coerceIn(0, 2)
                    ) { index -> store.updatePrefs { it.copy(glassStrength = index) } }
                }
            )
        }

        item { CastGroupLabel(BodyCastText.text("底层开关")) }

        item {
            GlassToggleRow(
                backdrop = backdrop,
                title = BodyCastText.text("强制磨砂降级"),
                checked = prefs.forceFrost,
                subtitle = BodyCastText.text("关掉折射着色器，统一走安卓 13 以下的磨砂外观。切换后返回其他页面即生效；当前页可能要离开再回来。"),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onCheckedChange = { on -> store.updatePrefs { it.copy(forceFrost = on) } }
            )
        }

        item {
            val liquidGlass = GlassSupport.isLiquidGlassAvailable
            CastCard(backdrop = backdrop) {
                Text(
                    text = BodyCastText.text("运行环境"),
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(8.dp))
                CastHint("版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                CastHint("SDK：${Build.VERSION.SDK_INT}")
                CastHint(
                    when {
                        liquidGlass -> "液态玻璃可用：是"
                        prefs.forceFrost -> "液态玻璃可用：否（已手动强制磨砂降级）"
                        else -> "液态玻璃可用：否（系统低于安卓 13，自动磨砂降级）"
                    }
                )
            }
        }

        // ---- 22.36.0 同步数据明细：一眼看清「到底同步到了什么」 ----
        item { CastGroupLabel(BodyCastText.text("同步数据明细")) }

        item {
            SyncedDataCard(backdrop, store)
        }

        item { CastGroupLabel(BodyCastText.text("测试数据")) }

        item {
            GlassActionRow(
                backdrop = backdrop,
                title = BodyCastText.text("一键填入演示读数"),
                icon = "bolt",
                subtitle = BodyCastText.text("会覆盖当前读数，仅供预览。"),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { store.useDemoData() }
            )
        }

        // 小米令牌的加密自检：**不显示令牌内容**，只回答四个「是 / 否」。
        // 放在这里是为了让「令牌是加密存的」这句话随时可以被验证，而不是靠信任。
        item {
            GlassActionRow(
                backdrop = backdrop,
                title = BodyCastText.text("小米令牌加密自检"),
                icon = "shield",
                subtitle = BodyCastText.text("用一串临时数据走一遍「存 → 读 → 删除」。不会显示任何令牌内容。"),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    val round = MiTokenVault.selfTest()
                    overlay.show(
                        title = "小米令牌加密自检",
                        subtitle = "四个问题各答一句；密钥在自检结束时已被删除。"
                    ) {
                        CastHint("密文与明文不同：" + yesNo(round.cipherTextDiffersFromPlain))
                        CastHint("加密后能原样读回来：" + yesNo(round.decryptedMatches))
                        CastHint("删除密钥之后读不回来：" + yesNo(round.removedAfterDelete))
                        CastHint("系统密钥库可用：" + yesNo(round.keyUsable))
                        CastHint("自检用的是临时字符串，不是你的令牌；过程里也不打印任何内容。")
                    }
                }
            )
        }

        // 网络失败路径自检：拿一个**假会话**去问一个不存在的域名，看它是不是按设计
        // 报「连不上小米的服务器」，而不是崩掉或假装成功。不涉及任何真实令牌。
        item {
            GlassActionRow(
                backdrop = backdrop,
                title = BodyCastText.text("小米云网络失败自检"),
                icon = "network",
                subtitle = BodyCastText.text("用一个假会话去问一个不存在的域名：应当报「连不上小米的服务器」，而且不崩。"),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = {
                    scope.launch {
                        val message = try {
                            MiCloudApi.fetchSnapshot(fakeMiSession(), hostOverride = MiCloudApi.SELF_CHECK_HOST)
                            "意外地成功了：这台机器居然解析了那个不存在的域名。"
                        } catch (e: Exception) {
                            MiCloudSource.messageOf(e)
                        }
                        overlay.show(title = "小米云网络失败自检", subtitle = message) {
                            CastHint("预期结果：连不上小米的服务器：请检查网络后重试。")
                            CastHint("这一条走的是假会话 + 一个永远不会解析的域名，不发真实请求内容，也不读任何真实数据。")
                        }
                    }
                }
            )
        }

        item { CastGroupLabel(BodyCastText.text("漏翻审计")) }

        item {
            // 缺口是普通 Set 而不是 Compose 状态，所以进这一页时读一次就够了；
            // 点开后浮层里的清空按钮会自己刷新（见 showTranslationGaps）。
            val sheetGaps = TranslationAudit.sheetGapSnapshot().size
            val fallbacks = TranslationAudit.fallbackSnapshot().size
            GlassActionRow(
                backdrop = backdrop,
                title = BodyCastText.text("漏翻审计"),
                icon = "type",
                subtitle = BodyCastText.text("还没翻的条目会回退成英语；这里列出当前语言检测到的缺口。"),
                value = (sheetGaps + fallbacks).toString(),
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = { showTranslationGaps(overlay, backdrop) }
            )
        }
    }
}

/**
 * 漏翻审计浮层：逐句表缺口（当前语言槽位为空、回退英语）与整条未登记的兜底串，各列前 30 条。
 * 这是日语「还在显示英语」唯一可靠的检出手段——日语本来就有汉字，
 * 靠「屏幕上有没有 CJK」是筛不出漏翻的，只能看这里。
 *
 * 函数本身不是 `@Composable`（它只负责组装浮层），`tick` 在组合之外创建，
 * 内容是组合时才读的，所以「清空」之后浮层会自己重新列一遍。
 */
private fun showTranslationGaps(overlay: CastOverlayController, backdrop: Backdrop) {
    val tick = mutableStateOf(0)
    overlay.show(
        title = BodyCastText.text("漏翻审计"),
        subtitle = BodyCastText.text("还没翻的条目会回退成英语；这里列出当前语言检测到的缺口。"),
        footer = {
            Box(Modifier.fillMaxWidth()) {
                CastSecondaryButton(
                    text = BodyCastText.text("清空"),
                    onClick = {
                        TranslationAudit.clear()
                        tick.value++
                    },
                    backdrop = backdrop
                )
            }
        }
    ) {
        // 订阅 tick：清空之后它变了，这一块才会重新组合。
        tick.value
        val sheet = TranslationAudit.sheetGapSnapshot()
        val fallback = TranslationAudit.fallbackSnapshot()
        // 缺口计数按**当前语言**算：切到拉丁文时这里显示的就是 la 的缺口数。
        CastHint(
            BodyCastText.text("逐句表缺口") + " [" + AppLanguage.current().tag + "] " +
                TranslationAudit.sheetGapCount(AppLanguage.current())
        )
        if (sheet.isEmpty()) {
            CastHint(BodyCastText.text("当前没有检测到缺口。"))
        } else {
            sheet.take(30).forEach { CastHint(it) }
        }
        Spacer(Modifier.height(10.dp))
        CastHint(BodyCastText.text("兜底回退") + " " + fallback.size)
        fallback.take(30).forEach { CastHint(it) }
    }
}

// ---------------------------------------------------------------------------
// 浮层内容
// ---------------------------------------------------------------------------

/** 自检里的「是 / 否」。三态写成文字，不靠颜色（这一套界面里没有红色）。 */
private fun yesNo(value: Boolean): String = if (value) "是" else "否"

/**
 * 网络自检用的**假会话**：字段齐全但全是占位值，区域刻意取一个不存在的主机名。
 * 它只用来验证「连不上时报什么、崩不崩」，不接触任何真实账号与数据。
 */
private fun fakeMiSession() = MiSession(
    userId = "0",
    ssecurity = "AAAAAAAAAAAAAAAAAAAAAA==",
    serviceToken = "self-check",
    passToken = "self-check",
    deviceId = "wb_selfcheck",
    region = "selfcheck"
)

/**
 * MIT 许可原文。
 *
 * 移植来源的许可是 MIT：要求在任何副本或实质性部分里保留版权声明与许可原文。
 * 所以原文**原样**放在这里（逐行、不翻译、不进翻译表）——法律文本翻译过就变了。
 */
private fun showMiLicense(overlay: CastOverlayController, backdrop: Backdrop) {
    overlay.show(
        title = "MIT License",
        subtitle = MiLicense.SUMMARY,
        tall = true
    ) {
        MiLicense.TEXT_LINES.forEach { line ->
            RawText(
                text = line,
                modifier = Modifier.fillMaxWidth(),
                fontSize = SkinTokens.caption,
                lineHeight = 20.sp,
                color = SkinTokens.muted
            )
        }
        Spacer(Modifier.height(8.dp))
        RawText(
            text = MiLicense.SOURCE_URL,
            modifier = Modifier.fillMaxWidth(),
            fontSize = SkinTokens.caption,
            lineHeight = 20.sp,
            color = SkinTokens.accent
        )
    }
}

/**
 * 手环连接引导浮层。
 *
 * 引导文案由 [HealthConnectBridge.vendorGuide] 提供，**只给这台机器上真实装到的
 * App 的路径**（方案 §3.8）：以前无论装没装都把两家各写一遍，用户照着找不到菜单，
 * 还以为是自己的问题。一个都没装时给的是「先装厂商 App」三步 + 去商店入口。
 *
 * 全篇不引导用户去做手环直连——应用也不申请任何直连相关权限。
 */
private fun showVendorGuide(
    overlay: CastOverlayController,
    backdrop: Backdrop,
    context: Context,
    title: String,
    family: String,
    // 页面已经异步扫好的名单：浮层不再在界面线程上重扫一遍「已安装应用」。
    vendors: List<HealthConnectBridge.InstalledVendor> =
        HealthConnectBridge.installedVendors(context)
) {
    val installed = vendors
        .filter { family == "any" || it.profile.family == family }
    val storeIntents = if (family == "huawei") {
        HealthConnectBridge.huaweiIntents(context)
    } else {
        HealthConnectBridge.xiaomiIntents(context)
    }
    overlay.show(
        title = title,
        subtitle = "数据不直接连应用，先汇总到系统的健康服务。",
        footer = {
            if (installed.isNotEmpty()) {
                // 装了哪个就打开哪个：按钮文字就是应用的真实名字，
                // 不会再出现「点小米运动健康却跳到小米穿戴」。
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    installed.take(2).forEach { vendor ->
                        Box(Modifier.weight(1f)) {
                            CastSecondaryButton(
                                text = "打开${vendor.app.label}",
                                onClick = { launchSafely(context, vendor.app.launch) },
                                backdrop = backdrop
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth()) {
                    CastSecondaryButton(
                        text = "去商店找厂商 App",
                        onClick = {
                            val opened = HealthConnectBridge.openFirstAvailable(context, storeIntents)
                            if (!opened) {
                                overlay.show(
                                    title = "商店里没搜到",
                                    subtitle = "可以换一个应用商店再试。"
                                ) {
                                    CastHint("有些手机自带商店里没有这个应用，换 Play 商店或厂商商店试试。")
                                }
                            }
                        },
                        backdrop = backdrop
                    )
                }
            }
        }
    ) {
        CastStepList(HealthConnectBridge.vendorGuideFor(installed, family))
        CastHint("应用不会直接连手环，所有数据都经过系统的健康服务汇总。")
        if (installed.any { it.profile.writesHealthConnect != true }) {
            // 华为 / 荣耀：厂商不往系统健康服务写，这条是现实里能走通的路。
            CastHint("这家不往系统健康服务写数据，下面这条是现实里能走通的路：")
            CastStepList(HealthConnectBridge.bridgeGuide())
        }
    }
}

/** 校准标签自定义浮层。 */
private fun showCalibrationEditor(
    overlay: CastOverlayController,
    backdrop: Backdrop,
    store: BodyCastStore
) {
    overlay.show(
        title = "校准标签自定义",
        subtitle = "关掉的标签不会出现在首页上。",
        tall = true
    ) {
        CalibrationEditorBody(backdrop = backdrop, store = store)
    }
}

@Composable
private fun CalibrationEditorBody(backdrop: Backdrop, store: BodyCastStore) {
    val prefs = store.prefs
    var draft by remember { mutableStateOf("") }

    // 重名判断要同时挡住默认标签和已经加过的自定义标签，重名就直接忽略，不弹错。
    val reserved = remember(prefs.customCalibrationLabels) {
        CalibrationTag.entries.map { it.label }.toSet() + prefs.customCalibrationLabels
    }

    CalibrationTag.entries.forEach { tag ->
        GlassToggleRow(
            backdrop = backdrop,
            title = tag.label,
            checked = tag !in prefs.hiddenCalibration,
            onCheckedChange = { on ->
                store.updatePrefs { p ->
                    p.copy(
                        hiddenCalibration = if (on) {
                            p.hiddenCalibration - tag
                        } else {
                            p.hiddenCalibration + tag
                        }
                    )
                }
            }
        )
    }

    Spacer(Modifier.height(4.dp))

    CastCard(backdrop = backdrop) {
        Text(
            text = "加一个自己的标签",
            fontSize = SkinTokens.body,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(10.dp))
        GlassTextField(
            value = draft,
            onValueChange = { draft = it.take(6) },
            placeholder = "比如「闷」",
            backdrop = backdrop
        )
        Spacer(Modifier.height(12.dp))
        CastPrimaryButton(
            text = "加一个",
            onClick = {
                val name = draft.trim()
                if (name.isNotEmpty() && name !in reserved) {
                    store.updatePrefs { it.copy(customCalibrationLabels = it.customCalibrationLabels + name) }
                }
                draft = ""
            },
            backdrop = backdrop,
            enabled = draft.isNotBlank() && draft.trim() !in reserved
        )
    }

    if (prefs.customCalibrationLabels.isNotEmpty()) {
        CastHint("已经加上的标签：")
        prefs.customCalibrationLabels.forEach { label ->
            CastCard(backdrop = backdrop) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RawText(
                        text = label,
                        modifier = Modifier.weight(1f),
                        fontSize = SkinTokens.body,
                        color = SkinTokens.ink
                    )
                    Spacer(Modifier.width(12.dp))
                    CastIconButton(
                        icon = "trash",
                        label = null,
                        onClick = {
                            store.updatePrefs { it.copy(customCalibrationLabels = it.customCalibrationLabels - label) }
                        },
                        backdrop = backdrop,
                        tint = SkinTokens.muted
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

/**
 * 带标题的玻璃卡片。分段控制器和标签组没有自带的标题行，用这个包一层，
 * 免得一页设置里出现「找不到标题的控件」。
 */
@Composable
private fun SettingBlock(
    title: String,
    backdrop: Backdrop,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CastCard(backdrop = backdrop) {
        Text(
            text = title,
            fontSize = SkinTokens.body,
            fontWeight = FontWeight.Medium,
            color = SkinTokens.ink
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = SkinTokens.caption,
                color = SkinTokens.muted
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

private fun overlayLanguagePicker(
    overlay: CastOverlayController,
    languages: List<AppLanguage>,
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    overlay.pickOne(
        title = BodyCastText.text("语言"),
        options = languages.map { it.selfName },
        selectedIndex = languages.indexOf(selected).coerceAtLeast(0)
    ) { index -> onSelect(languages[index]) }
}

/** 选项列表里找下标，找不到就用第一个，避免浮层里一个「当前」都不显示。 */
private fun indexOrFirst(options: List<String>, value: String): Int =
    options.indexOf(value).let { if (it < 0) 0 else it }

/** 跳转 Intent 一律包一层，跳不过去也不能让页面崩。 */
private fun launchSafely(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}

/** 先试主 Intent（例如直接打开厂商 App），失败就回落到应用商店。 */
private fun openOrFallback(context: Context, primary: Intent, fallback: Intent) {
    val opened = runCatching { context.startActivity(primary) }.isSuccess
    if (!opened) {
        runCatching { context.startActivity(fallback) }
    }
}

/** 通知设置页。 */
private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

/** 系统里的应用详情页（权限管理入口）。 */
private fun appDetailsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )

/** 复制纯文本到剪贴板。**不写文件，也不申请存储权限。** */
private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(ClipboardManager::class.java) ?: return
    manager.setPrimaryClip(ClipData.newPlainText("BodyCast", text))
}

/**
 * 把本机的天气记录序列化成文本。
 *
 * 只导出 [cn.pocket.toolbox.bodycast.WeatherRecord] 里的三个字段：
 * 日期、天气类型、勾过的标签。**没有任何原始生理数值可以泄漏**，
 * 因为仓库里根本没存过。
 */
private fun exportText(store: BodyCastStore, format: String): String {
    val records = store.records.values.sortedBy { it.date }
    return if (format.equals("CSV", ignoreCase = true)) {
        buildString {
            append("date,weather,tags\n")
            records.forEach { record ->
                append(record.date).append(',')
                append(record.kind.name).append(',')
                append(record.calibrated.joinToString("|") { it.name })
                append('\n')
            }
        }
    } else {
        buildString {
            append("[\n")
            records.forEachIndexed { index, record ->
                append("  {\"date\":\"").append(record.date)
                append("\",\"weather\":\"").append(record.kind.name)
                append("\",\"tags\":[")
                append(record.calibrated.joinToString(",") { "\"" + it.name + "\"" })
                append("]}")
                if (index != records.lastIndex) append(',')
                append('\n')
            }
            append("]\n")
        }
    }
}

/** 偏好改完之后把提醒重排一遍（通知页几个开关都会调它）。 */
private fun reschedule(context: android.content.Context, store: BodyCastStore) {
    Notifier.ensureChannel(context)
    ReminderScheduler.scheduleAll(context, store.prefs)
}
