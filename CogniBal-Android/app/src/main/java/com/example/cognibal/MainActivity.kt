package com.example.cognibal

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.cognibal.shortcut.AppShortcuts
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.screen.feature.bill.BillHomeWarmup
import com.example.cognibal.screen.feature.bill.rememberBillHomeWarmup
import com.example.cognibal.screen.feature.home.IndexScreen
import com.example.cognibal.screen.feature.legal.LegalConsentScreen
import com.example.cognibal.screen.feature.legal.LegalGate
import com.example.cognibal.screen.feature.settings.SettingsViewModel
import com.example.cognibal.screen.feature.splash.ApplyAppWindowBackground
import com.example.cognibal.screen.feature.splash.SplashExit
import com.example.cognibal.screen.feature.splash.StartupSplash
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.ThemeRevealHost
import com.example.cognibal.ui.theme.rememberThemeRevealState

/**
 * 应用入口，负责把系统启动页无缝交给 [StartupSplash]。
 *
 * 冷启动时系统先画 `Theme.CogniBal.Splash`（黑底 + 静态图标）。内容准备好后，
 * SplashScreen 会回调退出动画；这里不让它自己淡出，而是把图标在窗口中的位置交给
 * Compose。Compose 把卡片画到同一位置并完成一帧后，才撤掉系统启动页，再播扇形展开。
 * 这样两层图标重合，用户看不到跳变。首页先画在这层黑底下面，淡出时已经是完整的一帧。
 */
class MainActivity : AppCompatActivity() {
    /**
     * 跨 Compose 重组持有系统启动页。
     *
     * `SplashScreenViewProvider` 只能在退出回调里拿到，而图标坐标要等下一帧布局后才有效，
     * 所以不能放进组合里。对齐完成、动画被跳过，或 Activity 销毁时，都通过它调用 [SplashExit.release]。
     */
    private val splashExit = SplashExit()
    private var appLightStatusBars = true
    private var appLightNavigationBars = true
    private var shortcutTypeCode by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate 之前调用。否则窗口会先切到 postSplashScreenTheme，
        // 系统启动页在退出回调注册前就结束，后面无法拿到图标位置。
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val fromShortcut = AppShortcuts.typeCode(intent) != null
        shortcutTypeCode = AppShortcuts.typeCode(intent)
        // 内容延伸到状态栏和导航栏后面，启动页才能铺满系统图标所在的整窗。
        // ComponentActivity.enableEdgeToEdge 会按系统主题设置状态栏/导航栏图标颜色。
        enableEdgeToEdge()
        // 底栏会画进系统导航栏。关掉对比度遮罩，避免系统再铺一层半透明底。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val insets = WindowCompat.getInsetsController(window, window.decorView)
        appLightStatusBars = insets.isAppearanceLightStatusBars
        appLightNavigationBars = insets.isAppearanceLightNavigationBars
        // 默认退出是整页淡出。这里改成挂起：handoff 保留系统视图，并在布局完成后读出图标矩形。
        // 若此时启动动画已经结束（例如配置变更后恢复），handoff 会立刻 remove，避免挡住界面。
        //
        // androidx 会在这个回调的开头按浅色主题重刷系统栏（深色图标、导航栏对比度遮罩）。
        // 回调返回前就会画到屏幕上，所以要在 handoff 之前改回启动页的浅色图标。
        // 从桌面快捷方式进来时不要挂住系统启动页。挂住后图标对不齐，黑底会一直盖着，要点一下才进记一笔。
        splashScreen.setOnExitAnimationListener { provider ->
            if (fromShortcut) {
                provider.remove()
            } else {
                keepSplashSystemBars()
                splashExit.handoff(provider)
            }
        }
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
            val legalGate by settingsViewModel.legalGate.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val preferenceDark = preferences.useDarkTheme(systemDark)
            val themeReveal = rememberThemeRevealState()
            SideEffect {
                themeReveal.releaseHold(preferences.themeStyle, preferenceDark)
            }
            val isDark = themeReveal.darkOr(preferenceDark)

            CogniBalTheme(
                themeStyle = themeReveal.styleOr(preferences.themeStyle),
                darkTheme = isDark,
            ) {
                // rememberSaveable 把“是否还在播启动动画”写进 SavedState。
                // 旋转、分屏、深色模式等配置变更会重建 Activity，但不能从头再播一遍。
                var showSplash by rememberSaveable { mutableStateOf(!fromShortcut) }
                LaunchedEffect(shortcutTypeCode) {
                    if (shortcutTypeCode != null) showSplash = false
                }
                // 系统图标的窗口坐标；布局完成前为 null，StartupSplash 会等它再对齐。
                val iconBounds by splashExit.iconBounds
                // 主题包住整页。旧画面盖在新主题上，圆从点中的那一项挖开。
                ThemeRevealHost(
                    state = themeReveal,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val accepted = legalGate == LegalGate.Accepted
                    // 还没同意协议时不建账本、不把首页画出来。启动页盖住确认页，淡出后才能点同意。
                    val coverStartup = showSplash && shortcutTypeCode == null
                    val warmHome = coverStartup && accepted
                    val warmup = if (warmHome) rememberBillHomeWarmup() else BillHomeWarmup.Shown
                    // 数据写进状态后还要等它真正画出来。同一帧就揭开，会先闪出窗口底色。
                    var homeDrawn by remember { mutableStateOf(false) }
                    LaunchedEffect(warmHome, warmup.contentReady) {
                        if (!warmHome || !warmup.contentReady) {
                            homeDrawn = false
                            return@LaunchedEffect
                        }
                        withFrameNanos { }
                        withFrameNanos { }
                        homeDrawn = true
                    }
                    var consentDrawn by remember { mutableStateOf(false) }
                    LaunchedEffect(coverStartup, legalGate) {
                        if (!coverStartup || legalGate != LegalGate.Required) {
                            consentDrawn = false
                            return@LaunchedEffect
                        }
                        withFrameNanos { }
                        withFrameNanos { }
                        consentDrawn = true
                    }
                    val readyToReveal = when (legalGate) {
                        LegalGate.Pending -> false
                        LegalGate.Required -> consentDrawn
                        LegalGate.Accepted -> warmup.contentReady && homeDrawn
                    }
                    Box(Modifier.fillMaxSize()) {
                        if (legalGate == LegalGate.Required) {
                            LegalConsentScreen(
                                onAgree = settingsViewModel::acceptLegalTerms,
                                onDecline = { this@MainActivity.finish() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (accepted && (!coverStartup || warmup.composeHome)) {
                            IndexScreen(
                                shortcutTypeCode = shortcutTypeCode,
                                onShortcutConsumed = {
                                    shortcutTypeCode = null
                                    setIntent(Intent(this@MainActivity, MainActivity::class.java))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (coverStartup) {
                            StartupSplash(
                                iconBounds = iconBounds,
                                // 图标中心重合（约 2px）或等待超时后，再等这一帧画完才撤系统层。
                                onMatchedFrameDrawn = { revealStartupFrame() },
                                // 扇形播完后先停住。底下的首页或确认页画好才淡出。
                                onFinished = { showSplash = false },
                                readyToReveal = readyToReveal,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            // 从已结束状态恢复时不会进入 StartupSplash，但系统仍会回调退出动画。
                            // release 与 handoff 谁先发生都可以：已 release 则 handoff 直接移除，否则这里摘掉挂住的系统页。
                            // 主题窗口背景是启动页的黑色，主界面要改回自己的底色，状态栏渐变才不会透出黑底。
                            ApplyAppWindowBackground()
                            LaunchedEffect(isDark) {
                                splashExit.release()
                                restoreAppSystemBars(!isDark)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutTypeCode = AppShortcuts.typeCode(intent)
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
        // remove() 会再次按主题把系统栏改回深色图标，覆盖层还在，要马上改回来。
        splashExit.release()
        keepSplashSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun keepSplashSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun restoreAppSystemBars(lightBars: Boolean = appLightStatusBars) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }
}
