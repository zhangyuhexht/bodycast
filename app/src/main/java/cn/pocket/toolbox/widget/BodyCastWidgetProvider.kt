package cn.pocket.toolbox.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import cn.pocket.toolbox.R
import cn.pocket.toolbox.ui.MainActivity

/**
 * 桌面小组件：一眼看到今天的身体天气（22.0.0 新增；22.35.0 并入三种卡的统一刷新）。
 *
 * 用传统 RemoteViews（没有引入 Glance，避免为了一个卡片动构建配置）。
 * 刷新由系统按 updatePeriodMillis（30 分钟，系统允许的最小值）触发，
 * 应用内算完天气时也会主动推一次——Doze 下系统刷新可能被推迟，这是系统的
 * 省电策略，不是 bug（所以卡片上会显示「几点更新的」，用户自己能判断新不新）。
 *
 * 数据只读 [WidgetSnapshot]；没有快照时显示「还没有读到数据」，
 * 不会用 0 或任何编造的数字充数。
 */
class BodyCastWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val locale = WidgetRender.localizedContext(context)
        appWidgetIds.forEach { id ->
            runCatching {
                val snapshot = WidgetSnapshot.load(context)
                appWidgetManager.updateAppWidget(
                    id,
                    if (snapshot == null) buildEmpty(locale) else buildViews(locale, snapshot)
                )
            }
        }
    }

    companion object {

        /** 应用内算完天气后调一次，让桌面上所有卡片（三种）立刻跟上。 */
        fun updateAll(context: Context) {
            WidgetRender.updateAll(context)
        }

        private fun buildEmpty(localized: Context): RemoteViews {
            val views = RemoteViews(localized.packageName, R.layout.widget_bodycast)
            WidgetSnapshot.load(localized)?.let { views.applyTheme(it.dark) }
            views.setTextViewText(R.id.widget_kind, localized.getString(R.string.widget_no_data_title))
            views.setTextViewText(R.id.widget_headline, localized.getString(R.string.widget_no_data_body))
            views.setTextViewText(R.id.widget_action, "")
            views.setTextViewText(R.id.widget_focus_text, "")
            views.setViewVisibility(R.id.widget_focus, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_eat_button, android.view.View.GONE)
            views.setTextViewText(R.id.widget_stamp, "")
            views.setOnClickPendingIntent(R.id.widget_root, WidgetRender.openApp(localized, 0, ""))
            return views
        }

        /**
         * 主卡（2×2）：天气 + 今日小事 + 今天先补 + 吃什么按钮。
         * [localized] 必须是按应用语言配好的上下文（见 [WidgetRender.localizedContext]）。
         */
        fun buildViews(localized: Context, snapshot: WidgetSnapshot.Snapshot): RemoteViews {
            val views = RemoteViews(localized.packageName, R.layout.widget_bodycast)
            views.applyTheme(snapshot.dark)

            views.setOnClickPendingIntent(R.id.widget_root, WidgetRender.openApp(localized, 0, ""))

            views.setTextViewText(R.id.widget_kind, snapshot.kind)
            views.setTextViewText(R.id.widget_headline, snapshot.headline)
            views.setTextViewText(
                R.id.widget_action,
                if (snapshot.action.isNotBlank()) {
                    localized.getString(R.string.widget_action_prefix, snapshot.action)
                } else {
                    ""
                }
            )

            // 强调条随天气染色——这是整张卡的「脸色」。
            val weatherColor = snapshot.colorHex.toInt()
            views.setInt(R.id.widget_accent_bar, "setBackgroundColor", weatherColor)
            views.setTextColor(R.id.widget_kind, weatherColor)

            if (snapshot.focusLabel.isNotBlank()) {
                views.setViewVisibility(R.id.widget_focus, android.view.View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_focus_text,
                    localized.getString(R.string.widget_focus_prefix, snapshot.focusLabel)
                )
            } else {
                views.setViewVisibility(R.id.widget_focus, android.view.View.GONE)
            }
            if (snapshot.focusId.isNotBlank()) {
                views.setViewVisibility(R.id.widget_eat_button, android.view.View.VISIBLE)
                // 按钮文字与字号必须在代码里显式设置——实测布局里的
                // @string 默认值与 textSize 会被部分 Launcher 的 RemoteViews
                // 渲染管线丢掉：文字在节点树里存在但只测出 10px 高（被裁到看不见）。
                // setTextViewTextSize 走 RemoteViews 原生方法，强制重新度量。
                views.setTextViewText(R.id.widget_eat_button, localized.getString(R.string.widget_eat_button))
                views.setTextColor(R.id.widget_eat_button, 0xFF0B1512.toInt())
                views.setOnClickPendingIntent(
                    R.id.widget_eat_button,
                    WidgetRender.openApp(localized, 1, snapshot.focusId)
                )
            } else {
                views.setViewVisibility(R.id.widget_eat_button, android.view.View.GONE)
            }

            val stamp = java.text.SimpleDateFormat("HH:mm", cn.pocket.toolbox.i18n.AppLanguage.current().locale)
                .format(java.util.Date(snapshot.updatedAt))
            views.setTextViewText(
                R.id.widget_stamp,
                if (snapshot.demo) {
                    localized.getString(R.string.widget_stamp_demo, stamp)
                } else {
                    localized.getString(R.string.widget_stamp, stamp)
                }
            )
            return views
        }
    }
}
