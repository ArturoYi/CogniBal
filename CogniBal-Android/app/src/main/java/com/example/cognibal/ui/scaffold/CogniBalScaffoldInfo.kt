package com.example.cognibal.ui.scaffold

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 顶层窗口与导航骨架的共享状态。
 *
 * [windowAdaptiveInfo] 在 [com.example.cognibal.ui.navigation.CogniBalScaffold] 里用
 * `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)` 算一次。后续屏幕读这里，
 * 这样 large / extra-large 宽度断点全应用一致。
 *
 * 要收起导航条时，在副作用里改 [isNavigationVisible]。骨架会跟着把导航条做显示或隐藏动画。
 */
@Stable
class CogniBalScaffoldInfo(
    val windowAdaptiveInfo: WindowAdaptiveInfo,
    val navigationSuiteType: NavigationSuiteType,
    val navigationConsumedInsets: WindowInsets,
    private val navigationVisibleState: MutableState<Boolean>,
) {
    var isNavigationVisible: Boolean
        get() = navigationVisibleState.value
        set(value) {
            navigationVisibleState.value = value
        }
}

/**
 * 骨架之外（例如独立预览）为 null。此时安全区按整窗 [WindowInsets.safeDrawing] 计算。
 */
val LocalCogniBalScaffold = staticCompositionLocalOf<CogniBalScaffoldInfo?> { null }
