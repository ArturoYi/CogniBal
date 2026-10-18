package com.example.cognibal.data.repository

/**
 * 仓库拒绝这次写入的原因。界面可以按它决定提示文案。
 * 数据没有部分提交：抛出时事务所在的那一次改动会全部回滚。
 */
enum class LedgerReject {
    BlankName,
    NameTooLong,
    BlankIcon,
    BlankColor,
    RemarkTooLong,
    InvalidAmount,
    AccountNotFound,
    AccountDisabled,
    CategoryNotFound,
    CategoryDisabled,
    CategoryTypeMismatch,
    CategoryForbidden,
    SameTransferAccount,
    BillNotFound,
    NotTransfer,
    TransferBroken,
    AmbiguousTransfer,
    UseTransferApi,
    BudgetNotFound,
    BudgetCategoryRequired,
    BudgetCategoryMustBeExpense,
    BudgetCategoryForbidden,
    InvalidAlertPercent,
    InvalidPeriod,
    InvalidLimit,
    AccountInUse,
    /** 再停用或删掉就会一个启用账户都不剩。 */
    LastEnabledAccount,
    /** 当前展示的账户不能物理删除。 */
    DefaultAccountProtected,
    CategoryInUse,
    BalanceOverflow,
}

class LedgerException(
    val reason: LedgerReject,
) : IllegalStateException(reason.name)
