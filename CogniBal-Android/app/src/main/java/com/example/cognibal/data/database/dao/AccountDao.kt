package com.example.cognibal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.cognibal.data.database.entity.Account
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

    @Insert
    suspend fun insert(account: Account): Long

    @Insert
    suspend fun insertAll(accounts: List<Account>)

    @Update
    suspend fun update(account: Account)

    /** 只改余额和时间。调用方必须已经在同一个事务里算好新余额。 */
    @Query("UPDATE account SET balance = :balance, updateTime = :updateTime WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long, updateTime: LocalDateTime)

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM account")
    suspend fun deleteAll()
}
