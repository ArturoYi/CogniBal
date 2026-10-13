package com.example.cognibal.screen.index.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cognibal.screen.bill.ui.BillScreen
import com.example.cognibal.screen.expense.ui.AddExpenseScreen
import com.example.cognibal.screen.index.config.IndexTab
import com.example.cognibal.screen.index.tool.indexNavigationPlacement
import com.example.cognibal.screen.index.tool.navigationConsumedInsets
import com.example.cognibal.screen.settings.ui.SettingsRoute
import com.example.cognibal.ui.background.LocalPageBackgroundController
import com.example.cognibal.ui.background.PageBackground
import com.example.cognibal.ui.background.PageBackgroundController
import com.example.cognibal.ui.insets.StatusBarProtection
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.scaffold.CogniBalScaffoldInfo
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold
import com.example.cognibal.ui.theme.CogniBalTheme

/**
 * 主页。账单和设置共用白底导航：窄窗口在底部，中间按钮向上凸起；更宽的窗口挪到起始侧。
 *
 * 整页底色由 [PageBackground] 铺在导航下面。各页面用 `RequestPageBackground` 换颜色，
 * 不要在自己的内容里再铺一层，否则底栏凸起旁边的缝会对不上。
 *
 * 内容槽不把安全区 padding 传给页面，各页面用 `screenSafeDrawing` 或 `listContentPadding`。
 * 导航条自己的背景铺到屏幕边缘，不要再给它套一层 `safeDrawingPadding`。
 */
@Composable
fun IndexScreen(
    modifier: Modifier = Modifier,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
    val placement = navigationSuiteType.indexNavigationPlacement()
    val navigationVisibleState = rememberSaveable { mutableStateOf(true) }
    var addingExpense by rememberSaveable { mutableStateOf(false) }
    val showNavigation = navigationVisibleState.value && !addingExpense
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
    var tab by rememberSaveable { mutableStateOf(IndexTab.Bill) }

    LaunchedEffect(showNavigation) {
        if (showNavigation) {
            scaffoldState.show()
        } else {
            scaffoldState.hide()
        }
    }

    val pageBackgroundController = remember { PageBackgroundController() }

    CompositionLocalProvider(
        LocalCogniBalScaffold provides scaffoldInfo,
        LocalPageBackgroundController provides pageBackgroundController,
    ) {
        PageBackground(
            modifier = modifier,
            style = pageBackgroundController.style,
        ) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    IndexNavigationBar(
                        placement = placement,
                        selected = tab,
                        onSelect = { tab = it },
                        onAddExpense = { addingExpense = true },
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
                    if (addingExpense) {
                        BackHandler { addingExpense = false }
                        AddExpenseScreen(
                            onBack = { addingExpense = false },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        when (tab) {
                            IndexTab.Bill -> BillScreen(Modifier.fillMaxSize())
                            IndexTab.Settings -> SettingsRoute(Modifier.fillMaxSize())
                        }
                    }
                    StatusBarProtection()
                }
            }
        }
    }
}

@FormFactorPreviews
@Composable
private fun IndexScreenPreview() {
    CogniBalTheme {
        IndexScreen()
    }
}
