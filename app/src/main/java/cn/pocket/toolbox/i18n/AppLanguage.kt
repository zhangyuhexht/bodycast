package cn.pocket.toolbox.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/**
 * 应用语言。支持简体中文、英语、日语、拉丁文（Latina）四种。
 *
 * 跟随系统的规则：系统是中文（大陆 / 台湾 / 香港 / 澳门都算）→ 简体中文；
 * 系统是日语 → 日语；其它任何语言（英语、法语、俄语…）→ 英语。
 * **拉丁文只能由应用内的语言浮层显式选中**——系统不会有 `la` 这个语言，
 * 所以 [fromSystem] 永远不返回 [LATIN]。
 *
 * 枚举顺序就是设置页语言浮层里的选项顺序（[cn.pocket.toolbox.ui.bodycast.SettingsTab]
 * 用 `AppLanguage.entries`），所以顺序 = 简中 → English → 日本語 → Latina。
 *
 * 切换方式是**内存状态**而不是 `AppCompatDelegate.setApplicationLocales`：
 * 后者一定会重建 Activity，用户会看到界面整体闪一下、设置二级页被弹回一级页，
 * 体感就是「卡一下」。这里所有文案都经过 [BodyCastText] 读 [current]，
 * 状态一变整屏立刻重组，切换是瞬间完成的，不重建、不闪、不丢当前页面。
 */
enum class AppLanguage(
    val tag: String,
    val selfName: String
) {
    SIMPLIFIED_CHINESE("zh-Hans", "简体中文"),
    TRADITIONAL_CHINESE("zh-Hant", "繁體中文"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    JAPANESE("ja", "日本語"),
    LATIN("la", "Latina");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        val default = SIMPLIFIED_CHINESE

        /** 所有语言自称的集合：界面上这些词永远按原文显示，不翻译、不简繁转换。 */
        val SELF_NAMES: Set<String> = entries.mapTo(mutableSetOf()) { it.selfName }

        /** 当前生效的语言。首次读取前为 null，表示还没初始化。 */
        private val selected = mutableStateOf<AppLanguage?>(null)

        /**
         * 存量偏好里的语言值 → 枚举。
         *
         * `"ja" / "日语" / "日本語"` 三条是 **11.0.0 五语时期旧值的迁移路径**：
         * 那时候选过日语的用户，pref 里存的就是这些值，升级后应该落到日语，
         * 而不是被当成未知语言丢回英语。繁体中文仍然并入简体中文；
         * 俄语已经下线，`"ru"` 保持回退到英语的行为。
         *
         * `"la" / "lat" / "拉丁文" / "拉丁語" / "Latina"` 是拉丁文的存值；
         * 系统级「应用语言」选择器里选过拉丁文时，也是从这里落回来。
         */
        fun fromStored(value: String?): AppLanguage = when (value?.trim()) {
            "zh-Hans", "zh-CN", "zh-SG", "zh-MY", "简体中文", "簡體中文", "auto" -> SIMPLIFIED_CHINESE
            "zh-Hant", "zh-TW", "zh-HK", "zh-MO", "繁体中文", "繁體中文" -> TRADITIONAL_CHINESE
            "en", "英语", "英語", "English" -> ENGLISH
            "es", "西班牙语", "西班牙語", "Español", "Spanish", "es-ES", "es-MX" -> SPANISH
            "de", "德语", "德語", "Deutsch", "German", "de-DE", "de-AT" -> GERMAN
            "ja", "日语", "日語", "日本語" -> JAPANESE
            "la", "lat", "拉丁文", "拉丁語", "拉丁语", "Latina", "Latin" -> LATIN
            "ru", "俄语", "俄語", "Русский" -> ENGLISH
            else -> entries.firstOrNull { it.tag.equals(value, ignoreCase = true) } ?: ENGLISH
        }

        /** 系统语言 → 枚举。**拉丁文不参与**：任何系统都不会把 `la` 作为界面语言报上来。 */
        fun fromSystem(locale: Locale = Locale.getDefault()): AppLanguage = when {
            locale.language.equals("zh", ignoreCase = true) ->
                if (locale.script.equals("Hant", ignoreCase = true) ||
                    locale.country.equals("TW", ignoreCase = true) ||
                    locale.country.equals("HK", ignoreCase = true) ||
                    locale.country.equals("MO", ignoreCase = true)
                ) {
                    TRADITIONAL_CHINESE
                } else {
                    SIMPLIFIED_CHINESE
                }
            locale.language.equals("ja", ignoreCase = true) -> JAPANESE
            locale.language.equals("es", ignoreCase = true) -> SPANISH
            locale.language.equals("de", ignoreCase = true) -> GERMAN
            else -> ENGLISH
        }

        /** 当前语言：已初始化就用手里的，否则按系统语言推断。 */
        fun current(): AppLanguage = selected.value ?: fromSystem()

        /**
         * 切换语言。只改内存状态，不重建 Activity——
         * 调用方负责把选择持久化到 prefs（下次冷启动由 [initializeFromPrefs] 读回）。
         */
        fun apply(language: AppLanguage) {
            selected.value = language
        }

        /**
         * 冷启动时调用一次，按优先级决定初始语言：
         * 1. 系统「应用语言」设置里显式选过的（AppCompatDelegate）；
         * 2. 应用内选择里显式选过的；
         * 3. 都没选过 → 跟随系统语言。
         */
        fun initializeFromPrefs(context: Context) {
            if (selected.value != null) return
            val systemChoice = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
            val stored = context.applicationContext
                .getSharedPreferences("bodycast_v3", Context.MODE_PRIVATE)
                .getString("prefs", null)
                ?.split(';')
                ?.firstOrNull { it.startsWith("lang=") }
                ?.substringAfter('=')
            selected.value = when {
                !systemChoice.isNullOrBlank() -> fromStored(systemChoice)
                stored.isNullOrBlank() || stored == "auto" ->
                    fromSystem(context.resources.configuration.locales[0])
                else -> fromStored(stored)
            }
        }
    }
}
