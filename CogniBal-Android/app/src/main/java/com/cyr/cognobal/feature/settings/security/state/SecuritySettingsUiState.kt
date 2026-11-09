package com.cyr.cognobal.screen.feature.settings.security.state

import androidx.annotation.StringRes
import com.cyr.cognobal.R

internal enum class SecuritySettingsAction {
    Terms,
    Privacy,
}

internal data class SecuritySettingsRow(
    val action: SecuritySettingsAction,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

internal data class SecuritySettingsUiState(
    /** 退到后台时隐藏账本。没读到磁盘前先按默认打开。 */
    val hideContentInBackground: Boolean = true,
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
    ),
)

internal sealed interface SecuritySettingsUiEvent {
    data class RowClicked(val action: SecuritySettingsAction) : SecuritySettingsUiEvent
    data class HideContentInBackgroundChanged(val enabled: Boolean) : SecuritySettingsUiEvent
}
