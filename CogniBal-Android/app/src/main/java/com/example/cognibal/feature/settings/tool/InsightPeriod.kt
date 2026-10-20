package com.example.cognibal.screen.feature.settings.tool

import java.time.LocalDateTime
import java.time.YearMonth

/** 这个月的第一天 0 点，含在区间里。 */
internal fun YearMonth.startInclusive(): LocalDateTime = atDay(1).atStartOfDay()

/** 下个月的第一天 0 点，不含在区间里。 */
internal fun YearMonth.endExclusive(): LocalDateTime = plusMonths(1).atDay(1).atStartOfDay()

/** 不能翻到还没发生的月份。 */
internal fun YearMonth.cappedToCurrent(): YearMonth {
    val current = YearMonth.now()
    return if (this > current) current else this
}

/**
 * 收入减支出。溢出时第二个值是 true，界面改显示占位，不再拿一个被截断的数去算结余。
 */
internal fun balanceOf(income: Long, expense: Long): Pair<Long, Boolean> {
    return try {
        Math.subtractExact(income, expense) to false
    } catch (_: ArithmeticException) {
        0L to true
    }
}
