package com.example.cognibal.screen.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cognibal.navigation.PageSlideDurationMillis
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import com.example.cognibal.navigation.AddExpense
import com.example.cognibal.navigation.AppNavHost
import com.example.cognibal.navigation.HomeTab
import com.example.cognibal.navigation.rememberAppNavigator
import com.example.cognibal.navigation.rememberAppSceneState
import com.example.cognibal.navigation.showsHomeTab
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.insets.StatusBarProtection
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.scaffold.CogniBalScaffoldInfo
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold
import com.example.cognibal.ui.theme.CogniBalTheme

/**
 * 主页。账单和设置共用白底导航：窄窗口在底部，中间按钮向上凸起；更宽的窗口挪到起始侧。
 *
 * 窄屏底栏画在页面下面，不占内容槽高度。首页把底色收到栏上方，栏从留白里露出来。
 * 记一笔和设置二级页铺满整屏，滑进来盖住底栏。内容槽高度不变，动画结束不会再把页面撑高。
 * 宽屏列表和详情并排时，设置列表仍在，侧栏留在起始侧并占掉那一截宽度。
 *
 * 整页底色由 [PageBackgroundHost] 铺在导航下面，底栏凸起旁边的缝跟页面同一块颜色。
 * 滑动中的每一页另外有一层主题纯色，跟着页面走，侧滑时不会透出上一页。
 *
 * 侧栏收起或出现时，内容区宽度只变一次。侧滑过程中这个宽度保持不变，两页不用每帧重测。
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
    val navigationWanted = navigationVisibleState.value && sceneState.currentScene.showsHomeTab()
    val showBottomBar = placement != IndexNavigationPlacement.Side && navigationVisibleState.value
    // 侧栏会改内容区宽度。侧滑期间先保持原宽度，动画结束后再收起或放回，避免两页每帧重测。
    // 底栏不占高度，不用等动画。
    var sideRailReserved by rememberSaveable {
        mutableStateOf(placement == IndexNavigationPlacement.Side && navigationWanted)
    }
    LaunchedEffect(navigationWanted, placement) {
        val reserve = placement == IndexNavigationPlacement.Side && navigationWanted
        if (reserve == sideRailReserved) return@LaunchedEffect
        if (placement == IndexNavigationPlacement.Side) {
            delay(PageSlideDurationMillis.toLong())
        }
        sideRailReserved = reserve
    }
    // 停在首页时底栏盖在内容上，中间按钮才点得到。滑进二级页立刻放到页面下面，避免挡住新页。
    // 返回时等侧滑结束再盖上来，离开的那一页底部不会被栏切掉。
    var bottomBarAboveContent by rememberSaveable { mutableStateOf(navigationWanted && showBottomBar) }
    LaunchedEffect(navigationWanted, showBottomBar) {
        val above = navigationWanted && showBottomBar
        if (above == bottomBarAboveContent) return@LaunchedEffect
        if (above) delay(PageSlideDurationMillis.toLong())
        bottomBarAboveContent = above
    }
    val scaffoldInfo = CogniBalScaffoldInfo(
        windowAdaptiveInfo = windowAdaptiveInfo,
        navigationSuiteType = navigationSuiteType,
        navigationConsumedInsets = navigationConsumedInsets(
            type = navigationSuiteType,
            navigationVisible = sideRailReserved,
        ),
        navigationOverlayInsets = if (showBottomBar) {
            bottomNavigationOverlayInsets()
        } else {
            WindowInsets(0, 0, 0, 0)
        },
        navigationVisibleState = navigationVisibleState,
    )

    CompositionLocalProvider(
        LocalCogniBalScaffold provides scaffoldInfo,
    ) {
        PageBackgroundHost(modifier = modifier) {
            IndexScaffold(
                sideRailVisible = sideRailReserved,
                bottomBarVisible = showBottomBar,
                bottomBarAboveContent = bottomBarAboveContent,
                side = placement == IndexNavigationPlacement.Side,
                navigationSuite = {
                    IndexNavigationBar(
                        placement = placement,
                        selected = navigator.homeTab.toIndexTab(),
                        onSelect = { navigator.selectHome(it.toHomeTab()) },
                        onAddExpense = { navigator.navigate(AddExpense()) },
                        interactive = navigationWanted || sideRailReserved,
                    )
                },
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

/**
 * 侧栏可见时，内容区让出它测到的宽度。底栏不让出高度，先画在内容下面。
 * 首页在底栏那一截不铺底色，栏才露出来；二级页铺满，盖住它。
 */
@Composable
private fun IndexScaffold(
    sideRailVisible: Boolean,
    bottomBarVisible: Boolean,
    bottomBarAboveContent: Boolean,
    side: Boolean,
    navigationSuite: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(Modifier.layoutId(NavigationLayoutId)) {
                if (side) {
                    if (sideRailVisible) navigationSuite()
                } else if (bottomBarVisible) {
                    navigationSuite()
                }
            }
            Box(Modifier.layoutId(ContentLayoutId), content = { content() })
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val navigation = measurables.first { it.layoutId == NavigationLayoutId }.measure(loose)
        val reservedWidth = if (sideRailVisible) navigation.width else 0
        val page = measurables.first { it.layoutId == ContentLayoutId }.measure(
            constraints.copy(
                minWidth = (constraints.maxWidth - reservedWidth).coerceAtLeast(0),
                maxWidth = (constraints.maxWidth - reservedWidth).coerceAtLeast(0),
            ),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (side) {
                page.placeRelative(reservedWidth, 0)
                if (sideRailVisible) navigation.placeRelative(0, 0)
            } else {
                // 二级页滑入时栏在页面下面，被整页盖住。停在首页时栏盖到留白上，点击才落得到。
                if (bottomBarVisible) {
                    navigation.placeRelative(
                        x = 0,
                        y = constraints.maxHeight - navigation.height,
                        zIndex = if (bottomBarAboveContent) 1f else 0f,
                    )
                }
                page.placeRelative(0, 0)
            }
        }
    }
}

private const val NavigationLayoutId = "navigation"
private const val ContentLayoutId = "content"

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
