package cn.pocket.toolbox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import cn.pocket.toolbox.R
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.ui.MainActivity

/**
 * 三种小组件的共享渲染器（22.35.0）：
 * - [strip] 2×1 今日天气条
 * - [signals] 2×2 四信号卡
 * - 主卡（2×2 天气 + 小事 + 吃什么）在 [BodyCastWidgetProvider] 里
 *
 * 三条纪律与主卡一致：
 * 1. 只用 RemoteViews 白名单控件（FrameLayout / LinearLayout / TextView）；
 * 2. 快照没有数据就写「还没有读到数据」，不编数字；
 * 3. 静态串按**应用语言**取——先把 [AppLanguage] 从 prefs 初始化，
 *    否则小组件进程会跟系统语言走（22.35.0 修的 i18n bug）。
 */
object WidgetRender {

    /** 应用内算完天气后调一次，让桌面上所有卡片立刻跟上。 */
    fun updateAll(context: Context) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val snapshot = WidgetSnapshot.load(context) ?: return
            val locale = localizedContext(context)
            updateKind(context, manager, BodyCastWidgetProvider::class.java) {
                BodyCastWidgetProvider.buildViews(locale, snapshot)
            }
            updateKind(context, manager, BodyCastStripWidgetProvider::class.java) {
                strip(locale, snapshot)
            }
            updateKind(context, manager, BodyCastSignalsWidgetProvider::class.java) {
                signals(locale, snapshot)
            }
            updateKind(context, manager, BodyCastActionWidgetProvider::class.java) {
                actionRow(locale, snapshot)
            }
            updateKind(context, manager, BodyCastFocusWidgetProvider::class.java) {
                focusRow(locale, snapshot)
            }
        }
    }

    private inline fun updateKind(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        build: () -> RemoteViews
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        val views = runCatching { build() }.getOrNull() ?: return
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /** 2×1 今日天气条。 */
    fun strip(context: Context, snapshot: WidgetSnapshot.Snapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_strip)
        views.applyTheme(snapshot.dark)
        val weatherColor = snapshot.colorHex.toInt()
        views.setOnClickPendingIntent(R.id.widget_strip_root, openApp(context, 0, ""))
        if (snapshot.kind.isBlank()) {
            views.setTextViewText(R.id.widget_strip_kind, context.getString(R.string.widget_no_data_title))
            views.setTextViewText(R.id.widget_strip_headline, context.getString(R.string.widget_no_data_body))
            return views
        }
        views.setInt(R.id.widget_accent_bar, "setBackgroundColor", weatherColor)
        views.setTextColor(R.id.widget_strip_kind, weatherColor)
        views.setTextViewText(R.id.widget_strip_kind, snapshot.kind)
        views.setTextViewText(R.id.widget_strip_headline, snapshot.headline)
        return views
    }

    /** 2×2 四信号卡。 */
    fun signals(context: Context, snapshot: WidgetSnapshot.Snapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_signals)
        views.applyTheme(snapshot.dark)
        views.setOnClickPendingIntent(R.id.widget_signals_root, openApp(context, 0, ""))
        // 标题也必须显式设置：布局里的 @string 默认值跟**系统**语言走，
        // 会和快照里按应用语言翻好的格子标题混排（22.35.0 修）。
        views.setTextViewText(R.id.widget_signals_title, context.getString(R.string.widget_signals_title))
        val titleIds = intArrayOf(R.id.widget_m0_title, R.id.widget_m1_title, R.id.widget_m2_title, R.id.widget_m3_title)
        val levelIds = intArrayOf(R.id.widget_m0_level, R.id.widget_m1_level, R.id.widget_m2_level, R.id.widget_m3_level)
        val defaultTitle = context.getString(R.string.widget_signals_title_default)
        val notRead = context.getString(R.string.widget_no_data_title)
        for (i in 0..3) {
            views.setTextViewText(titleIds[i], snapshot.metricTitles.getOrNull(i) ?: defaultTitle)
            views.setTextViewText(levelIds[i], snapshot.metricLevels.getOrNull(i) ?: notRead)
        }
        if (snapshot.focusLabel.isNotBlank()) {
            views.setViewVisibility(R.id.widget_signals_focus, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_signals_focus,
                context.getString(R.string.widget_focus_prefix, snapshot.focusLabel)
            )
        } else {
            views.setViewVisibility(R.id.widget_signals_focus, View.GONE)
        }
        return views
    }

    /** 2×1「今日微行动」（22.42.0）：一屏一件今天能做的小事。 */
    fun actionRow(context: Context, snapshot: WidgetSnapshot.Snapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_action_row)
        views.applyTheme(snapshot.dark)
        views.setOnClickPendingIntent(R.id.widget_action_row_root, openApp(context, 0, ""))
        views.setTextViewText(R.id.widget_action_row_title, context.getString(R.string.widget_action_title))
        if (snapshot.action.isBlank()) {
            views.setTextViewText(R.id.widget_action_row_text, context.getString(R.string.widget_no_data_title))
            return views
        }
        views.setInt(R.id.widget_accent_bar, "setBackgroundColor", snapshot.colorHex.toInt())
        views.setTextViewText(R.id.widget_action_row_text, snapshot.action)
        if (snapshot.actionMinutes > 0) {
            views.setTextViewText(
                R.id.widget_action_row_text,
                context.getString(R.string.widget_action_minutes, snapshot.action, snapshot.actionMinutes)
            )
        }
        return views
    }

    /** 2×1「今天先补」（22.42.0）：今天该补的元素，点一下去食谱页筛好。 */
    fun focusRow(context: Context, snapshot: WidgetSnapshot.Snapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_focus_row)
        views.applyTheme(snapshot.dark)
        views.setOnClickPendingIntent(
            R.id.widget_focus_row_root,
            openApp(context, 1, snapshot.focusId)
        )
        views.setTextViewText(R.id.widget_focus_row_title, context.getString(R.string.widget_focus_title))
        if (snapshot.focusLabel.isBlank()) {
            views.setTextViewText(R.id.widget_focus_row_text, context.getString(R.string.widget_no_data_title))
            views.setTextViewText(R.id.widget_focus_row_hint, context.getString(R.string.widget_no_data_body))
            return views
        }
        views.setInt(R.id.widget_accent_bar, "setBackgroundColor", snapshot.colorHex.toInt())
        views.setTextViewText(R.id.widget_focus_row_text, snapshot.focusLabel)
        views.setTextViewText(
            R.id.widget_focus_row_hint,
            // 22.49.0：有理由就写理由（带真实数字，例如「今天走了 12000 步——先补矿物质」），
            // 没有就退回原来那句「去找含 X 的食物」。
            snapshot.focusReason.ifBlank {
                context.getString(R.string.widget_focus_hint, snapshot.focusLabel)
            }
        )
        return views
    }

    


/** 打开应用（tab：0 首页 1 食谱；focusId 非空时顺带把食谱筛好）。 */
    fun openApp(context: Context, tab: Int, focusId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_TAB, tab)
        if (focusId.isNotBlank()) intent.putExtra(MainActivity.EXTRA_RECIPE_FOCUS, focusId)
        return PendingIntent.getActivity(
            context,
            if (focusId.isNotBlank()) 2 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 按当前应用语言造的上下文：getString 出来的静态串跟应用内选择走。 */
    fun localizedContext(context: Context): Context {
        // 小组件进程里 AppLanguage 的内存态是空的——不初始化就跟系统语言走了。
        AppLanguage.initializeFromPrefs(context)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(AppLanguage.current().locale)
        return context.createConfigurationContext(config)
    }
}

/**
 * 小组件的调色板（22.52.0）。
 *
 * 以前颜色全走 `@color/widget_*` 资源，`values-night` 只在**系统**切夜间模式时生效——
 * 用户在应用内把主题调成深色、系统还是亮的，小组件就一直是亮的。
 * 现在渲染时按快照里的 `dark` 用显式 ARGB 覆盖：setTextColor / setInt 都是 RemoteViews
 * 白名单方法，不需要额外的资源选择。
 */
private object WidgetPalette {
    // 亮（与 values/colors.xml 一致）
    const val LIGHT_INK = 0xFF12211C.toInt()
    const val LIGHT_MUTED = 0xFF5A6B64.toInt()
    const val LIGHT_LABEL = 0xFF6B7F77.toInt()
    const val LIGHT_FAINT = 0xFF7A8A83.toInt()

    // 暗（与 values-night/colors.xml 一致）
    const val DARK_INK = 0xFFF1F5F3.toInt()
    const val DARK_MUTED = 0xFFC7D2CD.toInt()
    const val DARK_LABEL = 0xFF8FA39B.toInt()
    const val DARK_FAINT = 0xFF6F7C77.toInt()
}

/** 每张 layout 的根与它内部需要换色的文字。 */
private val ROOT_IDS = intArrayOf(
    R.id.widget_root,
    R.id.widget_strip_root,
    R.id.widget_signals_root,
    R.id.widget_action_row_root,
    R.id.widget_focus_row_root
)

/** 小标题（label 色，与各 layout 里的 @color/widget_label 一致）。 */
private val LABEL_IDS = intArrayOf(
    R.id.widget_signals_title,
    R.id.widget_m0_title,
    R.id.widget_m1_title,
    R.id.widget_m2_title,
    R.id.widget_m3_title,
    R.id.widget_action_row_title,
    R.id.widget_focus_row_title
)

/** 说明性文字（muted 色）。 */
private val MUTED_IDS = intArrayOf(
    R.id.widget_headline,
    R.id.widget_strip_headline,
    R.id.widget_focus_row_hint
)

/** 更淡的一层（faint 色）。 */
private val FAINT_IDS = intArrayOf(
    R.id.widget_stamp
)

/** 主体文字（ink 色）。 */
private val INK_IDS = intArrayOf(
    R.id.widget_kind,
    R.id.widget_strip_kind,
    R.id.widget_focus_text,
    R.id.widget_action_row_text,
    R.id.widget_focus_row_text,
    R.id.widget_m0_level,
    R.id.widget_m1_level,
    R.id.widget_m2_level,
    R.id.widget_m3_level
)

/** 强调色文字（accent 色）：深浅两套里强调色相同，这里不改。 */
private val ACCENT_TEXT_IDS = intArrayOf(
    R.id.widget_action,
    R.id.widget_signals_focus
)

/**
 * 按应用内的深浅色把一张 layout 刷一遍。
 *
 * 底板与胶囊用**两张显式 drawable**（亮/暗各一张），文字用显式 ARGB——
 * 这两样都是 RemoteViews 白名单里的方法（setBackgroundResource / setTextColor）。
 * 找不到某个 id 时静默跳过：五张布局各有各的控件，不该为别的布局里没有的控件报错。
 */
internal fun RemoteViews.applyTheme(dark: Boolean) {
    val bg = if (dark) R.drawable.widget_background_dark else R.drawable.widget_background
    val ink = if (dark) WidgetPalette.DARK_INK else WidgetPalette.LIGHT_INK
    val muted = if (dark) WidgetPalette.DARK_MUTED else WidgetPalette.LIGHT_MUTED
    val label = if (dark) WidgetPalette.DARK_LABEL else WidgetPalette.LIGHT_LABEL
    val faint = if (dark) WidgetPalette.DARK_FAINT else WidgetPalette.LIGHT_FAINT

    ROOT_IDS.forEach { id -> runCatching { setInt(id, "setBackgroundResource", bg) } }
    // 只有「今天先补」那个胶囊跟着深浅走；「今天吃什么」按钮用的是强调色底，
    // 两套主题里同一个绿，不需要换。

    INK_IDS.forEach { id -> runCatching { setTextColor(id, ink) } }
    LABEL_IDS.forEach { id -> runCatching { setTextColor(id, label) } }
    MUTED_IDS.forEach { id -> runCatching { setTextColor(id, muted) } }
    FAINT_IDS.forEach { id -> runCatching { setTextColor(id, faint) } }
}
