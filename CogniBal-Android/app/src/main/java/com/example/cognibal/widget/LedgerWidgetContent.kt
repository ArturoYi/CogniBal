package com.example.cognibal.widget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.settings.model.AppPreferences
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.ui.theme.AppThemeStyle
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 桌面卡片上的数字。时间在绘制时另取，不放进这份快照。
 */
internal data class LedgerWidgetContent(
    val monochrome: Boolean = false,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
    val balanceOverflow: Boolean = false,
)

internal fun AppPreferences.toWidgetContent(summary: PeriodAmountSummary): LedgerWidgetContent {
    val (balance, overflow) = monthBalance(summary)
    return LedgerWidgetContent(
        monochrome = themeStyle == AppThemeStyle.Monochrome,
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
    if (overflow) return overflowLabel
    return symbol + formatMinorAmount(amount, locale)
}

/** 卡片顶上的月份。下面的数字是本月合计，不显示日和时刻。 */
internal fun widgetNowLabel(locale: Locale, now: LocalDateTime = LocalDateTime.now()): String {
    val pattern = if (locale.language == "zh") "yyyy年M月" else "MMM yyyy"
    return YearMonth.from(now).format(DateTimeFormatter.ofPattern(pattern, locale))
}

private fun monthBalance(summary: PeriodAmountSummary): Pair<Long, Boolean> {
    return try {
        Math.subtractExact(summary.incomeAmount, summary.expenseAmount) to false
    } catch (_: ArithmeticException) {
        0L to true
    }
}
