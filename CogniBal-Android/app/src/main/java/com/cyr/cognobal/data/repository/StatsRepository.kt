package com.cyr.cognobal.data.repository

import com.cyr.cognobal.data.database.LedgerTime
import com.cyr.cognobal.data.database.dao.BillDao
import com.cyr.cognobal.data.database.entity.Bill
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.data.database.query.CategoryAmountSummary
import com.cyr.cognobal.data.database.query.DayAmountSummary
import com.cyr.cognobal.data.database.query.MonthTrendSummary
import com.cyr.cognobal.data.database.query.PeriodAmountSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 日历、报表，以及设置页的记账规模。
 * 全部是账单表上的聚合，账单一变 Flow 会重新发出。
 * 区间都是含开始、不含结束。转账不进入支出和收入。
 */
@Singleton
class StatsRepository @Inject constructor(
    private val billDao: BillDao,
) {
    fun observeDaily(month: YearMonth): Flow<List<DayAmountSummary>> {
        val start = month.atDay(1).atStartOfDay()
        val end = month.plusMonths(1).atDay(1).atStartOfDay()
        return observeDaily(start, end)
    }

    fun observeDaily(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<DayAmountSummary>> = billDao.observeDailySummary(
        LedgerTime.normalize(startInclusive),
        LedgerTime.normalize(endExclusive),
    )

    fun observeDay(day: LocalDate): Flow<List<Bill>> {
        val start = day.atStartOfDay()
        return billDao.observeBetween(
            LedgerTime.normalize(start),
            LedgerTime.normalize(start.plusDays(1)),
        )
    }

    fun observeMonthlyTrend(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<MonthTrendSummary>> = billDao.observeMonthlyTrend(
        LedgerTime.normalize(startInclusive),
        LedgerTime.normalize(endExclusive),
    )

    fun observeCategoryBreakdown(
        type: BillType,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<CategoryAmountSummary>> {
        if (type == BillType.Transfer) throw LedgerException(LedgerReject.CategoryForbidden)
        return billDao.observeCategorySummary(
            type,
            LedgerTime.normalize(startInclusive),
            LedgerTime.normalize(endExclusive),
        )
    }

    fun observePeriod(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<PeriodAmountSummary> = billDao.observePeriodSummary(
        LedgerTime.normalize(startInclusive),
        LedgerTime.normalize(endExclusive),
    )

    fun observeAccountPeriod(
        accountId: Long,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<PeriodAmountSummary> = billDao.observeAccountPeriodSummary(
        accountId,
        LedgerTime.normalize(startInclusive),
        LedgerTime.normalize(endExclusive),
    )

    /**
     * 设置页的记账规模。
     * 笔数把一次转账算成一笔。最早创建日给陪伴天数用；发生时间可以补记到过去，不拿来当开始日。
     */
    fun observeUsage(): Flow<LedgerUsage> = combine(
        billDao.observeEntryCount(),
        billDao.observeEarliestCreateTime(),
    ) { count, earliest ->
        LedgerUsage(
            entryCount = count,
            earliestCreatedOn = earliest.toRecordDate(),
        )
    }
}

/** 账本用了多少。没有账单时笔数是 0，最早创建日是 null。 */
data class LedgerUsage(
    val entryCount: Int,
    val earliestCreatedOn: LocalDate?,
)

private fun String?.toRecordDate(): LocalDate? {
    if (this.isNullOrEmpty()) return null
    return try {
        LedgerTime.decode(this).toLocalDate()
    } catch (_: IllegalArgumentException) {
        null
    }
}
