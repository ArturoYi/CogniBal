package com.example.cognibal.screen.feature.expense.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.ui.theme.OpenColor
import java.util.Locale

/**
 * 分类图标键。默认分类写在 [com.example.cognibal.data.repository.DefaultCategories]，
 * 用户以后自己建的分类如果用了别的键，就画成通用分类图标。
 */
internal fun categoryIcon(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Outlined.Restaurant
    "directions_bus" -> Icons.Outlined.DirectionsBus
    "shopping_bag" -> Icons.Outlined.ShoppingBag
    "home" -> Icons.Outlined.Home
    "movie" -> Icons.Outlined.Movie
    "medical_services" -> Icons.Outlined.MedicalServices
    "school" -> Icons.Outlined.School
    "groups" -> Icons.Outlined.Groups
    "chat" -> Icons.Outlined.Chat
    "pets" -> Icons.Outlined.Pets
    "credit_card" -> Icons.Outlined.CreditCard
    "account_balance_wallet" -> Icons.Outlined.AccountBalanceWallet
    "account_balance" -> Icons.Outlined.AccountBalance
    "work" -> Icons.Outlined.Work
    "volunteer_activism" -> Icons.Outlined.VolunteerActivism
    "redeem" -> Icons.Outlined.Redeem
    "more_horiz" -> Icons.Outlined.MoreHoriz
    "payments" -> Icons.Outlined.Payments
    "emoji_events" -> Icons.Outlined.EmojiEvents
    "savings" -> Icons.Outlined.Savings
    else -> Icons.Outlined.Category
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
    "groups",
    "chat",
    "pets",
    "credit_card",
    "account_balance_wallet",
    "account_balance",
    "payments",
    "emoji_events",
    "work",
    "volunteer_activism",
    "redeem",
    "savings",
    "more_horiz",
)

internal fun Color.toHexArgb(): String = String.format(Locale.US, "#%08X", toArgb())

/** 浅色分类底上用深色图标，避免白字落在沙金这类浅色上。 */
internal fun Color.onCategory(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.62f) OpenColor.Gray9 else Color.White
}
