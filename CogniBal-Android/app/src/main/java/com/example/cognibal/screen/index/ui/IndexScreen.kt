package com.example.cognibal.screen.index.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.cognibal.navigation.AddExpense
import com.example.cognibal.navigation.AppNavHost
import com.example.cognibal.navigation.HomeTab
import com.example.cognibal.navigation.rememberAppNavigator
import com.example.cognibal.navigation.rememberAppSceneState
import com.example.cognibal.navigation.showsHomeTab
import com.example.cognibal.screen.index.config.IndexTab
import com.example.cognibal.screen.index.tool.indexNavigationPlacement
import com.example.cognibal.screen.index.tool.navigationConsumedInsets
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.insets.StatusBarProtection
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.scaffold.CogniBalScaffoldInfo
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold
import com.example.cognibal.ui.theme.CogniBalTheme

/**
 * 主页。账单和设置共用白底导航：窄窗口在底部，中间按钮向上凸起；更宽的窗口挪到起始侧。
 *
 * 标签条只在当前场景还能看到这两个首页时出现。记一笔和窄屏上的设置二级页会把它收起。
 * 宽屏列表和详情并排时，设置列表仍在，侧栏留着。
 *
 * 整页底色由 [PageBackgroundHost] 铺在导航下面，底栏凸起旁边的缝跟页面同一块颜色。
 * 滑动中的每一页另外有一层主题纯色，跟着页面走，侧滑时不会透出上一页。
 *
 * 内容槽不把安全区 padding 传给页面，各页面用 `screenSafeDrawing` 或 `listContentPadding`。
 * 导航条自己的背景铺到屏幕边缘，不要再给它套一层 `safeDrawingPadding`。
 */
@Composable
fun IndexScreen(
    modifier: Modifier = Modifier,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
    val placement = navigationSuiteType.indexNavigationPlacement()
    val navigationVisibleState = rememberSaveable { mutableStateOf(true) }
    val navigator = rememberAppNavigator()
    val sceneState = rememberAppSceneState(navigator)
    val showNavigation = navigationVisibleState.value && sceneState.currentScene.showsHomeTab()
    val scaffoldState = rememberNavigationSuiteScaffoldState(
        initialValue = if (showNavigation) {
            NavigationSuiteScaffoldValue.Visible
        } else {
            NavigationSuiteScaffoldValue.Hidden
        },
    )
    // 收起动画还没结束时，条仍占着系统栏，内容先不要把这块补回来。
    val navigationOccupiesInsets =
        scaffoldState.currentValue != NavigationSuiteScaffoldValue.Hidden || scaffoldState.isAnimating
    val scaffoldInfo = CogniBalScaffoldInfo(
        windowAdaptiveInfo = windowAdaptiveInfo,
        navigationSuiteType = navigationSuiteType,
        navigationConsumedInsets = navigationConsumedInsets(
            type = navigationSuiteType,
            navigationVisible = navigationOccupiesInsets,
        ),
        navigationVisibleState = navigationVisibleState,
    )
    LaunchedEffect(showNavigation) {
        if (showNavigation) {
            scaffoldState.show()
        } else {
            scaffoldState.hide()
        }
    }

    CompositionLocalProvider(
        LocalCogniBalScaffold provides scaffoldInfo,
    ) {
        PageBackgroundHost(modifier = modifier) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    IndexNavigationBar(
                        placement = placement,
                        selected = navigator.homeTab.toIndexTab(),
                        onSelect = { navigator.selectHome(it.toHomeTab()) },
                        onAddExpense = { navigator.navigate(AddExpense()) },
                    )
                },
                navigationSuiteType = navigationSuiteType,
                state = scaffoldState,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(scaffoldInfo.navigationConsumedInsets),
                ) {
                    AppNavHost(
                        sceneState = sceneState,
                        onBack = navigator::back,
                        modifier = Modifier.fillMaxSize(),
                    )
                    StatusBarProtection()
                }
            }
        }
    }
}

private fun IndexTab.toHomeTab(): HomeTab = when (this) {
    IndexTab.Bill -> HomeTab.Bill
    IndexTab.Settings -> HomeTab.Settings
}

private fun HomeTab.toIndexTab(): IndexTab = when (this) {
    HomeTab.Bill -> IndexTab.Bill
    HomeTab.Settings -> IndexTab.Settings
}

@FormFactorPreviews
@Composable
private fun IndexScreenPreview() {
    CogniBalTheme {
        IndexScreen()
    }
}
