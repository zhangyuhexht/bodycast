package cn.pocket.toolbox.ui.bodycast

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.bodycast.ActionLibrary
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.BodyWeather
import cn.pocket.toolbox.bodycast.EchoGenerator
import cn.pocket.toolbox.bodycast.FetchFreshness
import cn.pocket.toolbox.bodycast.NutritionBridge
import cn.pocket.toolbox.bodycast.WeatherBodyWeatherKeys
import cn.pocket.toolbox.data.HealthConnectBridge
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.glass.GlassRow
import cn.pocket.toolbox.ui.skin.SkinTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * 页面一：首页（8.0.0 起）。
 *
 * 排版是「三段式、一个主角」，从上到下先给状态、再给行动、最后给工具：
 * 1. 页头与横幅（演示数据 / 没读到数据），然后是身体 hero——全页唯一
 *    有颜色的一张卡：四个信号（只给词）+ 气象球 + 今天是什么天气 +
 *    播报正文。每条指标的 hint 不铺在卡面上，收进 hero 右上角的
 *    「身体信号怎么看」浮层。
 * 2. 今天能做点什么：今日微行动（默认折叠，副标题就是「今天可以先做
 *    这一件」）与快速校准（「清空校准」挪到区块标题右侧）。
 * 3. 想再要一点的时候才用的工具箱（听 / 诗 / 依据）、身体回声入口，
 *    页脚是倾听次数。
 *
 * 三组之间多留 8dp（见 [CastPage] 里插入的分隔 item），组内沿用
 * 页面本身的 10dp 间距。
 *
 * 8.0.0 之前这一页有一个「长按光球 3 秒完成握手」的首次流程——
 * 用户要求去掉了，现在打开应用直接就是以上内容。原来的握手页、
 * 3 秒计时和「等不及了」入口都已删除。
 *
 * 产品铁律不变：没有评分、没有排名、没有卡路里、没有体重；
 * 四项指标只给词不给数字；数据缺失就写「暂时没读到」；
 * 手动校准的优先级永远高于系统自动判断。
 */

/**
 * 页面签名与 MainActivity 的调用保持一致，不要改。
 *
 * 注意：这里**不再有冷启动的开屏等待页**（原来会显示「正在连接你的身体信号…」
 * 约 2.4 秒）。用户明确要求直接进主界面，所以整段 splash 状态和动画都删掉了。
 */
@Composable
fun HomeTab(
    backdrop: Backdrop,
    store: BodyCastStore,
    onOpenEcho: () -> Unit
) {
    HomeContent(
        backdrop = backdrop,
        store = store,
        onOpenEcho = onOpenEcho
    )
}

// ---------------------------------------------------------------------------
// 主内容
// ---------------------------------------------------------------------------

@Composable
private fun HomeContent(
    backdrop: Backdrop,
    store: BodyCastStore,
    onOpenEcho: () -> Unit
) {
    val overlay = LocalCastOverlay.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 打开首页 = 倾听了一次身体。这个计数只在这里增加。
    LaunchedEffect(Unit) { store.markListened() }

    // 微行动默认折叠，PrescriptionSection 首屏不会组合，它内部那句
    // 「进来没处方就现开一张」也就没人执行——而小组件快照和微行动卡的
    // 「今天可以先做这一件」都读 store.prescription，所以在这里补开一张。
    LaunchedEffect(Unit) { if (store.prescription == null) store.issuePrescription() }

    // weather 是 getter，每次现算。在这里读一次，等于订阅了
    // signals / calibrated / customCalibrated / daySeed 四个状态，
    // 所以点了校准标签，气象球和播报会立刻跟着变，不需要额外处理。
    val weather = store.weather
    val signals = store.signals
    val prescription = store.prescription

    var scanning by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    // 22.53.0：播报改成一天一读，且再点一次能停——所以要看得见「正在播报」。
    var speaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    // 22.49.0：语音不可用时**不再把入口藏起来**——记住原因，界面上如实说清楚，
    // 并且给一个「打开系统语音设置」的出路（真机反馈：藏掉之后用户只看到
    // 「语音播报用不了」，既不知道原因也没法修）。
    var ttsNote by remember { mutableStateOf<String?>(null) }

    val speechLanguage = AppLanguage.current()
    DisposableEffect(context, speechLanguage) {
        val holder = arrayOfNulls<TextToSpeech>(1)
        val engine = TextToSpeech(context) { status ->
            val instance = holder[0] ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                ttsReady = false
                ttsNote = "本机没有可用的语音引擎"
                return@TextToSpeech
            }
            // 候选表逐个试（zh-Hans → zh_CN → zh…）：直接把带脚本的 tag
            // 交给引擎，很多机器会被判成「不支持」，这就是「用不了」的根因。
            val picked = Speech.pick(instance, speechLanguage)
            if (picked == null) {
                ttsReady = false
                ttsNote = "这台设备缺少当前语言的语音包"
            } else {
                ttsReady = true
                ttsNote = null
            }
            instance.setOnUtteranceProgressListener(
                object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        speaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        speaking = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        speaking = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        speaking = false
                    }
                }
            )
        }
        holder[0] = engine
        tts = engine
        onDispose {
            ttsReady = false
            speaking = false
            tts = null
            runCatching { engine.stop() }
            engine.shutdown()
        }
    }

    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "早上好"
        in 12..17 -> "下午好"
        else -> "晚上好"
    }
    // 22.49.0：开箱里问过「怎么称呼你」，这里把它接上——首页第一行就是问候。
    val nickname = store.prefs.nickname
    val greetingLine = if (nickname.isBlank()) greeting else BodyCastText.text("$greeting，$nickname")
    val language = AppLanguage.current()
    val dateLabel = remember(store.today, language) { BodyCastText.formatDate(store.today, language) }

    // 重新扫描（右上角按钮）与「上次读取」那行的刷新是同一条路，不写两份。
    val rescan: () -> Unit = {
        if (!scanning) {
            scanning = true
            scope.launch {
                // read 内部已经把异常吞掉了，读不到就返回空实例，
                // 页面会照实写「暂时没读到」，不会编一个数出来。
                store.updateSignals(HealthConnectBridge.read(context))
                scanning = false
            }
        }
    }

    // 「今天可以先做这一件」：优先读今天处方里的第一件；处方还没开出来时，
    // 退回按天气挑的第一件——和原来播报卡的取法完全一致。
    val suggestion = prescription?.items?.firstOrNull()?.title
        ?: ActionLibrary.forWeather(weather.kind).firstOrNull()?.title

    CastPage {
        // ---- 顶部：日期 + 问候语 + 重新扫描 ----
        item {
            CastPageHeader(
                title = greetingLine,
                subtitle = dateLabel,
                trailing = {
                    CastIconButton(
                        icon = "reset",
                        label = if (scanning) "正在读…" else "重新扫描",
                        onClick = rescan,
                        backdrop = backdrop,
                        tint = if (scanning) SkinTokens.muted else SkinTokens.accent
                    )
                }
            )
        }

        // ---- 演示数据必须打标 ----
        if (weather.demo) {
            item {
                CastBanner(
                    text = "当前是演示数据。它用来展示完整效果，不代表你的身体。",
                    backdrop = backdrop,
                    icon = "info",
                    tint = SkinTokens.cMood
                )
            }
        }

        // ---- 一个数都没读到时的说明（不催促，只指路）----
        if (!signals.hasAnything) {
            item {
                CastBanner(
                    text = "还没有读到数据。可以照着手环 App 里的数字手动填写读数，或者直接用手动校准——" +
                        "手动校准的优先级永远高于系统自动判断。",
                    backdrop = backdrop,
                    icon = "info",
                    tint = SkinTokens.cWater
                )
                Spacer(Modifier.height(10.dp))
                CastIconButton(
                    icon = "heart",
                    label = "用手动校准就好",
                    onClick = {
                        overlay.show(
                            title = "用手动校准就好",
                            subtitle = "在下面勾选你现在的感觉就够了。"
                        ) {
                            CastHint("不需要设备，也不需要授权。往下找到「我现在的感觉是——」。")
                            CastHint("勾完之后，上面的天气球和播报会立刻跟着变。")
                            CastHint("你说是，就是。手动校准的优先级永远高于系统自动判断。")
                        }
                    },
                    backdrop = backdrop,
                    tint = SkinTokens.accent
                )
                Spacer(Modifier.height(8.dp))
                // 零权限、零依赖的那条路：照着手环 App 里的数字抄进来。
                // 这条和手动校准不一样——它填的是读数，不是结论。
                CastIconButton(
                    icon = "type",
                    label = "去手动填写读数",
                    onClick = {
                        overlay.show(
                            title = "手动填写读数",
                            subtitle = "打开手环 App 看到数字，照着抄进来就行。"
                        ) {
                            CastHint("不需要设备，不需要授权，也不需要 Health Connect。")
                            CastHint("只填你认识的那几项，其余留空——留空不会当成 0。")
                            CastHint("填完 24 小时内有效，之后就不再参与今天的天气判定。")
                            Spacer(Modifier.height(4.dp))
                            CastPrimaryButton(
                                text = "现在去填",
                                onClick = {
                                    overlay.dismiss()
                                    BodyCastNav.openManualEntry()
                                },
                                backdrop = backdrop
                            )
                        }
                    },
                    backdrop = backdrop,
                    tint = SkinTokens.accent
                )
            }
        }

        // ---- 22.50.0：今天的数据（老师意见：「数据是客观值，应该放在前面」）----
        // 六类读数原样摆出来（取不到就写「没读到」），下面才是被翻译过的那句结论。
        // 这张卡是**事实陈列**：不带评分、不带达标线，页脚写明这一点。
        item { TodayDataCard(backdrop = backdrop, store = store) }

        // ---- 身体 hero：全页唯一的彩色块 ----
        // 四个信号 + 气象球 + 播报合成一张卡：打开软件先看到「现在是什么状态」，
        // 紧接着就是「今天是什么天气」和「这句话怎么来的」，中间不再被别的卡切断。
        item { BodyHeroCard(backdrop = backdrop, store = store, weather = weather) }

        // 22.32.0：四个信号的紧凑横带——从 hero 里搬出来放它后面，
        // 首屏只留「天气 + 结论 + 主按钮」，早晨一眼看得完。
        item { SignalStrip(backdrop = backdrop, store = store, weather = weather) }

        // ----「上次读取」+ 过期提示（22.27.0）----
        // 超过 30 分钟没读就如实说「数据可能过期，点此刷新」，点这一行走重新扫描同一条路；
        // 时间戳用 entryStamp 的中文源串写法，翻译交给「今天 HH:mm」那套模板。
        item {
            val at = store.prefs.lastFetchAt
            val stale = FetchFreshness.isStale(at)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = rescan)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = if (at <= 0L) "还没有读取过" else "上次读取：" + entryStamp(at),
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted
                )
                if (stale) {
                    Text(
                        text = "数据可能过期，点此刷新",
                        fontSize = SkinTokens.caption,
                        color = SkinTokens.accent
                    )
                }
            }
        }

        // 组与组之间多留 8dp：① 页头/横幅/hero ② 微行动入口/校准 ③ 工具箱/回声/页脚
        item { Spacer(Modifier.height(8.dp)) }

        // ---- 今日微行动（22.49.0：整块搬到回声页，这里只留一行入口）----
        // 用户连着三次说「微行动不在回声页面」——首页只留一行指路，
        // 真正要做的那张卡（处方、时段、倒计时、语音引导）在回声页顶部。
        item {
            GlassActionRow(
                backdrop = backdrop,
                title = "今日微行动",
                icon = "bolt",
                subtitle = if (suggestion != null) {
                    "在回声页：今天可以先做这一件：$suggestion"
                } else {
                    "在回声页：几分钟能做完的一件小事。"
                },
                shape = RoundedCornerShape(SkinTokens.cardRadius),
                onClick = onOpenEcho
            )
        }

        // ---- 快速校准 ----
        item {
            CastSectionHeader(
                title = "我现在的感觉是——",
                subtitle = "勾完立刻生效，不用点保存。",
                trailing = {
                    CastIconButton(
                        icon = "reset",
                        label = "清空校准",
                        onClick = { store.clearAllCalibration() },
                        backdrop = backdrop,
                        tint = SkinTokens.muted
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
            GlassChipFlow(
                items = store.visibleCalibrationTags,
                backdrop = backdrop,
                label = { it.label },
                isSelected = { store.calibrated.contains(it) },
                onToggle = { store.toggleCalibration(it) }
            )
            if (store.visibleCustomLabels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                GlassChipFlow(
                    items = store.visibleCustomLabels,
                    backdrop = backdrop,
                    label = { it },
                    isSelected = { store.customCalibrated.contains(it) },
                    onToggle = { store.toggleCustomCalibration(it) },
                    accent = SkinTokens.cSleep,
                    localizeLabels = false
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // ---- 页尾工具箱：听、诗、依据 ----
        item {
            ToolboxCard(
                backdrop = backdrop,
                weather = weather,
                tts = tts,
                ttsReady = ttsReady,
                ttsNote = ttsNote,
                speaking = speaking,
                suggestion = suggestion,
                focusSteps = store.signals.steps,
                focusMinutes = store.signals.activityMinutes
            )
        }

        // ---- 身体回声入口：跳到第三个 tab ----
        item {
            val calendar = remember(store.records, store.today) {
                EchoGenerator.weekCalendar(store.records, store.today)
            }

            CastCard(
                backdrop = backdrop,
                onClick = onOpenEcho
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "身体回声",
                            fontSize = SkinTokens.cardTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = SkinTokens.ink
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "这一周的身体天气、来信和趋势，都在回声页。",
                            fontSize = SkinTokens.caption,
                            lineHeight = 23.sp,
                            color = SkinTokens.muted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GlassIcon("next", SkinTokens.muted, Modifier.size(18.dp))
                }
                Spacer(Modifier.height(18.dp))
                WeekWeatherStrip(
                    days = calendar.days,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---- 倾听次数（不叫打卡）：全页最后一项 ----
        item {
            Text(
                text = "今日已倾听身体 ${store.listenCount} 次。",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                fontSize = SkinTokens.caption,
                color = SkinTokens.faint,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 身体 hero
// ---------------------------------------------------------------------------

/** 首页四项指标的静态描述：key、缺数据时的名字、图标、颜色。 */
private class MetricSpec(
    val key: String,
    val fallbackTitle: String,
    val icon: String,
    val tint: Color
)

/**
 * 身体 hero：全页唯一有颜色的一张卡（22.32.0「6 秒首屏」重排）。
 *
 * 从上到下一条线：今天是什么天气（气象球 + 天气名 + 播报正文）→
 * 这句话怎么来的（校准时的那句 headline、部分信号时的覆盖说明）→
 * 今天先补什么 + 主按钮（「天气 → 元素 → 食物」的首页入口）。
 *
 * 四个信号不再占 hero——收成 [SignalStrip] 紧凑横带放在 hero 之后，
 * 早晨打开一眼看得完首屏，信息一条不少，只是换层。
 */
@Composable
private fun BodyHeroCard(
    backdrop: Backdrop,
    store: BodyCastStore,
    weather: BodyWeather
) {
    CastCard(
        backdrop = backdrop,
        tint = Color(weather.kind.colorHex).copy(alpha = 0.18f)
    ) {
        // 22.32.0「6 秒首屏」：气象球提到最上——打开先看见「今天是什么天气」，
        // 结论、主按钮、今天补什么依次往下；四个信号不再占 hero，
        // 收成一张紧凑横带放在 hero 之后（信息一条不少，只换层）。
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeatherOrb(kind = weather.kind, diameter = 172.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "今天的身体天气 · ${weather.kind.label}",
                fontSize = SkinTokens.cardTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = weather.broadcast.body,
                fontSize = SkinTokens.body,
                lineHeight = 28.sp,
                color = SkinTokens.ink,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            // 部分信号可用时如实说明「今天是按 N/4 个信号算的」（22.27.0）。
            // 缺的那几路不补数、不隐藏，就写「暂时没读到」。
            weather.broadcast.coverage?.let { note ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note,
                    fontSize = SkinTokens.caption,
                    lineHeight = 20.sp,
                    color = SkinTokens.muted,
                    textAlign = TextAlign.Center
                )
            }
            // 22.50.0：把「综合」写成一行公式——客观读数几个、主观感受几个，
            // 一句结论就是这两样合出来的（老师说的「数据 + 感受综合去分析」）。
            val signalCount = listOf(
                store.signals.restingHeartRate,
                store.signals.hrv,
                store.signals.sleepMinutes,
                store.signals.steps
            ).count { v -> v != null }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "结论 = 客观读数（" + signalCount + "/4 个信号）+ 你的感受（" +
                    weather.calibrated.size + " 个标签）",
                fontSize = SkinTokens.caption,
                lineHeight = 20.sp,
                color = SkinTokens.muted,
                textAlign = TextAlign.Center
            )
            // 手动校准过的时候，补一句结论标题——「你说是，就是」这件事要看得见。
            if (weather.calibrated.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = weather.broadcast.headline,
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    color = SkinTokens.muted,
                    textAlign = TextAlign.Center
                )
            }
            // 「天气 → 元素 → 食物」闭环的首页入口：一句结论 + 一个主按钮。
            // 22.48.0：运动读数（今日步数 / 中高强度分钟）也进来——动得多的一天，
            // 结论换成运动口径，并把「为什么」写成一句带数字的话。
            Spacer(Modifier.height(12.dp))
            val steps = store.signals.steps
            val activeMinutes = store.signals.activityMinutes
            val focus = NutritionBridge.primary(weather.kind, steps, activeMinutes)
            Text(
                text = "今天先补：${focus.label}",
                fontSize = SkinTokens.cardTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink,
                textAlign = TextAlign.Center
            )
            NutritionBridge.reason(steps, activeMinutes).takeIf { it.isNotEmpty() }?.let { why ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = why,
                    fontSize = SkinTokens.caption,
                    lineHeight = 20.sp,
                    color = SkinTokens.muted,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(10.dp))
            CastPrimaryButton(
                text = "今天吃什么 · 去找含${focus.label}的食物",
                onClick = { BodyCastNav.openRecipe(focus) },
                backdrop = backdrop,
                icon = "leaf"
            )
            Spacer(Modifier.height(4.dp))
            CenteredHint("食谱页已经按今天的身体天气筛好了，进去就是含${focus.label}的食物。")
        }
    }
}

/**
 * 四个信号的紧凑横带（22.32.0）：一格一个小图标 + 名称 + 词。
 *
 * 22.30.0 之前它们是 hero 里的 2×2 大格，把「今天是什么天气」挤到了第二屏；
 * 现在 hero 收短、这四格合成一条横带放它后面——早晨一眼看不完的信息往下放，
 * 一条不少。点右上角 info 仍是完整的四项说明浮层。
 */
@Composable
private fun SignalStrip(
    backdrop: Backdrop,
    store: BodyCastStore,
    weather: BodyWeather
) {
    val overlay = LocalCastOverlay.current
    // 22.51.0（老师意见「更通俗」）：横带上写**平实词**——恢复 / 压力 / 精力 / 睡眠，
    // 天气隐喻（恢复云量 / 压力风速 / 能量湿度 / 睡眠云层）留在 ⓘ 浮层里，
    // 那里本来就有每一格的解释。隐喻不丢，只是不再挡在第一眼。
    val specs = listOf(
        MetricSpec(WeatherBodyWeatherKeys.RECOVERY, "恢复", "moon", SkinTokens.cSleep),
        MetricSpec(WeatherBodyWeatherKeys.PRESSURE, "压力", "speed", SkinTokens.muted),
        MetricSpec(WeatherBodyWeatherKeys.ENERGY, "精力", "bolt", SkinTokens.cMood),
        MetricSpec(WeatherBodyWeatherKeys.SLEEP, "睡眠", "clock", SkinTokens.cWater)
    )

    CastCard(backdrop = backdrop) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "现在 · 四个信号",
                modifier = Modifier.weight(1f),
                fontSize = SkinTokens.caption,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.muted
            )
            CastIconButton(
                icon = "info",
                label = null,
                onClick = {
                    overlay.show(
                        title = "身体信号怎么看",
                        subtitle = "恢复、压力、能量、睡眠分别说什么；没有分数，也没有及格线。"
                    ) {
                        specs.forEach { spec ->
                            val metric = weather.metric(spec.key)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(spec.tint.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GlassIcon(spec.icon, spec.tint, Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = metric?.title ?: spec.fallbackTitle,
                                        fontSize = SkinTokens.body,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SkinTokens.ink
                                    )
                                    Text(
                                        text = metric?.level ?: "暂时没读到",
                                        fontSize = SkinTokens.caption,
                                        color = SkinTokens.muted
                                    )
                                }
                            }
                            CastHint(metric?.hint ?: "接上手环或手动校准都可以")
                        }
                    }
                },
                backdrop = backdrop,
                tint = SkinTokens.muted
            )
        }

        Spacer(Modifier.height(10.dp))

        // 一格：小色块图标 + 词。名称收到 info 浮层里，横带上只留「哪个信号、什么水平」。
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            specs.forEach { spec ->
                val metric = weather.metric(spec.key)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(spec.tint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassIcon(spec.icon, spec.tint, Modifier.size(17.dp))
                    }
                    Spacer(Modifier.height(5.dp))
                    ValueFadeText(
                        text = metric?.level ?: "没读到",
                        fontSize = SkinTokens.caption,
                        fontWeight = FontWeight.Medium,
                        color = SkinTokens.ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = spec.fallbackTitle,
                        fontSize = SkinTokens.caption,
                        color = SkinTokens.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 数值/状态文字的换值过渡（22.28.0 动画批）。
 *
 * 「暂时没读到」→ 有值、或词本身变化时，旧值快出（120ms）、新值中档进（220ms），
 * 交叉淡入——不做位移、不做缩放，数值变化只需要"换得干净"。曲线与浮层、
 * 微行动展开同族（[CastMotion] emphasized）。静止时就是一个普通 [Text]。
 */
@Composable
private fun ValueFadeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(CastMotion.enter()) togetherWith fadeOut(CastMotion.exit()))
                .using(SizeTransform(clip = false))
        },
        label = "value-fade"
    ) { value ->
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 居中的小字说明。
 *
 * hero 里最后这句要居中，而 [CastHint] 是左对齐的（它平时要跟正文左边界对齐），
 * 所以这里用同一套字号 / 行高 / 颜色重画一次，只换对齐方式；文案词条不变。
 */
@Composable
private fun CenteredHint(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        fontSize = SkinTokens.caption,
        lineHeight = 22.sp,
        color = SkinTokens.muted,
        textAlign = TextAlign.Center
    )
}

// ---------------------------------------------------------------------------
// 展开 / 收起（微行动与天气诗共用）
// ---------------------------------------------------------------------------

/**
 * 「展开 / 收起」的文字与箭头。
 *
 * 折叠时箭头朝下（90°）、展开时朝上（-90°）。两个角度之间只差 180°，
 * 动画走**经右侧**的路径（90 → -90 经过 0°）——之前的写法是 90 → 270，
 * 数值上同样转半圈，但实际是**经左边**翻上去的，看起来像箭头先「倒退」
 * 再翻转，这就是折叠「奇怪」的来源。箭头走快档（120ms），
 * 比面板的展开（中档 220ms）先一步转到位——小控件要比容器利落，
 * 但曲线同族（[CastMotion] 的 emphasized），看起来还是同一个动作。
 * 今日微行动与工具箱里的天气诗共用它，两处的开合手感一致。
 */
@Composable
private fun ExpandAffordance(expanded: Boolean) {
    val arrowAngle by animateFloatAsState(
        targetValue = if (expanded) -90f else 90f,
        animationSpec = CastMotion.tween(CastMotion.FAST),
        label = "expand-arrow"
    )
    Text(
        text = if (expanded) "收起" else "展开",
        fontSize = SkinTokens.caption,
        color = SkinTokens.muted
    )
    Spacer(Modifier.width(6.dp))
    GlassIcon(
        kind = "next",
        tint = SkinTokens.muted,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = arrowAngle }
    )
}

@Composable
private fun ToolboxCard(
    backdrop: Backdrop,
    weather: BodyWeather,
    tts: TextToSpeech?,
    ttsReady: Boolean,
    ttsNote: String? = null,
    speaking: Boolean = false,
    suggestion: String? = null,
    focusSteps: Int? = null,
    focusMinutes: Int? = null
) {
    val overlay = LocalCastOverlay.current
    val context = LocalContext.current
    var poemExpanded by remember { mutableStateOf(false) }

    CastCard(backdrop = backdrop, contentPadding = PaddingValues(0.dp)) {
        GlassActionRow(
            backdrop = backdrop,
            title = when {
                !ttsReady -> "语音播报还差一步"
                speaking -> "停止播报"
                else -> "听一遍今天"
            },
            icon = if (ttsReady && speaking) "pause" else if (ttsReady) "play" else "info",
            subtitle = if (speaking) "正在播报…" else ttsNote,
            onClick = {
                if (ttsReady && speaking) {
                    // 再点一次就是停。
                    runCatching { tts?.stop() }
                } else if (ttsReady) {
                    // 22.53.0：一次就把今天说完——天气、结论、今天先补什么、
                    // 今天这一件小事。以前只念天气那两句，听完还得抬眼看屏幕。
                    val headline = weather.broadcast.headline
                    val focus = NutritionBridge.primary(
                        weather.kind,
                        focusSteps,
                        focusMinutes
                    )
                    val speech = buildString {
                        append(headline)
                        if (!headline.endsWith("。") && !headline.endsWith("！")) append("。")
                        append(weather.broadcast.body)
                        append("今天先补：")
                        append(BodyCastText.text(focus.label))
                        append("。")
                        if (!suggestion.isNullOrBlank()) {
                            append("今天可以先做这一件：")
                            append(suggestion)
                            append("。")
                        }
                    }
                    tts?.speak(
                        SpeechText.forSpeech(speech),
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "bodycast"
                    )
                } else {
                    overlay.show(
                        title = "语音播报还差一步",
                        subtitle = "本机还没有念得动这一种语言的音色。"
                    ) {
                        CastHint("系统朗读需要两样东西：一个语音合成引擎（系统里通常叫「语音服务」或「Google 语音服务」），以及这个语言的语音包。")
                        CastHint("装好之后回到这里，这一行会自己变成「只听身体播报」。")
                        Spacer(Modifier.height(8.dp))
                        CastPrimaryButton(
                            text = "打开系统语音设置",
                            icon = "settings",
                            onClick = {
                                val intent = android.content.Intent(Speech.TTS_SETTINGS_ACTION)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                val opened = runCatching { context.startActivity(intent) }.isSuccess
                                if (!opened) {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
                                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                                overlay.dismiss()
                            },
                            backdrop = backdrop
                        )
                    }
                }
            },
            surface = false,
            chevron = false
        )
        CastDivider()

        // ---- 今天的天气诗：点开才读 ----
        // 这一行要放「展开/收起 + 旋转箭头」，用 GlassRow（GlassActionRow 的
        // 底子）而不是 GlassActionRow——后者没有 trailing 位。
        CastDivider()

        // ---- 为什么这样播报：只讲依据，不给数字 ----
        // 22.33.0：补 surface = false——之前默认 true，会在卡片里再糊一层
        // 自己的圆角玻璃，形成「圆角套圆角」（图标圆角也跟着走了另一套）。
        GlassActionRow(
            backdrop = backdrop,
            title = "为什么这样播报？",
            icon = "info",
            surface = false,
            onClick = {
                overlay.show(
                    title = "为什么这样播报",
                    subtitle = "只讲依据，不给数字。"
                ) {
                    Text(
                        text = weather.broadcast.reason,
                        fontSize = SkinTokens.body,
                        lineHeight = 28.sp,
                        color = SkinTokens.ink
                    )
                    Spacer(Modifier.height(6.dp))
                    CastDivider()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "数据来源：${weather.source.label}",
                        fontSize = SkinTokens.caption,
                        lineHeight = 24.sp,
                        color = SkinTokens.muted
                    )
                    if (weather.calibrated.isNotEmpty()) {
                        Text(
                            text = "你的手动校准：" +
                                weather.calibrated.joinToString("、") { it.label },
                            fontSize = SkinTokens.caption,
                            lineHeight = 24.sp,
                            color = SkinTokens.ink
                        )
                        CastHint("手动校准的优先级高于系统读数——你说的话不参与投票，它直接决定结论。")
                    }
                    if (weather.missing.isNotEmpty()) {
                        Text(
                            text = "暂时没读到：" + weather.missing.joinToString("、"),
                            fontSize = SkinTokens.caption,
                            lineHeight = 24.sp,
                            color = SkinTokens.muted
                        )
                        CastHint("没读到就是没读到，这里不会替你补一个数。")
                    }
                    if (weather.demo) {
                        CastHint("当前是演示数据。")
                    }
                }
            }
        )
    }
}

/**
 * 今天的数据（22.50.0）。
 *
 * 老师的意见是：「手环的健康数据是一个客观的值，数据应该放在前面」——
 * 在这之前，首页从上到下都是**被翻译过的结论**（天气、播报、四信号的水平词），
 * 原始的六类读数只在设置与开发者页里看得到。这张卡补上那一层：
 *
 * - 六类读数**原样列出**（步数 / 睡眠 / 静息心率 / 心率变异性 / 血氧 / 运动时长），
 *   取不到的写「没读到」，**不补 0**；
 * - 只陈述不评分：没有达标线、没有箭头、没有「偏低」这类判断，页脚写明；
 * - 一行数据来源（手动填写 / 小米云 / 导入 / 系统健康服务）与「上次读取」时间，
 *   数据不是实时的时候用户得知道；
 * - 一条都读不到时不做空卡：给两个真实出路（去连接 / 手动填写）。
 */
@Composable
private fun TodayDataCard(backdrop: Backdrop, store: BodyCastStore) {
    val signals = store.signals
    val overlay = LocalCastOverlay.current

    data class Reading(val label: String, val value: String?)

    val readings = listOf(
        Reading("步数", signals.steps?.let { "$it 步" }),
        Reading("睡眠时长", signals.sleepMinutes?.let { "${it / 60} 小时 ${it % 60} 分" }),
        Reading("静息心率", signals.restingHeartRate?.let { "$it 次/分" }),
        Reading("心率变异性", signals.hrv?.let { "$it ms" }),
        Reading("血氧", signals.bloodOxygen?.let { "$it%" }),
        Reading("运动时长", signals.activityMinutes?.let { "$it 分钟" })
    )
    val readCount = readings.count { it.value != null }

    CastCard(backdrop = backdrop) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "今天的数据",
                modifier = Modifier.weight(1f),
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Text(
                text = if (readCount == 0) "还没读到" else "$readCount / 6",
                fontSize = SkinTokens.caption,
                color = SkinTokens.muted
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "手环读到的原始数值，只陈述，不评分。",
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(10.dp))

        if (readCount == 0) {
            // 一条都没有：不摆六行「没读到」，直接给两条路。
            Text(
                text = "还没有读到任何读数。连上手环数据（或手动填一次）之后，这里就是今天的第一屏。",
                fontSize = SkinTokens.caption,
                lineHeight = 23.sp,
                color = SkinTokens.muted
            )
            Spacer(Modifier.height(10.dp))
            CastPrimaryButton(
                text = "去连接运动数据",
                icon = "shield",
                onClick = { BodyCastNav.openDataGuide() },
                backdrop = backdrop
            )
            Spacer(Modifier.height(6.dp))
            CastSecondaryButton(
                text = "手动填写读数",
                icon = "edit",
                onClick = { BodyCastNav.openManualEntry() },
                backdrop = backdrop
            )
            return@CastCard
        }

        readings.forEachIndexed { index, reading ->
            if (index > 0) Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reading.label,
                    modifier = Modifier.weight(1f),
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted
                )
                Text(
                    text = reading.value ?: "没读到",
                    fontSize = SkinTokens.body,
                    fontWeight = if (reading.value != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (reading.value != null) SkinTokens.ink else SkinTokens.faint
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        CastDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            text = "来源：${signals.source.label}" +
                (if (store.prefs.lastFetchAt > 0L) " · 上次读取：" + entryStamp(store.prefs.lastFetchAt) else ""),
            fontSize = SkinTokens.caption,
            lineHeight = 22.sp,
            color = SkinTokens.muted
        )
    }
}
