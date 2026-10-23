package com.example.cognibal.screen.feature.settings.budget

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cognibal.MainActivity
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.repository.BudgetImpact
import com.example.cognibal.data.repository.BudgetLevel

/**
 * 预算通知。没开偏好、或没有通知权限时直接跳过。
 * 同一条通知会覆盖上一次，避免连续记账堆出一串。
 */
internal class BudgetNotifier(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun publish(impacts: List<BudgetImpact>, categoryNames: Map<Long, String>) {
        if (!canPost()) return
        val notable = impacts.filter { impact ->
            val after = impact.levelAfter()
            after != BudgetLevel.Normal && after != impact.levelBefore()
        }
        if (notable.isEmpty()) return
        val over = notable.any { impact -> impact.levelAfter() == BudgetLevel.Over }
        val listed = notable.filter { impact ->
            impact.levelAfter() == if (over) BudgetLevel.Over else BudgetLevel.Near
        }
        val separator = appContext.getString(R.string.budget_name_separator)
        val global = appContext.getString(R.string.bill_budget_global)
        val missing = appContext.getString(R.string.bill_budget_missing)
        val names = listed.joinToString(separator) { impact ->
            when (impact.budget.budgetType) {
                BudgetType.Global -> global
                BudgetType.Category -> {
                    val categoryId = impact.budget.categoryId
                    categoryId?.let { categoryNames[it] } ?: missing
                }
            }
        }
        val text = appContext.getString(
            if (over) R.string.budget_notification_over else R.string.budget_notification_near,
            names,
        )
        val manager = NotificationManagerCompat.from(appContext)
        val channelName = appContext.getString(R.string.budget_notification_channel)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(channelName)
                .build(),
        )
        val open = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_notification)
            .setContentTitle(channelName)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 权限在检查之后被收回。下次记账再试。
        }
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    private companion object {
        const val CHANNEL_ID: String = "cognibal.budget"
        const val NOTIFICATION_ID: Int = 41
    }
}
