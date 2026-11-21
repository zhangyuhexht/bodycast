package cn.pocket.toolbox.ui.bodycast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import cn.pocket.toolbox.anim.CastMotion
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.glass.GlassTheme
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.ui.skin.SkinTokens

/**
 * 第一次打开时的开箱引导（22.0.0 新增；22.34.0 重排为七页；22.42.0 收成五页）。
 *
 * 为什么需要它：新用户第一次进来看不到分数、看不到「该做多少」，会有几个疑问——
 * 「它为什么不给我打分？」「每天怎么用？」「界面怎么调？」。这里用五页说清楚。
 *
 * **导航规则（22.34.0）**：
 * - 不能跳过整个引导——右上角的全局「跳过」不存在，必须一页页走完；
 * - 每页有「上一步」，可以回头改；
 * - 内容超出一屏**可以上下滑动**（22.34.0 修复：长页此前被裁掉下半截）；
 * - 第三方数据获取（手环绑定）**不再出现在引导里**——那是设置页的活，
 *   引导里只讲产品是什么、界面怎么调，不逼用户当场连设备。
 *
 * 页面结构（22.42.0）：①欢迎 + 选语言 ②主题色 + 深浅色（含预览卡）
 * ③理念：不评分 + 每天三个问题（合并页）④用法：校准 + 微行动（合并页）
 * ⑤只存在这台手机上。
 *
 * 看过就不再出现；想重看：设置 → 关于 → 重看新手引导。
 */

/** 引导的显示状态。放在这里是为了让设置页也能把它重新打开。 */
object OnboardingState {
    var visible by mutableStateOf(false)
}

/** 读一次「是否看过引导」。true = 还没看过。（用偏好里已有的 onboardingDone 字段） */
fun shouldShowOnboarding(store: BodyCastStore): Boolean = !store.prefs.onboardingDone

fun markOnboardingDone(store: BodyCastStore) {
    store.updatePrefs { it.copy(onboardingDone = true) }
}

/** 供设置页使用：把引导重新打开（关于页那一行调用它）。 */
fun reopenOnboarding() {
    OnboardingState.visible = true
}

/** 开箱里的忌口选项：和设置 → 食谱指南里那份是同一组 key。 */
private val ONBOARDING_DIETS = listOf(
    "vegetarian" to "素食",
    "dairy_free" to "不吃乳制品",
    "seafood_free" to "不吃海鲜",
    "low_sugar" to "低糖"
)

private const val PAGE_COUNT = 8

@Composable
fun OnboardingOverlay(
    backdrop: Backdrop,
    store: BodyCastStore,
    onDone: () -> Unit,
    animate: Boolean = true
) {
    var page by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(SkinTokens.bg)
            // 22.28.0：引导页此前整棵画在状态栏触摸区里——顶部那行
            // y<156px，点击被 SystemUI 吃掉。整页退进安全区（状态栏 + 手势区）。
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = SkinTokens.pagePadding, vertical = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── 顶部：分段进度条（无跳过按钮）──────────────────────────
            SegmentedProgress(
                total = PAGE_COUNT,
                current = page,
                animate = animate,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // ── 中间：一页一张卡，可上下滑动 ─────────────────────────────
            // 22.34.0：内容加 verticalScroll——长页（语言 + 主题色、要点多的页）
            // 此前超出一屏就直接被裁掉，下半截根本看不到、也够不到按钮。
            Box(Modifier.weight(1f)) {
                // 22.35.0：内容滚出底边时柔化一下，不再和按钮区硬碰硬。
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(androidx.compose.ui.graphics.Color.Transparent, SkinTokens.bg)
                            )
                        )
                )
                // 22.47.0：页面切换重做——「框拉伸、字后现」。
                // 单例玻璃卡（就是用户看到的"框"）高度随页内容做拉伸 / 缩小；
                // 页文字延迟淡入，在拉伸动画的中后段浮现。旧的整页横滑 /
                // 缩放过场全部移除。
                var displayed by remember { mutableIntStateOf(page) }
                val contentAlpha = remember { Animatable(1f) }
                val scrollState = rememberScrollState()
                LaunchedEffect(page) {
                    if (page != displayed) {
                        contentAlpha.snapTo(0f)
                        displayed = page
                        scrollState.scrollTo(0)
                        contentAlpha.animateTo(
                            1f,
                            tween(
                                durationMillis = if (animate) 260 else 0,
                                delayMillis = if (animate) 180 else 0,
                                easing = CastMotion.Decelerate
                            )
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    contentAlignment = Alignment.Center
                ) {
                    CastCard(backdrop = backdrop) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = if (animate) 340 else 0,
                                        easing = CastMotion.Emphasized
                                    )
                                )
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = contentAlpha.value
                                        translationY = (1f - contentAlpha.value) * 22.dp.toPx()
                                    }
                            ) {
                                when (displayed) {
                            0 -> OnboardingPage(
                                icon = "smile",
                                hero = { PlayfulWeatherOrb() },
                                title = "把身体状态翻译成天气",
                                body = "把手环数据，说成一句人人都懂的话。",
                                // 22.49.0：这一页要装的东西最多（球 + 语言格），
                                // 三条要点砍到两条——「全部在本机完成」在隐私那一页有整页在讲。
                                bullets = listOf(
                                    "不打分、不排名、不制造焦虑。",
                                    "读不到就写「暂时没读到」，不用 0 冒充读数。"
                                ),
                                backdrop = backdrop
                            ) {
                                // 语言是看懂后面每一页的前提，放第一页。
                                Text(
                                    text = "语言",
                                    fontSize = SkinTokens.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = SkinTokens.muted
                                )
                                Spacer(Modifier.height(8.dp))
                                // 22.46.0：语言选择从流式小胶囊改成**两列大格**——
                                // 每格一个语言、整格可点、选中实心高亮，比一排小 chip
                                // 更好认更好点（尤其对不熟悉中文界面的海外用户）。
                                OnboardingLanguageGrid(
                                    entries = AppLanguage.entries,
                                    backdrop = backdrop,
                                    onPick = { language ->
                                        store.updatePrefs { it.copy(language = language.tag) }
                                        AppLanguage.apply(language)
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
                                CastHint("随时可在设置里换。")
                            }
                            1 -> OnboardingPage(
                                icon = "sun",
                                title = "挑一个顺眼的样子",
                                body = "主题色管按钮和图标的颜色，深浅色管整页的底色。" +
                                    "点选即时生效，以后随时能换。",
                                backdrop = backdrop
                            ) {
                                Text(
                                    text = "主题色",
                                    fontSize = SkinTokens.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = SkinTokens.muted
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(4) { style ->
                                        val selected = store.prefs.accentStyle.coerceIn(0, 3) == style
                                        Box(
                                            Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(GlassTheme.accentPreview(style))
                                                .border(
                                                    width = if (selected) 2.dp else 0.dp,
                                                    color = if (selected) SkinTokens.ink else SkinTokens.glassDivider,
                                                    shape = CircleShape
                                                )
                                                .clickable(role = Role.Button) {
                                                    store.updatePrefs { it.copy(accentStyle = style) }
                                                }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "深浅色",
                                    fontSize = SkinTokens.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = SkinTokens.muted
                                )
                                Spacer(Modifier.height(8.dp))
                                GlassSegmented(
                                    options = listOf("浅色", "深色", "跟随系统"),
                                    selectedIndex = store.prefs.themeMode.coerceIn(0, 2),
                                    onSelect = { index ->
                                        store.updatePrefs {
                                            it.copy(themeMode = index, darkMode = index != 0)
                                        }
                                    },
                                    backdrop = backdrop
                                )
                                Spacer(Modifier.height(4.dp))
                                                                // 22.35.0：所见即所得的迷你预览——选色/选深浅即时变，
                                // 不用等进主界面才知道长什么样。
                                Spacer(Modifier.height(12.dp))
                                ThemePreviewCard(accentStyle = store.prefs.accentStyle.coerceIn(0, 3))
                                Spacer(Modifier.height(4.dp))
CastHint("玻璃的折射在深色底上最清晰，不过浅色也一样好用。")
                            }
                            // 22.49.0：基础信息两页（忌口 / 称呼）——选完语言与主题接着问，
                            // 忌口直接写进食谱页的过滤字段，称呼贯穿首页问候与来信署名。
                            2 -> OnboardingPage(
                                icon = "leaf",
                                title = "有什么忌口吗",
                                body = "勾上的这几类不会出现在食谱推荐里；没有就空着过去，随时能改。",
                                backdrop = backdrop
                            ) {
                                Text(
                                    text = "饮食偏好",
                                    fontSize = SkinTokens.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = SkinTokens.muted
                                )
                                Spacer(Modifier.height(8.dp))
                                GlassChipFlow(
                                    items = ONBOARDING_DIETS,
                                    backdrop = backdrop,
                                    label = { BodyCastText.text(it.second) },
                                    isSelected = { it.first in store.prefs.dietPreferences },
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
                                Spacer(Modifier.height(4.dp))
                                CastHint("设置 → 食谱指南里还能加上「过敏原提醒」。")
                            }
                            3 -> OnboardingPage(
                                icon = "user",
                                title = "怎么称呼你",
                                body = "写信、分享卡和首页问候都用它；1–20 个字，随时能改。",
                                backdrop = backdrop
                            ) {
                                var draft by remember { mutableStateOf(store.prefs.nickname) }
                                GlassTextField(
                                    value = draft,
                                    onValueChange = { input ->
                                        // 20 个字上限：连输入都截断，不靠下一步再拦。
                                        val trimmed = input.take(20)
                                        draft = trimmed
                                        store.updatePrefs { it.copy(nickname = trimmed) }
                                    },
                                    placeholder = "比如：小林",
                                    backdrop = backdrop
                                )
                                Spacer(Modifier.height(6.dp))
                                CastHint("现在写了 " + draft.length + " / 20 个字。")
                            }
                            4 -> OnboardingPage(
                                icon = "info",
                                title = "它不评分，也不排名",
                                body = "这里不会有卡路里、体重，也不会有「今天没达标」。身体状态只有状态，没有好坏——" +
                                    "恢复天和高能天一样正当。",
                                bullets = listOf(
                                    "指标只给词，不给数字：「恢复云量：多云」是一句话，「恢复指数 62」是一个新的比较维度。",
                                    "读不到就说读不到：显示「暂时没读到」，绝不用 0 冒充真实读数。",
                                    "每天问三个问题：早上看一眼今天是什么天气；中午不知道吃什么就翻食谱；周末在回声页看看这一周。"
                                ),
                                backdrop = backdrop
                            )
                            5 -> OnboardingPage(
                                icon = "heart",
                                title = "每天怎么用",
                                body = "首页有九个校准标签，点一下就能改写今天的结论；每天还会给你一件几分钟能做完的小事。",
                                bullets = listOf(
                                    "手动校准的优先级永远高于设备读数：你说累，就是累。",
                                    "小事都在 10 分钟以内，不打卡、不连击、不算完成率。",
                                    "做完只回一句「身体收到。」；做不动也可以什么都不点，明天的天气照样来。"
                                ),
                                backdrop = backdrop
                            )
                            // 隐私单独一页。
                            6 -> OnboardingPage(
                                icon = "shield",
                                title = "只存在这台手机上",
                                body = "默认不联网，也没有服务器和账号。只有你主动打开「小米账号（云同步）」时应用才会联网——" +
                                    "登录在小米官方页面完成，数据只在你自己的账号与本机之间。",
                                bullets = listOf(
                                    "不保存原始心率、睡眠时长、血氧数值：读数进来只为算天气，算完就丢。",
                                    "导出的备份只含日期、天气词和标签；小米账号那一路的令牌加密存在本机，随时可以断开删除。",
                                    "想看细节：设置 → 隐私与安全；想清空：那里也有「删除所有数据」。"
                                ),
                                backdrop = backdrop
                            )
                            // 末页（22.49.0 简化）：只有「欢迎使用 BodyCast」，按钮「立即开始」。
                            else -> OnboardingPage(
                                icon = "check",
                                hero = { WelcomeHeader() },
                                title = "",
                                body = "",
                                backdrop = backdrop
                            )
                        }
                        // 底部留出按钮区高度，滚到底不会被按钮压住。
                        Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 底部：上一步 / 下一步 ────────────────────────────────────
            // 22.34.0：「上一步」文字此前在标准屏宽下排不下（weight 1:2 里还挤了
            // 图标）。去掉图标、两个按钮等宽，谁也不挤谁。
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (page > 0) {
                    CastSecondaryButton(
                        text = "上一步",
                        onClick = { page -= 1 },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (page < PAGE_COUNT - 1) {
                    CastPrimaryButton(
                        text = "下一步",
                        // 称呼那一页必须填（1–20 个字），空着不给过。
                        // 按钮的 enabled 只改配色（LiquidButton 的 isInteractive 不拦点击），
                        // 所以这里在 onClick 里再挡一次——两道都要有。
                        enabled = page != 3 || store.prefs.nickname.isNotBlank(),
                        onClick = {
                            if (page != 3 || store.prefs.nickname.isNotBlank()) page += 1
                        },
                        backdrop = backdrop,
                        modifier = Modifier.weight(if (page > 0) 1f else 2f)
                    )
                } else {
                    CastPrimaryButton(
                        text = "立即开始",
                        icon = "play",
                        onClick = onDone,
                        backdrop = backdrop,
                        modifier = Modifier.weight(if (page > 0) 1f else 2f)
                    )
                }
            }
        }
    }
}

/**
 * 可玩的气象球（22.38.0）：开箱第一页的「啊哈时刻」——
 * 点一下球，五种天气轮着来，球的颜色、球里的字和一句话立刻跟着变。
 * 引导不再只是「读」，第一分钟就先「玩到」产品的核心隐喻。
 */
@Composable
private fun PlayfulWeatherOrb(modifier: Modifier = Modifier) {
    val kinds = listOf(
        WeatherKind.BALANCED,
        WeatherKind.RECOVERING,
        WeatherKind.ENERGY_HIGH,
        WeatherKind.PRESSURE,
        WeatherKind.SENSITIVE
    )
    var index by remember { mutableIntStateOf(0) }
    val kind = kinds[index]
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            key(index) {
                val pop by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(320, easing = CastMotion.Decelerate),
                    label = "orb-pop"
                )
                WeatherOrb(
                    kind = kind,
                    diameter = 128.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.92f + 0.08f * pop
                            scaleY = 0.92f + 0.08f * pop
                        }
                        .clickable(role = Role.Button) {
                            index = (index + 1) % kinds.size
                        }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点一下球，看看五种天气",
            fontSize = SkinTokens.caption,
            color = SkinTokens.muted
        )
    }
}

/**
 * 主题实时预览（22.35.0）：一张迷你「气象球卡」，
 * 选主题色 / 选深浅色都即时反映——不用进主界面才知道长什么样。
 */
@Composable
private fun ThemePreviewCard(accentStyle: Int, modifier: Modifier = Modifier) {
    val accent = GlassTheme.accentPreview(accentStyle)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "平衡天",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SkinTokens.ink
        )
        Text(
            text = "预览：按钮与强调色就是这个颜色",
            fontSize = SkinTokens.caption,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .clip(Capsule())
                .background(accent)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(
                text = "今天吃什么",
                fontSize = SkinTokens.caption,
                fontWeight = FontWeight.Bold,
                color = SkinTokens.bg
            )
        }
    }
}

/**
 * 分段进度条：一段一个步骤，当前这段按比例填满。
 * 一页一段，走一步填一段；已经走过的段整条点亮。
 */
@Composable
private fun SegmentedProgress(
    total: Int,
    current: Int,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val target = when {
                index < current -> 1f
                index > current -> 0f
                else -> 1f
            }
            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(if (animate) CastMotion.SLOW else 0, easing = CastMotion.Decelerate),
                label = "onboarding-progress-$index"
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(Capsule())
                    .background(SkinTokens.glassTrack)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(Capsule())
                        .background(SkinTokens.accent)
                )
            }
        }
    }
}

/** 引导页的统一排版：玻璃卡片 + 图标 + 标题 + 正文 + 要点（可再挂按钮）。 */
@Composable
private fun OnboardingPage(
    icon: String,
    title: String,
    body: String,
    backdrop: Backdrop,
    bullets: List<String> = emptyList(),
    hero: (@Composable () -> Unit)? = null,
    extra: @Composable (() -> Unit)? = null
) {
    // 22.47.0：卡（"框"）移到外层单例——页面切换时它是同一个实例，
    // 高度才能做拉伸/缩小动画；这里只负责内容。
    // 注意：必须自带 Column 竖排容器——去卡后多个根节点若直接落进外层
    // 的 Box（叠加布局），所有子项会叠在同一位置互相遮盖。
    Column(Modifier.fillMaxWidth()) {
    if (hero != null) {
            hero()
        } else {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SkinTokens.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                GlassIcon(icon, SkinTokens.accent, Modifier.size(24.dp))
            }
        }
        // 22.49.0：标题/正文允许留空——末页只留「欢迎使用 BodyCast」那一块。
        if (title.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SkinTokens.ink
            )
        }
        if (body.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                fontSize = SkinTokens.body,
                lineHeight = 28.sp,
                color = SkinTokens.muted
            )
        }
        if (bullets.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                bullets.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SkinTokens.accent)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = line,
                            modifier = Modifier.weight(1f),
                            fontSize = SkinTokens.caption,
                            lineHeight = 23.sp,
                            color = SkinTokens.muted
                        )
                    }
                }
            }
        }
        if (extra != null) {
            Spacer(Modifier.height(18.dp))
            Column { extra() }
        }
    }
}

/**
 * 开箱的语言选择（22.46.0）：两列大格网格。
 *
 * 为什么不用一排小 chip：语言名是 endonym（Español / Deutsch / 日本語），
 * 长短不一，流式小胶囊里挤在一起既不齐也不好点；两列大格把可选面摊开，
 * 每格整块可点、选中实心——第一眼就知道「这里有 7 个语言，我选这个」。
 */
@Composable
private fun OnboardingLanguageGrid(
    entries: List<AppLanguage>,
    backdrop: Backdrop,
    onPick: (AppLanguage) -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { language ->
                    GlassChip(
                        label = language.selfName,
                        selected = AppLanguage.current() == language,
                        onClick = { onPick(language) },
                        backdrop = backdrop,
                        localizeLabel = false,
                        modifier = Modifier
                            .weight(1f)
                            // 22.49.0：一格 52dp → 44dp。7 个语言 4 行，
                            // 之前这一页总高度超一屏、必须滚动才能看全最后一行。
                            .heightIn(min = 44.dp)
                    )
                }
                // 奇数个时补一个空位，保证最后一格保持半宽、不拉通栏。
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * 末页的欢迎头（22.47.0）：替换默认图标块，给最后一步一个"仪式感"。
 * 大号「欢迎使用」+ 一行作品名，走完这页就进主界面。
 */
@Composable
private fun WelcomeHeader() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "欢迎使用",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = SkinTokens.accent
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "BodyCast · 身体气象台",
            fontSize = SkinTokens.caption,
            color = SkinTokens.muted
        )
        Spacer(Modifier.height(10.dp))
    }
}
