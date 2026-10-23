package com.example.cognibal.data.repository

import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.AccountDao
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.dao.CategoryDao
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.CategoryType
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 账单写入。支出、收入各一条；转账在一个事务里写两条，并同步改两个账户的余额。
 *
 * 支出的金额或发生时间一变，预算已花费会按账单重新合计，这里不写预算表。
 * 转账不增加字段。两条腿的 [Bill.createTime] 相同，金额一负一正，分类都是 null。
 * 修改或删除任意一条时，按创建时间和相反金额找到另一条，一起处理。
 * 同一毫秒里如果已经有转账，创建时间会往后再推 1 毫秒，避免两笔转账配错对。
 */
@Singleton
class BillRepository @Inject constructor(
    private val database: AppDatabase,
    private val billDao: BillDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {
    fun observeAll(): Flow<List<Bill>> = billDao.observeAll()

    fun observeBetween(
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): Flow<List<Bill>> = billDao.observeBetween(
        LedgerTime.normalize(startInclusive),
        LedgerTime.normalize(endExclusive),
    )

    fun observeByAccount(accountId: Long): Flow<List<Bill>> = billDao.observeByAccount(accountId)

    fun observeRecent(accountId: Long, limit: Int): Flow<List<Bill>> =
        billDao.observeRecentByAccount(accountId, limit)

    fun observeById(id: Long): Flow<Bill?> = billDao.observeById(id)

    suspend fun insert(draft: EntryDraft): Long = onIo {
        database.withTransaction {
            val now = LedgerTime.now()
            val account = usableAccount(draft.accountId)
            val category = usableCategory(draft.categoryId, draft.type.toCategoryType())
            LedgerText.requirePositiveAmount(draft.amount)
            val bill = Bill(
                amount = draft.amount,
                type = draft.type.toBillType(),
                categoryId = category.id,
                accountId = account.id,
                billTime = LedgerTime.normalize(draft.billTime),
                remark = LedgerText.cleanRemark(draft.remark),
                createTime = now,
                updateTime = now,
            )
            val id = billDao.insert(bill)
            applyDelta(account.id, bill.balanceDelta(), now)
            id
        }
    }

    suspend fun update(id: Long, draft: EntryDraft) = onIo {
        database.withTransaction {
            val existing = billDao.getById(id) ?: throw LedgerException(LedgerReject.BillNotFound)
            if (existing.type == BillType.Transfer) throw LedgerException(LedgerReject.UseTransferApi)
            LedgerText.requirePositiveAmount(draft.amount)
            val now = LedgerTime.now()
            val nextAccount = accountForEdit(existing.accountId, draft.accountId)
            val nextCategory = categoryForEdit(existing.categoryId, draft.categoryId, draft.type.toCategoryType())
            val updated = existing.copy(
                amount = draft.amount,
                type = draft.type.toBillType(),
                categoryId = nextCategory.id,
                accountId = nextAccount.id,
                billTime = LedgerTime.normalize(draft.billTime),
                remark = LedgerText.cleanRemark(draft.remark),
                updateTime = now,
            )
            applyDelta(existing.accountId, negate(existing.balanceDelta()), now)
            billDao.update(updated)
            applyDelta(updated.accountId, updated.balanceDelta(), now)
        }
    }

    suspend fun insertTransfer(draft: TransferDraft): TransferIds = onIo {
        database.withTransaction {
            LedgerText.requirePositiveAmount(draft.amount)
            if (draft.fromAccountId == draft.toAccountId) {
                throw LedgerException(LedgerReject.SameTransferAccount)
            }
            val from = usableAccount(draft.fromAccountId)
            val to = usableAccount(draft.toAccountId)
            val now = uniqueTransferTime(LedgerTime.now())
            val billTime = LedgerTime.normalize(draft.billTime)
            val remark = LedgerText.cleanRemark(draft.remark)
            val outId = billDao.insert(
                transferLeg(
                    amount = -draft.amount,
                    accountId = from.id,
                    billTime = billTime,
                    remark = remark,
                    created = now,
                ),
            )
            val inId = billDao.insert(
                transferLeg(
                    amount = draft.amount,
                    accountId = to.id,
                    billTime = billTime,
                    remark = remark,
                    created = now,
                ),
            )
            applyDelta(from.id, -draft.amount, now)
            applyDelta(to.id, draft.amount, now)
            TransferIds(outBillId = outId, inBillId = inId)
        }
    }

    /** [eitherBillId] 传两条腿里的任意一条。两条会一起改，创建时间保持不变，配对关系才还在。 */
    suspend fun updateTransfer(eitherBillId: Long, draft: TransferDraft) = onIo {
        database.withTransaction {
            LedgerText.requirePositiveAmount(draft.amount)
            if (draft.fromAccountId == draft.toAccountId) {
                throw LedgerException(LedgerReject.SameTransferAccount)
            }
            val leg = billDao.getById(eitherBillId) ?: throw LedgerException(LedgerReject.BillNotFound)
            if (leg.type != BillType.Transfer) throw LedgerException(LedgerReject.NotTransfer)
            val sibling = singleCounterpart(leg) ?: throw LedgerException(LedgerReject.TransferBroken)
            val out = if (leg.amount < 0) leg else sibling
            val incoming = if (leg.amount > 0) leg else sibling
            if (out.amount >= 0 || incoming.amount <= 0) {
                throw LedgerException(LedgerReject.TransferBroken)
            }
            val now = LedgerTime.now()
            val from = accountForEdit(out.accountId, draft.fromAccountId)
            val to = accountForEdit(incoming.accountId, draft.toAccountId)
            val billTime = LedgerTime.normalize(draft.billTime)
            val remark = LedgerText.cleanRemark(draft.remark)
            applyDelta(out.accountId, negate(out.balanceDelta()), now)
            applyDelta(incoming.accountId, negate(incoming.balanceDelta()), now)
            val updatedOut = out.copy(
                amount = -draft.amount,
                accountId = from.id,
                billTime = billTime,
                remark = remark,
                updateTime = now,
            )
            val updatedIn = incoming.copy(
                amount = draft.amount,
                accountId = to.id,
                billTime = billTime,
                remark = remark,
                updateTime = now,
            )
            billDao.update(updatedOut)
            billDao.update(updatedIn)
            applyDelta(updatedOut.accountId, updatedOut.balanceDelta(), now)
            applyDelta(updatedIn.accountId, updatedIn.balanceDelta(), now)
        }
    }

    /**
     * 删除支出或收入时把余额加回去。删除转账的任意一条时，另一条一起删。
     * 另一条已经不在了，就只清掉这一条，方便把写坏的数据修掉。
     */
    suspend fun delete(id: Long) = onIo {
        database.withTransaction {
            val bill = billDao.getById(id) ?: throw LedgerException(LedgerReject.BillNotFound)
            val now = LedgerTime.now()
            if (bill.type == BillType.Transfer) {
                val sibling = singleCounterpart(bill)
                reverseAndDelete(bill, now)
                if (sibling != null) reverseAndDelete(sibling, now)
            } else {
                reverseAndDelete(bill, now)
            }
        }
    }

    private suspend fun reverseAndDelete(bill: Bill, now: LocalDateTime) {
        applyDelta(bill.accountId, negate(bill.balanceDelta()), now)
        billDao.deleteById(bill.id)
    }

    private suspend fun usableAccount(id: Long): Account {
        val account = accountDao.getById(id) ?: throw LedgerException(LedgerReject.AccountNotFound)
        if (!account.isEnable) throw LedgerException(LedgerReject.AccountDisabled)
        return account
    }

    /** 还停在原账户上时，允许账户已经停用，这样历史账还能改备注。换到一个停用账户上则拒绝。 */
    private suspend fun accountForEdit(previousId: Long, nextId: Long): Account {
        val account = accountDao.getById(nextId) ?: throw LedgerException(LedgerReject.AccountNotFound)
        if (nextId != previousId && !account.isEnable) throw LedgerException(LedgerReject.AccountDisabled)
        return account
    }

    private suspend fun usableCategory(id: Long, expected: CategoryType): Category {
        val category = categoryDao.getById(id) ?: throw LedgerException(LedgerReject.CategoryNotFound)
        if (!category.isEnable) throw LedgerException(LedgerReject.CategoryDisabled)
        if (category.type != expected) throw LedgerException(LedgerReject.CategoryTypeMismatch)
        return category
    }

    private suspend fun categoryForEdit(
        previousId: Long?,
        nextId: Long,
        expected: CategoryType,
    ): Category {
        val category = categoryDao.getById(nextId) ?: throw LedgerException(LedgerReject.CategoryNotFound)
        if (category.type != expected) throw LedgerException(LedgerReject.CategoryTypeMismatch)
        if (nextId != previousId && !category.isEnable) throw LedgerException(LedgerReject.CategoryDisabled)
        return category
    }

    private suspend fun applyDelta(accountId: Long, delta: Long, now: LocalDateTime) {
        if (delta == 0L) return
        val account = accountDao.getById(accountId) ?: throw LedgerException(LedgerReject.AccountNotFound)
        val next = try {
            Math.addExact(account.balance, delta)
        } catch (_: ArithmeticException) {
            throw LedgerException(LedgerReject.BalanceOverflow)
        }
        accountDao.updateBalance(accountId, next, now)
    }

    private suspend fun uniqueTransferTime(seed: LocalDateTime): LocalDateTime {
        var candidate = seed
        repeat(MAX_TRANSFER_TIME_STEPS) {
            if (billDao.countTransfersAt(BillType.Transfer, candidate) == 0) return candidate
            candidate = candidate.plusNanos(NANOS_PER_MILLI)
        }
        throw LedgerException(LedgerReject.AmbiguousTransfer)
    }

    private suspend fun singleCounterpart(bill: Bill): Bill? {
        val opposite = negate(bill.amount)
        val rows = billDao.findTransferCounterparts(
            transferType = BillType.Transfer,
            createTime = bill.createTime,
            id = bill.id,
            oppositeAmount = opposite,
        )
        return when (rows.size) {
            0 -> null
            1 -> rows[0]
            else -> throw LedgerException(LedgerReject.AmbiguousTransfer)
        }
    }

    private fun transferLeg(
        amount: Long,
        accountId: Long,
        billTime: LocalDateTime,
        remark: String?,
        created: LocalDateTime,
    ): Bill = Bill(
        amount = amount,
        type = BillType.Transfer,
        categoryId = null,
        accountId = accountId,
        billTime = billTime,
        remark = remark,
        createTime = created,
        updateTime = created,
    )

    private fun negate(value: Long): Long = try {
        Math.negateExact(value)
    } catch (_: ArithmeticException) {
        throw LedgerException(LedgerReject.BalanceOverflow)
    }

    private companion object {
        const val MAX_TRANSFER_TIME_STEPS: Int = 1_000
        const val NANOS_PER_MILLI: Long = 1_000_000L
    }
}
