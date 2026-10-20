package com.example.cognibal.screen.feature.bill.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.BillRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.di.LedgerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.example.cognibal.screen.feature.bill.history.state.BillHistoryUiState
import com.example.cognibal.screen.feature.bill.state.billsToRows
import kotlinx.coroutines.flow.stateIn

/** 一个账户的全部账单，按时间从新到旧。 */
internal class BillHistoryViewModel(
    bills: BillRepository,
    categories: CategoryRepository,
    preferences: AppPreferencesRepository,
    accountId: Long,
) : ViewModel() {
    val uiState: StateFlow<BillHistoryUiState> = combine(
        bills.observeByAccount(accountId),
        categories.observeAll(),
        preferences.preferences,
    ) { rows, categoryRows, prefs ->
        BillHistoryUiState(
            loading = false,
            currencySymbol = prefs.currency.symbol,
            bills = billsToRows(rows, categoryRows.associateBy { it.id }),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = BillHistoryUiState(),
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(accountId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("BillHistoryViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                BillHistoryViewModel(
                    bills = entry.billRepository(),
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                    accountId = accountId,
                )
            }
        }
    }
}
