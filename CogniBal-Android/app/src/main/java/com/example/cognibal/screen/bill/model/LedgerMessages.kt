package com.example.cognibal.screen.bill.model

import androidx.annotation.StringRes
import com.example.cognibal.R
import com.example.cognibal.data.repository.LedgerReject

/** 账户表单会遇到的拒绝原因。其余原因用同一句兜底。 */
@StringRes
internal fun LedgerReject.accountMessageRes(): Int = when (this) {
    LedgerReject.BlankName -> R.string.account_error_name
    LedgerReject.NameTooLong -> R.string.account_error_name_long
    LedgerReject.RemarkTooLong -> R.string.account_error_remark
    LedgerReject.InvalidAmount -> R.string.account_error_amount
    LedgerReject.AccountNotFound -> R.string.account_error_missing
    LedgerReject.AccountInUse -> R.string.account_error_in_use
    LedgerReject.LastEnabledAccount -> R.string.account_error_last_enabled
    LedgerReject.DefaultAccountProtected -> R.string.account_error_default
    else -> R.string.account_error_generic
}
