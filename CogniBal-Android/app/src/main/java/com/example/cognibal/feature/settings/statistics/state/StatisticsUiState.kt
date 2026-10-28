package com.example.cognibal.screen.feature.settings.statistics.state

import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import java.time.LocalDateTime
import java.time.YearMonth

/** 顶部三档。全部看支出和收入的对比，另外两档看该档的分类构成。 */
internal enum class StatisticsKind {
    All,
    Expense,
    Income,
}

/** 扇区颜色从哪来。分类用 [StatisticsSliceUi.colorKey]，其余用主题色。 */
internal enum class StatisticsSliceRole {
    Category,
    Expense,
    Income,
    Other,
}

internal data class CategoryShareUi(
    val id: Long,
    val name: String?,
    val missing: Boolean,
    val iconKey: String,
    val colorKey: String,
    val amount: Long,
    val ratio: Float,
)

/**
 * 图上的一块。金额在进图之前已经准备好，绘制时不再格式化。
 *
 * [key] 用来区分扇区。全部分类仍在 [StatisticsSectionUi]，超过上限时这里才会出现 [StatisticsSliceRole.Other]。
 */
internal data class StatisticsSliceUi(
    val key: String,
    val amount: Long,
    val colorKey: String,
    val role: StatisticsSliceRole,
)

/** 下方列表的一组。全部档有支出、收入两组，单档只有一组。 */
internal data class StatisticsSectionUi(
    val kind: StatisticsKind,
    val categories: List<CategoryShareUi>,
)

/** 统计里能点开的一笔支出或收入。转账不进这里。 */
internal data class StatisticsBillUi(
    val id: Long,
    val categoryId: Long,
    val kind: StatisticsKind,
    val amount: Long,
    val time: LocalDateTime,
    val remark: String?,
    val accountName: String?,
)

internal data class StatisticsUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val canGoNext: Boolean = false,
    val kind: StatisticsKind = StatisticsKind.Expense,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    /** 支出或收入档是该档合计。全部档是结余，可能为负。 */
    val total: Long = 0L,
    val totalOverflow: Boolean = false,
    val slices: List<StatisticsSliceUi> = emptyList(),
    val sections: List<StatisticsSectionUi> = emptyList(),
    /** 当前档、当前月的账单。点开某个分类后按分类取出。 */
    val bills: List<StatisticsBillUi> = emptyList(),
)

internal sealed interface StatisticsUiEvent {
    data object PreviousMonth : StatisticsUiEvent
    data object NextMonth : StatisticsUiEvent
    data class SelectKind(val kind: StatisticsKind) : StatisticsUiEvent
}
