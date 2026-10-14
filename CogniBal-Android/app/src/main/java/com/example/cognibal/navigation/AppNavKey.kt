package com.example.cognibal.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用路由表里的目的地。
 *
 * 新增页面时在这里加一个可序列化的键，再在 [appRoutes] 里登记对应界面。
 * 键必须直接实现 [NavKey]，返回栈才能在旋转后按类型还原。
 */

/** 设置列表。设置栈的起点。 */
@Serializable
internal data object SettingsList : NavKey

/** 外观设置。 */
@Serializable
internal data object AppearanceSettings : NavKey

/** 备份设置。 */
@Serializable
internal data object BackupSettings : NavKey

/** 安全与隐私。 */
@Serializable
internal data object SecuritySettings : NavKey

/** 从设置列表打开的二级页。再点另一项时替换当前页，不叠一层。 */
internal val settingsDetailKeys: Set<NavKey> = setOf(
    AppearanceSettings,
    BackupSettings,
    SecuritySettings,
)
