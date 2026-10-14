package com.example.cognibal.screen.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
    val title: String,
    val subtitle: String,
)

internal data class BackupSettingsUiState(
    val rows: List<BackupSettingsRow> = listOf(
        BackupSettingsRow(
            action = BackupSettingsAction.Backup,
            title = "备份",
            subtitle = "把当前账单保存到这台设备",
        ),
        BackupSettingsRow(
            action = BackupSettingsAction.Export,
            title = "导出备份",
            subtitle = "生成一份可以带走的备份文件",
        ),
        BackupSettingsRow(
            action = BackupSettingsAction.Import,
            title = "导入备份",
            subtitle = "从备份文件恢复账单",
        ),
        BackupSettingsRow(
            action = BackupSettingsAction.NearbyShare,
            title = "附近分享",
            subtitle = "把备份发给附近的设备",
        ),
    ),
)

internal sealed interface BackupSettingsUiEvent {
    data class RowClicked(val action: BackupSettingsAction) : BackupSettingsUiEvent
}
