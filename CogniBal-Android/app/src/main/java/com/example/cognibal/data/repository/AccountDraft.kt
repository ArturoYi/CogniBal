package com.example.cognibal.data.repository

import com.example.cognibal.data.database.model.AccountType

/** 新建或修改账户时界面交过来的资料。余额不在这里，避免改名字时把缓存余额覆盖掉。 */
data class AccountDraft(
    val name: String,
    val icon: String,
    val color: String,
    val type: AccountType,
    val remark: String?,
)

/** 新建账户。 [openingBalance] 是期初余额，可以为负，表示一开始就有欠款。 */
data class NewAccount(
    val profile: AccountDraft,
    val openingBalance: Long,
)
