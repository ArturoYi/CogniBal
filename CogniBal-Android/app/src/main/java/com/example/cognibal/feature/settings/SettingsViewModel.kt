package com.example.cognibal.screen.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.toAssetSnapshot
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.model.AppPreferences
import com.example.cognibal.screen.feature.settings.state.DEFAULT_CACHE_SIZE
import com.example.cognibal.screen.feature.settings.state.SettingsUiEvent
import com.example.cognibal.screen.feature.settings.state.SettingsUiState
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设置与应用主题的唯一状态持有者。
 *
 * 挂在 Activity 上：入口用 [preferences] 决定主题，设置页用 [uiState] 画分组列表。
 * 两边拿到的是同一个 ViewModel，写入只走仓库，不直接碰 DataStore。
 * 总盈余是全部账户余额之和，和资产页净资产同一套加法。
 */
internal class SettingsViewModel(
    private val repository: AppPreferencesRepository,
    accounts: AccountRepository,
) : ViewModel() {
    /**
     * 应用级偏好。Activity 进入组合就开始收集，启动动画期间 DataStore 有时间读完。
     * 首帧用默认值，读到磁盘后再替换。
     */
    val preferences: StateFlow<AppPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppPreferences(),
    )

    private val transient = MutableStateFlow(SettingsTransientState())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences,
        accounts.observeAll(),
        transient,
    ) { prefs, rows, extra ->
        val snapshot = rows.toAssetSnapshot()
        SettingsUiState(
            preferences = prefs,
            cacheSizeText = extra.cacheSizeText,
            daysInUse = extra.daysInUse,
            totalRecordsCount = extra.totalRecordsCount,
            totalSurplus = snapshot.net,
            surplusOverflow = snapshot.netOverflow,
            isSurplusVisible = extra.isSurplusVisible,
            showMoreDialog = extra.showMoreDialog,
            showCurrencyDialog = extra.showCurrencyDialog,
            showClearCacheDialog = extra.showClearCacheDialog,
            showAboutDialog = extra.showAboutDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.HapticFeedbackChanged -> persist { repository.setHapticFeedback(event.enabled) }
            is SettingsUiEvent.CurrencySelected -> {
                transient.update { it.copy(showCurrencyDialog = false) }
                persist { repository.setCurrency(event.currency) }
            }
            SettingsUiEvent.ToggleSurplusVisibility ->
                transient.update { it.copy(isSurplusVisible = !it.isSurplusVisible) }
            SettingsUiEvent.OpenMoreDialog ->
                transient.update { it.copy(showMoreDialog = true) }
            SettingsUiEvent.DismissMoreDialog ->
                transient.update { it.copy(showMoreDialog = false) }
            SettingsUiEvent.OpenCurrencyDialog ->
                transient.update { it.copy(showCurrencyDialog = true) }
            SettingsUiEvent.DismissCurrencyDialog ->
                transient.update { it.copy(showCurrencyDialog = false) }
            SettingsUiEvent.OpenClearCacheDialog ->
                transient.update { it.copy(showClearCacheDialog = true) }
            SettingsUiEvent.DismissClearCacheDialog ->
                transient.update { it.copy(showClearCacheDialog = false) }
            SettingsUiEvent.ConfirmClearCache ->
                transient.update { it.copy(cacheSizeText = "0 KB", showClearCacheDialog = false) }
            SettingsUiEvent.OpenAboutDialog ->
                transient.update { it.copy(showAboutDialog = true) }
            SettingsUiEvent.DismissAboutDialog ->
                transient.update { it.copy(showAboutDialog = false) }
            // 发信由界面拉起邮件客户端，这里只接住事件。
            SettingsUiEvent.SendFeedback -> Unit
        }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private data class SettingsTransientState(
        val cacheSizeText: String = DEFAULT_CACHE_SIZE,
        val daysInUse: Int = 18,
        val totalRecordsCount: Int = 42,
        val isSurplusVisible: Boolean = false,
        val showMoreDialog: Boolean = false,
        val showCurrencyDialog: Boolean = false,
        val showClearCacheDialog: Boolean = false,
        val showAboutDialog: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("SettingsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                SettingsViewModel(
                    repository = AppPreferencesRepository.create(application),
                    accounts = entry.accountRepository(),
                )
            }
        }
    }
}
