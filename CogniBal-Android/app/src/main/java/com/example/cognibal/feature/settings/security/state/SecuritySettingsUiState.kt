package com.example.cognibal.screen.feature.settings.security.state

import androidx.annotation.StringRes
import com.example.cognibal.R

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
