package com.example.cognibal.screen.feature.expense.tool

import com.example.cognibal.data.database.entity.Account

/**
 * 还没选，或选中的账户已经不在启用列表里时，用当前展示的账户，否则用第一个。
 */
internal fun resolveAccountId(selected: Long?, accounts: List<Account>): Long? {
    if (selected != null && accounts.any { it.id == selected }) return selected
    return accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
}

/** 转入账户必须和转出不同。原先的选择失效后，改用另一个启用账户。 */
internal fun resolveCounterpartyId(
    selected: Long?,
    fromId: Long?,
    accounts: List<Account>,
): Long? {
    if (selected != null && selected != fromId && accounts.any { it.id == selected }) return selected
    return accounts.firstOrNull { it.id != fromId }?.id
}
