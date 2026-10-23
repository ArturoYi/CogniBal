package com.example.cognibal.data.repository

import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.database.model.BudgetType
import java.time.LocalDateTime

data class BudgetDraft(
    val budgetType: BudgetType,
    val categoryId: Long?,
    val limitAmount: Long,
    val alertPercent: Int,
    val periodType: BudgetPeriodType,
    /** 月预算的起始日。1 是自然月。其他周期为空。 */
    val cycleDay: Int? = null,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val remark: String?,
)
