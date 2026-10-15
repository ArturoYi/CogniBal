package com.example.cognibal.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用路由表里的目的地。
 *
 * 新增页面时在这里加一个可序列化的键，再在 [appRoutes] 里登记对应界面。
 * 键必须直接实现 [NavKey]，返回栈才能在旋转后按类型还原。
 */

/** 账单。账单栈的起点，也是首页标签。 */
@Serializable
internal data object Bill : NavKey

/** 记一笔。从首页中间的加号进入，不占首页标签。 */
@Serializable
internal data object AddExpense : NavKey

/** 设置列表。设置栈的起点，也是首页标签。 */
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

/**
 * 首页两个标签。各自有一条返回栈。
 *
 * 底栏和侧栏只在当前场景里还能看到其中一个时出现。
 */
internal enum class HomeTab {
    Bill,
    Settings,
}

internal fun HomeTab.route(): NavKey = when (this) {
    HomeTab.Bill -> Bill
    HomeTab.Settings -> SettingsList
}

/**
 * 写进首页目的地的 metadata。场景算完之后靠它判断标签条该不该留。
 *
 * 不拿返回栈顶来判断：宽屏列表-详情并排时，栈顶是二级页，列表窗格仍是首页。
 */
internal const val HomeTabMetadata: String = "cognibal.homeTab"

internal fun homeTabMetadata(): Map<String, Any> = mapOf(HomeTabMetadata to true)

/** 从设置列表打开的二级页。再点另一项时替换当前页，不叠一层。 */
internal val settingsDetailKeys: Set<NavKey> = setOf(
    AppearanceSettings,
    BackupSettings,
    SecuritySettings,
)
