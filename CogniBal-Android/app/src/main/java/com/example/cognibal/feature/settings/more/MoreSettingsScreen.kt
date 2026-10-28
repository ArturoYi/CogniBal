package com.example.cognibal.screen.feature.settings.more

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.CurrencySelectDialog
import com.example.cognibal.screen.feature.settings.SettingActionItem
import com.example.cognibal.screen.feature.settings.SettingsDivider
import com.example.cognibal.screen.feature.settings.SettingsSectionCard
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.screen.feature.settings.more.state.CurrencySwitchFailure
import com.example.cognibal.screen.feature.settings.more.state.MoreSettingsUiEvent
import com.example.cognibal.screen.feature.settings.more.state.MoreSettingsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.currentPageBackgroundColor
import com.example.cognibal.ui.loading.CogniBalLoadingIndicator
import com.example.cognibal.ui.overlay.FullScreenDialog
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/** 更多设置入口。货币和清理缓存都在这一页。 */
@Composable
internal fun MoreSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreSettingsViewModel = viewModel(factory = MoreSettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cacheClearedMessage = stringResource(R.string.cache_cleared)
    MoreSettingsScreen(
        uiState = uiState,
        onEvent = { event ->
            viewModel.onEvent(event)
            if (event == MoreSettingsUiEvent.ConfirmClearCache) {
                Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun MoreSettingsScreen(
    uiState: MoreSettingsUiState,
    onEvent: (MoreSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = stringResource(R.string.settings_more),
        onBack = onBack,
        modifier = modifier,
    ) {
        SettingsSectionCard {
            SettingActionItem(
                icon = Icons.Outlined.Payments,
                title = stringResource(R.string.default_currency),
                valueText = uiState.currency.symbol,
                onClick = { onEvent(MoreSettingsUiEvent.OpenCurrencyDialog) },
            )
            SettingsDivider()
            SettingActionItem(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.clear_temp_cache),
                valueText = uiState.cacheSizeText,
                onClick = { onEvent(MoreSettingsUiEvent.OpenClearCacheDialog) },
            )
        }
    }

    if (uiState.showCurrencyDialog) {
        CurrencySelectDialog(
            selectedCurrency = uiState.currency,
            onSelect = { onEvent(MoreSettingsUiEvent.CurrencySelected(it)) },
            onDismiss = { onEvent(MoreSettingsUiEvent.DismissCurrencyDialog) },
        )
    }

    val rateTarget = uiState.rateTarget
    if (rateTarget != null) {
        CurrencyRateDialog(
            from = uiState.currency,
            to = rateTarget,
            rateText = uiState.rateText,
            rateReady = uiState.rateReady,
            onRateText = { onEvent(MoreSettingsUiEvent.RateTextChanged(it)) },
            onConfirm = { onEvent(MoreSettingsUiEvent.ConfirmRate) },
            onDismiss = { onEvent(MoreSettingsUiEvent.DismissRateDialog) },
        )
    }

    uiState.failure?.let { failure ->
        CurrencySwitchFailureDialog(
            failure = failure,
            onRetry = { onEvent(MoreSettingsUiEvent.RetryCurrencySwitch) },
            onDismiss = { onEvent(MoreSettingsUiEvent.DismissCurrencyFailure) },
        )
    }

    if (uiState.converting) {
        CurrencySwitchCover()
    }

    if (uiState.showClearCacheDialog) {
        ClearCacheDialog(
            cacheSizeText = uiState.cacheSizeText,
            onConfirm = { onEvent(MoreSettingsUiEvent.ConfirmClearCache) },
            onDismiss = { onEvent(MoreSettingsUiEvent.DismissClearCacheDialog) },
        )
    }
}

@Composable
private fun CurrencyRateDialog(
    from: CurrencyPreference,
    to: CurrencyPreference,
    rateText: String,
    rateReady: Boolean,
    onRateText: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val fromName = stringResource(from.nameRes)
    val toName = stringResource(to.nameRes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.currency_rate_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.currency_rate_body, fromName, toName),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = onRateText,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.currency_rate_hint)) },
                    prefix = {
                        Text(text = "1 ${from.symbol} =", color = colors.textSecondary)
                    },
                    suffix = {
                        Text(text = to.symbol, color = colors.textSecondary)
                    },
                    isError = rateText.isNotEmpty() && !rateReady,
                    supportingText = if (rateText.isNotEmpty() && !rateReady) {
                        {
                            Text(text = stringResource(R.string.currency_rate_invalid))
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (rateReady) onConfirm() },
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.textTertiary,
                        errorLabelColor = colors.expense,
                        errorBorderColor = colors.expense,
                        errorSupportingTextColor = colors.expense,
                        focusedContainerColor = colors.cardBackground,
                        unfocusedContainerColor = colors.cardBackground,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = rateReady) {
                Text(
                    text = stringResource(R.string.currency_rate_confirm),
                    color = if (rateReady) colors.primary else colors.textTertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun CurrencySwitchFailureDialog(
    failure: CurrencySwitchFailure,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val message = when (failure) {
        CurrencySwitchFailure.Overflow -> R.string.currency_switch_overflow
        CurrencySwitchFailure.TimedOut -> R.string.currency_switch_timeout
        CurrencySwitchFailure.Failed -> R.string.currency_switch_failed
        CurrencySwitchFailure.Preference -> R.string.currency_switch_pref_failed
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.currency_switch_error_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(message),
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.currency_switch_retry),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun CurrencySwitchCover() {
    val colors = MaterialTheme.appColors
    FullScreenDialog(lightSystemBars = !colors.isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentPageBackgroundColor()),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                CogniBalLoadingIndicator(
                    contentDescription = null,
                    size = 72.dp,
                )
                Text(
                    text = stringResource(R.string.currency_switch_working),
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ClearCacheDialog(
    cacheSizeText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.clear_cache_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.clear_cache_message, cacheSizeText),
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.clear_cache_confirm),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

@FormFactorPreviews
@Composable
private fun MoreSettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            MoreSettingsScreen(
                uiState = MoreSettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
