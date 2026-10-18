package com.example.cognibal.data.repository

import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.AccountDao
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.entity.Account
import java.time.LocalDateTime
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

    fun observeBillCount(accountId: Long): Flow<Int> = billDao.observeCountByAccount(accountId)

    fun observeEnabledCount(): Flow<Int> = accountDao.observeEnabledCount()

    /**
     * 启动时调用。没有账户就建一个现金账户，并标成当前展示。
     * 已经有账户时不新增；[rememberedDisplayId] 是旧版偏好里记过的账户，还在就改成当前展示。
     * 判断和写入在同一个事务里，两次启动叠在一起也只会插入一条。
     */
    suspend fun prepareForStartup(account: NewAccount, rememberedDisplayId: Long?) = onIo {
        requireOpeningBalance(account.openingBalance)
        val row = newRow(account, LedgerTime.now(), isDefault = true)
        database.withTransaction {
            if (accountDao.count() == 0) {
                accountDao.insert(row)
                return@withTransaction
            }
            val remembered = rememberedDisplayId?.let { accountDao.getById(it) }
            if (remembered != null) {
                showOnly(remembered.id)
            } else {
                ensureOneDisplayed()
            }
        }
    }

    suspend fun insert(account: NewAccount): Long = onIo {
        requireOpeningBalance(account.openingBalance)
        val row = newRow(account, LedgerTime.now(), isDefault = false)
        database.withTransaction {
            accountDao.insert(row.copy(isDefault = accountDao.count() == 0))
        }
    }

    /** 把首页当前展示切到这个账户。停用的账户也可以展示，只是不能记新账。 */
    suspend fun setDisplayed(id: Long) = onIo {
        database.withTransaction {
            showOnly(id)
        }
    }

    /**
     * 只改名称、图标、颜色、类型和备注。
     * 余额与创建时间留在原来的行上，避免和账单事务互相覆盖。
     */
    suspend fun update(id: Long, draft: AccountDraft) = onIo {
        val updated = database.withTransaction {
            accountDao.updateProfile(
                id = id,
                name = LedgerText.requireName(draft.name),
                icon = LedgerText.requireIcon(draft.icon),
                color = LedgerText.requireColor(draft.color),
                type = draft.type,
                remark = LedgerText.cleanRemark(draft.remark),
                updateTime = LedgerTime.now(),
            )
        }
        if (updated == 0) throw LedgerException(LedgerReject.AccountNotFound)
    }

    /**
     * 停用只改标记，账单和余额都留着。
     * 不能停到一个启用账户都不剩。当前展示的账户可以停用。
     */
    suspend fun setEnabled(id: Long, enabled: Boolean) = onIo {
        database.withTransaction {
            val existing = accountDao.getById(id) ?: throw LedgerException(LedgerReject.AccountNotFound)
            if (existing.isEnable == enabled) return@withTransaction
            if (!enabled && accountDao.countEnabled() <= 1) {
                throw LedgerException(LedgerReject.LastEnabledAccount)
            }
            accountDao.updateEnabled(id, enabled, LedgerTime.now())
        }
    }

    /**
     * 物理删除只给空账户，而且不能是当前展示，也不能删掉最后一个启用账户。
     * 有账单的账户只能停用，历史报表还要靠这些行。
     */
    suspend fun delete(id: Long) = onIo {
        database.withTransaction {
            val existing = accountDao.getById(id) ?: throw LedgerException(LedgerReject.AccountNotFound)
            if (billDao.countByAccount(id) > 0) throw LedgerException(LedgerReject.AccountInUse)
            if (existing.isDefault) throw LedgerException(LedgerReject.DefaultAccountProtected)
            if (existing.isEnable && accountDao.countEnabled() <= 1) {
                throw LedgerException(LedgerReject.LastEnabledAccount)
            }
            accountDao.deleteById(id)
        }
    }

    private suspend fun ensureOneDisplayed() {
        val defaults = accountDao.countDefault()
        val id = when {
            defaults == 1 -> return
            defaults > 1 -> accountDao.firstDefaultId()
            else -> accountDao.fallbackDisplayId()
        } ?: return
        showOnly(id)
    }

    private suspend fun showOnly(id: Long) {
        accountDao.clearDefault()
        if (accountDao.markDefault(id) == 0) throw LedgerException(LedgerReject.AccountNotFound)
    }

    private fun newRow(account: NewAccount, now: LocalDateTime, isDefault: Boolean): Account {
        return Account(
            name = LedgerText.requireName(account.profile.name),
            icon = LedgerText.requireIcon(account.profile.icon),
            color = LedgerText.requireColor(account.profile.color),
            type = account.profile.type,
            balance = account.openingBalance,
            isEnable = true,
            remark = LedgerText.cleanRemark(account.profile.remark),
            createTime = now,
            updateTime = now,
            isDefault = isDefault,
        )
    }

    private fun requireOpeningBalance(balance: Long) {
        if (balance > LedgerLimits.MAX_MINOR_AMOUNT || balance < -LedgerLimits.MAX_MINOR_AMOUNT) {
            throw LedgerException(LedgerReject.InvalidAmount)
        }
    }
}
