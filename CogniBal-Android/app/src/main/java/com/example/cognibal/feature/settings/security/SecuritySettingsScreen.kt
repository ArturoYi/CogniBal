package com.example.cognibal.screen.feature.settings.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.legal.LegalDocumentKind
import com.example.cognibal.screen.feature.settings.security.state.SecuritySettingsAction
import com.example.cognibal.screen.feature.settings.security.state.SecuritySettingsUiEvent
import com.example.cognibal.screen.feature.settings.security.state.SecuritySettingsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.screen.feature.settings.SettingsAction
import com.example.cognibal.screen.feature.settings.SettingsActionGroup
import com.example.cognibal.screen.feature.settings.SettingsSubpage

/** 安全与隐私入口。用户协议和隐私协议打开全文。应用保护先不验证。 */
@Composable
internal fun SecuritySettingsRoute(
    onBack: () -> Unit,
    onOpenDocument: (LegalDocumentKind) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecuritySettingsViewModel = viewModel(factory = SecuritySettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openTerms = dropUnlessResumed { onOpenDocument(LegalDocumentKind.Terms) }
    val openPrivacy = dropUnlessResumed { onOpenDocument(LegalDocumentKind.Privacy) }
    SecuritySettingsScreen(
        uiState = uiState,
        onEvent = { event ->
            if (event is SecuritySettingsUiEvent.RowClicked) {
                when (event.action) {
                    SecuritySettingsAction.Terms -> openTerms()
                    SecuritySettingsAction.Privacy -> openPrivacy()
                    SecuritySettingsAction.AppLock -> viewModel.onEvent(event)
                }
            }
        },
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
        title = stringResource(R.string.settings_security),
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsActionGroup(
            actions = uiState.rows.map { row ->
                SettingsAction(
                    icon = row.action.icon,
                    title = stringResource(row.titleRes),
                    subtitle = stringResource(row.subtitleRes),
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
        SecuritySettingsAction.Terms -> Icons.AutoMirrored.Outlined.Article
        SecuritySettingsAction.Privacy -> Icons.Outlined.PrivacyTip
        SecuritySettingsAction.AppLock -> Icons.Outlined.Lock
    }

@FormFactorPreviews
@Composable
private fun SecuritySettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        PageBackgroundHost {
            SecuritySettingsScreen(
                uiState = SecuritySettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
