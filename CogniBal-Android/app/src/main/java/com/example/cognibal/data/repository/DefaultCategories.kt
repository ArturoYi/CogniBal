package com.example.cognibal.data.repository

import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.CategoryType

/**
 * 首次建账用的分类。图标是键，不是资源 id；颜色是 `#AARRGGBB`。
 * 色值取 Open Color 的第 6、7 级，和账户色同一套。已经入库的分类保持原色。
 */
internal object DefaultCategories {
    val all: List<Category> = listOf(
        item("餐饮", "restaurant", "#FFF03E3E", CategoryType.Expense, 0),
        item("交通", "directions_bus", "#FF0CA678", CategoryType.Expense, 1),
        item("购物", "shopping_bag", "#FFFD7E14", CategoryType.Expense, 2),
        item("居住", "home", "#FF228BE6", CategoryType.Expense, 3),
        item("娱乐", "movie", "#FFBE4BDB", CategoryType.Expense, 4),
        item("医疗", "medical_services", "#FF1098AD", CategoryType.Expense, 5),
        item("教育", "school", "#FF495057", CategoryType.Expense, 6),
        item("其他", "more_horiz", "#FF868E96", CategoryType.Expense, 7),
        item("工资", "payments", "#FF0CA678", CategoryType.Income, 0),
        item("奖金", "emoji_events", "#FFFD7E14", CategoryType.Income, 1),
        item("理财", "savings", "#FF228BE6", CategoryType.Income, 2),
        item("其他", "more_horiz", "#FF868E96", CategoryType.Income, 3),
    )

    private fun item(
        name: String,
        icon: String,
        color: String,
        type: CategoryType,
        sort: Int,
    ): Category = Category(
        name = name,
        icon = icon,
        color = color,
        type = type,
        sort = sort,
        isEnable = true,
    )
}
