package com.example.cognibal.screen.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.screen.feature.legal.LEGAL_ACCEPTANCE_VERSION
import com.example.cognibal.screen.feature.legal.LegalGate
import com.example.cognibal.screen.feature.settings.model.AppPreferences
import com.example.cognibal.screen.feature.settings.state.SettingsUiEvent
import com.example.cognibal.screen.feature.settings.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设置列表和应用主题的状态。
 *
 * 挂在 Activity 上：入口用 [preferences] 决定主题，设置页用 [uiState] 画分组列表。
 * 两边拿到的是同一个 ViewModel。
 */
internal class SettingsViewModel(
    private val repository: AppPreferencesRepository,
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

    /**
     * 协议确认。首帧是 [LegalGate.Pending]，避免磁盘还没读完就闪出确认页。
     * 同意的版本和正文日期不一致时，重新确认。
     */
    val legalGate: StateFlow<LegalGate> = repository.preferences
        .map { preferences ->
            if (preferences.acceptedLegalVersion == LEGAL_ACCEPTANCE_VERSION) {
                LegalGate.Accepted
            } else {
                LegalGate.Required
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LegalGate.Pending,
        )

    private val transient = MutableStateFlow(SettingsTransientState())

    val uiState: StateFlow<SettingsUiState> = transient
        .map { extra ->
            SettingsUiState(
                daysInUse = extra.daysInUse,
                totalRecordsCount = extra.totalRecordsCount,
                showAboutDialog = extra.showAboutDialog,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OpenAboutDialog ->
                transient.update { it.copy(showAboutDialog = true) }
            SettingsUiEvent.DismissAboutDialog ->
                transient.update { it.copy(showAboutDialog = false) }
            // 发信由界面拉起邮件客户端，这里只接住事件。
            SettingsUiEvent.SendFeedback -> Unit
        }
    }

    fun acceptLegalTerms() {
        viewModelScope.launch {
            repository.setAcceptedLegalVersion(LEGAL_ACCEPTANCE_VERSION)
        }
    }

    private data class SettingsTransientState(
        val daysInUse: Int = 18,
        val totalRecordsCount: Int = 42,
        val showAboutDialog: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("SettingsViewModel 需要 Application")
                SettingsViewModel(
                    repository = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}
