package com.example.cognibal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.model.AccountType
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

/** 账户表的读写。余额怎么变由仓库决定，这里只执行语句。 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM account ORDER BY id ASC")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM account ORDER BY id ASC")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM account WHERE isEnable = 1 ORDER BY id ASC")
    fun observeEnabled(): Flow<List<Account>>

    @Query("SELECT * FROM account WHERE id = :id")
    fun observeById(id: Long): Flow<Account?>

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Query("SELECT COUNT(*) FROM account")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM account WHERE isEnable = 1")
    fun observeEnabledCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM account WHERE isEnable = 1")
    suspend fun countEnabled(): Int

    @Query("SELECT COUNT(*) FROM account WHERE is_default = 1")
    suspend fun countDefault(): Int

    @Query("SELECT id FROM account WHERE is_default = 1 ORDER BY id ASC LIMIT 1")
    suspend fun firstDefaultId(): Long?

    @Query("SELECT id FROM account ORDER BY isEnable DESC, id ASC LIMIT 1")
    suspend fun fallbackDisplayId(): Long?

    @Query("UPDATE account SET is_default = 0 WHERE is_default = 1")
    suspend fun clearDefault()

    @Query("UPDATE account SET is_default = 1 WHERE id = :id")
    suspend fun markDefault(id: Long): Int

    @Insert
    suspend fun insert(account: Account): Long

    @Insert
    suspend fun insertAll(accounts: List<Account>)

    /**
     * 只改资料，不写余额。
     * 整行更新会把读到的旧余额盖回去，和同时记的一笔账对冲。
     */
    @Query(
        """
        UPDATE account
        SET name = :name,
            icon = :icon,
            color = :color,
            type = :type,
            remark = :remark,
            updateTime = :updateTime
        WHERE id = :id
        """,
    )
    suspend fun updateProfile(
        id: Long,
        name: String,
        icon: String,
        color: String,
        type: AccountType,
        remark: String?,
        updateTime: LocalDateTime,
    ): Int

    /** 只改停用标记，不写余额。 */
    @Query("UPDATE account SET isEnable = :isEnable, updateTime = :updateTime WHERE id = :id")
    suspend fun updateEnabled(id: Long, isEnable: Boolean, updateTime: LocalDateTime): Int

    /** 只改余额和时间。调用方必须已经在同一个事务里算好新余额。 */
    @Query("UPDATE account SET balance = :balance, updateTime = :updateTime WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long, updateTime: LocalDateTime)

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM account")
    suspend fun deleteAll()
}
