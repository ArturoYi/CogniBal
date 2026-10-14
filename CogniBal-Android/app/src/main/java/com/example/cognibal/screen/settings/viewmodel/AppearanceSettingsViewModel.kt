package com.example.cognibal.screen.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.screen.settings.model.DarkModePreference
import com.example.cognibal.ui.theme.AppThemeStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 外观设置页的状态。语言先只展示文案；主题和主题色沿用已有的偏好写入。
 */
internal class AppearanceSettingsViewModel(
    private val repository: AppPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<AppearanceSettingsUiState> = repository.preferences
        .map { preferences ->
            AppearanceSettingsUiState(
                languageLabel = "简体中文",
                darkMode = preferences.darkMode,
                themeStyle = preferences.themeStyle,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppearanceSettingsUiState(),
        )

    fun onEvent(event: AppearanceSettingsUiEvent) {
        when (event) {
            AppearanceSettingsUiEvent.LanguageClicked -> Unit
            is AppearanceSettingsUiEvent.DarkModeChanged ->
                persist { repository.setDarkMode(event.mode) }
            is AppearanceSettingsUiEvent.ThemeStyleChanged ->
                persist { repository.setThemeStyle(event.style) }
        }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("AppearanceSettingsViewModel 需要 Application")
                AppearanceSettingsViewModel(
                    repository = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}

internal data class AppearanceSettingsUiState(
    val languageLabel: String = "简体中文",
    val darkMode: DarkModePreference = DarkModePreference.FOLLOW_SYSTEM,
    val themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
)

/** 界面向上递交的操作。语言切换还没做，主题写入仓库。 */
internal sealed interface AppearanceSettingsUiEvent {
    data object LanguageClicked : AppearanceSettingsUiEvent
    data class DarkModeChanged(val mode: DarkModePreference) : AppearanceSettingsUiEvent
    data class ThemeStyleChanged(val style: AppThemeStyle) : AppearanceSettingsUiEvent
}
