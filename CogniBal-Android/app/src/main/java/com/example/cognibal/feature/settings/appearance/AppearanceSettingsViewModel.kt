package com.example.cognibal.screen.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.cognibal.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.cognibal.screen.feature.settings.appearance.state.AppearanceSettingsUiEvent
import com.example.cognibal.screen.feature.settings.appearance.state.AppearanceSettingsUiState
import com.example.cognibal.screen.feature.settings.appearance.state.LanguageChoice

/**
 * 外观设置页的状态。语言交给系统的应用内语言，不另存一份 DataStore。
 * 主题和主题色沿用已有的偏好写入。
 */
internal class AppearanceSettingsViewModel(
    private val repository: AppPreferencesRepository,
) : ViewModel() {
    private val showLanguageDialog = MutableStateFlow(false)
    private val languageConfirmation = MutableStateFlow<LanguageChoice?>(null)
    private val languageSwitch = MutableStateFlow<LanguageChoice?>(null)

    val uiState: StateFlow<AppearanceSettingsUiState> = combine(
        repository.preferences,
        showLanguageDialog,
        languageConfirmation,
        languageSwitch,
    ) { preferences, showLanguage, confirmation, switching ->
        AppearanceSettingsUiState(
            showLanguageDialog = showLanguage,
            languageConfirmation = confirmation,
            languageSwitch = switching,
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
            AppearanceSettingsUiEvent.DismissLanguageDialog -> {
                languageConfirmation.value = null
                showLanguageDialog.value = false
            }
            is AppearanceSettingsUiEvent.LanguageSelected -> {
                if (languageSwitch.value != null) return
                languageConfirmation.value = LanguageChoice(event.languageTag)
            }
            AppearanceSettingsUiEvent.DismissLanguageConfirmation ->
                languageConfirmation.value = null
            AppearanceSettingsUiEvent.ConfirmLanguageSwitch -> {
                val choice = languageConfirmation.value ?: return
                // 确认框留着，等盖层盖住后再和语言列表一起关。
                languageSwitch.value = choice
            }
            AppearanceSettingsUiEvent.ApplyLanguageSwitch -> applyLanguageSwitch()
            AppearanceSettingsUiEvent.LanguageSwitchFinished -> {
                languageSwitch.value = null
                languageConfirmation.value = null
                showLanguageDialog.value = false
            }
            is AppearanceSettingsUiEvent.DarkModeChanged ->
                persist { repository.setDarkMode(event.mode) }
            is AppearanceSettingsUiEvent.ThemeStyleChanged ->
                persist { repository.setThemeStyle(event.style) }
        }
    }

    private fun applyLanguageSwitch() {
        val choice = languageSwitch.value ?: return
        val locales = if (choice.languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(choice.languageTag)
        }
        // 相同语言不再写入。Activity 声明了 locale 配置变更，这里不会重建窗口。
        if (locales != AppCompatDelegate.getApplicationLocales()) {
            AppCompatDelegate.setApplicationLocales(locales)
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
