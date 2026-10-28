package com.example.cognibal.data.repository

import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.CategoryType

/**
 * 首次建账用的分类。图标是键，不是资源 id；颜色是 `#AARRGGBB`。
 * 色值取 Open Color，和账户色同一套。已经入库的分类保持原色，除非是旧版「理财」改名为「金融」。
 *
 * [sort] 按这里的顺序从 0 递增。用户后来新建的分类用更小的 sort，排在这些默认项前面。
 */
internal object DefaultCategories {
    val expense: List<Category> = typed(
        CategoryType.Expense,
        listOf(
            Spec("餐饮", "restaurant", "#FFF03E3E"),
            Spec("交通", "directions_bus", "#FF0CA678"),
            Spec("购物", "shopping_bag", "#FFFD7E14"),
            Spec("居住", "home", "#FF228BE6"),
            Spec("娱乐", "movie", "#FFBE4BDB"),
            Spec("医疗", "medical_services", "#FF1098AD"),
            Spec("教育", "school", "#FF495057"),
            Spec("社交", "groups", "#FFE64980"),
            Spec("通讯", "chat", "#FF4C6EF5"),
            Spec("宠物", "pets", "#FF82C91E"),
            Spec("债务", "credit_card", "#FFD9480F"),
            Spec("借款", "account_balance_wallet", "#FFF59F00"),
            Spec("金融", "account_balance", "#FF7950F2"),
            Spec("其他", "more_horiz", "#FF868E96"),
        ),
    )

    val income: List<Category> = typed(
        CategoryType.Income,
        listOf(
            Spec("工资", "payments", "#FF0CA678"),
            Spec("奖金", "emoji_events", "#FFFD7E14"),
            Spec("金融", "account_balance", "#FF7950F2"),
            Spec("副业", "work", "#FF228BE6"),
            Spec("补贴", "volunteer_activism", "#FF15AABF"),
            Spec("红包", "redeem", "#FFE64980"),
            Spec("其他", "more_horiz", "#FF868E96"),
        ),
    )

    val all: List<Category> = expense + income

    /** 旧默认收入分类。补齐这一版时改名为「金融」，分类 id 不变，已有账单还挂在上面。 */
    const val legacyIncomeFinance: String = "理财"

    /**
     * 这一版才加进默认表的名字。
     * 旧库里一个都还没有时，说明用户没见过这套，缺的可以补上。
     * 已经有其中任意一个，就不再补，避免把用户删掉的默认项填回来。
     */
    val introduced: List<Category> = listOf(
        expense.first { it.name == "社交" },
        expense.first { it.name == "通讯" },
        expense.first { it.name == "宠物" },
        expense.first { it.name == "债务" },
        expense.first { it.name == "借款" },
        expense.first { it.name == "金融" },
        income.first { it.name == "金融" },
        income.first { it.name == "副业" },
        income.first { it.name == "补贴" },
        income.first { it.name == "红包" },
    )

    fun isIntroduced(category: Category): Boolean {
        return introduced.any { it.type == category.type && it.name == category.name }
    }

    private fun typed(type: CategoryType, specs: List<Spec>): List<Category> {
        return specs.mapIndexed { index, spec ->
            Category(
                name = spec.name,
                icon = spec.icon,
                color = spec.color,
                type = type,
                sort = index,
                isEnable = true,
            )
        }
    }

    private data class Spec(
        val name: String,
        val icon: String,
        val color: String,
    )
}
