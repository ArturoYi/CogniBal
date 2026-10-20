package com.example.cognibal.screen.feature.settings.appearance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.locale.AppLanguage
import com.example.cognibal.screen.feature.settings.appearance.state.AppearanceSettingsUiEvent
import com.example.cognibal.screen.feature.settings.appearance.state.AppearanceSettingsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.screen.feature.settings.SettingActionItem
import com.example.cognibal.screen.feature.settings.SettingsDivider
import com.example.cognibal.screen.feature.settings.SettingsSectionCard
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.ThemeModeSelector
import com.example.cognibal.screen.feature.settings.ThemeStyleSelector

/** 外观设置入口。状态在 ViewModel，语言写入应用内区域。 */
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
    val languageTag = currentApplicationLanguageTag()
    var languageDialogAttached by remember { mutableStateOf(false) }
    var confirmDialogAttached by remember { mutableStateOf(false) }
    SettingsSubpage(
        title = stringResource(R.string.settings_appearance),
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsSectionCard {
            SettingActionItem(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.language),
                valueText = languageLabel(languageTag),
                onClick = { onEvent(AppearanceSettingsUiEvent.LanguageClicked) },
            )
            SettingsDivider()
            ThemeModeSelector(
                currentPreference = uiState.darkMode,
                onSelectPreference = { onEvent(AppearanceSettingsUiEvent.DarkModeChanged(it)) },
            )
            SettingsDivider()
            ThemeStyleSelector(
                onSelectStyle = { onEvent(AppearanceSettingsUiEvent.ThemeStyleChanged(it)) },
            )
        }
    }
    if (uiState.showLanguageDialog) {
        DisposableEffect(Unit) {
            languageDialogAttached = true
            onDispose { languageDialogAttached = false }
        }
        LanguageSelectDialog(
            selectedTag = languageTag,
            onSelect = { onEvent(AppearanceSettingsUiEvent.LanguageSelected(it)) },
            onDismiss = { onEvent(AppearanceSettingsUiEvent.DismissLanguageDialog) },
        )
    }
    uiState.languageConfirmation?.let { choice ->
        DisposableEffect(choice) {
            confirmDialogAttached = true
            onDispose { confirmDialogAttached = false }
        }
        LanguageSwitchConfirmDialog(
            languageName = languageLabel(choice.languageTag),
            onConfirm = { onEvent(AppearanceSettingsUiEvent.ConfirmLanguageSwitch) },
            onDismiss = { onEvent(AppearanceSettingsUiEvent.DismissLanguageConfirmation) },
        )
    }
    uiState.languageSwitch?.let { choice ->
        LanguageSwitchCover(
            targetTag = choice.languageTag,
            dialogsDismissed = !uiState.showLanguageDialog &&
                uiState.languageConfirmation == null &&
                !languageDialogAttached &&
                !confirmDialogAttached,
            onCoverShown = { onEvent(AppearanceSettingsUiEvent.DismissLanguageDialog) },
            onApply = { onEvent(AppearanceSettingsUiEvent.ApplyLanguageSwitch) },
            onFinished = { onEvent(AppearanceSettingsUiEvent.LanguageSwitchFinished) },
        )
    }
}

@Composable
private fun languageLabel(languageTag: String?): String {
    if (languageTag == null) return stringResource(R.string.language_follow_system)
    val language = AppLanguage.fromTag(languageTag) ?: return languageTag
    return stringResource(language.nameRes)
}

@FormFactorPreviews
@Composable
private fun AppearanceSettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            AppearanceSettingsScreen(
                uiState = AppearanceSettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
