package com.example.cognibal.screen.expense.model

import androidx.annotation.StringRes
import com.example.cognibal.R
import com.example.cognibal.data.repository.LedgerReject

/** 记一笔时能直接告诉用户的问题。仓库拒绝的原因在保存失败后映射过来。 */
internal enum class AddBillIssue {
    InvalidAmount,
    NoAccount,
    NoCategory,
    SameAccount,
    NeedSecondAccount,
    RemarkTooLong,
    AccountDisabled,
    CategoryDisabled,
    CategoryTypeMismatch,
    AccountMissing,
    CategoryMissing,
    BalanceOverflow,
    BlankName,
    NameTooLong,
    Generic,
}

@StringRes
internal fun AddBillIssue.messageRes(): Int = when (this) {
    AddBillIssue.InvalidAmount -> R.string.bill_entry_error_amount
    AddBillIssue.NoAccount -> R.string.bill_entry_error_account
    AddBillIssue.NoCategory -> R.string.bill_entry_error_category
    AddBillIssue.SameAccount -> R.string.bill_entry_error_same_account
    AddBillIssue.NeedSecondAccount -> R.string.bill_entry_error_second_account
    AddBillIssue.RemarkTooLong -> R.string.bill_entry_error_remark
    AddBillIssue.AccountDisabled -> R.string.bill_entry_error_disabled_account
    AddBillIssue.CategoryDisabled -> R.string.bill_entry_error_disabled_category
    AddBillIssue.CategoryTypeMismatch -> R.string.bill_entry_error_category_type
    AddBillIssue.AccountMissing -> R.string.bill_entry_error_missing_account
    AddBillIssue.CategoryMissing -> R.string.bill_entry_error_missing_category
    AddBillIssue.BalanceOverflow -> R.string.bill_entry_error_overflow
    AddBillIssue.BlankName -> R.string.bill_entry_error_category_name
    AddBillIssue.NameTooLong -> R.string.bill_entry_error_category_name_long
    AddBillIssue.Generic -> R.string.bill_entry_error_generic
}

internal fun LedgerReject.toAddBillIssue(): AddBillIssue = when (this) {
    LedgerReject.InvalidAmount -> AddBillIssue.InvalidAmount
    LedgerReject.RemarkTooLong -> AddBillIssue.RemarkTooLong
    LedgerReject.AccountNotFound -> AddBillIssue.AccountMissing
    LedgerReject.AccountDisabled -> AddBillIssue.AccountDisabled
    LedgerReject.CategoryNotFound -> AddBillIssue.CategoryMissing
    LedgerReject.CategoryDisabled -> AddBillIssue.CategoryDisabled
    LedgerReject.CategoryTypeMismatch -> AddBillIssue.CategoryTypeMismatch
    LedgerReject.SameTransferAccount -> AddBillIssue.SameAccount
    LedgerReject.BalanceOverflow -> AddBillIssue.BalanceOverflow
    LedgerReject.BlankName -> AddBillIssue.BlankName
    LedgerReject.NameTooLong -> AddBillIssue.NameTooLong
    else -> AddBillIssue.Generic
}
