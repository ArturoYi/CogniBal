package com.cyr.cognobal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cyr.cognobal.data.database.entity.LedgerState

/** 账本货币。整张表只有一行。 */
@Dao
interface LedgerStateDao {
    @Query("SELECT * FROM ledger_state WHERE id = :id")
    suspend fun get(id: Int): LedgerState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: LedgerState)
}
