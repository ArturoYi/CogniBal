package com.example.cognibal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.cognibal.data.database.entity.Budget
import com.example.cognibal.data.database.query.BudgetSpendRow
import kotlinx.coroutines.flow.Flow

/**
 * 预算表，以及“这条预算已经花了多少”的实时查询。
 *
 * 子查询读的是 bill，不把结果写回 budget。账单或预算一变，Room 会重新发出 Flow。
 * `budgetType = 0` 是全局预算；否则只加总同一个分类的支出。
 */
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget ORDER BY periodStart DESC, id DESC")
    fun observeAll(): Flow<List<Budget>>

    @Query("SELECT * FROM budget ORDER BY id ASC")
    suspend fun getAll(): List<Budget>

    @Query("SELECT * FROM budget WHERE isEnable = 1 ORDER BY periodStart DESC, id DESC")
    fun observeEnabled(): Flow<List<Budget>>

    @Query("SELECT * FROM budget WHERE id = :id")
    fun observeById(id: Long): Flow<Budget?>

    @Query("SELECT * FROM budget WHERE id = :id")
    suspend fun getById(id: Long): Budget?

    @Query("SELECT COUNT(*) FROM budget WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Insert
    suspend fun insert(budget: Budget): Long

    @Insert
    suspend fun insertAll(budgets: List<Budget>)

    @Update
    suspend fun update(budget: Budget)

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budget")
    suspend fun deleteAll()

    @Query(
        """
        SELECT budget.id AS budgetId,
               COALESCE((
                   SELECT SUM(bill.amount)
                   FROM bill
                   WHERE bill.type = 0
                     AND bill.billTime >= budget.periodStart
                     AND bill.billTime < budget.periodEnd
                     AND (
                         budget.budgetType = 0
                         OR bill.categoryId = budget.categoryId
                     )
               ), 0) AS spentAmount
        FROM budget
        """,
    )
    fun observeSpent(): Flow<List<BudgetSpendRow>>
}
