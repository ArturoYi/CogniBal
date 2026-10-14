package com.example.cognibal.screen.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.screen.settings.viewmodel.SecuritySettingsAction
import com.example.cognibal.screen.settings.viewmodel.SecuritySettingsUiEvent
import com.example.cognibal.screen.settings.viewmodel.SecuritySettingsUiState
import com.example.cognibal.screen.settings.viewmodel.SecuritySettingsViewModel
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/** 安全与隐私入口。条目来自 ViewModel，点击先不打开协议或验证。 */
@Composable
internal fun SecuritySettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecuritySettingsViewModel = viewModel(factory = SecuritySettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SecuritySettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun SecuritySettingsScreen(
    uiState: SecuritySettingsUiState,
    onEvent: (SecuritySettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = "安全与隐私",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsActionGroup(
            actions = uiState.rows.map { row ->
                SettingsAction(
                    icon = row.action.icon,
                    title = row.title,
                    subtitle = row.subtitle,
                )
            },
            onAction = { index ->
                onEvent(SecuritySettingsUiEvent.RowClicked(uiState.rows[index].action))
            },
        )
    }
}

private val SecuritySettingsAction.icon: ImageVector
    get() = when (this) {
        SecuritySettingsAction.Terms -> Icons.AutoMirrored.Rounded.Article
        SecuritySettingsAction.Privacy -> Icons.Rounded.PrivacyTip
        SecuritySettingsAction.AppLock -> Icons.Rounded.Lock
    }

@FormFactorPreviews
@Composable
private fun SecuritySettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.appColors.background) {
            SecuritySettingsScreen(
                uiState = SecuritySettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
