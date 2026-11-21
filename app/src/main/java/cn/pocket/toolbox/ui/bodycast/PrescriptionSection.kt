package cn.pocket.toolbox.ui.bodycast

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.Prescription
import cn.pocket.toolbox.bodycast.Slot
import cn.pocket.toolbox.bodycast.StepPacing
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.ui.skin.SkinTokens
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 今日微行动处方区块（8.0.0 起是首页的一个 section，不再是独立 tab）。
 *
 * 它把「今天身体是什么天气」翻译成**现在就能做完**的几件小事：
 * - 不打卡、不连击、不显示完成率。做完只回一句「身体收到。」，没有分数。
 * - 不显示卡路里、体重、时长目标。
 * - 没有「您今日未达标」这类句式，也没有红色警告。
 * - 不接任何大模型，全部文案来自本地规则引擎与预设模板。
 *
 * 关于「背景音」：本版本**没有任何音频资源文件**，22.28.0 起相关选择 UI
 * （设置页「背景音默认」、进行中卡片的声音分段）已隐藏——不给「能点但不响」
 * 的半成品；偏好字段仍保留，接上音频资源后把 UI 加回来即可。
 */

/** 处方区块的会话状态。 */
private enum class RxPhase { IDLE, RUNNING, PAUSED, DONE }

private val FEEDBACK_LABELS = mapOf(
    BodyCastStore.FEEDBACK_LIGHTER to "身体变轻了",
    BodyCastStore.FEEDBACK_NOTHING to "没感觉",
    BodyCastStore.FEEDBACK_HEAVIER to "更累了",
    BodyCastStore.FEEDBACK_AGAIN to "想再来一次"
)
private val SCENE_LABELS = mapOf(
    "desk" to "工位", "transit" to "地铁", "bed" to "床上",
    "balcony" to "阳台", "outdoors" to "户外"
)

/**
 * 四个反馈选项。
 *
 * 文案与「更累了→更短更温和」的翻译规则都在 [BodyCastStore] 里，
 * 这里只做一次别名——页面不许自己拼这几个字符串，否则以后改文案必漏。
 */
private val FEEDBACK_OPTIONS: List<String> get() = BodyCastStore.FEEDBACK_OPTIONS

/** 还没有任何反馈时显示的中性提示。有反馈了就换成 `feedbackAdjustmentNote`。 */
private const val FEEDBACK_HINT_NEUTRAL = "做完选一个就行。选了「更累了」，下次就会更短更温和。"

/** 三个放置目标。Slot.NIGHT 不在这里——它不是一个「放过去」的时段。 */
private val SLOT_TARGETS = listOf(Slot.MORNING, Slot.NOON, Slot.EVENING)

/** 环境建议候选。纯预设模板，按处方编号稳定挑一条，不是传感器读数。 */
private val ENV_TIPS = listOf(
    "把灯调暗一点",
    "开窗 2 分钟",
    "手机反面朝上 5 分钟",
    "热敷眼睛",
    "喝 200ml 水"
)

/** 票根宽度。打孔线也画在这个位置。 */
private val STUB_WIDTH = 74.dp

private fun slotText(slot: Slot): String = when (slot) {
    Slot.MORNING -> "早上"
    Slot.NOON -> "午后"
    Slot.EVENING -> "睡前"
    Slot.NIGHT -> "夜里"
}

/** 从候选里稳定挑一条环境建议：同一张票永远给同一句。 */
private fun environmentSuggestion(prescription: Prescription): String {
    val h = prescription.ticketNo.hashCode() % ENV_TIPS.size
    return ENV_TIPS[(h + ENV_TIPS.size) % ENV_TIPS.size]
}

// ---------------------------------------------------------------------------
// 区块
// ---------------------------------------------------------------------------

/**
 * 处方区块：时段选择 + 车票/进行中/已完成三态主卡 + 换票 + 环境建议。
 *
 * 作为首页 CastPage 里的单个 item 嵌入，内部是一个 Column——
 * 会话状态（倒计时、拖拽位移）只在这一处 remember，不会因为
 * LazyColumn 的回收而丢。
 */
@Composable
fun PrescriptionSection(
    backdrop: Backdrop,
    store: BodyCastStore,
    modifier: Modifier = Modifier
) {
    val speaker = rememberSpeaker()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 进来没处方就现开一张。只开一次，不会每次重组都换。
    LaunchedEffect(Unit) {
        if (store.prescription == null) store.issuePrescription()
    }

    val prescription = store.prescription
    val ticket = prescription?.ticketNo
    val totalSec = (prescription?.totalMinutes ?: 0) * 60

    // 反馈不用页面自己记：store.lastFeedback 是持久的，换票也不会丢。
    // 选中态读「这一轮的，没有就读上次的」，换一张处方后高亮仍然在。
    val selectedFeedback = store.feedback ?: store.lastFeedback
    val adjustmentNote = store.feedbackAdjustmentNote

    // ---- 会话状态 ----
    var phase by remember { mutableStateOf(RxPhase.IDLE) }
    var remainingSec by remember { mutableIntStateOf(0) }
    var swapSeq by remember { mutableIntStateOf(0) }
    var placedSlot by remember { mutableStateOf<Slot?>(null) }

    // 换票 = 换一轮。倒计时和进行中的状态跟着票号重置。
    LaunchedEffect(ticket) {
        phase = RxPhase.IDLE
        remainingSec = (prescription?.totalMinutes ?: 0) * 60
    }

    /**
     * 倒计时。
     *
     * 用 `withFrameNanos` 累加真实流逝时间，而不是 `delay(1000)` 递减——
     * 后者每帧都会漂，五分钟下来能差出好几秒，而这个界面唯一敢显示的
     * 数字就是这个剩余时间，漂不起。
     */
    LaunchedEffect(phase, ticket) {
        if (phase != RxPhase.RUNNING) return@LaunchedEffect
        var last = withFrameNanos { it }
        var carry = 0L
        while (true) {
            val now = withFrameNanos { it }
            carry += now - last
            last = now
            if (carry >= 1_000_000_000L) {
                carry -= 1_000_000_000L
                val next = remainingSec - 1
                if (next <= 0) {
                    remainingSec = 0
                    phase = RxPhase.DONE
                    // 做完一件微行动 = 照顾自己一次（回声页的正向计数）。
                    // 22.49.0：一次记齐「照顾自己一次」与「做过哪几件」。
                    store.completeActions(prescription?.items.orEmpty().map { it.id })
                    return@LaunchedEffect
                }
                remainingSec = next
            }
        }
    }

    // 进行中的步骤序列（22.55.0 重做节奏）：
    // 以前把每一步**等分**总时长——「站起来走 20 步」和「重复 6 轮」各占一样久，
    // 用户反馈「有些步骤明明一下就做完了，却给它留了一大段时间」。
    // 现在按每一步**自己写了多少动作量**分配（见 StepPacing），
    // 动作量小的步骤一闪而过，真要做的步骤才拿到时间。
    val runSteps: List<Pair<String, String>> = remember(ticket) {
        prescription?.items.orEmpty().flatMap { action -> action.guide.map { action.title to it } }
    }
    val stepPlan: List<Int> = remember(runSteps, totalSec) {
        StepPacing.plan(runSteps.map { it.second }, totalSec)
    }
    val elapsedSec = (totalSec - remainingSec).coerceAtLeast(0)
    val stepNo = if (runSteps.isEmpty()) {
        0
    } else {
        var acc = 0
        var index = 0
        while (index < stepPlan.size - 1 && elapsedSec >= acc + stepPlan[index]) {
            acc += stepPlan[index]
            index += 1
        }
        index + 1
    }

    // 每一步切换时朗读这一步。第一步由「开始」按钮负责朗读动作标题。
    // 22.49.0：报一句步号（「第 3 步：吸气 4 秒，不用吸满。」），
    // 而且说的时候不打断（[Speaker.sayStep]）。
    LaunchedEffect(stepNo, ticket) {
        if (phase == RxPhase.RUNNING && stepNo > 1 && store.prefs.voiceGuide) {
            val line = runSteps.getOrNull(stepNo - 1)?.second.orEmpty()
            if (line.isNotBlank()) {
                speaker.sayStep(SpeechText.forSpeech(BodyCastText.text("第 $stepNo 步：$line")))
            }
        }
    }

    // ---- 换票 / 拖拽 ----
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val slotRects = remember { mutableMapOf<Slot, Rect>() }
    var cardRestRect by remember { mutableStateOf(Rect.Zero) }
    val swipeThreshold = with(density) { 92.dp.toPx() }
    // 22.55.0：拖动时左右位移**夹在 ±96dp**（超出部分按 0.2 衰减）。
    // 以前是 1:1 不夹取，一路拖就能把整张票拉出屏幕，看着像「框被拉伸了」。
    val rubberLimit = with(density) { 96.dp.toPx() }
    fun rubberX(raw: Float): Float {
        val limit = rubberLimit
        return if (abs(raw) <= limit) raw
        else (limit + (abs(raw) - limit) * 0.2f) * (if (raw < 0) -1f else 1f)
    }

    fun settleBack() {
        scope.launch {
            // 22.28.0：松手回位与浮层下拉取消共用同一组落定弹簧（0.85/500）。
            dragOffset.animateTo(
                targetValue = Offset.Zero,
                animationSpec = CastMotion.settleSpec()
            )
        }
    }

    fun regenerate(exclude: Set<String> = emptySet()) {
        swapSeq += 1
        store.issuePrescription(seedOffset = swapSeq.toLong(), excludeIds = exclude)
    }

    // 整张票左右滑 → 换一张。
    val swipeModifier = Modifier.pointerInput(ticket) {
        detectHorizontalDragGestures(
            onDragEnd = {
                if (abs(dragOffset.value.x) > swipeThreshold) regenerate()
                settleBack()
            },
            onDragCancel = { settleBack() },
            onHorizontalDrag = { change, amount ->
                change.consume()
                scope.launch { dragOffset.snapTo(dragOffset.value + Offset(amount, 0f)) }
            }
        )
    }

    // 票身上的「拖到时段」手柄 → 把票拖到某个时段胶囊里。
    //
    // 手势只挂在这条手柄上，不挂整张票：整张票吃下拖拽会让 LazyColumn
    // 滚不动，为了一个次要交互牺牲整页滚动不划算。
    val handleModifier = Modifier.pointerInput(ticket) {
        detectDragGestures(
            onDragEnd = {
                val dropped = cardRestRect.center + dragOffset.value
                val hit = SLOT_TARGETS.firstOrNull { slot ->
                    slotRects[slot]?.contains(dropped) == true
                }
                if (hit != null) {
                    placedSlot = hit
                    regenerate()
                }
                settleBack()
            },
            onDragCancel = { settleBack() },
            onDrag = { change, amount ->
                change.consume()
                scope.launch { dragOffset.snapTo(dragOffset.value + amount) }
            }
        )
    }

    // 区块内部的间距收到 6dp：时段胶囊、票、按钮之间不再各空一格。
    // （外层 CastPage 的卡片间距不变，这一块只是自己更紧凑。）
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (prescription == null) {
            CastEmptyPanel("正在为你生成今天的处方…", backdrop = backdrop)
            return@Column
        }

        // ---- 放置目标：早上 / 午后 / 睡前 ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SLOT_TARGETS.forEach { slot ->
                Box(
                    Modifier
                        .weight(1f)
                        .onGloballyPositioned { slotRects[slot] = it.boundsInRoot() }
                ) {
                    GlassChip(
                        label = slotText(slot),
                        selected = (placedSlot ?: prescription.slot) == slot,
                        onClick = {
                            placedSlot = slot
                            regenerate()
                        },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        CastHint("点上面的时段，或者按住票身上的「拖到时段」把它拖过去。")

        // ---- 主卡片：待开始 / 进行中 / 已完成 ----
        Box(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { cardRestRect = it.boundsInRoot() }
        ) {
            val cardModifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = rubberX(dragOffset.value.x)
                    translationY = dragOffset.value.y
                }

            when (phase) {
                RxPhase.IDLE -> TicketCard(
                    backdrop = backdrop,
                    prescription = prescription,
                    scenes = store.prefs.preferredScenes.take(2).map { BodyCastText.text(SCENE_LABELS[it] ?: it) },
                    placedSlot = placedSlot,
                    voiceGuide = store.prefs.voiceGuide,
                    speakerNote = speaker.voiceNote,
                    onVoiceGuideChange = { value -> store.updatePrefs { it.copy(voiceGuide = value) } },
                    modifier = cardModifier.then(swipeModifier),
                    handleModifier = handleModifier,
                    onStart = {
                        phase = RxPhase.RUNNING
                        if (store.prefs.voiceGuide) {
                            // 22.57.0：**只念当前步骤**——开场就是第 1 步，
                            // 不再先念「共 N 件小事、约 M 分钟」这种整体信息
                            //（用户反馈：那一段又长又用不上，他要的是「现在这一下怎么做」）。
                            val firstStep = runSteps.getOrNull(0)?.second.orEmpty()
                            if (firstStep.isNotBlank()) {
                                speaker.say(SpeechText.forSpeech(BodyCastText.text("第 1 步：$firstStep")))
                            }
                        }
                    }
                )

                RxPhase.RUNNING, RxPhase.PAUSED -> RunningCard(
                    backdrop = backdrop,
                    prescription = prescription,
                    running = phase == RxPhase.RUNNING,
                    remainingSec = remainingSec,
                    totalSec = totalSec,
                    stepNo = stepNo,
                    stepCount = runSteps.size,
                    stepText = runSteps.getOrNull(stepNo - 1)?.second
                        ?: "按你舒服的节奏来。",
                    actionTitle = runSteps.getOrNull(stepNo - 1)?.first.orEmpty(),
                    speakerAvailable = speaker.available,
                    speakerSpeaking = speaker.speaking,
                    speakerNote = speaker.voiceNote,
                    voiceGuide = store.prefs.voiceGuide,
                    onVoiceGuideChange = { value ->
                        store.updatePrefs { it.copy(voiceGuide = value) }
                    },
                    onTogglePause = {
                        phase = if (phase == RxPhase.RUNNING) {
                            RxPhase.PAUSED
                        } else {
                            RxPhase.RUNNING
                        }
                    },
                    onFinish = {
                        phase = RxPhase.DONE
                        // 提前点「完成」也算照顾了自己一次，和倒计时走完同权。
                        store.completeActions(prescription.items.map { it.id })
                    },
                    modifier = cardModifier
                )

                RxPhase.DONE -> DoneCard(
                    backdrop = backdrop,
                    feedback = selectedFeedback,
                    adjustmentNote = adjustmentNote,
                    onAnother = {
                        // 22.53.0：再来一件——新处方排除刚做完的这几件。
                        val finished = prescription.items.map { it.id }.toSet()
                        remainingSec = (prescription.totalMinutes) * 60
                        regenerate(exclude = finished)
                        phase = RxPhase.IDLE
                    },
                    onFeedback = { text ->
                        store.recordFeedback(text)
                        // 引擎只认识时长上限和要避开的类别，
                        // 「更累了→更短更温和」的翻译在 store 的 adjustedPrefs() 里，
                        // 所以这里只要重开一张，调整就自动生效。
                        if (store.prefs.autoAdjustAfterFeedback) regenerate()
                    },
                    modifier = cardModifier
                )
            }
        }

        // 上一次反馈带来的调整，用一句人话讲出来——让「我选的东西真的生效了」
        // 这件事在界面上看得见，而不是一句泛泛的「已记录」。
        if (adjustmentNote != null && phase == RxPhase.IDLE) {
            CastHint(adjustmentNote)
        }

        // ---- 换一张 ----
        if (phase == RxPhase.IDLE || phase == RxPhase.DONE) {
            CastSecondaryButton(
                text = "换一张处方",
                icon = "swap",
                onClick = { regenerate() },
                backdrop = backdrop
            )
            CastHint("也可以左右滑动上面那张票，效果一样。")
        }

        // ---- 环境建议 ----
        Text(
            text = "顺便，一个环境建议",
            modifier = Modifier.padding(top = 2.dp),
            fontSize = SkinTokens.sectionTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        CastBanner(
            text = "${environmentSuggestion(prescription)}\n${prescription.note}",
            backdrop = backdrop,
            icon = "leaf",
            tint = SkinTokens.sage
        )
    }
}

// ---------------------------------------------------------------------------
// 主卡片 1：处方笺 + 车票
// ---------------------------------------------------------------------------

@Composable
private fun TicketCard(
    backdrop: Backdrop,
    prescription: Prescription,
    scenes: List<String>,
    placedSlot: Slot?,
    onStart: () -> Unit,
    voiceGuide: Boolean,
    speakerNote: String?,
    onVoiceGuideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier
) {
    val weatherColor = Color(prescription.weatherKind.colorHex)
    val effectiveSlot = placedSlot ?: prescription.slot

    CastCard(
        backdrop = backdrop,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                // 票根底色 + 打孔虚线 + 两个圆孔，都在这里一次画完。
                // 用 drawBehind（同一个 DrawScope / Canvas 绘制 API）而不是
                // 塞一个 Canvas 子节点，是为了不把整行拖进 IntrinsicSize 测量。
                .drawBehind { drawTicketDecor() }
        ) {
            // ---- 票根 ----
            // 票本身的留白收了一档：票根不再是 18dp 上下空，天气色条也短了 14dp。
            Column(
                Modifier
                    .width(STUB_WIDTH)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("处方", fontSize = SkinTokens.caption, color = SkinTokens.faint)
                Spacer(Modifier.height(6.dp))
                // 编号竖排：一个字一行，像被机器打上去的
                prescription.ticketNo.forEach { ch ->
                    Text(
                        text = ch.toString(),
                        fontSize = 17.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SkinTokens.ink
                    )
                }
                Spacer(Modifier.height(10.dp))
                // 天气色竖条
                Box(
                    Modifier
                        .width(6.dp)
                        .height(40.dp)
                        .clip(CircleShape)
                        .background(weatherColor)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = slotText(effectiveSlot),
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted
                )
            }

            // ---- 票身 ----
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 6.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(weatherColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = prescription.weatherKind.label,
                        fontSize = SkinTokens.cardTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = SkinTokens.ink
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = prescription.weatherKind.motto,
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    color = SkinTokens.muted
                )

                if (placedSlot != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIcon("check", SkinTokens.accent, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "已放到：${slotText(placedSlot)}",
                            fontSize = SkinTokens.caption,
                            color = SkinTokens.accent
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                CastDivider()
                Spacer(Modifier.height(10.dp))

                MetaRow("建议时长", "约 ${prescription.totalMinutes} 分钟")
                Spacer(Modifier.height(5.dp))
                MetaRow("适用时段", slotText(effectiveSlot))
                Spacer(Modifier.height(5.dp))
                MetaRow("适用场景", scenes.joinToString("、").ifEmpty { "还没设置" })

                Spacer(Modifier.height(10.dp))
                CastDivider()
                Spacer(Modifier.height(10.dp))

                // 3–5 个动作，每个动作连同它自己的分步引导。不做成一条长清单，
                // 因为「几件小事」比「十几步」更像一件现在就能开始的事。
                prescription.items.take(5).forEachIndexed { index, action ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(weatherColor.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassIcon(action.category.icon, weatherColor, Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = action.title,
                            modifier = Modifier.weight(1f),
                            fontSize = SkinTokens.body,
                            fontWeight = FontWeight.SemiBold,
                            color = SkinTokens.ink
                        )
                        Text(
                            text = "${action.minutes} 分钟",
                            fontSize = SkinTokens.caption,
                            color = SkinTokens.muted
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    CompactGuide(action.guide)
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    handleModifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(SkinTokens.glassTrack)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GlassIcon("swap", SkinTokens.muted, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("拖到时段", fontSize = SkinTokens.caption, color = SkinTokens.muted)
                }

                Spacer(Modifier.height(10.dp))

                CastPrimaryButton(
                    text = "开始 ${prescription.totalMinutes} 分钟微行动",
                    icon = "play",
                    onClick = onStart,
                    backdrop = backdrop
                )

                // 22.49.0：语音开关**开始之前**就能设——它以前只在进行中的卡片里，
                // 用户想开语音得先点开始，找不到、也用不上。
                Spacer(Modifier.height(8.dp))
                GlassToggleRow(
                    backdrop = backdrop,
                    title = "语音引导",
                    subtitle = speakerNote ?: "每一步会念出来，做的时候不用看屏幕。",
                    checked = voiceGuide,
                    onCheckedChange = onVoiceGuideChange,
                    enabled = true
                )
            }
        }
    }
}

/**
 * 票根底色 + 打孔线。
 *
 * 打孔线用 `PathEffect.dashPathEffect` 画竖虚线，两端各一个圆孔——
 * 这是「车票」这个隐喻唯一真正的识别点，所以宁可手画也不用 Divider。
 */
private fun DrawScope.drawTicketDecor() {
    val stubWidth = STUB_WIDTH.toPx()
    val radius = SkinTokens.cardRadius.toPx()

    // 票根底色：只保留左侧两个圆角，与卡片本身的圆角对齐
    clipRect(left = 0f, top = 0f, right = stubWidth, bottom = size.height) {
        drawRoundRect(
            color = SkinTokens.glassTrack,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(radius, radius)
        )
    }

    val holeRadius = 7.dp.toPx()
    val lineTop = holeRadius * 2f
    val lineBottom = size.height - holeRadius * 2f
    if (lineBottom > lineTop) {
        val path = Path().apply {
            moveTo(stubWidth, lineTop)
            lineTo(stubWidth, lineBottom)
        }
        drawPath(
            path = path,
            color = SkinTokens.faint.copy(alpha = 0.55f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                    0f
                )
            )
        )
    }

    val holeColor = SkinTokens.faint.copy(alpha = 0.30f)
    drawCircle(holeColor, holeRadius, Offset(stubWidth, 0f))
    drawCircle(holeColor, holeRadius, Offset(stubWidth, size.height))
}

// ---------------------------------------------------------------------------
// 主卡片 2：进行中
// ---------------------------------------------------------------------------

@Composable
private fun RunningCard(
    backdrop: Backdrop,
    prescription: Prescription,
    running: Boolean,
    remainingSec: Int,
    totalSec: Int,
    stepNo: Int,
    stepCount: Int,
    stepText: String,
    actionTitle: String,
    speakerAvailable: Boolean,
    speakerSpeaking: Boolean,
    speakerNote: String?,
    voiceGuide: Boolean,
    onVoiceGuideChange: (Boolean) -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    CastCard(backdrop = backdrop, modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CountdownRing(
                remainingSeconds = remainingSec,
                totalSeconds = totalSec,
                accent = Color(prescription.weatherKind.colorHex),
                running = running,
                label = if (running) "还剩" else "已暂停"
            )
            Spacer(Modifier.height(12.dp))
            if (stepCount > 0) {
                Text(
                    text = "第 $stepNo / $stepCount 步",
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.faint
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = stepText,
                fontSize = SkinTokens.cardTitle,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink,
                textAlign = TextAlign.Center
            )
            if (actionTitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = actionTitle,
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        CastDivider()
        Spacer(Modifier.height(8.dp))

        GlassToggleRow(
            backdrop = backdrop,
            title = "语音引导",
            // 不再因为「没有这一种语言的音色」把开关灰掉——引擎在就能念，
            // 有话说就写在副标题里；正在念的时候直接显示出来，用户看得见语音在工作。
            subtitle = when {
                speakerSpeaking -> "正在朗读…"
                speakerNote != null -> speakerNote
                else -> "每一步会念出来，做的时候不用看屏幕。"
            },
            checked = voiceGuide,
            onCheckedChange = onVoiceGuideChange,
            enabled = true
        )

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.weight(1f)) {
                CastSecondaryButton(
                    text = if (running) "暂停" else "继续",
                    icon = if (running) "pause" else "play",
                    onClick = onTogglePause,
                    backdrop = backdrop
                )
            }
            Box(Modifier.weight(1f)) {
                CastSecondaryButton(
                    text = "提前结束",
                    icon = "check",
                    onClick = onFinish,
                    backdrop = backdrop
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 主卡片 3：完成
// ---------------------------------------------------------------------------

@Composable
private fun DoneCard(
    backdrop: Backdrop,
    feedback: String?,
    adjustmentNote: String?,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAnother: (() -> Unit)? = null
) {
    CastCard(backdrop = backdrop, modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        // 这里不写「打卡成功」。做完一件事不需要被判定成功，
        // 只需要被接住——所以是「身体收到。」
        // 22.28.0 动画批：完成卡入场走慢档 320ms + emphasized decelerate——
        // 「身体收到。」要稳一下再停住，但 420ms 太拖；曲线与浮层同族。
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(CastMotion.enter(CastMotion.SLOW)) + scaleIn(initialScale = 0.90f, animationSpec = CastMotion.enter(CastMotion.SLOW))
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "身体收到。",
                    fontSize = SkinTokens.display,
                    fontWeight = FontWeight.Bold,
                    color = SkinTokens.ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "这一轮结束了。不用打分，也不用汇报。",
                    fontSize = SkinTokens.caption,
                    color = SkinTokens.muted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        CastDivider()
        Spacer(Modifier.height(8.dp))

        Text(
            text = "刚才那几分钟，身体有什么变化？",
            fontSize = SkinTokens.body,
            fontWeight = FontWeight.Medium,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(6.dp))

        GlassChipFlow(
            items = FEEDBACK_OPTIONS,
            backdrop = backdrop,
            label = { BodyCastText.text(FEEDBACK_LABELS[it] ?: it) },
            isSelected = { it == feedback },
            onToggle = onFeedback
        )

        // 有反馈就直说这次调整了什么；没有就给一句中性的说明。
        // 不写「已记录」这种空话——用户要看的是「我说的话有没有用」。
        CastHint(adjustmentNote ?: FEEDBACK_HINT_NEUTRAL)

        // 22.53.0：还想动一动就再来一件——新处方会**排掉刚做完的这几件**，
        // 不会把同一套原封不动地端回来。
        if (onAnother != null) {
            Spacer(Modifier.height(10.dp))
            CastSecondaryButton(
                text = "再来一件（不重复刚做的）",
                icon = "reset",
                onClick = onAnother,
                backdrop = backdrop
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 小零件
// ---------------------------------------------------------------------------

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = SkinTokens.caption, color = SkinTokens.muted)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            fontSize = SkinTokens.caption,
            fontWeight = FontWeight.Medium,
            color = SkinTokens.ink
        )
    }
}

/** 分步引导之间的分隔符。只是排版上的分隔，不改任何一句引导文案。 */
private const val GUIDE_SEPARATOR = " · "

/**
 * 分步引导的紧凑写法。
 *
 * 原来一步一行、每行一个序号圆点：一条引导就占一整行（多数还会折成两行），
 * 三五个动作叠起来会把整张处方笺撑得很长。这里把同一条动作的几步并成一段，
 * **一句不少**，只是不再各占一行——字号和内容都不动，动的是留白。
 */
@Composable
private fun CompactGuide(steps: List<String>) {
    if (steps.isEmpty()) return
    Text(
        text = steps.joinToString(GUIDE_SEPARATOR),
        modifier = Modifier.fillMaxWidth(),
        fontSize = SkinTokens.body,
        lineHeight = 24.sp,
        color = SkinTokens.muted
    )
}

// ---------------------------------------------------------------------------
// 语音引导（TTS）
// ---------------------------------------------------------------------------

/**
 * 语音引导（22.49.0 重做）。
 *
 * 之前有四处让这一步「难用」：
 * 1. **音色判定用的是应用语言的 tag**（`zh-Hans`），很多引擎判成不支持 →
 *    `available` 恒为 false → 页面上的开关**灰着**，用户点了没反应。现在跟首页
 *    一样走 [Speech.pick]（候选表逐个试，拉丁文退回英语）。
 * 2. **每条都用 QUEUE_FLUSH**：步子切得比话说完快时，上一条被硬切——听起来
 *    就是碎句子。现在每条等它**说完**再接下一条，正在说的时候跳过新来的那条
 *    （这一步的内容本来就在屏幕上）。
 * 3. 朗读只说做法，不报「第几步」——现在开头带一句步号，听的人知道到哪了。
 * 4. 开始时只念动作名，不说要花多久——现在念「N 件小事，约 M 分钟」。
 */
private class Speaker {
    /** 引擎本身可用（**不等于**有当前语言的音色）。 */
    var available by mutableStateOf(false)

    /** 正在朗读——界面上据此显示「正在朗读…」，用户看得见语音在工作。 */
    var speaking by mutableStateOf(false)

    /** 没有匹配音色时的说明（先用系统默认音色念；null = 一切正常）。 */
    var voiceNote by mutableStateOf<String?>(null)

    private var engine: TextToSpeech? = null
    private var startedAt = 0L

    fun attach(tts: TextToSpeech) {
        engine = tts
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
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
        })
    }

    fun detach() {
        engine = null
        available = false
        speaking = false
        voiceNote = null
    }

    /** 立刻说（打断上一句）。用于「开始」那一下。 */
    fun say(text: String) {
        if (text.isBlank() || !available) return
        speaking = true
        startedAt = System.currentTimeMillis()
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /**
     * 一步一步说：**正在说的时候不打断**，把新的一条丢掉。
     *
     * 步子切换得比话说完快时，硬切会让每一句都变成半截；
     * 而这一步的做法本来就写在屏幕上，跳过不会让人漏掉信息。
     *
     * [SPEECH_TIMEOUT_MS] 是保险：引擎万一没回调 onDone（换引擎、被系统掐掉），
     * `speaking` 会永远停在 true，从此一句话都不再念——超过这个时长就当成说完了。
     */
    fun sayStep(text: String) {
        if (text.isBlank() || !available) return
        if (speaking && System.currentTimeMillis() - startedAt < SPEECH_TIMEOUT_MS) return
        speaking = true
        startedAt = System.currentTimeMillis()
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private companion object {
        const val UTTERANCE_ID = "bodycast-guide"
        /** 一句引导最多念 30 秒；超过就当它已经结束。 */
        const val SPEECH_TIMEOUT_MS = 30_000L
    }
}

@Composable
private fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker() }
    val speechLanguage = AppLanguage.current()

    DisposableEffect(speechLanguage) {
        // OnInitListener 回调里拿不到 engine 本身，用一格数组兜住。
        // 回调是 post 到主线程的，一定晚于下面的赋值。
        val holder = arrayOfNulls<TextToSpeech>(1)
        val engine = TextToSpeech(context.applicationContext) { status ->
            val tts = holder[0]
            if (status == TextToSpeech.SUCCESS && tts != null) {
                // 候选音色表逐个试（zh-Hans → zh_CN → zh…），拉丁文退回英语：
                // 直接把 tag 交给引擎会被判成「不支持」，开关就永远是灰的。
                val picked = Speech.pick(tts, speechLanguage)
                // 关键：**没有匹配音色也不再禁用开关**。引擎在就够了——退回系统默认
                // 音色照样能念（中文文本配英文音色不好听，但比「点了没反应」强），
                // 页面上如实写一句说明，用户想装音色再自己去装。
                if (picked == null) {
                    runCatching { tts.language = java.util.Locale.getDefault() }
                    speaker.voiceNote = "本机没有「${speechLanguage.selfName}」的音色，先用系统默认音色朗读。"
                } else {
                    speaker.voiceNote = null
                }
                speaker.available = true
            } else {
                // 连引擎都没有：开关仍然能点，点的时候会给一个去系统设置的出路
                //（见页面上的说明卡），不做一个死了的开关。
                speaker.available = false
                speaker.voiceNote = "本机没有可用的语音引擎"
            }
        }
        holder[0] = engine
        speaker.attach(engine)

        onDispose {
            speaker.detach()
            engine.stop()
            engine.shutdown()
        }
    }

    return speaker
}
