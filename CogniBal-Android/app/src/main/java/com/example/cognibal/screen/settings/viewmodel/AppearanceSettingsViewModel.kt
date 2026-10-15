package com.example.cognibal.screen.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.screen.settings.model.DarkModePreference
import com.example.cognibal.ui.theme.AppThemeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 外观设置页的状态。语言交给系统的应用内语言，不另存一份 DataStore。
 * 主题和主题色沿用已有的偏好写入。
 */
internal class AppearanceSettingsViewModel(
    private val repository: AppPreferencesRepository,
) : ViewModel() {
    private val showLanguageDialog = MutableStateFlow(false)

    val uiState: StateFlow<AppearanceSettingsUiState> = combine(
        repository.preferences,
        showLanguageDialog,
    ) { preferences, showLanguage ->
        AppearanceSettingsUiState(
            showLanguageDialog = showLanguage,
            darkMode = preferences.darkMode,
            themeStyle = preferences.themeStyle,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AppearanceSettingsUiState(),
    )

    fun onEvent(event: AppearanceSettingsUiEvent) {
        when (event) {
            AppearanceSettingsUiEvent.LanguageClicked -> showLanguageDialog.value = true
            AppearanceSettingsUiEvent.DismissLanguageDialog -> showLanguageDialog.value = false
            is AppearanceSettingsUiEvent.LanguageSelected -> {
                showLanguageDialog.value = false
                val locales = if (event.languageTag == null) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(event.languageTag)
                }
                AppCompatDelegate.setApplicationLocales(locales)
            }
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
    val showLanguageDialog: Boolean = false,
    val darkMode: DarkModePreference = DarkModePreference.FOLLOW_SYSTEM,
    val themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
)

/** 界面向上递交的操作。语言写入应用内区域，主题写入仓库。 */
internal sealed interface AppearanceSettingsUiEvent {
    data object LanguageClicked : AppearanceSettingsUiEvent
    data object DismissLanguageDialog : AppearanceSettingsUiEvent

    /** `null` 表示跟随系统。 */
    data class LanguageSelected(val languageTag: String?) : AppearanceSettingsUiEvent
    data class DarkModeChanged(val mode: DarkModePreference) : AppearanceSettingsUiEvent
    data class ThemeStyleChanged(val style: AppThemeStyle) : AppearanceSettingsUiEvent
}
