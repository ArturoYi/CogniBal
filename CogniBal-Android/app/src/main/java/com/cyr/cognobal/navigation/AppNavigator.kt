package com.cyr.cognobal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * 四个一级标签（首页、统计、资产、设置）各自一条返回栈，当前标签记在 [homeTab]。
 */
internal class AppNavigator(
    val billStack: NavBackStack<NavKey>,
    val statisticsStack: NavBackStack<NavKey>,
    val assetsStack: NavBackStack<NavKey>,
    val settingsStack: NavBackStack<NavKey>,
    private val homeTabState: MutableState<HomeTab>,
) {
    var homeTab: HomeTab
        get() = homeTabState.value
        private set(value) {
            homeTabState.value = value
        }

    val activeStack: NavBackStack<NavKey>
        get() = when (homeTab) {
            HomeTab.Bill -> billStack
            HomeTab.Statistics -> statisticsStack
            HomeTab.Assets -> assetsStack
            HomeTab.Settings -> settingsStack
        }

    fun selectHome(tab: HomeTab) {
        if (homeTab == tab) {
            val stack = activeStack
            while (stack.size > 1) {
                stack.removeLastOrNull()
            }
        } else {
            homeTab = tab
        }
    }

    /** 从桌面小组件打开首页账本。 */
    fun showBill() {
        homeTab = HomeTab.Bill
        while (billStack.size > 1) {
            billStack.removeLastOrNull()
        }
    }

    /** 从桌面快捷方式记一笔。 */
    fun openEntry(typeCode: Int) {
        homeTab = HomeTab.Bill
        val entry = AddExpense(typeCode)
        if (billStack.lastOrNull() == entry) return
        while (billStack.size > 1) {
            billStack.removeLastOrNull()
        }
        billStack.add(entry)
    }

    fun navigate(key: NavKey) {
        when (key) {
            Statistics -> {
                homeTab = HomeTab.Statistics
                return
            }
            Assets -> {
                homeTab = HomeTab.Assets
                return
            }
            Bill -> {
                homeTab = HomeTab.Bill
                return
            }
            SettingsList -> {
                homeTab = HomeTab.Settings
                return
            }
        }
        val stack = activeStack
        val top = stack.lastOrNull()
        if (top == key) return
        val replacesDetail =
            (homeTab == HomeTab.Settings && key in settingsDetailKeys && top in settingsDetailKeys) ||
                (key is AccountEditor && top is AccountEditor)
        if (replacesDetail) {
            stack[stack.lastIndex] = key
        } else {
            stack.add(key)
        }
    }

    fun back() {
        val stack = activeStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
        }
    }
}

@Composable
internal fun rememberAppNavigator(initialEntryTypeCode: Int? = null): AppNavigator {
    val entryCode = remember { initialEntryTypeCode }
    val billStack = rememberNavBackStack(
        *if (entryCode == null) {
            arrayOf<NavKey>(Bill)
        } else {
            arrayOf<NavKey>(Bill, AddExpense(entryCode))
        },
    )
    val statisticsStack = rememberNavBackStack(Statistics)
    val assetsStack = rememberNavBackStack(Assets)
    val settingsStack = rememberNavBackStack(SettingsList)
    val homeTab = rememberSaveable { mutableStateOf(HomeTab.Bill) }
    return remember(billStack, statisticsStack, assetsStack, settingsStack, homeTab) {
        AppNavigator(
            billStack = billStack,
            statisticsStack = statisticsStack,
            assetsStack = assetsStack,
            settingsStack = settingsStack,
            homeTabState = homeTab,
        )
    }
}
