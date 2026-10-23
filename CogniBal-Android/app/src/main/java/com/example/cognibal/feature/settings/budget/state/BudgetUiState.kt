package com.example.cognibal.screen.feature.settings.budget.state

import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.data.repository.BudgetMath
import com.example.cognibal.data.repository.BudgetPeriods
import java.time.LocalDate
import java.time.LocalDateTime

internal data class BudgetCategoryOption(
    val id: Long,
    val name: String,
)

internal data class BudgetLineUi(
    val id: Long,
    val global: Boolean,
    val categoryName: String?,
    val missingCategory: Boolean,
    val spent: Long,
    val limit: Long,
    val progress: Float,
    val level: BudgetLevel,
    val alertPercent: Int,
    val enabled: Boolean,
    val editable: Boolean,
    val periodStart: LocalDate,
    val periodType: BudgetPeriodType,
    val cycleDay: Int? = null,
)

internal data class BudgetEditorState(
    val id: Long? = null,
    val global: Boolean = true,
    val categoryId: Long? = null,
    val amountText: String = "",
    val alertPercent: Int = BudgetMath.DEFAULT_ALERT_PERCENT,
    val enabled: Boolean = true,
    val allowGlobal: Boolean = true,
    val allowCategory: Boolean = true,
    val periodType: BudgetPeriodType = BudgetPeriodType.Month,
    val cycleDay: Int = BudgetPeriods.CALENDAR_MONTH_DAY,
    val periodStart: LocalDateTime? = null,
    val periodEnd: LocalDateTime? = null,
    val saving: Boolean = false,
)

internal enum class BudgetIssue {
    InvalidAmount,
    Overlap,
    NeedCategory,
    NoScope,
    Failed,
}

internal data class BudgetUiState(
    val loading: Boolean = true,
    val currencySymbol: String = "¥",
    val notify: Boolean = false,
    val current: List<BudgetLineUi> = emptyList(),
    val history: List<BudgetLineUi> = emptyList(),
    val categories: List<BudgetCategoryOption> = emptyList(),
    val editor: BudgetEditorState? = null,
    val issue: BudgetIssue? = null,
)

internal sealed interface BudgetUiEvent {
    data object Add : BudgetUiEvent
    data class Edit(val id: Long) : BudgetUiEvent
    data object DismissEditor : BudgetUiEvent
    data class ScopeGlobal(val global: Boolean) : BudgetUiEvent
    data class CategorySelected(val id: Long) : BudgetUiEvent
    data class AmountChanged(val value: String) : BudgetUiEvent
    data class AlertChanged(val percent: Int) : BudgetUiEvent
    data class CycleDayChanged(val day: Int) : BudgetUiEvent
    data class EnabledChanged(val enabled: Boolean) : BudgetUiEvent
    data object Save : BudgetUiEvent
    data object Delete : BudgetUiEvent
    data class NotifyChanged(val enabled: Boolean) : BudgetUiEvent
}
