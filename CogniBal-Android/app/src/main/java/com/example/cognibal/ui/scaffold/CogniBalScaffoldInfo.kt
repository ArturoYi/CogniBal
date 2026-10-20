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
 * [windowAdaptiveInfo] 在 [com.example.cognibal.screen.feature.home.IndexScreen] 里用
 * `currentWindowAdaptiveInfoV2()` 算一次。后续屏幕读这里，
 * 这样 large / extra-large 宽度断点全应用一致。
 *
 * 窄屏底栏不占内容槽高度，画在页面下面。[navigationOverlayInsets] 是它盖住的那一截，
 * 首页用它把底色收到栏上方，二级页不收，滑进来时高度不变。
 * 要收起底栏时，在副作用里改 [isNavigationVisible]。底栏不再绘制，首页也不再留这块高度。
 */
@Stable
class CogniBalScaffoldInfo(
    val windowAdaptiveInfo: WindowAdaptiveInfo,
    val navigationSuiteType: NavigationSuiteType,
    val navigationConsumedInsets: WindowInsets,
    val navigationOverlayInsets: WindowInsets,
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
