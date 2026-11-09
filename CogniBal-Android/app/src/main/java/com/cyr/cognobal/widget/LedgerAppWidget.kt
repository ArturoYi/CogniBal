package com.cyr.cognobal.widget

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
import androidx.glance.LocalSize
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.cyr.cognobal.MainActivity
import com.cyr.cognobal.R
import com.cyr.cognobal.shortcut.AppShortcuts
import java.util.Locale

/**
 * 桌面卡片：当前年月、本月收入支出结余，底下是记账。
 *
 * 尺寸用 Exact，排版按 [LocalSize] 的实际宽高来。数字保持完整；
 * 位置不够时先收字号和间距，再按记账、月份、收入和支出、结余标签的顺序拿掉。
 */
class LedgerAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = LedgerWidgetLoader.load(context)
        provideContent {
            LedgerGlanceTheme {
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
    val copy = WidgetCopy(
        month = widgetNowLabel(locale),
        incomeLabel = context.getString(R.string.bill_figure_income),
        expenseLabel = context.getString(R.string.bill_figure_expense),
        balanceLabel = context.getString(R.string.bill_figure_balance),
        income = widgetMoney(content.currencySymbol, content.income, overflow = false, overflow, locale),
        expense = widgetMoney(content.currencySymbol, content.expense, overflow = false, overflow, locale),
        balance = widgetMoney(
            content.currencySymbol,
            content.balance,
            content.balanceOverflow,
            overflow,
            locale,
        ),
        action = context.getString(R.string.bill_fab_cd),
        balanceOverflow = content.balanceOverflow,
    )
    val size = LocalSize.current
    val layout = fitWidget(size.width.value, size.height.value, copy, widgetTextMeasure(context))
    val record = Intent(context, MainActivity::class.java).apply {
        action = AppShortcuts.ACTION_ADD
        putExtra(AppShortcuts.EXTRA_KIND, AppShortcuts.KIND_EXPENSE)
    }
    val loneAmount = !layout.showMonth && !layout.showLabels && layout.slots.size == 1 && !layout.showAction
    Scaffold(
        backgroundColor = GlanceTheme.colors.background,
        horizontalPadding = 0.dp,
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = layout.paddingH.dp, vertical = layout.paddingV.dp),
            verticalAlignment = if (loneAmount) Alignment.CenterVertically else Alignment.Top,
            horizontalAlignment = if (loneAmount) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            if (layout.showMonth) {
                Text(
                    text = copy.month,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = layout.monthSp.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            if (layout.slots.isNotEmpty()) {
                if (layout.showMonth) Spacer(GlanceModifier.height(layout.gap.dp))
                Figures(copy, layout)
            }
            if (layout.showAction) {
                Spacer(GlanceModifier.defaultWeight())
                Button(
                    text = copy.action,
                    onClick = actionStartActivity(record),
                    modifier = GlanceModifier.fillMaxWidth().height(layout.actionHeight.dp),
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = layout.actionSp.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Figures(copy: WidgetCopy, layout: WidgetLayout) {
    when (layout.arrange) {
        WidgetArrange.Columns -> FigureColumns(copy, layout)
        WidgetArrange.Lines -> FigureLines(copy, layout)
        WidgetArrange.Blocks -> FigureBlocks(copy, layout)
    }
}

@Composable
private fun FigureColumns(copy: WidgetCopy, layout: WidgetLayout) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        layout.slots.forEachIndexed { index, slot ->
            if (index > 0) {
                Spacer(GlanceModifier.width(layout.gap.dp))
                Spacer(GlanceModifier.defaultWeight())
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (layout.showLabels) {
                    LabelText(copy.label(slot), layout)
                    Spacer(GlanceModifier.height(layout.labelGap.dp))
                }
                AmountText(copy.amount(slot), lines = 1, layout = layout, align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun FigureLines(copy: WidgetCopy, layout: WidgetLayout) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        layout.slots.forEachIndexed { index, slot ->
            if (index > 0) Spacer(GlanceModifier.height(layout.gap.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (layout.showLabels) {
                    LabelText(copy.label(slot), layout)
                    Spacer(GlanceModifier.width(layout.labelGap.dp))
                    Spacer(GlanceModifier.defaultWeight())
                }
                AmountText(copy.amount(slot), lines = 1, layout = layout, align = TextAlign.End)
            }
        }
    }
}

@Composable
private fun FigureBlocks(copy: WidgetCopy, layout: WidgetLayout) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        layout.slots.forEachIndexed { index, slot ->
            if (index > 0) Spacer(GlanceModifier.height(layout.gap.dp))
            if (layout.showLabels) {
                LabelText(copy.label(slot), layout)
                Spacer(GlanceModifier.height(layout.labelGap.dp))
            }
            val lines = if (slot == WidgetSlot.Balance) layout.balanceLines else 1
            val align = if (!layout.showLabels && layout.slots.size == 1) {
                TextAlign.Center
            } else {
                TextAlign.Start
            }
            AmountText(
                copy.amount(slot),
                lines = lines,
                layout = layout,
                align = align,
                fill = true,
            )
        }
    }
}

@Composable
private fun LabelText(text: String, layout: WidgetLayout) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = layout.labelSp.sp,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun AmountText(
    text: String,
    lines: Int,
    layout: WidgetLayout,
    align: TextAlign,
    fill: Boolean = false,
) {
    Text(
        text = text,
        maxLines = lines,
        modifier = if (fill) GlanceModifier.fillMaxWidth() else GlanceModifier,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = layout.amountSp.sp,
            fontWeight = FontWeight.Bold,
            textAlign = align,
        ),
    )
}

private fun widgetLocale(context: Context): Locale {
    val locales = context.resources.configuration.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}
