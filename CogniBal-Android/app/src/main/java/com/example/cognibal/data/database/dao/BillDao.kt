package com.example.cognibal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.query.CategoryAmountSummary
import com.example.cognibal.data.database.query.IdAmount
import com.example.cognibal.data.database.query.DayAmountSummary
import com.example.cognibal.data.database.query.MonthTrendSummary
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.data.database.query.TransferAmountSummary
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/**
 * 账单表的读写，以及日历、报表用的实时聚合。
 *
 * 聚合结果是普通数据类，不是表。`COALESCE` 让没有账单时合计为 0，而不是 null。
 * 支出、收入、转账的数字与 [BillType.code] 一致：0、1、2。
 * 转账金额带符号，所以收支合计只累加 type 0 和 1，避免转出的负数把支出减掉。
 */
@Dao
interface BillDao {
    @Query("SELECT * FROM bill ORDER BY billTime DESC, id DESC")
    fun observeAll(): Flow<List<Bill>>

    @Query("SELECT * FROM bill ORDER BY id ASC")
    suspend fun getAll(): List<Bill>

    @Query(
        """
        SELECT * FROM bill
        WHERE billTime >= :startInclusive AND billTime < :endExclusive
        ORDER BY billTime DESC, id DESC
        """,
    )
    fun observeBetween(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<Bill>>

    @Query(
        """
        SELECT * FROM bill
        WHERE accountId = :accountId
        ORDER BY billTime DESC, id DESC
        """,
    )
    fun observeByAccount(accountId: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bill WHERE id = :id")
    fun observeById(id: Long): Flow<Bill?>

    @Query("SELECT * FROM bill WHERE id = :id")
    suspend fun getById(id: Long): Bill?

    /** 保险柜里的在用条目。没有账单就视为空库，蓝牙迁移不会发送。 */
    @Query("SELECT COUNT(*) FROM bill")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM bill WHERE accountId = :accountId")
    suspend fun countByAccount(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM bill WHERE accountId = :accountId")
    fun observeCountByAccount(accountId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM bill WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM bill
        WHERE type = :transferType AND createTime = :createTime
        """,
    )
    suspend fun countTransfersAt(transferType: BillType, createTime: LocalDateTime): Int

    /**
     * 找同一笔转账的另一条腿。金额符号相反，创建时间相同。
     * 正常只有一条；多于一条说明两笔转账撞了同一个创建时间，仓库会拒绝继续改。
     */
    @Query(
        """
        SELECT * FROM bill
        WHERE type = :transferType
          AND createTime = :createTime
          AND id != :id
          AND amount = :oppositeAmount
        """,
    )
    suspend fun findTransferCounterparts(
        transferType: BillType,
        createTime: LocalDateTime,
        id: Long,
        oppositeAmount: Long,
    ): List<Bill>

    @Insert
    suspend fun insert(bill: Bill): Long

    @Insert
    suspend fun insertAll(bills: List<Bill>)

    @Update
    suspend fun update(bill: Bill)

    @Query("DELETE FROM bill WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bill")
    suspend fun deleteAll()

    /**
     * 日历：按本地日期汇总。没有账单的天不会返回。
     * 区间含开始，不含结束。
     */
    @Query(
        """
        SELECT substr(billTime, 1, 10) AS day,
               COALESCE(SUM(CASE WHEN type = 0 THEN amount ELSE 0 END), 0) AS expenseAmount,
               COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS incomeAmount
        FROM bill
        WHERE billTime >= :startInclusive AND billTime < :endExclusive
        GROUP BY substr(billTime, 1, 10)
        ORDER BY day ASC
        """,
    )
    fun observeDailySummary(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<DayAmountSummary>>

    /** 月度趋势。月份取账单时间的 `yyyy-MM`。 */
    @Query(
        """
        SELECT substr(billTime, 1, 7) AS month,
               COALESCE(SUM(CASE WHEN type = 0 THEN amount ELSE 0 END), 0) AS expenseAmount,
               COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS incomeAmount
        FROM bill
        WHERE billTime >= :startInclusive AND billTime < :endExclusive
        GROUP BY substr(billTime, 1, 7)
        ORDER BY month ASC
        """,
    )
    fun observeMonthlyTrend(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<MonthTrendSummary>>

    /** 饼图。调用方传入 0 或 1；转账没有分类，不会出现在这里。 */
    @Query(
        """
        SELECT categoryId AS categoryId,
               COALESCE(SUM(amount), 0) AS amount
        FROM bill
        WHERE type = :type
          AND billTime >= :startInclusive
          AND billTime < :endExclusive
          AND categoryId IS NOT NULL
        GROUP BY categoryId
        ORDER BY amount DESC
        """,
    )
    fun observeCategorySummary(
        type: BillType,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<CategoryAmountSummary>>

    /** 区间收支总览。始终一行。 */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 0 THEN amount ELSE 0 END), 0) AS expenseAmount,
               COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS incomeAmount
        FROM bill
        WHERE billTime >= :startInclusive AND billTime < :endExclusive
        """,
    )
    fun observePeriodSummary(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<PeriodAmountSummary>

    /** 一个账户在区间内的收支。转账不计入。始终一行。 */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 0 THEN amount ELSE 0 END), 0) AS expenseAmount,
               COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS incomeAmount
        FROM bill
        WHERE accountId = :accountId
          AND billTime >= :startInclusive
          AND billTime < :endExclusive
        """,
    )
    fun observeAccountPeriodSummary(
        accountId: Long,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<PeriodAmountSummary>

    @Query(
        """
        SELECT * FROM bill
        WHERE accountId = :accountId
        ORDER BY billTime DESC, id DESC
        LIMIT :limit
        """,
    )
    fun observeRecentByAccount(accountId: Long, limit: Int): Flow<List<Bill>>

    /**
     * 一个账户在区间内的账单。含开始，不含结束。
     * 首页用它取最近 7 个自然日，更早的账单不进这张列表。
     */
    @Query(
        """
        SELECT * FROM bill
        WHERE accountId = :accountId
          AND billTime >= :startInclusive
          AND billTime < :endExclusive
        ORDER BY billTime DESC, id DESC
        """,
    )
    fun observeAccountBetween(
        accountId: Long,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<Bill>>

    /**
     * 区间转账。库存里转出为负、转入为正，这里加总后都变成正数。
     */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 2 AND amount < 0 THEN -amount ELSE 0 END), 0) AS outAmount,
               COALESCE(SUM(CASE WHEN type = 2 AND amount > 0 THEN amount ELSE 0 END), 0) AS inAmount
        FROM bill
        WHERE billTime >= :startInclusive AND billTime < :endExclusive
        """,
    )
    fun observeTransferSummary(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<TransferAmountSummary>

    /**
     * 一个周期里的全部支出，给总预算用。
     * 走 `(type, billTime)`，不含收入和转账。
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM bill
        WHERE type = 0
          AND billTime >= :startInclusive
          AND billTime < :endExclusive
        """,
    )
    suspend fun sumExpense(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Long

    /** 一个周期里某个分类的支出，给分类预算用。走 `(type, categoryId, billTime)`。 */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM bill
        WHERE type = 0
          AND categoryId = :categoryId
          AND billTime >= :startInclusive
          AND billTime < :endExclusive
        """,
    )
    suspend fun sumCategoryExpense(
        categoryId: Long,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Long

    /** 汇率换算按主键向后翻页，一次只取一批金额。 */
    @Query(
        """
        SELECT id, amount FROM bill
        WHERE id > :afterId
        ORDER BY id ASC
        LIMIT :limit
        """,
    )
    suspend fun amountsAfter(afterId: Long, limit: Int): List<IdAmount>

    @Query("UPDATE bill SET amount = :amount, updateTime = :updateTime WHERE id = :id")
    suspend fun updateAmount(id: Long, amount: Long, updateTime: LocalDateTime)
}
