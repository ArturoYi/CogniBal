package com.example.cognibal.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.cognibal.MainActivity
import com.example.cognibal.R
import com.example.cognibal.shortcut.AppShortcuts
import java.util.Locale

/**
 * 桌面卡片：当前年月、本月收入支出结余，底下是记账。
 *
 * 尺寸用 Exact，窄的时候字号收一档，三列数字仍然留着。
 */
class LedgerAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = LedgerWidgetLoader.load(context)
        provideContent {
            LedgerGlanceTheme(monochrome = content.monochrome) {
                LedgerWidgetBody(content)
            }
        }
    }
}

@Composable
private fun LedgerWidgetBody(content: LedgerWidgetContent) {
    val context = LocalContext.current
    val locale = widgetLocale(context)
    val overflow = context.getString(R.string.account_balance_overflow)
    val record = Intent(context, MainActivity::class.java).apply {
        action = AppShortcuts.ACTION_ADD
        putExtra(AppShortcuts.EXTRA_KIND, AppShortcuts.KIND_EXPENSE)
    }
    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        horizontalPadding = 16.dp,
        modifier = GlanceModifier.fillMaxSize().padding(vertical = 12.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = widgetNowLabel(locale),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(12.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Figure(
                    label = context.getString(R.string.bill_figure_income),
                    amount = widgetMoney(content.currencySymbol, content.income, false, overflow, locale),
                    color = GlanceTheme.colors.primary,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Figure(
                    label = context.getString(R.string.bill_figure_expense),
                    amount = widgetMoney(content.currencySymbol, content.expense, false, overflow, locale),
                    color = GlanceTheme.colors.onSurface,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Figure(
                    label = context.getString(R.string.bill_figure_balance),
                    amount = widgetMoney(
                        content.currencySymbol,
                        content.balance,
                        content.balanceOverflow,
                        overflow,
                        locale,
                    ),
                    color = GlanceTheme.colors.onSurface,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
            Spacer(GlanceModifier.defaultWeight().height(12.dp))
            Button(
                text = context.getString(R.string.bill_home_title),
                onClick = actionStartActivity(record),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Figure(
    label: String,
    amount: String,
    color: ColorProvider,
    modifier: GlanceModifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = amount,
            maxLines = 1,
            style = TextStyle(
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private fun widgetLocale(context: Context): Locale {
    val locales = context.resources.configuration.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}
