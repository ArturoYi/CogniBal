package com.cyr.cognobal.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.cyr.cognobal.MainActivity
import com.cyr.cognobal.R
import com.cyr.cognobal.shortcut.AppShortcuts
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 桌面卡片：当前年月和本月结余。够大时内容居中，底下是支出、收入两个按钮，
 * 点按后直接打开对应的记账页。点数字区域打开记账首页。
 *
 * 尺寸用一组固定档位，不用 Exact。系统给每个档位各做一份界面，
 * 一份 RemoteViews 里最多 16 档；桌面如果把可拉伸范围内的每一种格子都传进来，
 * Exact 会超过这个上限，更新失败，卡片就停在加载布局。
 * 系统会选完全放得进当前格子、面积最大的那一档。
 * 结余用大号数字。位置不够时先拿掉按钮，再收字号、月份和两侧。
 *
 * 不使用 Glance 自己的状态存储。数据在绘制前先取一次，取不到就先用 0，
 * 避免数据库还没回来时卡片一直停在加载布局。
 */
class LedgerAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(LedgerWidgetSizes)

    override val stateDefinition: GlanceStateDefinition<*>? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadInitial(context)
        provideContent {
            val content by remember {
                LedgerWidgetLoader.observeContent(context)
            }.collectAsState(initial)
            LedgerGlanceTheme {
                LedgerWidgetBody(content)
            }
        }
    }
}

/**
 * 可拉伸范围里的代表尺寸。必须少于等于 16，并且宽高都要大于 0。
 * 系统选档时要求宽高都小于格子，并留 1dp，所以档位比常见格子略小。
 */
internal val LedgerWidgetSizes: Set<DpSize> = setOf(
    DpSize(110.dp, 80.dp),
    DpSize(160.dp, 100.dp),
    DpSize(180.dp, 110.dp),
    DpSize(250.dp, 110.dp),
    DpSize(300.dp, 136.dp),
    DpSize(320.dp, 140.dp),
    DpSize(360.dp, 140.dp),
    DpSize(180.dp, 180.dp),
    DpSize(250.dp, 180.dp),
    DpSize(320.dp, 180.dp),
    DpSize(360.dp, 180.dp),
    DpSize(250.dp, 280.dp),
    DpSize(360.dp, 280.dp),
    DpSize(520.dp, 180.dp),
    DpSize(520.dp, 320.dp),
)

private suspend fun loadInitial(context: Context): LedgerWidgetContent {
    return try {
        withTimeoutOrNull(2.seconds) {
            withContext(Dispatchers.IO) { LedgerWidgetLoader.load(context) }
        } ?: LedgerWidgetContent()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        LedgerWidgetContent()
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
    val align = if (layout.centered) TextAlign.Center else TextAlign.Start
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = layout.paddingH.dp, vertical = layout.paddingV.dp),
            horizontalAlignment = if (layout.centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .clickable(actionStartActivity(openBill(context))),
                verticalAlignment = if (layout.centered) Alignment.CenterVertically else Alignment.Top,
                horizontalAlignment = if (layout.centered) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Header(copy, layout, align)
                if (layout.showBalanceLabel && !layout.balanceLabelInHeader) {
                    if (layout.showMonth) Spacer(GlanceModifier.height(layout.labelGap.dp))
                    Caption(copy.balanceLabel, layout.labelSp, align)
                    Spacer(GlanceModifier.height(layout.labelGap.dp))
                } else if (layout.showMonth || layout.balanceLabelInHeader) {
                    Spacer(GlanceModifier.height(if (layout.centered) layout.labelGap.dp else layout.gap.dp))
                }
                Text(
                    text = copy.balance,
                    maxLines = layout.balanceLines,
                    modifier = if (layout.centered) GlanceModifier else GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = layout.balanceSp.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = align,
                    ),
                )
                if (WidgetSlot.Income in layout.slots) {
                    Spacer(GlanceModifier.height(layout.gap.dp))
                    if (!layout.showAction) Spacer(GlanceModifier.defaultWeight())
                    Sides(copy, layout, align)
                }
            }
            if (layout.showAction) {
                Spacer(GlanceModifier.height(layout.gap.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RecordButton(copy.expenseLabel, AppShortcuts.KIND_EXPENSE, layout)
                    Spacer(GlanceModifier.width(layout.gap.dp))
                    RecordButton(copy.incomeLabel, AppShortcuts.KIND_INCOME, layout)
                }
            }
        }
    }
}

private fun openBill(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    action = AppShortcuts.ACTION_OPEN_BILL
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
}

private fun recordBill(context: Context, kind: String): Intent = Intent(context, MainActivity::class.java).apply {
    action = AppShortcuts.ACTION_ADD
    putExtra(AppShortcuts.EXTRA_KIND, kind)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
}

@Composable
private fun RowScope.RecordButton(text: String, kind: String, layout: WidgetLayout) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .height(WIDGET_ACTION_HEIGHT.dp)
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .clickable(actionStartActivity(recordBill(context, kind))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontSize = layout.actionSp.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun Header(copy: WidgetCopy, layout: WidgetLayout, align: TextAlign) {
    val showHeader = layout.showMonth || layout.balanceLabelInHeader
    if (!showHeader) return
    if (layout.centered && layout.balanceLabelInHeader) {
        val text = if (layout.showMonth) {
            "${copy.month} · ${copy.balanceLabel}"
        } else {
            copy.balanceLabel
        }
        Caption(text, layout.monthSp, align)
        return
    }
    if (layout.centered) {
        if (layout.showMonth) Caption(copy.month, layout.monthSp, align)
        return
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (layout.showMonth) {
            Caption(copy.month, layout.monthSp, TextAlign.Start)
        }
        if (layout.balanceLabelInHeader) {
            Spacer(GlanceModifier.defaultWeight())
            Caption(copy.balanceLabel, layout.labelSp, TextAlign.End)
        }
    }
}

@Composable
private fun Sides(copy: WidgetCopy, layout: WidgetLayout, align: TextAlign) {
    if (layout.arrange == WidgetArrange.Stack) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = if (layout.centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Side(copy.label(WidgetSlot.Income), copy.income, layout, align)
            Spacer(GlanceModifier.height(layout.gap.dp))
            Side(copy.label(WidgetSlot.Expense), copy.expense, layout, align)
        }
    } else {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = if (layout.centered) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Side(copy.label(WidgetSlot.Income), copy.income, layout, align)
            }
            Spacer(GlanceModifier.width(layout.gap.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = if (layout.centered) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                Side(copy.label(WidgetSlot.Expense), copy.expense, layout, align)
            }
        }
    }
}

@Composable
private fun Side(label: String, amount: String, layout: WidgetLayout, align: TextAlign) {
    if (layout.showLabels) {
        Caption(label, layout.labelSp, align)
        Spacer(GlanceModifier.height(layout.labelGap.dp))
    }
    Text(
        text = amount,
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = layout.amountSp.sp,
            fontWeight = FontWeight.Medium,
            textAlign = align,
        ),
    )
}

@Composable
private fun Caption(text: String, sizeSp: Float, align: TextAlign) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = sizeSp.sp,
            textAlign = align,
        ),
    )
}

private fun widgetLocale(context: Context): Locale {
    val locales = context.resources.configuration.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}
