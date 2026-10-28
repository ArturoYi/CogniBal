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
 * 安全与隐私页的界面状态。
 *
 * 协议全文由路由打开。应用保护先只接住点击，不验证身份。
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
