package com.example.cognibal.screen.feature.settings.backup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsAction
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiEvent
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.screen.feature.settings.SettingsAction
import com.example.cognibal.screen.feature.settings.SettingsActionGroup
import com.example.cognibal.screen.feature.settings.SettingsSubpage

/** 备份设置入口。条目来自 ViewModel，点击先不执行备份。 */
@Composable
internal fun BackupSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupSettingsViewModel = viewModel(factory = BackupSettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackupSettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun BackupSettingsScreen(
    uiState: BackupSettingsUiState,
    onEvent: (BackupSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = stringResource(R.string.settings_backup),
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
                onEvent(BackupSettingsUiEvent.RowClicked(uiState.rows[index].action))
            },
        )
    }
}

private val BackupSettingsAction.icon: ImageVector
    get() = when (this) {
        BackupSettingsAction.Backup -> Icons.Rounded.Backup
        BackupSettingsAction.Export -> Icons.Rounded.CloudUpload
        BackupSettingsAction.Import -> Icons.Rounded.CloudDownload
        BackupSettingsAction.NearbyShare -> Icons.Rounded.Share
    }

@FormFactorPreviews
@Composable
private fun BackupSettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            BackupSettingsScreen(
                uiState = BackupSettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
