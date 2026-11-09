package com.cyr.cognobal.data.repository

/**
 * 收入减支出。溢出时第二个值是 true，界面改显示占位，不再拿一个被截断的数去算结余。
 */
internal fun balanceOf(income: Long, expense: Long): Pair<Long, Boolean> = try {
    Math.subtractExact(income, expense) to false
} catch (_: ArithmeticException) {
    0L to true
}
