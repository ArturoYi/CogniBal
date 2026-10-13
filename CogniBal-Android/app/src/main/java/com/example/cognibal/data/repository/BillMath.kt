package com.example.cognibal.data.repository

import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.model.BillType
import kotlin.math.absoluteValue

/**
 * 一条账单对账户余额的影响，单位分。
 *
 * 支出减少余额，收入增加。转账的库存金额自己带符号：负的是转出，正的是转入。
 */
internal fun Bill.balanceDelta(): Long = balanceDelta(type, amount)

internal fun balanceDelta(type: BillType, amount: Long): Long = when (type) {
    BillType.Expense -> -amount
    BillType.Income -> amount
    BillType.Transfer -> amount
}

/** 界面上显示的正数。转账把符号去掉，支出和收入本来就是正数。 */
fun Bill.displayAmount(): Long = amount.absoluteValue

fun Bill.isTransferOut(): Boolean = type == BillType.Transfer && amount < 0

fun Bill.isTransferIn(): Boolean = type == BillType.Transfer && amount > 0
