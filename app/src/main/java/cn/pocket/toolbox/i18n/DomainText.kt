package cn.pocket.toolbox.i18n

/**
 * 领域实体的英文名 / 日文名 / 拉丁文名。
 *
 * 中文是源串，直接返回；这里给的是另外三种语言的名字。
 * 键必须与 `Recipe.kt` / `Actions.kt` 里的 `sourceName` / `id` 逐字一致，
 * 对不上就会掉回 [BodyCastText.text]，界面上就会漏出中文。
 *
 * 口径（与 [BodyCastText] 相同）：食物名、动作名都是名词短语，体言止め，不加です；
 * 拉丁文一律**主格单数**，只存一个形式，不做变格、不做单复数变换。
 */
object DomainText {
    private data class N(val en: String, val ja: String, val la: String)

    private val foodNames = mutableMapOf<String, N>()
    private val actionNames = mutableMapOf<String, N>()

    private fun add(zh: String, en: String, ja: String, la: String = "") {
        foodNames[zh] = N(en, ja, la)
    }

    private fun addAction(id: String, en: String, ja: String, la: String = "") {
        actionNames[id] = N(en, ja, la)
    }

    init {
        add("鸡蛋", "Egg", "卵", "Ovum")
        add("原味酸奶", "Plain yogurt", "プレーンヨーグルト", "Iogurtum naturale")
        add("香蕉", "Banana", "バナナ", "Musa")
        add("西兰花", "Broccoli", "ブロッコリー", "Broccolis")
        add("红薯", "Sweet potato", "さつまいも", "Batata")
        add("橙子", "Orange", "オレンジ", "Citrus")
        add("猕猴桃", "Kiwi", "キウイ", "Actinidia")
        add("南瓜子", "Pumpkin seeds", "かぼちゃの種", "Semina cucurbitae")
        add("核桃", "Walnuts", "くるみ", "Iuglans")
        add("牛奶", "Milk", "牛乳", "Lac")
        add("蘑菇", "Mushrooms", "きのこ", "Fungi")
        add("胡萝卜", "Carrot", "にんじん", "Daucus")
        add("南瓜", "Pumpkin", "かぼちゃ", "Cucurbita")
        add("菠菜", "Spinach", "ほうれん草", "Spinacia")
        add("北豆腐", "Firm tofu", "木綿豆腐", "Tofu firmum")
        add("黑豆", "Black beans", "黒豆", "Fabae nigrae")
        add("糙米", "Brown rice", "玄米", "Oryza integra")
        add("海带 / 紫菜", "Kelp / seaweed", "昆布 / 海苔", "Alga")
        add("牛肉", "Beef", "牛肉", "Bubula")
        add("黑芝麻", "Black sesame", "黒ごま", "Sesamum nigrum")
        add("番茄炒蛋", "Tomato and egg stir-fry", "トマトと卵の炒めもの", "Ova cum lycopersicis")
        add("麻婆豆腐", "Mapo tofu", "麻婆豆腐")
        add("清蒸鲈鱼", "Steamed sea bass", "すずきの蒸し物")
        add("小米粥", "Millet porridge", "粟のお粥", "Puls milii")
        add("绿豆汤", "Mung bean soup", "緑豆スープ", "Ius fabarum viridium")
        add("山药", "Chinese yam", "長芋", "Dioscorea")
        add("木耳", "Wood ear mushrooms", "きくらげ", "Auricularia")
        add("红枣", "Red dates", "なつめ", "Zizypha")
        add("豆浆", "Soy milk", "豆乳", "Lac soiae")
        add("饺子", "Dumplings", "餃子")
        add("阳春面", "Plain noodles", "素ラーメン")
        add("白切鸡", "Poached chicken", "茹で鶏")
        add("皮蛋瘦肉粥", "Century egg congee", "ピータンと豚肉のお粥")
        add("螺蛳粉", "Luosifen noodles", "螺蛳粉（ルオスーフェン）")
        add("臭豆腐", "Fermented tofu", "臭豆腐")
        add("折耳根", "Fish mint", "ドクダミ（折耳根）")
        add("燕麦", "Oats", "オーツ麦", "Avena")
        add("三文鱼", "Salmon", "サーモン", "Salmo")
        add("沙丁鱼罐头", "Canned sardines", "イワシの缶詰", "Sardinae conservatae")
        add("鸡胸肉", "Chicken breast", "鶏むね肉", "Pectus gallinae")
        add("鹰嘴豆", "Chickpeas", "ひよこ豆", "Cicer")
        add("牛油果", "Avocado", "アボカド", "Persea")
        add("全麦面包", "Whole-grain bread", "全粒粉パン", "Panis integer")
        add("意大利面", "Pasta", "パスタ", "Pasta")
        add("蔬菜沙拉", "Garden salad", "野菜サラダ", "Acetaria")
        add("煎牛排", "Pan-seared steak", "ステーキ", "Bubula frixa")
        add("土豆泥", "Mashed potato", "マッシュポテト", "Patata contusa")
        add("牛油果吐司", "Avocado toast", "アボカドトースト", "Persea in pane tosto")
        add("纳豆 / 味噌", "Natto and miso", "納豆 / 味噌")
        add("味噌汤", "Miso soup", "味噌汁")
        add("荞麦面", "Soba noodles", "そば")
        add("披萨", "Pizza", "ピザ", "Pizza")
        add("三明治", "Sandwich", "サンドイッチ", "Panis fartus")
        add("烤鸡腿", "Roast chicken leg", "鶏もも肉のロースト", "Crus gallinae assum")
        add("蔬菜浓汤", "Vegetable soup", "野菜スープ", "Ius olerum")
        add("焗通心粉", "Baked macaroni", "マカロニグラタン")
        add("三文鱼刺身", "Salmon sashimi", "サーモンの刺身")
        add("寿司", "Sushi", "寿司")
        add("日式拉面", "Ramen", "ラーメン")
        add("照烧鸡肉", "Teriyaki chicken", "照り焼きチキン")
        add("茶碗蒸", "Savory egg custard", "茶碗蒸し")

        addAction("breath_456", "4-5-6 breathing", "4-5-6 呼吸", "Respiratio 4-5-6")
        addAction("breath_window", "Breathe by a window", "窓辺で深呼吸", "Respira ad fenestram")
        addAction("breath_long_exhale", "Long exhale", "長い呼気", "Exhalatio longa")
        addAction("move_stand", "Stand and walk 20 steps", "立って20歩歩く", "Surge et ambula 20 passus")
        addAction("move_neck", "Relax shoulders and neck", "肩と首をゆるめる", "Umeros et collum relaxa")
        addAction("move_walk_out", "Take a short walk", "少し外を歩く", "Paululum foras ambula")
        addAction("move_stretch_leg", "Seated leg raise", "座ったまま脚上げ", "Crus sedens tolle")
        addAction("move_desk_neck", "Desk neck reset", "デスクで肩首リセット", "Ad mensam umeros relaxa")
        addAction("move_office_stretch", "Quiet office stretch", "オフィスで目立たないストレッチ", "Extende clam in officio")
        addAction("move_quiet_walk", "Quieting walk", "気持ちを静める散歩", "Ambula ad animum sedandum")
        addAction("eat_warm_water", "Drink warm water", "温かい水を一杯", "Bibe aquam tepidam")
        addAction("eat_handful_nuts", "A small handful of nuts", "ナッツをひとつかみ", "Pugillus nucum")
        addAction("eat_fruit", "Have some fruit", "果物を一つ", "Fructus unus")
        addAction("eat_chew_slow", "Eat this meal slowly", "この食事はゆっくり", "Hoc cibo lente vescere")
        addAction("eat_three_sips", "Three sips of warm water", "温かい水を三口", "Tres haustus aquae tepidae")
        addAction("rest_eyes", "Rest your eyes for 20 seconds", "目を20秒休める", "Oculos 20 secundis quieta")
        addAction("rest_close_eyes", "Close your eyes for 3 minutes", "3分間目を閉じる", "Claude oculos 3 minuta")
        addAction("rest_screen_off", "Screen off before bed", "就寝30分前に画面を消す", "Ante somnum tegumentum claude")
        addAction("rest_flat_back", "Lie down and relax your back", "仰向けで腰をゆるめる", "Resupinus lumbos relaxa")
        addAction("rest_gaze_far", "Look into the distance", "窓辺で20秒遠くを見る", "Procul aspice 20 secunda")
        addAction("rest_body_scan", "Bedtime body scan", "寝る前のボディスキャン", "Perlustratio corporis ante somnum")
        addAction("light_sun", "Spend 5 minutes in daylight", "光の当たる場所で5分", "In luce 5 minuta mane")
        addAction("light_lamp_dim", "Dim the lights", "照明を一段暗くする", "Lumen obscura")
        addAction("light_balcony", "A little balcony light", "ベランダで光を浴びる", "In maeniano lucem cape")
        addAction("connect_one_msg", "Message someone", "誰かにメッセージを送る", "Uni homini scribe")
        addAction("connect_touch_ground", "Feet on the floor", "足の裏を床につけて2分", "Pedes in solo 2 minuta")
        addAction("connect_name_feeling", "Name the feeling", "今の感覚に名前をつける", "Sensui nomen da")
    }

    fun foodName(id: String): String = choose(foodNames[id]) ?: BodyCastText.text(id)

    /**
     * 食物的搜索索引：中文源串 + 英语名 + **当前语言的名字**。
     *
     * 少了最后一项，日语界面按「卵」就搜不到「鸡蛋」——名字只在 [foodName] 里换过，
     * 索引里没有，用户按屏幕上看到的名字搜反而搜不到。
     */
    fun foodSearchText(id: String): String = foodNames[id]?.let { entry ->
        listOf(id, entry.en, choose(entry)).filterNotNull().distinct().joinToString(" ")
    } ?: BodyCastText.searchable(id)

    fun foodWhy(id: String, source: String): String = BodyCastText.text(source)

    fun foodHow(id: String, source: String): String = BodyCastText.text(source)

    fun actionName(id: String, source: String): String = choose(actionNames[id]) ?: BodyCastText.text(source)

    fun actionGuide(id: String, source: List<String>): List<String> = source.map(BodyCastText::text)

    /** 中文界面直接用源串，所以这里返回 null；其他语言取值，槽位为空就回退英语。 */
    private fun choose(entry: N?): String? = when (AppLanguage.current()) {
        AppLanguage.SIMPLIFIED_CHINESE -> null
        AppLanguage.TRADITIONAL_CHINESE -> null
        AppLanguage.ENGLISH -> entry?.en
        // 西/德语走 EN→ES/DE 映射表（22.42.0）；表里没有就回退英语。
        AppLanguage.SPANISH -> entry?.let { EsDe.es(it.en) ?: it.en }
        AppLanguage.GERMAN -> entry?.let { EsDe.de(it.en) ?: it.en }
        AppLanguage.JAPANESE -> entry?.let { if (it.ja.isEmpty()) it.en else it.ja }
        AppLanguage.LATIN -> entry?.let { if (it.la.isEmpty()) it.en else it.la }
    }

    /** 某一门语言已经产出过的全部名字，给漏翻审计做「这条已经翻好了」的判据。 */
    fun outwardValues(language: AppLanguage): List<String> = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> emptyList()
        AppLanguage.TRADITIONAL_CHINESE -> emptyList()
        AppLanguage.ENGLISH -> (foodNames.values + actionNames.values).map { it.en }
        AppLanguage.SPANISH -> (foodNames.values + actionNames.values).mapNotNull { EsDe.es(it.en) }
        AppLanguage.GERMAN -> (foodNames.values + actionNames.values).mapNotNull { EsDe.de(it.en) }
        AppLanguage.JAPANESE -> (foodNames.values + actionNames.values).map { it.ja }
        AppLanguage.LATIN -> (foodNames.values + actionNames.values).map { it.la }
    }
}
