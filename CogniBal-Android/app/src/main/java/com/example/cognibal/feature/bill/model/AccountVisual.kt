package com.example.cognibal.screen.feature.bill.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.ui.theme.OpenColor

/**
 * 账户类型的界面文案。库存里仍是 [AccountType.code]，不写翻译后的名字。
 */
@StringRes
internal fun AccountType.labelRes(): Int = when (this) {
    AccountType.Cash -> R.string.account_type_cash
    AccountType.DebitCard -> R.string.account_type_debit
    AccountType.CreditCard -> R.string.account_type_credit
    AccountType.EWallet -> R.string.account_type_ewallet
    AccountType.Other -> R.string.account_type_other
}

/**
 * 账户图标。 [storageKey] 写入账户表的 icon 列，换语言不会改已经入库的值。
 */
internal enum class AccountIcon(
    val storageKey: String,
    @param:StringRes val labelRes: Int,
    val image: ImageVector,
) {
    Wallet("wallet", R.string.account_icon_wallet, Icons.Rounded.AccountBalanceWallet),
    Cash("cash", R.string.account_icon_cash, Icons.Rounded.Payments),
    Card("card", R.string.account_icon_card, Icons.Rounded.CreditCard),
    Bank("bank", R.string.account_icon_bank, Icons.Rounded.AccountBalance),
    Savings("savings", R.string.account_icon_savings, Icons.Rounded.Savings),
    Phone("phone", R.string.account_icon_phone, Icons.Rounded.Smartphone),
    Bag("bag", R.string.account_icon_bag, Icons.Rounded.ShoppingBag),
    Other("other", R.string.account_icon_other, Icons.Rounded.MoreHoriz),
    ;

    companion object {
        fun fromKey(key: String): AccountIcon? = entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * 账户颜色。 [storageKey] 写入 color 列。
 * 色值用 Open Color，按键取色，不跟主题一起变，账户换主题后仍能认出来。
 */
internal enum class AccountColor(
    val storageKey: String,
    @param:StringRes val labelRes: Int,
    val color: Color,
) {
    Teal("teal", R.string.account_color_teal, OpenColor.Teal7),
    Clay("clay", R.string.account_color_clay, OpenColor.Red7),
    Sand("sand", R.string.account_color_sand, OpenColor.Orange6),
    Blue("blue", R.string.account_color_blue, OpenColor.Blue6),
    Lilac("lilac", R.string.account_color_lilac, OpenColor.Grape6),
    Ink("ink", R.string.account_color_ink, OpenColor.Gray7),
    Gray("gray", R.string.account_color_gray, OpenColor.Gray6),
    ;

    companion object {
        fun fromKey(key: String): AccountColor? = entries.firstOrNull { it.storageKey == key }
    }
}

internal fun accountIconImage(key: String): ImageVector {
    return AccountIcon.fromKey(key)?.image ?: AccountIcon.Wallet.image
}

/** 认识枚举键；备份里的 `#AARRGGBB` 仍能画出原来的颜色。 */
internal fun accountColorValue(key: String): Color {
    AccountColor.fromKey(key)?.let { return it.color }
    val hex = key.removePrefix("#")
    val parsed = hex.toLongOrNull(16) ?: return AccountColor.Teal.color
    return when (hex.length) {
        6 -> Color(0xFF000000 or parsed)
        8 -> Color(parsed)
        else -> AccountColor.Teal.color
    }
}
