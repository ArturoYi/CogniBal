package com.example.cognibal.screen.feature.settings.overview.state

import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import java.time.LocalDate
import java.time.YearMonth

internal data class OverviewDayAmount(
    val day: LocalDate,
    val expense: Long,
    val income: Long,
)

/** 当天账单按支出或收入筛选。全部时两类都留下。 */
internal enum class OverviewKind {
    All,
    Expense,
    Income,
}

/** 筛选用的分类。支出和收入分开，转账没有分类。 */
internal data class OverviewCategoryOption(
    val id: Long,
    val name: String,
    val expense: Boolean,
)

/** 这种筛选下，这一个分类要不要出现在列表里。 */
internal fun OverviewCategoryOption.matches(kind: OverviewKind): Boolean = when (kind) {
    OverviewKind.All -> true
    OverviewKind.Expense -> expense
    OverviewKind.Income -> !expense
}

internal data class OverviewUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate = LocalDate.now(),
    val canGoNext: Boolean = false,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
    val balanceOverflow: Boolean = false,
    val days: List<OverviewDayAmount> = emptyList(),
    val bills: List<BillRowUi> = emptyList(),
    /** 选中那天在筛选前是否有账单。用来区分「这天没账」和「筛完是空的」。 */
    val dayHasBills: Boolean = false,
    val kind: OverviewKind = OverviewKind.All,
    val categories: List<OverviewCategoryOption> = emptyList(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val filterActive: Boolean = false,
)

internal sealed interface OverviewUiEvent {
    data object PreviousMonth : OverviewUiEvent
    data object NextMonth : OverviewUiEvent
    data class SelectMonth(val month: YearMonth) : OverviewUiEvent
    data class SelectDay(val day: LocalDate) : OverviewUiEvent

    /** 把草稿写成当天的筛选。调用方等状态跟上再收起。 */
    data class ApplyFilter(
        val kind: OverviewKind,
        val categoryIds: Set<Long>,
    ) : OverviewUiEvent

    /** 筛选取消，回到全部、不限分类。 */
    data object ClearFilter : OverviewUiEvent
}
