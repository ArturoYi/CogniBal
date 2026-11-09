package com.cyr.cognobal.screen.feature.settings.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.legal.LegalDocumentKind
import com.cyr.cognobal.screen.feature.settings.SettingActionItem
import com.cyr.cognobal.screen.feature.settings.SettingsDivider
import com.cyr.cognobal.screen.feature.settings.SettingsSectionCard
import com.cyr.cognobal.screen.feature.settings.SettingsSubpage
import com.cyr.cognobal.screen.feature.settings.security.state.SecuritySettingsAction
import com.cyr.cognobal.screen.feature.settings.security.state.SecuritySettingsUiEvent
import com.cyr.cognobal.screen.feature.settings.security.state.SecuritySettingsUiState
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors

/** 安全与隐私。协议打开全文。退到后台可以盖住账本。 */
@Composable
internal fun SecuritySettingsRoute(
    onBack: () -> Unit,
    onOpenDocument: (LegalDocumentKind) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecuritySettingsViewModel = viewModel(factory = SecuritySettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openTerms = dropUnlessResumed { onOpenDocument(LegalDocumentKind.Terms) }
    val openPrivacy = dropUnlessResumed { onOpenDocument(LegalDocumentKind.Privacy) }
    SecuritySettingsScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                is SecuritySettingsUiEvent.RowClicked -> when (event.action) {
                    SecuritySettingsAction.Terms -> openTerms()
                    SecuritySettingsAction.Privacy -> openPrivacy()
                }
                is SecuritySettingsUiEvent.HideContentInBackgroundChanged -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun SecuritySettingsScreen(
    uiState: SecuritySettingsUiState,
    onEvent: (SecuritySettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = stringResource(R.string.settings_security),
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsSectionCard {
            HideContentInBackgroundRow(
                checked = uiState.hideContentInBackground,
                onCheckedChange = { enabled ->
                    onEvent(SecuritySettingsUiEvent.HideContentInBackgroundChanged(enabled))
                },
            )
            uiState.rows.forEach { row ->
                SettingsDivider()
                SettingActionItem(
                    icon = row.action.icon,
                    title = stringResource(row.titleRes),
                    subtitle = stringResource(row.subtitleRes),
                    onClick = { onEvent(SecuritySettingsUiEvent.RowClicked(row.action)) },
                )
            }
        }
    }
}

/**
 * 整行都能拨动。开关只负责外形，避免和这一行抢焦点、抢读屏。
 */
@Composable
private fun HideContentInBackgroundRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val iconBackground = colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(
                if (focused) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBackground, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.security_hide_content),
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.security_hide_content_subtitle),
                color = colors.textTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .clearAndSetSemantics {},
        )
    }
}

private val SecuritySettingsAction.icon: ImageVector
    get() = when (this) {
        SecuritySettingsAction.Terms -> Icons.AutoMirrored.Outlined.Article
        SecuritySettingsAction.Privacy -> Icons.Outlined.PrivacyTip
    }

@FormFactorPreviews
@Composable
private fun SecuritySettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        PageBackgroundHost {
            SecuritySettingsScreen(
                uiState = SecuritySettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
