package com.cyr.cognobal.data.repository

import androidx.room.withTransaction
import com.cyr.cognobal.data.database.AppDatabase
import com.cyr.cognobal.data.database.LedgerTime
import com.cyr.cognobal.data.database.dao.AccountDao
import com.cyr.cognobal.data.database.dao.BillDao
import com.cyr.cognobal.data.database.dao.BudgetDao
import com.cyr.cognobal.data.database.dao.LedgerStateDao
import com.cyr.cognobal.data.database.entity.LedgerState
import com.cyr.cognobal.data.database.query.IdAmount
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first

/**
 * 按同一个汇率改写账本里的金额。
 *
 * 账单、账户余额、预算上限在一个事务里分批改。余额是账单加减后的缓存，
 * 只改账单会让余额和流水对不上，所以一起乘。
 * 每一批只拿主键和金额，不把整张表读进内存。任何一笔超出 Long 范围，事务全部回滚。
 * 目标货币和金额写在同一次提交里。偏好在提交成功后再改。
 */
@Singleton
class CurrencyConversionRepository @Inject constructor(
    private val database: AppDatabase,
    private val billDao: BillDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val ledgerStateDao: LedgerStateDao,
) {
    suspend fun apply(rate: BigDecimal, currencyCode: String) = onIo {
        require(rate.signum() > 0) { "汇率必须大于 0" }
        require(currencyCode.isNotBlank()) { "货币不能为空" }
        database.withTransaction {
            val updatedAt = LedgerTime.now()
            rewrite(rate, updatedAt, billDao::amountsAfter, billDao::updateAmount)
            rewrite(rate, updatedAt, accountDao::balancesAfter, accountDao::updateBalance)
            rewrite(rate, updatedAt, budgetDao::limitsAfter, budgetDao::updateLimit)
            ledgerStateDao.upsert(LedgerState(id = LedgerState.ROW_ID, currency = currencyCode))
        }
    }

    private suspend fun rewrite(
        rate: BigDecimal,
        updatedAt: LocalDateTime,
        page: suspend (Long, Int) -> List<IdAmount>,
        write: suspend (Long, Long, LocalDateTime) -> Unit,
    ) {
        var afterId = 0L
        while (true) {
            coroutineContext.ensureActive()
            val rows = page(afterId, BATCH_SIZE)
            if (rows.isEmpty()) return
            for (row in rows) {
                val converted = ExchangeRate.convertMinorUnits(row.amount, rate)
                if (converted != row.amount) {
                    write(row.id, converted, updatedAt)
                }
            }
            val lastId = rows.last().id
            check(lastId > afterId) { "换算分页没有前进" }
            afterId = lastId
            if (rows.size < BATCH_SIZE) return
        }
    }

    private companion object {
        const val BATCH_SIZE: Int = 100
    }
}

/**
 * 库里已经记下换算后的货币，而偏好还是旧符号时，把偏好改成库里的值。
 * 还没有这一行，说明没换过币，偏好继续作准。
 */
internal suspend fun alignCurrencyPreference(
    dao: LedgerStateDao,
    preferences: AppPreferencesRepository,
) {
    val stored = dao.get(LedgerState.ROW_ID)?.currency ?: return
    val match = CurrencyPreference.entries.firstOrNull { it.name == stored } ?: return
    if (preferences.preferences.first().currency == match) return
    preferences.setCurrencyOrThrow(match)
}
