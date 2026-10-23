package com.example.cognibal.screen.feature.bill.state

import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.data.repository.BudgetLevel
import java.time.LocalDateTime

internal data class AccountBarState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val accounts: List<AccountItemUi> = emptyList(),
    val currentId: Long? = null,
)

internal data class AccountItemUi(
    val id: Long,
    val name: String,
    val type: AccountType,
    val iconKey: String,
    val colorKey: String,
    val balance: Long,
    val enabled: Boolean,
    val selected: Boolean,
)

internal data class BillRowUi(
    val id: Long,
    val categoryName: String?,
    val type: BillType,
    val time: LocalDateTime,
    val signedAmount: Long,
)

internal data class BudgetProgressUi(
    val id: Long,
    /** 分类名。为空表示总预算，界面自己写文案。 */
    val categoryName: String?,
    val missingCategory: Boolean,
    val spent: Long,
    val limit: Long,
    val ratio: Float,
    val level: BudgetLevel,
)

internal data class BillHomeUiState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val accounts: List<AccountItemUi> = emptyList(),
    /** 顶栏正在展示的账户，对应 is_default。还没标时先用第一个启用账户。 */
    val currentId: Long? = null,
    val totalAssets: Long = 0L,
    val totalOverflow: Boolean = false,
    val monthIncome: Long = 0L,
    val monthExpense: Long = 0L,
    val monthBalance: Long = 0L,
    val monthOverflow: Boolean = false,
    val budgets: List<BudgetProgressUi> = emptyList(),
    val recent: List<BillRowUi> = emptyList(),
)

internal sealed interface BillHomeUiEvent {
    data class Select(val accountId: Long) : BillHomeUiEvent
}

internal fun billsToRows(bills: List<Bill>, categories: Map<Long, Category>): List<BillRowUi> {
    return bills.map { bill ->
        BillRowUi(
            id = bill.id,
            categoryName = bill.categoryId?.let { categories[it]?.name },
            type = bill.type,
            time = bill.billTime,
            signedAmount = when (bill.type) {
                BillType.Expense -> -bill.amount
                BillType.Income -> bill.amount
                BillType.Transfer -> bill.amount
            },
        )
    }
}
