package com.cyr.cognobal.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本已经换算到的货币。
 *
 * 换币种时，金额改写和这一行在同一个事务里提交。
 * 没有这一行，表示还没做过换算，货币仍以偏好里的值为准。
 * 进程在金额提交之后、偏好写上之前被杀掉时，下次打开库用这一行把偏好改回来。
 */
@Entity(tableName = "ledger_state")
data class LedgerState(
    @PrimaryKey val id: Int,
    /** [com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference] 的枚举名。 */
    val currency: String,
) {
    companion object {
        const val ROW_ID: Int = 1
    }
}
