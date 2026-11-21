package cn.pocket.toolbox.i18n

/**
 * 漏翻审计。
 *
 * 两路缺口分开记，因为它们的修法不一样：
 * - [recordFallback]：整条串**从没进过任何表**（连中文源串都没登记），
 *   修法是把源串加进 `add()` 或写成一条模板；
 * - [recordSheetGap]：串在表里，但**当前语言的槽位是空的**，界面回退成英语。
 *   日语界面「还在显示英语」几乎全是这一类，而且只有这里查得出来——
 *   日语本来就有汉字，用「屏幕上有没有 CJK」是筛不出漏翻的。
 *
 * 出口在设置 → 开发者模式 →「漏翻审计」（只在开发者模式里露出）。
 */
object TranslationAudit {
    private val fallbackSources = linkedSetOf<String>()
    private val sheetGaps = linkedSetOf<String>()

    @Synchronized
    fun recordFallback(source: String) {
        fallbackSources += source
    }

    @Synchronized
    fun fallbackSnapshot(): Set<String> = fallbackSources.toSet()

    /** 键带语言前缀（`[ja] 首页`），因为同一条串缺的可能是不同语言。 */
    @Synchronized
    fun recordSheetGap(source: String, language: AppLanguage) {
        sheetGaps += "[" + language.tag + "] " + source
    }

    @Synchronized
    fun sheetGapSnapshot(): Set<String> = sheetGaps.toSet()

    /** 某一门语言的逐句表缺口条数，给开发者页显示用。 */
    @Synchronized
    fun sheetGapCount(language: AppLanguage): Int =
        sheetGaps.count { it.startsWith("[" + language.tag + "] ") }

    @Synchronized
    fun clear() {
        fallbackSources.clear()
        sheetGaps.clear()
    }
}
