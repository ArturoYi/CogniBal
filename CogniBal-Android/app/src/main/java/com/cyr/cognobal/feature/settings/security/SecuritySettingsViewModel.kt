package com.cyr.cognobal.screen.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.screen.feature.settings.security.state.SecuritySettingsUiEvent
import com.cyr.cognobal.screen.feature.settings.security.state.SecuritySettingsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 安全与隐私页的界面状态。
 *
 * 协议全文由路由打开。退到后台是否盖住账本写在偏好里。
 */
internal class SecuritySettingsViewModel(
    private val repository: AppPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<SecuritySettingsUiState> = repository.preferences
        .map { preferences ->
            SecuritySettingsUiState(hideContentInBackground = preferences.hideContentInBackground)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SecuritySettingsUiState(),
        )

    fun onEvent(event: SecuritySettingsUiEvent) {
        when (event) {
            is SecuritySettingsUiEvent.RowClicked -> Unit
            is SecuritySettingsUiEvent.HideContentInBackgroundChanged -> {
                viewModelScope.launch { repository.setHideContentInBackground(event.enabled) }
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("SecuritySettingsViewModel 需要 Application")
                SecuritySettingsViewModel(
                    repository = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}
