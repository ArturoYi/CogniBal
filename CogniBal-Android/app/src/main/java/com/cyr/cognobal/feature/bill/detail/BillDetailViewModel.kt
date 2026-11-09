package com.cyr.cognobal.screen.feature.bill.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyr.cognobal.data.database.entity.Account
import com.cyr.cognobal.data.database.entity.Bill
import com.cyr.cognobal.data.database.entity.Category
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.screen.feature.settings.model.AppPreferences
import com.cyr.cognobal.data.repository.AccountRepository
import com.cyr.cognobal.data.repository.BillRepository
import com.cyr.cognobal.data.repository.CategoryRepository
import com.cyr.cognobal.data.repository.LedgerException
import com.cyr.cognobal.di.LedgerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class BillDetailUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val type: BillType = BillType.Expense,
    val categoryName: String? = null,
    val categoryIconKey: String? = null,
    val categoryColorKey: String? = null,
    val accountName: String = "",
    val counterpartyName: String? = null,
    val time: LocalDateTime? = null,
    val remark: String = "",
    val signedAmount: Long = 0L,
    val currencySymbol: String = "",
    val brokenTransfer: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val deleting: Boolean = false,
    val failed: Boolean = false,
)

internal sealed interface BillDetailUiEvent {
    data object RequestDelete : BillDetailUiEvent
    data object ConfirmDelete : BillDetailUiEvent
    data object DismissDelete : BillDetailUiEvent
}

/** 一笔账的详情。转账把另一条腿一起显示，删除时两条一起去掉。 */
internal class BillDetailViewModel(
    private val billId: Long,
    private val bills: BillRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    preferences: AppPreferencesRepository,
) : ViewModel() {
    private val prompt = MutableStateFlow(DetailPrompt())
    private val finishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finished: SharedFlow<Unit> = finishedEvents.asSharedFlow()

    val uiState: StateFlow<BillDetailUiState> = combine(
        bills.observeById(billId).flatMapLatest { bill -> recordOf(bill) },
        accounts.observeAll(),
        categories.observeAll(),
        preferences.preferences,
        prompt,
    ) { record, accountRows, categoryRows, prefs, promptState ->
        toState(record, accountRows, categoryRows, prefs, promptState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = BillDetailUiState(),
    )

    fun onEvent(event: BillDetailUiEvent) {
        when (event) {
            BillDetailUiEvent.RequestDelete -> prompt.update {
                it.copy(showDelete = true, failed = false)
            }
            BillDetailUiEvent.DismissDelete -> prompt.update { it.copy(showDelete = false) }
            BillDetailUiEvent.ConfirmDelete -> delete()
        }
    }

    private fun delete() {
        if (prompt.value.deleting) return
        prompt.update { it.copy(showDelete = false, deleting = true, failed = false) }
        viewModelScope.launch {
            try {
                bills.delete(billId)
                finishedEvents.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: LedgerException) {
                prompt.update { it.copy(deleting = false, failed = true) }
            }
        }
    }

    private fun recordOf(bill: Bill?): Flow<BillRecord?> = flow {
        if (bill == null) {
            emit(null)
            return@flow
        }
        if (bill.type != BillType.Transfer) {
            emit(BillRecord(bill = bill, peer = null, broken = false))
            return@flow
        }
        val peer = try {
            bills.transferPeer(bill.id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: LedgerException) {
            null
        }
        emit(BillRecord(bill = bill, peer = peer, broken = peer == null))
    }

    private fun toState(
        record: BillRecord?,
        accountRows: List<Account>,
        categoryRows: List<Category>,
        prefs: AppPreferences,
        promptState: DetailPrompt,
    ): BillDetailUiState {
        if (record == null) {
            return BillDetailUiState(
                loading = false,
                missing = true,
                currencySymbol = prefs.currency.symbol,
                failed = promptState.failed,
            )
        }
        val bill = record.bill
        val accountsById = accountRows.associateBy { it.id }
        val category = bill.categoryId?.let { id -> categoryRows.firstOrNull { it.id == id } }
        val outgoing = when {
            bill.type != BillType.Transfer -> null
            bill.amount < 0 -> bill
            else -> record.peer
        }
        val incoming = when {
            bill.type != BillType.Transfer -> null
            bill.amount > 0 -> bill
            else -> record.peer
        }
        return BillDetailUiState(
            loading = false,
            type = bill.type,
            categoryName = category?.name,
            categoryIconKey = category?.icon,
            categoryColorKey = category?.color,
            accountName = accountsById[bill.accountId]?.name.orEmpty(),
            counterpartyName = when (bill.type) {
                BillType.Transfer -> accountsById[incoming?.accountId]?.name
                else -> null
            },
            time = bill.billTime,
            remark = bill.remark.orEmpty(),
            signedAmount = when (bill.type) {
                BillType.Expense -> -bill.amount
                BillType.Income -> bill.amount
                BillType.Transfer -> bill.amount
            },
            currencySymbol = prefs.currency.symbol,
            brokenTransfer = record.broken,
            showDeleteConfirm = promptState.showDelete,
            deleting = promptState.deleting,
            failed = promptState.failed,
        ).let { state ->
            if (bill.type != BillType.Transfer) {
                state
            } else {
                state.copy(
                    accountName = accountsById[outgoing?.accountId]?.name.orEmpty(),
                    counterpartyName = accountsById[incoming?.accountId]?.name,
                )
            }
        }
    }

    private data class BillRecord(
        val bill: Bill,
        val peer: Bill?,
        val broken: Boolean,
    )

    private data class DetailPrompt(
        val showDelete: Boolean = false,
        val deleting: Boolean = false,
        val failed: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(billId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("BillDetailViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                BillDetailViewModel(
                    billId = billId,
                    bills = entry.billRepository(),
                    accounts = entry.accountRepository(),
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}
