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
     * 准备默认收支分类。
     *
     * 表是空的就写入整套。已经有数据时，只在还没见过这一版新增项时补上缺的，
     * 并把旧的收入「理财」改名为「金融」。用户删掉的默认项不会再填回来。
     * 自建分类留在同类型默认项前面，默认项之间按 [DefaultCategories] 的顺序排。
     */
    suspend fun seedDefaultsIfEmpty() = onIo {
        database.withTransaction {
            if (categoryDao.count() == 0) {
                categoryDao.insertAll(DefaultCategories.all)
                return@withTransaction
            }
            var rows = categoryDao.getAll()
            if (rows.none(DefaultCategories::isIntroduced)) {
                rows = renameLegacyIncomeFinance(rows)
                val missing = DefaultCategories.all.filter { template ->
                    rows.none { it.type == template.type && it.name == template.name }
                }
                if (missing.isNotEmpty()) {
                    categoryDao.insertAll(missing)
                    rows = categoryDao.getAll()
                }
            }
            alignCategoryOrder(rows)
        }
    }

    private suspend fun renameLegacyIncomeFinance(rows: List<Category>): List<Category> {
        val legacy = rows.find { category ->
            category.type == CategoryType.Income && category.name == DefaultCategories.legacyIncomeFinance
        } ?: return rows
        if (rows.any { it.type == CategoryType.Income && it.name == "金融" }) return rows
        val template = DefaultCategories.income.first { it.name == "金融" }
        val renamed = legacy.copy(
            name = template.name,
            icon = template.icon,
            color = template.color,
        )
        categoryDao.update(renamed)
        return rows.map { category -> if (category.id == legacy.id) renamed else category }
    }

    /** 自建分类的 sort 为负，默认分类从 0 起按规范顺序排。顺序已经对上的行不写。 */
    private suspend fun alignCategoryOrder(rows: List<Category>) {
        CategoryType.entries.forEach { type ->
            val ofType = rows.filter { it.type == type }
            val defaults = if (type == CategoryType.Expense) DefaultCategories.expense else DefaultCategories.income
            val matched = mutableSetOf<Long>()
            val orderedDefaults = defaults.mapNotNull { template ->
                ofType.find { it.name == template.name && it.id !in matched }?.also { matched.add(it.id) }
            }
            val custom = ofType
                .filter { it.id !in matched }
                .sortedWith(compareBy<Category> { it.sort }.thenBy { it.id })
            (custom + orderedDefaults).forEachIndexed { index, category ->
                val sort = index - custom.size
                if (category.sort != sort) {
                    categoryDao.update(category.copy(sort = sort))
                }
            }
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
