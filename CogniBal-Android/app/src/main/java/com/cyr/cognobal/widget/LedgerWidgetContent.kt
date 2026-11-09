package com.cyr.cognobal.widget

import com.cyr.cognobal.data.database.query.PeriodAmountSummary
import com.cyr.cognobal.data.repository.balanceOf
import com.cyr.cognobal.screen.feature.bill.tool.formatMoneyOrOverflow
import com.cyr.cognobal.screen.feature.settings.model.AppPreferences
import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference
import com.cyr.cognobal.ui.format.formatCalendarMonth
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

/**
 * 桌面卡片上的数字。时间在绘制时另取，不放进这份快照。
 */
internal data class LedgerWidgetContent(
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
    val balanceOverflow: Boolean = false,
)

internal fun AppPreferences.toWidgetContent(summary: PeriodAmountSummary): LedgerWidgetContent {
    val (balance, overflow) = balanceOf(summary.incomeAmount, summary.expenseAmount)
    return LedgerWidgetContent(
        currencySymbol = currency.symbol,
        income = summary.incomeAmount,
        expense = summary.expenseAmount,
        balance = balance,
        balanceOverflow = overflow,
    )
}

internal fun widgetMoney(
    symbol: String,
    amount: Long,
    overflow: Boolean,
    overflowLabel: String,
    locale: Locale,
): String {
    return formatMoneyOrOverflow(symbol, amount, overflow, overflowLabel, locale)
}

/** 卡片顶上的月份。下面的数字是本月合计，不显示日和时刻。 */
internal fun widgetNowLabel(locale: Locale, now: LocalDateTime = LocalDateTime.now()): String =
    formatCalendarMonth(YearMonth.from(now), locale)
