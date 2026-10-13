package com.example.cognibal.data.repository

import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.query.CategoryAmountSummary
import com.example.cognibal.data.database.query.DayAmountSummary
import com.example.cognibal.data.database.query.MonthTrendSummary
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.data.database.query.TransferAmountSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 日历和报表。全部是账单表上的 GROUP BY，账单一变 Flow 会重新发出。
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

    fun observeTransfers(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<TransferAmountSummary> = billDao.observeTransferSummary(
        LedgerTime.normalize(startInclusive),
        LedgerTime.normalize(endExclusive),
    )
}
