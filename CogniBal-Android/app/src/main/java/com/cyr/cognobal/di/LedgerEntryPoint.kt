package com.cyr.cognobal.di

import com.cyr.cognobal.data.backup.LedgerBackup
import com.cyr.cognobal.data.backup.bluetooth.BackupBluetooth
import com.cyr.cognobal.data.repository.AccountRepository
import com.cyr.cognobal.data.repository.BillRepository
import com.cyr.cognobal.data.repository.CurrencyConversionRepository
import com.cyr.cognobal.data.repository.BudgetRepository
import com.cyr.cognobal.data.repository.CategoryRepository
import com.cyr.cognobal.data.repository.StatsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 给手动创建的 ViewModel 取仓库。
 *
 * 界面沿用设置页的 [androidx.lifecycle.ViewModelProvider.Factory]，不走 Hilt ViewModel。
 * 入口必须是 public：Hilt 生成的代码在另一个包，读不到 internal。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LedgerEntryPoint {
    fun accountRepository(): AccountRepository

    fun billRepository(): BillRepository

    fun budgetRepository(): BudgetRepository

    fun categoryRepository(): CategoryRepository

    fun currencyConversionRepository(): CurrencyConversionRepository

    fun statsRepository(): StatsRepository

    fun ledgerBackup(): LedgerBackup

    fun backupBluetooth(): BackupBluetooth
}
