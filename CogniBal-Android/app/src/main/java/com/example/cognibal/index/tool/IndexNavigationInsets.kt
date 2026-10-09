package com.example.cognibal.index.tool

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable

/**
 * 自定义导航条盖住的系统栏，供列表 contentPadding 和内容槽消费。
 *
 * 底栏只扣底部，侧栏只扣起始侧。条本身的高度已经从内容槽里减掉，这里不再减一次。
 * 导航条收起且动画结束后不扣，内容按整窗安全区排。
 */
@Composable
internal fun navigationConsumedInsets(
    type: NavigationSuiteType,
    navigationVisible: Boolean,
): WindowInsets {
    if (!navigationVisible) return EmptyWindowInsets
    return when (type) {
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
}

private val EmptyWindowInsets = WindowInsets(0, 0, 0, 0)

/**
 * 导航条放在哪一侧。和 [androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout]
 * 的底栏判定一致：窄窗口和矮窗口在底部，其余在起始侧。
 */
internal enum class IndexNavigationPlacement {
    /** 竖屏底栏：图标在上，文案在下。 */
    BottomStacked,

    /** 矮窗口底栏：图标和文案同一行，少占高度。 */
    BottomInline,

    /** 宽窗口侧栏。 */
    Side,
}

internal fun NavigationSuiteType.indexNavigationPlacement(): IndexNavigationPlacement =
    when (this) {
        NavigationSuiteType.ShortNavigationBarMedium -> IndexNavigationPlacement.BottomInline
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.NavigationBar,
        -> IndexNavigationPlacement.BottomStacked
        else -> IndexNavigationPlacement.Side
    }
