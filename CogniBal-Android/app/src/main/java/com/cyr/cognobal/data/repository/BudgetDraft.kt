package com.cyr.cognobal.data.repository

import com.cyr.cognobal.data.database.model.BudgetPeriodType
import com.cyr.cognobal.data.database.model.BudgetType
import java.time.LocalDateTime

data class BudgetDraft(
    val budgetType: BudgetType,
    val categoryId: Long?,
    val limitAmount: Long,
    val alertPercent: Int,
    val periodType: BudgetPeriodType,
    /** 月预算为 1，表示自然月。其他周期为空。 */
    val cycleDay: Int? = null,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val remark: String?,
)
