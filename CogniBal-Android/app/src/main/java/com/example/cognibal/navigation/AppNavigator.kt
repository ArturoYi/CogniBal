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
 * 记一笔和设置二级页都压在当前标签的栈上。界面调用 [selectHome]、[navigate]、[openEntry] 和 [back]。
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

    /**
     * 从桌面快捷方式记一笔。先回到账单栈的首页，再打开对应类型，避免叠在设置页或上一张表单上。
     */
    fun openEntry(typeCode: Int) {
        selectHome(HomeTab.Bill)
        val entry = AddExpense(typeCode)
        if (billStack.lastOrNull() == entry) return
        while (billStack.size > 1) {
            billStack.removeLastOrNull()
        }
        billStack.add(entry)
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
internal fun rememberAppNavigator(initialEntryTypeCode: Int? = null): AppNavigator {
    // 只取第一次的快捷方式。消费掉之后不要重建返回栈，否则记一笔页会被拆掉。
    val entryCode = remember { initialEntryTypeCode }
    val billStack = rememberNavBackStack(
        *if (entryCode == null) {
            arrayOf<NavKey>(Bill)
        } else {
            arrayOf<NavKey>(Bill, AddExpense(entryCode))
        },
    )
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
