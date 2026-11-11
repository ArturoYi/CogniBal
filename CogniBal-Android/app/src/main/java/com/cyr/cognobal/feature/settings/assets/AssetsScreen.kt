package com.cyr.cognobal.screen.feature.settings.assets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.AccountType
import com.cyr.cognobal.screen.feature.bill.AccountHeroSurface
import com.cyr.cognobal.screen.feature.bill.AccountMark
import com.cyr.cognobal.screen.feature.bill.PressFade
import com.cyr.cognobal.screen.feature.settings.SettingsSubpage
import com.cyr.cognobal.screen.feature.settings.assets.state.AssetAccountUi
import com.cyr.cognobal.screen.feature.settings.assets.state.AssetsUiState
import com.cyr.cognobal.screen.feature.settings.component.HeaderCircleButton
import com.cyr.cognobal.screen.feature.settings.component.moneyText
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.loading.CogniBalLoadingIndicator
import com.cyr.cognobal.ui.loading.LoadingIndicatorAppearDelayMillis
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.privacy.AmountEyeToggle
import com.cyr.cognobal.ui.privacy.LocalAmountVisibility
import com.cyr.cognobal.ui.privacy.maskIfHidden
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors

@Composable
internal fun AssetsRoute(
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    showBack: Boolean = false,
    onCreateAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AssetsViewModel = viewModel(factory = AssetsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AssetsScreen(
        uiState = uiState,
        onBack = onBack,
        showBack = showBack,
        onOpenAccount = onOpenAccount,
        onCreateAccount = onCreateAccount,
        modifier = modifier,
    )
}

/**
 * 资产页（参考设计重构）：
 * 1. 顶部标题「资产」 + 右上角「+」新建账户按钮
 * 2. 曜石深灰净资产主卡（带隐私显隐开关 + 香槟金流线波纹艺术线条）
 * 3. 独立纯白圆角账户行列表（粉彩图标块 + 账户名 + 完整等宽余额 + 右向箭头）
 */
@Composable
internal fun AssetsScreen(
    uiState: AssetsUiState,
    onBack: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    showBack: Boolean = false,
    onCreateAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = stringResource(R.string.shortcut_assets),
        onBack = onBack,
        showBackOverride = showBack,
        trailing = {
            HeaderCircleButton(
                icon = Icons.Outlined.Add,
                description = stringResource(R.string.bill_account_create),
                onClick = dropUnlessResumed(block = onCreateAccount),
            )
        },
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
        NetAssetsHeroCard(uiState)

        if (uiState.assetAccounts.isEmpty() && uiState.liabilityAccounts.isEmpty()) {
            EmptyAssetsRow(onCreate = dropUnlessResumed(block = onCreateAccount))
        } else {
            if (uiState.assetAccounts.isNotEmpty()) {
                AccountListGroup(
                    title = stringResource(R.string.assets_section_assets),
                    accounts = uiState.assetAccounts,
                    currencySymbol = uiState.currencySymbol,
                    onOpenAccount = onOpenAccount,
                )
            }
            if (uiState.liabilityAccounts.isNotEmpty()) {
                AccountListGroup(
                    title = stringResource(R.string.assets_section_liabilities),
                    accounts = uiState.liabilityAccounts,
                    currencySymbol = uiState.currencySymbol,
                    onOpenAccount = onOpenAccount,
                )
            }
        }
    }
}

@Composable
private fun NetAssetsHeroCard(uiState: AssetsUiState) {
    val visibility = LocalAmountVisibility.current
    val hidden = visibility.hidden
    val netRaw = moneyText(uiState.currencySymbol, uiState.net, uiState.netOverflow)
    val netText = maskIfHidden(netRaw, hidden, uiState.currencySymbol)
    val hasLiabilities = uiState.liabilities != 0L || uiState.liabilityAccounts.isNotEmpty()

    AccountHeroSurface { paint ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp),
        ) {
            GoldenWaveArt(
                accent = paint.identity,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(152.dp)
                    .height(68.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.assets_net),
                        color = paint.on.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    AmountEyeToggle(
                        hidden = hidden,
                        onToggle = visibility::toggle,
                        tint = paint.on.copy(alpha = 0.72f),
                    )
                }

                Text(
                    text = netText,
                    color = paint.on,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 20.sp,
                        maxFontSize = 32.sp,
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (hasLiabilities) {
                    val assetsStr = maskIfHidden(
                        moneyText(uiState.currencySymbol, uiState.assets, uiState.assetsOverflow),
                        hidden,
                        uiState.currencySymbol,
                    )
                    val liabStr = maskIfHidden(
                        moneyText(uiState.currencySymbol, uiState.liabilities, uiState.liabilitiesOverflow),
                        hidden,
                        uiState.currencySymbol,
                    )
                    Text(
                        text = "${stringResource(R.string.assets_assets)} $assetsStr  ·  ${stringResource(R.string.assets_liabilities)} $liabStr",
                        color = paint.on.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.bodySmall.merge(
                            TextStyle(fontFeatureSettings = "tnum"),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoldenWaveArt(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val linePath = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.22f, h * 0.52f, w * 0.36f, h * 0.82f, w * 0.56f, h * 0.46f)
            cubicTo(w * 0.74f, h * 0.16f, w * 0.86f, h * 0.56f, w, h * 0.28f)
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                startY = h * 0.15f,
                endY = h,
            ),
        )
        drawPath(
            path = linePath,
            color = accent.copy(alpha = 0.72f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AccountListGroup(
    title: String,
    accounts: List<AssetAccountUi>,
    currencySymbol: String,
    onOpenAccount: (Long) -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        accounts.forEach { account ->
            AssetAccountRow(
                account = account,
                currencySymbol = currencySymbol,
                onClick = dropUnlessResumed { onOpenAccount(account.id) },
            )
        }
    }
}

@Composable
private fun AssetAccountRow(
    account: AssetAccountUi,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val shape = RoundedCornerShape(18.dp)
    val amountStr = maskIfHidden(
        moneyText(currencySymbol, account.amount, account.amountOverflow),
        hidden,
        currencySymbol,
    )

    PressFade(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(shape)
            .background(colors.cardBackground),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountMark(
                iconKey = account.iconKey,
                colorKey = account.colorKey,
                tileSize = 40.dp,
                iconSize = 20.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = account.name,
                    color = if (account.enabled) colors.textPrimary else colors.textTertiary,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!account.enabled) {
                    Text(
                        text = stringResource(R.string.account_status_disabled),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = amountStr,
                color = if (account.enabled) colors.textPrimary else colors.textTertiary,
                style = MaterialTheme.typography.titleMedium.merge(
                    TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                ),
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.65f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyAssetsRow(onCreate: () -> Unit) {
    val colors = MaterialTheme.appColors
    PressFade(
        onClick = onCreate,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.bill_account_create),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
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
                    net = 8_888_800L,
                    assets = 8_888_800L,
                    liabilities = 0L,
                    assetAccounts = listOf(
                        AssetAccountUi(1, "现金", AccountType.Cash, "cash", "teal", 688_800L, false, true),
                        AssetAccountUi(2, "银行卡", AccountType.DebitCard, "bank", "blue", 3_256_000L, false, true),
                        AssetAccountUi(3, "电子钱包", AccountType.EWallet, "phone", "sand", 832_000L, false, true),
                    ),
                ),
                onBack = {},
                onOpenAccount = {},
            )
        }
    }
}
