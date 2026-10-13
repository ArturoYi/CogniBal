package com.example.cognibal.data.repository

import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.CategoryType
import java.time.LocalDateTime

/**
 * 支出或收入的写入参数。转账不走这个类型，避免调用方漏掉第二条腿。
 */
enum class EntryType {
    Expense,
    Income,
    ;

    fun toBillType(): BillType = when (this) {
        Expense -> BillType.Expense
        Income -> BillType.Income
    }

    fun toCategoryType(): CategoryType = when (this) {
        Expense -> CategoryType.Expense
        Income -> CategoryType.Income
    }
}

data class EntryDraft(
    val type: EntryType,
    val amount: Long,
    val categoryId: Long,
    val accountId: Long,
    val billTime: LocalDateTime,
    val remark: String?,
)

/** 一次转账。金额为正，仓库会写成一负一正两条账单。 */
data class TransferDraft(
    val amount: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val billTime: LocalDateTime,
    val remark: String?,
)

data class TransferIds(
    val outBillId: Long,
    val inBillId: Long,
)
