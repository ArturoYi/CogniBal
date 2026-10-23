package com.example.cognibal.data.repository

import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.model.BudgetPeriodType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.min

/**
 * 由一个锚点日期算出预算区间。结果的结束时间不包含在区间内。
 *
 * 周从周一开始，到下周一 0 点结束。季度按 1、4、7、10 月对齐。
 * 月默认从 1 日到下月 1 日。[monthContaining] 可以改成每月固定一天。
 * 调用方也可以自己传入开始和结束，不必一定走这里。
 * 边界是 [com.example.cognibal.data.database.LedgerTime] 的本地时间，和账单发生时间同一套。
 */
object BudgetPeriods {
    const val CALENDAR_MONTH_DAY: Int = 1

    fun around(type: BudgetPeriodType, anchor: LocalDate): ClosedOpenRange = when (type) {
        BudgetPeriodType.Month -> monthContaining(CALENDAR_MONTH_DAY, anchor)
        BudgetPeriodType.Week -> {
            val start = anchor.with(DayOfWeek.MONDAY)
            range(start, start.plusWeeks(1))
        }
        BudgetPeriodType.Quarter -> {
            val firstMonth = ((anchor.monthValue - 1) / 3) * 3 + 1
            val start = LocalDate.of(anchor.year, firstMonth, 1)
            range(start, start.plusMonths(3))
        }
        BudgetPeriodType.Year -> {
            val start = LocalDate.of(anchor.year, 1, 1)
            range(start, start.plusYears(1))
        }
    }

    /**
     * 包含 [date] 的月周期。从当月或上月的 [cycleDay] 日 0 点，到下一次同一日 0 点。
     * [cycleDay] 为 1 就是自然月。该月没有这一天时用当月最后一天，下次仍按 [cycleDay]。
     */
    fun monthContaining(cycleDay: Int, date: LocalDate): ClosedOpenRange {
        require(cycleDay in 1..31)
        val startDate = startOnOrBefore(date, cycleDay)
        val endDate = onDay(YearMonth.from(startDate).plusMonths(1), cycleDay)
        return range(startDate, endDate)
    }

    private fun range(start: LocalDate, end: LocalDate): ClosedOpenRange = ClosedOpenRange(
        startInclusive = LedgerTime.normalize(start.atStartOfDay()),
        endExclusive = LedgerTime.normalize(end.atStartOfDay()),
    )

    private fun startOnOrBefore(date: LocalDate, cycleDay: Int): LocalDate {
        val thisStart = onDay(YearMonth.from(date), cycleDay)
        if (!date.isBefore(thisStart)) return thisStart
        return onDay(YearMonth.from(date).minusMonths(1), cycleDay)
    }

    private fun onDay(month: YearMonth, cycleDay: Int): LocalDate =
        month.atDay(min(cycleDay, month.lengthOfMonth()))
}

data class ClosedOpenRange(
    val startInclusive: LocalDateTime,
    val endExclusive: LocalDateTime,
)
