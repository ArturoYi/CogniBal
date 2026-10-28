package com.example.cognibal.screen.feature.settings.statistics

import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.query.CategoryAmountSummary
import com.example.cognibal.screen.feature.settings.statistics.state.CategoryShareUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsBillUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsKind
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsSectionUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsSliceRole
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsSliceUi

/** 图上最多这么多块。再多就不好认，绘制也更贵。 */
internal const val CHART_SLICE_LIMIT = 7

/** 需要合并时，留下金额最高的这几块，其余合成一块。 */
private const val CHART_NAMED_SLICES = 6

internal fun statisticsSections(
    kind: StatisticsKind,
    expenseRows: List<CategoryAmountSummary>,
    incomeRows: List<CategoryAmountSummary>,
    categories: List<Category>,
    expenseTotal: Long,
    incomeTotal: Long,
): List<StatisticsSectionUi> {
    val expense = categoryShares(expenseRows, categories, expenseTotal)
    val income = categoryShares(incomeRows, categories, incomeTotal)
    return when (kind) {
        StatisticsKind.Expense -> listOfNotNull(section(StatisticsKind.Expense, expense))
        StatisticsKind.Income -> listOfNotNull(section(StatisticsKind.Income, income))
        StatisticsKind.All -> listOfNotNull(
            section(StatisticsKind.Expense, expense),
            section(StatisticsKind.Income, income),
        )
    }
}

internal fun statisticsSlices(
    kind: StatisticsKind,
    sections: List<StatisticsSectionUi>,
    expenseTotal: Long,
    incomeTotal: Long,
): List<StatisticsSliceUi> = when (kind) {
    StatisticsKind.All -> comparisonSlices(expenseTotal, incomeTotal)
    StatisticsKind.Expense,
    StatisticsKind.Income,
    -> chartSlices(sections.firstOrNull()?.categories.orEmpty())
}

/** 全部档只比两边的合计。0 不占扇区。 */
internal fun comparisonSlices(expense: Long, income: Long): List<StatisticsSliceUi> = buildList {
    if (expense > 0L) {
        add(StatisticsSliceUi(key = "expense", amount = expense, colorKey = "", role = StatisticsSliceRole.Expense))
    }
    if (income > 0L) {
        add(StatisticsSliceUi(key = "income", amount = income, colorKey = "", role = StatisticsSliceRole.Income))
    }
}

/**
 * 分类不超过 [CHART_SLICE_LIMIT] 时一块一个分类。
 * 超过时保留金额最高的 [CHART_NAMED_SLICES] 个，其余金额合成 [StatisticsSliceRole.Other]。
 */
internal fun chartSlices(shares: List<CategoryShareUi>): List<StatisticsSliceUi> {
    if (shares.size <= CHART_SLICE_LIMIT) {
        return shares.map { share -> share.toSlice() }
    }
    val named = shares.take(CHART_NAMED_SLICES).map { share -> share.toSlice() }
    val rest = shares.drop(CHART_NAMED_SLICES).sumOf { share -> share.amount }
    if (rest <= 0L) return named
    return named + StatisticsSliceUi(
        key = "other",
        amount = rest,
        colorKey = "",
        role = StatisticsSliceRole.Other,
    )
}

internal fun categoryShares(
    rows: List<CategoryAmountSummary>,
    categories: List<Category>,
    total: Long,
): List<CategoryShareUi> {
    val byId = categories.associateBy { category -> category.id }
    return rows
        .filter { row -> row.amount > 0L }
        .sortedByDescending { row -> row.amount }
        .map { row ->
            val category = byId[row.categoryId]
            CategoryShareUi(
                id = row.categoryId,
                name = category?.name,
                missing = category == null,
                iconKey = category?.icon ?: "",
                colorKey = category?.color ?: "#FF868E96",
                amount = row.amount,
                ratio = shareRatio(row.amount, total),
            )
        }
}

private fun section(kind: StatisticsKind, categories: List<CategoryShareUi>): StatisticsSectionUi? {
    if (categories.isEmpty()) return null
    return StatisticsSectionUi(kind = kind, categories = categories)
}

private fun CategoryShareUi.toSlice(): StatisticsSliceUi = StatisticsSliceUi(
    key = "c:$id",
    amount = amount,
    colorKey = colorKey,
    role = StatisticsSliceRole.Category,
)

private fun shareRatio(amount: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return (amount.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

/** 这个月里能点开的支出和收入。转账没有分类，不进统计。新的在前。 */
internal fun statisticsBills(
    rows: List<Bill>,
    accounts: List<Account>,
    kind: StatisticsKind,
): List<StatisticsBillUi> {
    val names = accounts.associate { account -> account.id to account.name }
    return rows.mapNotNull { bill ->
        val billKind = when (bill.type) {
            BillType.Expense -> StatisticsKind.Expense
            BillType.Income -> StatisticsKind.Income
            BillType.Transfer -> return@mapNotNull null
        }
        if (kind != StatisticsKind.All && billKind != kind) return@mapNotNull null
        val categoryId = bill.categoryId ?: return@mapNotNull null
        StatisticsBillUi(
            id = bill.id,
            categoryId = categoryId,
            kind = billKind,
            amount = bill.amount,
            time = bill.billTime,
            remark = bill.remark?.trim()?.takeIf { it.isNotEmpty() },
            accountName = names[bill.accountId]?.trim()?.takeIf { it.isNotEmpty() },
        )
    }.sortedWith(
        compareByDescending<StatisticsBillUi> { bill -> bill.time }
            .thenByDescending { bill -> bill.id },
    )
}
