package cn.pocket.toolbox.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * 桌面小组件 2×1「今日微行动」（22.42.0）：一屏一件今天能做的小事。
 * 渲染走 [WidgetRender.actionRow]，数据只读 [WidgetSnapshot]。
 */
class BodyCastActionWidgetProvider : AppWidgetProvider() {

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
                    WidgetRender.actionRow(locale, EMPTY)
                } else {
                    WidgetRender.actionRow(locale, snapshot)
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
