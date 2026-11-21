package cn.pocket.toolbox.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * 桌面小组件 2×1「今日天气条」（22.35.0）：一行看今天——天气词 + 一句结论。
 * 渲染走 [WidgetRender.strip]，数据只读 [WidgetSnapshot]。
 */
class BodyCastStripWidgetProvider : AppWidgetProvider() {

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
                    WidgetRender.strip(locale, EMPTY)
                } else {
                    WidgetRender.strip(locale, snapshot)
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
