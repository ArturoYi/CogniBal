package com.example.cognibal.screen.feature.expense.state

import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.feature.expense.model.AddBillIssue
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import java.time.LocalDate
import java.time.LocalDateTime

internal data class CategoryChoice(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorKey: String,
)

internal data class AccountChoice(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorKey: String,
)

internal data class AddExpenseUiState(
    val ready: Boolean = false,
    val type: BillType = BillType.Expense,
    val amountText: String = "",
    val categories: List<CategoryChoice> = emptyList(),
    val categoryId: Long? = null,
    val accounts: List<AccountChoice> = emptyList(),
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val billTime: LocalDateTime,
    val remark: String = "",
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val issue: AddBillIssue? = null,
    val saving: Boolean = false,
    val overBudgetPrompt: OverBudgetPrompt? = null,
)

internal sealed interface AddExpenseUiEvent {
    data class TypeSelected(val type: BillType) : AddExpenseUiEvent
    data class AmountChanged(val value: String) : AddExpenseUiEvent
    data class CreateCategory(
        val name: String,
        val iconKey: String,
        val colorHex: String,
    ) : AddExpenseUiEvent
    data class CategorySelected(val categoryId: Long) : AddExpenseUiEvent
    data class AccountSelected(val accountId: Long) : AddExpenseUiEvent
    data class ToAccountSelected(val accountId: Long) : AddExpenseUiEvent
    data class DatePicked(val date: LocalDate) : AddExpenseUiEvent
    data class TimePicked(val hour: Int, val minute: Int, val second: Int) : AddExpenseUiEvent
    data class RemarkChanged(val value: String) : AddExpenseUiEvent
    data object Save : AddExpenseUiEvent
    data object ConfirmOverBudget : AddExpenseUiEvent
    data object DismissOverBudget : AddExpenseUiEvent
}

internal sealed interface CategoryCreateResult {
    data class Saved(val id: Long) : CategoryCreateResult
    data class Failed(val issue: AddBillIssue) : CategoryCreateResult
}

/** 记下这笔支出会让哪些预算达到 100%。 */
internal data class OverBudgetPrompt(
    val includesGlobal: Boolean,
    val categoryNames: List<String>,
)
