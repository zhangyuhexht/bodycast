package cn.pocket.toolbox.i18n

object HomeTextCatalog {
    private data class T(val en: String, val ja: String, val la: String)
    private val map = mutableMapOf<String, T>()
    private fun add(zh: String, en: String, ja: String = "", la: String = "") { map[zh] = T(en, ja, la) }

    init {
        add(
            "当前是演示数据。它用来展示完整效果，不代表你的身体。",
            "This is demo data for preview only. It does not represent your body.",
            "これは表示確認用のデモデータで、実際の体調ではありません。",
            "Data demonstrationis sunt: plenam speciem monstrant, corpus tuum non significant."
        )
        add(
            "还没有读到数据。你可以在设置里连接手环，或者直接用手动校准——",
            "No data yet. Connect a wearable in Settings, or use manual calibration.",
            "まだデータがありません。設定でウェアラブルを接続するか、手動調整を使えます。",
            "Nulla data adhuc. In Optionibus armillam coniunge aut calibratione manuali utere —"
        )
        add(
            "手动校准的优先级永远高于系统自动判断。",
            "Manual calibration always takes priority over automatic assessment.",
            "手動調整は常に自動判定より優先されます。",
            "Calibratio manualis semper sententiam automaticam praecedit."
        )
        add("用手动校准就好", "Use manual calibration", "手動で調整する", "Calibratione manuali utere")
        add(
            "在下面勾选你现在的感觉就够了。",
            "Just select how you feel below.",
            "下で今の感覚を選ぶだけです。",
            "Infra elige quomodo nunc sentias."
        )
        add(
            "不需要设备，也不需要授权。往下找到「我现在的感觉是——」。",
            "No device or permission is needed. Scroll to “Right now I feel…”.",
            "端末や権限は不要です。「今の感覚は…」まで下へ進んでください。",
            "Instrumentum et permissio non necessaria sunt. Deorsum ad \"Nunc sentio…\"."
        )
        add(
            "勾完之后，上面的天气球和播报会立刻跟着变。",
            "The weather orb and summary update immediately.",
            "選ぶと、上の体調オーブとまとめがすぐ更新されます。",
            "Post electionem globus et summarium statim mutantur."
        )
        add(
            "你说是，就是。手动校准的优先级永远高于系统自动判断。",
            "Your own feeling comes first. Manual calibration overrides automatic assessment.",
            "自分の感覚を優先します。手動調整は自動判定より常に優先されます。",
            "Quod dicis, id est. Calibratio manualis semper praecedit."
        )
        add("身体信号怎么看", "Understanding body signals", "体の信号の見方", "Quomodo signa corporis legenda")
        add(
            "恢复、压力、能量、睡眠分别说什么；没有分数，也没有及格线。",
            "Recovery, pressure, energy and sleep are shown without scores or pass marks.",
            "回復、緊張、元気、睡眠を、点数や合格基準なしで示します。",
            "Recuperatio, pressio, vis, somnus sine punctis, sine linea."
        )
        add(
            "从身体天气里挑一件最轻的小事；不是任务，做不到也没关系。",
            "Pick one easy action from today's body weather. It is not a task, and skipping it is fine.",
            "今日の体調から一番軽い行動を一つ。課題ではないので、できなくても構いません。",
            "Ex caelo corporis rem levissimam elige; non est officium."
        )
        add("—— 身体气象台", "— BodyCast", "— 体調気象台", "— BodyCast")
        add("—— BodyCast", "— BodyCast", "— BodyCast", "— BodyCast")
        add("你的手动校准：", "Your manual calibration:", "手動調整：", "Calibratio tua manualis:")
        add(
            "手动校准的优先级高于系统读数——你说的话不参与投票，它直接决定结论。",
            "Manual calibration takes priority over system readings and directly determines the result.",
            "手動調整はシステムの測定値より優先され、結果を直接決めます。",
            "Calibratio manualis mensuras praecedit et eventum directe constituit."
        )
        add(
            "没读到就是没读到，这里不会替你补一个数。",
            "Missing data stays missing; the app never fills in a number.",
            "読めないデータはそのままです。数値を補うことはありません。",
            "Quod non lectum est, non lectum manet; nullus numerus suppletur."
        )
        add("当前是演示数据。", "Demo data is active.", "デモデータを使用中です。", "Data demonstrationis.")
        add(
            "勾完立刻生效，不用点保存。",
            "Changes apply immediately; no save button is needed.",
            "選択はすぐ反映され、保存操作は不要です。",
            "Statim valet; servare non necesse est."
        )
        add(
            "这一周的身体天气、来信和趋势，都在回声页。",
            "This week's body weather, letter and trends are on the Echo page.",
            "今週の体調、手紙、傾向は記録ページにあります。",
            "Caelum corporis, epistula et inclinationes in pagina Echo sunt."
        )
        add(
            "接上手环或手动校准都可以",
            "Connect a wearable or use manual calibration",
            "ウェアラブル接続または手動調整を使えます",
            "Armilla aut calibratio manualis"
        )
        add("本地规则引擎", "On-device rule engine", "端末内ルールエンジン", "Regulae in instrumento")
        add(
            "接上设备或手动校准后，这里会有结果",
            "A result appears after connecting a device or calibrating manually",
            "端末を接続するか手動調整すると結果が表示されます",
            "Post coniunctionem aut calibrationem eventus hic apparebit"
        )
        add(
            "身体有余量，可以正常安排",
            "Your body has some reserve; plan normally",
            "体に余裕があり、普段どおり過ごせます",
            "Corpus vires habet; ut soles ordina"
        )
        add("还算稳，别排太满", "Fairly steady; leave some space", "おおむね安定。予定を詰めすぎないで", "Satis stabile; ne omnia impleas")
        add("余量不多，留点空档", "Reserve is limited; leave some room", "余裕は少なめ。空き時間を残して", "Vires paucae; spatium relinque")
        add(
            "先别加量，把该停的停下来",
            "Do not add more; pause what can wait",
            "負荷を増やさず、止められることは止めて",
            "Nihil adde; quae cessare possunt, cessa"
        )
        add(
            "设备没有提供压力数据",
            "The device did not provide pressure data",
            "端末から緊張データが提供されていません",
            "Instrumentum pressionem non praebet"
        )
        add("现在是松的", "You seem relaxed now", "今は力が抜けています", "Nunc remissum est")
        add(
            "有一点紧，属于正常",
            "A little tense, within a common range",
            "少し緊張していますが、よくある範囲です",
            "Paulum tensus, quod solitum est"
        )
        add(
            "紧的时候先松肩膀",
            "Relax your shoulders when tension rises",
            "緊張したら、まず肩の力を抜いて",
            "Cum tensus es, umeros relaxa"
        )
        add("今天适合少做决定", "Make fewer decisions today", "今日は決め事を少なめに", "Hodie pauciora decide")
        add("没有活动数据，判断不了", "No activity data to assess", "活動データがなく判定できません", "Sine notis motus iudicari non potest")
        add(
            "身体想动，顺着它",
            "Your body wants to move; follow it",
            "体が動きたがっています。自然に動いて",
            "Corpus moveri vult; sequere id"
        )
        add("不多不少", "A moderate amount", "多すぎず少なすぎず", "Modicum")
        add("能量偏低，别硬撑", "Energy is low; do not push", "元気は少なめ。無理をしないで", "Vis parva; noli contendere")
        add("今天先把力气省下来", "Save your energy today", "今日は力を温存して", "Hodie vires serva")
        add("没有睡眠数据", "No sleep data", "睡眠データがありません", "Nullae notae somni")
        add("昨晚睡透了", "Last night's sleep was restorative", "昨夜はしっかり眠れました", "Nocte praeterita bene dormivisti")
        add("睡得还行，差一点点", "Sleep was almost enough", "睡眠はほぼ十分でした", "Somnus fere sufficiens")
        add("昨晚不太够", "Last night's sleep was a little short", "昨夜の睡眠は少し不足しています", "Nocte praeterita parum")
        add(
            "缺得比较多，今天补一补",
            "Sleep was quite short; make room for rest today",
            "睡眠がかなり不足しています。今日は休む余地を",
            "Multum deest; hodie quiesce"
        )
        add(
            "云还没散完。今天不用把日程填满，能推掉的就推掉一件。",
            "The clouds have not fully cleared. Leave room in your schedule and postpone one thing if you can.",
            "雲はまだ晴れきっていません。予定を埋めず、一つ延期しても大丈夫です。",
            "Nubes nondum discesserunt. Diem ne impleas; unam rem differ si potes."
        )
        add(
            "身体有余量。趁这个窗口做点一直想做的事，但别一次用光。",
            "You have some reserve. Use this window for something you wanted to do, but do not spend it all.",
            "体に余裕があります。やりたかったことを少し。ただし使い切らないで。",
            "Vires supersunt. Hoc tempore fac quod diu voluisti, sed ne omnia simul consumas."
        )
        add(
            "风比平时大。今天不适合做重大决定，先处理手头最简单的。",
            "The wind is stronger than usual. Avoid major decisions and start with the simplest task.",
            "いつもより風が強い日です。大きな判断は避け、簡単なことから。",
            "Ventus solito fortior. Hodie magna decernere non expedit; a simplicissimo incipe."
        )
        add(
            "没有特别要提醒的。按你原本的节奏来就好。",
            "Nothing special needs attention. Keep your usual pace.",
            "特に注意することはありません。いつものペースで。",
            "Nihil praecipue monendum. Ritmo tuo utere."
        )
        add(
            "设备读到的和你的感受不完全一致。以你说的为准。",
            "The readings and your feelings do not fully match. Your own feeling takes priority.",
            "測定値と自分の感覚が一致していません。自分の感覚を優先します。",
            "Mensurae et sensus tuus non plene congruunt. Quod dicis praecedit."
        )
        add("恢复指标比平时好", "Recovery looks better than usual", "回復の状態は普段より良好", "Recuperatio solito melior")
        add("恢复指标在常见范围", "Recovery is within its usual range", "回復は普段の範囲", "Recuperatio in ambitu solito")
        add("恢复指标偏低", "Recovery is somewhat low", "回復はやや低め", "Recuperatio sub solito")
        add("恢复指标明显偏低", "Recovery is clearly low", "回復は明らかに低め", "Recuperatio clare parva")
        add("压力读数很松", "Pressure readings look relaxed", "緊張の測定値は低め", "Pressio remissa")
        add("压力读数正常", "Pressure readings are within a common range", "緊張は一般的な範囲", "Pressio solita")
        add("压力读数偏高", "Pressure readings are somewhat high", "緊張はやや高め", "Pressio super solitum")
        add("压力读数明显偏高", "Pressure readings are clearly high", "緊張は明らかに高め", "Pressio clare magna")
        add("活动量比平时多", "Activity is higher than usual", "活動量は普段より多め", "Motus solito maior")
        add("活动量正常", "Activity is within its usual range", "活動量は普段の範囲", "Motus solitus")
        add("活动量偏少", "Activity is somewhat low", "活動量はやや少なめ", "Motus sub solito")
        add("几乎没怎么活动", "There has been very little activity", "活動はごく少なめ", "Motus fere nullus")
        add("昨夜睡眠充足", "Last night's sleep was sufficient", "昨夜の睡眠は十分", "Somnos noctis praeteritae sufficiens")
        add(
            "昨夜睡眠勉强够",
            "Last night's sleep was just enough",
            "昨夜の睡眠はぎりぎり十分",
            "Somnos noctis praeteritae vix sufficiens"
        )
        add("昨夜睡眠偏少", "Last night's sleep was somewhat short", "昨夜の睡眠はやや不足", "Somnos noctis praeteritae sub solito")
        add(
            "昨夜睡眠明显不足",
            "Last night's sleep was clearly short",
            "昨夜の睡眠は明らかに不足",
            "Somnos noctis praeteritae clare deficiens"
        )
        add(
            "目前没有任何可用数据，所以这里的判断只是默认值，不代表你的身体状况。",
            "No data is available, so this is only a default display and does not represent your condition.",
            "利用できるデータがないため、これは既定表示であり実際の体調を示すものではありません。",
            "Nulla data praesto sunt; haec opinio default est, non status tuus."
        )
        add(
            "这些只是趋势，不是诊断。",
            "These are trends, not a diagnosis.",
            "これは傾向であり、診断ではありません。",
            "Hae inclinationes sunt, non diagnosis."
        )
        add(
            "按你说的来。这几件都不费力，做不完也没关系。",
            "Follow your own feeling. These actions are gentle, and you do not need to finish them all.",
            "自分の感覚に合わせます。軽い行動なので、全部できなくても大丈夫です。",
            "Sicut dicis. Hae res leves sunt; perficere non necesse est."
        )
        add(
            "今天的目标是少消耗一点，不是多做一点。",
            "Today's aim is to spend a little less energy, not do more.",
            "今日は多くこなすより、消耗を少なくする日です。",
            "Hodie minus consumere, non plus facere."
        )
        add(
            "风大的时候，把动作放小一点反而更容易完成。",
            "When the wind is strong, smaller actions are easier to complete.",
            "風が強い日は、行動を小さくすると取り組みやすくなります。",
            "Cum ventus fortis est, actiones minores faciliores sunt."
        )
        add(
            "有余量的时候做点小事，比等到没力气再硬撑更好。",
            "A small action while you have reserve is better than pushing later when tired.",
            "余裕があるうちの小さな行動は、疲れてから無理をするより楽です。",
            "Parvum facere dum vires sunt melius est quam postea contendere."
        )
        add(
            "随便挑一件做就行，不用每件都做。",
            "Choose any one action; you do not need to do them all.",
            "どれか一つで十分です。全部する必要はありません。",
            "Unum elige; omnia facere non necesse est."
        )
        add("潮水退下去的时候，沙滩才有机会被看见。", "The beach appears when the tide recedes.", "潮が引くと、砂浜が姿を見せる。")
        add("不是所有的低，都需要被修好。", "Not every low point needs to be fixed.", "すべての低調を直す必要はない。")
        add("风从背后推了一把，那就走两步。", "The wind gave you a gentle push; take a couple of steps.", "背中を風が押したなら、二歩だけ進もう。")
        add("今天的光是多的，用一点就好。", "There is plenty of light today; use only what you need.", "今日は光が多い。少し使うだけでいい。")
        add("树在风里摇，根还牢牢地站着。", "The tree sways in the wind, while its roots hold firm.", "木は風に揺れても、根はしっかり立っている。")
        add("紧的时候，先把肩膀放下来。", "When you feel tense, let your shoulders drop first.", "緊張したら、まず肩を下ろそう。")
        add("没有风，也没有雾，就这样走。", "No wind, no fog. Keep going as you are.", "風も霧もない。このまま進もう。")
        add(
            "平稳不是没有起伏，是起伏刚好在能承受的范围里。",
            "Steady does not mean flat; the changes are simply manageable.",
            "安定とは変化がないことではなく、受け止められる範囲にあること。"
        )
        add(
            "有些天气不适合解释，只适合待着。",
            "Some weather needs no explanation; it only needs space.",
            "説明しなくていい天気もある。ただそこにいればいい。"
        )
        add("敏感不是弱点，是天线更长。", "Sensitivity is not weakness; it is a longer antenna.", "敏感さは弱さではなく、アンテナが長いこと。")
        add("还没有数据", "No data yet", "まだデータがありません", "Nulla data adhuc")
        add("静息心率", "Resting heart rate", "安静時心拍数", "Pulsus quietus")
        add("心率变异性", "Heart rate variability", "心拍変動", "Variabilitas pulsus")
        add("睡眠时长", "Sleep duration", "睡眠時間", "Duratio somni")
        add("深睡占比", "Deep sleep share", "深い睡眠の割合", "Pars somni profundi")
        add("步数", "Steps", "歩数", "Passus")
        add("压力值", "Pressure reading", "緊張の測定値", "Pressio")
        add("血氧", "Blood oxygen", "血中酸素", "Oxygenium sanguinis")
        add("活动时长", "Activity duration", "活動時間", "Duratio motus")
        add("累", "Tired", "疲れた", "Lassus")
        add("紧", "Tense", "緊張", "Tensus")
        add("胀", "Bloated", "張る", "Inflatus")
        add("困", "Sleepy", "眠い", "Somnolentus")
        add("烦", "Irritated", "いらいら", "Irritatus")
        add("轻", "Light", "軽い", "Levis")
        add("空", "Empty", "空っぽ", "Vacuus")
        add("饿", "Hungry", "空腹", "Esuriens")
        add("说不清", "Hard to describe", "うまく言えない", "Difficile dictu")
        add("正在连接你的身体信号…", "Connecting to your body signals…", "体の信号に接続中…", "Signa corporis coniunguntur…")
    }

    fun translate(source: String, language: AppLanguage): String? = map[source]?.let {
        when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> source
            AppLanguage.TRADITIONAL_CHINESE -> source
            AppLanguage.ENGLISH -> it.en
            // 西/德语走 EN→ES/DE 映射表（22.42.0）；表里没有就回退英语。
            AppLanguage.SPANISH -> EsDe.es(it.en) ?: it.en
            AppLanguage.GERMAN -> EsDe.de(it.en) ?: it.en
            // 空槽回退英语，和 [BodyCastText] 一致：宁可显示英语，也不能把整句抹掉。
            AppLanguage.JAPANESE -> it.ja.ifEmpty { it.en }
            AppLanguage.LATIN -> it.la.ifEmpty { it.en }
        }
    }

    /** 某一门语言已经产出过的全部译文，给漏翻审计做「这条已经翻好了」的判据。 */
    fun outwardValues(language: AppLanguage): List<String> = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> emptyList()
        AppLanguage.TRADITIONAL_CHINESE -> emptyList()
        AppLanguage.ENGLISH -> map.values.map { it.en }
        AppLanguage.SPANISH -> map.values.mapNotNull { EsDe.es(it.en) }
        AppLanguage.GERMAN -> map.values.mapNotNull { EsDe.de(it.en) }
        AppLanguage.JAPANESE -> map.values.map { it.ja }
        AppLanguage.LATIN -> map.values.map { it.la }
    }
}
