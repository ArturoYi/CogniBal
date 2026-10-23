package com.example.cognibal.data.repository

import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.model.AccountType

/**
 * 净资产拆分。单位是分。
 *
 * [net] 是全部账户余额之和，顺序和首页账户条一样：先启用，再按 id。
 * 信用卡余额为负时，绝对值算 [liabilities]；其余余额算 [assets]，透支仍留在资产里。
 * 停用账户也计入。某一块相加溢出时，只把那一块标成溢出，另外两块仍按自己的加法出结果。
 */
internal data class AssetSnapshot(
    val net: Long,
    val netOverflow: Boolean,
    val assets: Long,
    val assetsOverflow: Boolean,
    val liabilities: Long,
    val liabilitiesOverflow: Boolean,
    val assetAccounts: List<Account>,
    val liabilityAccounts: List<Account>,
)

/** 和首页账户条同一套次序，总资产按这个顺序相加。 */
internal fun <T> assetAccountOrder(enabled: (T) -> Boolean, id: (T) -> Long): Comparator<T> =
    compareByDescending(enabled).thenBy(id)

internal fun List<Account>.toAssetSnapshot(): AssetSnapshot {
    val ordered = sortedWith(assetAccountOrder(Account::isEnable, Account::id))
    val assetAccounts = ArrayList<Account>()
    val liabilityAccounts = ArrayList<Account>()
    for (account in ordered) {
        if (account.isLiability()) {
            liabilityAccounts += account
        } else {
            assetAccounts += account
        }
    }
    val (net, netOverflow) = sumMinorAmounts(ordered.map { it.balance })
    val (assets, assetsOverflow) = sumMinorAmounts(assetAccounts.map { it.balance })
    val (liabilities, liabilitiesOverflow) = sumLiabilities(liabilityAccounts)
    return AssetSnapshot(
        net = net,
        netOverflow = netOverflow,
        assets = assets,
        assetsOverflow = assetsOverflow,
        liabilities = liabilities,
        liabilitiesOverflow = liabilitiesOverflow,
        assetAccounts = assetAccounts,
        liabilityAccounts = liabilityAccounts,
    )
}

/** 把若干分精确相加。溢出时返回 0 和 true。 */
internal fun sumMinorAmounts(amounts: List<Long>): Pair<Long, Boolean> {
    var total = 0L
    for (amount in amounts) {
        try {
            total = Math.addExact(total, amount)
        } catch (_: ArithmeticException) {
            return 0L to true
        }
    }
    return total to false
}

/** 信用卡欠款。余额为负才算负债，零和溢缴仍是资产。 */
internal fun Account.isLiability(): Boolean = type == AccountType.CreditCard && balance < 0L

private fun sumLiabilities(accounts: List<Account>): Pair<Long, Boolean> {
    val amounts = ArrayList<Long>(accounts.size)
    for (account in accounts) {
        val debt = try {
            Math.negateExact(account.balance)
        } catch (_: ArithmeticException) {
            return 0L to true
        }
        amounts += debt
    }
    return sumMinorAmounts(amounts)
}
