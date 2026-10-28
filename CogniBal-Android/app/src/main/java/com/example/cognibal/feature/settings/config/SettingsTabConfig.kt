package com.example.cognibal.screen.feature.settings.config

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.R

/**
 * 设置在 Index 导航条上的文案和图标。
 *
 * 界面在 `feature.settings`。和展示无关的计算放在 `feature.settings.tool`，不要写进配置或界面。
 */
internal object SettingsTabConfig {
    @StringRes val labelRes: Int = R.string.tab_settings
    val icon: ImageVector = Icons.Outlined.Settings
}
