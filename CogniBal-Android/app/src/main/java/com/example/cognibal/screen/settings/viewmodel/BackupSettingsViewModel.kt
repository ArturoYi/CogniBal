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
 * 备份设置页的界面状态。条目先只展示，点击不执行备份。
 */
internal class BackupSettingsViewModel : ViewModel() {
    val uiState: StateFlow<BackupSettingsUiState> = MutableStateFlow(BackupSettingsUiState())

    fun onEvent(event: BackupSettingsUiEvent) {
        when (event) {
            is BackupSettingsUiEvent.RowClicked -> Unit
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BackupSettingsViewModel() }
        }
    }
}

internal enum class BackupSettingsAction {
    Backup,
    Export,
    Import,
    NearbyShare,
}

internal data class BackupSettingsRow(
    val action: BackupSettingsAction,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

internal data class BackupSettingsUiState(
    val rows: List<BackupSettingsRow> = listOf(
        BackupSettingsRow(
            action = BackupSettingsAction.Backup,
            titleRes = R.string.backup_save,
            subtitleRes = R.string.backup_save_subtitle,
        ),
        BackupSettingsRow(
            action = BackupSettingsAction.Export,
            titleRes = R.string.backup_export,
            subtitleRes = R.string.backup_export_subtitle,
        ),
        BackupSettingsRow(
            action = BackupSettingsAction.Import,
            titleRes = R.string.backup_import,
            subtitleRes = R.string.backup_import_subtitle,
        ),
        BackupSettingsRow(
            action = BackupSettingsAction.NearbyShare,
            titleRes = R.string.backup_nearby,
            subtitleRes = R.string.backup_nearby_subtitle,
        ),
    ),
)

internal sealed interface BackupSettingsUiEvent {
    data class RowClicked(val action: BackupSettingsAction) : BackupSettingsUiEvent
}
