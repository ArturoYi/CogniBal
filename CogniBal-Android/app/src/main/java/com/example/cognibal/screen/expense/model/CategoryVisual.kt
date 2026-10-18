package com.example.cognibal.screen.expense.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * 分类图标键。默认分类写在 [com.example.cognibal.data.repository.DefaultCategories]，
 * 用户以后自己建的分类如果用了别的键，就画成通用分类图标。
 */
internal fun categoryIcon(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_bus" -> Icons.Rounded.DirectionsBus
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "home" -> Icons.Rounded.Home
    "movie" -> Icons.Rounded.Movie
    "medical_services" -> Icons.Rounded.MedicalServices
    "school" -> Icons.Rounded.School
    "more_horiz" -> Icons.Rounded.MoreHoriz
    "payments" -> Icons.Rounded.Payments
    "emoji_events" -> Icons.Rounded.EmojiEvents
    "savings" -> Icons.Rounded.Savings
    else -> Icons.Rounded.Category
}

/** 新建分类时可选的图标键，和默认分类用的是同一套。 */
internal val newCategoryIconKeys: List<String> = listOf(
    "restaurant",
    "directions_bus",
    "shopping_bag",
    "home",
    "movie",
    "medical_services",
    "school",
    "payments",
    "emoji_events",
    "savings",
    "more_horiz",
)

internal fun Color.toHexArgb(): String = String.format(Locale.US, "#%08X", toArgb())

/** 浅色分类底上用深色图标，避免白字落在沙金这类浅色上。 */
internal fun Color.onCategory(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.62f) Color(0xFF2C2A26) else Color.White
}
