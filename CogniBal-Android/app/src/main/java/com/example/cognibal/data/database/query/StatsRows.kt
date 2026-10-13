package com.example.cognibal.data.database.query

/**
 * 日历上一天的收支。没有账单的日期不会出现在结果里，界面自己补 0。
 *
 * 不是表。[day] 形如 `2026-10-13`，来自账单时间的前 10 个字符。
 * 转账不进这两个金额。
 */
data class DayAmountSummary(
    val day: String,
    val expenseAmount: Long,
    val incomeAmount: Long,
)

/**
 * 某个月的收支合计，给趋势图用。[month] 形如 `2026-10`。
 */
data class MonthTrendSummary(
    val month: String,
    val expenseAmount: Long,
    val incomeAmount: Long,
)

/**
 * 一个分类在区间内的合计，给饼图用。只包含带分类的支出或收入。
 */
data class CategoryAmountSummary(
    val categoryId: Long,
    val amount: Long,
)

/**
 * 一个区间的收支合计。查询始终返回一行，没有账单时两个数都是 0。
 */
data class PeriodAmountSummary(
    val expenseAmount: Long,
    val incomeAmount: Long,
)

/**
 * 一个区间的转账合计。转出、转入都是正数，已经把库存的符号去掉。
 */
data class TransferAmountSummary(
    val outAmount: Long,
    val inAmount: Long,
)

/**
 * 某条预算在自己的周期里已经花掉的支出。全局预算统计全部支出，分类预算只统计该分类。
 */
data class BudgetSpendRow(
    val budgetId: Long,
    val spentAmount: Long,
)
