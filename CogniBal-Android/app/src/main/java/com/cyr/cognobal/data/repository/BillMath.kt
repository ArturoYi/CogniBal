package com.cyr.cognobal.data.repository

import com.cyr.cognobal.data.database.entity.Bill
import com.cyr.cognobal.data.database.model.BillType

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
