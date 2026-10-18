package com.example.cognibal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * 首页两个标签各自一条返回栈，当前标签记在 [homeTab]。
 *
 * 记一笔和设置二级页都压在当前标签的栈上。界面只调用 [selectHome]、[navigate] 和 [back]。
 */
internal class AppNavigator(
    val billStack: NavBackStack<NavKey>,
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
            HomeTab.Settings -> settingsStack
        }

    fun selectHome(tab: HomeTab) {
        homeTab = tab
    }

    fun navigate(key: NavKey) {
        val stack = activeStack
        val top = stack.lastOrNull()
        if (top == key) return
        val replacesDetail = (key in settingsDetailKeys && top in settingsDetailKeys) ||
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
internal fun rememberAppNavigator(): AppNavigator {
    val billStack = rememberNavBackStack(Bill)
    val settingsStack = rememberNavBackStack(SettingsList)
    val homeTab = rememberSaveable { mutableStateOf(HomeTab.Bill) }
    return remember(billStack, settingsStack, homeTab) {
        AppNavigator(
            billStack = billStack,
            settingsStack = settingsStack,
            homeTabState = homeTab,
        )
    }
}
