package com.cyr.cognobal.screen.feature.expense.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Elderly
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.TakeoutDining
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.cyr.cognobal.R
import com.cyr.cognobal.ui.theme.OpenColor
import java.util.Locale

/**
 * 分类图标。默认分类写在 [com.cyr.cognobal.data.repository.DefaultCategories]。
 * 用户以后自己建的分类如果用了别的键，就画成通用分类图标。
 */
private data class CategoryGlyph(
    val key: String,
    val image: ImageVector,
    @param:StringRes val labelRes: Int,
)

private val glyphs = listOf(
    CategoryGlyph("restaurant", Icons.Outlined.Restaurant, R.string.bill_entry_icon_restaurant),
    CategoryGlyph("coffee", Icons.Outlined.Coffee, R.string.bill_entry_icon_coffee),
    CategoryGlyph("lunch_dining", Icons.Outlined.LunchDining, R.string.bill_entry_icon_meal),
    CategoryGlyph("bakery_dining", Icons.Outlined.BakeryDining, R.string.bill_entry_icon_bakery),
    CategoryGlyph("cake", Icons.Outlined.Cake, R.string.bill_entry_icon_cake),
    CategoryGlyph("local_bar", Icons.Outlined.LocalBar, R.string.bill_entry_icon_drinks),
    CategoryGlyph("takeout_dining", Icons.Outlined.TakeoutDining, R.string.bill_entry_icon_takeout),
    CategoryGlyph("shopping_bag", Icons.Outlined.ShoppingBag, R.string.bill_entry_icon_shopping),
    CategoryGlyph("shopping_cart", Icons.Outlined.ShoppingCart, R.string.bill_entry_icon_cart),
    CategoryGlyph("checkroom", Icons.Outlined.Checkroom, R.string.bill_entry_icon_clothes),
    CategoryGlyph("home", Icons.Outlined.Home, R.string.bill_entry_icon_home),
    CategoryGlyph("apartment", Icons.Outlined.Apartment, R.string.bill_entry_icon_apartment),
    CategoryGlyph("bolt", Icons.Outlined.Bolt, R.string.bill_entry_icon_electricity),
    CategoryGlyph("water_drop", Icons.Outlined.WaterDrop, R.string.bill_entry_icon_water),
    CategoryGlyph("wifi", Icons.Outlined.Wifi, R.string.bill_entry_icon_internet),
    CategoryGlyph("directions_bus", Icons.Outlined.DirectionsBus, R.string.bill_entry_icon_transit),
    CategoryGlyph("directions_car", Icons.Outlined.DirectionsCar, R.string.bill_entry_icon_car),
    CategoryGlyph("local_taxi", Icons.Outlined.LocalTaxi, R.string.bill_entry_icon_taxi),
    CategoryGlyph("train", Icons.Outlined.Train, R.string.bill_entry_icon_train),
    CategoryGlyph("flight", Icons.Outlined.Flight, R.string.bill_entry_icon_flight),
    CategoryGlyph("directions_bike", Icons.AutoMirrored.Outlined.DirectionsBike, R.string.bill_entry_icon_bike),
    CategoryGlyph("local_gas_station", Icons.Outlined.LocalGasStation, R.string.bill_entry_icon_fuel),
    CategoryGlyph("smartphone", Icons.Outlined.Smartphone, R.string.bill_entry_icon_phone),
    CategoryGlyph("laptop", Icons.Outlined.Laptop, R.string.bill_entry_icon_computer),
    CategoryGlyph("headphones", Icons.Outlined.Headphones, R.string.bill_entry_icon_headphones),
    CategoryGlyph("sports_esports", Icons.Outlined.SportsEsports, R.string.bill_entry_icon_games),
    CategoryGlyph("photo_camera", Icons.Outlined.PhotoCamera, R.string.bill_entry_icon_camera),
    CategoryGlyph("movie", Icons.Outlined.Movie, R.string.bill_entry_icon_entertainment),
    CategoryGlyph("music_note", Icons.Outlined.MusicNote, R.string.bill_entry_icon_music),
    CategoryGlyph("sports_soccer", Icons.Outlined.SportsSoccer, R.string.bill_entry_icon_sports),
    CategoryGlyph("groups", Icons.Outlined.Groups, R.string.bill_entry_icon_social),
    CategoryGlyph("family_restroom", Icons.Outlined.FamilyRestroom, R.string.bill_entry_icon_family),
    CategoryGlyph("child_care", Icons.Outlined.ChildCare, R.string.bill_entry_icon_child),
    CategoryGlyph("elderly", Icons.Outlined.Elderly, R.string.bill_entry_icon_elder),
    CategoryGlyph("pets", Icons.Outlined.Pets, R.string.bill_entry_icon_pets),
    CategoryGlyph("chat", Icons.AutoMirrored.Outlined.Chat, R.string.bill_entry_icon_messages),
    CategoryGlyph("favorite", Icons.Outlined.Favorite, R.string.bill_entry_icon_care),
    CategoryGlyph("celebration", Icons.Outlined.Celebration, R.string.bill_entry_icon_party),
    CategoryGlyph("medical_services", Icons.Outlined.MedicalServices, R.string.bill_entry_icon_health),
    CategoryGlyph("medication", Icons.Outlined.Medication, R.string.bill_entry_icon_medicine),
    CategoryGlyph("fitness_center", Icons.Outlined.FitnessCenter, R.string.bill_entry_icon_fitness),
    CategoryGlyph("spa", Icons.Outlined.Spa, R.string.bill_entry_icon_spa),
    CategoryGlyph("school", Icons.Outlined.School, R.string.bill_entry_icon_education),
    CategoryGlyph("menu_book", Icons.AutoMirrored.Outlined.MenuBook, R.string.bill_entry_icon_books),
    CategoryGlyph("park", Icons.Outlined.Park, R.string.bill_entry_icon_outdoors),
    CategoryGlyph("payments", Icons.Outlined.Payments, R.string.bill_entry_icon_pay),
    CategoryGlyph("emoji_events", Icons.Outlined.EmojiEvents, R.string.bill_entry_icon_bonus),
    CategoryGlyph("work", Icons.Outlined.Work, R.string.bill_entry_icon_work),
    CategoryGlyph("volunteer_activism", Icons.Outlined.VolunteerActivism, R.string.bill_entry_icon_benefit),
    CategoryGlyph("savings", Icons.Outlined.Savings, R.string.bill_entry_icon_savings),
    CategoryGlyph("account_balance", Icons.Outlined.AccountBalance, R.string.bill_entry_icon_finance),
    CategoryGlyph("account_balance_wallet", Icons.Outlined.AccountBalanceWallet, R.string.bill_entry_icon_loan),
    CategoryGlyph("credit_card", Icons.Outlined.CreditCard, R.string.bill_entry_icon_debt),
    CategoryGlyph("trending_up", Icons.AutoMirrored.Outlined.TrendingUp, R.string.bill_entry_icon_investing),
    CategoryGlyph("receipt_long", Icons.AutoMirrored.Outlined.ReceiptLong, R.string.bill_entry_icon_receipt),
    CategoryGlyph("redeem", Icons.Outlined.Redeem, R.string.bill_entry_icon_gift),
    CategoryGlyph("card_giftcard", Icons.Outlined.CardGiftcard, R.string.bill_entry_icon_present),
    CategoryGlyph("cleaning_services", Icons.Outlined.CleaningServices, R.string.bill_entry_icon_cleaning),
    CategoryGlyph("handyman", Icons.Outlined.Handyman, R.string.bill_entry_icon_repair),
    CategoryGlyph("more_horiz", Icons.Outlined.MoreHoriz, R.string.bill_entry_icon_other),
)

internal fun categoryIcon(key: String): ImageVector {
    return glyphs.firstOrNull { it.key == key }?.image ?: Icons.Outlined.Category
}

@StringRes
internal fun categoryIconLabel(key: String): Int {
    return glyphs.firstOrNull { it.key == key }?.labelRes ?: R.string.bill_entry_icon_other
}

/** 新建分类时可选的图标键。默认分类用到的都在里面，后面多放了常用图标。 */
internal val newCategoryIconKeys: List<String> = glyphs.map { it.key }

internal fun Color.toHexArgb(): String = String.format(Locale.US, "#%08X", toArgb())

/** 自建分类共用这一支颜色，界面上不再挑选。 */
internal val NewCategoryColor = Color(0xFFA3A3A3)

internal val NewCategoryColorHex: String = NewCategoryColor.toHexArgb()

/** 浅色分类底上用深色图标，避免白字落在沙金这类浅色上。 */
internal fun Color.onCategory(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.62f) OpenColor.Gray9 else Color.White
}
