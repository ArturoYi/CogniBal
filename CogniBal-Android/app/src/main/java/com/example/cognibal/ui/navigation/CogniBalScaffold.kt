package com.example.cognibal.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.cognibal.R
import com.example.cognibal.ui.insets.StatusBarProtection
import com.example.cognibal.ui.insets.screenSafeDrawing
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.scaffold.CogniBalScaffoldInfo
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold
import com.example.cognibal.ui.theme.CogniBalTheme

/**
 * 顶层目的地。新增页面时在这里加一项，导航条会跟着长出来。
 */
enum class CogniBalDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.destination_home, Icons.Filled.Home),
}

/**
 * 应用壳：一次读取 [currentWindowAdaptiveInfo]，再按窗口选择底栏或侧栏。
 *
 * 骨架本身不把安全区 padding 传给内容，各页面用 `screenSafeDrawing` 或 `listContentPadding`。
 * 不要给这个 Scaffold 加 `safeDrawingPadding`，否则底栏背景到不了屏幕边缘。
 */
@Composable
fun CogniBalScaffold(modifier: Modifier = Modifier) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
    val navigationVisibleState = rememberSaveable { mutableStateOf(true) }
    val scaffoldState = rememberNavigationSuiteScaffoldState(
        initialValue = if (navigationVisibleState.value) {
            NavigationSuiteScaffoldValue.Visible
        } else {
            NavigationSuiteScaffoldValue.Hidden
        },
    )
    val scaffoldInfo = CogniBalScaffoldInfo(
        windowAdaptiveInfo = windowAdaptiveInfo,
        navigationSuiteType = navigationSuiteType,
        navigationConsumedInsets = if (navigationVisibleState.value) {
            navigationConsumedInsets(navigationSuiteType)
        } else {
            EmptyWindowInsets
        },
        navigationVisibleState = navigationVisibleState,
    )
    var destination by rememberSaveable { mutableStateOf(CogniBalDestination.Home) }

    LaunchedEffect(scaffoldInfo.isNavigationVisible) {
        if (scaffoldInfo.isNavigationVisible) {
            scaffoldState.show()
        } else {
            scaffoldState.hide()
        }
    }

    CompositionLocalProvider(LocalCogniBalScaffold provides scaffoldInfo) {
        NavigationSuiteScaffold(
            navigationItems = {
                CogniBalDestination.entries.forEach { item ->
                    NavigationSuiteItem(
                        selected = item == destination,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                        navigationSuiteType = navigationSuiteType,
                    )
                }
            },
            modifier = modifier.fillMaxSize(),
            navigationSuiteType = navigationSuiteType,
            state = scaffoldState,
        ) {
            Box(Modifier.fillMaxSize()) {
                when (destination) {
                    CogniBalDestination.Home -> HomePane(Modifier.fillMaxSize())
                }
                StatusBarProtection()
            }
        }
    }
}

/**
 * 与 [NavigationSuiteScaffold] 内部消费的系统栏对齐，供列表 contentPadding 使用。
 * 底栏只扣底部，侧栏只扣起始侧；栏本身的高度已经从内容槽里减掉，这里不再减一次。
 */
@Composable
private fun navigationConsumedInsets(type: NavigationSuiteType): WindowInsets =
    when (type) {
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.ShortNavigationBarMedium,
        ->
            ShortNavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
        NavigationSuiteType.NavigationBar ->
            NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
        NavigationSuiteType.WideNavigationRailCollapsed,
        NavigationSuiteType.WideNavigationRailExpanded,
        ->
            WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
        NavigationSuiteType.NavigationRail ->
            NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
        NavigationSuiteType.NavigationDrawer ->
            DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)
        else -> EmptyWindowInsets
    }

private val EmptyWindowInsets = WindowInsets(0, 0, 0, 0)

@Composable
private fun HomePane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.screenSafeDrawing(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@FormFactorPreviews
@Composable
private fun CogniBalScaffoldPreview() {
    CogniBalTheme {
        CogniBalScaffold()
    }
}
