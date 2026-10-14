package com.example.cognibal.screen.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

internal enum class SecuritySettingsAction {
    Terms,
    Privacy,
    AppLock,
}

internal data class SecuritySettingsRow(
    val action: SecuritySettingsAction,
    val title: String,
    val subtitle: String,
)

internal data class SecuritySettingsUiState(
    val rows: List<SecuritySettingsRow> = listOf(
        SecuritySettingsRow(
            action = SecuritySettingsAction.Terms,
            title = "用户协议",
            subtitle = "查看服务条款",
        ),
        SecuritySettingsRow(
            action = SecuritySettingsAction.Privacy,
            title = "隐私协议",
            subtitle = "查看数据如何被使用",
        ),
        SecuritySettingsRow(
            action = SecuritySettingsAction.AppLock,
            title = "应用保护",
            subtitle = "打开应用前先验证身份",
        ),
    ),
)

internal sealed interface SecuritySettingsUiEvent {
    data class RowClicked(val action: SecuritySettingsAction) : SecuritySettingsUiEvent
}
