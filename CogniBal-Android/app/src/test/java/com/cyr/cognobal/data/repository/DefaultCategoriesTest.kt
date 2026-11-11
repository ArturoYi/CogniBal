package com.cyr.cognobal.data.repository

import com.cyr.cognobal.data.database.entity.Category
import com.cyr.cognobal.data.database.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {
    @Test
    fun familySitsBeforeOtherOnBothSides() {
        assertEquals(listOf("孩子", "长辈", "其他"), DefaultCategories.expense.takeLast(3).map { it.name })
        assertEquals(listOf("孩子", "长辈", "其他"), DefaultCategories.income.takeLast(3).map { it.name })
        val expenseChild = DefaultCategories.expense.first { it.name == "孩子" }
        val incomeChild = DefaultCategories.income.first { it.name == "孩子" }
        assertEquals(expenseChild.icon, incomeChild.icon)
        assertEquals(expenseChild.color, incomeChild.color)
        assertEquals("child_care", expenseChild.icon)
        assertEquals("elderly", DefaultCategories.expense.first { it.name == "长辈" }.icon)
    }

    @Test
    fun emptyLedgerIsSeededByTheCaller() {
        assertTrue(DefaultCategories.missingFrom(emptyList()).isEmpty())
    }

    @Test
    fun ledgerThatAlreadySawThePreviousWaveOnlyGainsFamily() {
        val existing = (DefaultCategories.expense + DefaultCategories.income)
            .filter { it.name != "孩子" && it.name != "长辈" }
        assertEquals(
            listOf(
                CategoryType.Expense to "孩子",
                CategoryType.Expense to "长辈",
                CategoryType.Income to "孩子",
                CategoryType.Income to "长辈",
            ),
            DefaultCategories.missingFrom(existing).map { it.type to it.name },
        )
    }

    @Test
    fun deletingOneFamilyCategoryDoesNotBringTheOthersBack() {
        val existing = (DefaultCategories.expense + DefaultCategories.income)
            .filterNot { it.type == CategoryType.Expense && it.name == "长辈" }
        assertTrue(DefaultCategories.missingFrom(existing).isEmpty())
    }

    @Test
    fun ancientLedgerFillsEveryMissingDefaultOnce() {
        val existing = listOf(
            named(CategoryType.Expense, "餐饮"),
            named(CategoryType.Income, "工资"),
            named(CategoryType.Income, "理财"),
        )
        val missing = DefaultCategories.missingFrom(existing)
        assertTrue(missing.any { it.type == CategoryType.Expense && it.name == "社交" })
        assertTrue(missing.any { it.type == CategoryType.Expense && it.name == "孩子" })
        assertTrue(missing.any { it.type == CategoryType.Income && it.name == "副业" })
        assertTrue(missing.none { it.name == "餐饮" || it.name == "工资" })
        assertEquals(missing.size, missing.distinctBy { it.type to it.name }.size)
    }

    private fun named(type: CategoryType, name: String): Category {
        return Category(
            name = name,
            icon = "more_horiz",
            color = "#FF868E96",
            type = type,
            sort = 0,
            isEnable = true,
        )
    }
}
