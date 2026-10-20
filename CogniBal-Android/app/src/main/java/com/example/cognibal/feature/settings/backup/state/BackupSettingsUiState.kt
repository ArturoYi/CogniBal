package com.example.cognibal.screen.feature.settings.backup.state

import androidx.annotation.StringRes
import com.example.cognibal.R

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
