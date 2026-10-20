package com.example.cognibal.screen.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiEvent
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiState

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
