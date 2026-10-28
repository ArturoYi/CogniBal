package com.example.cognibal.screen.feature.expense.config

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.R

/**
 * 添加开支页的文案，以及它在 Index 导航条中间的图标。
 *
 * 界面在 `feature.expense`。和展示无关的计算放在 `feature.expense.tool`，不要写进配置或界面。
 */
internal object AddExpenseConfig {
    @StringRes val titleRes: Int = R.string.add_expense
    @StringRes val backRes: Int = R.string.navigate_back
    val icon: ImageVector = Icons.Outlined.Add
}
