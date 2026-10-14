package com.example.cognibal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * 应用唯一的返回栈。
 *
 * 记在主页组合里，切换底部标签时栈还在。界面只调用 [navigate] 和 [back]，不自己持有栈。
 */
internal class AppNavigator(
    val backStack: NavBackStack<NavKey>,
) {
    fun navigate(key: NavKey) {
        if (backStack.lastOrNull() == key) return
        if (backStack.lastOrNull() in settingsDetailKeys) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    fun back() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}

@Composable
internal fun rememberAppNavigator(): AppNavigator {
    val backStack = rememberNavBackStack(SettingsList)
    return remember(backStack) { AppNavigator(backStack) }
}
