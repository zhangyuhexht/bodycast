package cn.pocket.toolbox.i18n

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object BodyCastText {
    private data class Entry(val en: String, val ja: String, val la: String)

    private val entries = mutableMapOf<String, Entry>()

    /**
     * 登记一条中文源串的译文。
     *
     * 译文口径（补齐日语时定下，改文案前先读一遍）：
     * - **句子**（说明、设置副标题、通知正文、隐私页）一律 です・ます 体；
     * - **标签 / 状态词**（`回復の日`、`未設定`、`許可済み`）用体言止め或名词短语，不加です；
     * - **气象球 motto、诗句、来信**允许常体——那是刻意的文学语气，和中文原文的调子一致，
     *   **不要当 bug 改掉**。
     *
     * 拉丁文（`la`）口径（补齐拉丁文时定下，同样先读一遍再改）：
     * - **风格是「简化的技术拉丁」**：短标签、固定短语，不为了文法漂亮而绕变格；
     * - **名词名单**（食物、动作、营养素、天气词）一律**主格单数**，表里只存一个形式；
     * - **含占位符的模板**只允许「主格 + 主格」并置或前置词短语
     *   （`Caelum corporis · %s`、`circa %s minuta`），禁止把 `%s` 塞进需要属格/与格的位置；
     * - **步骤 / 做法用祈使句**（`Bibe aquam tepidam.`、`Per X minuta coque.`），天然绕开变格；
     * - **不做单复数变换**，数字用阿拉伯数字 + 不变量词（`%s dies` 保持 `dies`）。
     *
     * `ja` / `la` 留空表示还没翻：界面会回退到英语，并往 [TranslationAudit] 记一条缺口，
     * 在设置 → 开发者模式 →「漏翻审计」里能看到还有多少条在回退。
     */
    private fun add(zh: String, en: String, ja: String = "", la: String = "") {
        entries[zh] = Entry(en, ja, la)
    }

    init {
        add("简体中文", "简体中文", "简体中文", "简体中文")
        add("繁體中文", "繁體中文", "繁體中文", "繁體中文")
        add("首页", "Home", "ホーム", "Domus")
        add("食谱", "Food", "食事", "Cibus")
        add("回声", "Echo", "記録", "Echo")
        add("设置", "Settings", "設定", "Optiones")
        add("返回", "Back", "戻る", "Retro")
        add("当前", "Current", "選択中", "Electum")
        add("确定", "OK", "決定", "Bene")
        add("先不要", "Not now", "あとで", "Non nunc")
        add("全部", "All", "すべて", "Omnia")
        add("小时", "Hour", "時", "Hora")
        add("分钟", "Minute", "分", "Minuta")
        add("就用这个时间", "Use this time", "この時間にする", "Hoc tempus eligere")
        add(
            "选一个你通常还没睡、但也不在忙的时间",
            "Choose a time when you are usually awake and free.",
            "普段起きていて、忙しくない時間を選んでください。",
            "Elige horam qua plerumque vigilas neque occupatus es."
        )

        add("设置与偏好", "Settings and preferences", "設定と好み", "Optiones et praeferentiae")
        add("数据源管理", "Data sources", "データソース", "Fontes datorum")
        add("身体天气校准", "Body weather calibration", "体調の調整", "Calibratio caeli corporis")
        add("微行动处方偏好", "Micro-action preferences", "小さな行動の設定", "Praeferentiae actionum parvarum")
        add("食谱指南", "Food guide", "食事ガイド", "Ductor cibi")
        add("通知与提醒", "Notifications", "通知とリマインダー", "Notae et admonitiones")
        add("隐私与安全", "Privacy and security", "プライバシー", "Privatum et securitas")
        add("外观与体验", "Appearance and experience", "表示と操作", "Species et usus")
        add("关于", "About", "アプリについて", "De hoc")
        add("开发者选项", "Developer options", "開発者向け", "Optiones programmatoris")
        add("日志开关", "Logging", "ログ")
        add("只影响本机日志输出。", "Only affects logs on this device.", "この端末のログだけに影響します。")
        add("模拟数据快捷入口", "Load demo data", "デモデータを読み込む")
        add("一键把演示读数填进来。", "Fill in demo readings.", "デモの測定値を入力します。")
        add(
            "当前是演示数据，不是你的真实身体状态。",
            "This is demo data, not your actual state.",
            "これはデモデータで、実際の体調ではありません。",
            "Data demonstrationis, non status corporis tui."
        )
        add(
            "数据从哪来、身体天气怎么定、提醒怎么发，都分门别类放在这里。",
            "Manage data, body weather, reminders and more.",
            "データ、体調、通知などをここで管理します。",
            "Origo datorum, caelum corporis, admonitiones: omnia hic disposita."
        )
        add(
            "手环接入、Health Connect 授权、手动同步。",
            "Wearables, Health Connect access and manual sync.",
            "ウェアラブル、Health Connect、手動同期。",
            "Armilla, permissio Health Connect, synchronizatio manualis."
        )
        add(
            "默认天气、校准标签、敏感度。",
            "Default weather, calibration tags and sensitivity.",
            "既定の体調、タグ、感度。",
            "Caelum default, tituli calibrationis, sensibilitas."
        )
        add(
            "时长预算、可用场景、语音引导。",
            "Duration, situations and voice guidance.",
            "時間、場面、音声ガイド。",
            "Tempus, loci, ductor vocalis."
        )
        add(
            "饮食偏好、过敏原提醒、搜索范围。",
            "Diet preferences, allergen notes and search scope.",
            "食事の好み、アレルゲン、検索範囲。",
            "Praeferentiae victus, allergena, ambitus quaerendi."
        )
        add("现在全部关着。", "All are off.", "すべてオフです。", "Omnia nunc clausa.")
        add(
            "数据只存在这台手机上，导出与删除也在这里。",
            "Data stays on this phone. Export or delete it here.",
            "データはこの端末だけに保存されます。書き出しと削除もここです。",
            "Data in hoc instrumento manent; exportatio et deletio hic."
        )
        add(
            "开发者、版本、开源许可。",
            "Developers, version and open-source licenses.",
            "開発者、バージョン、ライセンス。",
            "Programmator, versio, licentiae."
        )
        add(
            "看着舒服，比功能多更重要。",
            "Comfort matters more than more features.",
            "機能の多さより、見やすさを大切にします。",
            "Commoditas plus quam multitudo functionum."
        )
        add("主题", "Theme", "テーマ", "Thema")
        add("浅色", "Light", "ライト", "Lumen")
        add("深色", "Dark", "ダーク", "Tenebrae")
        add("跟随系统", "System", "システム", "Systema")
        add("动画效果", "Animations", "アニメーション", "Motus")
        add(
            "关掉之后页面切换和按压动画会变简单，省电一些。",
            "Turning this off simplifies motion and may save power.",
            "オフにすると動きが簡素になり、省電力になります。",
            "Motum claudis: transitus et pressiones simplices fiunt, vis parcitur."
        )
        add("字体大小", "Text size", "文字サイズ", "Magnitudo litterarum")
        add("小", "Small", "小", "Parvus")
        add("标准", "Standard", "標準", "Medius")
        add("大", "Large", "大", "Magnus")
        add("语言", "Language", "言語", "Lingua")
        add(
            "切换后整个应用会立即更新。",
            "The whole app updates immediately.",
            "切り替えるとアプリ全体がすぐ更新されます。",
            "Tota applicatio statim renovatur."
        )

        add("连接状态", "Connection status", "接続状態", "Status conexionis")
        add("点一下可以重新检查。", "Tap to check again.", "タップして再確認します。", "Tange ut iterum inspicias.")
        add("正在检查…", "Checking…", "確認中…", "Inspectio…")
        // 状态词用体言止め，不加です・ます（存量译文按口径统一过一遍，不是重译）。
        add("已就绪", "Ready", "利用可能", "Paratus")
        add(
            "需要安装系统健康服务",
            "Health Connect must be installed",
            "Health Connect のインストールが必要",
            "Health Connect installanda est"
        )
        add("需要更新系统健康服务", "Health Connect must be updated", "Health Connect の更新が必要", "Health Connect renovanda est")
        add("这台设备暂不支持", "This device is not supported", "この端末は非対応", "Hoc instrumentum non sustinet")
        add("连接小米手环", "Connect Xiaomi band", "Xiaomi バンドを接続", "Armillam Xiaomi coniungere")
        add("连接华为手环", "Connect Huawei band", "Huawei バンドを接続", "Armillam Huawei coniungere")
        add(
            "经过小米运动健康汇总到系统健康服务。",
            "Sync through Mi Fitness to Health Connect.",
            "Mi Fitness 経由で Health Connect に同期します。",
            "Per Mi Fitness ad Health Connect."
        )
        add(
            "经过华为运动健康汇总到系统健康服务。",
            "Sync through Huawei Health to Health Connect.",
            "Huawei Health 経由で Health Connect に同期します。",
            "Per Huawei Health ad Health Connect."
        )
        add("Health Connect 授权状态", "Health Connect access", "Health Connect の権限", "Permissio")
        add("还没有授权任何类型", "No data types allowed yet", "許可済みの項目はまだありません", "Nulla genera permissa")
        add("已授权数据类型", "Allowed data types", "許可済みのデータ", "Genera datorum permissa")
        add(
            "没授权的类型不影响其他功能。",
            "Other features still work without them.",
            "未許可の項目があっても他の機能は使えます。",
            "Genera non permissa alia munera non impediunt."
        )
        add("已授权", "Allowed", "許可済み", "Permissum")
        add("未授权", "Not allowed", "未許可", "Non permissum")
        add("去授权", "Grant access", "権限を設定", "Permittere")
        add("撤销授权", "Revoke access", "権限を取り消す", "Permissum revocare")
        add("撤销", "Revoke", "取り消す", "Revocare")
        add("手动同步", "Sync now", "今すぐ同期", "Synchronizare")
        add("正在读…", "Reading…", "読み込み中…", "Lectio…")
        add("已经读到了最新数据。", "Latest data loaded.", "最新データを読み込みました。", "Data recentissima lecta est.")
        add(
            "暂时没读到，可能是还没授权，或手环还没同步到系统。",
            "No data yet. Check access and wearable sync.",
            "まだデータがありません。権限と同期を確認してください。",
            "Nondum lectum: fortasse permissio deest aut armilla nondum synchronizata est."
        )
        add("数据来源优先级", "Preferred data source", "優先するデータソース", "Fons potior")
        add("模拟数据模式", "Demo mode", "デモモード", "Modus demonstrationis")
        add("还没有同步过", "Not synced yet", "未同期", "Nondum synchronizatum")
        add("同步状态", "Sync status", "同期状態", "Status synchronizationis")
        add("Health Connect", "Health Connect", "Health Connect")
        add("手动校准", "Manual calibration", "手動調整", "Calibratio manualis")
        add("演示数据", "Demo data", "デモデータ", "Data demonstrationis")

        add("默认天气类型", "Default body weather", "既定の体調", "Caelum default")
        add("跟随读数", "Use readings", "測定値に合わせる", "Mensuras sequi")
        add("校准标签自定义", "Edit calibration tags", "調整タグを編集", "Titulos calibrationis mutare")
        add("敏感度", "Sensitivity", "感度", "Sensibilitas")
        add("低", "Low", "低", "Humilis")
        add("中", "Medium", "中", "Medius")
        add("高", "High", "高", "Altus")
        add("重置校准", "Reset calibration", "調整をリセット", "Calibrationem restituere")
        add("重置", "Reset", "リセット", "Restituere")
        add("加一个自己的标签", "Add your own tag", "自分のタグを追加", "Titulum proprium addere")
        add("比如「闷」", "For example, heavy", "例：重い", "Exemplum: gravatum")
        add("加一个", "Add", "追加", "Addere")
        add("已经加上的标签：", "Your tags:", "追加したタグ：", "Tituli additi:")

        add("偏好时长", "Preferred duration", "時間", "Tempus praeferendum")
        add("1 分钟", "1 min", "1分", "1 minuta")
        add("3 分钟", "3 min", "3分", "3 minuta")
        add("5 分钟", "5 min", "5分", "5 minuta")
        add("不限", "Any", "制限なし", "Quaelibet")
        add("可用场景", "Available situations", "使える場面", "Loci usui")
        add("工位", "Desk", "デスク", "Mensa")
        add("地铁", "Transit", "移動中", "Metro")
        add("床上", "Bed", "ベッド", "Lectus")
        add("阳台", "Balcony", "ベランダ", "Maenianum")
        add("户外", "Outdoors", "屋外", "Foris")
        add("语音引导", "Voice guidance", "音声ガイド", "Dux vocalis")
        add("背景音默认", "Default background sound", "背景音", "Sonus default")
        add("雨声", "Rain", "雨", "Pluvia")
        add("风声", "Wind", "風", "Ventus")
        add("咖啡馆", "Café", "カフェ", "Cafea")
        add("纯静音", "Silent", "無音", "Silentium")
        add("完成反馈后自动调整", "Adjust after feedback", "感想に合わせて調整", "Post responsum adaptare")
        add("素食", "Vegetarian", "ベジタリアン", "Vegetarianus")
        add("不吃乳制品", "Dairy-free", "乳製品なし", "Sine lacte")
        add("不吃海鲜", "No seafood", "魚介なし", "Sine fructibus maris")
        add("低糖", "Lower sugar", "低糖質", "Parum sacchari")
        add("饮食偏好", "Diet preferences", "食事の好み", "Praeferentiae victus")
        add("过敏原提醒", "Allergen notes", "アレルゲン表示", "Monitio allergenorum")
        add("默认搜索范围", "Default search scope", "既定の検索範囲", "Ambitus quaerendi default")
        add("全部食物", "All foods", "すべての食品", "Omnia alimenta")
        add("只按营养素", "Nutrients only", "栄養素のみ", "Solum nutrimenta")
        add("只按功效", "Benefits only", "目的のみ", "Solum effectus")

        add("今天吃什么", "What to eat today", "今日の食事", "Quid hodie edam")
        add("直接搜", "Search", "検索", "Quaerere")
        add(
            "搜食物名，或者「补铁」「助眠」",
            "Search foods, nutrients or a need",
            "食品名、栄養素、目的で検索",
            "Quaere cibum, nutrimentum aut finem"
        )
        add("想吃什么口味", "Choose a style", "料理の種類", "Quis sapor")
        add("换一批", "Refresh", "入れ替える", "Renovare")
        add("按营养素找", "Browse by nutrient", "栄養素から探す", "Per nutrimentum quaerere")
        add("这些食物", "Foods", "食品", "Alimenta")
        add("饮食建议与小贴士", "Food tips", "食事のヒント", "Consilia victus")
        add("怎么吃", "How to eat it", "食べ方", "Quomodo edere")
        add("点开看详情", "View details", "詳しく見る", "Aperire pro detaliis")
        add("为什么值得吃", "Why consider it", "おすすめの理由", "Cur utile")
        add("怎么做", "Simple preparation", "簡単な作り方", "Praeparatio")
        add("含有哪些营养素", "Nutrients", "含まれる栄養素", "Nutrimenta")
        add("适合的天气", "Suitable body weather", "合う体調", "Caelum aptum")
        add("中餐", "Chinese", "中華", "Sinensis")
        add("西餐", "Western", "洋食", "Occidentalis")
        add("日料", "Japanese", "和食", "Iaponica")
        add("家常", "Everyday", "家庭料理", "Domesticus")

        add("蛋白质", "Protein", "たんぱく質", "Proteinum")
        add("膳食纤维", "Fiber", "食物繊維", "Fibra")
        add("益生菌", "Probiotics", "プロバイオティクス", "Probiotica")
        add("铁", "Iron", "鉄", "Ferrum")
        add("镁", "Magnesium", "マグネシウム", "Magnesium")
        add("维生素 C", "Vitamin C", "ビタミンC", "Vitaminum C")
        add("复合碳水", "Complex carbs", "複合炭水化物", "Carbohydrata complexa")
        add("钙", "Calcium", "カルシウム", "Calcium")
        add("锌", "Zinc", "亜鉛", "Zincum")
        add("维生素 A", "Vitamin A", "ビタミンA", "Vitaminum A")
        add("维生素 D", "Vitamin D", "ビタミンD", "Vitaminum D")

        add("身体回声", "Body echo", "体の記録", "Echo corporis")
        add("这一周的身体天气", "This week's body weather", "今週の体調", "Caelum corporis huius septimanae")
        add("颜色说明", "Color key", "色の説明", "Explicatio colorum")

        add("恢复天", "Recovery day", "回復の日", "Recuperatio")
        add("高能天", "High-energy day", "元気な日", "Alacritas")
        add("压力天", "Pressure day", "緊張の日", "Pressio")
        add("平衡天", "Balanced day", "安定した日", "Stabilitas")
        add("敏感天", "Sensitive day", "敏感な日", "Sensibilitas")
        add("恢复", "Recovery", "回復", "Recuperatio")
        add("高能", "Energy", "元気", "Alacritas")
        add("压力", "Pressure", "緊張", "Pressio")
        add("平衡", "Balance", "安定", "Stabilitas")
        add("敏感", "Sensitive", "敏感", "Sensibilitas")
        add(
            "云在慢慢散，不用急着赶路",
            "The clouds are clearing. There is no need to rush.",
            "雲はゆっくり晴れていく。急がなくていい。",
            "Nubes lente discedunt. Noli festinare."
        )
        add(
            "阳光正好，可以往外走一点",
            "The light is good. Step outside if you like.",
            "日差しがちょうどいい。少し外へ出てもいい。",
            "Lux bona est. Paulum foras ire potes."
        )
        add(
            "风有点大，先找个避风的地方",
            "The wind is strong. Find a sheltered place first.",
            "風が強い。まずは落ち着ける場所へ。",
            "Ventus fortis est. Primum locum tutum quaere."
        )
        add("今天的身体，是稳的", "Your body feels steady today.", "今日は体が安定している。", "Corpus hodie stabile est.")
        add(
            "身体在说话，先听它说完",
            "Your body is speaking. Let it finish.",
            "体の声を、最後まで聞いてみる。",
            "Corpus loquitur. Audi usque ad finem."
        )

        add("恢复云量", "Recovery clouds", "回復の雲", "Nubes")
        add("压力风速", "Pressure wind", "緊張の風", "Ventus")
        add("能量湿度", "Energy humidity", "元気の湿度", "Umor")
        add("睡眠云层", "Sleep clouds", "睡眠の雲", "Somnus")
        // 窄格里的状态词：中文「暂时没读到」五个字，日语超过五个字就会被省略号截断，
        // 所以用最短的体言止め，不用句子。
        add("暂时没读到", "Not available yet", "未計測", "Nondum lectum")
        add("晴", "Clear", "晴れ", "Serenum")
        add("少云", "Few clouds", "少し曇り", "Paucae nubes")
        add("多云", "Cloudy", "曇り", "Nubilum")
        add("阴", "Overcast", "くもり", "Obductum")
        add("无风", "Calm", "無風", "Tranquillum")
        add("微风", "Breeze", "そよ風", "Aura")
        add("有风", "Windy", "風あり", "Ventosum")
        add("风大", "Strong wind", "強い風", "Ventus fortis")
        add("偏湿", "Humid", "やや湿潤", "Humidum")
        add("适中", "Moderate", "適度", "Medium")
        add("偏干", "Dry", "やや乾燥", "Siccum")
        add("很干", "Very dry", "乾燥", "Valde siccum")
        add("薄雾", "Light mist", "薄い霧", "Nebula tenuis")
        add("有雾", "Misty", "霧", "Nebulosum")
        add("厚雾", "Thick mist", "濃い霧", "Nebula densa")

        add("早", "AM", "朝", "Mane")
        add("午", "Noon", "昼", "Meridies")
        add("晚", "PM", "夕", "Vesper")
        add("夜", "Night", "夜", "Nox")
        add("早上", "Morning", "朝", "Mane")
        add("午后", "Afternoon", "昼", "Post meridiem")
        add("睡前", "Before bed", "寝る前", "Ante somnum")
        add("夜里", "Night", "夜", "Nocte")
        add("处方", "Plan", "プラン", "Consilium")
        add("建议时长", "Suggested time", "目安時間", "Tempus suggestum")
        add("适用时段", "Time of day", "時間帯", "Hora diei")
        add("适用场景", "Situation", "場所", "Locus")
        add("还没设置", "Not set", "未設定", "Nondum statutum")
        add("拖到时段", "Drag to a time", "時間帯へ移動", "Ad horam trahe")
        add("换一张处方", "Another plan", "別のプラン", "Aliud consilium")
        add("顺便，一个环境建议", "A small environment tip", "環境のヒント", "Praeterea, consilium loci")
        add("还剩", "Remaining", "残り", "Restat")
        add("已暂停", "Paused", "一時停止", "Pausatum")
        add("暂停", "Pause", "一時停止", "Pausa")
        add("继续", "Continue", "続ける", "Perge")
        add("提前结束", "Finish", "終了", "Finire")
        add("身体收到。", "Your body was heard.", "体の声を受け取りました。", "Corpus audivit.")
        add("身体变轻了", "I feel lighter", "軽くなった", "Levior")
        add("没感觉", "No change", "変わらない", "Nihil mutatum")
        add("更累了", "More tired", "疲れた", "Lassior")
        add("想再来一次", "Again", "もう一度", "Iterum")
        add("早上好", "Good morning", "おはようございます", "Bonum mane")
        add("下午好", "Good afternoon", "こんにちは", "Bona dies")
        add("晚上好", "Good evening", "こんばんは", "Bona vespera")
        add("正在读…", "Reading…", "読み込み中…", "Lectio…")
        add("重新扫描", "Scan again", "再スキャン", "Iterum inspicere")
        add("今天的身体结论", "Today's body summary", "今日の体調まとめ", "Summarium corporis hodie")
        add("今天可以做什么", "A small action for today", "今日できること", "Actio hodierna")
        add("今天的天气诗", "Today's weather poem", "今日の体調の詩", "Carmen caeli hodiernum")
        add("为什么这样播报？", "Why this summary?", "このまとめの理由", "Cur hoc summarium?")
        add("为什么这样播报", "Why this summary", "このまとめの理由", "Cur hoc summarium")
        add(
            "只讲依据，不给数字。",
            "Qualitative reasons only, with no scores.",
            "根拠だけを示し、数値は出しません。",
            "Rationes tantum, numeri nulli."
        )
        add("我现在的感觉是——", "Right now I feel…", "今の感覚は…", "Nunc sentio…")
        add("清空校准", "Clear calibration", "調整をクリア", "Calibrationem purgare")
        add("只听身体播报", "Listen to the summary", "体調まとめを聞く", "Summarium audire")
        add("正在为你生成今天的处方…", "Preparing today's plan…", "今日のプランを準備中…", "Consilium hodiernum paratur…")
        add(
            "不是评分，而是把几种身体信号翻译成一句容易理解的话。",
            "Not a score: just a plain-language summary of several body signals.",
            "点数ではなく、いくつかの体の信号を分かりやすくまとめたものです。",
            "Non est punctum, sed signa corporis in unam sententiam facilem redacta."
        )
        add(
            "如有特殊健康状况，请咨询医生。这里只讲食物本身，不做诊断。",
            "For specific health concerns, consult a clinician. This app does not diagnose.",
            "持病などがある場合は医療専門家に相談してください。ここでは診断しません。",
            "Si valetudo peculiaris est, medicum consule. Hic de cibo tantum agitur, non de diagnosi."
        )

        add("数据边界", "Data boundaries", "データの範囲", "Fines datorum")
        add("读什么、存哪、去哪。", "What is read, stored and shared.", "読むもの、保存先、行き先。", "Quid legatur, ubi maneat, quo eat.")
        add("开发者", "Developers", "開発者", "Programmator")
        add("开发使用的 AI", "AI used in development", "開発に使用した AI", "AI in evolutione adhibita")
        add("版本号", "Version", "バージョン", "Versio")
        add("开源许可", "Open-source licenses", "オープンソースライセンス", "Licentiae fontis aperti")
        add("反馈与建议", "Feedback", "フィードバック", "Responsa")

        // ---- 隐私说明页（独立 Activity，之前整页漏翻）----
        add(
            "这份说明只讲事实。如果哪一条你不认可，直接撤销授权就好，应用不会因此不可用。",
            "This page only states facts. If you disagree with any point, simply revoke access; the app still works.",
            "この説明は事実だけを書いています。同意できない項目があれば、権限を取り消すだけで構いません。アプリが使えなくなることはありません。",
            "Haec pagina sola facta narrat. Si quid non probas, permissionem revoca: applicatio usui manet."
        )
        add("数据从哪里来", "Where the data comes from", "データの出どころ", "Unde data veniunt")
        add(
            "只从系统的健康服务（Health Connect）读取。你的手环先把数据同步到小米运动健康或华为运动健康，再由它们同步进 Health Connect，BodyCast 只做最后一步的读取。应用不直接连蓝牙，也不连手环。",
            "Everything is read from the system health service (Health Connect). Your band syncs to Mi Fitness or Huawei Health first, and those sync into Health Connect; BodyCast only does the final read. The app never connects to the band directly over Bluetooth.",
            "システムの健康サービス（Health Connect）からだけ読み取ります。ウェアラブルはまず Mi Fitness や Huawei Health に同期され、それらが Health Connect に書き込み、BodyCast は最後の読み取りだけを行います。アプリが Bluetooth で直接つながることも、ウェアラブルに接続することもありません。"
        )
        add("读了哪些数据", "What is read", "読み取るデータ", "Quae data legantur")
        add(
            "步数、睡眠、静息心率、心率变异性、血氧、运动记录，共六项。默认全部**只读**，应用不会修改系统里的健康记录。例外只有一个：你在「数据与同步」里打开「把读数写入系统健康服务」开关后，应用会把本机已同步的聚合读数写进 Health Connect（来源标注为本应用）；开关默认关闭，睡眠与运动只在有真实起止时间时写入。",
            "Steps, sleep, resting heart rate, heart rate variability, blood oxygen and exercise: six types in total. All read-only by default — the app never changes health records in the system. One exception: if you turn on “Write readings to the system health service” under Settings → Data & sync, the app writes the already-synced aggregate readings into Health Connect (labelled as coming from this app). The switch is off by default, and sleep / exercise are only written when real start and end times are known.",
            "歩数、睡眠、安静時心拍数、心拍変動、血中酸素、ワークアウトの六項目です。既定ではすべて**読み取り専用**で、システムの健康記録を変更しません。例外はひとつだけ：「設定 → データと同期」で「測定値をシステムの健康サービスに書き込む」をオンにすると、端末に同期済みの集計値を Health Connect に書き込みます（出どころは本アプリ）。スイッチは既定ではオフで、睡眠とワークアウトは実際の開始・終了時刻が分かるときだけ書き込みます。"
        )
        add("数据存在哪", "Where the data is stored", "データの保存先", "Ubi data maneant")
        add(
            "只存在这台手机上。应用默认不联网、没有服务器、没有账号，平时也没有任何上传行为。本机保存的只是「今天是什么天气」和「你手动勾选的感觉标签」，不保存原始心率、睡眠、血氧数值。",
            "Only on this phone. By default the app has no network access, no server, no account and never uploads anything. All that is kept locally is today's body weather and the feeling tags you ticked by hand; raw heart rate, sleep and blood oxygen values are not stored.",
            "この端末の中だけです。既定ではアプリは通信せず、サーバーもアカウントもなく、アップロードも行いません。端末に保存するのは「今日の体調」と「手動で選んだ感覚タグ」だけで、心拍数・睡眠・血中酸素の生の数値は保存しません。"
        )
        add(
            "可选的一条联网通道：小米账号（云同步）",
            "One optional online channel: Mi account (cloud sync)",
            "任意で使える通信経路：Mi アカウント（クラウド同期）"
        )
        add(
            "默认关闭。只有你自己在「设置 → 数据与同步 → 小米账号（云同步）」里主动连接之后，应用才会访问小米的服务器，把你自己账号里的运动健康数据取回来。· 连了什么域名：登录页是 account.xiaomi.com（小米官方账号页）；数据请求发给 hlth.io.mi.com（中国大陆）或 <区域>.hlth.io.mi.com（其它区域）；· 传了什么：只有查询条件——要哪一类读数、哪个时间段。应用不向小米写任何数据，也不上传本机的任何内容；· 密码呢：密码、验证码都只输在小米自己的登录页上。BodyCast 看不到、不保存、不转发。",
            "Off by default. Only after you connect it yourself under Settings → Data and sync → Mi account (cloud sync) does the app talk to Xiaomi's servers, to fetch the fitness data in your own account. · Domains: the sign-in page is account.xiaomi.com (Xiaomi's official account page); data requests go to hlth.io.mi.com (mainland China) or <region>.hlth.io.mi.com (other regions). · What is sent: query conditions only — which reading type and which time range. The app writes nothing to Xiaomi and uploads nothing from this phone. · What about the password: the password and any verification code are typed only on Xiaomi's own sign-in page. BodyCast cannot see, store or forward them.",
            "既定ではオフです。あなた自身が「設定 → データと同期 → Mi アカウント（クラウド同期）」で接続したときだけ、アプリは Xiaomi のサーバーにアクセスし、ご自身のアカウントにある運動健康データを取得します。· 接続先：ログインページは account.xiaomi.com（Xiaomi 公式アカウントページ）、データ要求は hlth.io.mi.com（中国本土）または <地域>.hlth.io.mi.com（その他の地域）です。· 送るもの：照会条件だけ（どの種類の測定値か、どの期間か）。アプリは Xiaomi に何も書き込まず、端末の内容も送りません。· パスワードについて：パスワードと確認コードは Xiaomi 自身のログインページにのみ入力します。BodyCast は見ることも、保存することも、転送することもできません。"
        )
        add(
            "这条通道在本机存了什么",
            "What this channel stores on the phone",
            "この経路が端末に保存するもの"
        )
        add(
            "只存三样东西：① 小米回给应用的访问令牌（serviceToken / passToken 等）；② 六类读数的聚合值；③ 上次同步时间。令牌用 Android Keystore 生成的 AES 密钥加密之后才落盘，明文令牌不落盘，应用日志与界面都不显示令牌内容。这条通道的请求只发生在你点「连接」与「立即同步」时，后台不会自己联网。",
            "Three things only: (1) the access tokens Xiaomi hands back (serviceToken / passToken and similar); (2) aggregate values for the six reading types; (3) the last sync time. Tokens are encrypted with an AES key generated by the Android Keystore before they are written to disk; the plaintext token is never stored, and neither the logs nor the interface show it. Requests only happen when you tap Connect or Sync now — the app never goes online on its own in the background.",
            "保存するのは三つだけです。① Xiaomi が返すアクセストークン（serviceToken / passToken など）。② 六種類の測定値の集計値。③ 前回の同期時刻。トークンは Android Keystore が生成した AES 鍵で暗号化してから保存し、平文のトークンは保存しません。ログや画面にも表示しません。通信が起きるのは「接続」と「今すぐ同期」を押したときだけで、バックグラウンドで勝手に通信することはありません。"
        )
        add(
            "怎么断开这条通道",
            "How to disconnect this channel",
            "この経路の切断方法"
        )
        add(
            "在同一个页面上点「断开并删除令牌」：密钥与令牌一起删掉，旧的密文从此解不开，云端取回来的读数也一并清除。断开之后应用回到默认的「不联网」状态，其它功能不受影响。",
            "Tap Disconnect and delete token on the same page: the key and the token are deleted together, the old ciphertext can never be decrypted again, and the readings fetched from the cloud are cleared as well. The app then returns to its default offline state; nothing else is affected.",
            "同じページで「切断してトークンを削除」を押します。鍵とトークンが一緒に削除され、古い暗号文は二度と復号できません。クラウドから取得した測定値も消去されます。切断後、アプリは既定の非通信状態に戻り、他の機能には影響しません。"
        )
        add("数据去哪了", "Where the data goes", "データの行き先", "Quo data eant")
        add(
            "哪也不去。没有第三方 SDK，没有广告，没有数据分析。你在设置里点「删除所有数据」，本机记录会立即清空。",
            "Nowhere. There is no third-party SDK, no advertising and no analytics. Tap Delete all data in Settings and the local records are cleared immediately.",
            "どこにも行きません。第三者 SDK も広告も解析もありません。設定で「すべてのデータを削除」を押すと、端末の記録はすぐに消えます。"
        )
        add("你可以随时收回", "You can take it back at any time", "いつでも取り戻せます", "Semper revocare potes")
        add(
            "在设置页点「撤销授权」，或者在系统的 Health Connect 里取消勾选，应用就再也读不到数据了。收回之后应用仍然可用，只是天气会显示为「暂时没读到」。小米账号那条通道则用「断开并删除令牌」收回。",
            "Revoke access in Settings, or untick the types inside Health Connect, and the app can no longer read anything. The app still works afterwards; the weather simply shows as not available yet. The Mi account channel is taken back with “Disconnect and delete token”.",
            "設定で「権限を取り消す」を押すか、システムの Health Connect でチェックを外すと、アプリはデータを読み取れなくなります。取り消した後もアプリは使えます。天気が「まだ読み取れていません」と表示されるだけです。Mi アカウントの経路は「切断してトークンを削除」で取り消します。"
        )
        // ---- 隐私页页脚 ----
        add(
            "如有特殊健康状况，请咨询医生。BodyCast 只播报状态，不做诊断。",
            "For specific health concerns, consult a clinician. BodyCast reports your state; it does not diagnose.",
            "持病などがある場合は医療専門家に相談してください。BodyCast は状態を伝えるだけで、診断はしません。",
            "Si valetudo peculiaris est, medicum consule. BodyCast statum refert, non diagnosim facit."
        )
    }

    // ------------------------------------------------------------------
    // 手环厂商接入：App 名、跳转引导、数据来源（21.1.0 新增）
    // ------------------------------------------------------------------
    init {
        add("小米运动健康", "Mi Fitness", "Mi Fitness")
        add("荣耀运动健康", "Honor Health", "Honor Health")
        add("华为运动健康", "Huawei Health", "Huawei Health")
        add("Zepp Life", "Zepp Life", "Zepp Life")
        add("三星健康", "Samsung Health", "Samsung Health")
        add("系统健康服务", "System health service", "システムの健康サービス", "Health Connect")
        add("数据来源", "Data origin", "データの出どころ", "Origo datorum")
        add("下一步：", "Next:", "次の手順：", "Deinde:")
        add(
            "本次读到的记录来自下面这些 App：",
            "The records read this time come from these apps:",
            "今回読み取った記録は次のアプリから来ています：",
            "Notae hac vice lectae ex his applicationibus veniunt:"
        )
        add(
            "这次什么都没读到。授权之后点上面的「手动同步」，这里会显示出数据是哪个 App 写进系统健康服务的。",
            "Nothing was read this time. After granting access, tap Sync now above and this card shows which app wrote the data into the system health service.",
            "今回は何も読み取れませんでした。権限を許可したあと、上の「今すぐ同期」を押すと、どのアプリがシステムの健康サービスに書き込んだかがここに表示されます。",
            "Hac vice nihil lectum est. Post permissionem tange \"Synchronizare\" supra: hic apparebit quae applicatio data in Health Connect scripserit."
        )
        add(
            "如果这里显示的不是你手环对应的那个 App，说明系统里还有别的应用在写同类数据——以你手动校准的结论为准。",
            "If the app listed here is not the one paired with your band, another app is writing the same data types — trust your manual calibration instead.",
            "ここに表示されたアプリが手元のウェアラブルに対応するものではない場合、同じ種類のデータを書いている別のアプリがシステムにあります。手動調整で出した結論を優先してください。",
            "Si applicatio hic monstrata non est ea quae armillae respondet, alia applicatio eadem data scribit — conclusio calibrationis manualis praecedit."
        )
        add(
            "这台机器上没找到手环厂商的运动健康 App。装一个，或者直接用「手动校准」——手动校准的优先级永远高于设备读数。",
            "No wearable maker's health app was found on this phone. Install one, or just use manual calibration — manual calibration always outranks device readings.",
            "この端末にはウェアラブルメーカーの運動健康アプリが見つかりませんでした。インストールするか、「手動調整」をそのままお使いください。手動調整は常にデバイスの測定値より優先されます。",
            "In hoc instrumento nulla applicatio fabricatoris inventa est. Installe unam, aut calibratione manuali utere — ea semper mensuras praecedit."
        )
        add(
            "最后一步：回到这里点「去授权」，在弹出的系统框里勾上要允许的类型。",
            "Last step: tap Grant access here and tick the types you want to allow in the system dialog.",
            "最後の手順：ここに戻って「権限を設定」を押し、表示されたシステムの画面で許可する項目にチェックを入れてください。",
            "Ultimus gradus: huc redi, tange \"Permittere\" et in systematis fenestra genera eligenda signa."
        )
        add(
            "打开厂商的运动健康 App，登录手环账号，确认今天的数据已经同步进来",
            "Open the wearable maker's health app, sign in, and confirm today's data has synced",
            "メーカーの運動健康アプリを開き、ウェアラブルのアカウントにログインして、今日のデータが同期されていることを確認します"
        )
        add(
            "在它的设置里找「数据共享 / 第三方接入 / Health Connect」——不同版本叫法不同",
            "In its settings look for data sharing / third-party access / Health Connect — the wording differs between versions",
            "その設定で「データ共有 / 第三者アクセス / Health Connect」を探します。バージョンによって名前が違います"
        )
        add(
            "把允许共享的类型打开：步数、睡眠、心率、血氧",
            "Enable the types you want to share: steps, sleep, heart rate, blood oxygen",
            "共有を許可する項目をオンにします：歩数、睡眠、心拍数、血中酸素"
        )
        add(
            "回到本页点「手动同步」，再看下面的「数据来源」，确认写进来的是哪个 App",
            "Come back and tap Sync now, then check Data origin below to see which app wrote the data",
            "このページに戻って「今すぐ同期」を押し、下の「データの出どころ」でどのアプリが書き込んだかを確認します"
        )
        add(
            "先登录厂商的运动健康 App，确认手环的数据已经同步进去",
            "Open the wearable maker's health app and sign in, then make sure the band's data has synced",
            "まずメーカーの運動健康アプリにログインし、ウェアラブルのデータが同期されていることを確認します"
        )
        add(
            "华为运动健康默认把数据存在华为自己的健康服务里，不写入系统的 Health Connect",
            "Huawei Health keeps data inside Huawei's own health service by default; it does not write to Health Connect",
            "Huawei Health は既定でデータを Huawei 独自の健康サービスに保存し、システムの Health Connect には書き込みません"
        )
        add(
            "如果手机是华为 / 荣耀：在系统「健康」里打开数据共享，支持情况随机型不同",
            "On a Huawei or Honor phone: turn on data sharing in the system Health app (support varies by model)",
            "Huawei / Honor のスマートフォンでは、システムの「ヘルスケア」でデータ共有をオンにします（対応状況は機種によって異なります）"
        )
        add(
            "其他品牌手机：可以用第三方桥接应用（例如 Health Sync）把华为的数据转写到 Health Connect",
            "On other phones: a third-party bridge app (for example Health Sync) can copy Huawei's data into Health Connect",
            "他メーカーのスマートフォンでは、サードパーティのブリッジアプリ（例：Health Sync）で Huawei のデータを Health Connect に転送できます"
        )
        add(
            "实在走不通也没关系：回到本页用手动校准，天气照样能给",
            "If none of that works: manual calibration on this page still gives you today's weather",
            "どうしてもうまくいかないときも大丈夫です。このページに戻って手動調整を使えば、体調はこれまでどおり表示されます"
        )
    }

    // 22.0.0 补漏：引导 / 通知 / 备份的英文
    init {
        add(
            "到点时轻轻提醒一次，不催促、不带数字。",
            "A gentle nudge at the right time - no pressure, no numbers.",
            "時間になったらそっと一度だけ知らせます。急かさず、数字も出しません。",
            "Semel leniter monet, sine festinatione, sine numeris."
        )
        add("它不评分，也不排名", "It does not score or rank", "点数も順位もつけません", "Non punctat, non ordinat")
        add(
            "这里不会有卡路里、体重，也不会有「今天没达标」。身体状态只有状态，没有好坏——恢复天和高能天一样正当。指标只给词，不给数字。",
            "No calories, no weight, no \"you missed the target\". A body state is just a state - a recovery day is as valid as a high-energy day. Metrics come as words, never numbers.",
            "ここにはカロリーも体重も「今日は未達成」もありません。体の状態は状態そのもので、良し悪しはありません。回復の日も元気な日と同じように正当です。指標は言葉だけで、数字は出しません。",
            "Nullae caloríae, nullum pondus, nulla \"hodie non attigisti\". Status corporis status est: dies recuperationis tam iustus quam dies alacritatis. Mensurae verbis dantur, non numeris."
        )
        add("数据从哪来", "Where the data comes from", "データの出どころ", "Unde data veniunt")
        add(
            "手环 → 厂商的运动健康 App → 系统健康服务 → 这里。应用不直连手环、不用蓝牙。不想连也没关系，下面三种方式随时可以换。",
            "Band → the maker’s health app → the system health service → here. The app never talks to the band directly. Not connecting is fine too - switch anytime.",
            "ウェアラブル → メーカーの運動健康アプリ → システムの健康サービス → ここ。アプリがウェアラブルに直接つながることも、Bluetooth を使うこともありません。つながなくても大丈夫で、下の三つの方法はいつでも切り替えられます。",
            "Armilla → applicatio fabricatoris → Health Connect → hic. Applicatio armillam directe non attingit, Bluetooth non utitur."
        )
        add(
            "连手环（去设置里授权）",
            "Connect a band (grant access in Settings)",
            "ウェアラブルをつなぐ（設定で権限を許可）",
            "Armillam coniungere (permitte in Optionibus)"
        )
        add("手动校准就好", "Manual calibration is enough", "手動調整で十分です", "Calibratio manualis satis est")
        add(
            "演示数据会在首页顶部挂一条提示，说明它不是你的真实状态。",
            "Demo data shows a banner on the home page so it is never mistaken for your real state.",
            "デモデータを使うと、ホームの上部に「実際の体調ではありません」という案内が出ます。",
            "Data demonstrationis in summa pagina Domus notantur ne pro statu tuo sumantur."
        )
        add(
            "今天是什么天气，你说了算",
            "What today is, you decide",
            "今日がどんな体調かは、あなたが決めます",
            "Quale caelum hodie sit, tu decidis"
        )
        add(
            "首页有九个校准标签（累、紧、胀、困、烦、轻、空、饿、说不清）。点一下就能改写今天的结论——手动校准的优先级永远高于设备读数。",
            "Nine tags on the home page (tired, tense, bloated, sleepy, annoyed, light, empty, hungry, cannot say). One tap rewrites today’s conclusion - manual calibration always outranks device readings.",
            "ホームには九つの調整タグ（疲れた、張る、張りつめる、眠い、いらいら、軽い、空っぽ、空腹、うまく言えない）があります。一つ押すだけで今日の結論が書き換わります。手動調整は常にデバイスの測定値より優先されます。"
        )
        add("开始使用", "Start", "はじめる", "Incipere")
        add("跳过", "Skip", "スキップ", "Transilire")
        add("已允许", "Allowed", "許可済み", "Permissum")
        add("未申请", "Not requested", "未申請", "Non petitum")
        add(
            "已经允许。提醒会在到点时发出来。",
            "Allowed. Reminders will arrive when they are due.",
            "許可済みです。リマインダーは時間になると届きます。",
            "Permissum est. Admonitiones tempore venient."
        )
        add(
            "点一下申请；系统里关掉过的话，会带你去系统设置。",
            "Tap to request. If you turned it off in the system, this opens system settings.",
            "押すと許可を求めます。システム側でオフにしていた場合は、システム設定を開きます。",
            "Tange ut petas; si in systemate clausum est, ad optiones systematis duceris."
        )
        add(
            "文件没有写进去，请换个位置再试。",
            "The file was not written. Try another location.",
            "ファイルを書き込めませんでした。別の場所でもう一度お試しください。",
            "Fasciculus scriptus non est. Alio in loco tenta."
        )
        add(
            "文件格式不对，或文件已经损坏。",
            "The file is not a valid backup, or it is damaged.",
            "ファイルの形式が正しくないか、ファイルが壊れています。",
            "Forma fasciculi falsa est aut fasciculus corruptus."
        )
        add(
            "生成 .bodycast 文件，换手机时可以导入。",
            "Creates a .bodycast file you can import on a new phone.",
            ".bodycast ファイルを作成します。機種変更のときに読み込めます。",
            "Fasciculum .bodycast creat, quem in novo instrumento importare potes."
        )
        add(
            "先看将导入多少天，再确认写入本机。",
            "See how many days would be merged before anything is written.",
            "何日分が取り込まれるかを先に確認してから、端末への書き込みを確定します。",
            "Primum vide quot dies importentur, tum in hoc instrumento scribe."
        )
        add("导入", "Import", "読み込む", "Importare")
        add("先不要", "Not now", "あとで", "Non nunc")
    }

    // 22.0.0 新增功能文案
    init {
        add("身体天气提醒", "Body weather reminders", "体調リマインダー", "Admonitio caeli corporis")
        add(
            "到点时提醒你看一眼今天的身体天气，或者做一件几分钟的小事。",
            "A gentle reminder to look at today’s body weather or do one small thing.",
            "時間になったら、今日の体調をひと目見るか、数分でできる小さなことを一つ行うよう知らせます。",
            "Tempore monet ut caelum corporis hodie inspicias aut rem parvam paucis minutis facias."
        )
        add("今天的身体天气", "Today’s body weather", "今日の体調", "Caelum corporis hodie")
        add(
            "打开应用看一眼，它不评价，也不催你。",
            "Open the app for a look. It does not judge or rush you.",
            "アプリを開いてひと目見るだけで大丈夫です。評価も催促もしません。",
            "Aperi applicationem et aspice. Non iudicat, non urget."
        )
        add("还没有读到数据", "Nothing read yet", "まだデータを読み取れていません", "Nihil adhuc lectum")
        add(
            "连接手环，或者用手动校准——手动校准的优先级永远高于设备读数。",
            "Connect a wearable or calibrate by hand - manual calibration always outranks device readings.",
            "ウェアラブルをつなぐか、手動調整をお使いください。手動調整は常にデバイスの測定値より優先されます。",
            "Armillam coniunge aut calibratione manuali utere — ea semper mensuras praecedit."
        )
        add("有件几分钟的小事在等你", "A small thing is waiting", "数分でできる小さなことが待っています", "Parva res paucis minutis te expectat")
        add(
            "打开 BodyCast，从今天可以做什么里挑一件——随便挑一件做就行，不用每件都做。",
            "Open BodyCast and choose one small thing from today’s plan.",
            "BodyCast を開いて「今日できること」から一つ選んでください。どれか一つで十分で、全部やる必要はありません。",
            "Aperi BodyCast, unum ex Actione hodierna elige — unum satis est."
        )
        add("今天身体是什么天气？", "What is your body weather today?", "今日の体調はどうですか？", "Quale caelum corporis hodie?")
        add(
            "看一眼就好。它不评价，也不催你。",
            "Just take a look. It does not judge or rush you.",
            "ひと目見るだけで十分です。評価も催促もしません。",
            "Aspice tantum. Non iudicat, non urget."
        )
        add("重看新手引导", "Replay the welcome guide", "はじめての案内を見直す", "Ducem iterum monstrare")
        add(
            "第一次打开时的那三页，随时可以再看。",
            "Replay the three-page welcome guide anytime.",
            "初回に表示された三つのページは、いつでも見直せます。",
            "Primae tres paginae iterum inspici possunt."
        )
        add("备份失败", "Backup failed", "バックアップに失敗しました", "Exemplar defecit")
        add("从备份恢复", "Restore from backup", "バックアップから復元", "Ex exemplari restituere")
        add("导出本机备份", "Export local backup", "端末のバックアップを書き出す", "Exemplar locale exportare")
        add("导入本机备份？", "Restore this backup?", "端末のバックアップを読み込みますか？", "Hoc exemplar importare?")
        add("导入完成", "Restore complete", "読み込みが完了しました", "Importatio perfecta")
        add("这个备份读不了", "This backup cannot be read", "このバックアップは読み込めません", "Hoc exemplar legi non potest")
        add("数据来源", "Data origin", "データの出どころ", "Origo datorum")
        add(
            "本次读到的记录来自下面这些 App：",
            "These apps wrote the records read this time:",
            "今回読み取った記録は次のアプリから来ています：",
            "Notae hac vice lectae ex his applicationibus veniunt:"
        )
        add("先看演示数据", "View demo data first", "まずデモデータを見る", "Primum data demonstrationis")
        add("下一步：", "Next:", "次の手順：", "Deinde:")
        add(
            "最后一步：回到这里点「去授权」，在弹出的系统框里勾上要允许的类型。",
            "Last step: tap Grant access and choose the types in the system dialog.",
            "最後の手順：ここに戻って「権限を設定」を押し、表示されたシステムの画面で許可する項目にチェックを入れてください。",
            "Ultimus gradus: huc redi, tange \"Permittere\" et in systematis fenestra genera eligenda signa."
        )
        add("特大", "Extra large", "特大", "Maximus")
        add("这个月", "This month", "今月", "Hic mensis")
        add("这一周", "This week", "今週", "Haec septimana")
        add("这个月的身体天气", "This month’s body weather", "今月の体調", "Caelum corporis huius mensis")
        add(
            "按四周回放，只看方向，不做排名。",
            "A four-week replay of direction, never a ranking.",
            "四週間ごとに振り返ります。見るのは方向だけで、順位はつけません。",
            "Quattuor septimanae: directionem tantum monstrant, ordinem nullum."
        )

        // ---- 22.30.0 产品定位落地：首页结论 / 回声正向计数 / 关于定位语 ----
        add("BodyCast 是什么", "What is BodyCast", "BodyCastとは", "Quid est BodyCast")
        add(
            "把身体状态翻译成天气，告诉你今天该怎么用力。",
            "We translate how your body feels into weather, so you know how to spend your energy today.",
            "体調を天気に翻訳し、今日はどう力を配るかを伝えます。",
            "Statum corporis in caelum transfert ut scias quomodo hodie vires impendas."
        )
        add(
            "不打分、不排名、不制造焦虑；读不到就说读不到。从天气到营养元素再到食物——今天压力大，就去食谱页找含镁的食物。",
            "No scores, no rankings, no anxiety. If a signal can’t be read, we say so. From weather to nutrients to food — on a high-pressure day, look for magnesium-rich food in the Food tab.",
            "点数も順位も不安も作りません。読み取れないものは読み取れないと言います。天気から栄養素、そして食事へ——プレッシャーの大きい日は、食事タブでマグネシウムを含む食材を探しましょう。",
            "Sine punctis, sine ordinibus, sine anxietate; si signum legi non potest, dicimus. A caelo ad nutriens ad cibum — die pressurae magnum cibum cum magnesium in tabula cibi quaere."
        )
        add(
            "做完一件微行动就记一次。没有目标，漏做也不扣——只是让你看见。",
            "One count per finished micro-action. No goal, nothing deducted for a miss — just so you can see it.",
            "小さな行動を終えるたびに1回と数えます。目標はなく、忘れても減りません——見えるようにするだけです。",
            "Singula actio parva perfecta semel computatur. Meta nulla, nihil detrahitur — ut videas tantum."
        )

        // ---- 22.48.0：数据连接指导（授权指南）与「运动读数 → 今天先补什么」 ----
        add("连接你的运动数据", "Connect your activity data", "運動データを接続", "Coniunge data motus")
        add(
            "BodyCast 读到你自己的运动数据，才说得出今天是什么天气。点「开始指导」走三步：授权系统健康服务、连接小米账号、确认数据真的进来了。不连也能用——每天的读数要自己填。",
            "BodyCast reads your own activity data before it can say what today feels like. Tap “Start guide” for three steps: grant health-service access, connect your Mi account, and check that the data really arrived. You can use it without connecting — you just enter each day's readings yourself.",
            "BodyCast はあなた自身の運動データを読んでこそ、今日の体調を言葉にできます。「ガイドを始める」を押すと三つの手順に進みます：ヘルスサービスの許可、Mi アカウントの接続、データが本当に届いたかの確認。接続しなくても使えます——その場合は毎日の測定値を自分で入力します。",
            "BodyCast data motus tua legit, ut dicere possit qualis hodie sit. Tange “Ducatum incipe” pro tribus gradibus: permissio servitii sanitatis, coniunge rationem Mi, confirma data advenerunt. Etiam sine coniunctione uti potes — lectiones cotidie ipse inscribas."
        )
        add("不再提示", "Don't ask again", "今後表示しない", "Ne iterum roga")
        add("开始指导", "Start guide", "ガイドを始める", "Ducatum incipe")
        add("忽略", "Ignore", "無視", "Neglege")

        add("连接运动数据", "Connect activity data", "運動データを接続", "Coniunge data motus")
        add(
            "三步指导：授权健康服务、连接小米账号、看数据有没有真的进来。",
            "Three steps: grant health access, connect your Mi account, and see whether the data really arrived.",
            "三つの手順：ヘルスサービスの許可、Mi アカウントの接続、データが本当に届いたかの確認。",
            "Tres gradus: permissio sanitatis, ratio Mi, num data advenerunt."
        )
        add(
            "三步：授权健康服务、连接小米账号、确认数据真的进来了。",
            "Three steps: grant health access, connect your Mi account, confirm the data really arrived.",
            "三つの手順：ヘルスサービスの許可、Mi アカウントの接続、データが届いたかの確認。",
            "Tres gradus: permissio sanitatis, ratio Mi, confirmatio datorum."
        )
        add("现在走到哪一步了", "Where you are now", "どこまで進んだか", "Quo nunc es")
        add("② 小米账号", "② Mi account", "② Mi アカウント", "② Ratio Mi")
        add("③ 数据读到了没有", "③ Did the data arrive", "③ データを読めたか", "③ Num data lecta sunt")
        add("还没有数据", "No data yet", "データはまだありません", "Nulla data adhuc")
        add(
            "两条路是并行的，走通一条就够：系统健康服务（手环 → 厂商 App → 系统）或小米账号（直接从云端取）。两条都通的时候，谁的数据新用谁。",
            "The two routes run in parallel — one working route is enough: the system health service (band → vendor app → system) or your Mi account (read straight from the cloud). When both work, the fresher reading wins.",
            "二つの経路は並行しています。どちらか一つ通れば十分です：システムのヘルスサービス（バンド → メーカー App → システム）または Mi アカウント（クラウドから直接取得）。両方通っているときは、新しいほうの値を使います。",
            "Duae viae parallelae sunt; una sufficit: servitium sanitatis systematis (armilla → app fabricatoris → systema) vel ratio Mi (ex nube directe). Si ambae valent, recentior praevalet."
        )
        add(
            "一条都不通也能用这个应用：首页会请你手动填一次读数，填完照样出天气。只是每天都要自己填。",
            "The app still works with neither route: the home page asks you to enter the readings once by hand, and the weather comes out just the same. You just have to do it every day.",
            "どちらも通らなくても使えます。ホームで測定値を一度手入力すれば、同じように天気が出ます。ただし毎日入力が必要です。",
            "Etiam sine utraque via uti potes: pagina domus lectiones manu semel petit, et caelum idem prodit. Cotidie tamen scribendum est."
        )
        add("① 授权系统健康服务", "① Grant health-service access", "① ヘルスサービスを許可", "① Permissio servitii sanitatis")
        add(
            "正在检查这台手机的健康服务…",
            "Checking this phone's health service…",
            "この端末のヘルスサービスを確認しています…",
            "Servitium sanitatis huius instrumenti inspicitur…"
        )
        add(
            "已经授权。数据要等厂商 App 写进来才会出现——手环同步过一次之后再看这里。",
            "Access granted. The data appears only once the vendor app writes it in — come back here after your band has synced once.",
            "許可済みです。データはメーカー App が書き込んで初めて現れます——バンドを一度同期してから、またここを見てください。",
            "Permissum datum est. Data tum apparent cum app fabricatoris scribit — post synchroniam armillae huc redi."
        )
        add(
            "还没有授权。下面这个按钮会打开系统的授权页面，把六类数据都打开。",
            "Not granted yet. The button below opens the system permission page — turn on all six data types.",
            "まだ許可されていません。下のボタンでシステムの許可画面が開きます。六種類すべてをオンにしてください。",
            "Nondum permissum. Puga infra paginam permissionis systematis aperit — omnes sex typos accende."
        )
        add("再看一眼授权状态", "Check the permission again", "許可の状態をもう一度確認", "Statum permissionis iterum inspice")
        add(
            "在系统页面上一次把六类都打开：步数、睡眠、静息心率、心率变异性、血氧、运动记录。少开一项，就少一类数据。",
            "On the system page, turn on all six at once: steps, sleep, resting heart rate, heart rate variability, blood oxygen and exercise records. Miss one and that data type stays empty.",
            "システム画面で六種類を一度にオンにしてください：歩数、睡眠、安静時心拍、心拍変動、血中酸素、運動記録。一つでも切れていると、その種類のデータは入りません。",
            "In pagina systematis omnes sex accende: gradus, somnus, pulsus quietus, variabilitas pulsus, oxygenium sanguinis, actus exercitationis. Uno omisso, ille typus vacuus manet."
        )
        add(
            "系统只给「授权之后」产生的数据。想看更早的记录，要在那个页面上单独再开一档历史权限。",
            "The system only shares data created after you grant access. To include older records, turn on a history permission on that same page.",
            "システムが渡すのは許可後のデータだけです。それより前の記録も見るには、同じ画面で履歴の権限を別にオンにしてください。",
            "Systema data post permissionem creata solum dat. Pro veteribus, permissionem historiae in eadem pagina accende."
        )
        add(
            "授权完回到这一页会自动重新检查一遍，不用手动刷新。",
            "Come back to this page after granting and it rechecks by itself — no manual refresh.",
            "許可後にこのページへ戻れば、自動で再確認します。手動更新は不要です。",
            "Post permissionem huc redi: pagina se ipsa iterum inspicit."
        )
        add(
            "如果授权了还是没数据，多半是厂商 App 没有把数据写进系统——设置 → 数据与同步 → 授权向导里有手环配对的完整步骤。",
            "If access is granted but there is still no data, the vendor app is probably not writing into the system — Settings → Data and sync → Access walkthrough has the full band-pairing steps.",
            "許可してもデータが入らないときは、メーカー App がシステムへ書き込んでいない可能性が高いです——設定 → データと同期 → 許可ガイドに、バンドのペアリング手順がすべて載っています。",
            "Si permissum datum est neque data adsunt, app fabricatoris in systema non scribit — in Optionibus → Data et synchronia → ductor permissionis gradus armillae habet."
        )
        add("② 连接小米账号（云同步）", "② Connect your Mi account (cloud sync)", "② Mi アカウントを接続（クラウド同期）", "② Coniunge rationem Mi (synchronia nubis)")
        add("③ 预览已同步数据", "③ Preview the synced data", "③ 同期したデータを見る", "③ Praevide data synchronizata")
        add(
            "这张表就是应用当前手里的全部读数：「小米云 / 手动 / 导入」各占一列，取到就填值，没取到写「没读到」，不猜也不补 0。",
            "This table is every reading the app holds right now: Mi cloud, manual and import each get a column. A value that was read is filled in; one that was not says “not available yet” — nothing is guessed, nothing is padded with 0.",
            "この表が、アプリがいま持っている測定値のすべてです。「Mi クラウド / 手動 / 取り込み」がそれぞれ一列。取れた値はそのまま、取れなかった項目は「まだ読み取れません」と表示し、推測も 0 での穴埋めもしません。",
            "Haec tabula omnes lectiones ostendit quas app nunc tenet: nubes Mi, manus, importatum singulae columnam habent. Quod lectum est scribitur; quod non, “nondum lectum” — nihil conicitur, nihil 0 completur."
        )
    }

    /**
     * 槽位翻译失败的记账（22.27.0「半截翻译保护」用，见 [patterns] 里的 format）。
     *
     * `text()` 把没登记、没模板的中文源串原样放出去时置位；模板层据此拒绝
     * 「外壳翻了、槽位还漏中文」的产出。只在 [patterns] 内部读写。
     */
    private var untranslatedSlot = false

    fun text(source: String, language: AppLanguage = AppLanguage.current()): String {
        if (language == AppLanguage.SIMPLIFIED_CHINESE || source.isBlank()) return source
        // 语言自称永远用**它自己的写法**（22.43.0）：简体中文就写「简体中文」、
        // 繁體中文就写「繁體中文」、Español 就写 Español……不随界面语言翻译、
        // 也不做简繁转换——否则一个西班牙人在中文界面里根本认不出哪行是西班牙语。
        if (source in AppLanguage.SELF_NAMES) return source
        // 繁体中文走转换层（22.34.0）：台湾用语词表 + 字级简转繁。
        // 不查词条、不做模板——包括没登记的中文串也一并转，不会漏成简体。
        if (language == AppLanguage.TRADITIONAL_CHINESE) return ZhHant.convert(source)
        entries[source]?.let { return select(it, source, language) }
        HomeTextCatalog.translate(source, language)?.let { return it }
        val patterned = patterns(source, language)
        if (patterned != source) return patterned
        // 什么都没匹配上：中文源串会原样漏出去。记一笔账——
        // 缺译要么整句回退、要么回退英语，不能变成「翻一半混排」。
        if (HAN.containsMatchIn(source) && !looksAlreadyLocalized(source, language)) {
            untranslatedSlot = true
        }
        return fallback(source, language)
    }


    /** 模板拼装：模板走 [text] 翻译，再把 {1}{2}… 替换成已翻好的槽位（22.34.0，Engine 组合句用）。 */
    fun compose(template: String, vararg slots: String, language: AppLanguage = AppLanguage.current()): String {
        var out = text(template, language)
        slots.forEachIndexed { i, v -> out = out.replace("{" + (i + 1) + "}", v) }
        return out
    }

    fun searchable(source: String, language: AppLanguage = AppLanguage.current()): String =
        listOf(source, text(source, language), text(source, AppLanguage.ENGLISH)).distinct().joinToString(" ")

    /**
     * 日期串。
     *
     * 日/英/拉丁走 ICU；**拉丁文要防一手**：CLDR 有 `la`，但设备侧 ICU 不一定打包了
     * （实测 API 37 模拟器的 `icudt78l.dat` 就没有 `la`，`ofLocalizedDate` 会静默回退成
     * 英文 `Friday, October 9, 2026`）。判据是**和英文格式化结果逐字比一次**：
     * 拉丁文的星期名（`Lunae`）与月份名和英文一定不同，两者相等就说明 ICU 没有 `la` 数据，
     * 换手写的拉丁日期格式兜底。
     *
     * 注意不能按「月份名里有没有 Ianuari/Octob 这些词干」判——这些词干**英文月份里也有**
     * （October 含 Octob），那样会漏判。
     */
    fun formatDate(date: LocalDate, language: AppLanguage = AppLanguage.current()): String {
        if (language == AppLanguage.LATIN) {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            val fromIcu = date.format(formatter.withLocale(AppLanguage.LATIN.locale))
            val english = date.format(formatter.withLocale(Locale.ENGLISH))
            if (fromIcu != english) return fromIcu
            return LATIN_WEEKDAYS[date.dayOfWeek.value - 1] + ", " +
                date.dayOfMonth + " " + LATIN_MONTHS[date.monthValue - 1] + " " + date.year
        }
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(language.locale))
    }

    /** 手写兜底用的月份名（属格，日期写法 `9 Octobris 2026`），固定短语、不做变格计算。 */
    private val LATIN_MONTHS = listOf(
        "Ianuarii", "Februarii", "Martii", "Aprilis", "Maii", "Iunii",
        "Iulii", "Augusti", "Septembris", "Octobris", "Novembris", "Decembris"
    )

    /** `Echo.weekday()` 的中文单字 → 拉丁星期名（属格，`Lunae` = die Lunae）。 */
    private val LATIN_WEEKDAYS = listOf("Lunae", "Martis", "Mercurii", "Iovis", "Veneris", "Saturni", "Solis")

    /**
     * 取某个语言的译文。
     *
     * `gapKey` 只用于审计：槽位为空时回退英语，并把「这条还没翻」记进 [TranslationAudit]，
     * 否则界面上会静默混进英语、没人发现。
     *
     * **空槽兜底到 en 这条规则不能删**：52 个模板里只要有一个 `la` 槽是空的，
     * `format()` 就会拿到空格式串、把整句渲染成空字符串。
     */
    private fun select(entry: Entry, gapKey: String, language: AppLanguage): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> error("Simplified Chinese uses the source text")
        AppLanguage.TRADITIONAL_CHINESE -> error("Traditional Chinese uses converted source text")
        AppLanguage.ENGLISH -> entry.en
        // 西/德语走 EN→ES/DE 映射表（22.42.0）：表里没有就回退英语并记缺口，
        // 保证界面永远不漏中文（英文串本身不含汉字）。
        AppLanguage.SPANISH -> EsDe.es(entry.en) ?: run {
            TranslationAudit.recordSheetGap(gapKey, language)
            entry.en
        }
        AppLanguage.GERMAN -> EsDe.de(entry.en) ?: run {
            TranslationAudit.recordSheetGap(gapKey, language)
            entry.en
        }
        AppLanguage.JAPANESE -> entry.ja.ifEmpty {
            TranslationAudit.recordSheetGap(gapKey, language)
            entry.en
        }
        AppLanguage.LATIN -> entry.la.ifEmpty {
            TranslationAudit.recordSheetGap(gapKey, language)
            entry.en
        }
    }

    private fun patterns(source: String, language: AppLanguage): String {
        fun format(pattern: Regex, values: (MatchResult) -> Array<out Any>, translations: Entry): String? {
            val match = pattern.matchEntire(source) ?: return null
            // 槽位翻译失败（[text] 在 [untranslatedSlot] 上记的账）时这条模板不认：
            // 外壳翻了、槽位还漏着中文就是「翻一半混排」（登录页中日混排的成因，22.27.0 修）。
            // 宁可整句回退并记漏翻缺口，也不产出混排句。
            val saved = untranslatedSlot
            untranslatedSlot = false
            val parts = values(match)
            val bad = untranslatedSlot
            untranslatedSlot = saved || bad
            if (bad) return null
            return select(translations, pattern.pattern, language).format(*parts)
        }
        format(Regex("已开启 (\\d+) 项。"), { arrayOf(it.groupValues[1]) }, Entry("%s enabled.", "%s 件オンです。", "%s activa."))?.let { return it }
        format(Regex("(.+) · 把睡眠、压力、能量和恢复，合成今天的身体天气。"), { arrayOf(it.groupValues[1]) }, Entry("%s · Sleep, pressure, energy and recovery become today's body weather.", "%s · 睡眠、緊張、元気、回復から今日の体調をまとめます。", "%s · Somnus, pressio, vis et recuperatio hodiernum caelum corporis componunt."))?.let { return it }
        format(Regex("今天是按 (\\d+)/4 个信号算的，缺的那 (\\d+) 个显示「暂时没读到」。"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Figured from %s of the 4 signals; the other %s show “not available yet”.", "4 つの信号のうち %s つで計算しました。残りの %s つは「まだ読み取れません」です。", "Ex %s signis e 4 computatum; cetera %s “nondum lecta” monstrant."))?.let { return it }
        format(Regex("今天是(.+)。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Today is a %s.", "今日は%sです。", "Hodie est %s."))?.let { return it }
        format(Regex("今天的身体天气 · (.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Today's body weather · %s", "今日の体調 · %s", "Caelum corporis hodie · %s"))?.let { return it }
        format(Regex("约 (\\d+) 分钟"), { arrayOf(it.groupValues[1]) }, Entry("About %s min", "約%s分", "circa %s minuta"))?.let { return it }
        format(Regex("(\\d+) 分钟"), { arrayOf(it.groupValues[1]) }, Entry("%s min", "%s分", "%s minuta"))?.let { return it }
        format(Regex("开始 (\\d+) 分钟微行动"), { arrayOf(it.groupValues[1]) }, Entry("Start %s-minute action", "%s分の行動を始める", "Incipe: %s minuta"))?.let { return it }
        format(Regex("第 (\\d+) / (\\d+) 步"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Step %s of %s", "手順 %s / %s", "Gradus %s / %s"))?.let { return it }
        format(Regex("今日已倾听身体 (\\d+) 次。"), { arrayOf(it.groupValues[1]) }, Entry("You listened to your body %s times today.", "今日は体の声を%s回聞きました。", "Hodie corpus %s vicibus audivisti."))?.let { return it }
        // ---- 22.30.0 产品定位落地：天气 → 元素 → 食物闭环 / 回声正向计数 ----
        format(Regex("今天先补：(.+)。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Today, start with %s.", "今日はまず%sから。", "Hodie incipe a %s."))?.let { return it }
        format(Regex("今天先补：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Today, start with %s.", "今日はまず%sから。", "Hodie incipe a %s."))?.let { return it }
        format(Regex("昨天是(.+)，今天是(.+)。"), { arrayOf(text(it.groupValues[1], language), text(it.groupValues[2], language)) }, Entry("Yesterday was %s; today is %s.", "昨日は%sでしたが、今日は%sです。", "Heri fuit %s; hodie est %s."))?.let { return it }
        // ---- 22.32.0 模板：共 N 样 / 洞察句（句中天气词是源串里的中文定串，整句翻）----
        format(Regex("共 (\\d+) 样"), { arrayOf(it.groupValues[1]) }, Entry("%s items", "%s 個", "%s res"))?.let { return it }
        format(Regex("这一周，你有 (\\d+) 天是压力天——风大的日子里，先把肩膀放下来。"), { arrayOf(it.groupValues[1]) }, Entry("This week you had %s pressure days — on windy days, set your shoulders down first.", "今週はプレッシャーの日が%s日ありました——風の強い日はまず肩の力を抜きましょう。", "Hac septimana %s dies pressurae — die ventosi primum umeros demitte."))?.let { return it }
        format(Regex("这一周，你有 (\\d+) 天是恢复天——身体在慢慢补，不用急着赶路。"), { arrayOf(it.groupValues[1]) }, Entry("This week you had %s recovery days — the body is rebuilding slowly; no need to hurry.", "今週は回復の日が%s日ありました——体はゆっくり整っています。急がなくて大丈夫です。", "Hac septimana %s dies recuperationis — corpus lente reficitur; nihil festinandum."))?.let { return it }
        format(Regex("这一周，你有 (\\d+) 天是敏感天——听它说完，比问它为什么更重要。"), { arrayOf(it.groupValues[1]) }, Entry("This week you had %s sensitive days — listening matters more than asking why.", "今週は敏感な日が%s日ありました——「なぜ」を問うより、最後まで聞くほうが大切です。", "Hac septimana %s dies sensibiles — audire maius est quam quaerere cur."))?.let { return it }
        format(Regex("这一周，你有 (\\d+) 天是高能天——阳光好的日子，记得也留一点给明天。"), { arrayOf(it.groupValues[1]) }, Entry("This week you had %s high-energy days — on sunny days, save a little for tomorrow.", "今週は元気な日が%s日ありました——晴れた日も、明日のために少し残しておきましょう。", "Hac septimana %s dies validi — die sereno parum crastino serva."))?.let { return it }
        format(Regex("这一周，你有 (\\d+) 天是平衡天——稳，本身就是一件好事。"), { arrayOf(it.groupValues[1]) }, Entry("This week you had %s balanced days — steady is already a good thing.", "今週は安定した日が%s日ありました——安定していること自体が良いことです。", "Hac septimana %s dies aequi — stabilitas iam bonum est."))?.let { return it }
        format(Regex("看起来(.+)常常是(.+)——只是你自己的规律，不是规定。"), { arrayOf(text(it.groupValues[1], language), text(it.groupValues[2], language)) }, Entry("%s often turns out to be %s — your own pattern, not a rule.", "%sはよく%sのようです——あなたの傾向であって、規則ではありません。", "%s saepe est %s — tua consuetudo, non regula."))?.let { return it }
        format(Regex("你每次说「(.+)」，天气都是(.+)——身体的信号，常常是连着的。"), { arrayOf(text(it.groupValues[1], language), text(it.groupValues[2], language)) }, Entry("Whenever you say “%s”, the weather is %s — body signals often travel together.", "「%s」と言うときはいつも%sです——体の信号はつながっていることが多いもの。", "Quando dicis '%s', caelum est %s — signa corporis saepe coniuncta sunt."))?.let { return it }
        format(Regex("今天吃什么 · 去找含(.+)的食物"), { arrayOf(text(it.groupValues[1], language)) }, Entry("What to eat today · find foods with %s", "今日の食事 · %sを含む食材を探す", "Quid edam hodie · cibos cum %s quaere"))?.let { return it }
        format(Regex("食谱页已经按今天的身体天气筛好了，进去就是含(.+)的食物。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("The Food tab is already filtered by today’s body weather — open it for foods with %s.", "食事タブは今日の体調で絞り込んであります。%sを含む食材が出てきます。", "Tabula cibi iam filtrata est secundum caelum corporis hodie — aperi pro cibis cum %s."))?.let { return it }
        format(Regex("本周你照顾自己 (\\d+) 次。"), { arrayOf(it.groupValues[1]) }, Entry("You took care of yourself %s times this week.", "今週、あなたは自分を大切にした%s回。", "Hac septimana te %s vicibus curavisti."))?.let { return it }
        format(Regex("数据来源：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Data source: %s", "データソース：%s", "Origo datorum: %s"))?.let { return it }
        format(Regex("暂时没读到：(.+)"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("Not available yet: %s", "まだ読み取れない項目：%s", "Nondum lecta: %s"))?.let { return it }
        format(Regex("搜索「(.+)」"), { arrayOf(it.groupValues[1]) }, Entry("Search: “%s”", "「%s」の検索", "Quaerere: %s"))?.let { return it }
        format(Regex("已放到：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Moved to: %s", "移動先：%s", "Positum in: %s"))?.let { return it }
        format(Regex("BodyCast · (.+)"), { arrayOf(it.groupValues[1]) }, Entry("BodyCast · %s", "BodyCast · %s", "BodyCast · %s"))?.let { return it }
        format(Regex("(.+) 的身体天气"), { arrayOf(it.groupValues[1]) }, Entry("%s's body weather", "%s の体調", "Caelum corporis · %s"))?.let { return it }
        // ------------------------------------------------------------------
        // 下面这些是运行期拼接出来的串（厂商引导、来信、播报依据……），
        // 只能按模板匹配。规则：先具体、后笼统，笼统的那条放最后。
        // ------------------------------------------------------------------
        format(Regex("打开「(.+)」，进入「设备 → 健康数据共享」"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Open “%s”, then go to Device → Health data sharing", "「%s」を開き、「デバイス → 健康データ共有」へ", "Aperi %s, deinde ad Instrumentum → Communicatio datorum sanitatis"))?.let { return it }
        format(Regex("你说「(.+)」，所以今天是(.+)。"), { arrayOf(localizeList(it.groupValues[1], language), text(it.groupValues[2], language)) }, Entry("You said “%s”, so today is %s.", "「%s」と言ったので、今日は%sです。", "Dixisti %s, ergo hodie %s."))?.let { return it }
        format(Regex("我手动校准了「(.+)」"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("I calibrated by hand: “%s”", "手動で「%s」を選びました", "Manu calibravi: %s"))?.let { return it }
        format(Regex("你手动校准了「(.+)」，这一条优先于设备读数"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("You calibrated by hand: “%s”. This one outranks the device readings", "手動で「%s」を選びました。これはデバイスの測定値より優先されます", "Manu calibrasti: %s. Hoc mensuras instrumenti praecedit"))?.let { return it }
        format(Regex("「(.+)」出现了 (\\d+) 次，是你这周最常说的感受。"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("“%s” came up %s times, the feeling you named most this week.", "「%s」が%s回。今週いちばん多く選んだ感覚です。", "%s %s vicibus apparuit, sensus hac septimana saepissime dictus."))?.let { return it }
        format(Regex("其中最多的是(.+)。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Most of them were %s.", "その中でいちばん多かったのは%sです。", "Inter ea plurimum %s."))?.let { return it }
        format(Regex("这周你留下了 (\\d+) 天的天气。"), { arrayOf(it.groupValues[1]) }, Entry("You left %s days of weather this week.", "今週は%s日分の体調が残りました。", "Hac septimana %s dies caeli reliquisti."))?.let { return it }
        format(Regex("记了 (\\d+) 天。没记的日子就是没记，不补、不猜。"), { arrayOf(it.groupValues[1]) }, Entry("%s days recorded. Days without a record stay empty; nothing is filled in or guessed.", "%s日分の記録。記録していない日はそのままです。補わず、推測もしません。", "%s dies notati. Dies sine nota sine nota manent; nihil suppletur, nihil conicitur."))?.let { return it }
        format(Regex("(\\d+)月(\\d+)日 — (\\d+)月(\\d+)日"), { arrayOf(it.groupValues[1], it.groupValues[2], it.groupValues[3], it.groupValues[4]) }, Entry("%s/%s – %s/%s", "%s/%s – %s/%s", "%s/%s – %s/%s"))?.let { return it }
        format(Regex("另外，(.+)暂时没读到，这部分没有参与判断。"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("Also, %s could not be read just now, so they played no part in this.", "また、%sはまだ読み取れていないため、判断には入っていません。", "Praeterea %s nondum lecta sunt, quae iudicium non attigerunt."))?.let { return it }
        format(Regex("依据：(.+)。(.+)"), {
            val parts = it.groupValues[1].split('；').joinToString("; ") { part -> text(part, language) }
            val tail = text(it.groupValues[2], language).removeSuffix("。").removeSuffix(".")
            arrayOf(parts, tail)
        }, Entry("What this rests on: %s. %s", "根拠：%s。%s", "Ratio: %s. %s"))?.let { return it }
        format(Regex("今天可以先做这一件：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("One thing you can do today: %s", "今日はこれを一つ：%s", "Hodie unum satis: %s"))?.let { return it }
        format(Regex("你的手动校准：(.+)"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("Your manual calibration: %s", "手動調整：%s", "Calibratio tua manualis: %s"))?.let { return it }
        format(Regex("根据你今天的身体天气配的：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Chosen for today's body weather: %s", "今日の体調に合わせたもの：%s", "Pro hodierno caelo corporis: %s"))?.let { return it }
        format(Regex("下面这些和「(.+)」有关。"), { arrayOf(it.groupValues[1]) }, Entry("These are related to “%s”.", "以下は「%s」に関係するものです。", "Haec ad %s pertinent."))?.let { return it }
        format(Regex("没找到「(.+)」。换个词试试，比如「补铁」或者「菠菜」。"), { arrayOf(it.groupValues[1]) }, Entry("Nothing found for “%s”. Try another word, such as “iron” or “spinach”.", "「%s」は見つかりませんでした。別の言葉で試してください。たとえば「鉄」や「ほうれん草」。", "Nihil inventum pro %s. Aliud verbum tenta, ut ferrum aut spinacia."))?.let { return it }
        format(Regex("按你的饮食偏好，已经隐藏了 (\\d+) 项。想全看的话去设置里改。"), { arrayOf(it.groupValues[1]) }, Entry("%s items hidden by your diet preferences. Change them in Settings to see everything.", "食事の好みにより%s件を非表示にしています。すべて見るには設定で変更してください。", "%s res praeferentiis victus occultatae sunt. In Optionibus muta ut omnia videas."))?.let { return it }
        format(Regex("当前范围：(.+)。想全看的话去设置里改。"), { arrayOf(it.groupValues[1]) }, Entry("Showing: %s. Change this in Settings to see everything.", "現在の範囲：%s。すべて見るには設定で変更してください。", "Monstrantur: %s. In Optionibus muta ut omnia videas."))?.let { return it }
        format(Regex("在应用商店里搜索「(.+)」并安装。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Search for “%s” in the app store and install it.", "アプリストアで「%s」を検索してインストールします。", "In taberna quaere %s et installe."))?.let { return it }
        format(Regex("怎么吃 · (.+)"), { arrayOf(it.groupValues[1]) }, Entry("How to eat it · %s", "食べ方 · %s", "Quomodo edere · %s"))?.let { return it }
        format(Regex("已授权 (\\d+) / (\\d+) 项"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("%s of %s types allowed", "%s / %s 件を許可済み", "%s / %s generum permissa"))?.let { return it }
        format(Regex("主题（(.+)）、动画、字体大小。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Theme (%s), animations and text size.", "テーマ（%s）、アニメーション、文字サイズ。", "Thema (%s), motus, magnitudo litterarum."))?.let { return it }
        format(Regex("打开(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Open %s", "%s を開く", "Aperire %s"))?.let { return it }
        // ------------------------------------------------------------------
        // 22.13.0 补齐：设置 / 数据 / 回声 / 食谱里运行期拼出来的串。
        // 同样是先具体、后笼统；纯数字的「N 天」放这一段的最后。
        // ------------------------------------------------------------------
        format(Regex("检测到：(.+)"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("Detected: %s", "検出：%s", "Deprehensum: %s"))?.let { return it }
        format(Regex("读到了 ([0-9]+) 类数据"), { arrayOf(it.groupValues[1]) }, Entry("%s data types read", "%s 種類のデータを読み取りました", "%s genera datorum lecta"))?.let { return it }
        format(Regex("将合并 ([0-9]+) 天记录和偏好。同一天以备份里的为准，这一步不能撤销。"), { arrayOf(it.groupValues[1]) }, Entry("This will merge %s days of records and preferences. For any day they share, the backup wins; this cannot be undone.", "%s日分の記録と設定を統合します。同じ日はバックアップの内容が優先されます。この操作は取り消せません。", "Coniunget %s dies notarum et praeferentiarum. Eodem die exemplar praecedit; hoc revocari non potest."))?.let { return it }
        format(Regex("合并了 ([0-9]+) 天记录。本机没有导入任何原始心率、睡眠或血氧数值。"), { arrayOf(it.groupValues[1]) }, Entry("Merged %s days of records. No raw heart rate, sleep or blood oxygen values were imported.", "%s日分の記録を統合しました。心拍数・睡眠・血中酸素の生の数値は端末に取り込んでいません。", "Coniuncti %s dies notarum. Nulla mensura cruda cordis, somni aut oxygenii sanguinis importata est."))?.let { return it }
        format(Regex("版本：(.+) \\((.+)\\)"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Version: %s (%s)", "バージョン：%s (%s)", "Versio: %s (%s)"))?.let { return it }
        format(Regex("去商店搜「(.+)」"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Search “%s” in the app store", "ストアで「%s」を検索", "In taberna quaere %s"))?.let { return it }
        format(Regex("这台机器上没检测到「(.+)」，先装上它并登录手环账号。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("“%s” was not found on this device; install it and sign in to your band account.", "この端末に「%s」が見つかりませんでした。まずインストールして、ウェアラブルのアカウントにログインしてください。", "%s in hoc instrumento non inventum est; installe id et intra rationem armillae."))?.let { return it }
        format(Regex("正在搜：(.+) —— 下面这些和「(.+)」有关。"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Searching: %s — these are related to “%s”.", "検索中：%s — 以下は「%s」に関係するものです。", "Quaeritur: %s — haec ad %s pertinent."))?.let { return it }
        format(Regex("正在看：(.+) —— 下列食物都能提供它。"), { arrayOf(it.groupValues[1]) }, Entry("Showing: %s — these foods all provide it.", "表示中：%s — 以下の食べ物から取れます。", "Monstrantur: %s — haec alimenta id praebent."))?.let { return it }
        format(Regex("(.+)最多的天气 · (.+)"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("%s: most common weather · %s", "%sがいちばん多い体調 · %s", "%s: caelum frequentissimum · %s"))?.let { return it }
        format(Regex("其次是 (.+)，([0-9]+) 天"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Then %s, %s days", "次に%s、%s日", "Deinde %s, %s dies"))?.let { return it }
        format(Regex("最常说的感觉 · (.+)"), { arrayOf(it.groupValues[1]) }, Entry("Most-named feeling · %s", "いちばん多く選んだ感覚 · %s", "Sensus saepissime dictus · %s"))?.let { return it }
        format(Regex("(.+)：上周 ([0-9]+) 天 → 本周 ([0-9]+) 天"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2], it.groupValues[3]) }, Entry("%s: last week %s days → this week %s days", "%s：先週 %s日 → 今週 %s日", "%s: septimana praeterita %s dies → haec septimana %s dies"))?.let { return it }
        format(Regex("([0-9]+) 天"), { arrayOf(it.groupValues[1]) }, Entry("%s days", "%s日", "%s dies"))?.let { return it }
        // ------------------------------------------------------------------
        // 22.22.0：数据流水线自检卡片里运行期拼出来的串。
        // 「最后写入」那一列的时间是拼出来的（今天/昨天/更早 + HH:mm），
        // 不登记成模板的话，非中文界面会把整串原样漏出去、并记一条假缺口。
        // ------------------------------------------------------------------
        format(Regex("今天 (\\d{2}:\\d{2})"), { arrayOf(it.groupValues[1]) }, Entry("Today %s", "今日 %s", "Hodie %s"))?.let { return it }
        format(Regex("昨天 (\\d{2}:\\d{2})"), { arrayOf(it.groupValues[1]) }, Entry("Yesterday %s", "昨日 %s", "Heri %s"))?.let { return it }
        // 更早的日期是纯数字（MM-DD），各语言写法一致，登记只为不给审计添假缺口。
        format(Regex("(\\d{2})-(\\d{2}) (\\d{2}:\\d{2})"), { arrayOf(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }, Entry("%s-%s %s", "%s-%s %s", "%s-%s %s"))?.let { return it }
        format(Regex("还没授权：(.+)"), { arrayOf(localizeList(it.groupValues[1], language)) }, Entry("Not allowed yet: %s", "まだ許可されていない項目：%s", "Nondum permissa: %s"))?.let { return it }
        // ------------------------------------------------------------------
        // 小米账号（云同步，22.24.0）里的运行期拼接。
        // 账号线索、主机名这些动态值**原样带出**、不再喂回 text()：它们是用户自己的
        // 数据或网络地址，翻不了，也不该被记成漏翻缺口。带时间戳的那一段照旧走模板。
        // ------------------------------------------------------------------
        format(Regex("已连接：(.+) · (.+)"), { arrayOf(it.groupValues[1], text(it.groupValues[2], language)) }, Entry("Connected: %s · %s", "接続済み：%s · %s", "Coniunctum: %s · %s"))?.let { return it }
        format(Regex("账号：(.+)"), { arrayOf(it.groupValues[1]) }, Entry("Account: %s", "アカウント：%s", "Ratio: %s"))?.let { return it }
        format(Regex("数据区域：(.+)"), { arrayOf(it.groupValues[1]) }, Entry("Data region: %s", "データ地域：%s", "Regio datorum: %s"))?.let { return it }
        format(Regex("当前页面：(.+)"), { arrayOf(it.groupValues[1]) }, Entry("Current page: %s", "現在のページ：%s", "Pagina nunc: %s"))?.let { return it }
        format(Regex("已连接，读到 (\\d+) 类读数。"), { arrayOf(it.groupValues[1]) }, Entry("Connected; %s types read.", "接続済み。%s 種類を読み取りました。", "Coniunctum; %s genera lecta."))?.let { return it }
        format(Regex("同步完成，读到 (\\d+) 类读数。"), { arrayOf(it.groupValues[1]) }, Entry("Sync finished; %s types read.", "同期完了。%s 種類を読み取りました。", "Synchronizatio perfecta; %s genera lecta."))?.let { return it }
        format(Regex("同步完成，读到 (\\d+) 类；另有 (\\d+) 类这次没能读到（网络原因），下次同步会再试。"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Sync finished; %s types read. Another %s could not be read this time (network); the next sync will try again.", "同期完了。%s 種類を読み取りました。他に %s 種類は今回読み取れませんでした（通信の問題）。次回の同期で再試行します。", "Synchronizatio perfecta; %s genera lecta. Alia %s hac vice legi non potuerunt (rete); proxima synchronizatio iterum tentabit."))?.let { return it }
        // 开发者页那四条自检结论：问句固定，答案只有「是 / 否」两种。
        format(Regex("密文与明文不同：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Ciphertext differs from plaintext: %s", "暗号文は平文と異なります：%s", "Litterae occultae a textu aperto differunt: %s"))?.let { return it }
        format(Regex("加密后能原样读回来：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Reads back unchanged after encryption: %s", "暗号化しても同じ内容で読み出せます：%s", "Post encryptionem eadem legitur: %s"))?.let { return it }
        format(Regex("删除密钥之后读不回来：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Cannot be read after the key is deleted: %s", "鍵を削除すると読み出せません：%s", "Clave deleta legi non potest: %s"))?.let { return it }
        format(Regex("系统密钥库可用：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("System key store available: %s", "システムのキーストアが使えます：%s", "Clavigera systematis praesto est: %s"))?.let { return it }
        // ------------------------------------------------------------------
        // 22.23.0：自检卡片顶部那句结论（四步全通时要带上最后一次写入时间），
        // 以及卡里那行「刚刚检查于 HH:MM」——两处都是运行期拼出来的。
        // ------------------------------------------------------------------
        format(Regex("四步都通 · 数据会自动更新，最后一次 (.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("All four steps pass · data updates by itself, last write %s", "4 ステップすべて正常です · データは自動で更新されます。最終書き込み %s", "Omnes gradus quattuor valent · data sponte renovantur, ultimum %s"))?.let { return it }
        format(Regex("刚刚检查于 (\\d{2}:\\d{2})"), { arrayOf(it.groupValues[1]) }, Entry("Checked just now at %s", "たった今 %s に確認しました", "Modo inspectum %s"))?.let { return it }
        // ------------------------------------------------------------------
        // 22.24.0：手动填写读数 / 文件导入（方案 B）里运行期拼出来的串。
        // 规则照旧：先具体、后笼统——「上次填写：…」在「…（已过期）」之前，
        // 免得后者的通配把整段时间戳也吞进去。
        // ------------------------------------------------------------------
        format(Regex("上次填写：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Last filled in: %s", "前回の入力：%s", "Ultimo inscriptum: %s"))?.let { return it }
        format(Regex("上次导入：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Last import: %s", "前回の取り込み：%s", "Ultimum importatum: %s"))?.let { return it }
        format(Regex("(.+)（已过期）"), { arrayOf(text(it.groupValues[1], language)) }, Entry("%s (expired)", "%s（期限切れ）", "%s (expiratum)"))?.let { return it }
        format(Regex("(.+) 只能填数字。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("%s takes numbers only.", "%s は数字だけを入力してください。", "%s numeros tantum accipit."))?.let { return it }
        format(Regex("(.+) 只能填 (\\d+)-(\\d+)。"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2], it.groupValues[3]) }, Entry("%s must be between %s and %s.", "%s は %s〜%s の範囲で入力してください。", "%s inter %s et %s esse debet."))?.let { return it }
        format(Regex("有 (\\d+) 项填得不对：超出范围的数字不会被保存，改好再点保存。"), { arrayOf(it.groupValues[1]) }, Entry("%s fields are out of range: out-of-range numbers are not saved. Fix them and save again.", "%s 件が範囲外です。範囲外の数字は保存されません。直してからもう一度保存してください。", "%s campi extra fines sunt; numeri extra fines non servantur. Corrige et iterum serva."))?.let { return it }
        format(Regex("现在会记录 6 类里的 (\\d+) 类。"), { arrayOf(it.groupValues[1]) }, Entry("This will record %s of the 6 types.", "6 種類のうち %s 種類を記録します。", "%s ex 6 generibus notabuntur."))?.let { return it }
        format(Regex("已记录：(.+) · 6 类里填了 (\\d+) 类"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("Recorded: %s · %s of 6 types filled in", "記録しました：%s · 6 種類のうち %s 種類を入力", "Notatum: %s · %s ex 6 generibus inscripta"))?.let { return it }
        format(Regex("这份数据是 (\\d+) 天前的"), { arrayOf(it.groupValues[1]) }, Entry("This data is %s days old", "このデータは %s 日前のものです", "Haec data %s dies vetusta sunt"))?.let { return it }
        format(Regex("识别 (\\d+) 项 / 忽略 (\\d+) 列"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("%s readings recognised / %s columns ignored", "%s 項目を認識 / %s 列を無視", "%s mensurae cognitae / %s columnae neglectae"))?.let { return it }
        format(Regex("已导入：(.+) · 识别 (\\d+) 项"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("Imported: %s · %s readings", "取り込みました：%s · %s 項目", "Importatum: %s · %s mensurae"))?.let { return it }
        // ------------------------------------------------------------------
        // 22.27.0：回前台自动轻量刷新 / 数据源页与首页的「上次读取」，
        // 以及自检第 ③ 步里小米云那一行（运行期拼接）。
        // 「按 N/4 个信号算的」那条在上面「今天是(.+)。」之前——先具体、后笼统。
        // ------------------------------------------------------------------
        format(Regex("上次读取：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Last read: %s", "前回の読み取り：%s", "Ultima lectio: %s"))?.let { return it }
        // ---- HC 写入桥（22.42.0）：写入状态行 ----
        format(Regex("已写入 (\\d+) 条。"), { arrayOf(it.groupValues[1]) }, Entry("%s records written.", "%s 件書き込みました。", "%s res scriptae sunt."))?.let { return it }
        format(Regex("上次写入：(.+) · (\\d+) 条"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("Last write: %s · %s records", "前回の書き込み：%s · %s 件", "Ultima scriptura: %s · %s res"))?.let { return it }
        // 自检第 ③ 步里小米云那一行（22.27.0）：状态 + 上次同步时间 + 读到几类。
        format(Regex("小米账号（云同步）· 上次同步 (.+) · 六类里读到 (\\d+) 类"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("Mi account (cloud sync) · last synced %s · %s of 6 types read", "Mi アカウント（クラウド同期）· 最終同期 %s · 6 種類のうち %s 種類を読み取り", "Ratio Mi (nubes) · ultima %s · %s e 6 generibus lecta"))?.let { return it }
        format(Regex("小米账号（云同步）· 六类里读到 (\\d+) 类"), { arrayOf(it.groupValues[1]) }, Entry("Mi account (cloud sync) · %s of 6 types read", "Mi アカウント（クラウド同期）· 6 種類のうち %s 種類を読み取り", "Ratio Mi (nubes) · %s e 6 generibus lecta"))?.let { return it }
        format(Regex("小米账号（云同步）· 上次同步 (.+) · 已过期（超过 24 小时），不参与今天的判定。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Mi account (cloud sync) · last synced %s · expired (over 24 hours), not used for today", "Mi アカウント（クラウド同期）· 最終同期 %s · 期限切れ（24 時間超過）のため今日の判定には使いません", "Ratio Mi (nubes) · ultima %s · expiratum (plus quam 24 horae); hodie non computatur"))?.let { return it }
        // 运行期拼接的多行文本（例如「动作名\n提示」）：整串一定匹配不上任何词条，
        // 但每一行本身是登记过的。逐行翻译再拼回去，避免整块漏出中文。
        if (source.contains('\n')) {
            val lines = source.split('\n')
            val translated = lines.map { text(it, language) }
            if (translated != lines) return translated.joinToString("\n")
        }

        // ---- 22.34.0 漏翻补登记：插值串 ----
        format(Regex("(.+) (\\d+) 分钟"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2]) }, Entry("%s for %s min", "%s %s分", "%s per %s minuta"))?.let { return it }

        format(Regex("(\\d+) 条 · 最新 "), { arrayOf(it.groupValues[1]) }, Entry("%s records · latest ", "%s 件・最新 ", "%s notae · ultima "))?.let { return it }
        format(Regex("(.+) 只能填 (\\d+)-(\\d+)。"), { arrayOf(text(it.groupValues[1], language), it.groupValues[2], it.groupValues[3]) }, Entry("%s can only be %s-%s.", "%s は %s-%s のみ入力できます。", "%s tantum %s-%s esse potest."))?.let { return it }
        format(Regex("(.+) 的身体天气"), { arrayOf(text(it.groupValues[1], language)) }, Entry("%s's body weather", "%s の体調", "Caelum corporis: %s"))?.let { return it }
        format(Regex("小米云拒绝了这次请求（HTTP (\\d+)）。"), { arrayOf(it.groupValues[1]) }, Entry("Xiaomi Cloud rejected the request (HTTP %s).", "Xiaomi クラウドがリクエストを拒否しました（HTTP %s）。", "Nubes Xiaomi petitionem reiecit (HTTP %s)."))?.let { return it }
        format(Regex("小米云返回 HTTP (\\d+)：(.+)"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Xiaomi Cloud returned HTTP %s: %s", "Xiaomi クラウドが HTTP %s を返しました：%s", "Nubes Xiaomi HTTP %s reddidit: %s"))?.let { return it }
        format(Regex("小米云拒绝了这次请求（代码 (\\d+)）。"), { arrayOf(it.groupValues[1]) }, Entry("Xiaomi Cloud rejected the request (code %s).", "Xiaomi クラウドがリクエストを拒否しました（コード %s）。", "Nubes Xiaomi petitionem reiecit (codex %s)."))?.let { return it }
        format(Regex("小米账号没有按预期返回内容（HTTP (\\d+)）。"), { arrayOf(it.groupValues[1]) }, Entry("The Mi account did not return what was expected (HTTP %s).", "Mi アカウントが想定どおりの応答を返しませんでした（HTTP %s）。", "Ratio Mi non expectatum reddidit (HTTP %s)."))?.let { return it }
        format(Regex("小米账号不再接受这份会话（代码 (\\d+)），需要重新连接。"), { arrayOf(it.groupValues[1]) }, Entry("The Mi account no longer accepts this session (code %s); reconnect.", "Mi アカウントがこのセッションを受け付けなくなりました（コード %s）。再接続してください。", "Ratio Mi hanc sessionem non accipit (codex %s); denuo coniunge."))?.let { return it }
        format(Regex("小米账号返回了代码 (\\d+)。"), { arrayOf(it.groupValues[1]) }, Entry("The Mi account returned code %s.", "Mi アカウントがコード %s を返しました。", "Ratio Mi codicem %s reddidit."))?.let { return it }
        // ------------------------------------------------------------------
        // 22.48.0：数据连接指导（授权指南）+「今天的运动量 → 今天先补什么」里
        // 运行期拼出来的串。规则照旧：先具体、后笼统。
        // ------------------------------------------------------------------
        format(Regex("通，读到 (\\d+) 类"), { arrayOf(it.groupValues[1]) }, Entry("Working — %s types read", "正常 — %s 種類を読み取り", "Valet — %s genera lecta"))?.let { return it }
        format(Regex("读到 (\\d+) 类读数"), { arrayOf(it.groupValues[1]) }, Entry("%s types read", "%s 種類を読み取り", "%s genera lecta"))?.let { return it }
        format(Regex("已经通了：系统健康服务里读到了 (\\d+) 类数据，首页正在用它算今天的天气。"), { arrayOf(it.groupValues[1]) }, Entry("It works: %s data types have been read from the system health service, and the home page is using them for today's weather.", "通っています：システムのヘルスサービスから %s 種類のデータを読み取り、ホームがそれで今日の天気を計算しています。", "Valet: %s genera datorum e servitio sanitatis lecta sunt, quibus pagina domus caelum hodiernum computat."))?.let { return it }
        format(Regex("今天的身体天气是「(.+)」——先按它来。"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Today's body weather is “%s” — start there.", "今日の体調は「%s」——まずそれから。", "Caelum corporis hodie “%s” est — inde incipe."))?.let { return it }
        format(Regex("今天已经做完 (\\d+) 件；还想做的话，可以先做这一件：(.+)"), { arrayOf(it.groupValues[1], text(it.groupValues[2], language)) }, Entry("You have finished %s today; if you feel like another, this works: %s", "今日はすでに%s件終えました。もう一つやるなら、これがおすすめです：%s", "Hodie %s perfecisti; si plus vis, hoc convenit: %s"))?.let { return it }
        // 22.49.0：微行动语音引导的两句（报步号 / 开场说明）。
        format(Regex("第 (\\d+) 步：(.+)"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Step %s: %s", "%s 歩目：%s", "Gradus %s: %s"))?.let { return it }
        format(Regex("共 (\\d+) 件小事，约 (\\d+) 分钟。第一件：(.+)"), { arrayOf(it.groupValues[1], it.groupValues[2], text(it.groupValues[3], language)) }, Entry("%s small things, about %s minutes. First: %s", "小さなことを%s件、約%s分。最初は：%s", "%s res parvae, circa %s minuta. Prima: %s"))?.let { return it }
        // 22.49.0：语音与回声入口的两句（带槽位）。
        format(Regex("在回声页：今天可以先做这一件：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("On the Echo page: today it suggests %s", "記録ページにあります：今日はまず%s", "In pagina Echo: hodie suadet %s"))?.let { return it }
        format(Regex("本机没有「(.+)」的音色，先用系统默认音色朗读。"), { arrayOf(it.groupValues[1]) }, Entry("No voice for %s on this device — reading with the system default voice for now.", "この端末に「%s」の音声がないため、当面はシステム既定の音声で読み上げます。", "Vox linguae %s deest — voce systematis praedefinita nunc legitur."))?.let { return it }
        // 22.49.0：称呼页的字数提示。
        format(Regex("现在写了 (\\d+) / 20 个字。"), { arrayOf(it.groupValues[1]) }, Entry("%s / 20 characters so far.", "いま %s / 20 文字です。", "%s / 20 litterae adhuc."))?.let { return it }
        // 22.50.0：首页的两句运行期拼接。
        format(Regex("结论 = 客观读数（(\\d+)/4 个信号）+ 你的感受（(\\d+) 个标签）"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("Conclusion = readings (%s of 4 signals) + how you feel (%s tags)", "結論 = 客観データ（シグナル %s/4）+ あなたの感覚（%s 個）", "Conclusio = mensurae (%s e 4 signis) + sensus tuus (%s notae)"))?.let { return it }
        format(Regex("来源：(.+)"), { arrayOf(text(it.groupValues[1], language)) }, Entry("From: %s", "出典：%s", "Origo: %s"))?.let { return it }
        format(Regex("只看含(.+)的食物"), { arrayOf(text(it.groupValues[1], language)) }, Entry("Only foods with %s", "%sを含む食材だけ", "Solum cibos cum %s"))?.let { return it }
        format(Regex("今天走了 (\\d+) 步，还有 (\\d+) 分钟中高强度运动——先补矿物质，把出汗流失的补回来。"), { arrayOf(it.groupValues[1], it.groupValues[2]) }, Entry("%s steps today plus %s minutes of moderate-to-vigorous activity — minerals first, to put back what sweating took out.", "今日は%s歩、さらに中〜高強度の運動が%s分——まずミネラルを。汗で失った分を戻しましょう。", "Hodie %s gradus et %s minuta exercitationis — primum mineralia, ut sudore amissa restituantur."))?.let { return it }
        format(Regex("今天走了 (\\d+) 步——先补矿物质，把出汗流失的补回来。"), { arrayOf(it.groupValues[1]) }, Entry("%s steps today — minerals first, to put back what sweating took out.", "今日は%s歩——まずミネラルを。汗で失った分を戻しましょう。", "Hodie %s gradus — primum mineralia, ut sudore amissa restituantur."))?.let { return it }
        format(Regex("今天有 (\\d+) 分钟中高强度运动——先补矿物质，肌肉放松用得着。"), { arrayOf(it.groupValues[1]) }, Entry("%s minutes of moderate-to-vigorous activity today — minerals first; they help muscles unwind.", "今日は中〜高強度の運動が%s分——まずミネラルを。筋肉をゆるめるのに役立ちます。", "Hodie %s minuta exercitationis — primum mineralia; musculos relaxant."))?.let { return it }
        return source
    }

    private fun fallback(source: String, language: AppLanguage): String {
        // 有些调用点会把「已经翻好的串」再喂回来一次（把 `kind.label` 拼进模板后整句又翻一遍），
        // 那不是缺口。不过滤的话审计列表里全是假阳性，真正的缺口反而看不见。
        if (!looksAlreadyLocalized(source, language)) TranslationAudit.recordFallback(source)
        return normalizePunctuation(source, language)
    }

    /** 日语假名。含有假名的串一定是日语译文，不是待翻的中文源串。 */
    private val KANA = Regex("[\u3040-\u30ff\uff66-\uff9d]")

    /** 汉字。英语 / 拉丁产出里出现汉字 = 漏翻；日语要靠 [KANA] 和译文表区分。 */
    private val HAN = Regex("[\u4e00-\u9fff]")

    /** 本地化日期的样子（`formatDate` 的产物，各语言一条）。 */
    private val LOCALIZED_DATE = Regex(
        "(\\d{4}年\\d{1,2}月\\d{1,2}日)|([A-Z][a-z]+ \\d{1,2}, \\d{4})|(\\d{1,2} [A-Z][a-z]+ \\d{4})"
    )

    /** 各语言已经产出过的全部译文，用来判断「这条其实已经翻好了」。 */
    private val outwardValues: Map<AppLanguage, Set<String>> by lazy {
        mapOf(
            AppLanguage.ENGLISH to (entries.values.map { it.en } +
                HomeTextCatalog.outwardValues(AppLanguage.ENGLISH) +
                DomainText.outwardValues(AppLanguage.ENGLISH)).toSet(),
            AppLanguage.JAPANESE to (entries.values.map { it.ja } +
                HomeTextCatalog.outwardValues(AppLanguage.JAPANESE) +
                DomainText.outwardValues(AppLanguage.JAPANESE)).toSet(),
            // 拉丁文没有假名这类一眼可辨的标记，噪音过滤几乎全靠这张表：
            // 拼进模板后整句又被喂回来时，只要那一段是登记过的译文就不记缺口。
            AppLanguage.LATIN to (entries.values.map { it.la } +
                HomeTextCatalog.outwardValues(AppLanguage.LATIN) +
                DomainText.outwardValues(AppLanguage.LATIN)).toSet()
        )
    }

    /**
     * 串是不是「已经翻好了又被喂回来一遍」的？
     *
     * 判据三条，都很便宜，宁可漏记也不要把审计塞满噪音：
     * 1. 日语串里含假名（中文源串里不会出现假名）；
     * 2. 串是本地化日期；
     * 3. 串本身就是某一门语言产出过的译文（覆盖全汉字的日语词，如「緊張」「空腹」）。
     */
    private fun looksAlreadyLocalized(source: String, language: AppLanguage): Boolean {
        if (language == AppLanguage.JAPANESE && KANA.containsMatchIn(source)) return true
        if (LOCALIZED_DATE.containsMatchIn(source)) return true
        return source in (outwardValues[language] ?: emptySet())
    }

    /**
     * 万一有串掉到这里，至少要保证标点是英文标点。
     * `joinToString("、")` 拼出来的列表走不进任何条目，但也不该在英文界面上出现顿号。
     */
    private fun normalizePunctuation(source: String, language: AppLanguage): String {
        // 日语和中文同用「、。！？」「」，只有拉丁字母那一路才需要换 ASCII 标点。
        if (language == AppLanguage.SIMPLIFIED_CHINESE ||
            language == AppLanguage.TRADITIONAL_CHINESE ||
            language == AppLanguage.JAPANESE
        ) return source
        val pairs = mapOf(
            "、" to ", ", "。" to ". ", "，" to ", ", "：" to ": ", "；" to "; ",
            "「" to "“", "」" to "”", "（" to " (", "）" to ") ", "！" to "!", "？" to "?"
        )
        var result = source
        pairs.forEach { (from, to) -> result = result.replace(from, to) }
        return result.trimEnd()
    }

    /**
     * 列表分隔符。英语和拉丁文用 ASCII 逗号，中日文用顿号。
     * 拉丁文的分隔符正好落在 [normalizePunctuation] 的 else 分支（ASCII 那一档），
     * 所以标点不用另外处理。
     */
    private fun localizeList(value: String, language: AppLanguage): String =
        value.split('、').joinToString(
            if (language == AppLanguage.ENGLISH || language == AppLanguage.LATIN) ", " else "、"
        ) { text(it, language) }

    // ------------------------------------------------------------------
    // 营养素说明（只写「管什么」，不含任何数字或摄入量）
    // ------------------------------------------------------------------
    init {
        add("修东西用的原料", "Raw material for repairs", "からだを修復する材料", "Materia reficiendi")
        add("让肠道有事可做", "Gives the gut something to do", "腸に仕事をあたえる", "Intestino laborem dat")
        add("肠道里的住客", "The residents in your gut", "腸内の住人", "Incolae intestini")
        add("对脑子友好的一类脂肪", "A kind of fat the brain likes", "脳にやさしい脂肪の一種", "Adeps cerebro gratus")
        add("容易缺，尤其是女性", "Easily short, especially for women", "不足しやすく、特に女性に", "Saepe deest, praesertim feminis")
        add("肌肉放松用得着", "Used when muscles let go", "筋肉がゆるむときに使われます", "Cum musculi laxantur")
        add("帮助吸收铁", "Helps iron get absorbed", "鉄の吸収を助けます", "Ferrum absorberi iuvat")
        add("慢一点释放的能量", "Energy that releases slowly", "ゆっくり放出されるエネルギー", "Energia lente liberata")
        add("骨头的日常开销", "The daily upkeep of bones", "骨の日々の維持費", "Sumptus ossium cotidianus")
        add("伤口和味觉都用得着", "Used for wounds and for taste", "傷の修復と味覚に使われます", "Vulneribus et gustatui")
        add("对眼睛和皮肤友好", "Kind to eyes and skin", "目と肌にやさしい", "Oculis et cuti gratus")
        add(
            "晒太阳也能得到，食物里不多",
            "Sunlight makes it too; food carries little",
            "日光でもつくられますが、食べ物には多くありません",
            "Sol id parit; in cibo parum est"
        )
    }

    // ------------------------------------------------------------------
    // 食谱：为什么值得吃 / 怎么做（卡片里不出现数字，所以全文不用阿拉伯数字）
    // ------------------------------------------------------------------
    init {
        add(
            "最省事的完整蛋白，煮一个就能顶一会儿",
            "The easiest complete protein; boil one and it holds you for a while",
            "いちばん手軽な完全たんぱく質。一つゆでればしばらく持ちます。"
        )
        add(
            "水开下锅 8 分钟，捞出来过一下凉水更好剥",
            "Lower it into boiling water, then cool it under cold water so the shell comes off cleanly",
            "熱湯に入れて8分ゆで、冷水に取ると殻がむきやすくなります。"
        )
        add("肠道和骨头都能照顾到，还不用开火", "Looks after gut and bones, and needs no cooking", "腸と骨の両方に良くて、火も使いません。")
        add(
            "挑配料表短的，糖越少越好；饭后吃比空腹吃舒服",
            "Pick a short ingredient list with little sugar; it sits better after a meal than on an empty stomach",
            "原材料の少ないものを選び、糖は少なめに。空腹より食後のほうが落ち着きます。"
        )
        add(
            "随身带着就能吃，腿容易抽的人可以常备",
            "Tuck one in your bag and eat it anywhere; handy if your legs cramp easily",
            "持ち歩いてそのまま食べられます。足がつりやすい人は常備しておくと安心です。"
        )
        add(
            "两餐之间吃，别空腹当饭吃",
            "Eat it between meals rather than on an empty stomach as a meal",
            "食事と食事の間に。空腹のまま主食代わりにしないでください。"
        )
        add(
            "耐放、好处理，冰箱里囤一点也不容易坏",
            "Keeps well, easy to prep, and stores fine in the fridge",
            "日持ちがよく扱いやすいので、冷蔵庫に少し置いておいても傷みません。"
        )
        add(
            "掰小朵，蒸 5 分钟，淋点生抽就行",
            "Break into florets, steam until just tender, then splash with soy sauce",
            "小房に分けて5分蒸し、しょうゆを少しかければ完成です。"
        )
        add("饱腹感强，微波炉就能搞定", "Filling, and the microwave does the work", "満足感があり、電子レンジだけで済みます。")
        add(
            "洗净扎几个孔，微波 6 分钟，比烤的快得多",
            "Wash it, prick a few holes and microwave; much faster than roasting",
            "洗って数か所穴をあけ、6分チン。焼くよりずっと早いです。"
        )
        add("剥开就能吃，顺手还能补点水", "Peel it and eat; a little water comes along with it", "むいてそのまま食べられます。水分もついでに取れます。")
        add(
            "白色那层筋膜别撕太干净，纤维在那里",
            "Leave some of the white pith on; that is where the fiber sits",
            "白いわたは取りすぎないでください。食物繊維はそこにあります。"
        )
        add("维生素 C 密度高，一个就够", "Dense in vitamin C, so one is enough", "ビタミンCがぎっしりなので、一つで十分です。")
        add(
            "对半切开用勺子挖着吃，比削皮省事",
            "Halve it and scoop with a spoon; less fuss than peeling",
            "半分に切ってスプーンですくうほうが、皮をむくより簡単です。"
        )
        add("抓一把当零嘴，比薯片划算", "A handful as a snack beats crisps", "ひとつかみを間食に。ポテトチップスより体にいいです。")
        add("买原味未盐焗的，一次一小把", "Buy them plain and unsalted, and keep to a small handful", "無塩・素焼きのものを買い、一度にひとつかみまで。")
        add("对脑力消耗大的人比较友好", "A friendly choice when your head is working hard", "頭をよく使う人に向いています。")
        add(
            "一天两三个就够，剥好的容易吃多",
            "A couple a day is plenty; shelled ones are easy to overeat",
            "一日二、三個で十分です。むいたものは食べすぎやすいです。"
        )
        add("最方便的钙来源", "The most convenient source of calcium", "いちばん手軽なカルシウム源")
        add("乳糖不耐就换无乳糖的，效果一样", "If lactose bothers you, switch to lactose-free milk", "乳糖が合わない場合は無乳糖のものに。効果は同じです。")
        add(
            "鲜味足，能让少油少盐的菜也好吃",
            "Full of savory depth, so a low-oil, low-salt dish still tastes good",
            "うま味が強いので、油も塩も少ない料理がおいしくなります。"
        )
        add(
            "别泡水洗，用湿布擦一擦更香",
            "Wipe them with a damp cloth instead of soaking; they stay firmer",
            "水に浸さず、湿らせた布で拭くほうが香りが残ります。"
        )
        add(
            "对眼睛好，也是深色蔬菜里最便宜好存的一种",
            "Good for the eyes, and the cheapest dark vegetable to keep on hand",
            "目にやさしく、色の濃い野菜では安くて日持ちもします。"
        )
        add(
            "适合和油脂一起烹饪，炒着吃或者跟肉一起炖，比生啃吸收得好",
            "Cook it with a little oil, stir-fried or braised with meat, rather than raw",
            "油と一緒に調理すると吸収がよくなります。炒めたり肉と煮たり、生でかじるより吸収されます。"
        )
        add(
            "软、甜、好入口，能顶一部分主食",
            "Soft, sweet and easy to eat; it can stand in for part of a staple",
            "やわらかく甘く食べやすいので、主食の一部の代わりになります。"
        )
        add(
            "切块蒸熟就行，皮薄的话不用削；也可以和小米一起煮粥",
            "Steam it in chunks, thin skin needs no peeling, and it also cooks into porridge",
            "角切りにして蒸すだけ。皮が薄ければむかなくて大丈夫です。粟と一緒にお粥にしてもいいです。"
        )
        add("深色叶菜里最百搭的一种", "The most versatile of the dark leafy greens", "色の濃い葉物では、いちばん何にでも合うもの")
        add(
            "焯水 30 秒再炒，涩味会少很多",
            "Blanch it briefly before stir-frying and the astringency drops away",
            "30秒下ゆでしてから炒めると、渋味がかなり減ります。"
        )
        add("植物蛋白里最容易买到的一种", "The easiest plant protein to find", "植物性たんぱく質では、いちばん手に入りやすいもの")
        add(
            "切块用厨房纸吸干水，煎的时候不容易碎",
            "Cube it and pat it dry, and it holds together in the pan",
            "角切りにしてキッチンペーパーで水気を拭くと、焼いても崩れにくくなります。"
        )
        add(
            "煮一锅可以吃好几顿，冷藏能放三四天",
            "Cook one pot and eat from it for days; it keeps in the fridge",
            "一度煮れば何食分にもなり、冷蔵で三、四日持ちます。"
        )
        add(
            "提前泡一夜，煮的时候加点盐更容易软",
            "Soak overnight and add a little salt while boiling so they soften",
            "一晩水に浸し、煮るときに少し塩を入れるとやわらかくなります。"
        )
        add("比白米多一点嚼头，血糖也更平缓", "More chew than white rice, and a gentler rise", "白米より歯ごたえがあり、血糖値の上がり方もゆるやかです。")
        add(
            "掺三分之一到白米里一起煮，口感更容易接受",
            "Cook a third of it in with white rice for an easier texture",
            "白米に三分の一ほど混ぜて炊くと、食べやすい食感になります。"
        )
        add("汤里丢一点，鲜味和矿物质都有了", "A little in the pot adds savor and minerals", "汁に入れるだけで、うま味とミネラルが取れます。")
        add(
            "紫菜撕碎用开水一冲就是一碗汤",
            "Tear the seaweed and pour hot water over it for an instant soup",
            "のりをちぎって熱湯を注げば、すぐ一杯のスープになります。"
        )
        add(
            "血红素铁吸收率高，容易累的人可以定期吃",
            "The kind of iron the body takes up easily; worth eating regularly if you tire quickly",
            "ヘム鉄は吸収率が高いので、疲れやすい人は定期的にどうぞ。"
        )
        add(
            "逆着纹路切薄片，快炒到变色就起锅",
            "Slice thin against the grain and pull it off the heat as soon as it changes color",
            "繊維を断ち切るように薄く切り、色が変わるまで強火で炒めたら火を止めます。"
        )
        add(
            "一勺就能添点香味，锌和钙也顺带有一些",
            "A spoonful adds aroma, and some zinc and calcium with it",
            "スプーン一杯で香りが立ち、亜鉛とカルシウムも少し取れます。"
        )
        add(
            "买熟的、研磨过的更省事，撒在粥、酸奶或凉拌菜上",
            "Buy them toasted and ground; sprinkle over porridge, yogurt or a cold dish",
            "炒ってあるものやすりつぶしたものを買えば手間が省けます。お粥やヨーグルト、和え物にかけてどうぞ。"
        )
        add(
            "最下饭的家常组合，汤汁拌饭都行",
            "The homeliest combination there is; the juices are good over rice",
            "いちばんご飯が進む家庭の組み合わせ。汁をご飯に混ぜても合います。"
        )
        add(
            "番茄先下锅炒出沙，再倒入蛋液，刚凝固就出锅最嫩",
            "Cook the tomatoes down first, then add the egg and take it off the heat the moment it sets",
            "トマトを先に炒めて崩し、溶き卵を加え、固まり始めたらすぐ火から下ろすと柔らかく仕上がります。"
        )
        add(
            "豆腐配肉末，麻辣下饭还补蛋白",
            "Tofu with minced meat: spicy, easy over rice, and plenty of protein",
            "豆腐とひき肉。辛くてご飯が進み、たんぱく質も取れます。"
        )
        add(
            "嫩豆腐先焯水再烧不容易碎；豆瓣酱先炒出红油再下豆腐",
            "Blanch the soft tofu first so it holds; fry the chili bean paste until the oil reddens before adding it",
            "絹ごし豆腐は下ゆでしてから煮ると崩れにくくなります。豆板醤は先に炒めて赤い油を出してから豆腐を入れます。"
        )
        add("蒸的鱼油少，鲜味全在里面", "Steaming keeps the oil low and the flavor in", "蒸した魚は油が少なく、うま味が中に閉じ込められています。")
        add(
            "水开后上锅蒸 8 分钟，倒掉盘里的汁再淋热油和蒸鱼豉油",
            "Steam once the water boils, pour off the liquid, then finish with hot oil and seasoned soy sauce",
            "湯が沸いてから8分蒸し、皿の汁を捨ててから熱い油と蒸し魚しょうゆをかけます。"
        )
        add(
            "熬出米油的那种，胃里不舒服时尤其合适",
            "The kind that cooks down creamy; kind to a grumbly stomach",
            "米の油が出るまで炊いたもので、胃が落ち着かないときに特に向いています。"
        )
        add(
            "小米加水小火熬 25 分钟，关火焖一会儿更稠",
            "Simmer millet in water over low heat, then rest it off the heat so it thickens",
            "粟に水を加えて弱火で25分炊き、火を止めて少し置くととろみが増します。"
        )
        add("夏天常喝的那一碗，放凉加一点点冰糖", "The summer bowl, cooled with a little rock sugar", "夏によく飲まれる一杯。冷まして氷砂糖を少しだけ加えます。")
        add(
            "绿豆提前泡 2 小时，煮到刚开花就关火，别煮太久",
            "Soak the beans first and stop cooking as soon as they split open",
            "緑豆は2時間ほど水に浸し、割れてきたらすぐ火を止めます。煮すぎないでください。"
        )
        add("蒸着就很好吃，口感绵的", "Steamed it needs nothing else; it tastes soft and starchy", "蒸すだけでおいしく、ほくほくした食感です。")
        add(
            "去皮切段蒸 15 分钟，蘸白糖或者炖汤都行；处理时戴手套防手痒",
            "Peel, cut into lengths and steam; eat with sugar or drop into soup. Wear gloves while handling it",
            "皮をむいて切り、15分蒸します。砂糖をつけてもスープに入れてもいいです。触るときは手袋をするとかゆくなりません。"
        )
        add("凉拌热炒都行，口感脆", "Works dressed cold or stir-fried, and stays crisp", "和え物にも炒め物にも使えて、食感はシャキッとしています。")
        add(
            "干木耳冷水泡发 2 小时，焯水 2 分钟再拌，一定要煮熟",
            "Soak the dried kind in cold water, blanch it, and always cook it through",
            "乾燥きくらげは冷水で2時間戻し、2分下ゆでしてから和えます。必ず火を通してください。"
        )
        add("当零嘴或者泡水，甜甜的一颗", "A sweet little snack, or steeped in hot water", "おやつにも、お湯に浸しても。甘い一粒です。")
        add(
            "每天三四颗就够，泡水喝要把枣撕开",
            "A few a day is enough; tear them open before steeping",
            "一日三、四粒で十分です。お湯に浸すときは裂いてから。"
        )
        add("早餐摊的主角，自己在家也能打", "The breakfast-stall staple, and easy to make at home", "朝食屋台の主役。家でも作れます。")
        add(
            "黄豆提前泡一夜，豆浆一定要彻底煮沸再喝",
            "Soak the soybeans overnight and bring the milk to a full boil before drinking",
            "大豆は一晩水に浸します。豆乳は必ずしっかり沸騰させてから飲んでください。"
        )
        add(
            "菜肉主食一包齐，冻一盒随时煮",
            "Wrapper, vegetable, meat and staple in one; keep a box in the freezer",
            "野菜も肉も主食も包みの中に。一箱冷凍しておけばいつでもゆでられます。"
        )
        add(
            "水开下锅，点三次凉水，漂起来就熟了",
            "Drop into boiling water, add cold water when it returns to a boil, and they are done when they float",
            "熱湯に入れ、途中で冷水を三度加えます。浮いてきたら出来上がりです。"
        )
        add(
            "清汤光面，早上吃没负担",
            "Plain noodles in a clear broth; light enough for the morning",
            "澄んだスープの素うどん。朝に食べても重くなりません。"
        )
        add(
            "猪油和生抽打底，面煮好连汤冲进去，撒点葱花",
            "Start with lard and soy sauce in the bowl, pour the noodle water over, then scatter spring onion",
            "器にラードとしょうゆを入れておき、ゆでた麺を汁ごと注ぎ、ねぎを散らします。"
        )
        add("皮滑肉嫩，蘸姜蓉吃", "Silky skin, tender meat, eaten with a ginger dip", "皮はつるり、肉はやわらか。しょうがのたれで食べます。")
        add(
            "水开转小火浸熟，过一遍冰水鸡皮更爽脆",
            "Poach gently below a boil, then shock it in ice water for a springier skin",
            "沸いたら弱火にしてじっくり火を通し、氷水に取ると皮がしまります。"
        )
        add("绵稠咸香，早餐店的老朋友", "Thick, savory and familiar from any breakfast shop", "とろりと塩気のある、朝食店の定番です。")
        add(
            "米提前冷冻一晚再煮更绵；皮蛋最后拌进去",
            "Freezing the rice overnight makes it creamier; stir the egg in at the end",
            "米を一晩凍らせてから炊くととろみが増します。ピータンは最後に混ぜます。"
        )
        add("闻着上头吃着香，偶尔解馋可以", "Loud on the nose, good in the mouth; an occasional treat", "香りは強いけれど味はおいしい。たまの楽しみに。")
        add(
            "米粉冷水下锅煮软，酸笋最后放，味道轻一半",
            "Cook the noodles from cold water and add the pickled bamboo last to keep it milder",
            "米粉は冷水からゆでて柔らかくし、酸筍は最後に。匂いが半分になります。"
        )
        add(
            "闻着冲吃着香，爱的人很爱",
            "Sharp on the nose, good in the mouth; the people who love it really love it",
            "匂いは強いけれど味はおいしく、好きな人はとことんです。"
        )
        add(
            "空气炸锅180度10分钟，比油炸省事太多",
            "An air fryer does the job with far less oil",
            "エアフライヤーなら180度で10分間。揚げるよりずっと手軽です。"
        )
        add(
            "西南限定的味道，吃不惯的人很远就能闻到",
            "A taste you mostly find in the southwest; people who dislike it smell it from far away",
            "南西部でしか味わえない味。慣れない人には遠くからでも匂います。"
        )
        add(
            "凉拌加糊辣椒面和醋，是西南朋友的本命菜",
            "Dress it cold with toasted chili and vinegar, the way it is loved at home",
            "焦がし唐辛子と酢で和えるのが、南西部の人にとっての定番です。"
        )
        add(
            "能量放得慢，上午不容易突然发空",
            "Releases slowly, so the morning does not suddenly run out",
            "エネルギーの放出がゆっくりなので、午前に急に力尽きにくいです。"
        )
        add(
            "热水泡 3 分钟，加一勺酸奶或几颗坚果",
            "Soak in hot water, then add a spoon of yogurt or a few nuts",
            "お湯に3分浸し、ヨーグルトをスプーン一杯かナッツを数粒加えます。"
        )
        add("好脂肪和好蛋白一起来了", "Good fat and good protein in the same bite", "良い脂質と良いたんぱく質が一緒に取れます。")
        add("煎的时候皮朝下先煎，不用频繁翻面", "Start it skin side down and leave it alone", "皮を下にして焼き始め、何度も返さなくて大丈夫です。")
        add("不用做，开罐就能吃，价格也友好", "No cooking: open the tin and eat, and it is cheap", "調理は不要。缶を開ければ食べられて、値段も手ごろです。")
        add(
            "配面包或拌面都行，罐头里的汤别倒掉",
            "Good on bread or tossed through noodles; keep the liquid in the tin",
            "パンにのせても麺に混ぜても。缶の汁は捨てないでください。"
        )
        add("蛋白密度高，做法对了也不柴", "Dense in protein, and not dry if you treat it right", "たんぱく質がぎっしりで、火入れ次第でぱさつきません。")
        add(
            "提前用一点盐和油抓匀，冷藏半小时再煎",
            "Rub it with a little salt and oil and chill it before pan-frying",
            "少しの塩と油をもみ込み、冷蔵で30分置いてから焼きます。"
        )
        add("罐头装的直接能用，不用泡不用煮", "The tinned kind needs no soaking or boiling", "缶詰ならそのまま使えます。浸す必要もゆでる必要もありません。")
        add(
            "沥干冲一下，拌橄榄油和盐就是一份小菜",
            "Drain, rinse, and dress with olive oil and salt",
            "水気を切ってさっと洗い、オリーブオイルと塩で和えれば一品になります。"
        )
        add("脂肪质量好，饱腹感也强", "Good quality fat, and it keeps you full", "脂質の質がよく、満足感もあります。")
        add(
            "按一下蒂部，软了就是熟了；没软放两天",
            "Press the stem end: soft means ready, firm means wait a day or two",
            "へたを押してみて、やわらかければ食べごろ。固ければ二日置きます。"
        )
        add(
            "看得见麦麸的那种，比白吐司顶饱",
            "The kind with visible bran; more filling than white toast",
            "ふすまが見えるくらいのもので、白い食パンより腹持ちします。"
        )
        add(
            "烤一下抹无糖酸奶或者夹鸡蛋；别选配料表里有糖浆的",
            "Toast it and spread plain yogurt or add an egg; skip loaves with syrup on the label",
            "軽く焼いて無糖ヨーグルトを塗るか、卵を挟みます。原材料にシロップがあるものは避けてください。"
        )
        add("煮一把拌酱，十分钟开饭", "Boil a portion, stir in a sauce, and eat", "ひと掴みゆでてソースと和えれば、10分間で食事です。")
        add(
            "水开加盐煮 8 分钟，酱比面先好就拌，别过凉水",
            "Salt the water, cook it until just done, and toss with the sauce without rinsing",
            "塩を入れた湯で8分ゆで、ソースを先に用意しておいて和えます。冷水には取らないでください。"
        )
        add(
            "冰箱剩什么拌什么，颜色越多越好",
            "Whatever is left in the fridge works; more colors is better",
            "冷蔵庫に残っているものを何でも混ぜて。色が多いほどいいです。"
        )
        add(
            "生菜甩干水再拌，酱放边上蘸着吃比全拌进去省",
            "Dry the leaves well and keep the dressing on the side to use less",
            "水気をよく切ってから和えます。ソースは横に置いてつけながら食べると減ります。"
        )
        add("一块够顶一餐，铁和蛋白都扎实", "One piece makes a meal, with solid iron and protein", "一枚で一食分。鉄もたんぱく質もしっかり取れます。")
        add(
            "提前从冰箱拿出来回温，大火每面一分半，醒肉三分钟再切",
            "Let it come to room temperature, sear hard on both sides, rest it, then slice",
            "冷蔵庫から出して室温に戻し、強火で片面一分半ずつ。三分钟休ませてから切ります。"
        )
        add("绵软好入口，当主食或者配菜都行", "Soft and easy to eat, as a staple or a side", "やわらかく食べやすいので、主食にも付け合わせにもなります。")
        add(
            "蒸熟压泥加一点点牛奶和盐，比炒土豆省油",
            "Steam and mash it with a splash of milk and salt; far less oil than frying",
            "蒸してつぶし、牛乳と塩を少し加えます。炒めるより油を使いません。"
        )
        add(
            "看着网红，做起来确实省事，好脂肪也有了",
            "Fashionable, but genuinely quick, and the good fat comes along",
            "見た目は流行りものですが、作るのは本当に簡単で、良い脂質も取れます。"
        )
        add(
            "牛油果压碎加几滴柠檬汁防氧化，抹在全麦面包上",
            "Mash the avocado with a squeeze of lemon and spread it on whole-grain bread",
            "アボカドをつぶしてレモン汁を数滴加え、全粒粉パンに塗ります。"
        )
        add(
            "一张饼把主食、蛋白和蔬菜都带上，周末做起来也不费劲",
            "One round carries a staple, protein and vegetables; not much work on a weekend",
            "一枚に主食もたんぱく質も野菜も乗ります。週末でも手間はかかりません。"
        )
        add(
            "面饼抹番茄酱，撒芝士和配料，烤箱 220 度烤一刻钟",
            "Spread tomato sauce over the base, add cheese and toppings, and bake until golden",
            "生地にトマトソースを塗り、チーズと具をのせ、オーブン220度で15分焼きます。"
        )
        add(
            "两片面包夹一切，早餐和带饭都合适",
            "Two slices hold anything; good for breakfast or a packed lunch",
            "二枚のパンで何でも挟めます。朝食にもお弁当にも。"
        )
        add(
            "全麦面包夹鸡蛋和生菜，酱薄薄一层就够",
            "Whole-grain bread with egg and lettuce needs only a thin layer of dressing",
            "全粒粉パンに卵とレタスを挟み、ソースは薄く一層で十分です。"
        )
        add(
            "比鸡胸多汁，腌一下进烤箱就不用管了",
            "Juicier than breast; marinate it and let the oven do the rest",
            "鶏むねよりしっとり。漬け込んでオーブンに入れたら放っておけます。"
        )
        add(
            "用盐、黑胡椒和蒜腌一夜，烤箱 200 度烤半小时",
            "Marinate overnight with salt, black pepper and garlic, then roast it",
            "塩、黒こしょう、にんにくで一晩漬け、オーブン200度で30分焼きます。"
        )
        add(
            "把吃不完的蔬菜煮一锅，暖胃也不浪费",
            "A pot of whatever vegetables are left: warming, and nothing wasted",
            "余った野菜を一鍋に。体が温まり、無駄にもなりません。"
        )
        add(
            "蔬菜炒软加水煮烂，料理棒打碎后加点盐和胡椒",
            "Soften the vegetables, simmer them in water, blend, then season with salt and pepper",
            "野菜を炒めて水を加えて煮崩し、ブレンダーで撹拌し、塩とこしょうで調えます。"
        )
        add(
            "芝士盖满的comfort food，偶尔吃一顿很满足",
            "Comfort food under a blanket of cheese; satisfying now and then",
            "チーズをたっぷりかけた comfort food。たまに食べると満たされます。"
        )
        add(
            "通心粉煮到八成熟，拌白酱铺芝士，烤到表面金黄",
            "Cook the macaroni partway, fold in a white sauce, cover with cheese and bake until golden",
            "マカロニを少し固めにゆで、ホワイトソースで和えてチーズをのせ、表面に焼き色がつくまで焼きます。"
        )
        add("发酵食品，肠道菌群喜欢的口味", "Fermented food, the flavor gut bacteria like", "発酵食品。腸内細菌が好む味です。")
        add(
            "味噌别用滚水冲，会杀掉活菌，温的就行",
            "Do not pour boiling water over miso; warm water keeps the cultures alive",
            "味噌は熱湯で溶かないでください。菌が死んでしまいます。ぬるま湯で十分です。"
        )
        add("一碗热汤下肚，晚餐收尾正好", "A warm bowl to close out dinner", "温かい一杯で、夕食の締めにちょうどいいです。")
        add(
            "水开关火再化味噌，别用滚水煮，保住里面的活菌",
            "Turn off the heat before dissolving the miso so the cultures survive",
            "火を止めてから味噌を溶きます。煮立てずに、菌を生かしてください。"
        )
        add("凉吃热吃都行，口感比普通面条韧", "Good hot or cold, with more bite than regular noodles", "冷たくても温かくても。普通の麺より粘りがあります。")
        add(
            "煮好过一遍凉水更劲道；蘸汁别太咸",
            "Rinse it in cold water for a firmer bite, and keep the dipping sauce light",
            "ゆでたあと冷水に取ると締まります。つけ汁は塩辛くしないでください。"
        )
        add(
            "好脂肪的好来源，冰一下直接吃，什么都不用做",
            "A good source of good fat; chill it and eat, no cooking at all",
            "良い脂質の良い源。冷やしてそのまま、何もしなくていいです。"
        )
        add(
            "买可生食级的，现切现吃；肠胃敏感的日子换成煎熟的",
            "Buy sashimi grade and eat it freshly cut; on a sensitive day have it cooked instead",
            "生食用を買って、切ってすぐ食べます。胃腸が敏感な日は焼いたものに。"
        )
        add(
            "一口一个，主食和蛋白都有了，买现成的也体面",
            "One bite holds a staple and protein, and bought ready-made is fine too",
            "一口で主食とたんぱく質。買ってきたものでもきちんとしています。"
        )
        add(
            "米饭别压太实，醋饭晾到温热再卷",
            "Do not pack the rice too tight, and roll it once the seasoned rice is lukewarm",
            "ご飯は詰めすぎず、酢飯は人肌まで冷ましてから巻きます。"
        )
        add(
            "一碗热汤面下肚，整个人都松下来了",
            "A hot bowl of noodles and broth, and the whole body loosens",
            "温かい麺を一杯食べると、全身がゆるみます。"
        )
        add(
            "汤头别用全调料包，减半加点青菜和蛋",
            "Use half the seasoning packet and add greens and an egg",
            "スープは付属のたれを半分にし、青菜と卵を足します。"
        )
        add("酱汁甜咸口，配米饭正好", "A sweet-salty glaze that sits well over rice", "甘辛いたれで、ご飯によく合います。")
        add(
            "鸡腿肉煎到两面金黄，倒照烧汁收到挂住肉",
            "Pan-fry the thigh until golden, then reduce the teriyaki sauce until it clings",
            "鶏ももを両面こんがり焼き、照り焼きだれを入れて絡むまで煮詰めます。"
        )
        add("滑嫩的热蛋羹，胃里不舒服时也能吃", "A warm, silky custard that goes down easily", "なめらかな温かい茶碗蒸し。胃が落ち着かないときにも食べられます。")
        add(
            "蛋液加高汤和一点盐，盖保鲜膜扎孔，中火蒸到刚凝固",
            "Mix the egg with stock and a pinch of salt, cover it, and steam gently until just set",
            "溶き卵に出汁と少しの塩を加え、ラップをかけて穴をあけ、中火で固まるまで蒸します。"
        )
    }

    // ------------------------------------------------------------------
    // 食谱：三餐搭配标题与提示
    // ------------------------------------------------------------------
    init {
        add("早上这样搭", "A morning plate like this", "朝はこんな組み合わせ", "Mane sic")
        add("中午这样搭", "A midday plate like this", "昼はこんな組み合わせ", "Meridie sic")
        add("晚上这样搭", "An evening plate like this", "夜はこんな組み合わせ", "Vesperi sic")
        add(
            "早上不用吃多，但要吃点有蛋白的。",
            "You do not need much in the morning, but get some protein in.",
            "朝は多くなくていいですが、たんぱく質は少し取りましょう。",
            "Mane multum non opus est, sed proteinum adde."
        )
        add(
            "中午吃饱一点没关系，下午不容易找零食。",
            "Eating properly at noon makes the afternoon snacking less likely.",
            "昼はしっかり食べても大丈夫。午後に間食を探さずに済みます。",
            "Meridie satis edere licet; post meridiem minus cibos quaeres."
        )
        add(
            "晚上离睡觉留出两小时，睡得会踏实些。",
            "Leave a couple of hours between dinner and bed; sleep comes easier.",
            "夜は就寝の二時間前に終えると、よく眠れます。",
            "Vesperi duas horas ante somnum relinque: melius dormies."
        )
    }

    // ------------------------------------------------------------------
    // 微行动：分类、名称、分步引导
    // ------------------------------------------------------------------
    init {
        add("呼吸", "Breathing", "呼吸", "Respiratio")
        add("活动", "Movement", "運動", "Motus")
        add("饮食", "Eating", "食事", "Cibus")
        add("休息", "Rest", "休息", "Quies")
        add("光照", "Light", "光", "Lux")
        add("连接", "Connection", "つながり", "Connexio")

        add("4-5-6 呼吸", "4-5-6 breathing", "4-5-6 呼吸", "Respiratio 4-5-6")
        add("窗边换气", "Breathe by a window", "窓辺で深呼吸", "Respira ad fenestram")
        add("长呼气", "Long exhale", "長い呼気", "Exhalatio longa")
        add("站起来走 20 步", "Stand and walk 20 steps", "立って20歩歩く", "Surge et ambula 20 passus")
        add("转肩松颈", "Relax shoulders and neck", "肩と首をゆるめる", "Umeros et collum relaxa")
        add("出门走一小段", "Take a short walk", "少し外を歩く", "Paululum foras ambula")
        add("坐姿抬腿", "Seated leg raise", "座ったまま脚上げ", "Crus sedens tolle")
        add("桌边肩颈解冻", "Desk neck reset", "デスクで肩首リセット", "Ad mensam umeros relaxa")
        add("办公室隐形拉伸", "Quiet office stretch", "オフィスで目立たないストレッチ", "Extende clam in officio")
        add("情绪降噪散步", "Quieting walk", "気持ちを静める散歩", "Ambula ad animum sedandum")
        add("喝一杯温水", "Drink warm water", "温かい水を一杯", "Bibe aquam tepidam")
        add("一小把坚果", "A small handful of nuts", "ナッツをひとつかみ", "Pugillus nucum")
        add("一份水果", "Have some fruit", "果物を一つ", "Fructus unus")
        add("这顿慢一点吃", "Eat this meal slowly", "この食事はゆっくり", "Hoc cibo lente vescere")
        add("喝三口温水", "Three sips of warm water", "温かい水を三口", "Tres haustus aquae tepidae")
        add("让眼睛歇 20 秒", "Rest your eyes for 20 seconds", "目を20秒休める", "Oculos 20 secundis quieta")
        add("闭眼 3 分钟", "Close your eyes for 3 minutes", "3分間目を閉じる", "Claude oculos 3 minuta")
        add("睡前半小时关屏", "Screen off before bed", "就寝30分前に画面を消す", "Ante somnum tegumentum claude")
        add("平躺放松腰", "Lie down and relax your back", "仰向けで腰をゆるめる", "Resupinus lumbos relaxa")
        add("窗边远眺 20 秒", "Look into the distance", "窓辺で20秒遠くを見る", "Procul aspice 20 secunda")
        add("睡前身体扫描", "Bedtime body scan", "寝る前のボディスキャン", "Perlustratio corporis ante somnum")
        add("去有光的地方待 5 分钟", "Spend 5 minutes in daylight", "光の当たる場所で5分", "In luce 5 minuta mane")
        add("把灯光调暗一档", "Dim the lights", "照明を一段暗くする", "Lumen obscura")
        add("阳台光合作用", "A little balcony light", "ベランダで光を浴びる", "In maeniano lucem cape")
        add("给一个人发条消息", "Message someone", "誰かにメッセージを送る", "Uni homini scribe")
        add("脚踩地面 2 分钟", "Feet on the floor", "足の裏を床につけて2分", "Pedes in solo 2 minuta")
        add("给现在的感觉起个名字", "Name the feeling", "今の感覚に名前をつける", "Sensui nomen da")

        add("找个能坐下的地方，背靠着椅背。", "Find somewhere to sit and lean back against the chair.", "座れる場所を見つけて、背もたれに寄りかかります。")
        add("吸气 4 秒，不用吸满。", "Breathe in for 4 seconds; no need to fill up.", "4秒かけて吸います。いっぱいまで吸わなくて大丈夫です。")
        add("停 5 秒，肩膀放下来。", "Hold for 5 seconds and let your shoulders drop.", "5秒止めて、肩を下ろします。")
        add(
            "呼气 6 秒，比吸气长一点。",
            "Breathe out for 6 seconds, a little longer than the breath in.",
            "6秒かけて吐きます。吸うより少し長めに。"
        )
        add(
            "重复 6 轮，做完就结束，不用加量。",
            "Repeat 6 rounds and stop there; no need to add more.",
            "6セット繰り返して終わりです。回数を増やさなくて大丈夫です。"
        )
        add("走到窗边，把窗开一条缝。", "Go to the window and open it a crack.", "窓辺まで行き、窓を少しだけ開けます。")
        add(
            "慢慢吸 5 次，每次都想把空气吸到肚子。",
            "Take 5 slow breaths, each one down toward your belly.",
            "ゆっくり5回吸います。毎回、おなかまで空気を入れるつもりで。"
        )
        add("再慢慢呼 5 次，肩膀跟着往下沉。", "Then 5 slow exhales, letting your shoulders sink.", "そのあとゆっくり5回吐きます。肩も一緒に下がります。")
        add("鼻子吸气 3 秒。", "Breathe in through your nose for 3 seconds.", "鼻から3秒吸います。")
        add(
            "嘴微微张开，呼气 8 秒，像吹凉一碗汤。",
            "Let your mouth fall slightly open and breathe out for 8 seconds, as if cooling a bowl of soup.",
            "口を少し開けて8秒吐きます。スープを冷ますように。"
        )
        add("做 5 轮，中途想停就停。", "Do 5 rounds; stop whenever you like.", "5セット行います。途中でやめたくなったら止めて大丈夫です。")
        add("从椅子上站起来。", "Stand up from the chair.", "椅子から立ち上がります。")
        add(
            "走到房间另一头再走回来，大概 20 步。",
            "Walk to the other end of the room and back, about 20 steps.",
            "部屋の反対側まで歩いて戻ります。だいたい20歩です。"
        )
        add("回来的时候顺便把肩膀转两圈。", "Roll your shoulders a couple of times on the way back.", "戻りながら肩を二回まわします。")
        add("坐直，双手自然放在腿上。", "Sit up and rest your hands on your thighs.", "背すじを伸ばして座り、両手を自然に太ももに置きます。")
        add(
            "肩膀往上耸到耳朵，停 2 秒，放下。",
            "Lift your shoulders toward your ears, hold for 2 seconds, then drop them.",
            "肩を耳まで上げ、2秒止めてから下ろします。"
        )
        add(
            "重复 8 次，然后慢慢左右转头各 5 次。",
            "Repeat 8 times, then turn your head slowly left and right, 5 times each.",
            "8回繰り返し、そのあとゆっくり左右に首を5回ずつ回します。"
        )
        add(
            "不用换运动服，穿鞋出门就行。",
            "No need to change; just put your shoes on and step out.",
            "運動着に着替えなくて大丈夫です。靴を履いて出るだけ。"
        )
        add("走 5 分钟，不追求速度。", "Walk for 5 minutes; speed is not the point.", "5分歩きます。速さは求めません。")
        add(
            "转身走回来，路上看看天。",
            "Turn around and walk back, looking up at the sky on the way.",
            "引き返します。帰り道で空を見てみてください。"
        )
        add("坐在椅子前三分之一处。", "Sit on the front third of the chair.", "椅子の前三分の一に座ります。")
        add(
            "一条腿伸直抬起，停 5 秒，放下。",
            "Straighten and lift one leg, hold for 5 seconds, then lower it.",
            "片脚を伸ばして上げ、5秒止めてから下ろします。"
        )
        add(
            "左右各 6 次，做的时候腰别塌。",
            "6 times on each side; keep your lower back from sagging.",
            "左右6回ずつ。腰が反らないようにしてください。"
        )
        add("坐直，双脚踩实地面。", "Sit up and plant both feet on the floor.", "背すじを伸ばし、両足を床にしっかりつけます。")
        add("肩膀往上耸，停一下，再放下来。", "Lift your shoulders, pause, then let them down.", "肩を上げて少し止め、それから下ろします。")
        add(
            "慢慢左右转头，各转两圈就停。",
            "Turn your head slowly left and right, two rounds each, then stop.",
            "ゆっくり左右に首を回し、それぞれ二回で終わりです。"
        )
        add("坐着就能做，别人看不出来。", "You can do this sitting down without anyone noticing.", "座ったままできて、周りに気づかれません。")
        add(
            "双手在背后扣住，胸口往前打开。",
            "Clasp your hands behind your back and open your chest forward.",
            "背後で手を組み、胸を前に開きます。"
        )
        add("保持几个呼吸，然后松开。", "Hold for a few breaths, then let go.", "数回呼吸するあいだ保ち、それからほどきます。")
        add("再来一次，肩膀跟着往下沉。", "Once more, letting your shoulders drop with it.", "もう一度。肩も一緒に下がります。")
        add("不用换衣服，出门就行。", "No need to change clothes; just head out.", "着替えなくて大丈夫です。そのまま出てください。")
        add("走得不快不慢，能正常说话的速度。", "Walk at a pace where you can still talk normally.", "早くも遅くもなく、普通に話せる速さで歩きます。")
        add(
            "把注意力放在脚底和呼吸上，不用想事。",
            "Keep your attention on your feet and your breathing; no thinking needed.",
            "足の裏と呼吸に意識を向けます。考えごとはしなくて大丈夫です。"
        )
        add("走五分钟，想停就停。", "Walk for five minutes and stop whenever you want.", "5分歩きます。やめたくなったら止めて大丈夫です。")
        add("倒一杯温水，别太烫。", "Pour a glass of warm water, not too hot.", "温かい水を一杯用意します。熱すぎないように。")
        add("分几口喝完，不要一口闷。", "Drink it in sips rather than in one go.", "何口かに分けて飲みます。一気に飲まないでください。")
        add(
            "喝的时候就专心喝，不用一边做别的事。",
            "Just drink while you drink; nothing else needs doing.",
            "飲むときは飲むことだけに集中します。ながらでなくて大丈夫です。"
        )
        add("抓一小把，大概盖住手心就够。", "Take a small handful, enough to cover your palm.", "ひとつかみ取ります。手のひらが覆われるくらいで十分です。")
        add("慢慢嚼，别一边刷手机一边吃。", "Chew slowly; no scrolling while you eat.", "ゆっくり噛みます。スマホを見ながら食べないでください。")
        add("嚼出味道再咽，吃完就停。", "Swallow once you taste it, and stop when it is gone.", "味を感じてから飲み込みます。食べ終わったら終わりです。")
        add("挑手边最容易拿到的水果。", "Pick whatever fruit is closest to hand.", "手近でいちばん取りやすい果物を選びます。")
        add("慢慢吃，尝到味道就行。", "Eat it slowly; tasting it is enough.", "ゆっくり食べます。味がわかれば十分です。")
        add(
            "吃完就好，不用凑够「每天五种」。",
            "When it is finished, that is enough; no quota to hit.",
            "食べ終わればそれで十分です。「一日五種類」を目指さなくて大丈夫です。"
        )
        add("把手机放到看不见的地方。", "Put your phone somewhere out of sight.", "スマホを視界に入らない場所に置きます。")
        add("每口多嚼几下再咽。", "Chew each mouthful a few more times before swallowing.", "一口ごとに何回か多く噛んでから飲み込みます。")
        add(
            "吃到七分饱就停，剩下的留着下一顿。",
            "Stop while you are still comfortable and keep the rest for later.",
            "腹八分で止め、残りは次の食事に取っておきます。"
        )
        add("倒一杯温水，放在手边。", "Pour a glass of warm water and keep it within reach.", "温かい水を一杯、手の届くところに置きます。")
        add("慢慢喝三口，每一口都停一下。", "Take three slow sips, pausing after each one.", "ゆっくり三口飲みます。一口ごとに少し止めます。")
        add("剩下的想喝再喝。", "Drink the rest only if you want it.", "残りは飲みたくなったら飲みます。")
        add("把视线从屏幕上移开。", "Look away from the screen.", "画面から視線を外します。")
        add(
            "看窗外最远的东西，20 秒。",
            "Find the furthest thing outside and watch it for 20 seconds.",
            "窓の外のいちばん遠いものを見ます。20秒間。"
        )
        add("顺便眨几下眼睛。", "Blink a few times while you are at it.", "ついでに何度かまばたきします。")
        add("放下手机，靠着椅背。", "Put the phone down and lean back.", "スマホを置いて、背もたれに寄りかかります。")
        add("闭眼，什么都不用想。", "Close your eyes; nothing needs thinking about.", "目を閉じます。何も考えなくて大丈夫です。")
        add(
            "3 分钟后自然睁开，不用定闹钟。",
            "Open them naturally in about 3 minutes; no alarm needed.",
            "3分たったら自然に開けます。アラームは要りません。"
        )
        add("把手机放到床头够不到的地方。", "Put the phone out of reach of the bed.", "スマホをベッドから手の届かない場所に置きます。")
        add(
            "去做一件不用屏幕的事：洗漱、叠衣服、发呆都行。",
            "Do something that needs no screen: wash up, fold clothes, stare at the ceiling.",
            "画面を使わないことを一つします。洗面、洗濯物たたみ、ぼんやりするだけでもいいです。"
        )
        add(
            "如果又想去拿手机，先坐一会儿再决定。",
            "If you reach for the phone again, sit for a moment before deciding.",
            "またスマホに手が伸びそうになったら、少し座ってから決めます。"
        )
        add("找一块平地或硬一点的床面躺下。", "Lie down on the floor or a firmer bed.", "平らな床か、少し硬めのベッドに横になります。")
        add("膝盖弯起来，脚踩地。", "Bend your knees and put your feet on the floor.", "膝を曲げて、足の裏を床につけます。")
        add(
            "躺 5 分钟，感受腰贴到地面。",
            "Stay for 5 minutes and feel your lower back meet the floor.",
            "5分横になります。腰が床につく感覚を確かめます。"
        )
        add("走到窗边，或者把头转向窗外的方向。", "Go to the window, or simply turn your head toward it.", "窓辺まで行くか、窓のほうへ顔を向けます。")
        add(
            "找一个最远的东西看住，眼睛别用力。",
            "Fix on the furthest thing you can find; keep your eyes soft.",
            "いちばん遠いものを見つめて、目に力を入れないようにします。"
        )
        add("看够 20 秒再收回来。", "Hold it for 20 seconds, then look back.", "20秒たってから視線を戻します。")
        add("躺下或靠好，手放在肚子上。", "Lie down or settle back, hands on your belly.", "横になるか寄りかかり、手をおなかに置きます。")
        add(
            "把注意力放到脚，慢慢往上走。",
            "Put your attention on your feet and move it slowly upward.",
            "意識を足に置き、ゆっくり上へ移していきます。"
        )
        add(
            "走到哪里松到哪里，不用刻意。",
            "Wherever it arrives, let that part soften; nothing forced.",
            "移ったところをゆるめていきます。意識して力を抜かなくて大丈夫です。"
        )
        add(
            "走完一遍就结束，睡着了也没关系。",
            "One pass is the whole thing, and falling asleep is fine.",
            "一周したら終わりです。眠ってしまっても大丈夫です。"
        )
        add("走到窗边或阳台，不用直晒。", "Step to a window or balcony; no need for direct sun.", "窓辺かベランダに出ます。直射日光でなくて大丈夫です。")
        add(
            "待 5 分钟，光会告诉身体现在是白天。",
            "Stay for 5 minutes; light tells the body it is daytime.",
            "5分過ごします。光が体に「今は昼」と伝えてくれます。"
        )
        add(
            "站着或者坐着都行，不用顺便做别的事。",
            "Standing or sitting both work; nothing else needs doing.",
            "立っていても座っていても大丈夫です。ついでに何かしなくていいです。"
        )
        add("关掉最亮的那盏灯。", "Turn off the brightest lamp.", "いちばん明るい灯りを消します。")
        add("只留一盏暖色的。", "Leave one warm light on.", "暖色の灯りを一つだけ残します。")
        add("让房间提前进入夜晚。", "Let the room turn into evening early.", "部屋を少し早めに夜の状態にします。")
        add(
            "走到阳台或窗边，不用直晒。",
            "Step onto the balcony or to the window; no need for direct sun.",
            "ベランダか窓辺に出ます。直射日光でなくて大丈夫です。"
        )
        add(
            "把脸朝向亮的地方，让光落在眼睛上。",
            "Turn your face toward the light and let it fall on your eyes.",
            "明るいほうへ顔を向け、光を目に入れます。"
        )
        add("站两分钟，什么都不用做。", "Stand for two minutes and do nothing else.", "二分立ちます。何もしなくて大丈夫です。")
        add("想一个最近没联系的人。", "Think of someone you have not talked to lately.", "最近連絡していない人を一人思い浮かべます。")
        add("发一句话就够，不用找话题。", "One line is enough; no topic needed.", "一言送れば十分です。話題を探さなくていいです。")
        add(
            "发完就把手机放下，不用等回复。",
            "Put the phone down right after; no need to wait for a reply.",
            "送ったらすぐスマホを置きます。返事を待たなくていいです。"
        )
        add("脱掉鞋，脚掌完整地踩在地板上。", "Take your shoes off and let your whole foot meet the floor.", "靴を脱いで、足の裏全体を床につけます。")
        add("感受地面的温度和硬度。", "Notice the temperature and hardness of the ground.", "床の温度と硬さを感じます。")
        add("2 分钟，什么都不用做。", "Two minutes, nothing else to do.", "二分間、何もしなくて大丈夫です。")
        add(
            "不用写下来，在心里想一个词。",
            "No need to write it down; find one word in your head.",
            "書き出さなくて大丈夫です。心の中で一つの言葉を探します。"
        )
        add(
            "想不出来也没关系，「说不清」就是一个名字。",
            "If nothing comes, that is fine; “not sure” is a name too.",
            "思い浮かばなくても大丈夫です。「うまく言えない」も一つの名前です。"
        )
        add(
            "起完名字就去做手边的事，不用分析它。",
            "Once it has a name, go back to what you were doing; no analysis needed.",
            "名前をつけたら手元のことに戻ります。分析しなくていいです。"
        )

        add(
            "按上次说的「更累了」，这次排得更短更温和。",
            "Last time you said you were more tired, so this one is shorter and gentler.",
            "前回の「疲れた」に合わせて、今回は短くやさしくしました。",
            "Quia \"lassior\" dixisti, hoc brevius et mitius est."
        )
        add(
            "按上次说的「没感觉」，这次稍微放开了一点。",
            "Last time you felt no change, so this one opens up a little.",
            "前回の「変わらない」に合わせて、少しだけ広げました。",
            "Quia \"nihil mutatum\" dixisti, hoc paulo latius est."
        )
        add(
            "上次做完说变轻了，这次保持。",
            "Last time you felt lighter afterwards, so this one stays as it was.",
            "前回は軽くなったということだったので、そのままにしました。",
            "Ultima vice levior te sensisti: hoc idem manet."
        )
        add(
            "上次说想再来一次，这次换一张同强度的。",
            "Last time you wanted another round, so here is a different one at the same level.",
            "前回はもう一度したいということだったので、同じ強さで別のものを用意しました。",
            "Ultima vice iterum voluisti: hoc aliud, eiusdem roboris."
        )

        add(
            "云在慢慢散，不用急着赶路。",
            "The clouds are clearing. There is no need to hurry.",
            "雲はゆっくり晴れていく。急がなくていい。",
            "Nubes lente discedunt. Noli festinare."
        )
        add(
            "阳光正好，可以往外走一点。",
            "The light is good; you can step a little further out.",
            "日差しがちょうどいい。少し外へ出てもいい。",
            "Lux bona est. Paulum foras ire potes."
        )
        add(
            "风有点大，先找个避风的地方。",
            "The wind is up; find somewhere sheltered first.",
            "風が強い。まずは落ち着ける場所へ。",
            "Ventus fortis est. Primum locum tutum quaere."
        )
        add("今天的身体，是稳的。", "Today the body is steady.", "今日は体が安定している。", "Corpus hodie stabile est.")
        add(
            "身体在说话，先听它说完。",
            "The body is talking; let it finish.",
            "体の声を、最後まで聞いてみる。",
            "Corpus loquitur. Audi usque ad finem."
        )
    }

    // ------------------------------------------------------------------
    // 回声：日历、来信、墙、分享卡
    // ------------------------------------------------------------------
    init {
        add("一", "Mon", "月")
        add("二", "Tue", "火")
        add("三", "Wed", "水")
        add("四", "Thu", "木")
        add("五", "Fri", "金")
        add("六", "Sat", "土")
        add("日", "Sun", "日")

        add(
            "这一周还没有留下天气记录。",
            "No weather was recorded this week.",
            "今週はまだ体調の記録がありません。",
            "Hac septimana nulla caeli nota relicta est."
        )
        add(
            "什么时候想记，再来就好。",
            "Come back whenever you feel like writing one.",
            "記録したくなったら、また来てください。",
            "Cum libebit, redi."
        )
        add("这周还没有回声", "No echo this week yet", "今週はまだ記録がありません", "Nondum echo hac septimana")
        add(
            "你有一段日子在收着过。这不叫落后。",
            "There were days you kept things small. That is not falling behind.",
            "小さくまとめて過ごした日があった。それは遅れではない。"
        )
        add(
            "有几天你是有余量的，希望那几天被用在了想做的事上。",
            "Some days left you something over; I hope it went to what you wanted to do.",
            "余裕のあった日がいくつかあった。その日が、やりたかったことに使われていたらいい。"
        )
        add(
            "紧的时候比较多。你已经处理得比你以为的好。",
            "There was more tightness than ease. You handled it better than you think.",
            "張りつめていた時間が多めだった。自分が思うより、うまく扱えている。"
        )
        add(
            "大部分时候是稳的。稳这件事，本身就不容易。",
            "Most of the time it was steady. Steady is not a small thing.",
            "ほとんどは安定していた。安定していること自体、簡単ではない。"
        )
        add(
            "有几天你选择了听身体的。那需要一点勇气。",
            "On some days you chose to listen to your body. That takes a little courage.",
            "体の声を聞くほうを選んだ日があった。それには少し勇気がいる。"
        )
        add(
            "天气换了好几种，这很常见，不用从中找规律。",
            "The weather changed several times. That is common; there is no pattern to find.",
            "体調は何度も入れ替わった。よくあることで、そこに法則を探さなくていい。"
        )
        add(
            "天气几乎没变。不变也是信息。",
            "The weather barely changed. Not changing is information too.",
            "体調はほとんど変わらなかった。変わらないことも情報になる。"
        )
        add(
            "有起伏，但都在你能走过去的范围里。",
            "There were ups and downs, all within what you can walk through.",
            "起伏はあったが、どれも歩いて越えられる範囲にある。"
        )
        add("这周的身体来信", "This week's body letter", "今週の体からの手紙", "Epistula corporis huius septimanae")
        add("BodyCast", "BodyCast", "BodyCast")
        add("这周还没有足够记录", "Not enough recorded this week", "今週はまだ記録が足りません", "Nondum satis notarum hac septimana")
        add("一周里基本没变", "Barely changed all week", "一週間ほぼ変わらず", "Per septimanam paene idem")
        add("中间有起伏", "Ups and downs along the way", "途中に起伏あり", "In medio fluctus")

        add("我的身体天气", "My body weather", "わたしの体調", "Caelum corporis meum")
        add(
            "BodyCast · 不评分，只播报",
            "BodyCast · Reports, never scores",
            "BodyCast · 点数はつけず、伝えるだけ",
            "BodyCast · Refert, non punctat"
        )

        add(
            "这一周，身体经历过什么。它不评价，只是回放。",
            "What your body went through this week. It does not judge, it only replays.",
            "今週、体に何があったか。評価はせず、ただ再生するだけです。",
            "Quid corpus hac septimana passum sit. Non iudicat, tantum repetit."
        )
        add(
            "数据源、校准、提醒、外观，都在设置页。",
            "Data sources, calibration, reminders and appearance are all in Settings.",
            "データ元、調整、リマインダー、表示は、設定ページにあります。",
            "Fontes, calibratio, admonitiones, species: omnia in Optionibus."
        )
        add(
            "身体回声只存在这台手机上。它不排名，也不和任何人比较。",
            "Your body echo lives only on this phone. It ranks nothing and compares you to no one.",
            "体の記録はこの端末の中だけにあります。順位もつけず、誰とも比べません。",
            "Echo corporis in hoc instrumento manet. Nihil ordinat, neminem comparat."
        )
        add(
            "诗是本地生成的，每天只换一次。刷新页面它不会变——这样它才像今天的东西。",
            "The poem is generated on this phone and changes once a day. Refreshing will not change it, which is what makes it feel like today's.",
            "詩は端末内で生成され、一日に一度だけ変わります。画面を更新しても変わらないからこそ、今日のものらしくなります。",
            "Carmen in hoc instrumento creatur et semel in die mutatur. Pagina renovata id non mutat: sic hodiernum videtur."
        )
        add(
            "还没有足够的记录，攒两天就能看到趋势了。",
            "Not enough recorded yet; a couple more days will show a trend.",
            "まだ記録が足りません。二日ほど貯まると傾向が見えてきます。",
            "Notae nondum satis; post duos dies inclinatio apparebit."
        )
        add(
            "这里只有词和方向，没有数值坐标轴。一旦有刻度，人就会开始追求更高的那条线。",
            "Only words and directions here, no numbered axis. Once there is a scale, people start chasing the higher line.",
            "ここにあるのは言葉と方向だけで、数値の軸はありません。目盛りがあれば、人は高いほうの線を追い始めます。",
            "Hic verba et directiones tantum sunt, nullus axis numerorum. Cum scala est, homines altiorem lineam petunt."
        )
        add(
            "直接截屏就能发出去。卡片上不会出现任何数值、体重或热量。",
            "Just take a screenshot to share it. No numbers, weight or calories appear on the card.",
            "そのままスクリーンショットで送れます。カードに数値も体重もカロリーも出ません。",
            "Imaginem capere satis est. In tabella nulli numeri, nullum pondus, nullae caloríae."
        )
    }

    // ------------------------------------------------------------------
    // 今天的播报与「为什么这样播报」
    // ------------------------------------------------------------------
    init {
        add(
            "云还没散完。今天不用把日程填满，能推掉的就推掉一件。",
            "The clouds have not finished clearing. No need to fill the day; push one thing off if you can.",
            "雲はまだ晴れきっていません。予定を埋めず、一つ延期しても大丈夫です。",
            "Nubes nondum discesserunt. Diem ne impleas; unam rem differ si potes."
        )
        add(
            "身体有余量。趁这个窗口做点一直想做的事，但别一次用光。",
            "Your body has something in reserve. Use the window for something you have been wanting to do, but do not spend it all at once.",
            "体に余裕があります。やりたかったことを少し。ただし使い切らないで。",
            "Vires supersunt. Hoc tempore fac quod diu voluisti, sed ne omnia simul consumas."
        )
        add(
            "风比平时大。今天不适合做重大决定，先处理手头最简单的。",
            "The wind is stronger than usual. Not a day for big decisions; start with the simplest thing at hand.",
            "いつもより風が強い日です。大きな判断は避け、簡単なことから。",
            "Ventus solito fortior. Hodie magna decernere non expedit; a simplicissimo incipe."
        )
        add(
            "没有特别要提醒的。按你原本的节奏来就好。",
            "Nothing in particular to flag. Your usual pace is fine.",
            "特に注意することはありません。いつものペースで。",
            "Nihil praecipue monendum est. Ritmo tuo utere."
        )
        add(
            "设备读到的和你的感受不完全一致。以你说的为准。",
            "What the device reads and what you feel do not fully agree. What you say goes.",
            "測定値と自分の感覚が一致していません。自分の感覚を優先します。",
            "Mensurae et sensus tuus non plene congruunt. Quod dicis praecedit."
        )
        add("恢复指标比平时好", "Recovery is better than usual", "回復の状態は普段より良好", "Recuperatio solito melior")
        add("恢复指标在常见范围", "Recovery is in the usual range", "回復は普段の範囲", "Recuperatio in ambitu solito")
        add("恢复指标偏低", "Recovery is on the low side", "回復はやや低め", "Recuperatio sub solito")
        add("恢复指标明显偏低", "Recovery is clearly low", "回復は明らかに低め", "Recuperatio clare parva")
        add("压力读数很松", "The pressure reading is relaxed", "緊張の測定値は低め", "Pressio remissa")
        add("压力读数正常", "The pressure reading is normal", "緊張は一般的な範囲", "Pressio solita")
        add("压力读数偏高", "The pressure reading is on the high side", "緊張はやや高め", "Pressio super solitum")
        add("压力读数明显偏高", "The pressure reading is clearly high", "緊張は明らかに高め", "Pressio clare magna")
        add("活动量比平时多", "Activity is above usual", "活動量は普段より多め", "Motus solito maior")
        add("活动量正常", "Activity is normal", "活動量は普段の範囲", "Motus solitus")
        add("活动量偏少", "Activity is on the low side", "活動量はやや少なめ", "Motus sub solito")
        add("几乎没怎么活动", "Hardly any activity", "活動はごく少なめ", "Motus fere nullus")
        add("昨夜睡眠充足", "Last night's sleep was enough", "昨夜の睡眠は十分", "Somnos noctis praeteritae sufficiens")
        add(
            "昨夜睡眠勉强够",
            "Last night's sleep was just about enough",
            "昨夜の睡眠はぎりぎり十分",
            "Somnos noctis praeteritae vix sufficiens"
        )
        add(
            "昨夜睡眠偏少",
            "Last night's sleep was on the short side",
            "昨夜の睡眠はやや不足",
            "Somnos noctis praeteritae sub solito"
        )
        add(
            "昨夜睡眠明显不足",
            "Last night's sleep was clearly short",
            "昨夜の睡眠は明らかに不足",
            "Somnos noctis praeteritae clare deficiens"
        )
        add(
            "目前没有任何可用数据，所以这里的判断只是默认值，不代表你的身体状况。",
            "No data is available, so this is only the default and says nothing about your condition.",
            "利用できるデータがないため、これは既定表示であり実際の体調を示すものではありません。",
            "Nulla data praesto sunt; haec opinio default est, non status tuus."
        )
        add(
            "这些只是趋势，不是诊断。",
            "These are only tendencies, not a diagnosis.",
            "これは傾向であり、診断ではありません。",
            "Hae inclinationes sunt, non diagnosis."
        )
    }

    // ------------------------------------------------------------------
    // Health Connect / 手环接入
    // ------------------------------------------------------------------
    init {
        add("心率变异性 HRV", "Heart rate variability (HRV)", "心拍変動 HRV", "Variabilitas pulsus HRV")
        add("运动记录", "Workouts", "ワークアウト", "Exercitationes")
        add("把步数、睡眠、心率、血氧这几项打开", "Turn on steps, sleep, heart rate and blood oxygen", "歩数、睡眠、心拍数、血中酸素をオンにします")
        add(
            "允许同步到系统健康服务（Health Connect）",
            "Allow syncing to Health Connect",
            "システムの健康サービス（Health Connect）への同期を許可します"
        )
        add(
            "回到这里点「手动同步」，数据就会读进来",
            "Come back here and tap Sync now to pull the data in",
            "ここに戻って「今すぐ同期」を押すと、データが読み込まれます"
        )
        add("小米运动健康", "Mi Fitness", "Mi Fitness")
        add("华为运动健康", "Huawei Health", "Huawei Health")
        add(
            "手环数据统一经过系统健康服务汇总到这里。",
            "Wearable data all comes together through Health Connect.",
            "ウェアラブルのデータは、システムの健康サービスを通してここにまとまります。",
            "Data armillae per Health Connect hic colliguntur."
        )
        add(
            "系统健康服务里勾了哪些，这里就显示哪些。",
            "Whatever is allowed in Health Connect shows up here.",
            "システムの健康サービスで許可した項目だけが、ここに表示されます。",
            "Quae in Health Connect permissa sunt, hic monstrantur."
        )
        add(
            "未授权的类型不会标红，也不影响使用——只是这部分数据暂时读不到。",
            "Types you have not allowed are not flagged and block nothing; that data simply cannot be read yet.",
            "許可していない項目は赤くもならず、使う支障もありません。その部分のデータがまだ読めないだけです。",
            "Genera non permissa non rubro notantur neque usum impediunt; ea data nondum legi possunt."
        )
        add(
            "在系统健康服务里勾选要允许的类型。",
            "Tick the types you want to allow in Health Connect.",
            "システムの健康サービスで、許可する項目にチェックを入れてください。"
        )
        add(
            "只能去系统里取消，应用自己撤不了。",
            "Only the system can take this away; the app cannot revoke it itself.",
            "取り消せるのはシステム側だけです。アプリ自身では戻せません。",
            "Solum systema revocare potest; applicatio ipsa non potest."
        )
        add(
            "撤销之后应用就读不到数据了，天气会显示「暂时没读到」。你随时可以再授权。",
            "After revoking, the app can no longer read data and the weather shows “Not available yet”. You can grant it again any time.",
            "取り消すとアプリはデータを読めなくなり、天気は「まだ読み取れていません」と表示されます。いつでも再び許可できます。",
            "Post revocationem applicatio data legere non potest; caelum \"nondum lectum\" monstrat. Semper iterum permittere potes."
        )
        add(
            "数据不直接连应用，先汇总到系统的健康服务。",
            "Data does not come straight to the app; it gathers in Health Connect first.",
            "データはアプリに直接届きません。まずシステムの健康サービスにまとまります。",
            "Data ad applicationem directe non veniunt: primum in Health Connect colliguntur."
        )
        add(
            "应用不会直接连手环，所有数据都经过系统的健康服务汇总。",
            "The app never connects to the band directly; everything gathers through Health Connect.",
            "アプリがウェアラブルに直接つながることはありません。すべてのデータはシステムの健康サービスを通してまとまります。",
            "Applicatio armillam directe non attingit: omnia data per Health Connect colliguntur."
        )
        add("去装/去更新", "Install / update", "インストール / 更新", "Installa / renovate")
    }

    // ------------------------------------------------------------------
    // 设置页
    // ------------------------------------------------------------------
    init {
        add(
            "优先用哪一路数据来定今天的天气。",
            "Which data takes priority for today's weather.",
            "今日の体調を決めるときに、どのデータを優先するか。",
            "Quae data praecedant caelum hodie."
        )
        add(
            "用来演示界面效果，不是你的真实状态。",
            "For previewing the interface, not your actual state.",
            "画面の見え方を確かめるためのもので、実際の体調ではありません。",
            "Ad speciem monstrandam, non status tuus."
        )
        add(
            "读不到数据时怎么办、你能手动告诉它什么。",
            "What happens when data cannot be read, and what you can tell it by hand.",
            "データが読めないときの動きと、手動で伝えられること。",
            "Quid fiat cum data desunt, et quid manu dicere possis."
        )
        add(
            "读不到数据时先按这个显示。",
            "Shown when no data can be read.",
            "データが読めないときは、まずこれを表示します。",
            "Cum data desunt, hoc monstratur."
        )
        add(
            "关掉的标签不会出现在首页上。",
            "Tags you switch off will not appear on the Home page.",
            "オフにしたタグはホームに表示されません。",
            "Tituli clausi in pagina Domus non apparent."
        )
        add(
            "敏感度越高，天气越容易跟着读数变化。",
            "The higher the sensitivity, the more the weather follows the readings.",
            "感度が高いほど、体調が測定値に合わせて変わりやすくなります。",
            "Quanto sensibilitas maior, tanto caelum facilius mensuras sequitur."
        )
        add(
            "把今天的校准标签和自定义标签都清掉。",
            "Clear today's calibration tags and your own tags.",
            "今日の調整タグと自分で追加したタグをすべて消します。",
            "Titulos hodiernos et proprios delere."
        )
        add(
            "会清掉今天勾过的感觉标签，也会清掉你自己加的标签。天气会回到按读数判断。",
            "This clears the feeling tags you ticked today and any tags you added. The weather goes back to the readings.",
            "今日選んだ感覚タグと、自分で追加したタグが消えます。体調は測定値にもとづく判定に戻ります。",
            "Tituli hodie electi et proprii delentur. Caelum ad mensuras redit."
        )
        add(
            "处方多长、在什么场景做、要不要声音。",
            "How long the plan runs, where you do it, and whether sound is on.",
            "プランの長さ、行う場面、音声の有無。",
            "Quam longum consilium, ubi fiat, sonusne adsit."
        )
        add(
            "至少留一个场景，不然处方没法生成。",
            "Keep at least one situation, or no plan can be made.",
            "場面を少なくとも一つ残してください。残さないとプランを作れません。",
            "Unum locum saltem relinque, aliter consilium fieri non potest."
        )
        add(
            "做微行动的时候有声音带着你。",
            "Sound guides you through the action.",
            "小さな行動の間、音声が案内します。",
            "Dum actionem parvam facis, vox te ducit."
        )
        add(
            "本版本还没有音频资源，这个选择会被记住。",
            "This version has no audio yet; the choice will be remembered.",
            "このバージョンにはまだ音声がありません。選択は記憶されます。",
            "Haec versio sonum nondum habet; electio memoria tenetur."
        )
        add(
            "说「更累了」下次就会更短更温和。",
            "Say you are more tired and the next one gets shorter and gentler.",
            "「疲れた」を選ぶと、次はもっと短くやさしくなります。",
            "Si \"lassior\" eligis, proximum brevius et mitius erit."
        )
        add(
            "吃什么、不吃什么，由你定。",
            "What you eat and what you skip is up to you.",
            "何を食べて何を避けるかは、あなたが決めます。",
            "Quid edas et quid vitas, tu decidis."
        )
        add(
            "在食谱里把可能踩雷的食材标出来。",
            "Marks ingredients that might be a problem in the food list.",
            "食事の一覧で、気をつけたい食材に印をつけます。",
            "In indice ciborum res suspectas notat."
        )
        add(
            "本版本的通知只做到这里，真正的提醒能力会在后续版本接入。",
            "Notifications stop here in this version; real reminders arrive later.",
            "このバージョンの通知はここまでです。実際のリマインダー機能は今後追加されます。",
            "Notae huius versionis hic desinunt; admonitiones verae in versionibus proximis addentur."
        )
        add("每日倾听提醒", "Daily check-in reminder", "毎日のチェックイン", "Admonitio cotidiana")
        add(
            "每天提醒你回来看看今天的身体天气。",
            "A daily nudge to come back and look at today's body weather.",
            "毎日、今日の体調を見に戻るよう知らせます。",
            "Cotidie monet ut caelum corporis hodiernum inspicias."
        )
        add("提醒时间", "Reminder time", "リマインダーの時刻", "Hora admonitionis")
        add("微行动提醒", "Action reminder", "小さな行動のリマインダー", "Admonitio actionis")
        add(
            "到了点提醒你今天的那张处方。",
            "A nudge when it is time for today's plan.",
            "時間になると、今日のプランを知らせます。",
            "Tempore consilium hodiernum nuntiat."
        )
        add("身体来信提醒", "Body letter reminder", "体からの手紙のリマインダー", "Admonitio epistulae")
        add("每周日晚上", "Sunday evening", "毎週日曜の夜", "Die Solis vesperi")
        add("免打扰时段", "Quiet hours", "おやすみ時間", "Horae quietis")
        add(
            "这段时间里不打扰你。",
            "Nothing will disturb you during these hours.",
            "この時間帯は通知しません。",
            "His horis non te interpellamus."
        )
        add("结束时间", "End time", "終了時刻", "Hora finis")
        add(
            "免打扰时仍允许手动校准",
            "Allow manual calibration during quiet hours",
            "おやすみ時間でも手動調整は許可する",
            "Calibratio manualis etiam horis quietis"
        )
        add(
            "免打扰只挡提醒，不挡你自己想说的话。",
            "Quiet hours block reminders, not what you want to say.",
            "おやすみ時間が止めるのはリマインダーだけです。あなたの言葉は止めません。",
            "Horae quietis admonitiones tantum impediunt, non te."
        )
        add("通知权限状态", "Notification permission", "通知の権限", "Permissio notificationum")
        add(
            "本版本还没有真正发出通知。",
            "This version does not send notifications yet.",
            "このバージョンではまだ実際に通知を送っていません。",
            "Haec versio notificationes nondum mittit."
        )
        add("未申请", "Not requested", "未申請", "Non petitum")
        add(
            "去系统设置里打开通知",
            "Turn on notifications in system settings",
            "システム設定で通知をオンにする",
            "Notificationes in systemate aperire"
        )
        add("数据只存在这台手机上。", "Data stays on this phone.", "データはこの端末の中だけにあります。", "Data in hoc instrumento manent.")
        add(
            "数据只存在这台手机上。应用默认不联网、没有服务器、没有账号。本机保存的只有「今天是什么天气」和「你手动勾选的感觉标签」，不保存原始心率、睡眠、血氧数值。",
            "Data stays on this phone. By default the app has no network, no server and no account. Only today's weather and the feeling tags you ticked are stored; raw heart rate, sleep and blood oxygen values are not.",
            "データはこの端末の中だけにあります。既定ではアプリは通信せず、サーバーもアカウントもありません。端末に保存するのは「今日の体調」と「手動で選んだ感覚タグ」だけで、心拍数・睡眠・血中酸素の生の数値は保存しません。",
            "Data in hoc instrumento manent. Applicatio rete non utitur, nullum servum, nullam rationem habet. Solum caelum hodiernum et tituli manu electi servantur; mensurae crudae non servantur."
        )
        add(
            "例外只有一条、而且默认关闭：你自己连接「小米账号（云同步）」之后，应用才会去小米的服务器取你自己账号里的数据；详情见设置 → 隐私与安全里的「数据边界」。",
            "There is exactly one exception, and it is off by default: only after you connect “Mi account (cloud sync)” yourself does the app fetch data from your own account on Xiaomi's servers. Details are under Settings → Privacy → “Data boundaries”.",
            "例外は一つだけで、既定ではオフです。あなた自身が「Mi アカウント（クラウド同期）」をつないだときだけ、アプリは Xiaomi のサーバーからご自身のアカウントのデータを取得します。詳細は「設定 → プライバシー」の「データの境界」にあります。",
            "Una tantum exceptio est, quae default clausa manet: solum postquam ipse \"Rationem Mi (synchronizatio nubis)\" coniungis, applicatio data ex ratione tua apud servos Xiaomi petit. Plura in Optionibus → Privato."
        )
        add("导出格式", "Export format", "書き出し形式", "Forma exportationis")
        add("导出到剪贴板", "Copy to clipboard", "クリップボードにコピー", "In clipboardum exportare")
        add(
            "不写文件，也不申请存储权限。",
            "No file is written and no storage permission is requested.",
            "ファイルは作成せず、ストレージの権限も求めません。",
            "Nullus fasciculus scribitur, nulla permissio memoriae petitur."
        )
        add("已经复制到剪贴板", "Copied to clipboard", "クリップボードにコピーしました", "In clipboardum copiatum est")
        add(
            "粘贴到任何地方都能存下来。",
            "Paste it anywhere to keep it.",
            "どこにでも貼り付けて残せます。",
            "Ubique imprimere potes ut serves."
        )
        add(
            "导出内容只有「日期 + 天气 + 你勾过的标签」，不含任何原始心率、睡眠或血氧数值。",
            "The export holds only date, weather and the tags you ticked — no raw heart rate, sleep or blood oxygen values.",
            "書き出す内容は「日付 + 体調 + 選んだタグ」だけです。心拍数・睡眠・血中酸素の生の数値は含みません。",
            "Exportatio diem, caelum et titulos electos tantum continet; mensurae crudae non insunt."
        )
        add("删除所有数据", "Delete all data", "すべてのデータを削除", "Omnia data delere")
        add(
            "清空本机的记录、标签和偏好。",
            "Clear records, tags and preferences on this phone.",
            "端末の記録、タグ、設定を消します。",
            "Notas, titulos et praeferentias huius instrumenti delere."
        )
        add(
            "会清空本机的天气记录、校准标签和偏好。这个操作没法撤销。",
            "This clears weather records, calibration tags and preferences on this phone. It cannot be undone.",
            "端末の体調記録、調整タグ、設定を消します。この操作は取り消せません。",
            "Notae caeli, tituli calibrationis et praeferentiae in hoc instrumento delentur. Hoc revocari non potest."
        )
        add("删除", "Delete", "削除", "Delere")
        add("权限管理", "Permissions", "権限の管理", "Permissiones")
        add(
            "去系统的应用详情页看这台机器给了什么权限。",
            "Open the system app page to see what permissions are granted.",
            "システムのアプリ詳細ページで、この端末が許可している権限を確認できます。",
            "Aperi paginam systematis applicationis ut videas quae permissions data sint."
        )
        add("跟随系统（简体中文）", "Follow system (Simplified Chinese)", "システムに合わせる（簡体字中国語）", "Systema sequi (简体中文)")
        add("跟随系统（English）", "Follow system (English)", "システムに合わせる（English）", "Systema sequi (English)")
        add(
            "跟随系统：中文显示简体中文，其他语言默认显示英语。",
            "Follow system: Chinese shows Simplified Chinese, other languages default to English.",
            "システムに合わせる場合：中国語は簡体字中国語、その他の言語は英語で表示します。",
            "Systema sequi: Sinice 简体中文, aliter English."
        )
        add(
            "这个应用是谁做的、站在谁的肩膀上。",
            "Who made this app, and what it stands on.",
            "このアプリを作った人と、その土台になっているもの。",
            "Quis hanc applicationem fecit et quibus nitatur."
        )
        add(
            "以上 AI 工具辅助完成了本应用的代码编写、界面设计与文档整理。",
            "These AI tools helped with the code, the interface design and the documentation.",
            "以上の AI ツールが、コードの作成、画面設計、資料の整理を補助しました。",
            "Instrumenta AI supradicta codicem, speciem et documenta adiuverunt."
        )
        add(
            "这个应用站在这些开源项目上面。",
            "This app stands on these open-source projects.",
            "このアプリは、これらのオープンソースプロジェクトの上に成り立っています。",
            "Haec applicatio his inceptis apertis nititur."
        )
        add("Jetpack Compose（Apache 2.0）", "Jetpack Compose (Apache 2.0)", "Jetpack Compose（Apache 2.0）")
        add(
            "Kyant Backdrop / Shapes（Apache 2.0）",
            "Kyant Backdrop / Shapes (Apache 2.0)",
            "Kyant Backdrop / Shapes（Apache 2.0）"
        )
    }

    // ------------------------------------------------------------------
    // 食谱页提示与饮食小贴士
    // ------------------------------------------------------------------
    init {
        add(
            "按营养素找吃的。这里没有热量，也没有该吃多少。",
            "Find food by nutrient. No calories here, and no amounts to hit.",
            "栄養素から食べ物を探せます。ここにカロリーも、食べる量もありません。",
            "Cibum per nutrimentum quaere. Nullae caloríae, nulla quantitas."
        )
        add(
            "搜「补铁」能找到含铁的食物，搜「菠菜」能直接找到那样食物。搜不到也没关系，换个词就行。",
            "Search “iron” to find foods that carry it, or “spinach” to go straight to it. Finding nothing is fine; try another word.",
            "「鉄」で検索すると鉄を含む食べ物が、「ほうれん草」で検索するとその食べ物そのものが出ます。見つからなければ、別の言葉で試してください。",
            "Quaere ferrum ut cibos ferri invenias, aut spinaciam ut ipsum invenias. Nihil inventum? Aliud verbum tenta."
        )
        add(
            "选一个菜系，三餐建议和下面的列表都跟着换。",
            "Pick a cuisine and the meal suggestions and the list below follow.",
            "料理の種類を一つ選ぶと、三食の提案と下の一覧もそれに合わせて変わります。",
            "Elige genus cibi: consilia et index infra sequuntur."
        )
        add(
            "选一个，只看这一类。",
            "Pick one to see just that group.",
            "一つ選ぶと、その分類だけを表示します。",
            "Unum elige: solum hunc gregem videbis."
        )
        add(
            "下面是全部食物。点上面的营养素可以只看那一类。",
            "Everything is listed below. Tap a nutrient above to narrow it.",
            "下はすべての食べ物です。上の栄養素を押すと、その分類だけを表示できます。",
            "Infra omnia alimenta sunt. Nutrimentum supra tange ut unum gregem videas."
        )
        add(
            "点卡片看为什么值得吃、怎么做最省事。",
            "Open a card for why it is worth eating and how to keep it simple.",
            "カードを押すと、おすすめの理由と、いちばん簡単な作り方が見られます。",
            "Tange tabellam ut causam et modum videas."
        )
        add(
            "过敏原提醒已打开。这里的每张卡片都写清了食材本身，如果你对某类食物过敏，请以卡片上的食材为准。",
            "Allergen notes are on. Every card states what is in the dish; if you react to something, go by the ingredients on the card.",
            "アレルゲン表示がオンです。どのカードにも使う食材が書かれています。アレルギーがある場合は、カードの食材を基準にしてください。",
            "Monitio allergenorum activa est. Quaeque tabella res indicat; si allergia est, tabellam sequere."
        )
        add(
            "按你的饮食偏好筛过了，这一批里没有需要隐藏的。",
            "Filtered by your diet preferences; nothing in this batch needed hiding.",
            "食事の好みで絞り込みました。この中に隠す必要のあるものはありません。",
            "Praeferentiis victus filtrata: nihil celandum in hoc grege."
        )
        add(
            "按你现在的饮食偏好，这一类里没有剩下的了。想全看的话，去设置里改一下偏好。",
            "With your current diet preferences there is nothing left in this group. To see everything, change them in Settings.",
            "現在の食事の好みでは、この分類に残りがありません。すべて見るには、設定で好みを変えてください。",
            "Praeferentiis hodiernis nihil in hoc grege restat. In Optionibus muta ut omnia videas."
        )
        add(
            "都是搭配上的小事，不用一次全做到。",
            "Small pairing notes; no need to do them all at once.",
            "組み合わせのちょっとした話です。一度に全部できなくて構いません。",
            "Res parvi momenti sunt; omnia simul non necesse est."
        )
        add(
            "为什么值得吃，怎么做最省事。",
            "Why it is worth eating and how to keep it simple.",
            "おすすめの理由と、いちばん簡単な作り方。",
            "Cur utile et quomodo facillime praeparetur."
        )
        add(
            "这里只讲食物本身，不做诊断。有特殊健康状况的话，请咨询医生。",
            "This is about food only and does not diagnose. If you have a specific condition, ask a clinician.",
            "ここでは食べ物そのものの話だけをし、診断はしません。持病などがある場合は医療専門家に相談してください。",
            "Hic de cibo tantum agitur, non de diagnosi. Si valetudo peculiaris est, medicum consule."
        )
        add(
            "维生素 C 可以促进铁的吸收，吃完含铁的食物，顺手来个橙子或猕猴桃。",
            "Vitamin C helps iron get absorbed; an orange or a kiwi after an iron-rich meal does the job.",
            "ビタミンCは鉄の吸収を助けます。鉄の多い食事のあとに、オレンジやキウイを一つどうぞ。",
            "Vitaminum C ferrum absorberi iuvat: post cibum ferri, citrum aut actinidiam sume."
        )
        add(
            "草酸会影响钙的吸收，菠菜建议先焯水再炒。",
            "Oxalates get in the way of calcium, so blanch spinach before stir-frying.",
            "シュウ酸はカルシウムの吸収を妨げます。ほうれん草は下ゆでしてから炒めるのがおすすめです。",
            "Oxalata calcium absorberi impediunt: spinaciam ante frixionem aqua ferventi coque."
        )
        add(
            "胡萝卜富含 β-胡萝卜素，对眼睛好，适合和油脂一起烹饪。",
            "Carrots carry plenty of beta-carotene, which is kind to the eyes; cook them with a little oil.",
            "にんじんはβ-カロテンが豊富で目にやさしく、油と一緒に調理すると吸収がよくなります。",
            "Daucus beta-caroteno abundat, oculis prodest, et cum oleo coqui debet."
        )
        add(
            "补钙不是只有牛奶，豆制品、小鱼干、芝麻酱都算。",
            "Calcium is not only milk: soy foods, dried small fish and sesame paste count too.",
            "カルシウムは牛乳だけではありません。大豆製品、小魚、ごまペーストも数に入ります。",
            "Calcium non solum in lacte est: e soia, pisciculis siccis, sesamo fit."
        )
        add(
            "蛋白质分散到三餐比集中在一顿更好吸收。",
            "Spreading protein across meals works better than loading it into one.",
            "たんぱく質は一食にまとめるより、三食に分けたほうが吸収がよくなります。",
            "Proteinum per tres cibos divisum melius quam in unum congestum absorbetur."
        )
        add(
            "深色叶菜的营养密度通常更高一些，菠菜、西兰花、芥蓝都算。",
            "Dark leafy greens are usually denser: spinach, broccoli, Chinese kale.",
            "色の濃い葉物は栄養密度が高めです。ほうれん草、ブロッコリー、カイランなどが当てはまります。",
            "Olera foliosa obscura densiora sunt: spinacia, broccolis, caulis."
        )
        add(
            "发酵食品（酸奶、纳豆、味噌）换着吃，比只吃一种更有意思。",
            "Rotate fermented foods — yogurt, natto, miso — instead of sticking to one.",
            "発酵食品（ヨーグルト、納豆、味噌）は、一種類に決めず順番に食べると楽しいです。",
            "Cibos fermentatos (iogurtum, natto, miso) alterna: melius quam unus solus."
        )
        add(
            "坚果一次一小把就够，多了容易顶掉正餐。",
            "A small handful of nuts is enough; more tends to crowd out a meal.",
            "ナッツは一度にひとつかみで十分です。多いと食事が入らなくなります。",
            "Pugillus nucum satis est; plus cibum principalem excludit."
        )
        add(
            "汤里的料比汤水更有内容，别只喝汤。",
            "The solids in a soup carry more than the broth; do not just drink it.",
            "スープは汁より具のほうが中身があります。汁だけ飲まないでください。",
            "In iure solidа plus quam liquor valent: noli solum ius bibere."
        )
        add(
            "维生素 D 在食物里不多，晒会儿太阳是最省事的一条路。",
            "Vitamin D is scarce in food; a little sunlight is the easiest route.",
            "ビタミンDは食べ物に多くありません。少し日光に当たるのがいちばん手軽です。",
            "Vitaminum D in cibo parum est: sol via facillima est."
        )
        add(
            "锌的来源比想象中多：南瓜子、牛肉、鹰嘴豆、黑芝麻都算。",
            "Zinc comes from more places than you would think: pumpkin seeds, beef, chickpeas, black sesame.",
            "亜鉛の源は思うより多くあります。かぼちゃの種、牛肉、ひよこ豆、黒ごまなどです。",
            "Zincum pluribus in rebus est quam credis: seminibus cucurbitae, bubula, cicere, sesamo nigro."
        )
    }

    // ------------------------------------------------------------------
    // 处方与完成反馈
    // ------------------------------------------------------------------
    init {
        add(
            "做完选一个就行。选了「更累了」，下次就会更短更温和。",
            "Just pick one afterwards. Choose “More tired” and the next one is shorter and gentler.",
            "終わったら一つ選ぶだけで十分です。「疲れた」を選ぶと、次はより短くやさしくなります。",
            "Post actionem unum elige. Si \"lassior\" eligis, proximum brevius et mitius erit."
        )
        add("把灯调暗一点", "Dim the lights a little", "照明を少し暗くする", "Lumen obscura")
        add("开窗 2 分钟", "Open a window for 2 minutes", "窓を2分開ける", "Fenestram 2 minuta aperi")
        add("手机反面朝上 5 分钟", "Phone face down for 5 minutes", "スマホを伏せて5分", "Telephonum inversum 5 minuta")
        add("热敷眼睛", "Warm compress on the eyes", "目を温める", "Oculos fove")
        add("喝 200ml 水", "Drink 200 ml of water", "水を200ml飲む", "Bibe 200 ml aquae")
        add(
            "点上面的时段，或者按住票身上的「拖到时段」把它拖过去。",
            "Tap a time slot above, or press “Drag to a time” and move it there.",
            "上の時間帯を押すか、チケットの「時間帯へ移動」を長押しして移動します。",
            "Horam supra tange, aut tabellam tene et ad horam trahe."
        )
        add("按你舒服的节奏来。", "Follow a pace that feels comfortable.", "楽なペースで大丈夫です。", "Ritmo commodo utere.")
        add(
            "也可以左右滑动上面那张票，效果一样。",
            "You can also swipe the ticket above; same result.",
            "上のチケットを左右にスワイプしても同じです。",
            "Tabellam supra transversim trahere potes: idem valet."
        )
        add(
            "这台设备没有可用的中文语音，暂时开不了",
            "No usable voice is installed on this device, so this cannot be turned on yet",
            "この端末に使える音声がないため、今はオンにできません"
        )
        add(
            "本版本还没有音频资源，这个选择会被记住，接上音频后立即生效。",
            "This version has no audio yet; your choice is remembered and takes effect once audio is added.",
            "このバージョンにはまだ音声がありません。選択は記憶され、音声が入り次第すぐ反映されます。",
            "Haec versio sonum nondum habet; electio memoria tenetur et, sono addito, statim valebit."
        )
        add(
            "这一轮结束了。不用打分，也不用汇报。",
            "That round is done. No rating, no report needed.",
            "この回は終わりです。採点も報告も要りません。",
            "Haec rotunda perfecta est. Punctis non opus est."
        )
        add(
            "刚才那几分钟，身体有什么变化？",
            "What changed in the body during those few minutes?",
            "さっきの数分で、体にどんな変化がありましたか？",
            "Quid in corpore paucis his minutis mutatum est?"
        )
        add(
            "CastOverlayHost 尚未绑定背景层，浮层必须在 CastOverlayHost 内部使用",
            "CastOverlayHost has no backdrop bound; overlays must be used inside CastOverlayHost"
        )
        add(
            "LocalCastOverlay 未提供，请检查 MainActivity 里的 CompositionLocalProvider",
            "LocalCastOverlay is not provided; check the CompositionLocalProvider in MainActivity"
        )
    }

    // ------------------------------------------------------------------
    // 隐私说明页与首页空状态（拼接后的完整句子）
    // ------------------------------------------------------------------
    init {
        add("数据从哪里来", "Where the data comes from", "データの出どころ", "Unde data veniunt")
        add(
            "只从系统的健康服务（Health Connect）读取。你的手环先把数据同步到小米运动健康或华为运动健康，再由它们同步进 Health Connect，BodyCast 只做最后一步的读取。应用不直接连蓝牙，也不连手环。",
            "It reads only from Health Connect. Your band syncs to Mi Fitness or Huawei Health first, those sync into Health Connect, and BodyCast does the last read. The app never talks to Bluetooth or to the band directly.",
            "システムの健康サービス（Health Connect）からだけ読み取ります。ウェアラブルはまず Mi Fitness や Huawei Health に同期され、それらが Health Connect に書き込み、BodyCast は最後の読み取りだけを行います。アプリが Bluetooth で直接つながることも、ウェアラブルに接続することもありません。"
        )
        add("读了哪些数据", "What is read", "読み取るデータ", "Quae data legantur")
        add(
            "步数、睡眠、静息心率、心率变异性、血氧、运动记录，共六项。默认全部**只读**，应用不会修改系统里的健康记录。例外只有一个：你在「数据与同步」里打开「把读数写入系统健康服务」开关后，应用会把本机已同步的聚合读数写进 Health Connect（来源标注为本应用）；开关默认关闭，睡眠与运动只在有真实起止时间时写入。",
            "Steps, sleep, resting heart rate, heart rate variability, blood oxygen and workouts: six types. Read-only by default; the app never changes your health records. One exception: turn on “Write readings to the system health service” under Data & sync and the app writes the already-synced aggregate readings into Health Connect (labelled as this app). Off by default; sleep and workouts are written only with real start and end times.",
            "歩数、睡眠、安静時心拍数、心拍変動、血中酸素、ワークアウトの六項目です。既定では**読み取り専用**で、健康記録を変更しません。例外はひとつだけ：「データと同期」で「測定値をシステムの健康サービスに書き込む」をオンにすると、同期済みの集計値を Health Connect に書き込みます（出どころは本アプリ）。既定ではオフで、睡眠とワークアウトは実際の時刻が分かるときだけ書き込みます。"
        )
        add("数据存在哪", "Where it is stored", "データの保存先", "Ubi data maneant")
        add(
            "只存在这台手机上。应用默认不联网、没有服务器、没有账号，平时也没有任何上传行为。本机保存的只是「今天是什么天气」和「你手动勾选的感觉标签」，不保存原始心率、睡眠、血氧数值。",
            "It stays on this phone. By default the app has no network, no server, no account and uploads nothing. What is stored is only today's weather and the feeling tags you ticked, never raw heart rate, sleep or blood oxygen values.",
            "この端末の中だけです。既定ではアプリは通信せず、サーバーもアカウントもなく、アップロードも行いません。端末に保存するのは「今日の体調」と「手動で選んだ感覚タグ」だけで、心拍数・睡眠・血中酸素の生の数値は保存しません。"
        )
        add(
            "可选的一条联网通道：小米账号（云同步）",
            "One optional online channel: Mi account (cloud sync)",
            "任意で使える通信経路：Mi アカウント（クラウド同期）"
        )
        add(
            "默认关闭。只有你自己在「设置 → 数据与同步 → 小米账号（云同步）」里主动连接之后，应用才会访问小米的服务器，把你自己账号里的运动健康数据取回来。· 连了什么域名：登录页是 account.xiaomi.com（小米官方账号页）；数据请求发给 hlth.io.mi.com（中国大陆）或 <区域>.hlth.io.mi.com（其它区域）；· 传了什么：只有查询条件——要哪一类读数、哪个时间段。应用不向小米写任何数据，也不上传本机的任何内容；· 密码呢：密码、验证码都只输在小米自己的登录页上。BodyCast 看不到、不保存、不转发。",
            "Off by default. Only after you connect it yourself under Settings → Data and sync → Mi account (cloud sync) does the app talk to Xiaomi's servers, to fetch the fitness data in your own account. · Domains: the sign-in page is account.xiaomi.com (Xiaomi's official account page); data requests go to hlth.io.mi.com (mainland China) or <region>.hlth.io.mi.com (other regions). · What is sent: query conditions only — which reading type and which time range. The app writes nothing to Xiaomi and uploads nothing from this phone. · What about the password: the password and any verification code are typed only on Xiaomi's own sign-in page. BodyCast cannot see, store or forward them.",
            "既定ではオフです。あなた自身が「設定 → データと同期 → Mi アカウント（クラウド同期）」で接続したときだけ、アプリは Xiaomi のサーバーにアクセスし、ご自身のアカウントにある運動健康データを取得します。· 接続先：ログインページは account.xiaomi.com（Xiaomi 公式アカウントページ）、データ要求は hlth.io.mi.com（中国本土）または <地域>.hlth.io.mi.com（その他の地域）です。· 送るもの：照会条件だけ（どの種類の測定値か、どの期間か）。アプリは Xiaomi に何も書き込まず、端末の内容も送りません。· パスワードについて：パスワードと確認コードは Xiaomi 自身のログインページにのみ入力します。BodyCast は見ることも、保存することも、転送することもできません。"
        )
        add(
            "这条通道在本机存了什么",
            "What this channel stores on the phone",
            "この経路が端末に保存するもの"
        )
        add(
            "只存三样东西：① 小米回给应用的访问令牌（serviceToken / passToken 等）；② 六类读数的聚合值；③ 上次同步时间。令牌用 Android Keystore 生成的 AES 密钥加密之后才落盘，明文令牌不落盘，应用日志与界面都不显示令牌内容。这条通道的请求只发生在你点「连接」与「立即同步」时，后台不会自己联网。",
            "Three things only: (1) the access tokens Xiaomi hands back (serviceToken / passToken and similar); (2) aggregate values for the six reading types; (3) the last sync time. Tokens are encrypted with an AES key generated by the Android Keystore before they are written to disk; the plaintext token is never stored, and neither the logs nor the interface show it. Requests only happen when you tap Connect or Sync now — the app never goes online on its own in the background.",
            "保存するのは三つだけです。① Xiaomi が返すアクセストークン（serviceToken / passToken など）。② 六種類の測定値の集計値。③ 前回の同期時刻。トークンは Android Keystore が生成した AES 鍵で暗号化してから保存し、平文のトークンは保存しません。ログや画面にも表示しません。通信が起きるのは「接続」と「今すぐ同期」を押したときだけで、バックグラウンドで勝手に通信することはありません。"
        )
        add(
            "怎么断开这条通道",
            "How to disconnect this channel",
            "この経路の切断方法"
        )
        add(
            "在同一个页面上点「断开并删除令牌」：密钥与令牌一起删掉，旧的密文从此解不开，云端取回来的读数也一并清除。断开之后应用回到默认的「不联网」状态，其它功能不受影响。",
            "Tap Disconnect and delete token on the same page: the key and the token are deleted together, the old ciphertext can never be decrypted again, and the readings fetched from the cloud are cleared as well. The app then returns to its default offline state; nothing else is affected.",
            "同じページで「切断してトークンを削除」を押します。鍵とトークンが一緒に削除され、古い暗号文は二度と復号できません。クラウドから取得した測定値も消去されます。切断後、アプリは既定の非通信状態に戻り、他の機能には影響しません。"
        )
        add("数据去哪了", "Where it goes", "データの行き先", "Quo data eant")
        add(
            "哪也不去。没有第三方 SDK，没有广告，没有数据分析。你在设置里点「删除所有数据」，本机记录会立即清空。",
            "Nowhere. No third-party SDKs, no ads, no analytics. Tap “Delete all data” in Settings and the local records are cleared straight away.",
            "どこにも行きません。第三者 SDK も広告も解析もありません。設定で「すべてのデータを削除」を押すと、端末の記録はすぐに消えます。"
        )
        add("你可以随时收回", "You can take it back at any time", "いつでも取り戻せます", "Semper revocare potes")
        add(
            "在设置页点「撤销授权」，或者在系统的 Health Connect 里取消勾选，应用就再也读不到数据了。收回之后应用仍然可用，只是天气会显示为「暂时没读到」。",
            "Tap “Revoke access” in Settings, or untick the types in Health Connect, and the app can no longer read data. It still works afterwards; the weather just shows “Not available yet”.",
            "設定で「権限を取り消す」を押すか、システムの Health Connect でチェックを外すと、アプリはデータを読み取れなくなります。取り消した後もアプリは使えます。天気が「まだ読み取れていません」と表示されるだけです。"
        )
        add(
            "这份说明只讲事实。如果哪一条你不认可，直接撤销授权就好，应用不会因此不可用。",
            "This page states facts only. If you disagree with any of it, just revoke access; the app will not stop working.",
            "この説明は事実だけを書いています。同意できない項目があれば、権限を取り消すだけで構いません。アプリが使えなくなることはありません。",
            "Haec pagina sola facta narrat. Si quid non probas, permissionem revoca: applicatio usui manet."
        )
        add(
            "如有特殊健康状况，请咨询医生。BodyCast 只播报状态，不做诊断。",
            "If you have a specific health condition, ask a clinician. BodyCast reports state and does not diagnose.",
            "持病などがある場合は医療専門家に相談してください。BodyCast は状態を伝えるだけで、診断はしません。",
            "Si valetudo peculiaris est, medicum consule. BodyCast statum refert, non diagnosim facit."
        )
        add(
            "还没有读到数据。你可以在设置里连接手环，或者直接用手动校准——手动校准的优先级永远高于系统自动判断。",
            "No data has been read yet. You can connect a wearable in Settings, or simply use manual calibration — manual calibration always outranks the automatic assessment.",
            "まだデータを読み取れていません。設定でウェアラブルをつなぐか、手動調整をそのままお使いください。手動調整は常に自動判定より優先されます。",
            "Nulla data adhuc lecta. In Optionibus armillam coniunge aut calibratione manuali utere — ea semper sententiam automaticam praecedit."
        )
    }

    // ------------------------------------------------------------------
    // 关于页：开源许可、反馈与建议、商店回退
    // ------------------------------------------------------------------
    init {
        add(
            "Jetpack Compose、AndroidX / AppCompat —— Apache License 2.0",
            "Jetpack Compose, AndroidX / AppCompat — Apache License 2.0",
            "Jetpack Compose、AndroidX / AppCompat —— Apache License 2.0"
        )
        add(
            "Kotlin 语言与标准库 —— Apache License 2.0",
            "Kotlin language and standard library — Apache License 2.0",
            "Kotlin 言語と標準ライブラリ —— Apache License 2.0"
        )
        add(
            "Kyant Backdrop / Shapes（液态玻璃与图形）—— Apache License 2.0",
            "Kyant Backdrop / Shapes (liquid glass and shapes) — Apache License 2.0",
            "Kyant Backdrop / Shapes（液体ガラスと図形）—— Apache License 2.0"
        )
        add(
            "AndroidX Health Connect 客户端 —— Apache License 2.0",
            "AndroidX Health Connect client — Apache License 2.0",
            "AndroidX Health Connect クライアント —— Apache License 2.0"
        )
        add(
            "Material Components（仅用于窗口主题底色）—— Apache License 2.0",
            "Material Components (window theme background only) — Apache License 2.0",
            "Material Components（ウィンドウのテーマ背景のみ）—— Apache License 2.0"
        )
        add(
            "以上项目均以原样提供，不含任何担保。完整许可文本可在各项目仓库查看。",
            "These projects are provided as is, without warranty. The full license texts are in each project's repository.",
            "これらのプロジェクトは現状のまま提供され、いかなる保証も伴いません。完全なライセンス文は各プロジェクトのリポジトリで確認できます。"
        )
        add(
            "这个应用不收集任何数据。",
            "This app collects no data.",
            "このアプリはいかなるデータも収集しません。",
            "Haec applicatio data nulla colligit."
        )
        add("复制版本信息", "Copy version info", "バージョン情報をコピー", "Informationem versionis copiare")
        add("已经复制", "Copied", "コピーしました", "Copiatum")
        add(
            "报问题时附上这行版本信息会更方便定位。",
            "Pasting this line when you report a problem makes it easier to pin down.",
            "問題を報告するとき、この行を添えると原因を特定しやすくなります。",
            "Hanc lineam adde cum problema nuntias: facilius reperitur."
        )
        add(
            "可以在应用商店留言，或直接告诉开发者。",
            "Leave a note in the app store, or tell the developer directly.",
            "アプリストアに書き込むか、開発者に直接伝えてください。",
            "In taberna scribe aut programmatori directe dic."
        )
        add(
            "应用内没有反馈上传入口。应用不收集数据，平时也不联网——唯一会联网的是你自己开启的小米账号（云同步）通道。",
            "There is no in-app feedback upload. The app collects no data and stays offline by default; the only network path is the Mi account (cloud sync) channel you turn on yourself.",
            "アプリ内にフィードバックの送信口はありません。アプリはデータを収集せず、普段は通信しません。通信するのは、あなた自身が開いた Mi アカウント（クラウド同期）の経路だけです。",
            "Intra applicationem nihil mitti potest. Applicatio data non colligit neque rete utitur; sola via est ratio Mi (synchronizatio nubis) quam ipse aperis."
        )
        add(
            "如果想提意见，可以在应用商店的评价里留言，或直接把想法告诉开发者。",
            "To share an opinion, leave it in an app store review or tell the developer directly.",
            "意見を伝えたい場合は、アプリストアのレビューに書くか、開発者に直接伝えてください。",
            "Si sententiam dicere vis, in taberna scribe aut programmatori dic."
        )
        add(
            "报问题时附上版本号，定位会快很多。",
            "Include the version number when you report a problem; it speeds things up.",
            "問題を報告するときはバージョン番号を添えると、特定がずっと早くなります。",
            "Numerum versionis adde: problema citius reperitur."
        )
        add("先手动装一下", "Install it by hand first", "まず手動でインストール")
        add(
            "这台机器的应用商店里没找到，需要你手动搜一下。",
            "Not found in this device's app store; you will need to search for it.",
            "この端末のアプリストアには見つかりませんでした。手動で検索してください。"
        )
        add(
            "装好后打开它，把数据同步到系统的健康服务。",
            "Once installed, open it and sync your data to Health Connect.",
            "インストールしたら開いて、データをシステムの健康サービスに同期します。"
        )
        add("回到这里点「手动同步」就能读到了。", "Come back here and tap Sync now to read it.", "ここに戻って「今すぐ同期」を押せば読み取れます。")
        add("商店里搜一下", "Search the store", "ストアで検索")
        add("商店里没搜到", "Not found in the store", "ストアで見つかりません")
        add("可以换一个应用商店再试。", "Try another app store.", "別のアプリストアでもう一度試してください。")
        add(
            "有些手机自带商店里没有这个应用，换 Play 商店或厂商商店试试。",
            "Some built-in stores do not carry this app; try the Play Store or the maker's store.",
            "端末付属のストアにないことがあります。Play ストアやメーカーのストアを試してください。"
        )
    }

    // ------------------------------------------------------------------
    // 关于页：系统健康服务的安装 / 更新引导
    // ------------------------------------------------------------------
    init {
        add(
            "这台机器还没有系统健康服务，点开看怎么装。",
            "This device has no Health Connect yet; open this to see how to install it.",
            "この端末にはまだシステムの健康サービスがありません。開くとインストール方法が確認できます。",
            "Hoc instrumentum Health Connect nondum habet; tange ut modum installationis videas."
        )
        add(
            "系统健康服务需要更新，点开看怎么更新。",
            "Health Connect needs an update; open this to see how.",
            "システムの健康サービスの更新が必要です。開くと更新方法が確認できます。",
            "Health Connect renovatione eget; tange ut modum renovationis videas."
        )
        add("系统健康服务", "Health Connect", "システムの健康サービス", "Health Connect")
        add(
            "手环数据要先汇总到它，BodyCast 才能读到。",
            "Wearable data gathers there first, and only then can BodyCast read it.",
            "ウェアラブルのデータがここにまとまって初めて、BodyCast が読み取れます。",
            "Data armillae primum hic colliguntur, tum BodyCast ea legit."
        )
        add("去应用商店", "Open the app store", "アプリストアを開く", "Tabernam aperire")
        add("商店里没找到", "Not found in the store", "ストアで見つかりません", "In taberna non inventum")
        add(
            "有些手机自带商店里没有 Health Connect，换 Play 商店试试。",
            "Some built-in stores do not carry Health Connect; try the Play Store.",
            "端末付属のストアに Health Connect がないことがあります。Play ストアを試してください。"
        )
        add(
            "安卓 14 及以上系统一般自带系统健康服务，不需要另外装。",
            "Android 14 and up usually includes Health Connect, so nothing extra is needed.",
            "Android 14 以降にはたいていシステムの健康サービスが入っているので、別途インストールは不要です。",
            "Android 14 et ultra Health Connect plerumque includunt: nihil extra necessarium est."
        )
        add(
            "安卓 14 及以上：系统一般自带「系统健康服务 / Health Connect」，不用额外安装。",
            "Android 14 and up: Health Connect is usually built in, so no separate install is needed.",
            "Android 14 以降は、たいてい「システムの健康サービス / Health Connect」が入っており、追加インストールは不要です。"
        )
        add(
            "安卓 13 及以下：需要单独装一个 Health Connect，装完再回到这里授权。",
            "Android 13 and below: Health Connect has to be installed separately; come back here to grant access afterwards.",
            "Android 13 以前は Health Connect を別途インストールする必要があります。入れたあと、ここに戻って権限を許可してください。"
        )
        add(
            "有些手机自带商店里搜不到它，可以换 Play 商店或厂商商店再搜一次。",
            "Some built-in stores do not list it; try the Play Store or the maker's store instead.",
            "端末付属のストアで見つからないことがあります。Play ストアやメーカーのストアでもう一度探してください。"
        )
        add(
            "装不上也不影响使用：手动校准和演示数据都能照常用。",
            "If it will not install, nothing breaks: manual calibration and demo data still work.",
            "インストールできなくても使えます。手動調整とデモデータはこれまでどおり使えます。",
            "Si installari non potest, nihil rumpitur: calibratio manualis et data demonstrationis usui manent."
        )

        // ------------------------------------------------------------------
        // 22.13.0：全量补齐——设置/内容/数据簇的界面串英文（多子 Agent 盘点）。
        // ------------------------------------------------------------------
        add("取消", "Cancel", "キャンセル", "Abrogare")
        add("确认", "Confirm", "確認", "Confirmare")
        add("现在 · 四个信号", "Now · four signals", "いま・四つの信号", "Nunc · quattuor signa")
        add("收起", "Collapse", "たたむ", "Claudere")
        add("展开", "Expand", "開く", "Aperire")
        add("今日微行动", "Today's micro-action", "今日の小さな行動", "Actio parva hodie")
        add(
            "几分钟能做完的一件小事；点开查看今天的处方。",
            "A small thing you can finish in a few minutes; open to see today's plan.",
            "数分で終わる小さなこと。開くと今日のプランが見られます。",
            "Res parva paucis minutis; tange ut consilium hodie videas."
        )
        add(
            "这里不会有卡路里、体重，也不会有「今天没达标」。身体状态只有状态，没有好坏——恢复天和高能天一样正当。",
            "No calories, no weight, and no “you did not hit today's goal”. A body state is just a state, never good or bad — a recovery day is as valid as a high-energy one.",
            "ここにはカロリーも体重も「今日は未達成」もありません。体の状態は状態そのもので、良し悪しはありません。回復の日も元気な日と同じように正当です。",
            "Nullae caloríae, nullum pondus, nulla \"hodie non attigisti\". Status corporis status est: dies recuperationis tam iustus quam dies alacritatis."
        )
        add(
            "指标只给词，不给数字：「恢复云量：多云」是一句话，「恢复指数 62」是一个新的比较维度。",
            "Metrics come as words, not numbers: “recovery cloud: cloudy” is a sentence, while “recovery score 62” is a new dimension to compare on.",
            "指標は言葉だけで、数字は出しません。「回復の雲量：曇り」は一文ですが、「回復指数 62」は新たな比較の軸になってしまいます。",
            "Mensurae verbis dantur, non numeris: \"nubes recuperationis: nubilum\" sententia est, \"index 62\" nova comparatio."
        )
        add(
            "读不到就说读不到：显示「暂时没读到」，绝不用 0 冒充真实读数。",
            "If it cannot be read, it says so: “Not available yet”, never a 0 pretending to be a real reading.",
            "読めないときは読めないと言います。「まだ読み取れていません」と表示し、0 で実測値のふりはしません。",
            "Si legi non potest, id dicitur: \"nondum lectum\", numquam 0 pro mensura vera."
        )
        add(
            "界面上没有红色警告，也没有「您今日未达标」这种句式。",
            "There are no red warnings in the interface, and no “you have not met today's goal” phrasing.",
            "画面に赤い警告も、「本日は未達成です」という言い方もありません。",
            "Nullae admonitiones rubrae, nulla sententia \"hodie non attigisti\"."
        )
        add(
            "手环 → 厂商的运动健康 App → 系统健康服务 → 这里。应用不直连手环、不用蓝牙，也不会把你的数据传到任何服务器。",
            "Band → the maker's health app → the system health service → here. The app never talks to the band directly, never uses Bluetooth, and never sends your data to any server.",
            "ウェアラブル → メーカーの運動健康アプリ → システムの健康サービス → ここ。アプリがウェアラブルに直接つながることも、Bluetooth を使うことも、データをどこかのサーバーへ送ることもありません。",
            "Armilla → applicatio fabricatoris → servitium sanitatis systematis → hic. Applicatio armillam directe non attingit, Bluetooth non utitur, data ad servum nullum mittit."
        )
        add(
            "连手环：从设置里打开 Health Connect 的读取授权，六类数据先按只读接进来。",
            "Connect a band: turn on read access for Health Connect in Settings; all six data types come in read-only first.",
            "ウェアラブルをつなぐ：設定で Health Connect の読み取り権限をオンにします。六種類のデータはまず読み取り専用で入ってきます。",
            "Armillam coniungere: in Optionibus permissionem legendi Health Connect aperi; omnia sex genera primum tantum leguntur."
        )
        add(
            "手动校准：读不到也能用，点一下标签就能改写今天的结论。",
            "Manual calibration: works even with no data — one tap on a tag rewrites today's result.",
            "手動調整：読み取れなくても使えます。タグを一つ押せば今日の結論が書き換わります。",
            "Calibratio manualis: etiam sine datis valet; unum titulum tange et conclusio hodierna mutatur."
        )
        add(
            "演示数据：想看完整界面效果时用，首页会挂一条「这是演示数据」的提示。",
            "Demo data: for seeing the full interface; Home shows a “this is demo data” note.",
            "デモデータ：画面全体の見え方を確かめたいときに。ホームに「これはデモデータです」という案内が出ます。",
            "Data demonstrationis: ad speciem plenam videndam; in pagina Domus nota apparet."
        )
        add(
            "首页有九个校准标签：累、紧、胀、困、烦、轻、空、饿、说不清。点一下就能改写今天的结论。",
            "Home has nine calibration tags: tired, tense, bloated, sleepy, irritated, light, empty, hungry, hard to describe. One tap rewrites today's result.",
            "ホームには九つの調整タグがあります：疲れた、張る、張りつめる、眠い、いらいら、軽い、空っぽ、空腹、うまく言えない。一つ押せば今日の結論が書き換わります。",
            "In pagina Domus novem tituli sunt: lassus, tensus, inflatus, somnolentus, irritatus, levis, vacuus, esuriens, difficile dictu. Unus tactus conclusionem mutat."
        )
        add(
            "手动校准的优先级永远高于设备读数：你说累，就是累。",
            "Manual calibration always comes before device readings: if you say tired, tired it is.",
            "手動調整は常にデバイスの測定値より優先されます。疲れたと言えば、それが結論です。",
            "Calibratio manualis mensuras semper praecedit: si lassum te dicis, lassus es."
        )
        add(
            "做完一件事只回一句「身体收到。」，不打卡、不算连续天数。",
            "Finish one thing and you get a single line back: “Your body was heard.” No check-in, no streak counting.",
            "一つ終えると「体の声を受け取りました。」と一度返すだけ。チェックインも連続日数の計算もありません。",
            "Una res perfecta unam lineam reddit: \"Corpus audivit.\" Nulla signa, nulli dies continui."
        )
        add(
            "做不动也可以什么都不点，明天的天气照样来。",
            "If you cannot manage it, tap nothing at all; tomorrow's weather comes anyway.",
            "できそうになければ何も押さなくて大丈夫です。明日の体調はいつもどおり届きます。",
            "Si nihil facere potes, nihil tange: caelum cras ut semper venit."
        )
        // ---- 22.42.0 开箱收成五页：理念页 / 用法页的合并文案 ----
        add("每天怎么用", "How to use it every day", "毎日の使い方", "Quotidie utere")
        add(
            "首页有九个校准标签，点一下就能改写今天的结论；每天还会给你一件几分钟能做完的小事。",
            "Home has nine calibration tags — tap one to rewrite today's verdict; every day it also offers one small thing you can finish in minutes.",
            "ホームには九つの校準タグがあり、押すと今日の結論を書き換えられます。毎日、数分でできる小さなこともひとつ届きます。",
            "Novem notae in domo sunt — tange et hodiernum iudicium muta; etiam res parva quotidiani minutarum offertur."
        )
        add(
            "每天问三个问题：早上看一眼今天是什么天气；中午不知道吃什么就翻食谱；周末在回声页看看这一周。",
            "Three questions a day: what's today's weather in the morning; what to eat at noon — check the food page; how was this week — see the echo page at the weekend.",
            "毎日三つの問い：朝は今日の体調をひと目、昼は食事を探すときは食事ページ、週末はエコーページで今週を振り返ります。",
            "Tres quotidianae quaestiones: mane caelum hodiernum; meridie cibum in pagina cibi quaere; fine septimanae hebdomadam in pagina echo respice."
        )
        add(
            "小事都在 10 分钟以内，不打卡、不连击、不算完成率。",
            "Every small thing takes under 10 minutes — no check-ins, no streaks, no completion rate.",
            "小さなことはすべて 10 分以内。チェックインも連続も達成率もありません。",
            "Res parvae sub 10 minutis — nulla signa, nulla series, nulla rata perfecta."
        )
        add(
            "做完只回一句「身体收到。」；做不动也可以什么都不点，明天的天气照样来。",
            "Finish it and you get one line back: “Your body was heard.” If you cannot manage it, tap nothing at all.",
            "終えたら「体の声を受け取りました。」と一度返すだけ。できそうになければ何も押さなくて大丈夫です。",
            "Perfecto unam lineam reddit: \"Corpus audivit.\" Si nihil potes, nihil tange."
        )
        add("只存在这台手机上", "Only on this phone", "この端末の中だけ", "In hoc instrumento tantum")
        add(
            "默认不联网，也没有服务器和账号。只有你主动打开「小米账号（云同步）」时应用才会联网——登录在小米官方页面完成，数据只在你自己的账号与本机之间。",
            "No network by default, and no server or account. The app only goes online when you turn on “Mi account (cloud sync)” yourself: sign-in happens on Xiaomi's official page, and the data only travels between your own account and this phone.",
            "既定では通信しません。サーバーもアカウントもありません。アプリが通信するのは、あなたが自分で「Mi アカウント（クラウド同期）」を開いたときだけです。ログインは Xiaomi の公式ページで行われ、データはご自身のアカウントとこの端末の間だけを行き来します。",
            "Rete non utitur nisi tu ipse \"Rationem Mi (synchronizatio nubis)\" aperis; ratio et servus desunt. In pagina officiali Xiaomi intras, data inter rationem tuam et hoc instrumentum tantum eunt."
        )
        add(
            "不保存原始心率、睡眠时长、血氧数值：读数进来只为算天气，算完就丢。",
            "Raw heart rate, sleep duration and blood oxygen values are not stored: readings come in only to work out the weather and are dropped once that is done.",
            "心拍数、睡眠時間、血中酸素の生の数値は保存しません。測定値は体調を出すためだけに読み、計算したら捨てます。",
            "Mensurae crudae pulsus, somni et oxygenii non servantur: tantum ad caelum computandum leguntur, deinde omittuntur."
        )
        add(
            "导出与备份都在你自己的文件里：只含日期、天气词和标签。",
            "Exports and backups live in your own files, with dates, weather words and tags only.",
            "書き出しとバックアップはあなた自身のファイルに。含まれるのは日付、体調の言葉、タグだけです。",
            "Exportatio et exemplar in fasciculis tuis sunt: diem, caelum et titulos tantum continent."
        )
        add(
            "想看细节：设置 → 隐私与安全；想清空：那里也有「删除所有数据」。",
            "For details: Settings → Privacy and security. To clear everything: “Delete all data” is there too.",
            "詳しく見る：設定 → プライバシーと安全。消したいとき：そこに「すべてのデータを削除」もあります。",
            "Pro detaliis: Optiones → Privatum et securitas. Pro deletione: ibi etiam \"Omnia data delere\"."
        )
        add("下一步", "Next", "次へ", "Deinde")
        add("数据", "Data", "データ", "Data")
        add("数据与同步", "Data & sync", "データと同期", "Data et synchronizatio")
        add(
            "手环接入、Health Connect 授权、数据来源、手动同步。",
            "Wearables, Health Connect access, data origin and manual sync.",
            "ウェアラブル接続、Health Connect の権限、データの出どころ、手動同期。",
            "Armilla, permissio Health Connect, origo datorum, synchronizatio manualis."
        )
        add("身体", "Body", "からだ", "Corpus")
        add("微行动与处方", "Micro-actions & plans", "小さな行動とプラン", "Actiones parvae et consilia")
        add("内容", "Content", "コンテンツ", "Contentum")
        add("提醒", "Reminders", "リマインダー", "Admonitiones")
        add("数据与隐私", "Data & privacy", "データとプライバシー", "Data et privatum")
        add("我的数据", "My data", "わたしのデータ", "Data mea")
        add(
            "导出到剪贴板、备份与恢复、删除所有数据。",
            "Export to clipboard, back up and restore, or delete all data.",
            "クリップボードへの書き出し、バックアップと復元、すべてのデータの削除。",
            "In clipboardum exportare, exemplar et restitutio, omnia data delere."
        )
        add("权限与说明", "Permissions & notes", "権限と説明", "Permissiones et notae")
        add(
            "授权状态、隐私说明、系统权限。",
            "Access status, privacy notes and system permissions.",
            "権限の状態、プライバシー説明、システムの権限。",
            "Status permissionis, pagina privati, permissiones systematis."
        )
        add("外观与关于", "Appearance & about", "表示とこのアプリについて", "Species et de hoc")
        add(
            "这台机器上没找到小米的运动健康类应用。",
            "No Xiaomi fitness app found on this device.",
            "この端末には Xiaomi の運動健康アプリが見つかりませんでした。",
            "In hoc instrumento nulla applicatio sanitatis Xiaomi inventa est."
        )
        add(
            "这台机器上没找到华为运动健康。",
            "No Huawei Health found on this device.",
            "この端末には Huawei Health が見つかりませんでした。",
            "In hoc instrumento Huawei Health non inventa est."
        )
        add(
            "会弹出系统的授权框，勾选要允许的类型。",
            "A system dialog will appear; tick the types you want to allow.",
            "システムの許可画面が表示されるので、許可する項目にチェックを入れます。",
            "Fenestra permissionis systematis apparebit; genera eligenda signa."
        )
        add("打不开授权框", "Cannot open the permission dialog", "許可画面を開けません", "Fenestra permissionis aperiri non potest")
        // ---- HC 写入桥（22.42.0）：把读数写入系统健康服务 ----
        add("写入系统健康服务", "Write to the system health service", "システムの健康サービスに書き込む", "Scribere in servitium sanitatis")
        add(
            "把读数写入系统健康服务",
            "Write readings to the system health service",
            "測定値をシステムの健康サービスに書き込む",
            "Scriptiones in servitium sanitatis scribe"
        )
        add(
            "打开后，本机已同步的读数会写进 Health Connect，来源标注为本应用。睡眠与运动只在有真实起止时间时写入。",
            "When on, readings already synced to this phone are written into Health Connect, labelled as coming from this app. Sleep and exercise are written only when real start and end times are known.",
            "オンにすると、この端末に同期済みの測定値が Health Connect に書き込まれます（出どころは本アプリ）。睡眠とワークアウトは実際の開始・終了時刻が分かるときだけ書き込みます。",
            "Excitato, valores iam synchroni in Health Connect scribuntur, origine hac app notata. Somnus et exercitium solum cum veris horis initii et finis scribuntur."
        )
        add("立即写入一次", "Write once now", "今すぐ一度書き込む", "Nunc semel scribe")
        add("没有拿到写入权限", "No write permission granted", "書き込み権限が得られませんでした", "Nulla permissio scribendi")
        add("开关没有打开，什么都没写。", "The switch is off; nothing was written.", "スイッチはオフのままなので、何も書き込んでいません。", "Commotio non excita est; nihil scriptum est.")
        add(
            "系统授权框里要勾选允许写入的类型；也可以在「在系统设置里改」里补授权后再打开开关。",
            "Tick the write types in the system permission dialog; or grant them under “Change in system settings”, then turn the switch on again.",
            "システムの許可画面で書き込みの種類にチェックしてください。あるいは「システム設定で変更」で許可を足してから、もう一度スイッチをオンにしてください。",
            "In fenestra permissionis genera scribendi signa; vel in \"Mutare in optionibus systematis\" concede, deinde commotionem denuo excita."
        )
        add("可以在「在系统设置里改」里手动勾选写入权限后再打开。", "Grant write permission under “Change in system settings”, then turn it on again.", "「システム設定で変更」で書き込み権限を付けてから、もう一度オンにしてください。", "Concede permissionem scribendi in \"Mutare in optionibus systematis\", deinde denuo excita.")
        add("已关闭，不会再写入。", "Turned off; nothing more will be written.", "オフにしました。これ以上は書き込みません。", "Exstinctum; nihil amplius scribet.")
        add("写入失败：先打开上面的开关并授权。", "Write failed: turn on the switch above and grant permission first.", "書き込み失敗：上のスイッチをオンにして許可してください。", "Scriptura defecit: primum commotionem supra excita et concede.")
        add("写入失败：先确认系统健康服务可用。", "Write failed: make sure the system health service is available first.", "書き込み失敗：まずシステムの健康サービスが使えるか確認してください。", "Scriptura defecit: primum confirma servitium sanitatis praesto esse.")
        add("没有可写的内容：先去同步一次读数。", "Nothing to write yet: sync your readings first.", "書き込む内容がまだありません：まず測定値を同期してください。", "Nihil ad scribendum: primum valores synchronos accipe.")
        add(
            "写的是本机已同步的聚合读数；睡眠与运动没有真实起止时间就跳过，不编造。",
            "What gets written are the aggregate readings already synced here; sleep and exercise without real start / end times are skipped, never invented.",
            "書き込むのはこの端末に同期済みの集計値です。睡眠とワークアウトは実際の時刻が無いとスキップし、でっち上げません。",
            "Scribuntur valores aggregati iam synchroni; somnus et exercitium sine veris horis praetereuntur, non confinguntur."
        )
        add(
            "先在系统里确认健康服务可用。",
            "First make sure the health service is available on this device.",
            "まずシステムで健康サービスが使えるか確認してください。",
            "Primum in systemate confirma servitium sanitatis praesto esse."
        )
        add(
            "可以点下面的「系统健康服务设置」，在那里手动勾选本应用。",
            "Tap “System health service settings” below and tick this app there.",
            "下の「システムの健康サービス設定」を押して、そこでこのアプリに手動でチェックを入れてください。",
            "Tange \"Optiones Health Connect\" infra et hanc applicationem ibi signa."
        )
        add(
            "先装好系统健康服务",
            "Install the system health service first",
            "まずシステムの健康サービスをインストール",
            "Primum Health Connect installe"
        )
        add(
            "没有它就没有授权框可以弹。",
            "Without it there is no permission dialog to show.",
            "これがないと許可画面が表示されません。",
            "Sine eo nulla fenestra permissionis apparet."
        )
        add(
            "安卓 14 及以上一般自带；安卓 13 及以下需要单独安装 Health Connect。",
            "Android 14 and up usually include it; Android 13 and below needs Health Connect installed separately.",
            "Android 14 以降はたいてい入っています。Android 13 以前は Health Connect を別途インストールしてください。"
        )
        add(
            "可以点上面的「连接状态」看怎么装。",
            "Tap “Connection status” above to see how to install it.",
            "上の「接続状態」を押すとインストール方法が確認できます。",
            "Tange \"Status conexionis\" supra ut modum installationis videas."
        )
        add("系统健康服务设置", "System health service settings", "システムの健康サービス設定", "Optiones Health Connect")
        add(
            "在 Health Connect 里手动看 / 改本应用的授权。",
            "View or change this app's access in Health Connect.",
            "Health Connect でこのアプリの権限を確認・変更できます。",
            "In Health Connect permissionem huius applicationis vide aut muta."
        )
        add(
            "这次一类都没读到",
            "No data types could be read this time",
            "今回はどの種類も読み取れませんでした",
            "Hac vice nullum genus lectum est"
        )
        add(
            "先看下面两步，通常卡在第一步。",
            "Start with the two steps below; it usually stops at the first one.",
            "まず下の二つの手順を確認してください。たいてい最初で止まっています。",
            "Duos gradus infra vide: plerumque primus impedit."
        )
        add(
            "六类数据都能读到。手环继续戴着，数据会自己更新。",
            "All six data types can be read. Keep wearing the band; the data updates on its own.",
            "六種類のデータをすべて読み取れています。ウェアラブルをつけたままでいれば、データは自動で更新されます。",
            "Omnia sex genera legi possunt. Armillam gere: data se renovant."
        )
        add(
            "本次读到的记录来自：",
            "The records read this time came from:",
            "今回読み取った記録の提供元：",
            "Notae hac vice lectae ex his veniunt:"
        )
        add(
            "导出、备份、恢复与删除，都在这一页。",
            "Export, back up, restore and delete — all on this page.",
            "書き出し、バックアップ、復元、削除はすべてこのページです。",
            "Exportatio, exemplar, restitutio, deletio: omnia in hac pagina."
        )
        add(
            "这台机器允许了它什么，以及它承诺不做什么。",
            "What this device allows it to do, and what it promises not to do.",
            "この端末が何を許可していて、アプリが何をしないと約束しているか。",
            "Quid hoc instrumentum permittat et quid applicatio promittat non facere."
        )
        add(
            "数据只存在这台手机上。应用默认不联网、没有服务器、没有账号。",
            "Data stays on this phone. By default the app has no network, no server and no account.",
            "データはこの端末の中だけにあります。既定ではアプリは通信せず、サーバーもアカウントもありません。",
            "Data in hoc instrumento manent. Applicatio rete non utitur, nullum servum, nullam rationem habet."
        )
        add(
            "本机保存的只有「今天是什么天气」和「你手动勾选的感觉标签」，不保存原始心率、睡眠、血氧数值。",
            "Only today's weather and the feeling tags you ticked by hand are kept on this phone; raw heart rate, sleep and blood oxygen values are not.",
            "端末に保存するのは「今日の体調」と「手動で選んだ感覚タグ」だけで、心拍数・睡眠・血中酸素の生の数値は保存しません。",
            "In hoc instrumento solum caelum hodiernum et tituli manu electi servantur; mensurae crudae non servantur."
        )
        add(
            "唯一会联网的是小米账号（云同步）那条可选通道，默认关闭；连接与断开都在「数据与同步」里。",
            "The only network path is the optional Mi account (cloud sync) channel, which is off by default; connect and disconnect it under Data and sync.",
            "通信するのは任意の Mi アカウント（クラウド同期）の経路だけで、既定ではオフです。接続と切断は「データと同期」で行います。",
            "Sola via retis est ratio Mi (synchronizatio nubis), default clausa; coniunge et solve in Optionibus datorum."
        )
        add("通知权限", "Notification permission", "通知の権限", "Permissio notificationum")
        add(
            "已允许。提醒会在到点时发出来。",
            "Allowed. Reminders will be sent when they are due.",
            "許可済みです。リマインダーは時間になると届きます。",
            "Permissum est. Admonitiones tempore venient."
        )
        add("健康数据授权", "Health data access", "健康データの権限", "Permissio datorum sanitatis")
        add(
            "在系统的健康服务里看 / 改本应用的读取授权。",
            "View or change this app's read access in the system health service.",
            "システムの健康サービスでこのアプリの読み取り権限を確認・変更できます。",
            "In Health Connect permissionem legendi huius applicationis vide aut muta."
        )
        add("隐私说明页", "Privacy page", "プライバシー説明ページ", "Pagina privati")
        add(
            "写清「读什么、不读什么、存在哪」的那一页。",
            "The page that spells out what is read, what is not, and where it stays.",
            "「何を読み、何を読まず、どこに保存するか」を書いたページ。",
            "Pagina quae explicat quid legatur, quid non, et ubi maneat."
        )
        add("系统权限管理", "System permissions", "システムの権限管理", "Permissiones systematis")
        add(
            "你将要打开开发者模式。里面的功能都是一些测试未完善、可能有 bug、甚至可能损坏数据的功能，正常用户请不要打开。",
            "You are about to turn on developer mode. It holds features that are not fully tested, may have bugs and could even corrupt data. Regular users should leave it off.",
            "これから開発者モードをオンにします。中にはテストが不十分で、不具合があったりデータを壊したりする可能性のある機能があります。通常のユーザーはオンにしないでください。",
            "Modum programmatoris aperturus es. In eo functiones nondum satis probatae, mendis obnoxiae, immo data laedere possunt. Usor communis ne aperiat."
        )
        add("开发者模式", "Developer mode", "開発者モード", "Modus programmatoris")
        add("确认打开", "Turn on", "オンにする", "Aperire")
        add(
            "测试信息与实验性开关；改动即时生效。",
            "Test information and experimental switches; changes take effect immediately.",
            "テスト情報と実験用のスイッチ。変更はすぐ反映されます。",
            "Notae probationis et experimenta; mutationes statim valent."
        )
        add(
            "测试未完善、可能有 bug，正常用户请不要打开。",
            "Not fully tested and may have bugs; regular users should leave it off.",
            "テストが不十分で不具合の可能性があります。通常のユーザーはオンにしないでください。",
            "Nondum satis probatum, mendis obnoxium; usor communis ne aperiat."
        )
        add("绿", "Green", "緑")
        add("蓝", "Blue", "青")
        add("紫", "Purple", "紫")
        add("橙", "Orange", "オレンジ")
        add("标准（32dp）", "Standard (32dp)", "標準（32dp）")
        add("更圆（40dp）", "Rounder (40dp)", "より丸く（40dp）")
        add("更方（24dp）", "Squarer (24dp)", "より角ばって（24dp）")
        add("更透", "More transparent", "より透明")
        add("更磨砂", "More frosted", "よりすりガラス")
        add("实验性功能", "Experimental features", "実験的な機能", "Functiones experimentales")
        add(
            "给开发者与好奇者的东西；改动即时生效。",
            "For developers and the curious; changes take effect immediately.",
            "開発者と好奇心のある人向け。変更はすぐ反映されます。",
            "Programmatoribus et curiosibus; mutationes statim valent."
        )
        add("外观实验", "Appearance experiments", "見た目の実験")
        add("强调色", "Accent color", "アクセントカラー")
        add(
            "按钮、图标与选中态的底色。绿色是原来的样子。",
            "The base color of buttons, icons and selected states. Green is how it looked before.",
            "ボタン、アイコン、選択状態の下地の色。緑が元の見た目です。"
        )
        add("圆角风格", "Corner style", "角の丸み")
        add(
            "只改主卡片的圆角，次级行与间距保持不动。",
            "Only the main cards change; secondary rows and spacing stay as they are.",
            "変わるのは主なカードの角だけです。副次的な行と余白はそのままです。"
        )
        add("玻璃强度", "Glass strength", "ガラスの強さ")
        add(
            "模糊与挂色的浓淡。调过头会牺牲可读性，看个新鲜就够。",
            "How strong the blur and tint are. Overdoing it costs readability; just enough to see the effect.",
            "ぼかしと色づきの濃さ。強くしすぎると読みにくくなるので、試す程度で十分です。"
        )
        add("底层开关", "Low-level switches", "低レベルのスイッチ")
        add("强制磨砂降级", "Force frosted fallback", "すりガラス表示を強制")
        add(
            "关掉折射着色器，统一走安卓 13 以下的磨砂外观。切换后返回其他页面即生效；当前页可能要离开再回来。",
            "Turns off the refraction shader and uses the pre-Android 13 frosted look everywhere. Takes effect once you leave this page and come back.",
            "屈折シェーダーを切り、Android 13 以前のすりガラス表示に統一します。切り替えたあと他のページに移動して戻ると反映されます。"
        )
        add("运行环境", "Environment", "実行環境")
        add("液态玻璃可用：是", "Liquid glass available: yes", "液体ガラス：使用可")
        add(
            "液态玻璃可用：否（已手动强制磨砂降级）",
            "Liquid glass available: no (frosted fallback forced by hand)",
            "液体ガラス：使用不可（手動で磨砂表示を強制）"
        )
        add(
            "液态玻璃可用：否（系统低于安卓 13，自动磨砂降级）",
            "Liquid glass available: no (system below Android 13; automatic frosted fallback)",
            "液体ガラス：使用不可（Android 13 未満のため自動で磨砂表示）"
        )
        add("测试数据", "Test data", "テストデータ")
        add("一键填入演示读数", "Fill in demo readings", "デモの測定値をまとめて入力")
        add("会覆盖当前读数，仅供预览。", "Overwrites the current readings; for preview only.", "現在の測定値を上書きします。確認用です。")
        add(
            "这个月，身体经历过什么。它不评价，只是回放。",
            "What your body went through this month. It does not judge, it only replays.",
            "今月、体に何があったか。評価はせず、ただ再生するだけです。",
            "Quid corpus hoc mense passum sit. Non iudicat, tantum repetit."
        )
        add("回声统计", "Echo stats", "記録の集計", "Summa echonis")
        add(
            "把记下的日子数出来。没有目标值，也没有达标线。",
            "Just a count of the days you recorded. No target, no pass mark.",
            "記録した日を数えます。目標値も達成ラインもありません。",
            "Dies notati numerantur. Nullus finis, nulla linea."
        )
        add(
            "记录还很少——回声会在你多记几天之后慢慢清楚起来。",
            "Not many records yet — your echo comes into focus after a few more days.",
            "まだ記録が少なめです。数日分増えると、記録は少しずつ見えてきます。",
            "Notae adhuc paucae — echo post aliquot dies clarescit."
        )
        add("本月", "This month", "今月", "Hic mensis")
        add("记录里", "All records", "記録全体", "Omnes notae")
        add("记录天数", "Days recorded", "記録した日数", "Dies notati")
        add("连续记录", "Days in a row", "連続記録", "Dies continui")
        add("从今天往回数", "Counted back from today", "今日から遡って数えます", "A hodie retro numerantur")
        add("本周对照", "This week vs. last", "今週の比較", "Haec septimana et prior")
        add(
            "最近七天和上一个七天比一比，只数天数，不做评价。",
            "The last seven days next to the seven before. Just counting days, no judgement.",
            "直近の七日間と、その前の七日日間を比べます。数えるだけで、評価はしません。",
            "Septem dies proximi cum septem prioribus comparantur: dies tantum numerantur, sine iudicio."
        )
        add(
            "这两周记录还太少，对比不出来。",
            "Too little recorded in these two weeks to compare.",
            "この二週間はまだ記録が少なく、比べられません。",
            "His duabus septimanis notae paucae sunt: comparari non possunt."
        )
        add(
            "这两周几乎一样，没有哪一类明显变多或变少。",
            "The two weeks are much the same; nothing clearly rose or fell.",
            "この二週間はほとんど同じで、大きく増えた種類も減った種類もありません。",
            "Duae septimanae paene eaedem: nihil clare crevit aut decrevit."
        )
        add(
            "先查元素，再找食物；吃法只是顺手的建议。",
            "Start with a nutrient, then find the food; how to eat it is only a suggestion.",
            "まず栄養素を調べ、それから食べ物を探します。食べ方はあくまで参考です。",
            "Primum nutrimentum, deinde cibus; modus edendi consilium tantum est."
        )
        add("元素 ↔ 食物", "Nutrient ↔ food", "栄養素 ↔ 食べ物", "Nutrimentum ↔ cibus")
        add(
            "先选一个元素看它能从哪些食物来，或者直接搜食物名。",
            "Pick a nutrient to see which foods carry it, or search for a food by name.",
            "栄養素を一つ選ぶと、どの食べ物から取れるかが見られます。食べ物の名前で直接検索してもいいです。",
            "Elige nutrimentum ut videas quibus in cibis sit, aut quaere nomen cibi."
        )
        add(
            "正在看：全部食物。点上面的元素，可以只看那一类。",
            "Showing: all foods. Tap a nutrient above to see just that group.",
            "表示中：すべての食べ物。上の栄養素を押すと、その分類だけを表示できます。",
            "Monstrantur: omnia alimenta. Nutrimentum supra tange ut unum gregem videas."
        )
        add("配套的吃法", "Putting meals together", "組み合わせの食べ方", "Cibi componendi")
        add(
            "把上面的元素和食物组合成一顿；不是必选项。",
            "Combine the nutrients and foods above into a meal; optional.",
            "上の栄養素と食べ物を一食に組み合わせます。必須ではありません。",
            "Nutrimenta et cibos supra in unum ferculum compone; non necessarium."
        )
        add(
            "想吃什么口味？选一个菜系，上面的食物列表和三餐建议都跟着换。",
            "In the mood for something? Pick a cuisine — the food list and meal ideas follow.",
            "どんな味が食べたいですか？料理の種類を選ぶと、上の食べ物一覧と三食の提案も変わります。",
            "Quis sapor? Elige genus cibi: index alimentorum et consilia sequentur."
        )
        add("今天的三餐", "Today's three meals", "今日の三食", "Tres cibi hodierni")
        add("睡眠", "Sleep", "睡眠", "Somnus")
        add(
            "这周还没有足够记录。今天记一天，周日就有信了。",
            "Not enough recorded this week. Record one day today, and the letter will arrive on Sunday.",
            "今週はまだ記録が足りません。今日一日記せば、日曜に手紙が届きます。",
            "Nondum satis notarum hac septimana. Unum diem hodie nota: die Solis epistula aderit."
        )
    }

    // ------------------------------------------------------------------
    // 22.19.0：语言选择浮层与漏翻审计新增的串
    // ------------------------------------------------------------------
    init {
        add("跟随系统（日本語）", "Follow system (Japanese)", "システムに合わせる（日本語）", "Systema sequi (日本語)")
        add(
            "跟随系统：中文→简体中文，日语→日本語，其他→英语；拉丁文需手动选择。",
            "Follow system: Chinese shows Simplified Chinese, Japanese shows Japanese, everything else shows English. Latin must be picked by hand.",
            "システムに合わせる場合：中国語は簡体字中国語、日本語は日本語、その他は英語になります。ラテン語は手動で選択してください。",
            "Systema sequi: 简体中文, 日本語, English; Latina manu."
        )
        // 语言浮层里的选项名走翻译层，所以要给恒等词条，
        // 否则每打开一次浮层就往漏翻审计里塞一条假阳性。
        add("English", "English", "English", "English")
        add("日本語", "Japanese", "日本語", "日本語")
        add("Latina", "Latin", "Latina", "Latina")
        add(
            "本机没有可用的语音，暂时开不了",
            "No usable voice is installed on this device, so this cannot be turned on yet",
            "この端末に使える音声がないため、今はオンにできません",
            "In hoc instrumento vox praesto non est; adhuc aperiri non potest"
        )
        add("漏翻审计", "Translation gaps", "未翻訳の検出", "Lacunae translationis")
        // 机械审计（把 UI 里的中文字面量和词条表对差集）捞到的硬编码串：
        // 它们从没进过任何表，界面上直接漏出中文，只有对差集才找得到。
        add("打开开发者模式", "Turn on developer mode", "開発者モードをオンにする", "Modum programmatoris aperire")
        add("界面皮肤", "Interface skin", "界面スキン", "Species interfaciei")
        add("液态玻璃", "Liquid glass", "液体ガラス", "Vitrum liquidum")
        add("谷歌 Material 3", "Google Material 3", "Google Material 3", "Google Material 3")
        add(
            "液态玻璃是原来的样子；谷歌皮肤是标准 Material 3 外观（底栏只有点击，没有横滑切页）。",
            "Liquid glass is how it looked before; the Google skin is the standard Material 3 look (bottom bar taps only, no swipe between pages).",
            "液体ガラスが元の見た目です。Google スキンは標準の Material 3 表示になります（下部バーはタップのみで、横スワイプのページ切り替えはありません）。"
        )
        add(
            "还没翻的条目会回退成英语；这里列出当前语言检测到的缺口。",
            "Untranslated entries fall back to English; these are the gaps detected in the current language.",
            "未翻訳の項目は英語で表示されます。ここには現在の言語で検出された不足分を並べます。",
            "Res nondum translatae ad Anglicam redeunt; hic lacunae linguae hodiernae monstrantur."
        )
        add("逐句表缺口", "Sheet gaps", "逐語表の不足", "Lacunae tabulae")
        add("兜底回退", "Fallbacks", "フォールバック", "Recursus ad linguam")
        add("清空", "Clear", "クリア", "Purgare")
        add("当前没有检测到缺口。", "No gaps detected right now.", "現在、不足は検出されていません。", "Nullae lacunae nunc detectae.")
    }

    // ------------------------------------------------------------------
    // 22.21.0：开箱引导里选界面风格 + 主题色
    // ------------------------------------------------------------------
    init {
        add("挑一个界面风格", "Pick a look", "画面のスタイルを選ぶ", "Elige formam")
        add(
            "两套界面随时可以换：液态玻璃是应用原本的样子；谷歌 Material 3 是标准 Material 外观，主题色可以自己挑。",
            "Both looks can be swapped at any time: liquid glass is how the app originally looked; Google Material 3 is the standard Material look, with a theme color you can pick yourself.",
            "どちらの画面もいつでも切り替えられます。液体ガラスは元の見た目、Google Material 3 は標準の Material 表示で、テーマカラーも選べます。",
            "Uterque aspectus semper mutari potest: vitrum liquidum est forma originalis; Google Material 3 est forma Material standard, colore thematis electo."
        )
        add("主题色", "Theme color", "テーマカラー", "Color thematis")
        add(
            "以后可以在「设置 → 外观与体验」里换。",
            "You can change this later in Settings → Appearance and experience.",
            "あとから「設定 → 外観と操作感」で変更できます。",
            "Postea in Optionibus → Species et usus mutari potest."
        )
    }

    // ------------------------------------------------------------------
    // 22.22.0：Health Connect 自动通道做透
    // 数据流水线自检（四步三态）、四种失败、厂商同步路径、桥接引导。
    // 口径铁律：不承诺「华为手环也能读通」；厂商 App 打开 ≠ 数据已写入；
    // 授权前的历史不补写；手动校准优先级永远最高。
    // ------------------------------------------------------------------
    init {
        // ---- 三态与列头 ----
        add("通", "OK", "正常", "Bene")
        add("断", "Broken", "切断", "Fractum")
        add("未知", "Unknown", "不明", "Ignotum")
        add("无记录", "No record", "記録なし", "Nulla nota")
        add("估算", "Estimated", "推定", "Aestimatum")
        add("今天", "Today", "今日", "Hodie")
        add("昨天", "Yesterday", "昨日", "Heri")

        // ---- 来源名 ----
        add("小米运动健康（国行）", "Mi Fitness (China)", "Mi Fitness（中国版）", "Mi Fitness (Sinarum)")
        add(
            "小米运动健康（国际版 / Mi Fitness）",
            "Mi Fitness (global)",
            "Mi Fitness（国際版）",
            "Mi Fitness (internationale)"
        )
        add("Zepp Life / 小米运动（老版）", "Zepp Life / Mi Fit (legacy)", "Zepp Life / Mi Fit（旧版）", "Zepp Life / Mi Fit (vetus)")
        add("OPPO 健康", "OPPO Health", "OPPO ヘルスケア", "OPPO Sanitas")

        // ---- 自检卡片本体 ----
        add("数据流水线自检", "Data pipeline self-check", "データ経路の自己診断", "Diagnosis rationis datorum")
        add(
            "四步都通，数据才会自动进来。",
            "All four steps must be OK before data arrives on its own.",
            "四つの手順がすべて通って初めて、データは自動で入ってきます。",
            "Omnes quattuor gradus valere debent ut data sponte veniant."
        )

        // ---- 22.30.0 授权向导（SetupGuidePage）----
        add("授权向导", "Setup guide", "セットアップガイド", "Ductor auctoramenti")
        add(
            "手环数据的完整接法：厂商 App → 系统健康服务 → BodyCast。",
            "The full chain for band data: vendor app → system health service → BodyCast.",
            "バンドデータの流れ：メーカー App → システム健康サービス → BodyCast。",
            "Via datorum armillae: app fabricatoris → servitium sanitatis → BodyCast."
        )
        add("正在检查你走到哪一步了…", "Checking which step you are on…", "いまどの段階か確認しています…", "Inquisitio quod gradus es…")
        add("已经通了：数据在自动进来。", "All set: data is flowing in by itself.", "完了です。データは自動で入ってきます。", "Perfectum: data sponte veniunt.")
        add("从第 ① 步开始。", "Start from step ①.", "① から始めましょう。", "Incipe a gradu ①.")
        add("第 ① 步已通，做第 ② 步。", "Step ① is done; go to step ②.", "① が完了。次は ② です。", "Gradus ① perfectus; fac gradum ②.")
        add("前两步已通，做第 ③ 步（最容易漏的一步）。", "Steps ① and ② are done; go to step ③ (the one most often missed).", "①② が完了。次は ③ です（一番見落とされやすい段階）。", "Gradus ① et ② perfecti; fac gradum ③ (maxime omissum).")
        add("前三步已通，做第 ④ 步验证。", "The first three steps are done; verify in step ④.", "①②③ が完了。最後に ④ で確認しましょう。", "Tres gradus perfecti; confirma in gradu ④.")
        add(
            "链路是：手环 → 厂商运动健康 App → 系统健康服务（Health Connect）→ BodyCast。任何一环断了，数据都进不来。",
            "The chain: band → vendor health app → system health service (Health Connect) → BodyCast. If any link breaks, no data gets through.",
            "流れ：バンド → メーカーの健康 App → システム健康サービス（Health Connect）→ BodyCast。どこかが切れるとデータは入りません。",
            "Catena: armilla → app sanitatis fabricatoris → servitium sanitatis (Health Connect) → BodyCast. Si quaelibet rumpitur, data non veniunt."
        )
        add("② 给 BodyCast 授权", "② Grant access to BodyCast", "② BodyCast に許可する", "② Permittere BodyCast")
        add("同步并查看结果", "Sync and check the result", "同期して結果を見る", "Synchroniza et resultatum vide")
        add("详细状态都在下面，也写回了自检卡。", "Details are below, and the self-check card has been updated too.", "詳しい状態は下にあります。自己診断カードも更新されました。", "Status infra est; etiam tabula probationis renovata est.")
        add("厂商还没把数据写进来", "The vendor has not written any data yet", "メーカーがまだデータを書き込んでいません", "Fabricator nondum data scripsit")
        add("走不通也没关系", "If any step does not work, that is fine", "どこかで詰まっても大丈夫です", "Si quid non valet, nihil refert")
        add("支持写入系统健康服务——去它设置里把开关打开", "Can write to the system health service — turn it on in its settings", "システム健康サービスに書き込めます。App の設定でスイッチを入れてください", "Scribere potest in servitium sanitatis — in optionibus eius excita")
        add("这个版本不写系统健康服务——看下面的替代方案", "This version does not write to the system health service — see the alternatives below", "このバージョンはシステム健康サービスに書き込みません。下の代替案を見てください", "Haec versio non scribit in servitium sanitatis — vide alternativa infra")
        add("没查到它的写入能力（不影响使用）", "Could not check its write support (does not affect usage)", "書き込み可否を確認できませんでした（使用には影響しません）", "Facultas scribendi non cognita (usum non turbat)")
        add("去商店找厂商 App", "Find the vendor app in the app store", "アプリストアでメーカー App を探す", "Quaere app fabricatoris in taberna")
        add("同步一次手环再回来——厂商一般只写新产生的数据，授权前的历史不补写。", "Sync your band once, then come back — vendors usually write new data only; history from before authorization is not backfilled.", "バンドを一度同期して戻ってください。メーカーは通常、新しく発生したデータだけを書き込みます。許可前の履歴は補完されません。", "Synchroniza armillam semel, deinde redi — fabricatores fere nova data solum scribunt; historiam ante permissionem non supplet.")
        add("打开厂商 App 不等于数据进来了——要去它自己的设置里打开「数据共享 / Health Connect」开关，数据才会写进系统健康服务。", "Opening the vendor app is not enough — you must turn on its \"Data sharing / Health Connect\" switch before data reaches the system health service.", "メーカー App を開くだけでは足りません。App 自身の設定で「データ共有 / Health Connect」のスイッチを入れて初めて、システム健康サービスに書き込まれます。", "Aperire app fabricatoris non satis est — excita commutatorem \"Communicatio datorum / Health Connect\" in optionibus eius ut data in servitium sanitatis scribantur.")
        add("第 ③ 步还没通：回上面看看厂商 App 的开关路径。", "Step ③ is not done yet: scroll up for the vendor app switch path.", "③ がまだ完了していません。上でメーカー App の設定場所を確認してください。", "Gradus ③ nondum perfectus: supra vide viam commutatoris app fabricatoris.")
        add("按上面四步走一遍，每一步的状态都会自动刷新。", "Walk through the four steps above; each step refreshes itself.", "上の四つの手順を順に進めれば、各段階の状態は自動で更新されます。", "Per quattuor gradus supra perage; status cuiusque gradus sponte renovatur.")
        add("不写系统健康服务的厂商（华为 / 荣耀是已知情况），有两条替代路：", "For vendors that do not write to the system health service (Huawei / Honor are known cases), there are two alternatives:", "システム健康サービスに書き込まないメーカー（HUAWEI / HONOR は既知のケース）には二つの代替策があります：", "Pro fabricatoribus qui non scribunt in servitium sanitatis (HUAWEI / HONOR nota sunt), duae viae alternativae:")
        add("厂商 App → 系统健康服务 → BodyCast。", "Vendor app → system health service → BodyCast.", "メーカー App → システム健康サービス → BodyCast。", "App fabricatoris → servitium sanitatis → BodyCast.")

        add(
            "第一次接入手环数据：一步步带你从厂商 App 接到本应用。",
            "First time connecting band data: a step-by-step path from the vendor app into this app.",
            "バンドデータを初めて接続する方へ：メーカー App から本アプリまで段階的に案内します。",
            "Prima coniunctio datorum armillae: gradatim ab app fabricatoris ad hanc applicationem."
        )
        add("装完回到这一页，下一步会自动亮起来。", "Come back to this page after installing; the next step lights up by itself.", "インストールしたらこのページに戻ってください。次の手順が自動的に点灯します。", "Post installationem redi huc; gradus sequens sponte lucet.")
        add(
            "在弹出的系统框里把要读的类型都勾上。这一步只读取，不写入。",
            "Tick all the types you want to read in the system dialog. This step only reads, never writes.",
            "システムの画面で読み取りたい種類をすべてチェックしてください。この手順は読み取りだけで、書き込みはしません。",
            "In fenestra systematis genera omnia quae vis lege signa. Hic gradus solum legit, nihil scribit."
        )
        add(
            "授权框只有系统健康服务能弹出来——厂商 App 替不了这一步。",
            "Only the system health service can show the permission dialog — a vendor app cannot do this step for you.",
            "許可の画面を出せるのはシステム健康サービスだけです。メーカー App はこの段階を代わりにできません。",
            "Solum servitium sanitatis fenestram permissionis ostendere potest — app fabricatoris hunc gradum supplere non potest."
        )
        add(
            "这台机器上没找到手环厂商的运动健康 App。先装上它、登录手环账号、确认数据在它里面能看到，再回来。",
            "No band vendor health app was found on this device. Install it, sign in to your band account, make sure the data shows up there, then come back.",
            "この端末にバンドメーカーの健康 App が見つかりません。まずインストールしてバンドアカウントでログインし、データが表示されることを確認してから戻ってください。",
            "Nulla app sanitatis fabricatoris inventa est. Eam installe, rationem armillae intras, data ibi apparere confirma, deinde redi."
        )
        add(
            "从厂商 App 同步完回来，点下面的按钮看结果。每类读数的来源和最后写入时间都会列出来。",
            "After syncing in the vendor app, come back and tap the button below. The source and last write time of every type are listed.",
            "メーカー App で同期したら戻って下のボタンを押してください。各データの送信元と最終書き込み時刻が表示されます。",
            "Post synchronisationem in app fabricatoris redi et puga infra. Origo et ultimum tempus scripturae cuiusque generis monstrantur."
        )
        add(
            "数据会随厂商写入自动更新，不用再来回点。",
            "Data updates by itself as the vendor writes; no need to tap back and forth.",
            "メーカーが書き込めばデータは自動で更新されます。何度も切り替えて確認する必要はありません。",
            "Data sponte renovantur dum fabricator scribit; non opus est saepe tangere."
        )
        add(
            "厂商不配合、机器太老、不想授权——任何一步卡住，下面三条路都不需要 Health Connect：",
            "Vendor uncooperative, device too old, or you would rather not grant access — if any step blocks you, these three ways need no Health Connect:",
            "メーカーが非対応、端末が古い、許可したくない——どの段階でつまずいても、次の三つは Health Connect 不要です：",
            "Fabricator non cooperatur, machina vetus, permissio non placet — si quid obstat, hae tres viae Health Connect non requirunt:"
        )
        add(
            "手动填写读数：照手环 App 里的数字抄进来，24 小时内有效。",
            "Enter readings by hand: copy the numbers from the band app; valid for 24 hours.",
            "手動入力：バンド App の数字を書き写してください。24 時間有効です。",
            "Lectiones manu intra: numeros ex app armillae transcribe; per 24 horas valent."
        )
        add(
            "导入数据文件：厂商导出的 CSV / JSON，或从分享面板发给本应用。",
            "Import a data file: CSV / JSON exported by the vendor, or send it here from the share sheet.",
            "データファイルの読み込み：メーカーが書き出した CSV / JSON、または共有画面から本アプリへ送ってください。",
            "Importa fasciculum datorum: CSV / JSON a fabricatore exportatum, vel ex tabula communicationis huc mitte."
        )
        add(
            "手动校准：勾选现在的感觉就够了，优先级永远高于设备读数。",
            "Manual calibration: just tick how you feel right now; it always outranks device readings.",
            "手動調整：いまの感じにチェックするだけ。デバイスの測定値より常に優先されます。",
            "Calibratio manuali: signa quomodo te nunc sentis; semper mensuras praecedit."
        )
        add(
            "这条链路的每一步都在本机完成。应用不上传任何数据，也不保存原始生理数值。",
            "Every step of this chain happens on your device. Nothing is uploaded, and no raw physiological values are stored.",
            "この流れのすべての段階は端末内で完結します。データはアップロードせず、生の生体値も保存しません。",
            "Omnis huius catenae gradus in machina perficitur. Nihil subvehitur, nec valores physiologici crudi servantur."
        )

        add("③ 厂商 App 写数据", "③ Vendor app writes data", "③ メーカー App がデータを書き込む", "③ App fabricatoris data scribit")
        add("④ 回来验证", "④ Come back and verify", "④ 戻って確認する", "④ Redi et confirma")
        add("最近 7 天没有厂商写入", "No vendor writes in the last 7 days", "直近 7 日間にメーカーの書き込みはありません", "Nulla scriptura fabricatoris his 7 diebus")
        add(
            "安卓 14 及以上系统自带；安卓 13 及以下要在应用商店装一个 Health Connect。",
            "Built in on Android 14 and above; on Android 13 and below, install Health Connect from the app store.",
            "Android 14 以降はシステム標準です。Android 13 以下ではアプリストアから Health Connect をインストールしてください。",
            "In Android 14 et supra inest; in Android 13 et infra Health Connect ex taberna installe."
        )

        // ---- 22.31.0 开箱引导重排（七页 / 上一步 / 仅绑定步可跳）----


        // ---- 22.32.0 食谱闭环（就吃这个 / 想吃的）----
        add("就吃这个", "I'll have this", "これにする", "Hoc edam")
        add("记下了。", "Noted.", "記録しました。", "Notatum est.")
        add("回声页的「这周吃了什么」里能看到它。", "You’ll find it on the Echo page under “What you ate this week”.", "Echo ページの「今週食べたもの」で見つけられます。", "In pagina Echo sub 'Quae edisti' invenies.")
        add(
            "想留着以后吃的，去食物详情里点「收进想吃的」。",
            "Want to save it for later? Tap “Add to my list” in the food details.",
            "また食べたいときは、食材の詳細で「食べたいリストに入れる」を押してください。",
            "Servare vis? Tange 'In desideriis pone' in ciborum singulis."
        )
        add("收进想吃的", "Add to my list", "食べたいリストに入れる", "In desideriis pone")
        add("已收进想吃的（再点移出）", "On my list (tap again to remove)", "リストに入っています（もう一度押すと外れます）", "In desideriis est (iterum tange ut removeas)")
        add("想吃的", "My list", "食べたいリスト", "Desideria mea")
        add(
            "做饭前看一眼就是购物清单。点名字旁边的小叉移出。",
            "Glance at it before cooking — it is your shopping list. Tap the small cross next to a name to remove it.",
            "料理の前に見れば買い物リストになります。名前の横の小さな叉で外せます。",
            "Ante coctionem vide — index emptionum est. Crucem parvam tange ut removeas."
        )
        add(
            "「想吃的」清单在食谱页顶部，做饭前看一眼就是购物清单。",
            "Your list is at the top of the Food page — glance at it before cooking.",
            "リストは食事ページの上部にあります。料理の前に見てください。",
            "Index in pagina cibi superius est — ante coctionem vide."
        )
        // ---- 22.32.0 回声：这周吃了什么 ----
        add("这周吃了什么", "What you ate this week", "今週食べたもの", "Quae edisti hac septimana")
        add("只是把按过的记下来，不评价，也不打分。", "It only notes what you tapped — no judgement, no scoring.", "押したものを記録するだけです。評価も採点もありません。", "Solum quod tangis notat — nec iudicium nec puncta.")
        add(
            "这周还没有记过「就吃这个」。在食谱页的三餐卡片上按一下，这里就能看到。",
            "Nothing logged yet this week. Tap “I’ll have this” on a meal card in the Food page and it shows up here.",
            "今週はまだ何も記録がありません。食事ページの食事カードで「これにする」を押すとここに出ます。",
            "Nihil adhuc notatum. Tange 'Hoc edam' in pagina cibi et hic apparebit."
        )
        // ---- 22.32.0 回声：模式洞察 ----
        add("它注意到", "What it noticed", "気づいたこと", "Quae animadvertit")
        add(
            "只说观察，不下诊断。记录多了，这里会更懂你。",
            "Observations only, never diagnoses. The more you record, the better it knows you.",
            "観察だけで、診断はしません。記録が多いほど、あなたをより理解できます。",
            "Observationes solum, numquam diagnoses. Quo plura notas, eo melius te novit."
        )
        // ---- 星期词（洞察槽位用）----
        add("周一", "Monday", "月曜日", "Dies Lunae")
        add("周二", "Tuesday", "火曜日", "Dies Martis")
        add("周三", "Wednesday", "水曜日", "Dies Mercurii")
        add("周四", "Thursday", "木曜日", "Dies Iovis")
        add("周五", "Friday", "金曜日", "Dies Veneris")
        add("周六", "Saturday", "土曜日", "Dies Saturni")
        add("周日", "Sunday", "日曜日", "Dies Solis")

        add("选一个语言", "Pick a language", "言語を選ぶ", "Elige linguam")
        add(
            "随时可以在「设置 → 外观与体验」里换。",
            "You can change this anytime in Settings → Appearance and experience.",
            "「設定 → 表示と操作」でいつでも変えられます。",
            "Mutare potes in Optionibus → Species et usus."
        )
        add("随时可在设置里换。", "Change it anytime in Settings.", "設定でいつでも変えられます。", "In Optionibus mutare potes.")
        // ---- 22.47.0：开箱文案精简 / 末页欢迎 ----
        add(
            "把手环数据，说成一句人人都懂的话。",
            "Band data, in one plain sentence.",
            "ウェアラブルデータを、ひと言で。",
            "Data armillae, verbis simplicibus."
        )
        add(
            "读不到就写「暂时没读到」，不用 0 冒充读数。",
            "If it can't be read, it says “not available yet” — never a 0.",
            "読めなければ「まだ読み取れません」— 0 で偽りません。",
            "Si non legitur, \"nondum\" dicit — numquam 0."
        )
        add(
            "全部在本机完成：没有服务器。",
            "All on this device: no server.",
            "すべて端末内：サーバーなし。",
            "Omnia in hoc instrumento: nullus servus."
        )
        add("完成", "Done", "完了", "Peractum")
        add("欢迎使用", "Welcome", "ようこそ", "Salve")
        // ---- 22.49.0：开箱末页（欢迎/装好了）、语音播报、回声「做过的小事」 ----
        add("装好了", "Ready to go", "準備できました", "Paratum est")
        add(
            "每天打开看一眼今天是什么天气就行；想改什么、想连手环，都在设置里。",
            "Open it once a day to see what today feels like; anything you want to change — including connecting your band — lives in Settings.",
            "毎日一度開いて、今日の体調を見るだけで十分です。変えたいことやバンドの接続は設定にまとめてあります。",
            "Semel in die aperi ut caelum hodie videas; quicquid mutare vis — etiam armillam coniungere — in Optionibus est."
        )
        add("语音播报还差一步", "Voice needs one more step", "音声まであと一歩", "Voci unus gradus deest")
        add(
            "本机还没有念得动这一种语言的音色。",
            "This device has no voice for this language yet.",
            "この端末には、この言語を読み上げる音声がまだありません。",
            "Huic instrumento deest vox quae hanc linguam legat."
        )
        add(
            "系统朗读需要两样东西：一个语音合成引擎（系统里通常叫「语音服务」或「Google 语音服务」），以及这个语言的语音包。",
            "System speech needs two things: a speech engine (usually listed as “Speech Services” or “Google Speech Services”) and a voice pack for this language.",
            "読み上げには二つ必要です。音声合成エンジン（通常「音声サービス」や「Google 音声サービス」）と、この言語の音声データです。",
            "Oratio systematis duo requirit: machinam vocis (plerumque “Servitia vocis” vel “Servitia vocis Google”) et fasciculum vocis huius linguae."
        )
        add(
            "装好之后回到这里，这一行会自己变成「只听身体播报」。",
            "Once that is installed, come back here — this row turns into “Listen to the body report”.",
            "インストール後にここへ戻ると、この行が「体のレポートを聞く」に変わります。",
            "Postquam id installatum est, huc redi — hic versus in “Audi relationem corporis” vertitur."
        )
        add("打开系统语音设置", "Open system speech settings", "システムの音声設定を開く", "Aperi optiones vocis systematis")
        add("本机没有可用的语音引擎", "No speech engine on this device", "使える音声エンジンがありません", "Nulla machina vocis in hoc instrumento")
        add("这台设备缺少当前语言的语音包", "This device is missing the voice pack for this language", "この言語の音声データがありません", "Deest fasciculus vocis huius linguae")
        add("这周做过的小事", "Small things done this week", "今週やった小さなこと", "Parvae res hac septimana gestae")
        add("两条路怎么走", "How the two routes work", "二つの経路のしくみ", "Quomodo duae viae operantur")
        add("正在朗读…", "Reading aloud…", "読み上げ中…", "Recitatio…")
        add("每一步会念出来，做的时候不用看屏幕。", "Every step is read out loud, so you need not look at the screen.", "各ステップを読み上げます。画面を見なくても大丈夫です。", "Singuli gradus recitantur, ut ad ecranem non respicias.")
        // ---- 22.49.0：开箱新增两页（忌口 / 称呼）与末页 ----
        add("有什么忌口吗", "Anything you avoid eating?", "食べられないものはありますか", "Quid vitare soles in cibis?")
        add("勾上的这几类不会出现在食谱推荐里；没有就空着过去，随时能改。", "What you tick here stays out of the recipe suggestions; skip it if there is nothing, and you can change it later.", "チェックしたものは食事の提案に出てきません。なければそのまま次へ、あとで変えられます。", "Quae hic signas e suggestione ciborum exeunt; si nihil est, transi — postea mutare potes.")
        add("饮食偏好", "Diet preferences", "食事の好み", "Praeferentiae victus")
        add("设置 → 食谱指南里还能加上「过敏原提醒」。", "In Settings → Food guide you can also turn on allergen notes.", "設定 → 食事ガイドで「アレルゲン表示」もオンにできます。", "In Optionibus → ductor cibi etiam monita allergenorum accendere potes.")
        add("怎么称呼你", "What should we call you?", "何と呼べばいいですか", "Quomodo te appellem?")
        add("写信、分享卡和首页问候都用它；1–20 个字，随时能改。", "It goes into the letter, the share card and the home greeting; 1–20 characters, changeable later.", "手紙・共有カード・ホームの挨拶に使います。1〜20 文字、あとで変更できます。", "In epistula, tabula communicationis et salutatione domus adhibetur; 1–20 litterae, postea mutabiles.")
        add("比如：小林", "For example: Sam", "例：さくら", "Exempli gratia: Linus")
        add("立即开始", "Start now", "すぐ始める", "Incipe nunc")
        add("几分钟能做完的一件小事。", "One small thing that takes minutes.", "数分でできる小さなこと。", "Una res parva minutis peracta.")
        add("今天的数据", "Today's data", "今日のデータ", "Data hodierna")
        add("手环读到的原始数值，只陈述，不评分。", "The raw numbers from your band — stated, never scored.", "バンドが読んだ生の数値です。述べるだけで、評価はしません。", "Valores crudi armillae — narrantur, non aestimantur.")
        add("还没读到", "Nothing yet", "まだ読み取れていません", "Nondum lecta")
        add("还没读到任何读数。连上手环数据（或手动填一次）之后，这里就是今天的第一屏。", "No readings yet. Connect your band data (or enter them once by hand) and this becomes the first thing you see today.", "まだ何も読み取れていません。バンドのデータをつなぐ（または一度手入力する）と、ここが今日の最初の画面になります。", "Nihil adhuc lectum. Coniunge data armillae (vel semel manu scribe) — tum hic primus conspectus hodie erit.")
        add("去连接运动数据", "Connect activity data", "運動データをつなぐ", "Coniunge data motus")
        add("精力", "Energy", "元気", "Vis")
        add("这一周的样子", "How the week looked", "今週のようす", "Species huius septimanae")
        add("留着或发出去", "Keep it or share it", "残すか共有する", "Serva vel mitte")
        add("在回声页：几分钟能做完的一件小事。", "On the Echo page: one small thing that takes minutes.", "記録ページにあります：数分でできる小さなこと。", "In pagina Echo: una res parva minutis peracta.")
        // ---- 22.49.0：处方按运动量倾斜的两句说明 ----
        add("今天已经动得够多了，这几件都偏向放松。", "You have moved plenty today — these lean gentle.", "今日はもう十分動いています。どれもゆるめです。", "Hodie satis motum est — haec leniora sunt.")
        add("今天几乎没怎么动，先挑要起身的那一件。", "Hardly any movement today — start with the one that gets you up.", "今日はほとんど動いていません。まず立ち上がるものを一つ。", "Hodie vix motum est — elige quod te erigit.")
        add("授权要做的几件事", "What the permission needs", "許可で必要なこと", "Quae permissio requirit")
        add("这张表怎么看", "How to read this table", "この表の見方", "Quomodo haec tabula legitur")
        add(
            "连上之后应用才会去小米取你自己账号里的数据；登录在小米官方页面完成。",
            "Only after you connect does the app fetch the data in your own account from Xiaomi; sign-in happens on Xiaomi’s official page.",
            "接続してはじめて、アプリがあなたのアカウントのデータを Xiaomi から取得します。ログインは Xiaomi の公式ページで行います。",
            "Post coniunctionem app data rationis tuae a Xiaomi petit; intra pagina officiali Xiaomi fit."
        )
        add(
            "只是把点完的记下来：没有目标，也没有连击。",
            "It only notes what you finished — no goal, no streak.",
            "終えたものを記録するだけです。目標も連続記録もありません。",
            "Solum notat quae perfecisti — nec meta nec series."
        )
        add(
            "这周还没有做完过微行动。首页那张「今日微行动」卡点开、做完一件，这里就会留下记录。",
            "No finished micro-action this week yet. Open the “Today’s micro-action” card on the home page, finish one, and it shows up here.",
            "今週はまだ小さな行動を終えていません。ホームの「今日の小さな行動」カードを開いて一つ終えると、ここに記録が残ります。",
            "Hac septimana nulla actio parva perfecta est. Aperi “Actio parva hodie” in pagina domus, unam perfice, et hic apparebit."
        )
        add("BodyCast · 身体气象台", "BodyCast · body weather station", "BodyCast・身体気象台", "BodyCast · statio caeli corporis")
        add("今天的天气变了", "Today’s weather changed", "今日の天気が変わりました", "Caelum hodie mutatum est")
        add("上一步", "Back", "前へ", "Retro")
        add(
            "把身体状态翻译成天气",
            "Body state, translated into weather",
            "体調を天気に翻訳する",
            "Status corporis in caelum translatum"
        )
        add(
            "BodyCast 读手环数据，把它说成一句人人都懂的话：今天是什么天气、今天先做这一件、今天吃什么。",
            "BodyCast reads your band data and turns it into plain words: what today feels like, the one thing to do first, and what to eat.",
            "BodyCast はバンドのデータを読み、誰にでも分かる言葉に変えます。今日の体調、最初にこれ、今日の食事。",
            "BodyCast data armillae legit et in verba simplicia vertit: quale hodie, quid primum, quid edas."
        )
        add("不打分、不排名、不制造焦虑。", "No scores, no rankings, no anxiety.", "点数も順位も不安も作りません。", "Sine punctis, sine ordinibus, sine anxietate.")
        add(
            "读不到数据就写「暂时没读到」，绝不用 0 冒充真实读数。",
            "If a reading cannot be found, it says so — a real reading is never faked with 0.",
            "読み取れないものは「まだ読み取れません」と表示します。0 でごまかすことはしません。",
            "Si lectio inveniri non potest, dicitur — numquam 0 fingitur."
        )
        add("全部在本机完成：没有服务器，也没有账号。", "Everything happens on this device: no server, no account.", "すべて端末内で完結します。サーバーもアカウントもありません。", "Omnia in machina fiunt: nec servitus nec ratio.")
        add("挑一个主题色", "Pick an accent color", "テーマカラーを選ぶ", "Elige colorem accentus")
        add("每天的三个问题", "Three questions every day", "毎日の三つの問い", "Tres quaestiones cotidianae")
        add("首页：今天的天气、一句结论、一件几分钟能做完的小事。", "Home: today’s weather, one conclusion, and one small thing you can finish in minutes.", "ホーム：今日の天気、一つの結論、数分で終わる小さなこと一つ。", "Domus: caelum hodie, una conclusio, una parva res minuta.")
        add("食谱：按身体需要的元素找食物——今天压力大，就找含镁的食物。", "Food: find what your body needs — on a high-pressure day, look for foods with magnesium.", "食事：体に必要な栄養素から食材を探す——プレッシャーが大きい日はマグネシウムを含む食材を。", "Cibus: quaere quod corpus postulat — die pressurae magnum cibos cum magnesium.")
        add("回声：一周的天气日历、身体来信，和「本周你照顾自己几次」。", "Echo: the week’s weather calendar, letters from your body, and how many times you cared for yourself.", "Echo：一週間の天気カレンダー、体からの手紙、「今週、自分を大切にした回数」。", "Echo: calendarium caeli septimanae, epistulae corporis, et quoties te curavisti.")
        add(
            "早上看一眼今天是什么天气；中午不知道吃什么就翻食谱；周末在回声页看看这一周。",
            "Check the weather in the morning; open the food guide when lunch is undecided; look back at the week on the Echo page.",
            "朝に今日の天気を確認。お昼に迷ったら食事ガイドを。週末は Echo ページでこの一週間を振り返ります。",
            "Mane caelum inspice; meridie cibum elige; fine septimanae paginam Echo vide."
        )
        add("今天的这一件小事", "Today’s one small thing", "今日の一つの小さなこと", "Una parva res hodie")
        add(
            "首页会给你一件几分钟能做完的小事：喝杯温水、闭眼三分钟、拉伸肩颈。",
            "The home page gives you one small thing that takes minutes: warm water, three minutes with eyes closed, a neck stretch.",
            "ホームには数分でできる小さなことが一つ届きます。温水を飲む、目を閉じて三分、首を伸ばす。",
            "Pagina principalis rem parvam dat: aquam tepidam, tribus minutis oculis clausis, collum extendere."
        )
        add("都在 10 分钟以内，不需要器械——「做不完」最伤信任。", "All within 10 minutes, no equipment — nothing erodes trust like an unfinished task.", "すべて 10 分以内、道具も不要。「終わらない」ほど信頼を損なうものはありません。", "Omnia intra 10 minuta, sine instrumentis — nihil fidem magis laedit quam imperfectum.")
        add("不打卡、不连击、不算完成率。", "No check-ins, no streaks, no completion rates.", "チェックインも連続記録も達成率もありません。", "Nec signa nec series nec rata perfecta.")
        add("做完说「更累了」，下次自动排得更短更温和。", "Say it felt heavier and the next one is automatically shorter and gentler.", "「もっと疲れたら」と言えば、次は自動的に短く穏やかになります。", "Si dicis 'fessius', sequens brevior mitiorque fit.")
        add(
            "手环 → 厂商的运动健康 App → 系统健康服务 → 这里。应用不直连手环、不用蓝牙，也不会把你的数据传到任何服务器。",
            "Band → vendor health app → system health service → here. The app never talks to the band directly, uses no Bluetooth, and never uploads your data.",
            "バンド → メーカーの健康 App → システム健康サービス → ここ。アプリはバンドに直接接続せず、Bluetooth も使わず、データを送信もしません。",
            "Armilla → app sanitatis fabricatoris → servitium sanitatis → huc. App armillam non directe alloquitur, Bluetooth non utitur, data non subvehit."
        )
        add("① 系统健康服务", "① System health service", "① システム健康サービス", "① Servitium sanitatis systematis")
        add("② 授权", "② Permission", "② 許可", "② Permissio")
        add("③ 厂商数据写入", "③ Vendor writes", "③ メーカーの書き込み", "③ Scriptura fabricatoris")
        add("④ 本次读数", "④ This reading", "④ 今回の読み取り", "④ Lectio hodierna")
        add("去应用商店装 / 更新", "Install or update from the app store", "アプリストアでインストール / 更新", "In taberna installe aut renovare")
        add("六类都授权了", "All six types are allowed", "6 種類すべて許可済みです", "Omnia sex genera permissa")
        add(
            "最近 7 天有厂商写入的记录",
            "A vendor wrote records in the last 7 days",
            "直近 7 日間にメーカーの書き込みがあります",
            "Fabricator intra septem dies notavit"
        )
        add(
            "最近 7 天六类都没有记录",
            "No records of any of the six types in the last 7 days",
            "直近 7 日間、6 種類とも記録がありません",
            "Nulla ex sex generibus intra septem dies notata"
        )
        add("看厂商同步路径", "See the vendor sync path", "メーカーの同期手順を見る", "Viam synchronizationis videre")
        add("厂商同步路径", "Vendor sync path", "メーカーの同期手順", "Via synchronizationis fabricatoris")
        add(
            "第 ③ 步是判断「厂商到底写没写」的关键：厂商 App 打开，不等于数据已经写进 Health Connect。",
            "Step ③ is how you tell whether the vendor wrote anything: opening the vendor app does not mean the data reached Health Connect.",
            "③ は「メーカーが書き込んだかどうか」を判断する鍵です。メーカーのアプリを開いただけでは、Health Connect に書き込まれたことにはなりません。",
            "Gradus ③ ostendit num fabricator scripserit; aperire applicationem non significat data in Health Connect scripta."
        )

        // ---- 四种失败：没装 / 没授权 / 有数据读不到 / 授权了但没写 ----
        add("还没有系统健康服务", "No system health service yet", "システム健康サービスがありません", "Servitium sanitatis systematis deest")
        add(
            "手环数据要先汇总到它，BodyCast 才能读到。",
            "Wearable data has to reach it first before BodyCast can read anything.",
            "ウェアラブルのデータはまずそこに集まらないと、BodyCast は読み取れません。",
            "Data armillae primum ad id pervenire debet ut BodyCast legere possit."
        )
        add(
            "安卓 14 及以上一般自带系统健康服务；这台机器上没有，说明这条路在本机走不通。",
            "Android 14 and up usually ships the system health service; this device has none, so this route cannot work here.",
            "Android 14 以降は通常システム健康サービスが入っています。この端末にはないため、この経路は使えません。",
            "Android 14 et ultra servitium sanitatis systematis plerumque habet; hoc instrumento caret, ergo haec via hic non patet."
        )
        add(
            "不用一直试，下面三条都还能用：",
            "No need to keep trying; all three of these still work:",
            "何度も試す必要はありません。次の三つはどれも使えます：",
            "Noli semper temptare; haec tria etiam nunc valent:"
        )
        add(
            "· 手动校准：读完数自己看一眼，随时可用，优先级永远最高。",
            "· Manual calibration: read the numbers and judge for yourself. Always available, always outranks device readings.",
            "· 手動調整：数値を見て自分で判断します。いつでも使え、常にデバイスの測定値より優先されます。",
            "· Calibratio manualis: numeros vide et ipse iudica; semper praesto, semper mensuras praecedit."
        )
        add(
            "· 厂商 App：数据留在厂商自己家里，不往外写，应用拿不到。",
            "· Vendor app: the data stays inside the vendor's own service and is not written out, so this app cannot read it.",
            "· メーカーのアプリ：データはメーカー側にとどまり、外へは書き出されないため、本アプリは読めません。",
            "· Applicatio fabricatoris: data apud fabricatorem manent neque foras scribuntur; ergo haec applicatio ea legere non potest."
        )
        add(
            "· 第三方桥接：装一个桥接 App 把厂商数据转写进系统健康服务，见「让华为数据进得来」。",
            "· Third-party bridge: install a bridge app that copies vendor data into the system health service; see \"Getting Huawei data in\".",
            "· サードパーティのブリッジ：ブリッジアプリでメーカーのデータをシステム健康サービスへ転送します（「Huawei のデータを取り込む」参照）。",
            "· Pons tertiae partis: installe applicationem quae data fabricatoris in servitium sanitatis transfert; vide \"Data Huawei admittere\"."
        )
        add("还没有授权", "Not allowed yet", "まだ許可されていません", "Nondum permissum")
        add(
            "系统健康服务里没有给本应用任何读取权限。",
            "The system health service has not given this app any read permission.",
            "システム健康サービスは本アプリに読み取り許可を与えていません。",
            "Servitium sanitatis systematis nullam permissionem legendi huic applicationi dedit."
        )
        add(
            "点下面的「去授权」，在弹出的系统框里勾上要允许的类型。",
            "Tap Grant access below and tick the types you want to allow in the system dialog.",
            "下の「権限を設定」を押し、表示されるシステムの画面で許可する項目にチェックを入れてください。",
            "Tange infra \"Permittere\" et in systematis fenestra genera eligenda signa."
        )
        add(
            "授权前产生的历史通常不会补写：授权完先在厂商 App 里同步一次。",
            "History from before you granted access is usually not backfilled: after granting, sync once in the vendor app.",
            "許可前に作られた履歴は通常さかのぼって書き込まれません。許可後にメーカーのアプリで一度同期してください。",
            "Historia ante permissionem plerumque non suppletur: post permissionem semel in applicatione fabricatoris synchroniza."
        )
        add(
            "来源和最后写入时间都在下面。",
            "Sources and last-written times are listed below.",
            "出どころと最終書き込み時刻は下に表示されます。",
            "Origo et tempus ultimae scripturae infra monstrantur."
        )
        add(
            "标「估算」的那一条：厂商没写静息心率记录，应用用这 48 小时里最低一档的动态心率推的，不是厂商的原始数据。",
            "The row marked \"estimated\": the vendor wrote no resting heart-rate record, so the app inferred it from the lowest band of the last 48 hours of heart rate. It is not raw vendor data.",
            "「推定」と付いた行：メーカーが安静時心拍数の記録を書いていないため、直近 48 時間の心拍数のうち最も低い層から推定した値です。メーカーの生データではありません。",
            "Linea \"aestimatum\": fabricator nullam notam cordis quieti scripsit; applicatio eam ex infima parte 48 horarum aestimavit, non ex data crudo."
        )
        add(
            "手环继续戴着，厂商 App 同步进新数据之后这里会自己更新。",
            "Keep wearing the band; once the vendor app syncs new data, this updates by itself.",
            "ウェアラブルはそのまま着けてください。メーカーのアプリが新しいデータを同期すると、ここは自動で更新されます。",
            "Armillam gere; cum applicatio fabricatoris nova data synchronizat, haec sponte renovantur."
        )
        add("有数据，但这次没读到", "There is data, but this read got nothing", "データはありますが、今回は読み取れませんでした", "Data adsunt, sed lectio hodierna nihil cepit")
        add(
            "系统健康服务里有记录（看自检第 ③ 步），应用这一次没读上来。",
            "The system health service has records (see step ③), but this read did not get them.",
            "システム健康サービスには記録があります（自己診断の③参照）。今回は読み取れませんでした。",
            "Servitium sanitatis systematis notas habet (vide gradum ③), sed haec lectio eas non cepit."
        )
        add(
            "先点一次「去授权」重新确认权限——有些系统会在重启后把健康权限清掉。",
            "Tap Grant access once to reconfirm permissions - some systems clear health permissions after a restart.",
            "まず「権限を設定」でもう一度許可を確認してください。再起動後に健康の権限が消えるシステムもあります。",
            "Semel tange \"Permittere\" ut permissiones confirmes; quaedam systemata eas post restitutionem delent."
        )
        add(
            "还不行就是应用侧的读取问题：麻烦把「数据流水线自检」这四步的状态记下来反馈。",
            "If it still fails, the problem is on the app's side: please note the four step states above and report them.",
            "それでもだめならアプリ側の読み取りの問題です。「データ経路の自己診断」の四つの状態を控えてご報告ください。",
            "Si adhuc deficit, culpa in applicatione est: quattuor gradus nota et refer."
        )
        add("授权了，但厂商还没写数据", "Allowed, but the vendor has not written anything yet", "許可済みですが、メーカーはまだ書き込んでいません", "Permissum, sed fabricator nihil adhuc scripsit")
        add(
            "权限没问题，是厂商 App 还没有把记录写进系统健康服务。",
            "Permissions are fine; the vendor app just has not written records into the system health service yet.",
            "権限は問題ありません。メーカーのアプリがまだ記録をシステム健康サービスへ書き込んでいないだけです。",
            "Permissiones bene se habent; applicatio fabricatoris notas in servitium sanitatis nondum scripsit."
        )
        add(
            "厂商 App 打开不等于数据已经写进 Health Connect。",
            "Opening the vendor app does not mean the data reached Health Connect.",
            "メーカーのアプリを開いただけでは、Health Connect に書き込まれたことにはなりません。",
            "Aperire applicationem non significat data in Health Connect scripta."
        )
        add(
            "厂商 App 通常只写新产生的数据，授权前的历史不会补写；先在厂商 App 里同步一次。",
            "The vendor app usually writes only newly created data and does not backfill history from before you granted access; sync once in the vendor app first.",
            "メーカーのアプリは通常、新しく作られたデータだけを書き込みます。許可前の履歴はさかのぼって書き込まれません。まずアプリで一度同期してください。",
            "Applicatio fabricatoris plerumque sola data nova scribit; historia ante permissionem non suppletur. Semel in ea synchroniza."
        )
        add("这台机器上装到的：", "Installed on this device:", "この端末で見つかったもの：", "In hoc instrumento inventa:")
        add(
            "这台机器上没找到手环厂商的运动健康 App。",
            "No wearable vendor health app was found on this device.",
            "この端末にウェアラブルメーカーの健康アプリが見つかりませんでした。",
            "Nulla applicatio sanitatis fabricatoris in hoc instrumento inventa."
        )
        add(
            "实在写不进来（华为 / 荣耀是已知情况），可以用第三方桥接，或直接用手动校准。",
            "If nothing can be written in (a known situation with Huawei and Honor), use a third-party bridge or just calibrate by hand.",
            "どうしても書き込まれない場合（Huawei / Honor では既知です）、サードパーティのブリッジか手動調整をお使いください。",
            "Si nihil scribi potest (ut apud Huawei et Honor), utere ponte tertiae partis aut calibratione manuali."
        )
        add(
            "这台机器上没有系统健康服务，这条路走不通。",
            "This device has no system health service, so this route cannot work.",
            "この端末にはシステム健康サービスがないため、この経路は使えません。",
            "Hoc instrumento servitium sanitatis deest; haec via non patet."
        )
        add("还没有授权，先点「去授权」。", "Not allowed yet; tap Grant access first.", "まだ許可されていません。まず「権限を設定」を押してください。", "Nondum permissum; primum tange \"Permittere\".")
        add(
            "暂时没读到，可能是厂商还没把数据同步进系统。",
            "Nothing read yet; the vendor may not have synced data into the system yet.",
            "まだ読み取れていません。メーカーがシステムへデータを同期していない可能性があります。",
            "Nihil adhuc lectum; fortasse fabricator data in systema nondum synchronizavit."
        )

        // ---- 厂商同步路径（方案 §3.8 的表） ----
        add(
            "打开小米运动健康（国行），在它的设置里找「三方数据管理」：我的 → 设置 → 三方数据管理 → 授权 Health Connect",
            "Open Mi Fitness (China) and look for third-party data management: Profile → Settings → Third-party data → authorise Health Connect",
            "Mi Fitness（中国版）を開き、「サードパーティのデータ管理」を探します：マイ → 設定 → サードパーティのデータ管理 → Health Connect を許可",
            "Aperi Mi Fitness (Sinarum) et quaere administrationem datorum tertiarum: Meus → Optiones → Data tertiarum → Health Connect permittere"
        )
        add(
            "这一项在大陆区域可能被隐藏；看不到就等于这条路在本机走不通。授权前产生的历史不会补写。",
            "This entry may be hidden in mainland China; if you cannot see it, this route does not work on this device. History from before you granted access is not backfilled.",
            "この項目は中国大陸では非表示になっている場合があります。見つからない場合は、この端末ではこの経路が使えません。許可前の履歴はさかのぼって書き込まれません。",
            "Haec res in Sinis continenti occultari potest; si non apparet, haec via hic non patet. Historia ante permissionem non suppletur."
        )
        add(
            "打开小米运动健康（国际版），在它的设置里找「三方数据管理」：我的 → 三方数据管理 → 授权 Health Connect",
            "Open Mi Fitness (global) and look for third-party data management: Profile → Third-party data → authorise Health Connect",
            "Mi Fitness（国際版）を開き、「サードパーティのデータ管理」を探します：マイ → サードパーティのデータ管理 → Health Connect を許可",
            "Aperi Mi Fitness (internationale) et quaere administrationem datorum tertiarum: Meus → Data tertiarum → Health Connect permittere"
        )
        add(
            "第三方教程都指明要用国际版（没有支付宝的那个）；国行手机能不能登录国际版需要你自己验证。",
            "Third-party guides all point to the global version (the one without Alipay); whether a China-market phone can sign in to it is something you need to check yourself.",
            "サードパーティの解説はいずれも国際版（Alipay が入っていない方）を前提にしています。中国国内向け端末で国際版にログインできるかはご自身で確認してください。",
            "Ductoria tertiae partis versionem internationalem (sine Alipay) monstrant; num instrumentum Sinense eam adire possit, ipse proba."
        )
        add(
            "打开 Zepp Life（老版小米运动），找「第三方接入」：我的 → 第三方接入",
            "Open Zepp Life (legacy Mi Fit) and look for third-party access: Profile → Third-party access",
            "Zepp Life（旧 Mi Fit）を開き、「サードパーティ連携」を探します：マイ → サードパーティ連携",
            "Aperi Zepp Life (Mi Fit vetus) et quaere accessum tertiarum: Meus → Accessus tertiarum"
        )
        add(
            "官方资料里只提到微信这类第三方绑定，有没有 Health Connect 开关未证实。",
            "Official material only mentions third-party bindings such as WeChat; whether a Health Connect switch exists is unconfirmed.",
            "公式資料には WeChat などのサードパーティ連携しか記載がなく、Health Connect の切り替えがあるかは未確認です。",
            "In documentis publicis sola coniunctio tertiarum (ut WeChat) memoratur; num commutatio Health Connect adsit, incertum est."
        )
        add(
            "打开华为运动健康，找「数据分享与授权」：我的 → 隐私管理 → 数据分享与授权",
            "Open Huawei Health and look for data sharing and authorisation: Me → Privacy management → Data sharing and authorisation",
            "Huawei ヘルスケアを開き、「データ共有と許可」を探します：マイ → プライバシー管理 → データ共有と許可",
            "Aperi Huawei Health et quaere communicationem datorum: Ego → Administratio privata → Communicatio datorum"
        )
        add(
            "华为的数据走自己的分享链路（微信、Keep、咕咚这类），不写入 Health Connect。这是厂商限制，本应用修不了。",
            "Huawei data travels its own sharing route (WeChat, Keep, Codoon and the like) and is not written into Health Connect. That is a vendor limit this app cannot fix.",
            "Huawei のデータは自社の共有経路（WeChat、Keep、咕咚 など）を通り、Health Connect には書き込まれません。これはメーカー側の制限で、本アプリでは解決できません。",
            "Data Huawei viam suam (WeChat, Keep, Codoon) sequuntur neque in Health Connect scribuntur; hic finis fabricatoris est, quem haec applicatio solvere non potest."
        )
        add(
            "打开荣耀运动健康，找「数据分享、授权与导入」：我的 → 隐私管理 → 数据分享、授权与导入",
            "Open Honor Health and look for data sharing, authorisation and import: Me → Privacy management → Data sharing, authorisation and import",
            "Honor ヘルスケアを開き、「データ共有・許可・読み込み」を探します：マイ → プライバシー管理 → データ共有・許可・読み込み",
            "Aperi Honor Health et quaere communicationem et importationem datorum: Ego → Administratio privata → Communicatio datorum"
        )
        add(
            "没找到任何官方证据说它写 Health Connect，能确认的只有它自己的分享入口。",
            "No official evidence was found that it writes to Health Connect; only its own sharing entry is confirmed.",
            "Health Connect へ書き込むという公式の根拠は見つかっていません。確認できるのは自社の共有入口だけです。",
            "Nullum indicium publicum invenitur id in Health Connect scribere; sola communicatio propria confirmata est."
        )
        add(
            "打开三星健康，先启用开发者模式，才能打开 Health Connect 共享",
            "Open Samsung Health: you must enable developer mode before Health Connect sharing can be turned on",
            "Samsung Health を開きます。Health Connect 共有を有効にするには、まず開発者モードを有効にする必要があります",
            "Aperi Samsung Health: modum programmatoris primum aperi ut communicatio Health Connect fieri possit"
        )
        add(
            "国行版 App 普遍有区域化裁剪，找不到开关不是本应用的问题。",
            "China-market app builds are commonly trimmed by region; not finding the switch is not this app's fault.",
            "中国国内向けのアプリは地域ごとに機能が削られていることが多く、切り替えが見つからないのは本アプリの問題ではありません。",
            "Applicationes in Sinis saepe pro regione minuuntur; commutatio non inventa culpa huius applicationis non est."
        )

        // ---- 桥接引导（华为 / 荣耀） ----
        add("让华为数据进得来", "Getting Huawei data in", "Huawei のデータを取り込む", "Data Huawei admittere")
        add(
            "华为运动健康不往系统健康服务写数据，这里有一条现实的路。",
            "Huawei Health does not write into the system health service; here is a path that works in practice.",
            "Huawei ヘルスケアはシステム健康サービスへ書き込みません。現実に使える道筋をここに示します。",
            "Huawei Health in servitium sanitatis non scribit; hic via quae re vera patet."
        )
        add("点一下收起。", "Tap to collapse.", "押すと閉じます。", "Tange ut claudas.")
        add(
            "装一个第三方桥接 App：Health Sync 这类，官网说明支持把 HUAWEI Health 的数据转写到 Health Connect，中国大陆有 APK 下载说明",
            "Install a third-party bridge app such as Health Sync: its site says it can transfer HUAWEI Health data into Health Connect, with APK download notes for mainland China",
            "Health Sync などのサードパーティ製ブリッジアプリを入れます。公式サイトには HUAWEI Health のデータを Health Connect へ転送できると書かれており、中国大陸向けの APK 入手方法も案内されています",
            "Installe applicationem pontis tertiae partis, ut Health Sync; situs eius dicit data HUAWEI Health in Health Connect transferri posse"
        )
        add(
            "在桥接 App 里把数据源设成 HUAWEI Health（华为运动健康），目标设成 Health Connect",
            "In the bridge app, set HUAWEI Health as the source and Health Connect as the target",
            "ブリッジアプリで、データ元を HUAWEI Health（Huawei ヘルスケア）に、転送先を Health Connect に設定します",
            "In applicatione pontis fontem HUAWEI Health pone, finem Health Connect"
        )
        add(
            "回到本页点「去授权」，重新授权一次——只有桥接写进来的数据才算数",
            "Come back to this page, tap Grant access and allow again - only what the bridge writes counts",
            "このページに戻り「権限を設定」からもう一度許可してください。ブリッジが書き込んだデータだけが有効です",
            "Redi ad hanc paginam, tange \"Permittere\", et iterum permitte; sola data pontis valent"
        )
        add(
            "之后是自动的：桥接在后台同步，本页第 ③ 步会开始出现最后写入时间",
            "After that it is automatic: the bridge syncs in the background and step ③ here starts showing a last-written time",
            "あとは自動です。ブリッジがバックグラウンドで同期し、本ページの③に最終書き込み時刻が表示され始めます",
            "Deinde automaticum est: pons in fundo synchronizat, et gradus ③ tempus ultimae scripturae monstrare incipit"
        )
        add(
            "我们只给这条路径，不集成任何第三方 SDK，也不替它们的可用性做承诺",
            "We only point to this path: we bundle no third-party SDK and make no promise about its availability",
            "私たちはこの道筋を示すだけで、サードパーティ製 SDK は組み込んでおらず、その可用性も保証しません",
            "Nos viam tantum monstramus; nullum SDK tertiae partis includimus neque eius usum promittimus"
        )
        add(
            "这家不往系统健康服务写数据，下面这条是现实里能走通的路：",
            "This vendor does not write into the system health service; here is the path that works in practice:",
            "このメーカーはシステム健康サービスへ書き込みません。現実に使える道筋はこちらです：",
            "Hic fabricator in servitium sanitatis non scribit; haec via re vera patet:"
        )
        add(
            "我们只是把这条路讲清楚：不集成任何第三方 SDK，也不替它们的可用性做承诺。",
            "We are only describing the path: no third-party SDK is bundled and we make no promise about their availability.",
            "私たちはこの道筋を説明するだけです。サードパーティ製 SDK は組み込んでおらず、その可用性も保証しません。",
            "Viam tantum describimus; nullum SDK tertiae partis includimus neque eius usum promittimus."
        )

        // ---- 已授权数据类型（多一列「最后写入时间」） ----
        add(
            "系统健康服务里勾了哪些、最后一次写进来是什么时候，一起看。",
            "See which types are ticked and when each was last written.",
            "どの項目にチェックが入っているかと、最後に書き込まれた時刻をまとめて確認できます。",
            "Vide quae genera signata sint et quando ultimo scripta."
        )
        add(
            "右边是这一类最后一次写进系统健康服务的时间；「无记录」= 最近 7 天厂商没有写。",
            "The right column is when this type was last written into the system health service; \"No record\" means no vendor write in the last 7 days.",
            "右側はこの種類が最後にシステム健康サービスへ書き込まれた時刻です。「記録なし」は直近 7 日間に書き込みがないという意味です。",
            "Columna dextra tempus ultimae scripturae huius generis monstrat; \"Nulla nota\" significat intra septem dies nihil scriptum."
        )
        add("去商店找厂商 App", "Find the vendor app in the app store", "ストアでメーカーのアプリを探す", "In taberna applicationem fabricatoris quaere")
        add(
            "这台机器上没找到手环厂商的运动健康 App",
            "No wearable vendor health app was found on this device",
            "この端末にウェアラブルメーカーの健康アプリが見つかりません",
            "Nulla applicatio sanitatis fabricatoris in hoc instrumento inventa est"
        )
        add(
            "先装上厂商的运动健康 App，登录手环账号，确认数据在它里面能看到",
            "Install the vendor's health app first, sign in to your band account and make sure the data shows up there",
            "まずメーカーの健康アプリを入れ、ウェアラブルのアカウントでログインし、そこにデータが表示されることを確認してください",
            "Primum applicationem sanitatis fabricatoris installe, rationem armillae intra, et vide data ibi apparere"
        )
        add(
            "然后在它的设置里找「数据共享 / 第三方接入 / Health Connect」这类开关",
            "Then look in its settings for a switch such as data sharing, third-party access or Health Connect",
            "その後、設定で「データ共有 / サードパーティ連携 / Health Connect」といった項目を探してください",
            "Deinde in optionibus quaere communicationem datorum, accessum tertiarum aut Health Connect"
        )
        add(
            "厂商 App 打开不等于数据已经写进 Health Connect——回本页看自检第 ③ 步的最后写入时间才算数",
            "Opening the vendor app does not mean the data reached Health Connect - only the last-written time in step ③ counts",
            "メーカーのアプリを開いただけでは Health Connect に書き込まれたことにはなりません。本ページの自己診断③の最終書き込み時刻で確認してください",
            "Aperire applicationem non significat data in Health Connect scripta; tempus ultimae scripturae gradus ③ vide"
        )
        add(
            "授权前产生的历史通常不会补写：先在厂商 App 里同步一次，再回来点「手动同步」",
            "History from before you granted access is usually not backfilled: sync once in the vendor app, then come back and tap Sync now",
            "許可前に作られた履歴は通常さかのぼって書き込まれません。メーカーのアプリで一度同期してから、戻って「手動同期」を押してください",
            "Historia ante permissionem plerumque non suppletur: semel in applicatione fabricatoris synchroniza, tum redi et tange Synchronizare"
        )
    }

    // ------------------------------------------------------------------
    // 22.23.0：数据源页把整页收进「数据流水线自检」卡之后新增 / 改写的串。
    //
    // 卡顶那句结论、卡里那一个主按钮、每一步自己的一条出路，以及为此改写的
    // 三句浮层提示，全都在这里。带变量的两句（结论里的「最后一次 <时间>」、
    // 卡里的「刚刚检查于 HH:MM」）在 patterns() 里，不在这里。
    // ------------------------------------------------------------------
    init {
        // 卡片顶部那句结论：断在哪一步，或者四步全通。
        add(
            "断在第 ① 步：这台机器上没有系统健康服务",
            "Stuck at step ①: no system health service on this device",
            "①で止まっています：この端末にシステムのヘルスケアサービスがありません",
            "In gradu ① haeret: servitium sanitatis systematis in hoc instrumento deest"
        )
        add(
            "断在第 ② 步：还没给应用授权",
            "Stuck at step ②: the app has no access yet",
            "②で止まっています：アプリにまだ許可がありません",
            "In gradu ② haeret: app nondum permissa est"
        )
        add(
            "断在第 ③ 步：厂商还没写数据",
            "Stuck at step ③: the vendor app has not written anything yet",
            "③で止まっています：メーカーのアプリがまだデータを書き込んでいません",
            "In gradu ③ haeret: app fabricatoris nondum data scripsit"
        )
        add(
            "断在第 ④ 步：这次没读到数据",
            "Stuck at step ④: nothing was read this time",
            "④で止まっています：今回はデータを読み取れませんでした",
            "In gradu ④ haeret: hac vice nihil lectum est"
        )

        // 卡顶那个唯一的主按钮（四步里断在哪一步，它就是那一步的修法）。
        add(
            "检查并安装健康服务",
            "Check and install the health service",
            "ヘルスケアサービスを確認してインストール",
            "Servitium sanitatis inspicere et installare"
        )
        add("一键授权", "Grant access in one tap", "まとめて許可", "Permittere uno tactu")
        add(
            "安装厂商运动健康 App",
            "Install the vendor's health app",
            "メーカーのヘルスケアアプリをインストール",
            "Applicationem sanitatis fabricatoris installare"
        )
        // 次要的「重新检查」：执行过任何一步之后都由它刷新四步状态。
        add("重新检查", "Check again", "再確認", "Iterum recognoscere")

        // ② 的次要入口：系统设置里的授权页（原来的「系统健康服务设置 / 撤销授权」并成这一条）。
        add("在系统设置里改", "Change it in system settings", "システム設定で変更", "In optionibus systematis mutare")
        add(
            "看 / 改本应用的授权；取消授权也只能在这里做。",
            "See or change this app's access; revoking it is only possible here.",
            "このアプリの許可を確認・変更します。許可の取り消しもここだけで行えます。",
            "Permissa huius applicationis vide aut muta; revocatio hic tantum fieri potest."
        )

        // 主按钮旁边那句「跳出应用之后怎么回来」。
        add(
            "先装系统健康服务；装完回来点「重新检查」。",
            "Install the system health service first; then come back and tap Check again.",
            "先にシステムのヘルスケアサービスをインストールしてください。終わったら戻って「再確認」を押します。",
            "Primum servitium sanitatis systematis installe; deinde redi et tange \"Iterum recognoscere\"."
        )
        add(
            "同步之后回来点「重新检查」。",
            "After it syncs, come back and tap Check again.",
            "同期が終わったら戻って「再確認」を押してください。",
            "Post synchronizatio, redi et tange \"Iterum recognoscere\"."
        )
        add(
            "装上厂商 App 并登录手环账号，再回来点「重新检查」。",
            "Install the vendor app and sign in to your band account, then come back and tap Check again.",
            "メーカーのアプリをインストールしてウェアラブルのアカウントにログインし、戻って「再確認」を押してください。",
            "Applicationem fabricatoris installe et in rationem armillae intra, deinde redi et tange \"Iterum recognoscere\"."
        )
        // ③ 里那行真实检测结果下面的说明。
        add(
            "打开它同步一次；同步之后回来点「重新检查」。",
            "Open it and sync once; then come back and tap Check again.",
            "アプリを開いて一度同期してください。同期が終わったら戻って「再確認」を押します。",
            "Aperi id et semel synchroniza; deinde redi et tange \"Iterum recognoscere\"."
        )

        // ③ 的六类明细表头 + 桥接提示。
        add(
            "六类各自的最后写入时间与来源：",
            "Last write time and source for each of the six types:",
            "6 種類それぞれの最終書き込み時刻と提供元：",
            "Sex generum tempus ultimum et origo:"
        )
        add(
            "华为 / 荣耀不往系统健康服务写数据，是已知情况——下面那张卡有办法。",
            "Huawei and Honor do not write to the system health service. That is a known case - the card below has a way around it.",
            "Huawei / HONOR はシステムのヘルスケアサービスにデータを書き込みません。これは既知の状況で、下のカードに対処法があります。",
            "Huawei et Honor in servitium sanitatis systematis data non scribunt. Casus notus est; via in card infra monstratur."
        )

        // 商店搜索词（没检测到厂商 App 时用）：按界面语言搜，不写死某个品牌。
        add("运动健康", "Health", "ヘルスケア", "Sanitas")

        // 这一页底部那条铁律：原来它一直没登记，非中文界面会回退成中文原文（审计里算一条兜底缺口）。
        add(
            "手动校准的优先级永远高于系统自动判断。",
            "Manual calibration always outranks automatic judgement.",
            "手動での調整は常にシステムの自動判定より優先されます。",
            "Calibratio manualis iudicium automaticum semper praecedit."
        )

        // 三句改写过位置的浮层提示（按钮已经不在原来的位置上了）。
        add(
            "装完回到这一页，点一次「重新检查」。",
            "When it is installed, come back to this page and tap Check again.",
            "インストールが終わったらこのページに戻り、「再確認」を一度押してください。",
            "Cum installatum erit, huc redi et tange \"Iterum recognoscere\"."
        )
        add(
            "可以点下面的「在系统设置里改」，在那里手动勾选本应用。",
            "You can tap Change it in system settings below and tick this app there.",
            "下の「システム設定で変更」から、このアプリに手動でチェックを入れられます。",
            "Tange infra \"In optionibus systematis mutare\" et ibi applicationem manu signa."
        )
        add(
            "点「一键授权」，在弹出的系统框里勾上要允许的类型。",
            "Tap Grant access in one tap and choose the types in the system dialog.",
            "「まとめて許可」を押し、表示されるシステムの画面で許可する項目にチェックを入れてください。",
            "Tange \"Permittere uno tactu\" et in systematis fenestra genera eligenda signa."
        )
    }

    // ------------------------------------------------------------------
    // 22.24.0：手动填写读数 / 文件导入（方案 B §3.1–§3.3）。
    //
    // 这两条是零权限、零依赖的兜底通道；文案口径与全应用一致：
    // 日语句子 です・ます、标签体言止め；拉丁文用简化的技术拉丁短句。
    // 带变量的句子（「上次填写：<时间>」「识别 N 项 / 忽略 M 列」……）在
    // patterns() 里，不在这里。
    // ------------------------------------------------------------------
    init {
        // 数据来源枚举（SignalSource）与字段名。
        add("手动填写读数", "Manual entry", "手動入力", "Inscriptio manualis")
        add("导入数据", "Imported data", "取り込みデータ", "Data importata")
        add("运动时长", "Workout duration", "運動時間", "Duratio exercitationis")

        // ---- 手动填写读数那一页 ----
        add(
            "打开厂商 App 看到数字，照着抄进来。不认识的项留空就行，留空不会当成 0。",
            "Open the vendor app, read the numbers and copy them in. Leave the ones you cannot find empty; empty is never treated as 0.",
            "メーカーのアプリで数字を見て、そのまま写してください。分からない項目は空のままで大丈夫です。空欄を 0 として扱うことはありません。",
            "Aperi applicationem fabricatoris, numeros lege et scribe. Quae ignoras vacua relinque; vacuum numquam ut 0 habetur."
        )
        add(
            "小米运动健康 → 心率 → 静息心率；华为运动健康 → 心脏健康。单位：次/分",
            "Mi Fitness → Heart rate → Resting heart rate; Huawei Health → Heart health. Unit: bpm",
            "Mi Fitness → 心拍数 → 安静時心拍数。Huawei ヘルスケア → 心臓の健康。単位：回/分",
            "Mi Fitness → Pulsus → Pulsus quietus; Huawei Health → Sanitas cordis. Unitas: pulsus per minutam"
        )
        add(
            "小米运动健康 → 心率 → 心率变异性；华为运动健康 → 心脏健康。单位：毫秒",
            "Mi Fitness → Heart rate → HRV; Huawei Health → Heart health. Unit: ms",
            "Mi Fitness → 心拍数 → 心拍変動。Huawei ヘルスケア → 心臓の健康。単位：ミリ秒",
            "Mi Fitness → Pulsus → Variabilitas pulsus; Huawei Health → Sanitas cordis. Unitas: millisecunda"
        )
        add(
            "厂商 App 首页的「睡眠」卡片：昨晚一共睡了多久。单位：分钟（7 小时 32 分 = 452）",
            "The Sleep card on the vendor app home screen: how long you slept last night. Unit: minutes (7 h 32 min = 452)",
            "メーカーのアプリのホームにある「睡眠」カード：昨夜どのくらい眠ったか。単位：分（7 時間 32 分 = 452）",
            "Cardo somni in pagina prima applicationis: quantum heri dormivisti. Unitas: minuta (7 horae 32 minuta = 452)"
        )
        add(
            "同一张睡眠卡片：深睡时长 ÷ 总睡眠时长。填百分数（21% 就填 21）",
            "Same sleep card: deep sleep ÷ total sleep. Enter a percentage (for 21%, enter 21)",
            "同じ睡眠カード：深い睡眠 ÷ 総睡眠時間。百分率で入力します（21% なら 21）",
            "Idem cardo: somnus profundus ÷ somnus totus. Percentum scribe (21% → 21)"
        )
        add(
            "厂商 App 首页的「步数」卡片：到现在的今日步数",
            "The Steps card on the vendor app home screen: today's steps so far",
            "メーカーのアプリのホームにある「歩数」カード：今日のここまでの歩数",
            "Cardo passuum in pagina prima applicationis: passus hodierni"
        )
        add(
            "小米运动健康 → 血氧；华为运动健康 → 血氧。填百分数（97% 就填 97）",
            "Mi Fitness → Blood oxygen; Huawei Health → Blood oxygen. Enter a percentage (for 97%, enter 97)",
            "Mi Fitness → 血中酸素。Huawei ヘルスケア → 血中酸素。百分率で入力します（97% なら 97）",
            "Mi Fitness → Oxygenium sanguinis; Huawei Health → Oxygenium sanguinis. Percentum scribe (97% → 97)"
        )
        add(
            "厂商 App 的「运动 / 锻炼」卡片：今天的中高强度时长。单位：分钟",
            "The Workout card in the vendor app: today's moderate-to-vigorous minutes. Unit: minutes",
            "メーカーのアプリの「運動」カード：今日の中高強度の時間。単位：分",
            "Cardo exercitationis in applicatione: minuta intensa hodierna. Unitas: minuta"
        )
        add("填数字，不认识就留空", "Enter a number, or leave it empty", "数字を入力。分からなければ空欄", "Numerum scribe, aut vacuum relinque")
        add(
            "还没有填过。填过一次之后，下次可以用「和上次一样」。",
            "Not filled in yet. Once you have, \"Same as last time\" fills it back in.",
            "まだ入力していません。一度入力すると、次回は「前回と同じ」で戻せます。",
            "Nondum inscriptum. Post primum, \"Ut antea\" iterum implet."
        )
        add(
            "填进来的数字只用于今天的身体天气，不参与诊断；只存这几项聚合值，明细不落盘。",
            "What you enter only shapes today's body weather; it is not a diagnosis. Only these aggregate values are stored - never the detail.",
            "入力した数字は今日の体調にだけ使われ、診断には使いません。保存するのはこの集計値だけで、明細は残しません。",
            "Numeri tantum caelum corporis hodiernum formant, non diagnosis. Sola summa servatur, non singula."
        )
        add("和上次一样", "Same as last time", "前回と同じ", "Ut antea")
        add("保存这几项读数", "Save these readings", "この測定値を保存", "Has mensuras servare")
        add(
            "六类里至少填一项再保存。只填你认识的那几项就够了。",
            "Fill in at least one of the six before saving. Only the ones you can find is enough.",
            "6 種類のうち少なくとも 1 つ入力してから保存してください。分かる項目だけで十分です。",
            "Unum saltem ex sex generibus scribe ante servandum. Satis est ea quae invenis."
        )
        add("清空手动填写的读数", "Clear manual readings", "手動入力の測定値を消去", "Mensuras manuales purgare")
        add(
            "只清这一路；设备读到的数据不受影响。",
            "Only this route is cleared; device readings are untouched.",
            "消えるのはこの経路だけです。デバイスから読んだデータには影響しません。",
            "Haec via tantum purgatur; data instrumenti intacta manent."
        )
        add(
            "会清掉你抄进来的这几项数字。设备读到的数据不受影响，这一步不能撤销。",
            "This clears the numbers you entered. Device readings are untouched. It cannot be undone.",
            "入力した数字を消去します。デバイスから読んだデータには影響しません。この操作は取り消せません。",
            "Numeros inscriptos purgat. Data instrumenti intacta manent. Hoc revocari non potest."
        )
        add(
            "这份读数 24 小时内有效，之后就不再参与今天的天气判定。",
            "These readings stay valid for 24 hours; after that they no longer shape today's weather.",
            "この測定値は 24 時間有効です。それを過ぎると今日の体調の判定には使われません。",
            "Hae mensurae per 24 horas valent; deinde caelum hodiernum non formant."
        )
        add(
            "没填的项一律留空，界面继续显示「暂时没读到」。",
            "Anything not filled in stays empty, and the app keeps saying \"not read yet\".",
            "入力しなかった項目は空のままで、アプリは「まだ読み取れていません」と表示し続けます。",
            "Quae non scripta vacua manent, et applicatio \"nondum lectum\" monstrat."
        )
        add(
            "设备读到的数据没有被覆盖，也没有被清掉。",
            "Device readings were neither overwritten nor cleared.",
            "デバイスから読んだデータは上書きも消去もされていません。",
            "Data instrumenti nec oblita nec purgata sunt."
        )
        add("现在去首页看看", "See it on the home screen", "ホームで確認する", "Ad paginam primam ire")
        add("还没有填过", "Not filled in yet", "まだ入力していません", "Nondum inscriptum")
        add("还没有导入过", "Nothing imported yet", "まだ取り込んでいません", "Nondum importatum")
        add("已过期", "Expired", "期限切れ", "Expiratum")
        add(
            "照着手环 App 里的数字抄进来，24 小时内有效。",
            "Copy the numbers from your band app; valid for 24 hours.",
            "ウェアラブルのアプリの数字を写して入力します。24 時間有効です。",
            "Numeros ex applicatione armillae transcripe; per 24 horas valent."
        )
        add(
            "没识别到的项一律留空，界面继续显示「暂时没读到」。",
            "Anything not recognised stays empty, and the app keeps saying \"not read yet\".",
            "認識できなかった項目は空のままで、アプリは「まだ読み取れていません」と表示し続けます。",
            "Quae non cognita vacua manent, et applicatio \"nondum lectum\" monstrat."
        )

        // ---- 导入数据文件 ----
        add("导入数据文件", "Import a data file", "データファイルを取り込む", "Fasciculum datorum importare")
        add(
            "厂商 App 导出的 CSV / JSON；也可以直接从分享面板发给本应用。",
            "A CSV or JSON exported from the vendor app; you can also share it to this app directly.",
            "メーカーのアプリが書き出した CSV / JSON。共有メニューから本アプリへ直接送ることもできます。",
            "CSV vel JSON ex applicatione fabricatoris; etiam per communicationem systematis mitti potest."
        )
        add(
            "这两条都不需要授权、不需要装任何东西，任何手机都能用。",
            "Neither route needs permissions or extra installs; they work on any phone.",
            "どちらも許可も追加インストールも不要で、どの端末でも使えます。",
            "Ambae viae nec permissione nec installatura egent; in omni instrumento valent."
        )
        add(
            "只按表头认列；手动填写、导入与小米云快照的读数 24 小时后过期。有效期内手动填的读数优先于小米云 / 导入 / 设备读数。",
            "Columns are matched by header only. Manual, imported and Mi cloud snapshot readings expire after 24 hours; while fresh, manual readings outrank Mi cloud, imported and device ones.",
            "列は見出しだけで判断します。手動入力・取り込み・Mi クラウドのスナップショットは 24 時間で期限切れになります。有効な間は手動入力が Mi クラウド・取り込み・デバイスより優先されます。",
            "Columnae solo titulo agnoscuntur. Mensurae manu scriptae, importatae et ex nube Mi post 24 horas expirant; manuales recentes ceteras praecedunt."
        )
        add(
            "手动填写的读数也一样：只要没超过 24 小时，它填的那几项就以它为准。",
            "Manual readings work the same way: within 24 hours, the fields you filled in win.",
            "手動入力も同じです。24 時間以内であれば、入力した項目が優先されます。",
            "Mensurae manuales idem valent: intra 24 horas ea quae scripsisti praecedunt."
        )
        add("这个文件读不了", "This file cannot be read", "このファイルは読み取れません", "Hic fasciculus legi non potest")
        add(
            "它不是表格文本，或者里面是空的。",
            "It is not table text, or it is empty.",
            "表形式のテキストではないか、中身が空です。",
            "Non est textus tabulae, aut vacuus est."
        )
        add(
            "在厂商 App 里用「导出」生成 CSV，再从分享面板发给本应用，或者用设置里的「导入数据文件」。",
            "Use Export in the vendor app to make a CSV, then share it to this app, or use Import a data file in Settings.",
            "メーカーのアプリの「書き出し」で CSV を作り、共有メニューから本アプリへ送るか、設定の「データファイルを取り込む」を使ってください。",
            "In applicatione fabricatoris CSV exporta, deinde communica, aut in Optionibus \"Fasciculum datorum importare\" adhibe."
        )
        add(
            "Excel 文件请先另存为 CSV 再导入。",
            "Save Excel files as CSV first, then import.",
            "Excel ファイルは先に CSV で保存し直してから取り込んでください。",
            "Fasciculos Excel primum ut CSV serva, deinde importa."
        )
        add(
            "只接受今天或昨夜导出的数据，所以这次没有写入。",
            "Only data exported today or last night is accepted, so nothing was written.",
            "受け付けるのは今日か昨夜に書き出したデータだけなので、今回は書き込みませんでした。",
            "Sola data hodie vel heri nocte exportata accipiuntur; ideo nihil scriptum est."
        )
        add(
            "旧数据不参与今天的天气判定——读不到就说没读到，不拿旧数字顶上。",
            "Old data does not shape today's weather: if nothing is current, the app says so instead of using old numbers.",
            "古いデータは今日の体調の判定に使いません。新しいものがなければ、古い数字で埋めずに「読み取れていない」と伝えます。",
            "Vetera data caelum hodiernum non formant; si nova desunt, applicatio id dicit nec numeros veteres substituit."
        )
        add(
            "在厂商 App 里选今天（或昨夜）重新导出一次就好。",
            "Just export today (or last night) again from the vendor app.",
            "メーカーのアプリで今日（または昨夜）を選んで、もう一度書き出してください。",
            "In applicatione fabricatoris hodie (aut heri nocte) iterum exporta."
        )
        add(
            "表头里没有一列是它认识的读数。",
            "No column header matches a reading it knows.",
            "見出しの中に、認識できる測定値の列がありませんでした。",
            "Nullus titulus mensuram cognitam continet."
        )
        add("识别到的表头：", "Headers found:", "見つかった見出し：", "Tituli inventi:")
        add(
            "只按表头认列，不按列序猜：表头里有「静息心率 / 心率变异性 HRV / 睡眠时长 / 深睡 / 步数 / 血氧 / 运动时长」这些字样的列才会被读。",
            "Columns are matched by header, never guessed by position: only columns headed like resting heart rate, HRV, sleep duration, deep sleep, steps, blood oxygen or workout duration are read.",
            "列は見出しだけで判断し、並び順から推測はしません。安静時心拍数 / 心拍変動 HRV / 睡眠時間 / 深い睡眠 / 歩数 / 血中酸素 / 運動時間 といった見出しの列だけを読みます。",
            "Columnae solo titulo agnoscuntur, numquam ordine; tantum columnae titulo pulsus quietus, HRV, duratio somni, somnus profundus, passus, oxygenium sanguinis aut exercitatio leguntur."
        )
        add(
            "没有一列被写进本机，也没有填 0。",
            "No column was written to this phone, and nothing was filled in as 0.",
            "どの列も端末には書き込んでおらず、0 で埋めてもいません。",
            "Nulla columna in instrumentum scripta est, nihil ut 0 suppletum."
        )
        add(
            "这是从你的文件里读到的内容，确认之后再写入本机。",
            "This is what was read from your file; confirm before it is written to this phone.",
            "ファイルから読み取った内容です。確認してから端末に書き込みます。",
            "Haec ex fasciculo lecta sunt; ante scripturam confirma."
        )
        add("写入", "Write", "書き込む", "Scribere")
        add(
            "这一份读数 24 小时内有效。",
            "These readings are valid for 24 hours.",
            "この測定値は 24 時間有効です。",
            "Hae mensurae per 24 horas valent."
        )
        add(
            "原始文件只在内存里解析过，不会留在手机上。",
            "The original file is parsed in memory only and is not kept on the phone.",
            "元のファイルはメモリ上で読むだけで、端末には残しません。",
            "Fasciculus originalis in memoria tantum legitur neque in instrumento servatur."
        )
        add("忽略的列：", "Columns ignored:", "無視した列：", "Columnae neglectae:")
        add(
            "只按表头认列；认不出的列不会被写进来，也不会按列序猜。",
            "Columns are matched by header only: unrecognised columns are not written and never guessed by position.",
            "列は見出しだけで判断します。認識できない列は書き込まず、並び順から推測もしません。",
            "Columnae solo titulo agnoscuntur; incognitae non scribuntur neque ordine coniciuntur."
        )
        add(
            "写入的是当天的聚合读数，明细不会留在这台手机上。",
            "Only today's aggregate readings are written; the detail stays off this phone.",
            "書き込むのはその日の集計値だけです。明細は端末に残しません。",
            "Sola summa hodierna scribitur; singula in instrumento non manent."
        )

        // ---- 首页「一个数都没读到」时的第二条出路 ----
        add(
            "还没有读到数据。可以照着手环 App 里的数字手动填写读数，或者直接用手动校准——手动校准的优先级永远高于系统自动判断。",
            "Nothing read yet. You can copy the numbers from your band app by hand, or just use manual calibration - manual calibration always outranks automatic judgement.",
            "まだデータを読み取れていません。ウェアラブルのアプリの数字を手で入力するか、手動での調整を使ってください。手動での調整は常に自動判定より優先されます。",
            "Nondum lectum. Numeros ex applicatione armillae manu scribere potes, aut calibratione manuali uti; calibratio manualis iudicium automaticum semper praecedit."
        )
        add("去手动填写读数", "Enter readings by hand", "手動で測定値を入力", "Mensuras manu inscribere")
        add(
            "打开手环 App 看到数字，照着抄进来就行。",
            "Open your band app and copy the numbers in as they are.",
            "ウェアラブルのアプリを開き、数字をそのまま写してください。",
            "Aperi applicationem armillae et numeros transcripe."
        )
        add(
            "不需要设备，不需要授权，也不需要 Health Connect。",
            "No device, no permissions, no Health Connect.",
            "デバイスも許可も Health Connect も不要です。",
            "Nec instrumentum, nec permissio, nec Health Connect."
        )
        add(
            "只填你认识的那几项，其余留空——留空不会当成 0。",
            "Fill in only what you can find; leave the rest empty - empty is never treated as 0.",
            "分かる項目だけ入力し、残りは空のままにします。空欄を 0 として扱うことはありません。",
            "Tantum quae invenis scribe, cetera vacua relinque; vacuum numquam ut 0 habetur."
        )
        add(
            "填完 24 小时内有效，之后就不再参与今天的天气判定。",
            "Once filled in it is valid for 24 hours; after that it no longer shapes today's weather.",
            "入力後 24 時間有効です。それを過ぎると今日の体調の判定には使われません。",
            "Post inscriptionem per 24 horas valent; deinde caelum hodiernum non formant."
        )
        add("现在去填", "Fill it in now", "今すぐ入力する", "Nunc inscribere")
    }

    // ------------------------------------------------------------------
    // 小米账号（云同步，22.24.0）
    //
    // 这一路的文案分两类，写法不同：
    // 1. **固定句**登记在这里（四语全填）；
    // 2. **带变量的句子**（「账号：%s」「数据区域：%s」）走 `patterns()` 的模板。
    //    动态值（账号线索、主机名）**不再喂回 `text()`**——它们是用户自己的数据，
    //    翻不了，也不该记成漏翻缺口。
    // ------------------------------------------------------------------
    init {
        add("小米账号（云同步）", "Mi account (cloud sync)", "Mi アカウント（クラウド同期）", "Ratio Mi (synchronizatio nubis)")
        add("小米账号", "Mi account", "Mi アカウント", "Ratio Mi")
        add("已连接", "Connected", "接続済み", "Coniunctum")
        add("未登录", "Not connected", "未接続", "Non coniunctum")
        add("数据区域", "Data region", "データ地域", "Regio datorum")
        add("上次同步", "Last sync", "前回の同期", "Synchronizatio ultima")
        add("还没有同步过", "Not synced yet", "まだ同期していません", "Nondum synchronizatum")
        add("还没有同步过。", "Not synced yet.", "まだ同期していません。", "Nondum synchronizatum est.")
        add("读到了", "Read", "読み取りました", "Lectum")
        add("静息心率", "Resting heart rate", "安静時心拍数", "Pulsus quies")
        add("睡眠时长与深睡", "Sleep duration and deep sleep", "睡眠時間と深い睡眠", "Tempus somni et somnus profundus")
        add("步数", "Steps", "歩数", "Passus")
        add("血氧", "Blood oxygen", "血中酸素", "Oxygenium sanguinis")
        add("中国大陆", "Mainland China", "中国本土", "Sinae continentes")
        add("新加坡", "Singapore", "シンガポール", "Singapura")
        add("美国", "United States", "アメリカ", "Civitates Foederatae")
        add("欧洲", "Europe", "ヨーロッパ", "Europa")
        add("俄罗斯", "Russia", "ロシア", "Russia")
        add("印度", "India", "インド", "India")

        // ---- 页面说明与连接流程 ----
        add(
            "可选通道。连上之后，应用才会去小米取你自己账号里的数据。",
            "An optional channel. Only once you connect it does the app fetch data from your own account at Xiaomi.",
            "任意の経路です。あなたが接続したときだけ、アプリは Xiaomi のご自身のアカウントからデータを取得します。",
            "Via optionalis. Solum postquam coniungis, applicatio data ex ratione tua apud Xiaomi petit."
        )
        add(
            "点之前先看清楚",
            "Read this before you tap",
            "押す前に読んでください",
            "Ante tactum lege"
        )
        add(
            "默认不联网。本应用在没有连接小米账号之前，不会向任何服务器发出请求。",
            "Offline by default. Until you connect a Mi account, the app sends no requests to any server.",
            "既定では通信しません。Mi アカウントを接続するまで、アプリはどのサーバーにも要求を送りません。",
            "Default rete non utitur. Antequam rationem Mi coniungis, applicatio nihil ad servum ullum mittit."
        )
        add(
            "只有你主动连接小米账号之后，应用才会访问小米的服务器，把你自己账号里的运动健康数据取回来。",
            "Only after you connect a Mi account yourself does the app contact Xiaomi's servers to fetch the fitness data in your own account.",
            "あなた自身が Mi アカウントを接続したときだけ、アプリは Xiaomi のサーバーにアクセスし、ご自身のアカウントの運動健康データを取得します。",
            "Solum postquam rationem Mi coniungis, applicatio servos Xiaomi adit ut data ex ratione tua petat."
        )
        add(
            "登录在小米官方页面（account.xiaomi.com）完成：密码、验证码都只输在小米那里，BodyCast 不接触你的密码。",
            "Sign-in happens on Xiaomi's official page (account.xiaomi.com): the password and any code are typed only there. BodyCast never touches your password.",
            "ログインは Xiaomi の公式ページ（account.xiaomi.com）で行います。パスワードと確認コードは Xiaomi 側にのみ入力し、BodyCast はパスワードに触れません。",
            "In pagina officiali Xiaomi (account.xiaomi.com) intras: tesseram et codicem ibi tantum scribis. BodyCast tesseram tuam non attingit."
        )
        add(
            "令牌用系统密钥库（Android Keystore）加密后存在本机，明文不落盘；随时可以「断开并删除令牌」。",
            "The token is encrypted with the system key store (Android Keystore) before it is written to this phone; the plaintext is never stored. You can disconnect and delete the token at any time.",
            "トークンはシステムのキーストア（Android Keystore）で暗号化してから端末に保存し、平文は保存しません。「切断してトークンを削除」はいつでも行えます。",
            "Testimonium clave systematis (Android Keystore) antequam in instrumento scribitur; textus apertus non servatur. Semper \"disiungere et testimonium delere\" potes."
        )
        add(
            "取回来的是快照——设备同步到小米云的那一刻的样子，不是实时读数。所以这里会显示上次同步时间。",
            "What comes back is a snapshot: the state at the moment your device synced to Xiaomi's cloud, not a live reading. That is why the last sync time is shown here.",
            "取得するのはスナップショットです。デバイスが Xiaomi のクラウドに同期した時点の状態で、リアルタイムの測定値ではありません。そのため前回の同期時刻を表示します。",
            "Imago quaedam redit: status eo momento quo instrumentum ad nubem Xiaomi synchronizatum est, non mensura viva. Ideo tempus synchronizationis ultimae hic ostenditur."
        )
        add("还没有连接", "Not connected yet", "まだ接続していません", "Nondum coniunctum")
        add(
            "点下面的按钮会打开小米官方的登录页；登录完成后回到这里，应用立刻同步一次。",
            "The button below opens Xiaomi's official sign-in page. When you come back here after signing in, the app syncs once straight away.",
            "下のボタンで Xiaomi 公式のログインページが開きます。ログイン後にここへ戻ると、アプリはすぐ一度同期します。",
            "Nexus infra paginam officialem Xiaomi aperit; postquam redieris, applicatio statim semel synchronizat."
        )
        add("正在连接…", "Connecting…", "接続しています…", "Coniungitur…")
        add("连接小米账号", "Connect Mi account", "Mi アカウントを接続", "Rationem Mi coniungere")
        add(
            "不想连了？随时回到这一页点「断开并删除令牌」，本机不会留下任何令牌。",
            "Changed your mind? Come back to this page and tap Disconnect and delete token; no token is left on this phone.",
            "やめたいときは、このページに戻って「切断してトークンを削除」を押してください。端末にトークンは残りません。",
            "Mutavisti sententiam? Huc redi et \"disiungere et testimonium delere\" tange; nihil in hoc instrumento manet."
        )
        add("令牌加密存在本机；断开即删除。", "The token is stored here encrypted; disconnecting deletes it.", "トークンは暗号化して端末に保存し、切断すると削除されます。", "Testimonium hic cryptatum servatur; disiunctio id delet.")
        add("快照 24 小时内有效，参与今天的判定。", "The snapshot is valid for 24 hours and shapes today's assessment.", "スナップショットは 24 時間有効で、今日の判定に使われます。", "Imago per 24 horas valet et iudicium hodiernum attingit.")
        add(
            "已过期（超过 24 小时），不参与今天的判定。",
            "Expired (older than 24 hours), so it does not shape today's assessment.",
            "期限切れです（24 時間を超えています）。今日の判定には使われません。",
            "Expirata (plus quam 24 horas): iudicium hodiernum non attingit."
        )
        add("六类读数读到了没有", "Which of the six readings came back", "六種類の測定値の取得状況", "Quae ex sex mensuris lecta sint")
        add(
            "「暂时没读到」= 小米那边没有这一类数据（可能是设备不支持，或者还没同步上云）。这一项留空，不填 0。",
            "“Not available yet” means Xiaomi has no data of that kind (the device may not support it, or it has not synced to the cloud). That field stays empty; it is never filled with 0.",
            "「まだ読み取れていません」は、Xiaomi 側にその種類のデータがないという意味です（デバイスが対応していないか、まだクラウドに同期していない可能性があります）。その項目は空のままで、0 は入れません。",
            "「Nondum lectum」significat Xiaomi data eius generis non habere (instrumentum fortasse non valet, aut nondum ad nubem synchronizatum est). Campus vacat, numquam 0."
        )
        add(
            "读到的项已经并进首页读数：手动填的读数优先级更高，手动校准永远最高。",
            "The readings that came back are merged into your home readings: manually entered readings outrank them, and manual calibration always ranks highest.",
            "読み取れた項目はホームの測定値に統合されます。手動入力のほうが優先され、手動調整が常に最優先です。",
            "Lecta in mensuras paginae principalis miscentur: manus scripta praecedunt, calibratio manualis semper prima."
        )
        add("立即同步", "Sync now", "今すぐ同期", "Nunc synchronizare")
        add("正在同步…", "Syncing…", "同期しています…", "Synchronizatur…")
        add(
            "同步只在你点这一下（或刚连接完的那一次）发生；应用不会在后台自己联网。",
            "Sync only happens when you tap this (or once right after connecting); the app never goes online by itself in the background.",
            "同期はここを押したとき（または接続直後の一度）だけ行われます。バックグラウンドで勝手に通信することはありません。",
            "Synchronizatio tantum fit cum hoc tangis (aut semel statim post coniunctionem); applicatio in fundo rete non adit."
        )
        add("断开并删除令牌", "Disconnect and delete token", "切断してトークンを削除", "Disiungere et testimonium delere")
        add(
            "断开之后，本机不再有这份令牌，云端读数也一并清掉。",
            "After disconnecting, this phone no longer holds the token, and the cloud readings are cleared as well.",
            "切断すると、端末にこのトークンは残らず、クラウドから取得した測定値も消去されます。",
            "Post disiunctionem hoc instrumentum testimonium non tenet, et mensurae e nube deletae sunt."
        )
        add(
            "密钥由系统密钥库保管，删除之后旧的密文再也解不开。",
            "The key is kept by the system key store; once deleted, the old ciphertext can never be opened again.",
            "鍵はシステムのキーストアが保管します。削除すると古い暗号文は二度と復号できません。",
            "Clavis a clavigera systematis servatur; deleta, litterae occultae veteres numquam aperiuntur."
        )
        add("想再用的时候，重新连接一次就好。", "Whenever you want it again, just connect once more.", "また使いたくなったら、もう一度接続するだけです。", "Si iterum vis, iterum coniunge.")
        add("断开并删除", "Disconnect and delete", "切断して削除", "Disiungere et delere")
        add("已断开并删除令牌。", "Disconnected and the token was deleted.", "切断し、トークンを削除しました。", "Disiunctum et testimonium deletum est.")
        add("这一步没成功", "This step did not succeed", "この手順は成功地しませんでした", "Hic gradus non successit")
        add(
            "网络不通就检查网络；令牌过期就重新连接；接口变了的话这条路会整体失效——那时它会一直报错，不会假装成功。",
            "If the network is down, check your connection; if the token expired, connect again; if Xiaomi changed the interface, this whole path stops working — and it will keep saying so instead of pretending to succeed.",
            "通信できないときは回線を確認し、トークンが切れていれば接続し直します。Xiaomi が仕様を変えた場合、この経路は使えなくなります。そのときは成功したふりをせず、ずっとエラーを表示します。",
            "Si rete deest, rete inspice; si testimonium expiravit, iterum coniunge; si Xiaomi interfaciem mutavit, haec via tota cessat — et semper errorem dicit, non successum simulat."
        )
        add(
            "说明：这是小米的非官方接口，小米随时可能改动它。应用只读你自己账号里的数据，不向小米写任何东西、也不上传本机的任何内容。",
            "Note: this is an unofficial Xiaomi interface and Xiaomi may change it at any time. The app only reads from your own account, writes nothing to Xiaomi and uploads nothing from this phone.",
            "注記：これは Xiaomi の非公式インターフェースであり、Xiaomi はいつでも変更できます。アプリはご自身のアカウントから読み取るだけで、Xiaomi に何も書き込まず、端末の内容も送りません。",
            "Nota: interfacies Xiaomi non officialis est et mutari potest. Applicatio tantum ex ratione tua legit, nihil ad Xiaomi scribit, nihil ex hoc instrumento mittit."
        )
        add(
            "已取消，没有连接任何账号。",
            "Cancelled; no account was connected.",
            "キャンセルしました。どのアカウントも接続していません。",
            "Cancellatum; nulla ratio coniuncta est."
        )
        add(
            "本机没有可用的令牌，请重新连接小米账号。",
            "No usable token on this phone; please connect the Mi account again.",
            "端末に使えるトークンがありません。Mi アカウントを接続し直してください。",
            "Nullum testimonium utile in hoc instrumento; rationem Mi iterum coniunge."
        )
        add(
            "本机没能加密保存令牌（系统密钥库不可用）。",
            "The token could not be encrypted on this phone (the system key store is unavailable).",
            "端末でトークンを暗号化して保存できませんでした（システムのキーストアが利用できません）。",
            "Testimonium in hoc instrumento cryptari non potuit (clavigera systematis deest)."
        )
        add(
            "正在取回会话…",
            "Fetching the session…",
            "セッションを取得しています…",
            "Sessio petitur…"
        )
        add(
            "已连接，正在同步一次…",
            "Connected; syncing once…",
            "接続しました。一度同期しています…",
            "Coniunctum; semel synchronizatur…"
        )
        add(
            "同步完成，但六类都没读到——账号里可能还没有设备同步上来的数据。",
            "Sync finished, but none of the six could be read — the account may not have any data synced from a device yet.",
            "同期は完了しましたが、六種類とも読み取れませんでした。アカウントにまだデバイスから同期されたデータがない可能性があります。",
            "Synchronizatio perfecta est, sed nihil ex sex lectum est: fortasse nulla data ex instrumento ad rationem pervenerunt."
        )

        // ---- 失败说明（每一种都对应一个具体原因，不笼统说「失败了」）----
        add(
            "小米拒绝了这份会话（可能已过期），请重新连接小米账号。",
            "Xiaomi rejected this session (it may have expired). Please connect the Mi account again.",
            "Xiaomi がこのセッションを拒否しました（期限切れの可能性があります）。Mi アカウントを接続し直してください。",
            "Xiaomi hanc sessionem reppulit (fortasse expiravit). Rationem Mi iterum coniunge."
        )
        add(
            "连不上小米的服务器：请检查网络后重试。",
            "Cannot reach Xiaomi's servers: check your network and try again.",
            "Xiaomi のサーバーに接続できません。回線を確認して再試行してください。",
            "Servi Xiaomi attingi non possunt: rete inspice et iterum tenta."
        )
        add(
            "小米云限流了，过一会儿再试。",
            "Xiaomi's cloud is rate-limiting; try again in a little while.",
            "Xiaomi のクラウドが制限をかけています。しばらくしてからお試しください。",
            "Nubes Xiaomi multa petit; paulo post iterum tenta."
        )
        add(
            "小米云的响应读不懂（接口可能变了，或会话已失效）。",
            "The response from Xiaomi's cloud could not be read (the interface may have changed, or the session is no longer valid).",
            "Xiaomi のクラウドの応答を読み取れません（仕様が変わったか、セッションが無効になっています）。",
            "Responsum nubis Xiaomi legi non potest (interfacies fortasse mutata est, aut sessio invalida)."
        )
        add(
            "小米云返回了错误，稍后再试；一直失败的话这条路可能已经不适用了。",
            "Xiaomi's cloud returned an error; try again later. If it keeps failing, this path may no longer apply.",
            "Xiaomi のクラウドがエラーを返しました。後でもう一度お試しください。失敗が続く場合、この経路はもう使えない可能性があります。",
            "Nubes Xiaomi errorem reddidit; serius tenta. Si semper deficit, via haec fortasse iam non valet."
        )
        add(
            "同步失败：这次没成功，稍后再试。",
            "Sync failed: it did not go through this time; try again later.",
            "同期に失敗しました。今回は成功しませんでした。後でもう一度お試しください。",
            "Synchronizatio defecit: hac vice non successit; serius tenta."
        )
        add(
            "登录完成了，但没拿到可续期的凭据（passToken）。请在小米页面里完成登录后马上返回。",
            "Sign-in finished, but the renewable credential (passToken) was not issued. Please finish signing in on Xiaomi's page and come straight back.",
            "ログインは完了しましたが、更新用の資格情報（passToken）を取得できませんでした。Xiaomi のページでログインを終えたら、すぐに戻ってください。",
            "Inscriptio perfecta est, sed testimonium renovabile (passToken) defuit. In pagina Xiaomi inscribe et statim redi."
        )
        add(
            "登录完成了，但没拿到账号标识（userId）。",
            "Sign-in finished, but no account identifier (userId) came back.",
            "ログインは完了しましたが、アカウント識別子（userId）を取得できませんでした。",
            "Inscriptio perfecta est, sed nota rationis (userId) defuit."
        )
        add(
            "登录完成了，但没能取得会话密钥（ssecurity）。",
            "Sign-in finished, but the session key (ssecurity) could not be obtained.",
            "ログインは完了しましたが、セッション鍵（ssecurity）を取得できませんでした。",
            "Inscriptio perfecta est, sed clavis sessionis (ssecurity) obtineri non potuit."
        )
        add(
            "续期时没拿到会话密钥（ssecurity）。",
            "The session key (ssecurity) was not issued when renewing.",
            "更新時にセッション鍵（ssecurity）を取得できませんでした。",
            "In renovatione clavis sessionis (ssecurity) defuit."
        )
        add(
            "登录没有完成。",
            "Sign-in was not completed.",
            "ログインは完了しませんでした。",
            "Inscriptio non perfecta est."
        )

        // ---- 设置页里那一条 ----
        add(
            "可选：连接你自己的小米账号，把云端数据取回来。默认不联网。",
            "Optional: connect your own Mi account to fetch your data from the cloud. Offline by default.",
            "任意：ご自身の Mi アカウントを接続して、クラウドからデータを取得します。既定では通信しません。",
            "Optionalis: rationem Mi tuam coniunge ut data ex nube petas. Default rete non utitur."
        )
        add(
            "这条路不经过系统健康服务，读的是小米云里的快照。",
            "This path does not go through the system health service; it reads a snapshot from Xiaomi's cloud.",
            "この経路はシステムの健康サービスを通らず、Xiaomi のクラウドのスナップショットを読みます。",
            "Haec via servitium sanitatis systematis non transit; imaginem e nube Xiaomi legit."
        )
        add(
            "走的不是系统健康服务，所以第 ③ 步显示「没写入」时它照样可能有数据。",
            "It does not go through the system health service, so it may still have data even when step ③ says nothing was written.",
            "システムの健康サービスを通らないため、手順③が「書き込みなし」でもデータがある場合があります。",
            "Servitium sanitatis systematis non transit; ergo etsi gradus ③ nihil scriptum dicit, data adsunt."
        )
        add("查看 MIT 许可原文", "View the MIT license text", "MIT ライセンス原文を表示", "Textum licentiae MIT videre")
        add(
            "小米云网络失败自检",
            "Mi cloud network-failure self-check",
            "Mi クラウド通信失敗の自己検査",
            "Autotestium retis nubis Mi"
        )
        add(
            "用一个假会话去问一个不存在的域名：应当报「连不上小米的服务器」，而且不崩。",
            "Asks a non-existent domain with a fake session: it should report “cannot reach Xiaomi's servers” and not crash.",
            "偽のセッションで存在しないドメインに問い合わせます。「Xiaomi のサーバーに接続できません」と表示され、クラッシュしないはずです。",
            "Sessione ficta dominium non exsistens interrogat: \"servi Xiaomi attingi non possunt\" referre debet neque labi."
        )
        add("意外地成功了：这台机器居然解析了那个不存在的域名。", "Unexpectedly succeeded: this machine actually resolved that non-existent domain.", "意外にも成功しました。この端末は存在しないはずのドメインを解決してしまいました。", "Inopinanter successit: hoc instrumentum dominium non exsistens repperit.")
        add(
            "预期结果：连不上小米的服务器：请检查网络后重试。",
            "Expected result: cannot reach Xiaomi's servers — check your network and try again.",
            "期待される結果：「Xiaomi のサーバーに接続できません。回線を確認して再試行してください」。",
            "Eventus exspectatus: servi Xiaomi attingi non possunt — rete inspice et iterum tenta."
        )
        add(
            "这一条走的是假会话 + 不存在的域名，不发真实请求内容，也不读任何真实数据。",
            "This check uses a fake session and a non-existent domain: no real request content is sent and no real data is read.",
            "この検査は偽のセッションと存在しないドメインを使います。実際の要求内容は送らず、実データも読みません。",
            "Hoc autotestium sessione ficta et dominio non exsistente utitur; nihil veri mittitur, nihil veri legitur."
        )

        // ---- 开发者页：小米令牌加密自检 ----
        add("小米令牌加密自检", "Mi token encryption self-check", "Mi トークン暗号化の自己検査", "Autotestium testimonii Mi cryptati")
        add(
            "用一串临时数据走一遍「存 → 读 → 删除」。不会显示任何令牌内容。",
            "Runs a temporary string through store → read → delete. No token content is ever shown.",
            "一時的な文字列で「保存 → 読み出し → 削除」を一巡します。トークンの中身は表示しません。",
            "Funiculum temporarium per serva → lege → dele ducit. Nihil testimonii monstratur."
        )
        add(
            "四个问题各答一句；密钥在自检结束时已被删除。",
            "Four questions, one line each; the key is deleted when the check ends.",
            "四つの問いに一行ずつ答えます。検査の終了時に鍵は削除されます。",
            "Quattuor quaestiones, singulae lineae; clavis in fine deleta est."
        )
        add(
            "自检用的是临时字符串，不是你的令牌；过程里也不打印任何内容。",
            "The check uses a temporary string, not your token, and prints nothing along the way.",
            "検査に使うのは一時的な文字列で、あなたのトークンではありません。途中で何も出力しません。",
            "Autotestium funiculo temporario utitur, non testimonio tuo, et nihil imprimit."
        )
        add("是", "Yes", "はい", "Ita")
        add("否", "No", "いいえ", "Non")

        // ---- 登录页 ----
        add(
            "在你的小米官方页面里登录。",
            "Sign in on Xiaomi's official page.",
            "Xiaomi の公式ページでログインします。",
            "In pagina officiali Xiaomi inscribe."
        )
        add(
            "登录在小米自己的页面上完成",
            "Sign-in happens on Xiaomi's own page",
            "ログインは Xiaomi 自身のページで行います",
            "Inscriptio in pagina ipsius Xiaomi fit"
        )
        add(
            "下面这一页是小米的官方网站。账号、密码、验证码都只输在小米那里，BodyCast 看不到、也不保存这些东西——它只在你登录完之后，从浏览器的 Cookie 里取一个访问令牌。",
            "The page below is Xiaomi's official site. The account name, password and code are typed only there; BodyCast cannot see or store them — once you are signed in, it only takes an access token from the browser cookie.",
            "下のページは Xiaomi の公式サイトです。アカウント、パスワード、確認コードは Xiaomi 側にのみ入力し、BodyCast はそれらを見ることも保存することもできません。ログイン後にブラウザの Cookie からアクセストークンを受け取るだけです。",
            "Pagina infra situs officialis Xiaomi est. Nomen rationis, tessera et codex ibi tantum scribuntur; BodyCast ea videre aut servare non potest — post inscriptionem solum testimonium ex crustulo navigatri sumit."
        )
        add("登录完成，正在取回会话…", "Signed in; fetching the session…", "ログイン完了。セッションを取得しています…", "Inscriptum; sessio petitur…")
        add("我已经登录完了", "I have finished signing in", "ログインが終わりました", "Inscriptionem perfeci")
        add("正在确认登录结果…", "Checking the sign-in result…", "ログイン結果を確認しています…", "Eventus inscriptionis inspicitur…")
        add(
            "登录只在小米的页面上完成；BodyCast 不会看到你的密码，也不会把它存下来。",
            "Sign-in only happens on Xiaomi's page; BodyCast never sees your password and never stores it.",
            "ログインは Xiaomi のページでのみ行います。BodyCast はパスワードを見ることも、保存することもありません。",
            "Inscriptio in pagina Xiaomi tantum fit; BodyCast tesseram tuam neque videt neque servat."
        )

        // ---- 登录页打不开时的提示（22.26.0：断网 / 被拦时不能留一块黑框）----
        add("打不开小米登录页", "Couldn't open the Mi sign-in page", "Mi ログインページを開けません", "Pagina login Mi aperiri non potest")
        add(
            "可能是网络不通，或者页面被拦住了。检查网络之后点「重试」。",
            "The network may be down, or the page was blocked. Check your connection and tap Retry.",
            "ネットワークが不通か、ページがブロックされている可能性があります。接続を確認してから「再試行」を押してください。",
            "Network deest fortasse, aut pagina impedita est. Conexum inspice, deinde «Repetere» tange."
        )
        add("重试", "Retry", "再試行", "Repetere")
        add("正在重新打开…", "Reopening…", "再度開いています…", "Repetitur…")

        // ---- 自检第 ③ 步：一类都没授权时（22.26.0，不能说成「厂商没写」）----
        add(
            "还没授权，看不到写入时间",
            "Not allowed yet, so write times cannot be read",
            "まだ許可されていないため、書き込み時刻は確認できません",
            "Nondum permissum; tempora scripturae legi non possunt"
        )

        // ---- 自检第 ③ 步里的小米云状态 +「上次读取」（22.27.0）----
        add(
            "还没有连接小米账号。连上之后这里会显示上次同步时间和读到几类。",
            "No Mi account connected yet. Connect one and this line will show the last sync time and how many types were read.",
            "まだ Mi アカウントを接続していません。接続すると、最終同期時刻と読み取れた種類の数がここに表示されます。",
            "Ratio Mi nondum coniuncta est. Post coniunctionem ultima synchronizatio et numerus generum hic monstrantur."
        )
        add(
            "本机的令牌已失效，请重新登录。",
            "The sign-in token on this phone is no longer valid; please sign in again.",
            "この端末のトークンは無効になりました。もう一度ログインしてください。",
            "Testimonium in hoc instrumento invalidum est; iterum inscribe."
        )
        add("已连接，还没有同步过。", "Connected; not synced yet.", "接続済み。まだ同期していません。", "Coniunctum; nondum synchronizatum.")
        add("未连接", "Not connected", "未接続", "Non coniunctum")
        add("需要重新登录", "Sign in again required", "再ログインが必要", "Iterum inscribendum")
        add("去连接", "Connect", "接続する", "Coniungere")
        add("重新登录", "Sign in again", "再ログイン", "Iterum inscribe")
        add("去同步", "Sync now", "同期する", "Synchronizare")
        add(
            "还没有读取过",
            "Nothing has been read yet",
            "まだ読み取っていません",
            "Nondum lectum"
        )
        add(
            "数据可能过期，点此刷新",
            "Data may be out of date; tap to refresh",
            "データが古い可能性があります。タップして更新してください",
            "Data fortasse vetera sunt; tange ut renoventur"
        )
        // ---- 22.27.0 漏翻补登记（英/日/拉三语整页 dump 时抓到的存量漏网）----
        add("主题色", "Theme color", "テーマ色", "Color thematis")
        add(
            "导出的备份只含日期、天气词和标签；小米账号那一路的令牌加密存在本机，随时可以断开删除。",
            "An exported backup holds only dates, weather words and tags. The Mi-account token is encrypted on this phone and can be disconnected and deleted at any time.",
            "書き出したバックアップには日付、体調の言葉、タグだけが入ります。Mi アカウントのトークンはこの端末で暗号化して保存しており、いつでも接続を切って削除できます。",
            "Exemplar exportatum solum dies, verba caeli et titulos continet. Testimonium rationis Mi in hoc instrumento cryptatum est; semper disiungi et deleri potest."
        )
        add(
            "小米账号（云同步）：厂商不往系统健康服务写数据时的一条可选路。默认关闭；只有你主动连接自己的小米账号，应用才会去小米的服务器把数据取回来，登录在小米官方页面完成。",
            "Mi account (cloud sync): an optional route for when the vendor does not write to the system health service. Off by default; only when you connect your own Mi account does the app fetch data from Xiaomi's servers — sign-in happens on Xiaomi's official page.",
            "Mi アカウント（クラウド同期）：メーカーがシステムの健康サービスに書き込まない場合の任意の経路です。既定ではオフで、ご自分で Mi アカウントを接続したときにだけ、アプリが Xiaomi のサーバーからデータを取得します。ログインは Xiaomi の公式ページで行います。",
            "Ratio Mi (nubes): via optionalis si fabrica ad sanitatis servum non scribit. Default clausa; solum cum rationem Mi tuam coniungis, app data a servis Xiaomi sumit — inscriptio in pagina officiali Xiaomi fit."
        )
        add(
            "这一条走的是假会话 + 一个永远不会解析的域名，不发真实请求内容，也不读任何真实数据。",
            "This check uses a fake session and a domain that never resolves: no real request content is sent and no real data is read.",
            "この検査は偽のセッションと、決して名前解決されないドメインを使います。実際のリクエスト内容は送らず、実データも読み取りません。",
            "Haec probatio sessione ficta et domino numquam soluto utitur: nihil verum mittitur, nihil verum legitur."
        )

        // ---- 22.34.0 开箱重排 + 小米云链路漏翻补登记 ----
        add("挑一个顺眼的样子", "Pick a look that feels right", "好みの見た目を選ぶ", "Speciem placentem elige")
        add(
            "主题色管按钮和图标的颜色，深浅色管整页的底色。点选即时生效，以后随时能换。",
            "Theme color sets buttons and icons; light or dark sets the page background. It applies right away, and you can switch anytime.",
            "テーマカラーはボタンやアイコンの色を、明暗はページ全体の背景を決めます。選んだ瞬間に反映され、あとからいつでも変えられます。",
            "Color thematis gemmas et icones, lux vel tenebrae fundum paginae regunt. Confestim valet; mutare potes quandoque."
        )
        add("深浅色", "Light or dark", "明暗", "Lux vel tenebrae")
        add(
            "玻璃的折射在深色底上最清晰，不过浅色也一样好用。",
            "Glass refraction reads clearest on a dark background, but light works just as well.",
            "ガラスの屈折は暗い背景ではっきり見えますが、明るい背景でも同じように使えます。",
            "Refractio vitri in fundo obscuro clarissima est; sed in aperto aeque bene valet."
        )
        add(
            "首页会给你一件几分钟能做完的小事：喝杯温水、闭眼三分钟、拉伸肩颈。做完只回一句「身体收到。」",
            "The home page gives you one small thing that takes minutes: warm water, three minutes with your eyes closed, a neck stretch. Finishing it only gets one line back: \u201cBody received.\u201d",
            "ホームには数分でできる小さなことが一つ届きます。温水を飲む、目を閉じて三分、首を伸ばす。終わったら「体が受け取りました。」の一行だけが返ってきます。",
            "Pagina principalis rem parvam dat: aquam tepidam, tribus minutis oculis clausis, collum extendere. Peracto una linea respondetur: Corpus accepit."
        )
        add(
            "手环数据的连接在「设置 → 数据与同步」里，有一步步的引导，不用现在弄。",
            "Band data connection lives in Settings \u2192 Data and sync, with a step-by-step guide \u2014 no need to do it now.",
            "バンドのデータ接続は「設定 \u2192 データと同期」にあります。手順に沿った案内があるので、いまやる必要はありません。",
            "Coniunctio datae armillae in Optionibus \u2192 Data et synchronizatio, cum ductu passuum; nunc non necesse est."
        )
        add("网络不通", "No network", "ネットワークに接続できません", "Rete non respondet")
        add(
            "小米云限流了（HTTP 429），过一会儿再试。",
            "Xiaomi Cloud is rate-limiting (HTTP 429). Try again in a while.",
            "Xiaomi クラウドがレート制限中です（HTTP 429）。しばらくしてからもう一度お試しください。",
            "Nubes Xiaomi nimium petitum (HTTP 429). Postea denuo tenta."
        )
        add(
            "小米云返回了登录页，这份会话已经失效。",
            "Xiaomi Cloud returned a sign-in page; this session is no longer valid.",
            "Xiaomi クラウドがログインページを返しました。このセッションは無効です。",
            "Nubes Xiaomi paginam login reddidit; haec sessio non valet."
        )
        add(
            "小米云的响应解不开（会话可能已失效）。",
            "Xiaomi Cloud's response could not be decrypted (the session may have expired).",
            "Xiaomi クラウドの応答を復号できません（セッションが切れている可能性があります）。",
            "Responsum nubis Xiaomi solvi non potest (forsitan sessio expiravit)."
        )
        add(
            "小米云返回的不是 JSON。",
            "Xiaomi Cloud returned something that is not JSON.",
            "Xiaomi クラウドの応答が JSON ではありません。",
            "Nubes Xiaomi non JSON reddidit."
        )
        add(
            "下面这一页是小米的官方网站。账号、密码、验证码都只输在小米那里，BodyCast 看不到、也不保存这些东西——它只在你登录完之后，从浏览器的 Cookie 里取一个访问令牌。",
            "The next page is Xiaomi's official site. Account, password and verification code are entered only on Xiaomi's side; BodyCast never sees or keeps them — it only takes one access token from the browser's cookies after you sign in.",
            "次のページは Xiaomi の公式サイトです。アカウント、パスワード、認証コードは Xiaomi 側でのみ入力し、BodyCast はそれらを見ることも保存もしません。ログイン後にブラウザの Cookie からアクセストークンを一つ受け取るだけです。",
            "Pagina sequens situs officialis Xiaomi est. Ratio, tessera, codex tantum apud Xiaomi intrantur; BodyCast ea nec videt nec servat — solum testimonium e Cookie accipit."
        )
        add(
            "只存三样东西：① 小米回给应用的访问令牌（serviceToken / passToken 等）；② 六类读数的聚合值；③ 上次同步时间。令牌用 Android Keystore 生成的 AES 密钥加密之后才落盘，**明文令牌不落盘**，应用日志与界面都不显示令牌内容。这条通道的请求只发生在你点「连接」与「立即同步」时，后台不会自己联网。",
            "Only three things are kept: (1) the access tokens Xiaomi returns (serviceToken / passToken etc.); (2) aggregate values of the six reading types; (3) the last sync time. Tokens are encrypted with an AES key from Android Keystore before they touch disk — **plaintext tokens are never written** — and neither logs nor UI ever show them. This channel only sends requests when you tap “Connect” or “Sync now”; it never goes online by itself.",
            "保存するのは三つだけです。① Xiaomi から返されるアクセストークン（serviceToken / passToken など）、② 6 種類の読み取りの集計値、③ 最終同期時刻。トークンは Android Keystore の AES 鍵で暗号化してから保存し、**平文のトークンは保存しません**。ログにも画面にもトークンは出ません。この経路の通信は「接続」と「今すぐ同期」を押したときだけです。バックグラウンドで勝手に通信することはありません。",
            "Tria sola servantur: (1) testimonia quae Xiaomi reddit (serviceToken / passToken etc.); (2) valores aggregati sex generum; (3) tempus ultimae synchronizationis. Testimonia cum clave AES ex Android Keystore cryptantur antequam in disco ponantur — **testimonia aperta numquam scribuntur** — neque in log neque in interfacie apparent. Haec via tantum cum “Coniunge” vel “Nunc synchroniza” premis, numquam sola."
        )
        add("没读到", "Not read", "読み取れません", "Non lectum")
        add("上次填写", "Last entered", "最終入力", "Ultima scriptura")
        add("你手动校准了「{0}」，这一条优先于设备读数", "You calibrated “{0}” by hand, which outranks device readings.", "「{0}」を手動調整しました。これはデバイスの測定値より優先されます。", "Tu calibravisti “{0}” manu; hoc praeponderat lectionibus.")
        add("另外，{0}暂时没读到，这部分没有参与判断。", "Also, {0} were not read; they did not take part in the judgment.", "また、{0}はまだ読み取れず、判定には加わりませんでした。", "Praeterea {0} nondum lecta sunt; in iudicio non affuerunt.")
        add("依据：", "Basis: ", "根拠：", "Fundamentum: ")


        // ---- 22.34.0 配对向导（⓪ 手环 ↔ 厂商 App）----
        add("⓪ 先把手环连到厂商 App", "0. Pair the band with the vendor app first", "0. まずバンドをメーカーの App とつなぐ", "0. Primum band cum app fabricatoris coniunge")
        add(
            "这是整条链的起点：手环的数据先到厂商的运动健康 App，才轮得到后面几步。手环和 BodyCast 之间没有直连。",
            "This is where the chain starts: band data reaches the maker's health app first, then the later steps apply. The band never talks to BodyCast directly.",
            "ここが連鎖の起点です。バンドのデータはまずメーカーの健康 App に届き、その後の手順に進みます。バンドと BodyCast が直接つながることはありません。",
            "Hic catena incipit: data band primum ad app fabricatoris veniunt, deinde sequentes passus valent. Band numquam directe cum BodyCast loquitur."
        )
        add("小米 / 红米手环", "Xiaomi / Redmi band", "Xiaomi / Redmi バンド", "Band Xiaomi / Redmi")
        add("装好「小米运动健康」，登录你的小米账号。", "Install Mi Fitness (小米运动健康) and sign in to your Mi account.", "「小米運動健康（Mi Fitness）」を入れ、Mi アカウントでログインします。", "Installe Mi Fitness et in rationem Mi intra.")
        add("在「我的」页或首页顶部找到「添加设备」。", "Find “Add device” on the Me tab or at the top of the home page.", "「我的」ページかホーム上部の「添加设备（デバイスを追加）」を開きます。", "Inveni “Add device” in pagina Me vel in summo domus.")
        add("扫包装盒上的二维码，或在列表里选你的手环型号。", "Scan the QR code on the box, or pick your band model from the list.", "箱の QR コードをスキャンするか、一覧からバンドの型番を選びます。", "Scanne codicem QR in cista aut elige exemplar band e lista.")
        add("手环屏幕会弹出配对请求，在手环上点确认。", "A pairing prompt appears on the band — confirm it on the band.", "バンドの画面にペアリング確認が出るので、バンド上で承認します。", "Petitio coniungendi in band apparet; in band confirma.")
        add("回到 App 首页，能看到今天的步数或睡眠就是配对成功。", "Back in the app's home page, seeing today's steps or sleep means pairing worked.", "App のホームに戻り、今日の歩数か睡眠が見えたらペアリング成功です。", "Reversus ad domum app, si hodierna passus vel somnus vides, coniunctio felix est.")
        add("华为 / 荣耀手环", "Huawei / Honor band", "HUAWEI / Honor バンド", "Band Huawei / Honor")
        add("装好「华为运动健康」或「荣耀运动健康」，登录账号。", "Install HUAWEI Health or Honor Health and sign in.", "「HUAWEI Health」か「Honor Health」を入れてログインします。", "Installe HUAWEI Health aut Honor Health et intra.")
        add("底部「设备」页 →「添加设备」。", "Devices tab at the bottom → “Add device”.", "下部の「设备（デバイス）」→「添加设备（デバイスを追加）」。", "Pagina “Devices” infra → “Add device”.")
        add("扫一扫或选型号，在手环上点确认。", "Scan or pick the model, then confirm on the band.", "スキャンか型番選びをして、バンド上で承認します。", "Scanne aut exemplar elige, deinde in band confirma.")
        add("同步一次，首页能看到今天的数据就是配对成功。", "Sync once; today's data on the home page means pairing worked.", "一度同期し、ホームに今日のデータが出れば成功です。", "Synchroniza semel; si hodierna data in domo vides, coniunctio felix est.")
        add("判断标准只有一个：打开厂商 App，能看到今天的数据。", "One simple check: open the maker's app and look for today's data.", "判定基準は一つだけ。メーカーの App を開いて今日のデータが見えるかです。", "Unum solum criterium: aperi app fabricatoris et vide num hodierna data adsint.")
        add("用小米手环的话，还有条更省事的路", "With a Xiaomi band, there is an easier route", "Xiaomi バンドなら、もっと楽な経路があります", "Cum band Xiaomi, via facilior adest")
        add(
            "「小米账号（云同步）」直接从云端取数，不依赖厂商 App 写系统健康服务。只要手环用小米运动健康同步过一次，数据就到了小米的云端。",
            "Mi account (cloud sync) fetches data straight from the cloud, without relying on the maker's app writing to Health Connect. One sync with Mi Fitness is enough for the data to reach Xiaomi's cloud.",
            "「Mi アカウント（クラウド同期）」はクラウドから直接データを取得します。メーカーの App が Health Connect に書き込む必要はありません。Mi Fitness で一度同期すれば、データは Xiaomi のクラウドに届きます。",
            "Ratio Mi (synchronizatio nubis) data directe e nube sumit, sine app fabricatoris ad Health Connect scribendo. Una synchronizatio Mi Fitness satis est ut data ad nubem Xiaomi veniant."
        )
        add("去看小米账号（云同步）", "Go to Mi account (cloud sync)", "Mi アカウント（クラウド同期）へ", "I ad rationem Mi (synchronizatio nubis)")
        add(
            "在「设置 → 数据与同步」里，就在这一页下面。",
            "It lives in Settings → Data and sync, just below on this page.",
            "「設定 → データと同期」にあります。このページのすぐ下です。",
            "In Optionibus → Data et synchronizatio, infra in hac pagina."
        )
        add(
            "连接后点「立即同步」，读到的六类数据和 Health Connect 那条路是一样的。两条路可以并存，谁的数据新用谁。",
            "After connecting, tap “Sync now”. The six reading types are the same as the Health Connect route. Both routes can coexist — the fresher data wins.",
            "接続後「今すぐ同期」を押すと、Health Connect の経路と同じ 6 種類のデータが読めます。両方の経路を併用でき、新しいデータが優先されます。",
            "Post coniunctionem, “Nunc synchroniza” preme. Sex genera eadem sunt atque via Health Connect. Utraque via simul valet; recentiora data praecellunt."
        )


        // ---- 22.34.0 厂商接入文案修正（三星 HC 同步 / 华为限制原因 / 小米开放接口现状）----
        add(
            "打开三星健康 → 设置 →「Health Connect」并打开同步（v6.22.5 起与系统健康服务双向同步）",
            "Open Samsung Health → Settings → “Health Connect” and turn on sync (two-way with the system health service since v6.22.5).",
            "Samsung Health を開く → 設定 →「Health Connect」で同期をオン（v6.22.5 以降、システム健康サービスと双方向同期）。",
            "Aperi Samsung Health → Optiones → “Health Connect” et synchronizam accende (a v6.22.5 cum servitio sanitatis utroque modo)."
        )
        add(
            "华为不把数据写进系统健康服务：官方接口（Health Service Kit）要企业开发者资质，个人开发者拿不到。这是厂商限制，本应用修不了，走下面的替代路。",
            "HUAWEI does not write data into the system health service: its official kit (Health Service Kit) requires a company developer account, which individuals cannot get. This is a vendor limit the app cannot work around — use one of the alternatives below.",
            "HUAWEI はシステムの健康サービスにデータを書き込みません。公式の Health Service Kit は法人開発者資格が必要で、個人では取得できません。メーカー側の制限のため、下の代替手段を使ってください。",
            "HUAWEI data ad servitium sanitatis non scribit: Health Service Kit rationem fabricae postulat, quam privatus non potest. Hoc limitatio fabricatoris est; viae infra utere."
        )
        add(
            "这一项在大陆区域可能被隐藏；看不到就等于这条路在本机走不通。官方的健康云开放接口目前也只对合作企业开放。授权前产生的历史不会补写。",
            "This entry may be hidden on mainland builds; if you cannot see it, this route is unavailable on this device. The official health-cloud API is also partner-only for now. Data from before you grant access is never backfilled.",
            "この項目は中国大陸版では非表示のことがあります。見えない場合、この経路は本機では使えません。公式の健康クラウド API も現在パートナー限定です。許可前のデータは遡って書き込まれません。",
            "Hoc intrum in editione continenti abscondi potest; si non apparet, via hic non valet. API nubis sanitatis officialis nunc solum sociis patet. Data ante permissum numquam suppleta sunt."
        )


        // ---- 22.35.0 开箱主题预览卡 ----
        add(
            "预览：按钮与强调色就是这个颜色",
            "Preview: buttons and accents use this color",
            "プレビュー：ボタンとアクセントはこの色になります",
            "Praevisio: gemmae et accentus hoc colore utuntur"
        )


        // ---- 22.36.0 同步数据明细表 ----
        add("同步数据明细", "Synced data detail", "同期データ明細", "Detailium datae synchronizatae")
        add("这就是应用当前手里的全部读数。", "This is every reading the app currently holds.", "これがアプリが現在持っている読み取りのすべてです。", "Haec sunt omnia quae app nunc tenet.")
        add("小米云未连接", "Mi cloud not connected", "Mi クラウド未接続", "Nubes Mi non coniuncta")
        add("已连接，还没有同步过", "Connected; not synced yet", "接続済み・まだ同期していません", "Coniunctum; nondum synchronizatum")
        add("上次同步：", "Last synced: ", "最終同期：", "Ultima synchronizatio: ")
        add("账号：", "Account: ", "アカウント：", "Ratio: ")
        add("区域：", "Region: ", "リージョン：", "Regio: ")
        add("小米云", "Mi cloud", "Mi クラウド", "Nubes Mi")
        add("手动", "Manual", "手動", "Manuale")
        add("睡眠时长", "Sleep duration", "睡眠時間", "Spatium somni")
        add("深睡占比", "Deep sleep share", "深睡の割合", "Pars somni profundi")
        add("心率变异性", "Heart rate variability", "心拍変動", "Variabilitas pulsus")


        // ---- 22.37.0 HC 内容透视 + 小米云扩面 ----
        add("Health Connect 里实际有什么（最近 30 天）", "What Health Connect actually holds (last 30 days)", "Health Connect に実際に入っているもの（直近 30 日）", "Quid Health Connect re vera continet (30 dies)")
        add("正在读 Health Connect…", "Reading Health Connect…", "Health Connect を読み取り中…", "Health Connect legitur…")
        add(
            "读不到 Health Connect（先在自检里把第①②步的服务和授权弄好）。",
            "Cannot read Health Connect (finish steps 1-2 in the self-check first).",
            "Health Connect を読み取れません（先にセルフチェックの①②を完了してください）。",
            "Health Connect legi non potest (primum gradus 1-2 perfice)."
        )
        add("0 条（厂商没写入）", "0 records (the vendor app wrote nothing)", "0 件（メーカーが書き込んでいない）", "0 notae (nihil scriptum)")
        add("条 · 最新 ", " records · latest ", " 件・最新 ", " notae · ultima ")
        add(
            "0 条 = 厂商 App 没往 Health Connect 写；有条但上面读不到 = 授权或读取侧的问题。",
            "0 records = the vendor app never writes to Health Connect; records exist but unread = an access/read-side problem.",
            "0 件 = メーカーの App が Health Connect に書いていない。あるのに読めない = 権限または読み取り側の問題。",
            "0 notae = app fabricatoris non scribit; notae adsunt sed non leguntur = quaestio permissionis."
        )
        add("压力值", "Stress level", "ストレス値", "Gradus pressurae")


        // ---- 22.37.0 华为/荣耀：导出文件明路 ----
        add(
            "不想装桥接也有一条明路：在华为运动健康 App 里「我的 → 设置 → 隐私管理 → 数据下载/导出」拿一份数据文件（不同版本入口略有差异），发到本应用的「导入数据文件」，今天的数据就能进来",
            "No bridge app? There is one clean route: in HUAWEI Health, get a data file via Me -> Settings -> Privacy management -> Data download/export (menu names vary by version), then send it to this app's Import data file - today's data comes right in.",
            "ブリッジを入れたくない場合の正道：HUAWEI Health の「マイページ → 設定 → プライバシー管理 → データのダウンロード/書き出し」でデータファイルを入手し（入口はバージョンにより多少異なります）、このアプリの「データファイルを読み込む」に送ると今日のデータが入ります。",
            "Sine ponte? Via recta: in HUAWEI Health, fasciculum dato cape per Me -> Optiones -> Privatus -> Data export, deinde ad Import in hoc app mitte."
        )
        add(
            "实在都不方便：手动填写读数永远可用——照着华为运动健康里的数字抄进来，24 小时内有效",
            "If none of that is convenient: manual entry always works - copy the numbers from HUAWEI Health by hand; they stay valid for 24 hours.",
            "どれも不便なら：手動入力はいつでも使えます。HUAWEI Health の数字を書き写すだけで、24 時間有効です。",
            "Si nihil facile est: semper potes manualiter scribere - numeros ex HUAWEI Health transcribe; per 24 horas valent."
        )


        // ---- 22.38.0 天气明信片 + 可玩开箱 ----
        add("生成天气明信片并分享", "Make a weather postcard to share", "天気ポストカードを作って共有", "Fac cartam meteorologicam et communica")
        add(
            "明信片是画出来的：天气球、一句话、日期和落款，不含任何数值、体重或热量。",
            "The postcard is drawn, not screenshotted: weather ball, one line, date and signature - never any numbers, weight or calories.",
            "ポストカードは描き起こしです：天気球、ひとこと、日付、署名のみ。数値・体重・カロリーは一切出ません。",
            "Carta pingitur: sphaera caeli, una linea, dies, subscriptio — sine numeris, pondere, caloriis."
        )
        add("身体气象台 · BodyCast", "BodyCast · body weather station", "BodyCast · 体調気象台", "BodyCast · statio meteorologica corporis")
        add("今日天气明信片", "Today's weather postcard", "今日の天気ポストカード", "Carta meteorologica hodierna")
        add("把明信片发出去", "Send the postcard", "ポストカードを送る", "Mitte cartam")
        add("点一下球，看看五种天气", "Tap the ball - see all five kinds of weather", "球をタップして、五つの天気を見てみよう", "Tange spheram - vide quinque caeli genera")


        // ---- 22.41.0 烹饪方法（食谱卡）：动词 + 「N 分钟」模式 ----
        add("烹饪", "Cook", "調理", "Coque")
        add("水煮", "Boil", "茹で", "Coque")
        add("清蒸", "Steam", "蒸す", "Vapore")
        add("快炒", "Stir-fry", "炒める", "Frigito")
        add("烤", "Roast", "焼く", "Assa")
        add("蒸", "Steam", "蒸す", "Vapore")
        add("煎", "Pan-fry", "焼く", "Frigito")
        add("炖", "Stew", "煮込む", "Coque lente")
        add("煮", "Simmer", "煮る", "Coque")
        add("焯水", "Blanch", "茹でこぼす", "Elige")
        add("干炒", "Dry-roast", "乾煎り", "Torre")
        add("烧", "Braise", "煮込む", "Coque lente")
        add("浸煮", "Poach", "ゆでる", "Coque")
        add("炸", "Deep-fry", "揚げる", "Frigito")
        add("低温烤", "Slow-roast", "低温で焼く", "Assa lente")
        add("温热", "Warm", "温める", "Calefac")
        add("加热", "Heat", "加熱する", "Calefac")
        add("泡", "Soak", "浸す", "Macerare")
        add("煮粥", "Simmer as porridge", "お粥にする", "Coque in pulmentum")
        add("煮汤", "Simmer in soup", "スープにする", "Coque in iure")
        add("冲汤", "Mix into soup", "スープに溶く", "Misce in iure")
        add("蒸蛋羹", "Steam custard", "茶碗蒸しにする", "Vapore custardum")
        add("直接吃", "Eat as is", "そのまま食べる", "Sicut est ede")
        add("拌水果", "Mix with fruit", "フルーツと和える", "Misce cum fructu")
        add("打奶昔", "Blend into a shake", "シェイクにする", "Miscere in potum")
        add("榨汁", "Juice it", "ジュースにする", "Exprime sucum")
        add("磨粉冲饮", "Grind and stir into a drink", "粉末にして飲む", "Tere et misce in potum")
        add("打豆浆", "Blend into soy milk", "豆乳にする", "Miscere in lac soiae")
        add("凉拌", "Toss cold", "和える", "Misce frigidum")
        add("打泥", "Mash", "なめらかにする", "Tere")
        add("夹好即食", "Assemble and eat", "挟んでそのまま", "Compone et ede")

        // ---- 22.41.0 导出：另存为/分享 ----
        add("导出数据", "Export data", "データを書き出す", "Exporta data")
        add("复制到剪贴板", "Copy to clipboard", "クリップボードにコピー", "Copy in tabula")
        add("另存为…", "Save as\u2026", "名前を付けて保存\u2026", "Serva ut\u2026")
        add("分享…", "Share\u2026", "共有\u2026", "Communica\u2026")
        add("把文件发出去", "Send the file", "ファイルを送る", "Mitte fasciculum")
        add("保存失败", "Save failed", "保存できませんでした", "Servare non potuit")
        add("文件没有写进去，请换个位置再试。", "The file was not written. Try a different location.", "ファイルを書き込めませんでした。別の場所を試してください。", "Fasciculus scriptus non est. Alium locum tenta.")
        add("保存方式", "How to save", "保存方法", "Quomodo servare")

    }
}
