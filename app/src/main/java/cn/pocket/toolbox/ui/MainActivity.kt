package cn.pocket.toolbox.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.FetchFreshness
import cn.pocket.toolbox.data.HealthConnectBridge
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import cn.pocket.toolbox.bodycast.rememberBodyCastStore
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.glass.GlassTheme
import cn.pocket.toolbox.glass.LiquidBottomTab
import cn.pocket.toolbox.glass.LiquidBottomTabs
import cn.pocket.toolbox.notify.Notifier
import cn.pocket.toolbox.notify.ReminderScheduler
import cn.pocket.toolbox.ui.bodycast.CastOverlayController
import cn.pocket.toolbox.ui.bodycast.CastOverlayHost
import cn.pocket.toolbox.ui.bodycast.DataGuidePage
import cn.pocket.toolbox.ui.bodycast.UiMotion
import cn.pocket.toolbox.ui.bodycast.BodyCastNav
import cn.pocket.toolbox.ui.bodycast.EchoTab
import cn.pocket.toolbox.ui.bodycast.ImportInbox
import cn.pocket.toolbox.ui.bodycast.ImportInboxHost
import cn.pocket.toolbox.ui.bodycast.OnboardingOverlay
import cn.pocket.toolbox.ui.bodycast.OnboardingState
import cn.pocket.toolbox.ui.bodycast.markOnboardingDone
import cn.pocket.toolbox.ui.bodycast.shouldShowOnboarding
import cn.pocket.toolbox.ui.skin.SkinTokens
import cn.pocket.toolbox.widget.BodyCastWidgetProvider
import cn.pocket.toolbox.widget.WidgetSnapshot
import cn.pocket.toolbox.ui.bodycast.HomeTab
import cn.pocket.toolbox.ui.bodycast.LocalCastOverlay
import cn.pocket.toolbox.ui.bodycast.RecipeTab
import cn.pocket.toolbox.ui.bodycast.SettingsTab
import kotlinx.coroutines.launch

/**
 * BodyCast 身体气象台 —— 应用入口。
 *
 * 四栏：
 * 0 首页 —— 今天身体是什么天气 + 今日微行动，主操作都在这一页
 * 1 食谱 —— 吃什么，按营养素找
 * 2 回声 —— 周天气、身体来信、趋势、分享卡（回放，不行动）
 * 3 设置 —— 数据源、校准、偏好、隐私、外观
 *
 * **不做的事**：不评分、不排名、不显示卡路里、不显示体重、不制造焦虑。
 * 界面上不会出现红色警告，也不会出现「您今日未达标」这类句式。
 *
 * 全局共用同一个 [rememberLayerBackdrop]，这样底部玻璃栏折射的是页面内容本身，
 * 而不是只有一层背景色。为了让它真的折到东西，页面内容必须**铺满整屏**、
 * 从底栏底下穿过去——各页面自己用 `CastPage` 预留底部空白。
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** 通知点击时带的 tab 序号（0 首页 / 1 食谱 / 2 回声 / 3 设置）。 */
        const val EXTRA_OPEN_TAB = "open_tab"

        /**
         * 桌面小组件「今天吃什么」按钮带的元素名（Nutrient 枚举名，如 MAGNESIUM）。
         * 带上它就直接切到食谱页并按该元素筛好（22.32.0 小组件升级）。
         */
        const val EXTRA_RECIPE_FOCUS = "recipe_focus"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguage.initializeFromPrefs(this)
        super.onCreate(savedInstanceState)
        // 22.36.1：撤掉 preferredDisplayModeId 强切显示模式——
        // 实测它会把帧驱动的展开动画冻在第 0 帧（折叠栏「打不开」的真凶），
        // 高刷让 Compose 自己跟系统可变刷新率即可，不再手动切 mode。
        // 状态栏图标色跟随当前深浅主题（浅色主题用深色图标）；导航栏保持浅色图标。
        // 进入组合后 BodyCastApp 会按主题档位再校准一次，深浅切换即时生效。
        applySystemBars(this)
        // 通知点进来时可以带一个 tab（0 首页 / 2 回声）。
        val initialTab = intent?.getIntExtra(EXTRA_OPEN_TAB, 0) ?: 0
        // 小组件「今天吃什么」：带元素名进来，直接切食谱页并筛上（22.32.0）。
        intent?.getStringExtra(EXTRA_RECIPE_FOCUS)?.let { name ->
            cn.pocket.toolbox.bodycast.Nutrient.entries.firstOrNull { it.name == name }?.let { focus ->
                BodyCastNav.openRecipe(focus)
            }
        }
        // 分享面板送进来的文件（22.24.0）：只记下 uri，解析与确认卡由界面层做。
        ImportInbox.accept(intent)
        // 通知渠道必须在发之前建好；提醒则按当前偏好重排一遍。
        runCatching { Notifier.ensureChannel(this) }
        setContent { BodyCastApp(initialTab = initialTab, appContext = this) }
    }

    /**
     * 应用在前台时又收到一次分享（Manifest 里是 singleTop）。
     *
     * 不重建 Activity、也不弹第二个界面：只把 uri 交给 [ImportInbox]，
     * 界面层的宿主会把它换成一张确认卡。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ImportInbox.accept(intent)
        // 小组件「今天吃什么」在应用已开时也要生效（22.32.0）：
        // 放一次性的打开请求，主界面的 LaunchedEffect 看到就切食谱页。
        intent.getStringExtra(EXTRA_RECIPE_FOCUS)?.let { name ->
            cn.pocket.toolbox.bodycast.Nutrient.entries.firstOrNull { it.name == name }?.let { focus ->
                BodyCastNav.openRecipe(focus)
            }
        }
    }
}

/**
 * 状态栏图标色跟随当前深浅主题：浅色主题用深色图标（[SystemBarStyle.light]），
 * 深色主题用浅色图标（[SystemBarStyle.dark]）。导航栏仍是浅色图标，不动。
 * 深浅档位切换时由 `BodyCastApp` 里的 [LaunchedEffect] 重新调一次，即时生效。
 */
private fun applySystemBars(activity: androidx.activity.ComponentActivity) {
    activity.enableEdgeToEdge(
        statusBarStyle = if (SkinTokens.isDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        },
        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )
}

/** 底部四个栏。icon 对应 [GlassIcon] 的 kind。8.0.0 起的信息架构：
 * 0 首页 —— 今天的身体天气 + 今日微行动（主操作都在这一页）
 * 1 食谱 —— 今天吃什么，按营养素找
 * 2 回声 —— 周天气、来信、图表、分享卡（回放，不行动）
 * 3 设置 —— 数据源、校准、偏好、隐私、外观
 */
private data class TabSpec(val label: String, val icon: String)

private val TABS = listOf(
    TabSpec("首页", "home"),
    TabSpec("食谱", "leaf"),
    TabSpec("回声", "chart"),
    TabSpec("设置", "settings")
)

@Composable
fun BodyCastApp(initialTab: Int = 0, appContext: Context? = null) {
    val backdrop = rememberLayerBackdrop()
    // 页面内容层：单独录一份，只给底栏玻璃采样（页面里的玻璃自己不采这一层，
    // 所以不会成环）。
    val pageBackdrop = rememberLayerBackdrop()

    val store = rememberBodyCastStore()
    val overlay = remember { CastOverlayController() }
    val context = LocalContext.current

    /*
     * 第一次打开时的三页引导（22.0.0 新增）。
     * 看过就不再出现；想重看可以去 设置 → 关于 → 重看新手引导，
     * 那里会把 OnboardingState.visible 置为 true。
     */
    var showOnboarding by remember { mutableStateOf(shouldShowOnboarding(store)) }
    LaunchedEffect(OnboardingState.visible) {
        if (OnboardingState.visible) showOnboarding = true
    }
    /*
     * 22.48.0：数据连接指导（全屏页）。
     *
     * 开箱结束后、且**一条数据通道都没通**、又没勾过「不再提示」时，先弹一次
     * 「要不要连运动数据」；点「开始指导」就打开这一页（三步：系统健康服务 →
     * 小米账号 → 预览已同步数据）。它是主界面之上的一层独立页面，
     * 不是设置里的某个二级菜单——用户第一次遇到它时根本不知道设置里有什么。
     */
    var showDataGuide by remember { mutableStateOf(false) }

    /*
     * 22.51.0：开场动画（BC → BodyCast）改成**每次冷启动都播**。
     *
     * 以前它只在「开箱还没看过」时播一次（`remember { mutableStateOf(showOnboarding) }`），
     * 老用户几乎是直接进主界面。现在固定从 true 起——每次冷启动都先走一遍 3.2s 的
     * 开场，再决定是进开箱还是进主页。
     *
     * 两点必须守住：
     * ① 「重看新手引导」（设置 → 关于）只把 showOnboarding 置真，**不再重播开场**
     *    （那时 showIntro 早已是 false）；
     * ② 启动时的「要不要连运动数据」弹窗等开场结束再问（见下面那个 LaunchedEffect）。
     */
    var showIntro by remember { mutableStateOf(true) }
    var selected by rememberSaveable { mutableIntStateOf(initialTab) }

    /*
     * 换页手感：**立刻换，不等**。
     *
     * 上一版用遮罩做过场（压暗 90ms → 换页 → 亮起 130ms），代价是点下去之后
     * 有两百多毫秒"不响应"的窗口，用户感觉就是「点了要反应一下才滑过去」。
     *
     * 页面在点下去的那一帧就直接换成目标页，中间没有任何过渡；只给新页面
     * 一个极短的「落定」——微缩放 + 一点点淡入，观感像"唰"地凸上来一下。
     * 22.28.0 动画批：落定 130/110ms（FastOutSlowIn，中段慢）压到
     * **120/100ms + M3 emphasized decelerate**——第一下就动起来、尾部轻轻
     * 停住，总视觉时长落在 120–160ms 档的下沿，"快进快出"但不硬切。
     * 时长与开关全走 [CastMotion]。选中滑块在底栏里本来就有自己的滑动动画，
     * 那部分反馈照旧（谷歌皮肤指示器弹簧同步收紧，见 MaterialBottomTabs）。
     */
    // 22.46.0：切页改成**瞬时切换**（与 Kyant0/AndroidLiquidGlass demo 的
    // BottomTabs 示例一致：点击后内容直接替换，不做任何整页过渡）。
    // 此前的 120ms 微缩放 + 淡入虽然"凸一下"好看，但每次切换都要给整棵
    // 页面做一次合成层变换——那一下就是用户感知到的「卡」。
    // 底栏滑块自身的滑动动画不受影响（在 LiquidBottomTabs 组件内部）。
    val switchTo: (Int) -> Unit = { target ->
        if (target != selected) {
            selected = target
        }
    }

    /*
     * 动画总开关：设置里的「动画效果」+ 系统「减少动画」无障碍
     * （ANIMATOR_DURATION_SCALE == 0）取与，写进 [CastMotion] 一处生效。
     * Compose 的 tween/spring 本来就吃系统时长缩放，这里是第二层保险，
     * 也给循环类动画（呼吸/游走）一个统一的判定点。
     */
    SideEffect {
        val reduced = runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
        CastMotion.enabled.value = store.prefs.animationEnabled && !reduced
    }

    /*
     * 跨页的一次性请求（22.24.0）。
     *
     * 「现在去首页看看」（手动填写 / 导入完成之后）与首页的「去手动填写读数」
     * 都要换 tab，而 tab 是这一层持有的状态，所以由这里消费：
     * 前者切到首页，后者切到设置（二级页由设置页自己打开，见 SettingsTab）。
     */
    LaunchedEffect(BodyCastNav.requestHome) {
        if (BodyCastNav.requestHome) {
            switchTo(0)
            BodyCastNav.consumeHome()
        }
    }
    LaunchedEffect(BodyCastNav.requestManualEntry) {
        if (BodyCastNav.requestManualEntry) switchTo(3)
    }
    // 22.30.0「天气 → 元素 → 食物」联动：首页按下「今天吃什么」就切食谱页，
    // 元素筛选由 RecipeTab 组合时消费 requestRecipeFocus 并清掉。
    LaunchedEffect(BodyCastNav.requestRecipeFocus) {
        if (BodyCastNav.requestRecipeFocus != null) switchTo(1)
    }

    // 22.48.0：设置 → 数据与同步 →「连接运动数据」= 打开全屏的授权指导页。
    LaunchedEffect(BodyCastNav.requestDataGuide) {
        if (BodyCastNav.requestDataGuide) {
            showDataGuide = true
            BodyCastNav.consumeDataGuide()
        }
    }

    /*
     * 主题。
     *
     * 判断依据是 prefs.themeMode（0 浅 / 1 深 / 2 跟随系统），**不是** darkMode。
     * 这样「跟随系统」才真的是跟随：LocalConfiguration 在系统切夜间模式时会变，
     * 重组就会重新算一遍。如果只读点击那一刻存下来的 darkMode，
     * 用户把系统调成夜间模式、应用却还亮着——那就不叫跟随了。
     */
    val configuration = LocalConfiguration.current
    val systemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val dark = when (store.prefs.themeMode) {
        0 -> false
        1 -> true
        else -> systemDark
    }
    LaunchedEffect(dark) {
        GlassTheme.setDark(dark)
        // 22.52.0：桌面小组件跟**应用内**的深浅色走。
        // 这里用 prefs 直接算出来的 dark 当准绳——GlassTheme 的状态是异步推过去的，
        // 拿它去写会在冷启动第一帧把旧值写进快照（踩过）。
        appContext?.let { ctx ->
            runCatching {
                WidgetSnapshot.setDark(ctx, dark)
                BodyCastWidgetProvider.updateAll(ctx)
            }
        }
    }

    // 字体大小：设置页的档位直接驱动 GlassTheme 的字号表。
    LaunchedEffect(store.prefs.fontScale) {
        GlassTheme.setFontScale(store.prefs.fontScale)
    }

    /*
     * 桌面小组件：天气/处方一变就把快照写下来并刷新卡片。
     * 信号是内存态，小组件进程读不到，所以必须在这里同步一份最小快照。
     * 22.48.0：键里加上 signals——运动读数（步数 / 中高强度分钟）会改「今天先补什么」，
     * 小组件上的元素名必须跟着一起变，不然桌面显示的和应用里说的不是一回事。
     */
    LaunchedEffect(store.weather, store.prescription, store.signals) {
        appContext?.let { ctx ->
            val weather = store.weather
            val action = store.prescription?.items?.firstOrNull()?.title.orEmpty()
            val focus = cn.pocket.toolbox.bodycast.NutritionBridge.primary(
                weather.kind,
                store.signals.steps,
                store.signals.activityMinutes
            )
            runCatching {
                // 四信号水平词一并进快照（22.35.0 四信号小组件用）。
                val metricKeys = listOf(
                    cn.pocket.toolbox.bodycast.WeatherBodyWeatherKeys.RECOVERY,
                    cn.pocket.toolbox.bodycast.WeatherBodyWeatherKeys.PRESSURE,
                    cn.pocket.toolbox.bodycast.WeatherBodyWeatherKeys.ENERGY,
                    cn.pocket.toolbox.bodycast.WeatherBodyWeatherKeys.SLEEP
                )
                val metricTitles = metricKeys.map {
                    cn.pocket.toolbox.i18n.BodyCastText.text(weather.metric(it)?.title ?: "")
                }
                val metricLevels = metricKeys.map {
                    cn.pocket.toolbox.i18n.BodyCastText.text(
                        weather.metric(it)?.level ?: "暂时没读到"
                    )
                }
                WidgetSnapshot.save(
                    context = ctx,
                    kind = weather.kind.label,
                    motto = weather.kind.motto,
                    headline = weather.broadcast.headline,
                    action = action,
                    focusLabel = focus.label,
                    focusId = focus.name,
                    // 22.49.0：桌面也带上「为什么」和「几分钟」——
                    // 理由与「今天先补什么」同一个口径（含运动读数）。
                    focusReason = cn.pocket.toolbox.i18n.BodyCastText.text(
                        cn.pocket.toolbox.bodycast.NutritionBridge.reason(
                            store.signals.steps,
                            store.signals.activityMinutes
                        )
                    ),
                    actionMinutes = store.prescription?.totalMinutes ?: 0,
                    // 22.52.0：小组件跟应用内的深浅色走（不是只看系统）。
                    // 用 prefs 直接算出来的 dark，不用 SkinTokens.isDark——
                    // 后者由 GlassTheme 异步推过去，冷启动第一帧可能还是旧值。
                    dark = dark,
                    colorHex = weather.kind.colorHex,
                    demo = weather.demo,
                    metricTitles = metricTitles,
                    metricLevels = metricLevels
                )
                BodyCastWidgetProvider.updateAll(ctx)
            }
            // 22.32.0：天气变化提醒——今天 ≠ 昨天才说话，每天最多一条。
            // 昨天没有记录就不发（没有对照就不算「变化」）；演示数据不发。
            if (!weather.demo) {
                runCatching {
                    cn.pocket.toolbox.notify.WeatherChangeNotifier.maybeNotify(
                        context = ctx,
                        todayRecord = store.records[store.todayKey],
                        yesterdayRecord = store.records[store.today.minusDays(1).toString()],
                        // 22.48.0：通知里的「今天先补」跟首页同一个口径。
                        steps = store.signals.steps,
                        activityMinutes = store.signals.activityMinutes
                    )
                }
            }
        }
    }

    // 提醒：应用启动时按当前偏好重排一遍（闹钟重启后会丢，BootReceiver 也会补一次）。
    LaunchedEffect(store.prefs) {
        appContext?.let { ctx ->
            runCatching { Notifier.ensureChannel(ctx) }
            runCatching { ReminderScheduler.scheduleAll(ctx, store.prefs) }
        }
    }

    /*
     * 回前台自动轻量刷新（22.27.0）。
     *
     * 进前台时隔 5 分钟以上才重读一次（[FetchFreshness.AUTO_REFRESH_MS]），
     * 读完更新首页读数与「上次读取」时间。三条边界：
     * - 演示数据模式不自动读——那份整份是假数据，重读没有意义；
     * - 「自动获取」关掉也不读（用户显式设置优先）；
     * - 读在 IO 线程（HealthConnectBridge.read 内部），写状态前等界面空下来：
     *   不在滚动中（[UiMotion]）整页重排，回场动画先落定（400ms）。
     */
    val refreshScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val owner = context as? android.app.Activity
        val observer = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(
                source: androidx.lifecycle.LifecycleOwner,
                event: androidx.lifecycle.Lifecycle.Event
            ) {
                if (event != androidx.lifecycle.Lifecycle.Event.ON_RESUME) return
                val prefs = store.prefs
                if (prefs.demoMode || !prefs.autoFetch) return
                if (!FetchFreshness.shouldAutoRefresh(prefs.lastFetchAt)) return
                refreshScope.launch {
                    kotlinx.coroutines.delay(400)
                    val signals = HealthConnectBridge.read(context)
                    while (UiMotion.busy) kotlinx.coroutines.delay(100)
                    store.updateSignals(signals)
                }
            }
        }
        (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.addObserver(observer)
        onDispose {
            (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.removeObserver(observer)
        }
    }

    // 状态栏图标色跟随深浅主题（已知缺陷修复）：主题档位一变就重新套一次系统栏样式。
    val barActivity = LocalContext.current as? androidx.activity.ComponentActivity
        ?: appContext as? androidx.activity.ComponentActivity
    val isDarkNow = SkinTokens.isDark
    LaunchedEffect(isDarkNow) {
        barActivity?.let { applySystemBars(it) }
    }

    /*
     * 版式保护层：`Material3` 的 `Text`（[cn.pocket.toolbox.ui.bodycast.Text] 底层）
     * 会读 `LocalTextStyle` / `LocalIndication` / `LocalTextSelectionColors`。
     * 22 个版本排好的版式不能被 M3 的正文样式（letterSpacing / lineHeight 24sp）
     * 渗进去，所以这里显式把当前值原样 provide 回去。
     *
     * 22.31.0 起只有一套界面（液态玻璃）：原先的「谷歌 Material 3」皮肤、
     * `ui/google/` 包与皮肤切换点一起删除；这里的 `MaterialTheme` 包装也一并
     * 去掉了——不再需要「换主题再还回去」的对冲结构。
     */
    CompositionLocalProvider(
        LocalCastOverlay provides overlay
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(SkinTokens.bg)
        ) {
            /*
             * 环境层：**只有背景装饰进这一层**。
             *
             * 这里有一个必须守住的约束，踩过一次：
             * `layerBackdrop` 会把所挂节点的整棵子树录成一个渲染节点，
             * 供玻璃元素采样。如果**玻璃元素自己也在这层里**，它就会采样到
             * 「包含它自己」的那一层 —— 渲染节点树里出现环，RenderThread 在
             * prepareTreeImpl 里无限递归，512 层后栈溢出，表现为启动即
             * SIGSEGV（不是 Kotlin 异常，编译期完全看不出来）。
             *
             * 所以「页面里的玻璃卡片」只采样这一层环境渐变；页面内容另录一层
             * （pageBackdrop，见下面页面层的注释），由底栏玻璃去采样——
             * 底栏是页面层的兄弟节点、不在其中，不构成环。
             *
             * 谷歌皮肤：没有玻璃元素会采样这一层，所以**只是不再挂**
             * `layerBackdrop`，层的结构、挂载顺序、变量定义一字未动——
             * 不动结构就不会重现上面那个成环条件。
             */
            Box(
                Modifier
                    .fillMaxSize()
                    .then(Modifier.layerBackdrop(backdrop))
                    // 22.16.0：底色必须录进这一层。玻璃元素把"背后的画面"录一份
                    // 重画在自己位置上；录制里缺了不透明底色时，副本=半透明，
                    // 屏幕原本的内容就会从玻璃里透出来（用户报告的"玻璃里显示
                    // 没折射过的文字/框"，且透明区明显、实心处不可见）。
                    // 补上底色后，录制结果=屏幕本身，未折射区域是真正的隐形照抄。
                    .background(SkinTokens.bg)
            ) {
                // 唯一的结构分支，放在叶子：谷歌皮肤是 M3 的平涂底色，不画环境光斑。
                AmbientBackground()
            }

            /*
             * 页面内容层。**不加**底部 padding —— 内容要能从玻璃栏底下滑过去，
             * 底部留白由各页面的 CastPage 自己给。
             *
             * 这里挂的就是「落定」效果：换页是瞬时的（见 switchTo），
             * 新页面只做一个 130ms 的微缩放 + 短淡入，观感像"凸"上来一下，
             * 不会推迟内容出现。
             *
             * pageBackdrop 录的是整棵页面子树：底栏玻璃靠它把「玻璃后面的
             * 页面内容」真实地模糊、弯折映进玻璃（21.1.0 起的效果）。
             * 环的约束在这里天然成立：页面内部的玻璃卡片只采样上面的环境层
             * （backdrop），不碰这一层；只有底栏（画在页面层之后的兄弟）
             * 才采样它。两个采样层各录一份全屏内容，代价是多一块显存与
             * 一次合成，换来底栏终于能「反射」身后的页面。
             */
            // 22.49.0：开箱结束后的「落地」——整页从 0.97 倍 + 半透明弹到正常。
            // 变换包在录制层**外面**（渲染节点树里是父节点），不会让 pageBackdrop
            // 逐帧重录；底栏采样的仍是里面那一层。
            val reveal = remember { Animatable(1f) }
            LaunchedEffect(showIntro, showOnboarding) {
                if (!showIntro && !showOnboarding) {
                    reveal.snapTo(0.97f)
                    reveal.animateTo(1f, androidx.compose.animation.core.tween(540, easing = CastMotion.Emphasized))
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = reveal.value
                        scaleX = p
                        scaleY = p
                        alpha = ((p - 0.97f) / 0.03f).coerceIn(0f, 1f)
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // 22.43.0：settle 变换曾外置到此层之外；22.46.0 起切换改为瞬时
                        // （demo 写法），不再有任何整页变换——恢复单层结构：
                        // 录制层即内容层，底栏照常采样这一层做折射。
                        .then(Modifier.layerBackdrop(pageBackdrop))
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    PageContent(selected, store, backdrop, switchTo)
                }
            }

            /*
             * 悬浮玻璃底部栏。
             */
            LiquidBottomTabs(
                selectedTabIndex = { selected },
                onTabSelected = { switchTo(it) },
                // 与官方 demo 同构：backdrop 就是「底栏背后整片内容」的录制层。
                // 这里 = 环境渐变层 + 页面内容层两层合并（页面里的玻璃卡片
                // 只采环境层，不成环；底栏是页面层的兄弟，采它安全）。
                backdrop = rememberCombinedBackdrop(backdrop, pageBackdrop),
                tabsCount = TABS.size,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = SkinTokens.pagePadding,
                        end = SkinTokens.pagePadding,
                        bottom = SkinTokens.bottomBarBottomMargin
                    )
            ) {
                TABS.forEachIndexed { index, tab ->
                    // 官方做法（22.49.0 起严格照抄）：**四个 tab 用同一个内容色**，
                    // 选中态的强调色由滑块的「染色副本层」（ColorFilter.tint(accent)）
                    // 单独给出——底座那一行自己不区分选中/未选中。
                    // 以前这里按 selected 换色，会和副本层的染色叠在一起
                    // （选中项看起来发虚、发亮、像描了两遍）。
                    LiquidBottomTab(onClick = { switchTo(index) }) {
                        GlassIcon(
                            kind = tab.icon,
                            tint = SkinTokens.ink,
                            modifier = Modifier.size(24.dp)
                        )
                        BasicText(
                            text = BodyCastText.text(tab.label),
                            style = TextStyle(
                                color = SkinTokens.ink,
                                // 22.44.0：底栏文字跟着「字体大小」设置走
                                //（此前 12.sp 写死，调字号时四栏文字纹丝不动）。
                                fontSize = (12f * GlassTheme.fontScale).sp
                            )
                        )
                    }
                }
            }

            // 22.48.0：数据连接指导（全屏页）。画在浮层**之前**——页面里的
            // 「断开并删除令牌」等浮层仍然盖在它上面；画在底栏**之后**，所以
            // 打开时整屏被它盖住，看起来就是独立的一页，不是换了个 tab。
            androidx.compose.animation.AnimatedVisibility(
                visible = showDataGuide,
                enter = fadeIn(
                    androidx.compose.animation.core.tween(200, easing = CastMotion.Decelerate)
                ) + slideInHorizontally(
                    androidx.compose.animation.core.tween(320, easing = CastMotion.Decelerate)
                ) { it / 4 },
                exit = fadeOut(
                    androidx.compose.animation.core.tween(180, easing = CastMotion.Accelerate)
                ) + slideOutHorizontally(
                    androidx.compose.animation.core.tween(260, easing = CastMotion.Accelerate)
                ) { it / 4 }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(SkinTokens.bg)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    DataGuidePage(
                        backdrop = backdrop,
                        store = store,
                        onBack = { showDataGuide = false }
                    )
                }
            }

            // 浮层：同样是内容层的兄弟节点（不是子节点，所以不会破坏 backdrop 采样），
            // 但必须画在底部栏**之后**。浮层是模态，要盖住导航栏；
            // 顺序反了的话底栏会压在浮层上面，还能隔着浮层点到 tab——
            // 一边切页面一边浮层不关，交互是错的。
            CastOverlayHost(backdrop = backdrop, controller = overlay)

            // 分享面板送进来的文件（22.24.0）：解析之后弹一张确认卡，
            // 用户点「写入」才落盘。放在浮层之后，所以确认卡自然盖在浮层之上。
            ImportInboxHost(backdrop = backdrop, store = store, overlay = overlay)

            // 开场动画：每次冷启动都播一遍（showIntro 已在上面声明）。
            if (showIntro) {
                IntroOverlay(onDone = { showIntro = false })
            }
            // 22.47.0：开箱层用 AnimatedVisibility 包裹——「入场动画 → 开箱第一页」
            // 与「完成 → 主界面」两处过渡共用一套：淡入 + 轻微上浮 / 淡出 + 轻微上移。
            androidx.compose.animation.AnimatedVisibility(
                visible = !showIntro && showOnboarding,
                enter = androidx.compose.animation.fadeIn(
                    androidx.compose.animation.core.tween(340, easing = CastMotion.Decelerate)
                ) + androidx.compose.animation.slideInVertically(
                    androidx.compose.animation.core.tween(380, easing = CastMotion.Decelerate)
                ) { it / 16 },
                // 22.49.0：末页「立即开始」之后要有一段落得住的入场——
                // 开箱层淡出 + 轻轻放大（像卡片往屏幕里推开），主界面同时从
                // 0.97 倍弹回 1（见下面的 reveal），两边错开 80ms 交叠，不硬切。
                exit = androidx.compose.animation.fadeOut(
                    androidx.compose.animation.core.tween(460, easing = CastMotion.Accelerate)
                ) + androidx.compose.animation.scaleOut(
                    androidx.compose.animation.core.tween(520, easing = CastMotion.Decelerate),
                    targetScale = 1.06f
                )
            ) {
                // 新手指引盖在一切之上（包括浮层）：第一次打开时先把它看完。
                OnboardingOverlay(
                    backdrop = backdrop,
                    store = store,
                    animate = store.prefs.animationEnabled,
                    onDone = {
                        markOnboardingDone(store)
                        showOnboarding = false
                        OnboardingState.visible = false
                    }
                )
            }

            /*
             * 22.48.0 启动提示：开箱结束之后、数据一条都不通时问一次「要不要连运动数据」。
             *
             * 判定条件（三条都要满足才会弹）：
             * ① 没勾过「不再提示」；
             * ② 小米账号没连上——连上了数据就已经在手里，不该再打扰；
             * ③ 系统健康服务这一条也没通（没授权，或授权了但一类都读不到）。
             *
             * 手机得先醒过来：延迟 900ms 再弹，避免和「开箱 → 首页」的过渡抢镜头。
             * 自检要读系统健康服务（IO），失败就当「没通」处理——宁可不弹，也不能拿
             * 一个异常去骚扰用户。
             *
             * 一次启动只问一次（[guidePrompted]）：用户在设置里连了又断、或者授权状态
             * 变了，都不该在同一次使用里被问第二遍——「每次打开软件弹一次」就是字面意思。
             */
            var guidePrompted by remember { mutableStateOf(false) }
            LaunchedEffect(
                showIntro,
                showOnboarding,
                store.prefs.dataGuideNever,
                store.prefs.miCloudConnected
            ) {
                if (showIntro || showOnboarding || guidePrompted) return@LaunchedEffect
                if (store.prefs.dataGuideNever || store.prefs.miCloudConnected) return@LaunchedEffect
                kotlinx.coroutines.delay(900)
                val diagnosis = runCatching { HealthConnectBridge.diagnose(context) }.getOrNull()
                // 授权通了、或者已经读到了数据，就不再问。
                if (diagnosis != null &&
                    (diagnosis.readCount > 0 ||
                        diagnosis.grantState == HealthConnectBridge.StepState.OK)
                ) return@LaunchedEffect
                guidePrompted = true

                fun rememberChoice(checked: Boolean) {
                    if (checked) store.updatePrefs { it.copy(dataGuideNever = true) }
                }

                overlay.showDialogWithCheckbox(
                    title = "连接你的运动数据",
                    message = "BodyCast 读到你自己的运动数据，才说得出今天是什么天气。点「开始指导」走三步：授权系统健康服务、连接小米账号、确认数据真的进来了。不连也能用——每天的读数要自己填。",
                    checkboxLabel = "不再提示",
                    confirmLabel = "开始指导",
                    cancelLabel = "忽略",
                    onConfirm = { checked ->
                        rememberChoice(checked)
                        showDataGuide = true
                    },
                    onCancel = { checked -> rememberChoice(checked) }
                )
            }
        }
    }
}

/**
 * 页面内容分发（22.15.0 抽出）。
 *
 * 四个页面共用一份代码，配色与形状统一由 [cn.pocket.toolbox.ui.skin.SkinTokens]
 * 门面取值，这里不做任何分支。
 */
@Composable
private fun PageContent(
    selected: Int,
    store: BodyCastStore,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    switchTo: (Int) -> Unit
) {
    when (selected) {
        0 -> HomeTab(
            backdrop = backdrop,
            store = store,
            onOpenEcho = { switchTo(2) }
        )
        1 -> RecipeTab(backdrop = backdrop, store = store)
        2 -> EchoTab(
            backdrop = backdrop,
            store = store,
            onOpenSettings = { switchTo(3) }
        )
        3 -> SettingsTab(backdrop = backdrop, store = store)
        else -> HomeTab(
            backdrop = backdrop,
            store = store,
            onOpenEcho = { switchTo(2) }
        )
    }
}

/**
 * 柔和的背景装饰。
 *
 * 玻璃的折射需要有东西可折——纯色背景上，玻璃看起来就只是一块半透明色板。
 * 深色下用低饱和的大色块 + 径向渐变，既有东西可折，又不会喧宾夺主。
 * 色块取的是五种身体天气的颜色，所以背景本身也在暗示「天气」这件事。
 *
 * 只有玻璃皮肤会组合它（谷歌皮肤的底是 M3 的平涂 surface 色）。
 */
@Composable
private fun AmbientBackground() {
    Box(Modifier.fillMaxSize()) {
        Blob(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 10.dp),
            size = 300.dp,
            center = Color(0x4D3ED598),
            edge = Color(0x000E3B2B)
        )
        Blob(
            modifier = Modifier
                .align(Alignment.BottomStart)
                // 向下偏移 80dp：让色块 bleed 出屏幕底缘，穿过底栏正后方。
                // （Compose 的 padding 不允许负值，bleed 用 offset 做。）
                // 底栏的玻璃只折射这一层环境渐变——色块断在底栏上方时，
                // 它的边缘会在玻璃里露出一条横线；延伸过去之后采样到的
                // 才是一段连续渐变。
                // 20.0.0：色块浓度调高了一档（0x4D → 0x66）。底栏要做出
                // 「液态玻璃自带的边缘畸变」，透镜必须先有东西可折——
                // 背景太平滑时折射再强也看不出来。
                .offset(y = 80.dp),
            size = 340.dp,
            center = Color(0x6656B8F0),
            edge = Color(0x00101F33)
        )
        Blob(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // 同样压进底栏区域：淡紫色块与蓝紫色块在底栏中央交汇，
                // 交界线会被底栏玻璃的透镜真实地弯折（配合色散形成可见的折射），
                // blur 把它抹成彩色的雾——液态玻璃的「折」和「糊」都来自这条交界。
                // 20.0.0：同样调浓（0x40 → 0x59），让交界线在栏内更清楚。
                .offset(y = 60.dp),
            size = 300.dp,
            center = Color(0x599B8CC4),
            edge = Color(0x001E1740)
        )
        // 22.6.0：底栏胶囊的两端也要有东西可折。上面两个大色块的边界
        // 只穿过底栏中段，两端全是平滑渐变，折射在两端「无米下锅」，
        // 看起来就是最边上没有拉伸。这两个小色块的圆弧边界正好压在
        // 底栏左/右两端的取样带上（透镜从栏外约 30dp 处取内容）。
        // 22.7.0：加浓（0x33 → 0x4D）并重定位，让圆弧边界确实落在
        // 胶囊两端的取样带里。
        Blob(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -50.dp, y = 40.dp),
            size = 140.dp,
            center = Color(0x4D3ED598),
            edge = Color(0x000E3B2B)
        )
        Blob(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 40.dp),
            size = 140.dp,
            center = Color(0x4D9B8CC4),
            edge = Color(0x001E1740)
        )
        Blob(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 160.dp),
            size = 200.dp,
            center = Color(0x2EE8A33D),
            edge = Color(0x001F1808)
        )
    }
}

@Composable
private fun Blob(
    modifier: Modifier,
    size: Dp,
    center: Color,
    edge: Color
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = listOf(center, edge)))
    )
}
