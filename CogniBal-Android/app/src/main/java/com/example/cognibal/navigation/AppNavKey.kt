package com.example.cognibal.navigation

import androidx.navigation3.runtime.NavKey
import com.example.cognibal.data.database.model.BillType
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

/**
 * 记一笔。从首页中间的加号或右下角菜单进入，不占首页标签。
 *
 * [typeCode] 用 [BillType.code]。默认是支出，和底栏中间的加号一致。
 */
@Serializable
internal data class AddExpense(
    val typeCode: Int = BillType.Expense.code,
) : NavKey

/** 当前账户的全部账单。从首页「查看全部」进入，不占首页标签。 */
@Serializable
internal data class BillHistory(val accountId: Long) : NavKey

/**
 * 新建或编辑账户。
 *
 * [accountId] 为空是新建。宽屏时作为账单页的详情窗格，窄屏单独占一页。
 */
@Serializable
internal data class AccountEditor(val accountId: Long? = null) : NavKey

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

/** 统计。按分类看一个月的支出或收入。 */
@Serializable
internal data object Statistics : NavKey

/** 总览。一个月的日历，点开某一天的账单。 */
@Serializable
internal data object Overview : NavKey

/** 报表。近几个月的收支趋势。 */
@Serializable
internal data object Reports : NavKey

/** 预算。本月支出上限，以及以往周期。 */
@Serializable
internal data object Budget : NavKey

/** 资产。全部账户余额拆成净资产、资产和负债。 */
@Serializable
internal data object Assets : NavKey

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
    Statistics,
    Overview,
    Reports,
    Budget,
    Assets,
)
