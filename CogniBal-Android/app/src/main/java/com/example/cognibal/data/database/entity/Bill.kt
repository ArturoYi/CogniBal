package com.example.cognibal.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cognibal.data.database.model.BillType
import java.time.LocalDateTime

/**
 * 一条账单。
 *
 * [amount] 单位是分。支出和收入必须为正。转账拆成两条 [BillType.Transfer]：
 * 转出账户那条金额为负，转入账户那条金额为正，[categoryId] 都是 null。
 * 同一笔转账的两条记录共用一个 [createTime]，仓库靠它把两条腿找回来。
 *
 * [billTime] 是交易发生时间，日历和统计按它分组，不按 [createTime]。
 * 删除账户或分类时如果还有账单引用，外键会拒绝，避免把历史抹掉。
 */
@Entity(
    tableName = "bill",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("billTime"),
        Index("accountId"),
        Index("categoryId"),
        Index(value = ["type", "billTime"]),
        Index(value = ["type", "categoryId", "billTime"]),
        Index(value = ["type", "createTime"]),
    ],
)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long,
    val type: BillType,
    val categoryId: Long?,
    val accountId: Long,
    val billTime: LocalDateTime,
    val remark: String?,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
)
