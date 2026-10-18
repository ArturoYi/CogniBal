package com.example.cognibal.data.repository

import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.dao.BudgetDao
import com.example.cognibal.data.database.dao.CategoryDao
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.CategoryType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 分类。支出账单只能绑支出分类，收入账单只能绑收入分类，这条规则在 [BillRepository] 执行。
 * 这里负责不让一个已经有账单的分类被改成另一种类型，也不让它被删掉。
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
) {
    fun observeAll(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeByType(type: CategoryType): Flow<List<Category>> = categoryDao.observeByType(type)

    fun observeEnabledByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.observeEnabledByType(type)
    }

    suspend fun insert(draft: CategoryDraft): Long = onIo {
        categoryDao.insert(draft.toRow(isEnable = true))
    }

    /**
     * 新建分类插到同类型的最前面。
     * sort 比当前最小值再小 1，默认分类仍留在后面。
     */
    suspend fun insertInFront(
        name: String,
        icon: String,
        color: String,
        type: CategoryType,
    ): Long = onIo {
        database.withTransaction {
            val currentMin = categoryDao.minSort(type)
            val sort = when (currentMin) {
                null -> 0
                Int.MIN_VALUE -> Int.MIN_VALUE
                else -> currentMin - 1
            }
            categoryDao.insert(
                CategoryDraft(
                    name = name,
                    icon = icon,
                    color = color,
                    type = type,
                    sort = sort,
                ).toRow(isEnable = true),
            )
        }
    }

    suspend fun update(id: Long, draft: CategoryDraft) = onIo {
        database.withTransaction {
            val existing = categoryDao.getById(id) ?: throw LedgerException(LedgerReject.CategoryNotFound)
            if (existing.type != draft.type && billDao.countByCategory(id) > 0) {
                throw LedgerException(LedgerReject.CategoryTypeMismatch)
            }
            if (draft.type != CategoryType.Expense && budgetDao.countByCategory(id) > 0) {
                throw LedgerException(LedgerReject.BudgetCategoryMustBeExpense)
            }
            categoryDao.update(draft.toRow(isEnable = existing.isEnable).copy(id = id))
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = onIo {
        val existing = categoryDao.getById(id) ?: throw LedgerException(LedgerReject.CategoryNotFound)
        if (existing.isEnable == enabled) return@onIo
        categoryDao.update(existing.copy(isEnable = enabled))
    }

    suspend fun delete(id: Long) = onIo {
        database.withTransaction {
            if (categoryDao.getById(id) == null) throw LedgerException(LedgerReject.CategoryNotFound)
            if (billDao.countByCategory(id) > 0 || budgetDao.countByCategory(id) > 0) {
                throw LedgerException(LedgerReject.CategoryInUse)
            }
            categoryDao.deleteById(id)
        }
    }

    /**
     * 分类表是空的时候写入一套默认收支分类。已经有数据就什么都不做，
     * 避免用户删掉默认项之后下次启动又被填回来。
     * 当前没有在应用启动时调用，等界面准备好再接。
     */
    suspend fun seedDefaultsIfEmpty() = onIo {
        database.withTransaction {
            if (categoryDao.count() > 0) return@withTransaction
            categoryDao.insertAll(DefaultCategories.all)
        }
    }
}

private fun CategoryDraft.toRow(isEnable: Boolean): Category {
    return Category(
        name = LedgerText.requireName(name),
        icon = LedgerText.requireIcon(icon),
        color = LedgerText.requireColor(color),
        type = type,
        sort = sort,
        isEnable = isEnable,
    )
}
