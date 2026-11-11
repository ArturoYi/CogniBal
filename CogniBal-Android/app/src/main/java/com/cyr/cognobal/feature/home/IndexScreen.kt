package com.cyr.cognobal.screen.feature.home

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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import com.cyr.cognobal.navigation.AddExpense
import com.cyr.cognobal.navigation.AppNavHost
import com.cyr.cognobal.navigation.HomeTab
import com.cyr.cognobal.navigation.PageSlideDurationMillis
import com.cyr.cognobal.navigation.rememberAppNavigator
import com.cyr.cognobal.navigation.rememberAppSceneState
import com.cyr.cognobal.navigation.showsHomeTab
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.insets.StatusBarProtection
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.privacy.LocalAmountVisibility
import com.cyr.cognobal.ui.privacy.rememberAmountVisibilityState
import com.cyr.cognobal.ui.scaffold.CogniBalScaffoldInfo
import com.cyr.cognobal.ui.scaffold.LocalCogniBalScaffold
import com.cyr.cognobal.ui.theme.CogniBalTheme
import kotlinx.coroutines.delay

@Composable
fun IndexScreen(
    shortcutTypeCode: Int? = null,
    onShortcutConsumed: () -> Unit = {},
    openBillToken: Int = 0,
    onBillOpened: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
    val placement = navigationSuiteType.indexNavigationPlacement()
    val navigationVisibleState = rememberSaveable { mutableStateOf(true) }
    val amountVisibility = rememberAmountVisibilityState()
    val navigator = rememberAppNavigator(shortcutTypeCode)
    val sceneState = rememberAppSceneState(navigator)
    LaunchedEffect(shortcutTypeCode) {
        val typeCode = shortcutTypeCode ?: return@LaunchedEffect
        navigator.openEntry(typeCode)
        onShortcutConsumed()
    }
    LaunchedEffect(openBillToken) {
        val token = openBillToken
        if (token == 0) return@LaunchedEffect
        navigator.showBill()
        onBillOpened(token)
    }
    val navigationWanted = navigationVisibleState.value && sceneState.currentScene.showsHomeTab()
    val showBottomBar = placement != IndexNavigationPlacement.Side && navigationVisibleState.value
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
        LocalAmountVisibility provides amountVisibility,
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
    IndexTab.Statistics -> HomeTab.Statistics
    IndexTab.Assets -> HomeTab.Assets
    IndexTab.Settings -> HomeTab.Settings
}

private fun HomeTab.toIndexTab(): IndexTab = when (this) {
    HomeTab.Bill -> IndexTab.Bill
    HomeTab.Statistics -> IndexTab.Statistics
    HomeTab.Assets -> IndexTab.Assets
    HomeTab.Settings -> IndexTab.Settings
}

@FormFactorPreviews
@Composable
private fun IndexScreenPreview() {
    CogniBalTheme {
        IndexScreen()
    }
}
