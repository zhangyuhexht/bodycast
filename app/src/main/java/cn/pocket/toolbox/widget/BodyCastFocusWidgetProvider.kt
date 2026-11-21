package cn.pocket.toolbox.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * 桌面小组件 2×1「今天先补」（22.42.0）：一眼看到今天该补什么元素，
 * 点一下直达食谱页并按这个元素筛好。
 * 渲染走 [WidgetRender.focusRow]，数据只读 [WidgetSnapshot]。
 */
class BodyCastFocusWidgetProvider : AppWidgetProvider() {

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
                    WidgetRender.focusRow(locale, EMPTY)
                } else {
                    WidgetRender.focusRow(locale, snapshot)
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
