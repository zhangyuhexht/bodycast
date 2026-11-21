package cn.pocket.toolbox.bodycast

import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.i18n.DomainText

/**
 * 营养素分类。
 *
 * [blurb] 是「它大概管什么」的一句话，**不写含量、不写每日推荐量、不写功效承诺**。
 * 食谱页的作用是帮人决定今天吃什么，不是做营养学考试。
 */
enum class Nutrient(private val sourceLabel: String, private val sourceBlurb: String, val icon: String) {
    PROTEIN("蛋白质", "修东西用的原料", "flame"),
    FIBER("膳食纤维", "让肠道有事可做", "leaf"),
    PROBIOTIC("益生菌", "肠道里的住客", "water"),
    OMEGA3("Omega-3", "对脑子友好的一类脂肪", "water"),
    IRON("铁", "容易缺，尤其是女性", "heart"),
    MAGNESIUM("镁", "肌肉放松用得着", "moon"),
    VITAMIN_C("维生素 C", "帮助吸收铁", "sun"),
    COMPLEX_CARB("复合碳水", "慢一点释放的能量", "leaf"),
    CALCIUM("钙", "骨头的日常开销", "shield"),
    // 下面三类是用户点名要能搜到的：锌 / 维生素 A / 维生素 D。
    // 它们的存在感本来就比蛋白质、铁低，所以 blurb 只写「管什么」，不写「你缺它」。
    ZINC("锌", "伤口和味觉都用得着", "shield"),
    VITAMIN_A("维生素 A", "对眼睛和皮肤友好", "sun"),
    VITAMIN_D("维生素 D", "晒太阳也能得到，食物里不多", "sun");

    val label: String get() = BodyCastText.text(sourceLabel)
    val blurb: String get() = BodyCastText.text(sourceBlurb)
    val searchText: String get() = BodyCastText.searchable(sourceLabel)
}

/**
 * 菜系。食谱页顶部可以按它筛选，三餐建议也照它抽。
 *
 * [HOME] 是「不好分中外」的那一类：香蕉、牛奶、西兰花这种全世界都吃的。
 */
enum class Cuisine(private val sourceLabel: String) {
    CHINESE("中餐"),
    WESTERN("西餐"),
    JAPANESE("日料"),
    HOME("家常");

    val label: String get() = BodyCastText.text(sourceLabel)
}

/**
 * 一种食物（或一道菜）。
 *
 * 刻意不写热量。热量一旦出现，这一页就会从「今天吃什么」变成「今天还能吃多少」，
 * 而那正是本产品要避开的方向。
 *
 * [weight] 是抽取权重：1 = 日常常见，越小越少见。螺蛳粉、折耳根这类
 * 「现实里不常吃到」的，权重给得低，随机抽到的次数自然就少——
 * 不是不出现，是不霸屏。
 */
data class FoodItem(
    private val sourceName: String,
    val nutrients: Set<Nutrient>,
    /** 一句话说明为什么值得吃 */
    private val sourceWhy: String,
    /** 怎么吃最省事 */
    private val sourceHowToEat: String,
    /** 哪些身体天气下更合适 */
    val forKinds: Set<WeatherKind>,
    val cuisine: Cuisine = Cuisine.HOME,
    val weight: Float = 1f
) {
    val id: String get() = sourceName
    val name: String get() = DomainText.foodName(sourceName)
    val why: String get() = DomainText.foodWhy(sourceName, sourceWhy)
    val howToEat: String get() = DomainText.foodHow(sourceName, sourceHowToEat)
    val searchText: String get() = listOf(
        DomainText.foodSearchText(sourceName),
        BodyCastText.searchable(sourceWhy),
        nutrients.joinToString(" ") { it.searchText }
    ).joinToString(" ")
}

/** 今天吃什么的一餐建议。 */
data class MealSuggestion(
    val slot: Slot,
    val title: String,
    val items: List<String>,
    val tip: String
)

/**
 * 食谱库。
 *
 * 以「楼下超市就能买到、不用开火半小时」为主，
 * 也收了一批中国家常菜和几道中 / 西 / 日式的正经菜——
 * 用户点名要「中国特有的菜」和「能选西餐」。
 * 不收录需要特殊渠道或复杂处理的食材——那类建议只会被收藏，不会被吃。
 */
object RecipeLibrary {

    private val ALL_WEATHER = WeatherKind.entries.toSet()

    val ALL: List<FoodItem> = listOf(
        // --------------------------------------------------------------
        // 家常（不好分中外）
        // --------------------------------------------------------------
        FoodItem(
            "鸡蛋", setOf(
                Nutrient.PROTEIN, Nutrient.VITAMIN_C, Nutrient.ZINC,
                Nutrient.VITAMIN_A, Nutrient.VITAMIN_D
            ),
            "最省事的完整蛋白，煮一个就能顶一会儿",
            "水开下锅 8 分钟，捞出来过一下凉水更好剥",
            ALL_WEATHER
        ),
        FoodItem(
            "原味酸奶", setOf(Nutrient.PROBIOTIC, Nutrient.CALCIUM, Nutrient.PROTEIN),
            "肠道和骨头都能照顾到，还不用开火",
            "挑配料表短的，糖越少越好；饭后吃比空腹吃舒服",
            setOf(WeatherKind.PRESSURE, WeatherKind.RECOVERING)
        ),
        FoodItem(
            "香蕉", setOf(Nutrient.MAGNESIUM, Nutrient.COMPLEX_CARB),
            "随身带着就能吃，腿容易抽的人可以常备",
            "两餐之间吃，别空腹当饭吃",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.PRESSURE)
        ),
        FoodItem(
            "西兰花", setOf(Nutrient.FIBER, Nutrient.VITAMIN_C, Nutrient.VITAMIN_A),
            "耐放、好处理，冰箱里囤一点也不容易坏",
            "掰小朵，蒸 5 分钟，淋点生抽就行",
            ALL_WEATHER
        ),
        FoodItem(
            "红薯", setOf(Nutrient.FIBER, Nutrient.COMPLEX_CARB, Nutrient.VITAMIN_C, Nutrient.VITAMIN_A),
            "饱腹感强，微波炉就能搞定",
            "洗净扎几个孔，微波 6 分钟，比烤的快得多",
            ALL_WEATHER
        ),
        FoodItem(
            "橙子", setOf(Nutrient.VITAMIN_C, Nutrient.FIBER),
            "剥开就能吃，顺手还能补点水",
            "白色那层筋膜别撕太干净，纤维在那里",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.SENSITIVE)
        ),
        FoodItem(
            "猕猴桃", setOf(Nutrient.VITAMIN_C, Nutrient.FIBER),
            "维生素 C 密度高，一个就够",
            "对半切开用勺子挖着吃，比削皮省事",
            setOf(WeatherKind.RECOVERING, WeatherKind.PRESSURE)
        ),
        FoodItem(
            "南瓜子", setOf(Nutrient.MAGNESIUM, Nutrient.IRON, Nutrient.PROTEIN, Nutrient.ZINC),
            "抓一把当零嘴，比薯片划算",
            "买原味未盐焗的，一次一小把",
            setOf(WeatherKind.PRESSURE, WeatherKind.SENSITIVE)
        ),
        FoodItem(
            "核桃", setOf(Nutrient.OMEGA3, Nutrient.MAGNESIUM),
            "对脑力消耗大的人比较友好",
            "一天两三个就够，剥好的容易吃多",
            setOf(WeatherKind.PRESSURE, WeatherKind.RECOVERING)
        ),
        FoodItem(
            "牛奶", setOf(Nutrient.CALCIUM, Nutrient.PROTEIN),
            "最方便的钙来源",
            "乳糖不耐就换无乳糖的，效果一样",
            ALL_WEATHER
        ),
        FoodItem(
            "蘑菇", setOf(Nutrient.FIBER, Nutrient.PROTEIN, Nutrient.VITAMIN_D),
            "鲜味足，能让少油少盐的菜也好吃",
            "别泡水洗，用湿布擦一擦更香",
            setOf(WeatherKind.BALANCED, WeatherKind.RECOVERING)
        ),
        FoodItem(
            "胡萝卜", setOf(Nutrient.VITAMIN_A, Nutrient.FIBER),
            "对眼睛好，也是深色蔬菜里最便宜好存的一种",
            "适合和油脂一起烹饪，炒着吃或者跟肉一起炖，比生啃吸收得好",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE)
        ),
        FoodItem(
            "南瓜", setOf(Nutrient.VITAMIN_A, Nutrient.FIBER, Nutrient.COMPLEX_CARB),
            "软、甜、好入口，能顶一部分主食",
            "切块蒸熟就行，皮薄的话不用削；也可以和小米一起煮粥",
            setOf(WeatherKind.RECOVERING, WeatherKind.BALANCED)
        ),

        // --------------------------------------------------------------
        // 中餐
        // --------------------------------------------------------------
        FoodItem(
            "菠菜", setOf(Nutrient.IRON, Nutrient.FIBER, Nutrient.MAGNESIUM, Nutrient.VITAMIN_A),
            "深色叶菜里最百搭的一种",
            "焯水 30 秒再炒，涩味会少很多",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "北豆腐", setOf(Nutrient.PROTEIN, Nutrient.CALCIUM, Nutrient.IRON),
            "植物蛋白里最容易买到的一种",
            "切块用厨房纸吸干水，煎的时候不容易碎",
            ALL_WEATHER,
            Cuisine.CHINESE
        ),
        FoodItem(
            "黑豆", setOf(Nutrient.FIBER, Nutrient.IRON, Nutrient.PROTEIN),
            "煮一锅可以吃好几顿，冷藏能放三四天",
            "提前泡一夜，煮的时候加点盐更容易软",
            setOf(WeatherKind.RECOVERING, WeatherKind.BALANCED),
            Cuisine.CHINESE
        ),
        FoodItem(
            "糙米", setOf(Nutrient.FIBER, Nutrient.COMPLEX_CARB, Nutrient.MAGNESIUM),
            "比白米多一点嚼头，血糖也更平缓",
            "掺三分之一到白米里一起煮，口感更容易接受",
            setOf(WeatherKind.PRESSURE, WeatherKind.BALANCED),
            Cuisine.CHINESE
        ),
        FoodItem(
            "海带 / 紫菜", setOf(Nutrient.FIBER, Nutrient.CALCIUM),
            "汤里丢一点，鲜味和矿物质都有了",
            "紫菜撕碎用开水一冲就是一碗汤",
            ALL_WEATHER,
            Cuisine.CHINESE
        ),
        FoodItem(
            "牛肉", setOf(Nutrient.IRON, Nutrient.PROTEIN, Nutrient.ZINC),
            "血红素铁吸收率高，容易累的人可以定期吃",
            "逆着纹路切薄片，快炒到变色就起锅",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "黑芝麻", setOf(Nutrient.ZINC, Nutrient.MAGNESIUM, Nutrient.CALCIUM),
            "一勺就能添点香味，锌和钙也顺带有一些",
            "买熟的、研磨过的更省事，撒在粥、酸奶或凉拌菜上",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "番茄炒蛋", setOf(Nutrient.PROTEIN, Nutrient.VITAMIN_C),
            "最下饭的家常组合，汤汁拌饭都行",
            "番茄先下锅炒出沙，再倒入蛋液，刚凝固就出锅最嫩",
            ALL_WEATHER,
            Cuisine.CHINESE
        ),
        FoodItem(
            "麻婆豆腐", setOf(Nutrient.PROTEIN, Nutrient.IRON, Nutrient.ZINC),
            "豆腐配肉末，麻辣下饭还补蛋白",
            "嫩豆腐先焯水再烧不容易碎；豆瓣酱先炒出红油再下豆腐",
            setOf(WeatherKind.PRESSURE, WeatherKind.BALANCED),
            Cuisine.CHINESE
        ),
        FoodItem(
            "清蒸鲈鱼", setOf(Nutrient.PROTEIN, Nutrient.OMEGA3),
            "蒸的鱼油少，鲜味全在里面",
            "水开后上锅蒸 8 分钟，倒掉盘里的汁再淋热油和蒸鱼豉油",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "小米粥", setOf(Nutrient.COMPLEX_CARB, Nutrient.FIBER),
            "熬出米油的那种，胃里不舒服时尤其合适",
            "小米加水小火熬 25 分钟，关火焖一会儿更稠",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "绿豆汤", setOf(Nutrient.FIBER, Nutrient.MAGNESIUM),
            "夏天常喝的那一碗，放凉加一点点冰糖",
            "绿豆提前泡 2 小时，煮到刚开花就关火，别煮太久",
            setOf(WeatherKind.PRESSURE, WeatherKind.ENERGY_HIGH),
            Cuisine.CHINESE
        ),
        FoodItem(
            "山药", setOf(Nutrient.FIBER, Nutrient.COMPLEX_CARB),
            "蒸着就很好吃，口感绵的",
            "去皮切段蒸 15 分钟，蘸白糖或者炖汤都行；处理时戴手套防手痒",
            setOf(WeatherKind.RECOVERING, WeatherKind.BALANCED),
            Cuisine.CHINESE
        ),
        FoodItem(
            "木耳", setOf(Nutrient.FIBER, Nutrient.IRON),
            "凉拌热炒都行，口感脆",
            "干木耳冷水泡发 2 小时，焯水 2 分钟再拌，一定要煮熟",
            setOf(WeatherKind.BALANCED, WeatherKind.RECOVERING),
            Cuisine.CHINESE
        ),
        FoodItem(
            "红枣", setOf(Nutrient.COMPLEX_CARB, Nutrient.VITAMIN_C),
            "当零嘴或者泡水，甜甜的一颗",
            "每天三四颗就够，泡水喝要把枣撕开",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "豆浆", setOf(Nutrient.PROTEIN, Nutrient.CALCIUM),
            "早餐摊的主角，自己在家也能打",
            "黄豆提前泡一夜，豆浆一定要彻底煮沸再喝",
            ALL_WEATHER,
            Cuisine.CHINESE
        ),
        FoodItem(
            "饺子", setOf(Nutrient.PROTEIN, Nutrient.COMPLEX_CARB),
            "菜肉主食一包齐，冻一盒随时煮",
            "水开下锅，点三次凉水，漂起来就熟了",
            ALL_WEATHER,
            Cuisine.CHINESE
        ),
        FoodItem(
            "阳春面", setOf(Nutrient.COMPLEX_CARB),
            "清汤光面，早上吃没负担",
            "猪油和生抽打底，面煮好连汤冲进去，撒点葱花",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.CHINESE
        ),
        FoodItem(
            "白切鸡", setOf(Nutrient.PROTEIN),
            "皮滑肉嫩，蘸姜蓉吃",
            "水开转小火浸熟，过一遍冰水鸡皮更爽脆",
            setOf(WeatherKind.BALANCED, WeatherKind.ENERGY_HIGH),
            Cuisine.CHINESE
        ),
        // 不常吃的，权重压低：偶尔出现解个馋，不霸屏。
        FoodItem(
            "皮蛋瘦肉粥", setOf(Nutrient.PROTEIN, Nutrient.COMPLEX_CARB),
            "绵稠咸香，早餐店的老朋友",
            "米提前冷冻一晚再煮更绵；皮蛋最后拌进去",
            setOf(WeatherKind.RECOVERING),
            Cuisine.CHINESE, 0.5f
        ),
        FoodItem(
            "螺蛳粉", setOf(Nutrient.COMPLEX_CARB, Nutrient.FIBER),
            "闻着上头吃着香，偶尔解馋可以",
            "米粉冷水下锅煮软，酸笋最后放，味道轻一半",
            setOf(WeatherKind.PRESSURE),
            Cuisine.CHINESE, 0.3f
        ),
        FoodItem(
            "臭豆腐", setOf(Nutrient.PROTEIN),
            "闻着冲吃着香，爱的人很爱",
            "空气炸锅180度10分钟，比油炸省事太多",
            setOf(WeatherKind.PRESSURE),
            Cuisine.CHINESE, 0.25f
        ),
        FoodItem(
            "折耳根", setOf(Nutrient.FIBER),
            "西南限定的味道，吃不惯的人很远就能闻到",
            "凉拌加糊辣椒面和醋，是西南朋友的本命菜",
            setOf(WeatherKind.BALANCED),
            Cuisine.CHINESE, 0.2f
        ),

        // --------------------------------------------------------------
        // 西餐
        // --------------------------------------------------------------
        FoodItem(
            "燕麦", setOf(Nutrient.FIBER, Nutrient.COMPLEX_CARB, Nutrient.MAGNESIUM),
            "能量放得慢，上午不容易突然发空",
            "热水泡 3 分钟，加一勺酸奶或几颗坚果",
            setOf(WeatherKind.PRESSURE, WeatherKind.BALANCED, WeatherKind.RECOVERING),
            Cuisine.WESTERN
        ),
        FoodItem(
            "三文鱼", setOf(Nutrient.OMEGA3, Nutrient.PROTEIN, Nutrient.VITAMIN_D),
            "好脂肪和好蛋白一起来了",
            "煎的时候皮朝下先煎，不用频繁翻面",
            setOf(WeatherKind.PRESSURE, WeatherKind.RECOVERING),
            Cuisine.WESTERN
        ),
        FoodItem(
            "沙丁鱼罐头", setOf(Nutrient.OMEGA3, Nutrient.CALCIUM, Nutrient.PROTEIN, Nutrient.VITAMIN_D),
            "不用做，开罐就能吃，价格也友好",
            "配面包或拌面都行，罐头里的汤别倒掉",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.WESTERN
        ),
        FoodItem(
            "鸡胸肉", setOf(Nutrient.PROTEIN),
            "蛋白密度高，做法对了也不柴",
            "提前用一点盐和油抓匀，冷藏半小时再煎",
            setOf(WeatherKind.ENERGY_HIGH),
            Cuisine.WESTERN
        ),
        FoodItem(
            "鹰嘴豆", setOf(Nutrient.FIBER, Nutrient.PROTEIN, Nutrient.IRON, Nutrient.ZINC),
            "罐头装的直接能用，不用泡不用煮",
            "沥干冲一下，拌橄榄油和盐就是一份小菜",
            setOf(WeatherKind.PRESSURE, WeatherKind.BALANCED),
            Cuisine.WESTERN
        ),
        FoodItem(
            "牛油果", setOf(Nutrient.OMEGA3, Nutrient.FIBER, Nutrient.MAGNESIUM),
            "脂肪质量好，饱腹感也强",
            "按一下蒂部，软了就是熟了；没软放两天",
            setOf(WeatherKind.PRESSURE, WeatherKind.RECOVERING),
            Cuisine.WESTERN
        ),
        FoodItem(
            "全麦面包", setOf(Nutrient.FIBER, Nutrient.COMPLEX_CARB),
            "看得见麦麸的那种，比白吐司顶饱",
            "烤一下抹无糖酸奶或者夹鸡蛋；别选配料表里有糖浆的",
            ALL_WEATHER,
            Cuisine.WESTERN
        ),
        FoodItem(
            "意大利面", setOf(Nutrient.COMPLEX_CARB),
            "煮一把拌酱，十分钟开饭",
            "水开加盐煮 8 分钟，酱比面先好就拌，别过凉水",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED),
            Cuisine.WESTERN
        ),
        FoodItem(
            "蔬菜沙拉", setOf(Nutrient.FIBER, Nutrient.VITAMIN_C),
            "冰箱剩什么拌什么，颜色越多越好",
            "生菜甩干水再拌，酱放边上蘸着吃比全拌进去省",
            setOf(WeatherKind.PRESSURE, WeatherKind.BALANCED),
            Cuisine.WESTERN
        ),
        FoodItem(
            "煎牛排", setOf(Nutrient.PROTEIN, Nutrient.IRON, Nutrient.ZINC),
            "一块够顶一餐，铁和蛋白都扎实",
            "提前从冰箱拿出来回温，大火每面一分半，醒肉三分钟再切",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.RECOVERING),
            Cuisine.WESTERN
        ),
        FoodItem(
            "土豆泥", setOf(Nutrient.COMPLEX_CARB, Nutrient.FIBER),
            "绵软好入口，当主食或者配菜都行",
            "蒸熟压泥加一点点牛奶和盐，比炒土豆省油",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.WESTERN
        ),
        FoodItem(
            "牛油果吐司", setOf(Nutrient.OMEGA3, Nutrient.FIBER),
            "看着网红，做起来确实省事，好脂肪也有了",
            "牛油果压碎加几滴柠檬汁防氧化，抹在全麦面包上",
            setOf(WeatherKind.PRESSURE),
            Cuisine.WESTERN, 0.6f
        ),
        FoodItem(
            "披萨", setOf(Nutrient.PROTEIN, Nutrient.CALCIUM, Nutrient.COMPLEX_CARB),
            "一张饼把主食、蛋白和蔬菜都带上，周末做起来也不费劲",
            "面饼抹番茄酱，撒芝士和配料，烤箱 220 度烤一刻钟",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED),
            Cuisine.WESTERN
        ),
        FoodItem(
            "三明治", setOf(Nutrient.PROTEIN, Nutrient.COMPLEX_CARB, Nutrient.FIBER),
            "两片面包夹一切，早餐和带饭都合适",
            "全麦面包夹鸡蛋和生菜，酱薄薄一层就够",
            ALL_WEATHER,
            Cuisine.WESTERN
        ),
        FoodItem(
            "烤鸡腿", setOf(Nutrient.PROTEIN, Nutrient.ZINC),
            "比鸡胸多汁，腌一下进烤箱就不用管了",
            "用盐、黑胡椒和蒜腌一夜，烤箱 200 度烤半小时",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED),
            Cuisine.WESTERN
        ),
        FoodItem(
            "蔬菜浓汤", setOf(Nutrient.FIBER, Nutrient.VITAMIN_A, Nutrient.VITAMIN_C),
            "把吃不完的蔬菜煮一锅，暖胃也不浪费",
            "蔬菜炒软加水煮烂，料理棒打碎后加点盐和胡椒",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.WESTERN
        ),
        // 不常吃 / 略麻烦的，权重压低一档。
        FoodItem(
            "焗通心粉", setOf(Nutrient.PROTEIN, Nutrient.CALCIUM, Nutrient.COMPLEX_CARB),
            "芝士盖满的comfort food，偶尔吃一顿很满足",
            "通心粉煮到八成熟，拌白酱铺芝士，烤到表面金黄",
            setOf(WeatherKind.PRESSURE),
            Cuisine.WESTERN, 0.5f
        ),

        // --------------------------------------------------------------
        // 日料
        // --------------------------------------------------------------
        FoodItem(
            "纳豆 / 味噌", setOf(Nutrient.PROBIOTIC, Nutrient.PROTEIN),
            "发酵食品，肠道菌群喜欢的口味",
            "味噌别用滚水冲，会杀掉活菌，温的就行",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.JAPANESE
        ),
        FoodItem(
            "味噌汤", setOf(Nutrient.PROBIOTIC, Nutrient.PROTEIN),
            "一碗热汤下肚，晚餐收尾正好",
            "水开关火再化味噌，别用滚水煮，保住里面的活菌",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.JAPANESE
        ),
        FoodItem(
            "荞麦面", setOf(Nutrient.COMPLEX_CARB, Nutrient.FIBER),
            "凉吃热吃都行，口感比普通面条韧",
            "煮好过一遍凉水更劲道；蘸汁别太咸",
            setOf(WeatherKind.BALANCED, WeatherKind.ENERGY_HIGH),
            Cuisine.JAPANESE
        ),
        FoodItem(
            "三文鱼刺身", setOf(Nutrient.OMEGA3, Nutrient.PROTEIN, Nutrient.VITAMIN_D),
            "好脂肪的好来源，冰一下直接吃，什么都不用做",
            "买可生食级的，现切现吃；肠胃敏感的日子换成煎熟的",
            setOf(WeatherKind.PRESSURE, WeatherKind.BALANCED),
            Cuisine.JAPANESE
        ),
        FoodItem(
            "寿司", setOf(Nutrient.PROTEIN, Nutrient.COMPLEX_CARB),
            "一口一个，主食和蛋白都有了，买现成的也体面",
            "米饭别压太实，醋饭晾到温热再卷",
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED),
            Cuisine.JAPANESE
        ),
        FoodItem(
            "日式拉面", setOf(Nutrient.PROTEIN, Nutrient.COMPLEX_CARB),
            "一碗热汤面下肚，整个人都松下来了",
            "汤头别用全调料包，减半加点青菜和蛋",
            setOf(WeatherKind.PRESSURE, WeatherKind.RECOVERING),
            Cuisine.JAPANESE
        ),
        FoodItem(
            "照烧鸡肉", setOf(Nutrient.PROTEIN, Nutrient.ZINC),
            "酱汁甜咸口，配米饭正好",
            "鸡腿肉煎到两面金黄，倒照烧汁收到挂住肉",
            setOf(WeatherKind.ENERGY_HIGH),
            Cuisine.JAPANESE, 0.8f
        ),
        // 费工夫一点的，权重再低一档。
        FoodItem(
            "茶碗蒸", setOf(Nutrient.PROTEIN, Nutrient.CALCIUM),
            "滑嫩的热蛋羹，胃里不舒服时也能吃",
            "蛋液加高汤和一点盐，盖保鲜膜扎孔，中火蒸到刚凝固",
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE),
            Cuisine.JAPANESE, 0.6f
        )
    )

    /**
     * 搜索别名表：口语词 → 营养素。
     *
     * 用户不会去搜「维生素 C」这个词，他会搜「补维 C」「吃了眼睛好」「睡不着」。
     * 这张表只做一件事：把口语翻译成营养素，命中哪些食物仍然完全由 [ALL] 决定，
     * 不额外造食物、不额外给「推荐指数」。
     */
    private val SEARCH_ALIASES: Map<String, Set<Nutrient>> = mapOf(
        "助眠" to setOf(Nutrient.MAGNESIUM),
        "失眠" to setOf(Nutrient.MAGNESIUM),
        "睡" to setOf(Nutrient.MAGNESIUM),
        "放松" to setOf(Nutrient.MAGNESIUM),
        "抽筋" to setOf(Nutrient.MAGNESIUM, Nutrient.CALCIUM),
        "护眼" to setOf(Nutrient.VITAMIN_A, Nutrient.VITAMIN_C, Nutrient.OMEGA3),
        "眼睛" to setOf(Nutrient.VITAMIN_A, Nutrient.VITAMIN_C, Nutrient.OMEGA3),
        "视力" to setOf(Nutrient.VITAMIN_A, Nutrient.VITAMIN_C, Nutrient.OMEGA3),
        "夜盲" to setOf(Nutrient.VITAMIN_A),
        "皮肤" to setOf(Nutrient.VITAMIN_A, Nutrient.ZINC),
        "维A" to setOf(Nutrient.VITAMIN_A),
        "维生素A" to setOf(Nutrient.VITAMIN_A),
        "补维生素A" to setOf(Nutrient.VITAMIN_A),
        "维D" to setOf(Nutrient.VITAMIN_D),
        "维生素D" to setOf(Nutrient.VITAMIN_D),
        "补维生素D" to setOf(Nutrient.VITAMIN_D),
        "晒太阳" to setOf(Nutrient.VITAMIN_D),
        "阳光" to setOf(Nutrient.VITAMIN_D),
        "锌" to setOf(Nutrient.ZINC),
        "补锌" to setOf(Nutrient.ZINC),
        "伤口" to setOf(Nutrient.ZINC),
        "味觉" to setOf(Nutrient.ZINC),
        "肠道" to setOf(Nutrient.FIBER, Nutrient.PROBIOTIC),
        "消化" to setOf(Nutrient.FIBER, Nutrient.PROBIOTIC),
        "便秘" to setOf(Nutrient.FIBER),
        "补血" to setOf(Nutrient.IRON),
        "贫血" to setOf(Nutrient.IRON),
        "铁" to setOf(Nutrient.IRON),
        "钙" to setOf(Nutrient.CALCIUM),
        "骨头" to setOf(Nutrient.CALCIUM, Nutrient.VITAMIN_D),
        "骨" to setOf(Nutrient.CALCIUM, Nutrient.VITAMIN_D),
        "肌肉" to setOf(Nutrient.PROTEIN, Nutrient.MAGNESIUM),
        "蛋白" to setOf(Nutrient.PROTEIN),
        "增肌" to setOf(Nutrient.PROTEIN),
        "脑子" to setOf(Nutrient.OMEGA3),
        "脑" to setOf(Nutrient.OMEGA3),
        "记忆" to setOf(Nutrient.OMEGA3),
        "专注" to setOf(Nutrient.OMEGA3, Nutrient.COMPLEX_CARB),
        "情绪" to setOf(Nutrient.OMEGA3, Nutrient.MAGNESIUM),
        "心情" to setOf(Nutrient.OMEGA3, Nutrient.MAGNESIUM),
        "压力" to setOf(Nutrient.MAGNESIUM),
        "血糖" to setOf(Nutrient.FIBER, Nutrient.COMPLEX_CARB),
        "能量" to setOf(Nutrient.COMPLEX_CARB),
        "维C" to setOf(Nutrient.VITAMIN_C),
        // 这里刻意**不**放泛化的「维生素」→ VITAMIN_C：
        // 否则搜「维生素 A」会被它拽出一堆维生素 C 的食物。
        // 泛化词交给 [search] 里的营养素名匹配处理，那里会同时命中 A / C / D。
        "纤维" to setOf(Nutrient.FIBER),
        "益生菌" to setOf(Nutrient.PROBIOTIC),
        "发酵" to setOf(Nutrient.PROBIOTIC),
        "omega" to setOf(Nutrient.OMEGA3),
        "鱼油" to setOf(Nutrient.OMEGA3),
        "脂肪" to setOf(Nutrient.OMEGA3),
        "免疫" to setOf(Nutrient.VITAMIN_C, Nutrient.PROBIOTIC),
        "抵抗力" to setOf(Nutrient.VITAMIN_C, Nutrient.PROBIOTIC),
        "感冒" to setOf(Nutrient.VITAMIN_C),
        "碳水" to setOf(Nutrient.COMPLEX_CARB),
        "主食" to setOf(Nutrient.COMPLEX_CARB),
        "饱腹" to setOf(Nutrient.FIBER, Nutrient.PROTEIN),
        "sleep" to setOf(Nutrient.MAGNESIUM),
        "relax" to setOf(Nutrient.MAGNESIUM),
        "eye" to setOf(Nutrient.VITAMIN_A, Nutrient.VITAMIN_C, Nutrient.OMEGA3),
        "skin" to setOf(Nutrient.VITAMIN_A, Nutrient.ZINC),
        "digestion" to setOf(Nutrient.FIBER, Nutrient.PROBIOTIC),
        "gut" to setOf(Nutrient.FIBER, Nutrient.PROBIOTIC),
        "energy" to setOf(Nutrient.COMPLEX_CARB),
        "immunity" to setOf(Nutrient.VITAMIN_C, Nutrient.PROBIOTIC),
        "сон" to setOf(Nutrient.MAGNESIUM),
        "глаза" to setOf(Nutrient.VITAMIN_A, Nutrient.OMEGA3),
        "пищеварение" to setOf(Nutrient.FIBER, Nutrient.PROBIOTIC),
        "энергия" to setOf(Nutrient.COMPLEX_CARB),
        "睡眠" to setOf(Nutrient.MAGNESIUM),
        "目" to setOf(Nutrient.VITAMIN_A, Nutrient.OMEGA3),
        "消化" to setOf(Nutrient.FIBER, Nutrient.PROBIOTIC),
        "元気" to setOf(Nutrient.COMPLEX_CARB)
    )

    /**
     * 搜索别名表：口语词 → 具体食物名。
     *
     * 有些词指的就是某几样东西（「早餐」「零食」），按营养素扩散会跑偏，
     * 直接点名更准。名字必须与 [ALL] 里的 `name` 完全一致。
     */
    private val SEARCH_FOOD_ALIASES: Map<String, Set<String>> = mapOf(
        "早餐" to setOf("鸡蛋", "燕麦", "牛奶", "原味酸奶", "豆浆", "小米粥", "阳春面"),
        "宵夜" to setOf("牛奶", "香蕉", "原味酸奶"),
        "夜宵" to setOf("牛奶", "香蕉", "原味酸奶"),
        "零食" to setOf("南瓜子", "核桃", "原味酸奶", "红枣"),
        "汤" to setOf("海带 / 紫菜", "纳豆 / 味噌", "味噌汤", "绿豆汤"),
        "下饭" to setOf("蘑菇", "北豆腐", "牛肉", "番茄炒蛋", "麻婆豆腐"),
        "带饭" to setOf("糙米", "黑豆", "鸡胸肉", "西兰花", "饺子"),
        "免煮" to setOf("沙丁鱼罐头", "鹰嘴豆", "原味酸奶", "香蕉", "全麦面包"),
        "快手菜" to setOf("番茄炒蛋", "阳春面", "蔬菜沙拉", "意大利面"),
        "宴客" to setOf("清蒸鲈鱼", "白切鸡", "煎牛排")
    )

    /**
     * 搜索。
     *
     * 匹配四类输入，命中任意一类即返回：
     * 1. **食物名**——「菠菜」「胡萝卜」直接按 `name` 包含匹配；
     * 2. **营养素**——`nutrient.label` 忽略空格包含匹配，所以「铁」「锌」「维生素C」
     *    「维生素 A」「Omega-3」都能搜到；搜泛化的「维生素」会同时命中 A / C / D 三类；
     * 3. **口语别名**——[SEARCH_ALIASES] 翻译成营养素，所以「补铁」「补锌」「助眠」
     *    「护眼」「晒太阳」也能搜到；
     * 4. **点名的食物**——[SEARCH_FOOD_ALIASES] 直接指到具体食物，例如「早餐」。
     *
     * 别名匹配是**双向包含**：搜「补铁」能命中别名「铁」，搜「铁」也能命中别名「补血」。
     * 这样用户不用猜自己该输入哪个词。
     *
     * 空查询返回全部；搜不到返回空列表（由页面渲染空状态，不抛异常、不报错）。
     */
    fun search(query: String): List<FoodItem> {
        val q = query.trim()
        if (q.isEmpty()) return ALL

        val hitNutrients = linkedSetOf<Nutrient>()
        SEARCH_ALIASES.forEach { (key, value) ->
            if (aliasMatches(q, key)) hitNutrients += value
        }
        val hitNames = linkedSetOf<String>()
        SEARCH_FOOD_ALIASES.forEach { (key, value) ->
            if (aliasMatches(q, key)) hitNames += value
        }

        return ALL.filter { item ->
            item.searchText.contains(q, ignoreCase = true) ||
                    item.nutrients.any { looseContains(it.searchText, q) } ||
                    item.nutrients.any { it in hitNutrients } ||
                    hitNames.any { it == item.id }
        }
    }

    /**
     * 忽略空格和连字符的包含匹配。
     *
     * 营养素名里带空格（「维生素 C」「维生素 A」「Omega-3」），而用户打出来的是
     * 「维生素C」「维生素A」。两边都去掉空格再比，搜「维生素A」就只命中维生素 A，
     * 不会顺带把维生素 C 的食物也带出来。
     */
    private fun looseContains(label: String, query: String): Boolean =
        label.contains(query, ignoreCase = true) ||
                normalize(label).contains(normalize(query), ignoreCase = true)

    private fun normalize(text: String): String =
        text.replace(" ", "").replace("-", "").replace("－", "")

    /** 别名双向包含：query 里含别名，或别名里含 query，都算命中。 */
    private fun aliasMatches(query: String, key: String): Boolean =
        query.contains(key, ignoreCase = true) || key.contains(query, ignoreCase = true)

    fun byNutrient(nutrient: Nutrient): List<FoodItem> = ALL.filter { it.nutrients.contains(nutrient) }

    /** 按身体天气挑食物。天气是唯一的筛选维度，不做「推荐指数」。 */
    fun forWeather(kind: WeatherKind): List<FoodItem> =
        ALL.filter { it.forKinds.contains(kind) }.ifEmpty { ALL }

    /**
     * 今天吃什么。
     *
     * 用日期做种子，保证同一天进来看到的建议是稳定的；
     * 否则每次刷新都换一批，用户就不会真的照做。
     * 「换一批」由页面把种子往后推。
     *
     * [cuisine] 非空时只从该菜系抽；抽取按 [FoodItem.weight] 加权，
     * 常见的菜霸屏，少见的菜（螺蛳粉、折耳根）偶尔露个面。
     */
    fun todayMeals(kind: WeatherKind, seed: Long, cuisine: Cuisine? = null): List<MealSuggestion> {
        val base = forWeather(kind)
        // 菜系回退只能逐级放宽，绝不跨菜系：先按「天气+菜系」，太少就放宽天气
        // 保留菜系（用户选了西餐就永远不该抽出饺子），全菜系保底只在
        // 该菜系整个没有适配当前天气且库里也没有时才发生。
        val byWeatherAndCuisine = if (cuisine == null) base else base.filter { it.cuisine == cuisine }
        val effective = when {
            cuisine == null -> base
            // 阈值给到 6：小众菜系（日料）在特定天气下只剩三四道，
            // 池子太小会在三餐里反复出现同一道菜。宁可放宽天气维度，
            // 也要保住菜系和多样性。
            byWeatherAndCuisine.size >= 6 -> byWeatherAndCuisine
            else -> ALL.filter { it.cuisine == cuisine }.ifEmpty { base }
        }
        val slotOrder = listOf(Slot.MORNING, Slot.NOON, Slot.EVENING)
        // 抽「整天」的一次性 9 道（pool 够大时），再切给早/中/晚：
        // 这样早餐和晚餐不会撞同一道菜。pool 小于 9 时自然会有重复，
        // 但不会在同一天里把同一道菜重复三次。
        val wholeDay = pickWeighted(effective, slotOrder.size * 3, seed)
        return slotOrder.mapIndexed { index, slot ->
            val picks = wholeDay.drop(index * 3).take(3).ifEmpty {
                pickWeighted(effective, 3, seed + index * 31L)
            }
            MealSuggestion(
                slot = slot,
                title = when (slot) {
                    Slot.MORNING -> BodyCastText.text("早上这样搭")
                    Slot.NOON -> BodyCastText.text("中午这样搭")
                    else -> BodyCastText.text("晚上这样搭")
                },
                items = picks.map { it.name },
                tip = when (slot) {
                    Slot.MORNING -> BodyCastText.text("早上不用吃多，但要吃点有蛋白的。")
                    Slot.NOON -> BodyCastText.text("中午吃饱一点没关系，下午不容易找零食。")
                    else -> BodyCastText.text("晚上离睡觉留出两小时，睡得会踏实些。")
                }
            )
        }
    }

    /**
     * 按权重无放回抽取 [count] 个（Efraimidis-Spirakis：
     * 每个元素生成 u^(1/w) 的钥匙，钥匙越大越优先）。
     * 权重 1 的菜和权重 0.2 的菜会按权重比例出现在三餐里。
     */
    private fun pickWeighted(pool: List<FoodItem>, count: Int, seed: Long): List<FoodItem> {
        if (pool.size <= count) return pool
        val random = java.util.Random(seed)
        return pool
            .map { it to Math.pow(random.nextDouble(), 1.0 / it.weight) }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
    }
}

/**
 * 天气 → 今天该先补的元素（产品主轴「天气 → 元素 → 食物」的桥，22.30.0）。
 *
 * 口径（与《答辩要点》第六节一致）：
 * - 压力天 → 镁（肌肉放松用得着）；
 * - 敏感天 → 镁（身体在说话，先把矿物质补稳）；
 * - 恢复天 → 蛋白质（修东西用的原料）；
 * - 高能天 / 平衡天 → 复合碳水（慢一点释放的能量）。
 *
 * 一个天气只给 **一个** 首选元素：给两个以上，用户就要开始比较，
 * 而「今天先补什么」只有一个答案时才是导航。备选在食谱页里自己看得到。
 *
 * 22.48.0：把**运动读数**接进来。天气是「身体现在的样子」，步数和中高强度
 * 分钟是「今天身体做了什么」——两个一起看，推荐才站得住：
 * 走了 8000 步以上、或者中高强度满 30 分钟，那一天先给镁（出汗流失的矿物质、
 * 放松下来的肌肉），其余情况照旧按天气给。阈值只有这两个（不进设置、不给档位），
 * 因为「今天动不动得够多」本来就该是一条线，不该让用户自己去调。
 */
object NutritionBridge {

    /** 今天的运动量算不算「动得够多」。 */
    fun activeDay(steps: Int?, activityMinutes: Int?): Boolean =
        (activityMinutes ?: 0) >= ACTIVE_MINUTES || (steps ?: 0) >= ACTIVE_STEPS

    private const val ACTIVE_STEPS = 8_000
    private const val ACTIVE_MINUTES = 30

    /** 只看身体天气的首选元素（没有运动数据时的口径，也用于天气变化通知）。 */
    fun weatherPrimary(kind: WeatherKind): Nutrient = when (kind) {
        WeatherKind.PRESSURE -> Nutrient.MAGNESIUM
        WeatherKind.SENSITIVE -> Nutrient.MAGNESIUM
        WeatherKind.RECOVERING -> Nutrient.PROTEIN
        WeatherKind.ENERGY_HIGH -> Nutrient.COMPLEX_CARB
        WeatherKind.BALANCED -> Nutrient.FIBER
    }

    /**
     * 今天首推的元素。食谱页的筛选入口从这里取。
     *
     * [steps] / [activityMinutes] 是今天的读数，可以为空（没读到就是空）——
     * 空的时候退回天气口径，不编一个数字来凑。
     */
    fun primary(kind: WeatherKind, steps: Int? = null, activityMinutes: Int? = null): Nutrient =
        if (activeDay(steps, activityMinutes)) Nutrient.MAGNESIUM else weatherPrimary(kind)

    /**
     * 「今天为什么先补这个」。
     *
     * 只讲**观察得到的数字**，不讲功效、不写剂量；含数字的三句话都走
     * [cn.pocket.toolbox.i18n.BodyCastText] 的模板翻译。返回空串 = 今天没有
     * 值得一提的运动量，调用方用天气那一句说明。
     */
    fun reason(steps: Int?, activityMinutes: Int?): String {
        val s = steps ?: 0
        val a = activityMinutes ?: 0
        return when {
            s >= ACTIVE_STEPS && a >= ACTIVE_MINUTES ->
                "今天走了 $s 步，还有 $a 分钟中高强度运动——先补矿物质，把出汗流失的补回来。"
            s >= ACTIVE_STEPS ->
                "今天走了 $s 步——先补矿物质，把出汗流失的补回来。"
            a >= ACTIVE_MINUTES ->
                "今天有 $a 分钟中高强度运动——先补矿物质，肌肉放松用得着。"
            else -> ""
        }
    }

    /** 顺路可以看的第二元素——不写「必须」，只写「也可以看看」。 */
    fun secondary(kind: WeatherKind): Nutrient = when (kind) {
        WeatherKind.PRESSURE -> Nutrient.OMEGA3
        WeatherKind.SENSITIVE -> Nutrient.CALCIUM
        WeatherKind.RECOVERING -> Nutrient.IRON
        WeatherKind.ENERGY_HIGH -> Nutrient.PROTEIN
        WeatherKind.BALANCED -> Nutrient.VITAMIN_C
    }
}
