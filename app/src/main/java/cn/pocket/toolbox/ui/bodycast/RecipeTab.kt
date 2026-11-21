package cn.pocket.toolbox.ui.bodycast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.Cuisine
import cn.pocket.toolbox.bodycast.FoodItem
import cn.pocket.toolbox.bodycast.MealSuggestion
import cn.pocket.toolbox.bodycast.Nutrient
import cn.pocket.toolbox.bodycast.NutritionBridge
import cn.pocket.toolbox.bodycast.RecipeLibrary
import cn.pocket.toolbox.bodycast.Slot
import cn.pocket.toolbox.bodycast.WeatherKind
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.glass.GlassIcon
import cn.pocket.toolbox.ui.skin.SkinTokens

/**
 * 页面二：今天吃什么（8.0.0 起从第三页提升上来，标题即用途）。
 *
 * 这一页回答的问题只有一个：**今天吃什么**。
 * 所以它刻意不做另外几件事——不显示热量、不显示体重、不给食物打分排名、
 * 不写「你缺什么」、不写「该吃多少克」。卡片里连一个数字都没有（见 [digitFree]）。
 *
 * 信息层级（从上到下）。重心是「元素 ↔ 食物」的对应关系，食谱只是顺手的建议：
 * 1. 元素速查（元素单选 + 直接搜）——先选一个元素看它能从哪些食物来，或者直接搜食物名
 * 2. 这些食物（受饮食偏好温和过滤的卡片列表，过滤了几项会明说，点卡片看详情）
 * 3. 配套的吃法（菜系口味 + 按身体天气配好的早/午/晚三餐，「换一批」随时换）
 * 4. 饮食建议与小贴士
 * 5. 页面尾部小字：当前范围 + 有特殊健康状况请咨询医生
 *
 * 所有可点元素都走 `CastControls.kt` 里的玻璃控件；页面里不引入 Material 3 组件。
 */
@Composable
fun RecipeTab(
    backdrop: Backdrop,
    store: BodyCastStore
) {
    val overlay = LocalCastOverlay.current
    val prefs = store.prefs

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Nutrient?>(null) }
    var cuisine by remember { mutableStateOf<Cuisine?>(null) }
    var mealOffset by remember { mutableStateOf(0L) }
    // 三餐建议直接给出，不用先点按钮——进来就知道这页是干什么的。
    // 同一天同一个种子的结果稳定，「换一批」才推进种子。
    var meals by remember { mutableStateOf(mealsFor(store.weather.kind, store.daySeed, cuisine)) }

    // 22.30.0「天气 → 元素 → 食物」联动：首页「今天吃什么」带过来的元素筛选。
    // 键用 requestRecipeFocus 本身：请求在 RecipeTab 组合前到（进来就筛上）、
    // 或组合后到（正停在食谱页时按了首页按钮），两种时序都会被应用；应用后即清，
    // 一次性请求，回退再进不会重复筛。
    LaunchedEffect(BodyCastNav.requestRecipeFocus) {
        BodyCastNav.requestRecipeFocus?.let { focus ->
            query = ""
            selected = focus
            BodyCastNav.consumeRecipeFocus()
        }
    }

    // 先取「候选池」，再做菜系与饮食偏好过滤，这样能算出到底隐藏了几项并如实告诉用户。
    val pool = remember(query, selected) {
        when {
            query.isNotBlank() -> RecipeLibrary.search(query)
            selected != null -> RecipeLibrary.byNutrient(selected!!)
            else -> RecipeLibrary.ALL
        }
    }
    val foods = remember(pool, cuisine, prefs.dietPreferences) {
        pool
            .filter { cuisine == null || it.cuisine == cuisine }
            .filter { !hiddenByDiet(it, prefs.dietPreferences) }
    }
    val hiddenCount = remember(pool, cuisine, prefs.dietPreferences) {
        pool
            .filter { cuisine == null || it.cuisine == cuisine }
            .count { hiddenByDiet(it, prefs.dietPreferences) }
    }

    CastPage(topPadding = 2.dp) {
        item {
            CastPageHeader(
                title = "今天吃什么",
                subtitle = "先查元素，再找食物；吃法只是顺手的建议。"
            )
        }

        // ---- 22.48.0：今天身体需要什么 ----
        // 这一页的主角是「元素 ↔ 食物」，而元素从哪来要有一句话交代：
        // 今天动得多（步数 / 中高强度分钟）就先按运动说，否则按身体天气说。
        // 结论与首页 hero 用的是同一个 [NutritionBridge.primary]，不会互相打脸。
        item {
            val steps = store.signals.steps
            val activeMinutes = store.signals.activityMinutes
            val focus = NutritionBridge.primary(store.weather.kind, steps, activeMinutes)
            CastCard(backdrop = backdrop) {
                Text(
                    text = "今天先补：${focus.label}",
                    fontSize = SkinTokens.cardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(4.dp))
                CastHint(
                    NutritionBridge.reason(steps, activeMinutes).takeIf { it.isNotEmpty() }
                        ?: "今天的身体天气是「${store.weather.kind.label}」——先按它来。"
                )
                Spacer(Modifier.height(8.dp))
                CastSecondaryButton(
                    text = "只看含${focus.label}的食物",
                    icon = "leaf",
                    onClick = {
                        query = ""
                        selected = focus
                    },
                    backdrop = backdrop
                )
            }
        }

        // ---- 「想吃的」清单（22.32.0 食谱闭环）----
        // 收进来就是购物清单：做饭前打开看一眼。空清单不占版面，只给一行提示。
        if (store.wishList.isNotEmpty()) {
            item {
                CastCard(backdrop = backdrop) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "想吃的",
                            fontSize = SkinTokens.cardTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = SkinTokens.ink
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "共 ${store.wishList.size} 样",
                            fontSize = SkinTokens.caption,
                            color = SkinTokens.muted
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    CastHint("做饭前看一眼就是购物清单。点名字旁边的小叉移出。")
                    Spacer(Modifier.height(8.dp))
                    GlassChipFlow(
                        items = store.wishList,
                        backdrop = backdrop,
                        label = { it },
                        isSelected = { true },
                        onToggle = { name -> store.removeWish(name) },
                        accent = SkinTokens.cMood,
                        localizeLabels = false
                    )
                }
            }
        }

        // ---------------------------------------------------------------
        // 1 元素速查：元素 ↔ 食物，这一页真正的主角
        // ---------------------------------------------------------------
        item {
            CastSectionHeader(
                title = BodyCastText.text("元素 ↔ 食物"),
                subtitle = BodyCastText.text("先选一个元素看它能从哪些食物来，或者直接搜食物名。")
            )
        }
        item {
            GlassChipFlow(
                items = NUTRIENT_FILTERS,
                backdrop = backdrop,
                label = { it?.label ?: "全部" },
                isSelected = { it == selected },
                onToggle = { nutrient ->
                    // 点元素就是「按元素看」：正在搜的词让位，否则列表看起来没反应。
                    if (query.isNotEmpty()) query = ""
                    selected = nutrient
                }
            )
        }

        val current = selected
        if (current != null && query.isBlank()) {
            item {
                CastCard(backdrop = backdrop) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(nutrientTint(current).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassIcon(current.icon, nutrientTint(current), Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = current.label,
                                fontSize = SkinTokens.cardTitle,
                                fontWeight = FontWeight.SemiBold,
                                color = SkinTokens.ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = current.blurb,
                                fontSize = SkinTokens.caption,
                                lineHeight = 22.sp,
                                color = SkinTokens.muted
                            )
                        }
                    }
                }
            }
        }

        // 搜索框紧跟元素选择：这是「元素 ↔ 食物」的另一种问法。
        item {
            CastCard(backdrop = backdrop) {
                Text(
                    text = "直接搜",
                    fontSize = SkinTokens.body,
                    fontWeight = FontWeight.SemiBold,
                    color = SkinTokens.ink
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        GlassTextField(
                            value = query,
                            onValueChange = { query = it.take(24) },
                            placeholder = "搜食物名，或者「补铁」「助眠」",
                            backdrop = backdrop
                        )
                    }
                    if (query.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        CastIconButton(
                            icon = "reset",
                            label = null,
                            onClick = { query = "" },
                            backdrop = backdrop,
                            tint = SkinTokens.muted
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                CastHint("搜「补铁」能找到含铁的食物，搜「菠菜」能直接找到那样食物。搜不到也没关系，换个词就行。")
            }
        }

        // 查询语境：一行小字说清下面这列食物是按什么来的。
        item {
            CastHint(
                BodyCastText.text(
                    when {
                        query.isNotBlank() -> "正在搜：$query —— 下面这些和「$query」有关。"
                        current == null -> "正在看：全部食物。点上面的元素，可以只看那一类。"
                        else -> "正在看：${current.label} —— 下列食物都能提供它。"
                    }
                )
            )
        }

        // ---------------------------------------------------------------
        // 2 食物列表标题（页面主体，紧跟在元素速查之后）
        // ---------------------------------------------------------------
        item {
            CastSectionHeader(
                title = if (query.isNotBlank()) "搜索「$query」" else BodyCastText.text("这些食物"),
                subtitle = when {
                    query.isNotBlank() && foods.isEmpty() -> null
                    query.isNotBlank() -> "下面这些和「$query」有关。"
                    else -> BodyCastText.text("点卡片看为什么值得吃、怎么做最省事。")
                }
            )
        }

        // ---------------------------------------------------------------
        // 3 过敏原与饮食偏好（紧贴食物列表上方，搜索时同样显示）
        // ---------------------------------------------------------------
        if (prefs.allergenAlert) {
            item {
                CastBanner(
                    shape = RoundedCornerShape(SkinTokens.cardRadius),
                    text = "过敏原提醒已打开。这里的每张卡片都写清了食材本身，如果你对某类食物过敏，请以卡片上的食材为准。",
                    backdrop = backdrop,
                    icon = "info"
                )
            }
        }
        if (prefs.dietPreferences.isNotEmpty()) {
            item {
                CastHint(
                    if (hiddenCount > 0) {
                        "按你的饮食偏好，已经隐藏了 $hiddenCount 项。想全看的话去设置里改。"
                    } else {
                        "按你的饮食偏好筛过了，这一批里没有需要隐藏的。"
                    }
                )
            }
        }

        // ---------------------------------------------------------------
        // 食物卡片：元素筛选与搜索的结果都落在这里
        // ---------------------------------------------------------------
        if (foods.isEmpty()) {
            item {
                CastEmptyPanel(
                    text = if (query.isNotBlank()) {
                        "没找到「$query」。换个词试试，比如「补铁」或者「菠菜」。"
                    } else {
                        "按你现在的饮食偏好，这一类里没有剩下的了。想全看的话，去设置里改一下偏好。"
                    },
                    backdrop = backdrop
                )
            }
        } else {
            foods.forEach { food ->
                item {
                    FoodCard(
                        food = food,
                        backdrop = backdrop,
                        onClick = { showFood(overlay, food, backdrop, store) }
                    )
                }
            }
        }

        // ---------------------------------------------------------------
        // 4 配套的吃法：口味 + 今日三餐（在食物列表之后，不是必选项）
        // ---------------------------------------------------------------
        item {
            CastSectionHeader(
                title = BodyCastText.text("配套的吃法"),
                subtitle = BodyCastText.text("把上面的元素和食物组合成一顿；不是必选项。")
            )
        }
        item {
            CastHint(BodyCastText.text("想吃什么口味？选一个菜系，上面的食物列表和三餐建议都跟着换。"))
        }
        item {
            GlassChipFlow(
                items = CUISINE_FILTERS,
                backdrop = backdrop,
                label = { it?.label ?: "全部" },
                isSelected = { it == cuisine },
                onToggle = {
                    cuisine = it
                    // 换菜系时三餐也跟着重抽（不换会留着旧菜系的结果）
                    meals = mealsFor(store.weather.kind, store.daySeed + mealOffset * 1009L, it)
                }
            )
        }
        item {
            CastSectionHeader(
                title = BodyCastText.text("今天的三餐"),
                subtitle = "根据你今天的身体天气配的：${store.weather.kind.label}",
                trailing = {
                    CastIconButton(
                        icon = "dice",
                        label = "换一批",
                        onClick = {
                            val next = mealOffset + 1L
                            mealOffset = next
                            meals = mealsFor(store.weather.kind, store.daySeed + next * 1009L, cuisine)
                        },
                        backdrop = backdrop
                    )
                }
            )
        }
        meals.forEach { meal ->
            item { MealCard(meal = meal, backdrop = backdrop, store = store) }
        }

        // ---------------------------------------------------------------
        // 5 饮食建议与小贴士
        // ---------------------------------------------------------------
        item {
            CastSectionHeader(
                title = "饮食建议与小贴士",
                subtitle = "都是搭配上的小事，不用一次全做到。"
            )
        }
        item {
            CastCard(backdrop = backdrop) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    TIPS.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(SkinTokens.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassIcon("bulb", SkinTokens.accent, Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Text(
                                text = tip,
                                modifier = Modifier.weight(1f),
                                fontSize = SkinTokens.body,
                                lineHeight = 26.sp,
                                color = SkinTokens.ink
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // 6 页面尾部小字：当前范围 + 免责声明
        // ---------------------------------------------------------------
        if (prefs.recipeScope != "all") {
            val scope = if (prefs.recipeScope == "nutrient") "只按营养素" else "只按功效"
            item { CastHint("当前范围：${BodyCastText.text(scope)}。想全看的话去设置里改。") }
        }
        item {
            CastBanner(
                text = "如有特殊健康状况，请咨询医生。这里只讲食物本身，不做诊断。",
                backdrop = backdrop,
                icon = "info",
                // 22.5.0：页面最底部的框体圆角与底部导航栏胶囊（32dp）对齐。
                shape = RoundedCornerShape(SkinTokens.cardRadius)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 食物卡片
// ---------------------------------------------------------------------------

/** 一张食物卡片。**卡片里不出现任何数字**，见 [digitFree]。 */
@Composable
private fun FoodCard(
    food: FoodItem,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    CastCard(backdrop = backdrop, onClick = onClick) {
        Text(
            text = food.name,
            fontSize = SkinTokens.cardTitle,
            fontWeight = FontWeight.SemiBold,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            food.nutrients.forEach { nutrient -> NutrientTag(nutrient) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = digitFree(food.why),
            fontSize = SkinTokens.body,
            lineHeight = 27.sp,
            color = SkinTokens.ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "怎么吃 · ${digitFree(food.howToEat)}",
            fontSize = SkinTokens.caption,
            lineHeight = 23.sp,
            color = SkinTokens.muted
        )
        // 22.41.0：烹饪方法直接上卡片——1–2 种一眼能执行的短做法（保留分钟数，那是做法不是指标）。
        val cookings = cn.pocket.toolbox.bodycast.Cookings.of(food.id)
        if (cookings.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = BodyCastText.text("烹饪") + " · " +
                    cookings.joinToString(" / ") { BodyCastText.text(it) },
                fontSize = SkinTokens.caption,
                lineHeight = 23.sp,
                color = SkinTokens.muted
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "点开看详情",
                fontSize = SkinTokens.caption,
                color = SkinTokens.faint
            )
            Spacer(Modifier.width(4.dp))
            GlassIcon("next", SkinTokens.faint, Modifier.size(14.dp))
        }
    }
}

/** 营养素小标签：一个带色的小圆角块，图标 + 名称。没有数值。 */
@Composable
private fun NutrientTag(nutrient: Nutrient) {
    val tint = nutrientTint(nutrient)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIcon(nutrient.icon, tint, Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = nutrient.label,
            fontSize = SkinTokens.tabLabel,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

/** 一餐建议：早 / 午 / 晚，各几样食物 + 一句提醒。 */
@Composable
private fun MealCard(
    meal: MealSuggestion,
    backdrop: Backdrop,
    store: BodyCastStore
) {
    val overlay = LocalCastOverlay.current
    CastCard(backdrop = backdrop) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SkinTokens.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = meal.slot.label,
                    fontSize = SkinTokens.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = SkinTokens.accent
                )
            }
            Spacer(Modifier.width(11.dp))
            Text(
                text = meal.title,
                fontSize = SkinTokens.cardTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            meal.items.forEach { item ->
                Text(
                    text = "· $item",
                    fontSize = SkinTokens.body,
                    lineHeight = 26.sp,
                    color = SkinTokens.ink
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = meal.tip,
            fontSize = SkinTokens.caption,
            lineHeight = 23.sp,
            color = SkinTokens.muted
        )
        // 22.32.0 食谱闭环：一键「就吃这个」——记进回声「这周吃了什么」，
        // 同时算一次照顾自己。同一天同一餐重复按不重复记（幂等）。
        Spacer(Modifier.height(10.dp))
        CastSecondaryButton(
            text = "就吃这个",
            icon = "check",
            onClick = {
                meal.items.forEach { store.recordMeal(it) }
                store.recordCare()
                overlay.show(
                    title = "记下了。",
                    subtitle = "回声页的「这周吃了什么」里能看到它。"
                ) {
                    CastHint("想留着以后吃的，去食物详情里点「收进想吃的」。")
                }
            },
            backdrop = backdrop
        )
    }
}

// ---------------------------------------------------------------------------
// 食物详情浮层
// ---------------------------------------------------------------------------

/** 打开食物详情。浮层由 MainActivity 的 `CastOverlayHost` 渲染，这里只负责内容。 */
private fun showFood(
    overlay: CastOverlayController,
    food: FoodItem,
    backdrop: Backdrop,
    store: BodyCastStore
) {
    var wished by mutableStateOf(store.isWished(food.name))
    overlay.show(
        title = food.name,
        subtitle = "为什么值得吃，怎么做最省事。",
        tall = true
    ) {
        CastCard(backdrop = backdrop) {
            Text(
                text = "为什么值得吃",
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = digitFree(food.why),
                fontSize = SkinTokens.body,
                lineHeight = 27.sp,
                color = SkinTokens.ink
            )
        }

        CastCard(backdrop = backdrop) {
            Text(
                text = "怎么做",
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = digitFree(food.howToEat),
                fontSize = SkinTokens.body,
                lineHeight = 27.sp,
                color = SkinTokens.ink
            )
        }

        CastCard(backdrop = backdrop) {
            Text(
                text = "含有哪些营养素",
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                food.nutrients.forEach { nutrient ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(nutrientTint(nutrient).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassIcon(nutrient.icon, nutrientTint(nutrient), Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = nutrient.label,
                                fontSize = SkinTokens.body,
                                fontWeight = FontWeight.Medium,
                                color = SkinTokens.ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = nutrient.blurb,
                                fontSize = SkinTokens.caption,
                                lineHeight = 22.sp,
                                color = SkinTokens.muted
                            )
                        }
                    }
                }
            }
        }

        CastCard(backdrop = backdrop) {
            Text(
                text = "适合的天气",
                fontSize = SkinTokens.sectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = SkinTokens.ink
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                food.forKinds.forEach { kind ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(kind.colorHex))
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = kind.label,
                                fontSize = SkinTokens.body,
                                fontWeight = FontWeight.Medium,
                                color = SkinTokens.ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = kind.motto,
                                fontSize = SkinTokens.caption,
                                lineHeight = 22.sp,
                                color = SkinTokens.muted
                            )
                        }
                    }
                }
            }
        }

        CastHint("这里只讲食物本身，不做诊断。有特殊健康状况的话，请咨询医生。")

        // 22.32.0 食谱闭环：「想吃的」= 购物清单——做饭前打开看一眼。
        Spacer(Modifier.height(6.dp))
        CastPrimaryButton(
            text = if (wished) "已收进想吃的（再点移出）" else "收进想吃的",
            icon = "heart",
            onClick = { wished = store.toggleWish(food.name) },
            backdrop = backdrop
        )
        if (wished) CastHint("「想吃的」清单在食谱页顶部，做饭前看一眼就是购物清单。")
    }
}

// ---------------------------------------------------------------------------
// 数据与工具
// ---------------------------------------------------------------------------

/** 菜系筛选条：第一个是「全部」，后面是中餐 / 西餐 / 日料 / 家常。 */
private val CUISINE_FILTERS: List<Cuisine?> = listOf<Cuisine?>(null) + Cuisine.entries

/** 营养素筛选条：第一个是「全部」，后面是全部营养素。 */
private val NUTRIENT_FILTERS: List<Nutrient?> = listOf<Nutrient?>(null) + Nutrient.entries

/** 营养素配色。只用于区分，不表示好坏——没有「更重要」的颜色。 */
private fun nutrientTint(nutrient: Nutrient): Color = when (nutrient) {
    Nutrient.PROTEIN -> SkinTokens.peach
    Nutrient.FIBER -> SkinTokens.accent
    Nutrient.PROBIOTIC -> SkinTokens.cWater
    Nutrient.OMEGA3 -> SkinTokens.blue
    Nutrient.IRON -> SkinTokens.cHeart
    Nutrient.MAGNESIUM -> SkinTokens.lavender
    Nutrient.VITAMIN_C -> SkinTokens.cMood
    Nutrient.COMPLEX_CARB -> SkinTokens.cWarning
    Nutrient.CALCIUM -> SkinTokens.lavender
    Nutrient.ZINC -> SkinTokens.cWater
    Nutrient.VITAMIN_A -> SkinTokens.cMood
    Nutrient.VITAMIN_D -> SkinTokens.peach
}

/**
 * 饮食偏好过滤规则。
 *
 * 只按**食物名**做包含匹配，命中就隐藏；不在食物数据上加「荤素」字段，
 * 这样食物表本身保持简单，规则改起来也只动这一处。
 *
 * - 「素食」       → 隐藏名字含 牛肉 / 鸡胸肉 / 三文鱼 / 沙丁鱼 / 小鱼干 的项
 * - 「不吃乳制品」 → 隐藏名字含 牛奶 / 酸奶 / 奶酪 的项
 * - 「不吃海鲜」   → 隐藏名字含 三文鱼 / 沙丁鱼 / 海带 / 紫菜 的项
 * - 「低糖」       → 隐藏名字含 香蕉 / 橙子 / 猕猴桃 / 红薯 的项
 *
 * 隐藏了几项会在食物列表上方用 [CastHint] 明说，不悄悄过滤。
 *
 * 胡萝卜 / 南瓜 / 黑芝麻 不落在上面任何一条规则里，任何偏好下都看得见。
 */
private val DIET_RULES: Map<String, List<String>> = mapOf(
    "vegetarian" to listOf("牛肉", "鸡胸肉", "三文鱼", "沙丁鱼", "小鱼干", "白切鸡", "清蒸鲈鱼", "煎牛排"),
    "dairy_free" to listOf("牛奶", "酸奶", "奶酪"),
    "seafood_free" to listOf("三文鱼", "沙丁鱼", "海带", "紫菜", "鲈鱼"),
    "low_sugar" to listOf("香蕉", "橙子", "猕猴桃", "红薯")
)

private fun hiddenByDiet(food: FoodItem, dietPreferences: Set<String>): Boolean {
    if (dietPreferences.isEmpty()) return false
    return dietPreferences.any { pref ->
        DIET_RULES[pref]?.any { keyword -> food.id.contains(keyword) } == true
    }
}

/**
 * 一餐建议。
 *
 * `RecipeLibrary.todayMeals` 在天气没有对应食物时会回落到全部食物，正常不会为空；
 * 这里仍然兜一层底——万一食物表被裁剪到只剩很少几样，也不给用户看空白。
 */
private fun mealsFor(kind: WeatherKind, seed: Long, cuisine: Cuisine?): List<MealSuggestion> {
    val generated = RecipeLibrary.todayMeals(kind, seed, cuisine)
    if (generated.any { it.items.isNotEmpty() }) return generated

    val filtered = RecipeLibrary.ALL.filter { cuisine == null || it.cuisine == cuisine }
    val pool = filtered.ifEmpty { RecipeLibrary.ALL }
    if (pool.isEmpty()) return emptyList()
    val offset = ((seed % pool.size) + pool.size).toInt() % pool.size
    val rotated = pool.drop(offset) + pool.take(offset)
    return listOf(
        MealSuggestion(
            slot = Slot.MORNING,
            title = "早上这样搭",
            items = rotated.take(2).map { it.name },
            tip = "早上不用吃多，但要吃点有蛋白的。"
        ),
        MealSuggestion(
            slot = Slot.NOON,
            title = "中午这样搭",
            items = rotated.drop(2).take(2).map { it.name },
            tip = "中午吃饱一点没关系，下午不容易找零食。"
        ),
        MealSuggestion(
            slot = Slot.EVENING,
            title = "晚上这样搭",
            items = rotated.drop(4).take(2).map { it.name },
            tip = "晚上离睡觉留出两小时，睡得会踏实些。"
        )
    )
}

/**
 * 把阿拉伯数字换成中文数字。
 *
 * 食谱数据里的做法文字带了时间（「水开下锅 8 分钟」「焯水 30 秒」）。
 * 这一页的硬规矩是卡片里一个数字都不出现，所以显示前统一转换，
 * **只改显示、不改数据**，食物表里的原文保持不动。
 *
 * **只对中文 / 日语生效**（日语汉字数字的读法和中文同源）：
 * - 英语回退的长做法段落里带的是拉丁字母 + 阿拉伯数字（`cook 8 minutes`），
 *   再转一次会写出汉字数字「八」，在英语和拉丁文界面上就是漏翻的 CJK；
 * - 拉丁文的口径是「阿拉伯数字 + 不变量词」，本来就不该转。
 * 所以英/拉两种语言原样返回。见 [BodyCastText] 头部的拉丁文口径注释。
 */
private val CN_DIGITS = "零一二三四五六七八九"

private fun digitFree(text: String): String {
    val language = AppLanguage.current()
    if (language == AppLanguage.ENGLISH || language == AppLanguage.LATIN) return text
    return Regex("\\d+\\s*").replace(text) { match ->
        val raw = match.value.trim()
        val value = raw.toIntOrNull()
        if (value == null) raw else chineseNumber(value)
    }
}

private fun chineseNumber(value: Int): String = when {
    value < 10 -> "${CN_DIGITS[value]}"
    value == 10 -> "十"
    value < 20 -> "十${CN_DIGITS[value % 10]}"
    value < 100 -> buildString {
        append(CN_DIGITS[value / 10])
        append("十")
        if (value % 10 != 0) append(CN_DIGITS[value % 10])
    }
    else -> value.toString()
}

/** 饮食建议与小贴士。全部是搭配上的小事，不涉及诊断，也没有摄入量。 */
private val TIPS: List<String> = listOf(
    "维生素 C 可以促进铁的吸收，吃完含铁的食物，顺手来个橙子或猕猴桃。",
    "草酸会影响钙的吸收，菠菜建议先焯水再炒。",
    "胡萝卜富含 β-胡萝卜素，对眼睛好，适合和油脂一起烹饪。",
    "补钙不是只有牛奶，豆制品、小鱼干、芝麻酱都算。",
    "蛋白质分散到三餐比集中在一顿更好吸收。",
    "深色叶菜的营养密度通常更高一些，菠菜、西兰花、芥蓝都算。",
    "发酵食品（酸奶、纳豆、味噌）换着吃，比只吃一种更有意思。",
    "坚果一次一小把就够，多了容易顶掉正餐。",
    "汤里的料比汤水更有内容，别只喝汤。",
    "维生素 D 在食物里不多，晒会儿太阳是最省事的一条路。",
    "锌的来源比想象中多：南瓜子、牛肉、鹰嘴豆、黑芝麻都算。"
)
