package com.example.cognibal.screen.feature.settings.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.bill.model.accountIconImage
import com.example.cognibal.screen.feature.bill.model.labelRes
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.assets.state.AssetAccountUi
import com.example.cognibal.screen.feature.settings.assets.state.AssetsUiState
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.moneyText
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.loading.CogniBalLoadingIndicator
import com.example.cognibal.ui.loading.LoadingIndicatorAppearDelayMillis
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/** 资产入口。点一行去编辑那个账户，返回仍回到这一页。 */
@Composable
internal fun AssetsRoute(
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssetsViewModel = viewModel(factory = AssetsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AssetsScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenAccount = onOpenAccount,
        modifier = modifier,
    )
}

@Composable
internal fun AssetsScreen(
    uiState: AssetsUiState,
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = stringResource(R.string.shortcut_assets),
        onBack = onBack,
        modifier = modifier,
        placeholder = if (uiState.loading) {
            {
                CogniBalLoadingIndicator(
                    showDelayMillis = LoadingIndicatorAppearDelayMillis,
                )
            }
        } else {
            null
        },
    ) {
        InsightCard {
            AssetSummary(uiState)
        }
        if (uiState.assetAccounts.isNotEmpty()) {
            AccountSection(
                title = stringResource(R.string.assets_section_assets),
                accounts = uiState.assetAccounts,
                currencySymbol = uiState.currencySymbol,
                debt = false,
                onOpenAccount = onOpenAccount,
            )
        }
        if (uiState.liabilityAccounts.isNotEmpty()) {
            AccountSection(
                title = stringResource(R.string.assets_section_liabilities),
                accounts = uiState.liabilityAccounts,
                currencySymbol = uiState.currencySymbol,
                debt = true,
                onOpenAccount = onOpenAccount,
            )
        }
    }
}

@Composable
private fun AssetSummary(uiState: AssetsUiState) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = stringResource(R.string.assets_net),
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = moneyText(uiState.currencySymbol, uiState.net, uiState.netOverflow),
                color = amountColor(
                    overflow = uiState.netOverflow,
                    negative = uiState.net < 0L,
                    debt = false,
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            AssetFigure(
                label = stringResource(R.string.assets_assets),
                amount = moneyText(uiState.currencySymbol, uiState.assets, uiState.assetsOverflow),
                color = amountColor(
                    overflow = uiState.assetsOverflow,
                    negative = uiState.assets < 0L,
                    debt = false,
                ),
                modifier = Modifier.weight(1f),
            )
            AssetFigure(
                label = stringResource(R.string.assets_liabilities),
                amount = moneyText(
                    uiState.currencySymbol,
                    uiState.liabilities,
                    uiState.liabilitiesOverflow,
                ),
                color = amountColor(
                    overflow = uiState.liabilitiesOverflow,
                    negative = false,
                    debt = uiState.liabilities > 0L,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AssetFigure(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.appColors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = amount,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AccountSection(
    title: String,
    accounts: List<AssetAccountUi>,
    currencySymbol: String,
    debt: Boolean,
    onOpenAccount: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.appColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        InsightCard(itemSpacing = 4.dp) {
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    currencySymbol = currencySymbol,
                    debt = debt,
                    onClick = dropUnlessResumed { onOpenAccount(account.id) },
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: AssetAccountUi,
    currencySymbol: String,
    debt: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(account.colorKey)
    val typeName = stringResource(account.type.labelRes())
    val subtitle = if (account.enabled) {
        typeName
    } else {
        stringResource(R.string.assets_disabled_type, typeName)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveClick(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    tint.copy(alpha = if (colors.isDark) 0.22f else 0.16f),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = accountIconImage(account.iconKey),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = account.name,
                color = if (account.enabled) colors.textPrimary else colors.textTertiary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = colors.textTertiary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = moneyText(currencySymbol, account.amount, account.amountOverflow),
            color = amountColor(
                overflow = account.amountOverflow,
                negative = account.amount < 0L,
                debt = debt,
            ),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun amountColor(overflow: Boolean, negative: Boolean, debt: Boolean): Color {
    val colors = MaterialTheme.appColors
    return when {
        overflow -> colors.textSecondary
        debt || negative -> colors.expense
        else -> colors.textPrimary
    }
}

@FormFactorPreviews
@Composable
private fun AssetsScreenPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            AssetsScreen(
                uiState = AssetsUiState(
                    loading = false,
                    net = 1_271_000L,
                    assets = 1_357_000L,
                    liabilities = 86_000L,
                    assetAccounts = listOf(
                        AssetAccountUi(1, "现金", AccountType.Cash, "cash", "teal", 32_000L, false, true),
                        AssetAccountUi(2, "工资卡", AccountType.DebitCard, "bank", "blue", 1_280_000L, false, true),
                        AssetAccountUi(3, "支付宝", AccountType.EWallet, "phone", "sand", 45_000L, false, false),
                    ),
                    liabilityAccounts = listOf(
                        AssetAccountUi(4, "信用卡", AccountType.CreditCard, "card", "clay", 86_000L, false, true),
                    ),
                ),
                onBack = {},
                onOpenAccount = {},
            )
        }
    }
}
