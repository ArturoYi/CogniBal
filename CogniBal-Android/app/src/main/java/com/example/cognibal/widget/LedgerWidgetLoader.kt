package com.example.cognibal.widget

import android.content.Context
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.assetAccountOrder
import com.example.cognibal.di.LedgerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** 当前账户本月的收入和支出。没有账户时两个数都是 0。 */
internal object LedgerWidgetLoader {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeContent(context: Context): Flow<LedgerWidgetContent> {
        val app = context.applicationContext
        val entry = EntryPointAccessors.fromApplication(app, LedgerEntryPoint::class.java)
        val preferences = AppPreferencesRepository.create(app)
        return months().flatMapLatest { (start, end) ->
            entry.accountRepository().observeAll().flatMapLatest { accounts ->
                val account = displayedAccount(accounts)
                val period = if (account == null) {
                    flowOf(PeriodAmountSummary(0L, 0L))
                } else {
                    entry.statsRepository().observeAccountPeriod(account.id, start, end)
                }
                combine(preferences.preferences, period) { prefs, summary ->
                    prefs.toWidgetContent(summary)
                }
            }
        }.distinctUntilChanged()
    }

    suspend fun load(context: Context): LedgerWidgetContent = observeContent(context).first()
}

private fun displayedAccount(accounts: List<Account>): Account? {
    val ordered = accounts.sortedWith(assetAccountOrder(Account::isEnable, Account::id))
    return ordered.firstOrNull { it.isDefault } ?: ordered.firstOrNull()
}

private fun months(): Flow<Pair<LocalDateTime, LocalDateTime>> = flow {
    while (true) {
        val month = java.time.YearMonth.now()
        emit(month.atDay(1).atStartOfDay() to month.plusMonths(1).atDay(1).atStartOfDay())
        val next = month.plusMonths(1).atDay(1).atStartOfDay()
        val wait = java.time.Duration.between(LocalDateTime.now(), next).toMillis()
        delay(wait.coerceAtLeast(60_000L).milliseconds)
    }
}
