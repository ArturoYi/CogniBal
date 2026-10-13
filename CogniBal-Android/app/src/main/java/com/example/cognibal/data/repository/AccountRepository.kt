package com.example.cognibal.data.repository

import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.AccountDao
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.entity.Account
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 账户。改名称、图标、停用都不会动余额；余额只在 [BillRepository] 的账单事务里变化。
 */
@Singleton
class AccountRepository @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val billDao: BillDao,
) {
    fun observeAll(): Flow<List<Account>> = accountDao.observeAll()

    fun observeEnabled(): Flow<List<Account>> = accountDao.observeEnabled()

    fun observeById(id: Long): Flow<Account?> = accountDao.observeById(id)

    suspend fun insert(account: NewAccount): Long = onIo {
        val now = LedgerTime.now()
        val row = Account(
            name = LedgerText.requireName(account.profile.name),
            icon = LedgerText.requireIcon(account.profile.icon),
            color = LedgerText.requireColor(account.profile.color),
            type = account.profile.type,
            balance = account.openingBalance,
            isEnable = true,
            remark = LedgerText.cleanRemark(account.profile.remark),
            createTime = now,
            updateTime = now,
        )
        accountDao.insert(row)
    }

    /** 保留原来的余额和创建时间。 */
    suspend fun update(id: Long, draft: AccountDraft) = onIo {
        val existing = accountDao.getById(id) ?: throw LedgerException(LedgerReject.AccountNotFound)
        accountDao.update(
            existing.copy(
                name = LedgerText.requireName(draft.name),
                icon = LedgerText.requireIcon(draft.icon),
                color = LedgerText.requireColor(draft.color),
                type = draft.type,
                remark = LedgerText.cleanRemark(draft.remark),
                updateTime = LedgerTime.now(),
            ),
        )
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = onIo {
        val existing = accountDao.getById(id) ?: throw LedgerException(LedgerReject.AccountNotFound)
        if (existing.isEnable == enabled) return@onIo
        accountDao.update(existing.copy(isEnable = enabled, updateTime = LedgerTime.now()))
    }

    /**
     * 还有账单时不能删。历史账单要留在原来的账户上，余额也才对得上。
     */
    suspend fun delete(id: Long) = onIo {
        database.withTransaction {
            if (accountDao.getById(id) == null) throw LedgerException(LedgerReject.AccountNotFound)
            if (billDao.countByAccount(id) > 0) throw LedgerException(LedgerReject.AccountInUse)
            accountDao.deleteById(id)
        }
    }
}
