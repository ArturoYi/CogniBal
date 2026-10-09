package com.example.cognibal.index.config

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.bill.config.BillTabConfig
import com.example.cognibal.settings.config.SettingsTabConfig

/**
 * Index 的子页。声明顺序就是导航条从左到右、从上到下的顺序。
 *
 * 文案和图标来自各子页自己的 config。新增子页时在这里加一项，再在 Index 界面里接上对应页面。
 */
internal enum class IndexTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Bill(BillTabConfig.labelRes, BillTabConfig.icon),
    Settings(SettingsTabConfig.labelRes, SettingsTabConfig.icon),
}
