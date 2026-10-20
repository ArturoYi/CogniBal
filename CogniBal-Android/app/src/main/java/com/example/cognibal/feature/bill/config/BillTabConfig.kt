package com.example.cognibal.screen.feature.bill.config

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.R

/**
 * 账单在 Index 导航条上的文案和图标。
 *
 * 界面在 `feature.bill`。和展示无关的计算放在 `feature.bill.tool`，不要写进配置或界面。
 */
internal object BillTabConfig {
    @StringRes val labelRes: Int = R.string.tab_bill
    val icon: ImageVector = Icons.AutoMirrored.Rounded.List
}
