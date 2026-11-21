package cn.pocket.toolbox.ui.bodycast

/**
 * 设置搜索的索引（22.55.0）。
 *
 * 设置的二/三级菜单加起来有几十项，用户要找一个开关（「过敏原提醒在哪」）
 * 得一层层翻。这里把**二级菜单里的行**也登记进来，一级菜单顶部搜一下就能直达：
 * 每条只带「叫什么、在哪一页、点进去落到哪个 route」。
 *
 * 纪律：
 * - 只登记**真实存在**的条目（改了设置页要回来同步，测试脚本会核对数量）；
 * - 关键词里带上同义词（「深色/夜间」「暗色」都能搜到外观页），
 *   这样用户不需要记得我们起的名字；
 * - 三级菜单（开发者模式）里的项按「落到开发者页」登记，不做逐项定位。
 */
object SettingsSearch {

    /** 一条可搜的设置项。 */
    data class Item(
        /** 显示在结果里的名字（中文源串，渲染时过 BodyCastText）。 */
        val title: String,
        /** 它属于哪一页（结果里的第二行）。 */
        val group: String,
        /** 点进去落到哪一页。 */
        val route: SettingsSearchRoute,
        /** 额外关键词（同义词、口语说法）。 */
        val keywords: List<String> = emptyList()
    ) {
        fun matches(query: String): Boolean = hitOf(query) != null

        /** 命中在哪：返回**用户输入的那个词出现在哪**，没命中返回 null。 */
        fun hitOf(query: String): String? {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return null
            if (title.lowercase().contains(q)) return title
            if (group.lowercase().contains(q)) return group
            return keywords.firstOrNull { it.lowercase().contains(q) }
        }
    }

    /** 与 SettingsTab 的二级页路由一一对应（在那边映射成真正的 SettingsRoute）。 */
    enum class SettingsSearchRoute {
        DATA_SOURCE, CALIBRATION, PRESCRIPTION, RECIPE, NOTIFY,
        MY_DATA, PERMISSIONS, APPEARANCE, ABOUT, DEVELOPER,
        MANUAL_ENTRY, MI_CLOUD, SETUP_GUIDE, IMPORT_FILE, DELETE_ALL, LANGUAGE, THEME
    }

    val ITEMS: List<Item> = listOf(
        // ---- 一级菜单 ----
        Item("数据与同步", "设置首页", SettingsSearchRoute.DATA_SOURCE, listOf("手环", "接入", "同步", "health connect")),
        Item("身体天气校准", "设置首页", SettingsSearchRoute.CALIBRATION, listOf("校准", "默认天气", "敏感度")),
        Item("微行动与处方", "设置首页", SettingsSearchRoute.PRESCRIPTION, listOf("微行动", "处方", "时长", "语音")),
        Item("食谱指南", "设置首页", SettingsSearchRoute.RECIPE, listOf("食谱", "饮食", "偏好", "过敏")),
        Item("通知与提醒", "设置首页", SettingsSearchRoute.NOTIFY, listOf("通知", "提醒", "铃")),
        Item("我的数据", "设置首页", SettingsSearchRoute.MY_DATA, listOf("导出", "备份", "恢复", "删除")),
        Item("权限与说明", "设置首页", SettingsSearchRoute.PERMISSIONS, listOf("权限", "隐私", "说明")),
        Item("外观与体验", "设置首页", SettingsSearchRoute.APPEARANCE, listOf("主题", "深色", "夜间", "字号", "语言")),
        Item("关于", "设置首页", SettingsSearchRoute.ABOUT, listOf("版本", "开发者", "开源")),

        // ---- 数据与同步 ----
        Item("授权向导", "数据与同步", SettingsSearchRoute.SETUP_GUIDE, listOf("配对", "手环", "一步一步")),
        Item("连接运动数据", "数据与同步", SettingsSearchRoute.SETUP_GUIDE, listOf("三步", "授权")),
        Item("手动填写读数", "数据与同步", SettingsSearchRoute.MANUAL_ENTRY, listOf("手抄", "输入", "没数据")),
        Item("导入数据文件", "数据与同步", SettingsSearchRoute.IMPORT_FILE, listOf("csv", "json", "分享")),
        Item("小米账号（云同步）", "数据与同步", SettingsSearchRoute.MI_CLOUD, listOf("小米", "登录", "云端")),
        Item("数据来源与优先级", "数据与同步", SettingsSearchRoute.DATA_SOURCE, listOf("来源", "优先级")),

        // ---- 身体天气校准 ----
        Item("默认天气类型", "身体天气校准", SettingsSearchRoute.CALIBRATION, listOf("默认")),
        Item("校准标签", "身体天气校准", SettingsSearchRoute.CALIBRATION, listOf("感觉", "标签")),
        Item("敏感度", "身体天气校准", SettingsSearchRoute.CALIBRATION, listOf("敏感")),

        // ---- 微行动与处方 ----
        Item("时长预算", "微行动与处方", SettingsSearchRoute.PRESCRIPTION, listOf("几分钟", "时间")),
        Item("可用场景", "微行动与处方", SettingsSearchRoute.PRESCRIPTION, listOf("工位", "地铁", "床上")),
        Item("语音引导", "微行动与处方", SettingsSearchRoute.PRESCRIPTION, listOf("朗读", "语音", "念")),
        Item("安静优先", "微行动与处方", SettingsSearchRoute.PRESCRIPTION, listOf("安静")),
        Item("避开的类别", "微行动与处方", SettingsSearchRoute.PRESCRIPTION, listOf("不要", "避开")),

        // ---- 食谱指南 ----
        Item("饮食偏好", "食谱指南", SettingsSearchRoute.RECIPE, listOf("素食", "乳制品", "海鲜", "低糖")),
        Item("过敏原提醒", "食谱指南", SettingsSearchRoute.RECIPE, listOf("过敏", "提醒")),
        Item("搜索范围", "食谱指南", SettingsSearchRoute.RECIPE, listOf("范围")),

        // ---- 通知与提醒 ----
        Item("每日倾听提醒", "通知与提醒", SettingsSearchRoute.NOTIFY, listOf("提醒", "每天")),
        Item("微行动提醒", "通知与提醒", SettingsSearchRoute.NOTIFY, listOf("提醒", "行动")),
        Item("身体来信提醒", "通知与提醒", SettingsSearchRoute.NOTIFY, listOf("来信", "每周")),
        Item("免打扰时段", "通知与提醒", SettingsSearchRoute.NOTIFY, listOf("安静", "别打扰")),

        // ---- 我的数据 ----
        Item("导出到剪贴板", "我的数据", SettingsSearchRoute.MY_DATA, listOf("导出")),
        Item("备份与恢复", "我的数据", SettingsSearchRoute.MY_DATA, listOf("备份", "恢复")),
        Item("删除所有数据", "我的数据", SettingsSearchRoute.DELETE_ALL, listOf("清空", "删除")),

        // ---- 权限与说明 ----
        Item("授权状态", "权限与说明", SettingsSearchRoute.PERMISSIONS, listOf("权限", "状态")),
        Item("隐私说明", "权限与说明", SettingsSearchRoute.PERMISSIONS, listOf("隐私")),

        // ---- 外观与体验 ----
        Item("深浅色模式", "外观与体验", SettingsSearchRoute.THEME, listOf("深色", "夜间", "暗色", "浅色", "跟随系统")),
        Item("主题色", "外观与体验", SettingsSearchRoute.APPEARANCE, listOf("强调色", "绿色", "蓝色")),
        Item("字体大小", "外观与体验", SettingsSearchRoute.APPEARANCE, listOf("字号", "字大")),
        Item("界面语言", "外观与体验", SettingsSearchRoute.LANGUAGE, listOf("语言", "english", "日本語", "español")),
        Item("动画效果", "外观与体验", SettingsSearchRoute.APPEARANCE, listOf("动画", "减少动画")),

        // ---- 关于 / 三级 ----
        Item("开源许可", "关于", SettingsSearchRoute.ABOUT, listOf("许可", "开源")),
        Item("重看新手引导", "关于", SettingsSearchRoute.ABOUT, listOf("引导", "开箱")),
        Item("开发者模式", "关于", SettingsSearchRoute.DEVELOPER, listOf("测试", "实验性")),
        Item("强制磨砂降级", "开发者模式", SettingsSearchRoute.DEVELOPER, listOf("液态玻璃", "折射", "磨砂", "开关")),
        Item("强调色（实验）", "开发者模式", SettingsSearchRoute.DEVELOPER, listOf("实验")),
        Item("同步数据明细", "开发者模式", SettingsSearchRoute.DEVELOPER, listOf("读数", "明细", "步数", "睡眠", "心率")),
        Item("一键填入演示读数", "开发者模式", SettingsSearchRoute.DEVELOPER, listOf("演示", "测试数据"))
    )

    /** 搜索框空着时给用户看几个常见去处（人性化：别让框空着）。 */
    val SUGGESTIONS: List<String> = listOf("深色", "语音", "备份", "过敏", "玻璃", "授权", "字号")

    /** 按关键词过滤；空查询返回空列表。 */
    fun search(query: String): List<Item> =
        if (query.isBlank()) emptyList() else ITEMS.filter { it.matches(query) }
}
