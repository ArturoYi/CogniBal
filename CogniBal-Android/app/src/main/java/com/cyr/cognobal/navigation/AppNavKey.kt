package com.cyr.cognobal.navigation

import androidx.navigation3.runtime.NavKey
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.screen.feature.legal.LegalDocumentKind
import kotlinx.serialization.Serializable

/**
 * 应用路由表里的目的地。
 */

/** 首页账单栈起点。 */
@Serializable
internal data object Bill : NavKey

/** 记一笔。从底栏中央加号或快捷方式进入。 */
@Serializable
internal data class AddExpense(
    val typeCode: Int = BillType.Expense.code,
    val billId: Long = 0L,
) : NavKey

/** 单笔账单详情。 */
@Serializable
internal data class BillDetail(val billId: Long) : NavKey

/** 当前账户全部账单。 */
@Serializable
internal data class BillHistory(val accountId: Long) : NavKey

/** 新建或编辑资产账户。 */
@Serializable
internal data class AccountEditor(val accountId: Long? = null) : NavKey

/** 设置页起点。 */
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

/** 用户协议、隐私协议或软件许可。 */
@Serializable
internal data class LegalPage(val kind: LegalDocumentKind) : NavKey

/** 更多设置（默认货币）。 */
@Serializable
internal data object MoreSettings : NavKey

/** 统计页起点。 */
@Serializable
internal data object Statistics : NavKey

/** 日历总览。 */
@Serializable
internal data object Overview : NavKey

/** 收支趋势报表。 */
@Serializable
internal data object Reports : NavKey

/** 预算管理。 */
@Serializable
internal data object Budget : NavKey

/** 资产页起点。 */
@Serializable
internal data object Assets : NavKey

/**
 * 底部导航四个主标签，各自维护独立返回栈。
 */
internal enum class HomeTab {
    Bill,
    Statistics,
    Assets,
    Settings,
}

internal fun HomeTab.route(): NavKey = when (this) {
    HomeTab.Bill -> Bill
    HomeTab.Statistics -> Statistics
    HomeTab.Assets -> Assets
    HomeTab.Settings -> SettingsList
}

internal const val HomeTabMetadata: String = "cognibal.homeTab"

internal fun homeTabMetadata(): Map<String, Any> = mapOf(HomeTabMetadata to true)

/** 设置列表打开的二级详情页集合。宽屏列表-详情并排时替换右侧详情窗格。 */
internal val settingsDetailKeys: Set<NavKey> = setOf(
    AppearanceSettings,
    BackupSettings,
    SecuritySettings,
    MoreSettings,
    Overview,
    Reports,
    Budget,
)
