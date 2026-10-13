package com.example.cognibal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.CategoryType
import kotlinx.coroutines.flow.Flow

/** 分类表的读写。支出分类和收入分类能不能挂到某张账单，由仓库判断。 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY type ASC, sort ASC, id ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM category ORDER BY id ASC")
    suspend fun getAll(): List<Category>

    @Query(
        """
        SELECT * FROM category
        WHERE type = :type
        ORDER BY sort ASC, id ASC
        """,
    )
    fun observeByType(type: CategoryType): Flow<List<Category>>

    @Query(
        """
        SELECT * FROM category
        WHERE type = :type AND isEnable = 1
        ORDER BY sort ASC, id ASC
        """,
    )
    fun observeEnabledByType(type: CategoryType): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM category")
    suspend fun deleteAll()
}
