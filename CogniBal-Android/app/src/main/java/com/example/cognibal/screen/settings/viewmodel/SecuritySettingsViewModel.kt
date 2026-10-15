package com.example.cognibal.screen.settings.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.R
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
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

internal data class SecuritySettingsUiState(
    val rows: List<SecuritySettingsRow> = listOf(
        SecuritySettingsRow(
            action = SecuritySettingsAction.Terms,
            titleRes = R.string.security_terms,
            subtitleRes = R.string.security_terms_subtitle,
        ),
        SecuritySettingsRow(
            action = SecuritySettingsAction.Privacy,
            titleRes = R.string.security_privacy,
            subtitleRes = R.string.security_privacy_subtitle,
        ),
        SecuritySettingsRow(
            action = SecuritySettingsAction.AppLock,
            titleRes = R.string.security_app_lock,
            subtitleRes = R.string.security_app_lock_subtitle,
        ),
    ),
)

internal sealed interface SecuritySettingsUiEvent {
    data class RowClicked(val action: SecuritySettingsAction) : SecuritySettingsUiEvent
}
