package com.example.cognibal.screen.bill.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountDraft
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.LedgerException
import com.example.cognibal.data.repository.LedgerLimits
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.data.repository.NewAccount
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.bill.model.AccountColor
import com.example.cognibal.screen.bill.model.AccountIcon
import com.example.cognibal.screen.bill.tool.parseMajorAmount
import com.example.cognibal.screen.settings.model.CurrencyPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                            val icon = AccountIcon.fromKey(account.icon)
                            val color = AccountColor.fromKey(account.color)
                            loaded.copy(
                                ready = true,
                                isEdit = true,
                                missing = false,
                                name = account.name,
                                type = account.type,
                                icon = icon ?: AccountIcon.Wallet,
                                iconKeyOverride = if (icon == null) account.icon else null,
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
            is AccountEditorUiEvent.TypeSelected -> edit { it.copy(type = event.type) }
            is AccountEditorUiEvent.IconSelected -> edit {
                it.copy(icon = event.icon, iconKeyOverride = null)
            }
            is AccountEditorUiEvent.ColorSelected -> edit {
                it.copy(color = event.color, colorKeyOverride = null)
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
                    icon = snapshot.iconKeyOverride ?: snapshot.icon.storageKey,
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

internal data class AccountEditorUiState(
    val ready: Boolean = false,
    val missing: Boolean = false,
    val isEdit: Boolean = false,
    val name: String = "",
    val type: AccountType = AccountType.Cash,
    val icon: AccountIcon = AccountIcon.Wallet,
    /** 备份里不是枚举键的图标。用户重新选过图标后清空，保存时改用枚举键。 */
    val iconKeyOverride: String? = null,
    val color: AccountColor = AccountColor.Teal,
    val colorKeyOverride: String? = null,
    val openingBalanceText: String = "",
    val remark: String = "",
    val currentBalance: Long? = null,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val billCount: Int = 0,
    val enabledCount: Int = 0,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val error: LedgerReject? = null,
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
)

internal sealed interface AccountEditorUiEvent {
    data class NameChanged(val value: String) : AccountEditorUiEvent
    data class RemarkChanged(val value: String) : AccountEditorUiEvent
    data class OpeningBalanceChanged(val value: String) : AccountEditorUiEvent
    data class TypeSelected(val type: AccountType) : AccountEditorUiEvent
    data class IconSelected(val icon: AccountIcon) : AccountEditorUiEvent
    data class ColorSelected(val color: AccountColor) : AccountEditorUiEvent
    data object Save : AccountEditorUiEvent
    data class SetEnabled(val enabled: Boolean) : AccountEditorUiEvent
    data object DeleteClicked : AccountEditorUiEvent
    data object DismissDelete : AccountEditorUiEvent
    data object ConfirmDelete : AccountEditorUiEvent
}

/** 编辑页上为什么不能出现删除按钮。仓库仍会再拦一次。 */
internal enum class AccountDeleteBlock {
    Bills,
    Default,
    LastEnabled,
}

internal fun AccountEditorUiState.deleteBlock(): AccountDeleteBlock? {
    if (!isEdit) return null
    if (billCount > 0) return AccountDeleteBlock.Bills
    if (isDefault) return AccountDeleteBlock.Default
    if (enabled && enabledCount <= 1) return AccountDeleteBlock.LastEnabled
    return null
}
