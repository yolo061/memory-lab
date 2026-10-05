package com.shiguangji.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.shiguangji.app.MainActivity
import com.shiguangji.app.R

/**
 * 桌面快捷「写记录」小组件：点击直达编辑页。
 * 纯快捷入口，不展示数据，因此无需任何刷新逻辑；
 * 按小米规范跑在 :widgetProvider 独立进程，跳转用 PendingIntent.getActivity 直达。
 */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setOnClickPendingIntent(android.R.id.background, editorPendingIntent(context))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        fun editorPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_WIDGET_ROUTE, "editor")
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
