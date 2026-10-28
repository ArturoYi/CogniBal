package com.example.cognibal.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** 系统微件选择器里的「知余」。名称用应用名，长按图标时才能对上这张卡片。 */
class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LedgerAppWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        HomeWidgetRefresh.ensureStarted(context)
        WidgetClock.cancel(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        HomeWidgetRefresh.ensureStarted(context)
        WidgetClock.cancel(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetClock.cancel(context)
        HomeWidgetRefresh.stop()
    }
}

/**
 * 以前卡片显示时分，用这个闹钟大约每分钟刷新一次。
 * 现在只显示年月，跨月由账本订阅处理。已经排上的闹钟在卡片更新时取消。
 */
internal object WidgetClock {
    private const val ACTION: String = "com.example.cognibal.widget.TICK"

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, LedgerWidgetReceiver::class.java).setAction(ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
