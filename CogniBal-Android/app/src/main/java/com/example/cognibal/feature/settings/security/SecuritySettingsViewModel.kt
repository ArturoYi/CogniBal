package com.example.cognibal.screen.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.cognibal.screen.feature.settings.security.state.SecuritySettingsUiEvent
import com.example.cognibal.screen.feature.settings.security.state.SecuritySettingsUiState

/**
 * 安全与隐私页的界面状态。条目先只展示，点击不打开协议或验证。
 */
internal class SecuritySettingsViewModel : ViewModel() {
    val uiState: StateFlow<SecuritySettingsUiState> = MutableStateFlow(SecuritySettingsUiState())

    fun onEvent(event: SecuritySettingsUiEvent) {
        when (event) {
            is SecuritySettingsUiEvent.RowClicked -> Unit
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SecuritySettingsViewModel() }
        }
    }
}
