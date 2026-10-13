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
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val remark: String?,
)
