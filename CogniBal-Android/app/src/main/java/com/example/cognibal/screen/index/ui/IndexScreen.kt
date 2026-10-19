package com.example.cognibal.screen.index.ui

import androidx.compose.foundation.layout.Box
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
import com.example.cognibal.screen.index.config.IndexTab
import com.example.cognibal.screen.index.tool.IndexNavigationPlacement
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
 * 标签条收起或出现时，内容区尺寸只变一次。侧滑过程中这个尺寸保持不变，两页不用每帧重测。
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
    // 侧滑期间保持原来的内容区尺寸。动画结束后再收起或放回标签条，避免两页每帧重测。
    var navigationShown by rememberSaveable { mutableStateOf(navigationWanted) }
    LaunchedEffect(navigationWanted) {
        if (navigationWanted == navigationShown) return@LaunchedEffect
        delay(PageSlideDurationMillis.toLong())
        navigationShown = navigationWanted
    }
    val scaffoldInfo = CogniBalScaffoldInfo(
        windowAdaptiveInfo = windowAdaptiveInfo,
        navigationSuiteType = navigationSuiteType,
        navigationConsumedInsets = navigationConsumedInsets(
            type = navigationSuiteType,
            navigationVisible = navigationShown,
        ),
        navigationVisibleState = navigationVisibleState,
    )

    CompositionLocalProvider(
        LocalCogniBalScaffold provides scaffoldInfo,
    ) {
        PageBackgroundHost(modifier = modifier) {
            IndexScaffold(
                navigationVisible = navigationShown,
                side = placement == IndexNavigationPlacement.Side,
                navigationSuite = {
                    IndexNavigationBar(
                        placement = placement,
                        selected = navigator.homeTab.toIndexTab(),
                        onSelect = { navigator.selectHome(it.toHomeTab()) },
                        onAddExpense = { navigator.navigate(AddExpense()) },
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
 * 导航和内容分成两格。导航可见时，内容区让出导航测到的宽或高；收起时内容铺满。
 * 同一次显隐只测量一轮，避免和页面侧滑抢同一段时间。
 */
@Composable
private fun IndexScaffold(
    navigationVisible: Boolean,
    side: Boolean,
    navigationSuite: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(Modifier.layoutId(NavigationLayoutId)) {
                if (navigationVisible) navigationSuite()
            }
            Box(Modifier.layoutId(ContentLayoutId), content = { content() })
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val navigation = measurables.first { it.layoutId == NavigationLayoutId }.measure(loose)
        val reservedWidth = if (navigationVisible && side) navigation.width else 0
        val reservedHeight = if (navigationVisible && !side) navigation.height else 0
        val page = measurables.first { it.layoutId == ContentLayoutId }.measure(
            constraints.copy(
                minWidth = (constraints.maxWidth - reservedWidth).coerceAtLeast(0),
                maxWidth = (constraints.maxWidth - reservedWidth).coerceAtLeast(0),
                minHeight = (constraints.maxHeight - reservedHeight).coerceAtLeast(0),
                maxHeight = (constraints.maxHeight - reservedHeight).coerceAtLeast(0),
            ),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (side) {
                page.placeRelative(reservedWidth, 0)
                if (navigationVisible) navigation.placeRelative(0, 0)
            } else {
                page.placeRelative(0, 0)
                if (navigationVisible) {
                    navigation.placeRelative(0, constraints.maxHeight - navigation.height)
                }
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
