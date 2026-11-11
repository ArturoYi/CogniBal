package com.cyr.cognobal.screen.feature.home

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.bill.config.BillTabConfig
import com.cyr.cognobal.screen.feature.settings.config.SettingsTabConfig

/**
 * 底部导航四个主标签：首页、统计、资产、设置。
 * 选中态采用实心字形，未选中态采用线性字形，与中央圆形加号形成对称五格节奏。
 */
internal enum class IndexTab(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Bill(
        labelRes = BillTabConfig.labelRes,
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
    ),
    Statistics(
        labelRes = R.string.shortcut_statistics,
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart,
    ),
    Assets(
        labelRes = R.string.shortcut_assets,
        icon = Icons.Outlined.AccountBalanceWallet,
        selectedIcon = Icons.Filled.AccountBalanceWallet,
    ),
    Settings(
        labelRes = SettingsTabConfig.labelRes,
        icon = Icons.Outlined.Tune,
        selectedIcon = Icons.Filled.Tune,
    ),
}
