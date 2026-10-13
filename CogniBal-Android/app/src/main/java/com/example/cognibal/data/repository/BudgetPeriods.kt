package com.example.cognibal.data.repository

import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.model.BudgetPeriodType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 由一个锚点日期算出预算区间。结果的结束时间不包含在区间内。
 *
 * 周从周一开始，到下周一 0 点结束。季度按 1、4、7、10 月对齐。
 * 调用方也可以自己传入开始和结束，不必一定走这里。
 */
object BudgetPeriods {
    fun around(type: BudgetPeriodType, anchor: LocalDate): ClosedOpenRange {
        val startDate = when (type) {
            BudgetPeriodType.Week -> anchor.with(DayOfWeek.MONDAY)
            BudgetPeriodType.Month -> anchor.withDayOfMonth(1)
            BudgetPeriodType.Quarter -> {
                val firstMonth = ((anchor.monthValue - 1) / 3) * 3 + 1
                LocalDate.of(anchor.year, firstMonth, 1)
            }
            BudgetPeriodType.Year -> LocalDate.of(anchor.year, 1, 1)
        }
        val endDate = when (type) {
            BudgetPeriodType.Week -> startDate.plusWeeks(1)
            BudgetPeriodType.Month -> startDate.plusMonths(1)
            BudgetPeriodType.Quarter -> startDate.plusMonths(3)
            BudgetPeriodType.Year -> startDate.plusYears(1)
        }
        return ClosedOpenRange(
            startInclusive = LedgerTime.normalize(startDate.atStartOfDay()),
            endExclusive = LedgerTime.normalize(endDate.atStartOfDay()),
        )
    }
}

data class ClosedOpenRange(
    val startInclusive: LocalDateTime,
    val endExclusive: LocalDateTime,
)
