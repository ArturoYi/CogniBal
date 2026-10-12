package com.example.cognibal.screen.settings.config

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognibal.R

/**
 * 设置在 Index 导航条上的文案和图标。
 *
 * 界面在 `settings.ui`。和展示无关的计算放在 `settings.tool`，不要写进配置或界面。
 */
internal object SettingsTabConfig {
    @StringRes val labelRes: Int = R.string.tab_settings
    val icon: ImageVector = Icons.Rounded.Settings
}
