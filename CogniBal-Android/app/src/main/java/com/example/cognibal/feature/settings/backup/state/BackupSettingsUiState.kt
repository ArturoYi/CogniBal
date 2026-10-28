package com.example.cognibal.screen.feature.settings.backup.state

import android.net.Uri
import androidx.annotation.StringRes
import com.example.cognibal.R

internal enum class BackupSettingsAction {
    Backup,
    Export,
    Import,
    NearbyShare,
}

internal enum class MigrationRole {
    Send,
    Receive,
}

internal data class BackupSettingsRow(
    val action: BackupSettingsAction,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

internal data class BackupWorkUi(
    @StringRes val labelRes: Int,
    val progress: Float?,
    val cancellable: Boolean,
)

internal data class BluetoothPeerUi(
    val address: String,
    val name: String,
)

internal sealed interface MigrationUi {
    data object Role : MigrationUi

    data class Receive(
        val waiting: Boolean,
        val paused: Boolean,
        val secondsLeft: Int = 0,
        val timedOut: Boolean = false,
    ) : MigrationUi

    data class Scan(
        val devices: List<BluetoothPeerUi>,
        val scanning: Boolean,
        val paused: Boolean,
        val secondsLeft: Int = 0,
    ) : MigrationUi

    data object Connecting : MigrationUi

    data class Code(
        val digits: String,
        val secondsLeft: Int,
        val waitingForPeer: Boolean,
    ) : MigrationUi

    data object WaitReplace : MigrationUi

    data object Replace : MigrationUi

    data class Transfer(
        @StringRes val labelRes: Int,
        val progress: Float?,
    ) : MigrationUi

    data class Failed(
        @StringRes val messageRes: Int,
        val diagnostic: String,
        val role: MigrationRole,
    ) : MigrationUi
}

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
    val savedAtMillis: Long? = null,
    val work: BackupWorkUi? = null,
    val confirmImport: Boolean = false,
    val migration: MigrationUi? = null,
)

internal sealed interface BackupSettingsUiEvent {
    data class RowClicked(val action: BackupSettingsAction) : BackupSettingsUiEvent

    data object DismissImportConfirm : BackupSettingsUiEvent

    data object ConfirmImport : BackupSettingsUiEvent

    data object ChooseSend : BackupSettingsUiEvent

    data object ChooseReceive : BackupSettingsUiEvent

    data object StartReceive : BackupSettingsUiEvent

    data object Rescan : BackupSettingsUiEvent

    data class SelectPeer(val address: String) : BackupSettingsUiEvent

    data object CodeMatches : BackupSettingsUiEvent

    data object CodeDiffers : BackupSettingsUiEvent

    data object ConfirmReplace : BackupSettingsUiEvent

    data object DeclineReplace : BackupSettingsUiEvent

    data object RetryMigration : BackupSettingsUiEvent

    data object CopyDiagnostic : BackupSettingsUiEvent

    data object MigrationBack : BackupSettingsUiEvent

    data object ForegroundLost : BackupSettingsUiEvent

    data object PageLeft : BackupSettingsUiEvent

    data object CancelWork : BackupSettingsUiEvent

    data object BluetoothPermissionGranted : BackupSettingsUiEvent

    data object BluetoothPermissionDenied : BackupSettingsUiEvent

    data object BluetoothEnabled : BackupSettingsUiEvent

    data object BluetoothEnableDeclined : BackupSettingsUiEvent

    data object SystemPromptFinished : BackupSettingsUiEvent
}

internal sealed interface BackupSettingsEffect {
    data class Toast(@StringRes val messageRes: Int, val long: Boolean = false) : BackupSettingsEffect

    data class Share(val uri: Uri) : BackupSettingsEffect

    data class RequestBluetoothPermission(val permissions: List<String>) : BackupSettingsEffect

    data object RequestEnableBluetooth : BackupSettingsEffect

    data class CopyDiagnostic(val text: String) : BackupSettingsEffect

    data object OpenAppSettings : BackupSettingsEffect

    data object RestartApp : BackupSettingsEffect
}
