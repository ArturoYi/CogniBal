package com.example.cognibal.screen.feature.bill.account

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountDraft
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.LedgerException
import com.example.cognibal.data.repository.LedgerLimits
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.data.repository.NewAccount
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.bill.model.AccountColor
import com.example.cognibal.screen.feature.bill.model.pairedIcon
import com.example.cognibal.screen.feature.bill.tool.parseMajorAmount
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.cognibal.screen.feature.bill.account.state.AccountEditorUiEvent
import com.example.cognibal.screen.feature.bill.account.state.AccountEditorUiState
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.random.Random

/**
 * 新建或编辑一个账户。
 *
 * 编辑不改余额。余额是账单事务里的缓存，在这里改会和历史账对不上。
 * 新建成功后把当前展示切到这个账户。
 * 有账单只能停用。当前展示的账户和最后一个启用账户不能物理删除。
 */
internal class AccountEditorViewModel(
    private val accountId: Long?,
    private val repository: AccountRepository,
    preferences: AppPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AccountEditorUiState(
            ready = accountId == null,
            isEdit = accountId != null,
            colorKeyOverride = if (accountId == null) vividColorHex() else null,
        ),
    )
    val uiState: StateFlow<AccountEditorUiState> = _uiState

    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    init {
        viewModelScope.launch {
            preferences.preferences.collect { prefs ->
                _uiState.update { it.copy(currencySymbol = prefs.currency.symbol) }
            }
        }
        val id = accountId
        if (id != null) {
            viewModelScope.launch {
                combine(
                    repository.observeById(id),
                    repository.observeBillCount(id),
                    repository.observeEnabledCount(),
                ) { account, billCount, enabledCount ->
                    Triple(account, billCount, enabledCount)
                }.collect { (account, billCount, enabledCount) ->
                    if (account == null) {
                        _uiState.update { it.copy(ready = true, missing = true, isEdit = true) }
                        return@collect
                    }
                    _uiState.update { state ->
                        val loaded = state.copy(
                            enabled = account.isEnable,
                            isDefault = account.isDefault,
                            billCount = billCount,
                            enabledCount = enabledCount,
                            currentBalance = account.balance,
                        )
                        if (state.ready && !state.missing) {
                            loaded
                        } else {
                            val color = AccountColor.fromKey(account.color)
                            loaded.copy(
                                ready = true,
                                isEdit = true,
                                missing = false,
                                name = account.name,
                                type = account.type,
                                icon = account.type.pairedIcon(),
                                iconKeyOverride = null,
                                color = color ?: AccountColor.Teal,
                                colorKeyOverride = if (color == null) account.color else null,
                                remark = account.remark.orEmpty(),
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEvent(event: AccountEditorUiEvent) {
        when (event) {
            is AccountEditorUiEvent.NameChanged -> edit {
                it.copy(name = event.value.take(LedgerLimits.NAME))
            }
            is AccountEditorUiEvent.RemarkChanged -> edit {
                it.copy(remark = event.value.take(LedgerLimits.REMARK))
            }
            is AccountEditorUiEvent.OpeningBalanceChanged -> edit {
                it.copy(openingBalanceText = event.value.filter { char ->
                    char.isDigit() || char == '-' || char == '+' || char == '.' || char == ',' ||
                        char == '，' || char == '。' || char == ' '
                }.take(AMOUNT_INPUT_LIMIT))
            }
            is AccountEditorUiEvent.TypeSelected -> edit {
                it.copy(type = event.type, icon = event.type.pairedIcon(), iconKeyOverride = null)
            }
            is AccountEditorUiEvent.ColorSelected -> edit {
                it.copy(color = event.color, colorKeyOverride = null)
            }
            is AccountEditorUiEvent.CustomColorSelected -> edit {
                it.copy(colorKeyOverride = event.hex)
            }
            AccountEditorUiEvent.Save -> save()
            is AccountEditorUiEvent.SetEnabled -> setEnabled(event.enabled)
            AccountEditorUiEvent.DeleteClicked ->
                _uiState.update { it.copy(confirmDelete = true) }
            AccountEditorUiEvent.DismissDelete ->
                _uiState.update { it.copy(confirmDelete = false) }
            AccountEditorUiEvent.ConfirmDelete -> delete()
        }
    }

    private fun edit(block: (AccountEditorUiState) -> AccountEditorUiState) {
        _uiState.update { block(it).copy(error = null) }
    }

    private fun save() {
        val snapshot = _uiState.value
        if (!snapshot.ready || snapshot.missing || snapshot.saving) return
        val openingBalance = if (snapshot.isEdit) {
            0L
        } else {
            parseMajorAmount(snapshot.openingBalanceText) ?: run {
                _uiState.update { it.copy(error = LedgerReject.InvalidAmount) }
                return
            }
        }
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val draft = AccountDraft(
                    name = snapshot.name,
                    icon = snapshot.type.pairedIcon().storageKey,
                    color = snapshot.colorKeyOverride ?: snapshot.color.storageKey,
                    type = snapshot.type,
                    remark = snapshot.remark,
                )
                if (snapshot.isEdit) {
                    val id = accountId ?: return@launch
                    repository.update(id, draft)
                } else {
                    val id = repository.insert(NewAccount(profile = draft, openingBalance = openingBalance))
                    repository.setDisplayed(id)
                }
                _finished.tryEmit(Unit)
            } catch (exception: LedgerException) {
                _uiState.update { it.copy(saving = false, error = exception.reason) }
            }
        }
    }

    private fun setEnabled(enabled: Boolean) {
        val id = accountId ?: return
        val snapshot = _uiState.value
        if (!snapshot.ready || snapshot.missing || snapshot.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.setEnabled(id, enabled)
                _uiState.update { it.copy(saving = false) }
            } catch (exception: LedgerException) {
                _uiState.update { it.copy(saving = false, error = exception.reason) }
            }
        }
    }

    private fun delete() {
        val id = accountId ?: return
        _uiState.update { it.copy(saving = true, confirmDelete = false, error = null) }
        viewModelScope.launch {
            try {
                repository.delete(id)
                _finished.tryEmit(Unit)
            } catch (exception: LedgerException) {
                _uiState.update { it.copy(saving = false, error = exception.reason) }
            }
        }
    }

    companion object {
        private const val AMOUNT_INPUT_LIMIT = 16

        /** 新建时随机一支饱和色。避开灰、白和过暗，卡片上还能认出来。 */
        private fun vividColorHex(): String {
            val random = Random.Default
            val hsv = floatArrayOf(
                random.nextFloat() * 360f,
                0.62f + random.nextFloat() * 0.28f,
                0.78f + random.nextFloat() * 0.14f,
            )
            return String.format(Locale.US, "#%08X", Color.HSVToColor(hsv))
        }

        fun factory(accountId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("AccountEditorViewModel 需要 Application")
                val repository = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                ).accountRepository()
                AccountEditorViewModel(
                    accountId = accountId,
                    repository = repository,
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}
