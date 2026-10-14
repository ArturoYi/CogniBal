package com.example.cognibal.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay

/**
 * 全局导航宿主。
 *
 * 宽屏用列表-详情并排，窄屏把场景让给单页，这样微信式滑动只发生在手机上。
 * 不要给宿主再垫安全区，每个目的地自己处理状态栏和导航栏。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AppNavHost(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = navigator::back,
        sceneStrategies = listOf(listDetailStrategy),
        entryProvider = entryProvider { appRoutes(navigator) },
        transitionSpec = { wechatPush() },
        popTransitionSpec = { wechatPop() },
        predictivePopTransitionSpec = { wechatPop() },
    )
}
