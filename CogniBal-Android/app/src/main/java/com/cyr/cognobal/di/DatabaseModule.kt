package com.cyr.cognobal.di

import android.content.Context
import com.cyr.cognobal.data.database.AppDatabase
import com.cyr.cognobal.data.database.dao.AccountDao
import com.cyr.cognobal.data.database.dao.BillDao
import com.cyr.cognobal.data.database.dao.BudgetDao
import com.cyr.cognobal.data.database.dao.CategoryDao
import com.cyr.cognobal.data.database.dao.LedgerStateDao
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.data.repository.alignCurrencyPreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * 提供账单库和 DAO。仓库用构造注入，不必在这里再声明一遍。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val database = AppDatabase.open(context)
        // 金额已经按库里的货币改过，偏好却还停在旧符号时，这里把符号改回来。
        runBlocking {
            try {
                alignCurrencyPreference(
                    database.ledgerStateDao(),
                    AppPreferencesRepository.create(context),
                )
            } catch (_: Exception) {
                // 这次没写上偏好，下次打开库再试。金额已经在库里。
            }
        }
        return database
    }

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideBillDao(database: AppDatabase): BillDao = database.billDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideLedgerStateDao(database: AppDatabase): LedgerStateDao = database.ledgerStateDao()
}
