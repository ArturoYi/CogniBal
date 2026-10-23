package com.example.cognibal.screen.feature.settings.assets.state

import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference

internal data class AssetAccountUi(
    val id: Long,
    val name: String,
    val type: AccountType,
    val iconKey: String,
    val colorKey: String,
    /** 资产行是带符号的余额。负债行是正数欠款。 */
    val amount: Long,
    val amountOverflow: Boolean,
    val enabled: Boolean,
)

internal data class AssetsUiState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val net: Long = 0L,
    val netOverflow: Boolean = false,
    val assets: Long = 0L,
    val assetsOverflow: Boolean = false,
    val liabilities: Long = 0L,
    val liabilitiesOverflow: Boolean = false,
    val assetAccounts: List<AssetAccountUi> = emptyList(),
    val liabilityAccounts: List<AssetAccountUi> = emptyList(),
)

internal fun Account.toAssetAccountUi(debt: Boolean): AssetAccountUi {
    val (amount, overflow) = if (!debt) {
        balance to false
    } else {
        try {
            Math.negateExact(balance) to false
        } catch (_: ArithmeticException) {
            0L to true
        }
    }
    return AssetAccountUi(
        id = id,
        name = name,
        type = type,
        iconKey = icon,
        colorKey = color,
        amount = amount,
        amountOverflow = overflow,
        enabled = isEnable,
    )
}
