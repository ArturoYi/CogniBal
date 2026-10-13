package com.example.cognibal.data.repository

import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.CategoryType

/**
 * 首次建账用的分类。图标是键，不是资源 id；颜色是 `#AARRGGBB`。
 * 支出用偏暖的色，收入用主题里的奶油青绿，界面以后可以换成用户自己选的。
 */
internal object DefaultCategories {
    val all: List<Category> = listOf(
        item("餐饮", "restaurant", "#FFD96C6C", CategoryType.Expense, 0),
        item("交通", "directions_bus", "#FF78A898", CategoryType.Expense, 1),
        item("购物", "shopping_bag", "#FFE0A15A", CategoryType.Expense, 2),
        item("居住", "home", "#FF7EA0C4", CategoryType.Expense, 3),
        item("娱乐", "movie", "#FFB98BC4", CategoryType.Expense, 4),
        item("医疗", "medical_services", "#FF6BA8A0", CategoryType.Expense, 5),
        item("教育", "school", "#FF8C7E6A", CategoryType.Expense, 6),
        item("其他", "more_horiz", "#FF8A8A8A", CategoryType.Expense, 7),
        item("工资", "payments", "#FF78A898", CategoryType.Income, 0),
        item("奖金", "emoji_events", "#FFE0A15A", CategoryType.Income, 1),
        item("理财", "savings", "#FF7EA0C4", CategoryType.Income, 2),
        item("其他", "more_horiz", "#FF8A8A8A", CategoryType.Income, 3),
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
