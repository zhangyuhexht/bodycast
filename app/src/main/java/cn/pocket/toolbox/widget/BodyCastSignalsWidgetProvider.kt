package cn.pocket.toolbox.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * 桌面小组件 2×2「四信号」（22.35.0）：恢复云量 / 压力风速 / 能量湿度 / 睡眠云层一眼看全，
 * 底部一行「今天先补」。渲染走 [WidgetRender.signals]。
 */
class BodyCastSignalsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val locale = WidgetRender.localizedContext(context)
        appWidgetIds.forEach { id ->
            runCatching {
                val snapshot = WidgetSnapshot.load(context)
                val views = if (snapshot == null) {
                    WidgetRender.signals(locale, EMPTY)
                } else {
                    WidgetRender.signals(locale, snapshot)
                }
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }

    private companion object {
        val EMPTY = WidgetSnapshot.Snapshot(
            kind = "", motto = "", headline = "", action = "",
            focusLabel = "", focusId = "", colorHex = 0xFF3ED598L,
            updatedAt = 0L, demo = false
        )
    }
}
