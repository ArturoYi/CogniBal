package com.cyr.cognobal.data.repository

import com.cyr.cognobal.data.database.entity.Category
import com.cyr.cognobal.data.database.model.CategoryType

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
            Spec("孩子", "child_care", "#FF74C0FC"),
            Spec("长辈", "elderly", "#FF862E9C"),
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
            Spec("孩子", "child_care", "#FF74C0FC"),
            Spec("长辈", "elderly", "#FF862E9C"),
            Spec("其他", "more_horiz", "#FF868E96"),
        ),
    )

    val all: List<Category> = expense + income

    /** 旧默认收入分类。补齐这一版时改名为「金融」，分类 id 不变，已有账单还挂在上面。 */
    const val legacyIncomeFinance: String = "理财"

    /**
     * 上一版才加进默认表的名字，用来判断旧库有没有见过那一套。
     * 一个都还没有时，缺的可以补上。已经有其中任意一个，就不再补，避免把用户删掉的默认项填回来。
     * 孩子和长辈不在这里，见 [family]。
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

    /**
     * 这一版补上的家人分类。支出和收入各有孩子、长辈，图标和颜色两边相同。
     * 不放进 [introduced]：那一组是上一版的标记，混进去会让旧库漏补更早的默认项。
     */
    val family: List<Category> = listOf(
        expense.first { it.name == "孩子" },
        expense.first { it.name == "长辈" },
        income.first { it.name == "孩子" },
        income.first { it.name == "长辈" },
    )

    fun isFamily(category: Category): Boolean {
        return family.any { it.type == category.type && it.name == category.name }
    }

    /**
     * 旧库还缺的默认分类。空表由调用方整套写入，这里返回空。
     * 上一版新增项一个都没有时，补上所有缺的，其中已经包含 [family]。
     * 已经见过上一版、但家人分类一个都没有时，只补这一对。
     * 家人分类里已经有任意一个，就不再补，避免把用户删掉的填回来。
     */
    fun missingFrom(existing: List<Category>): List<Category> {
        if (existing.isEmpty()) return emptyList()
        val templates = when {
            existing.none(::isIntroduced) -> all
            existing.none(::isFamily) -> family
            else -> return emptyList()
        }
        return templates.filter { template ->
            existing.none { it.type == template.type && it.name == template.name }
        }
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
