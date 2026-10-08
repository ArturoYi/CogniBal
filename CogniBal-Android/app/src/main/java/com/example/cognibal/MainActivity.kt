package com.example.cognibal

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.cognibal.ui.navigation.CogniBalScaffold
import com.example.cognibal.ui.splash.SplashExit
import com.example.cognibal.ui.splash.StartupSplash
import com.example.cognibal.ui.theme.CogniBalTheme

/**
 * 应用入口，负责把系统启动页无缝交给 [StartupSplash]。
 *
 * 冷启动时系统先画 `Theme.CogniBal.Splash`（黑底 + 静态图标）。内容准备好后，
 * SplashScreen 会回调退出动画；这里不让它自己淡出，而是把图标在窗口中的位置交给
 * Compose。Compose 把卡片画到同一位置并完成一帧后，才撤掉系统启动页，再播扇形展开。
 * 这样两层图标重合，用户看不到跳变。
 */
class MainActivity : ComponentActivity() {
    /**
     * 跨 Compose 重组持有系统启动页。
     *
     * `SplashScreenViewProvider` 只能在退出回调里拿到，而图标坐标要等下一帧布局后才有效，
     * 所以不能放进组合里。对齐完成、动画被跳过，或 Activity 销毁时，都通过它调用 [SplashExit.release]。
     */
    private val splashExit = SplashExit()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate 之前调用。否则窗口会先切到 postSplashScreenTheme，
        // 系统启动页在退出回调注册前就结束，后面无法拿到图标位置。
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 内容延伸到状态栏和导航栏后面，启动页才能铺满系统图标所在的整窗。
        // ComponentActivity.enableEdgeToEdge 会按系统主题设置状态栏/导航栏图标颜色。
        enableEdgeToEdge()
        // 底栏会画进系统导航栏。关掉对比度遮罩，避免系统再铺一层半透明底。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // 默认退出是整页淡出。这里改成挂起：handoff 保留系统视图，并在下一帧读出图标矩形。
        // 若此时启动动画已经结束（例如配置变更后恢复），handoff 会立刻 remove，避免挡住界面。
        splashScreen.setOnExitAnimationListener { provider ->
            splashExit.handoff(provider)
        }
        setContent {
            CogniBalTheme {
                // rememberSaveable 把“是否还在播启动动画”写进 SavedState。
                // 旋转、分屏、深色模式等配置变更会重建 Activity，但不能从头再播一遍。
                var showSplash by rememberSaveable { mutableStateOf(true) }
                // 系统图标的窗口坐标；布局完成前为 null，StartupSplash 会等它再对齐。
                val iconBounds by splashExit.iconBounds
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        StartupSplash(
                            iconBounds = iconBounds,
                            // 图标中心重合（约 2px）或等待超时后撤掉系统层，避免两层图标错位。
                            onMatchedFrameDrawn = { revealStartupFrame() },
                            // 扇形动画结束，或用户点击跳过、系统关闭动效时，卸下 Compose 覆盖层。
                            onFinished = { showSplash = false },
                        )
                    } else {
                        // 从已结束状态恢复时不会进入 StartupSplash，但系统仍会回调退出动画。
                        // release 与 handoff 谁先发生都可以：已 release 则 handoff 直接移除，否则这里摘掉挂住的系统页。
                        LaunchedEffect(Unit) {
                            splashExit.release()
                        }
                        CogniBalScaffold(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // 退出回调可能仍持有 SplashScreen 的窗口视图；不移除会在 Activity 销毁后泄漏。
        splashExit.release()
        super.onDestroy()
    }

    /**
     * Compose 图标与系统图标重合后撤掉系统启动页。
     *
     * 系统页背景是 `icon_black`。`isAppearanceLight* = false` 表示状态栏和导航栏
     * 使用浅色图标，避免白底主题的深色图标留在黑底上。此调用发生在覆盖层还在时；
     * [StartupSplash] 结束时会按进入前的值还原，主界面仍跟随 `Theme.CogniBal`。
     */
    private fun revealStartupFrame() {
        splashExit.release()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}
