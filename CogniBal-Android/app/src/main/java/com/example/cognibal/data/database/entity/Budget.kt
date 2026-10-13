package com.example.cognibal.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.database.model.BudgetType
import java.time.LocalDateTime

/**
 * 一条预算。没有 usedAmount，已花费每次从账单表按区间求和。
 *
 * [periodStart] 含在区间里，[periodEnd] 不包含。月预算的结束是下月 1 日 0 点，
 * 而不是本月最后一秒，这样整点不会被漏掉或算进两个月。
 *
 * [alertPercent] 为 0 表示不提醒。提醒本身不在这张表，也不建 Reminder 表，
 * 界面拿已花费和 [limitAmount] 比较即可。
 */
@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index(value = ["periodStart", "periodEnd"]),
    ],
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val budgetType: BudgetType,
    val categoryId: Long?,
    val limitAmount: Long,
    val alertPercent: Int,
    val periodType: BudgetPeriodType,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val isEnable: Boolean,
    val remark: String?,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
)
