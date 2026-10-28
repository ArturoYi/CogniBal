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
 * [periodStart] 含在区间里，[periodEnd] 不包含。
 * 月预算按自然月：从 1 日 0 点到下月 1 日 0 点。[cycleDay] 写入 1。
 * 整点落在结束一侧，不会被漏掉或算进两个周期。
 *
 * [alertPercent] 是 50 到 100 的预警线，新写入默认 80。
 * 提醒不建表：界面拿已花费和 [limitAmount] 比较。
 *
 * 周期和账单时间都走 [com.example.cognibal.data.database.LedgerTime]：
 * 写入时的本地时间，文本不含时区。改系统时区不会重算已经入库的边界。
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
    /** 月预算写入 1，表示自然月。周、季、年为空。 */
    val cycleDay: Int? = null,
    val periodStart: LocalDateTime,
    val periodEnd: LocalDateTime,
    val isEnable: Boolean,
    val remark: String?,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
)
