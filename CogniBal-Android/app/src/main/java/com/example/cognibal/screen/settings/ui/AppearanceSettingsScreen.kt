package com.example.cognibal.screen.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.screen.settings.viewmodel.AppearanceSettingsUiEvent
import com.example.cognibal.screen.settings.viewmodel.AppearanceSettingsUiState
import com.example.cognibal.screen.settings.viewmodel.AppearanceSettingsViewModel
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/** 外观设置入口。状态在 ViewModel，语言点击先不切换。 */
@Composable
internal fun AppearanceSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppearanceSettingsViewModel = viewModel(factory = AppearanceSettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppearanceSettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun AppearanceSettingsScreen(
    uiState: AppearanceSettingsUiState,
    onEvent: (AppearanceSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = "外观设置",
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsSectionCard {
            SettingActionItem(
                icon = Icons.Rounded.Language,
                title = "语言",
                valueText = uiState.languageLabel,
                onClick = { onEvent(AppearanceSettingsUiEvent.LanguageClicked) },
            )
            SettingsDivider()
            ThemeModeSelector(
                currentPreference = uiState.darkMode,
                onSelectPreference = { onEvent(AppearanceSettingsUiEvent.DarkModeChanged(it)) },
            )
            SettingsDivider()
            ThemeStyleSelector(
                currentStyle = uiState.themeStyle,
                onSelectStyle = { onEvent(AppearanceSettingsUiEvent.ThemeStyleChanged(it)) },
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun AppearanceSettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.appColors.background) {
            AppearanceSettingsScreen(
                uiState = AppearanceSettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
