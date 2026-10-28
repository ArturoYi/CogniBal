package com.example.cognibal.ui.overlay

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat

/**
 * 透明全屏窗口。底色由内容自己画。返回键在这里被吃掉，盖层还在时不能关掉。
 */
@Composable
internal fun FullScreenDialog(
    lightSystemBars: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val currentLightBars by rememberUpdatedState(lightSystemBars)
    val dialog = remember(context) {
        ComponentDialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen).apply {
            val window = window ?: return@apply
            window.setWindowAnimations(0)
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setDimAmount(0f)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }
    DisposableEffect(dialog) {
        val composeView = ComposeView(dialog.context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setParentCompositionContext(composition)
            setContent {
                SideEffect {
                    dialog.window?.let { window ->
                        WindowCompat.getInsetsController(window, this@apply).apply {
                            isAppearanceLightStatusBars = currentLightBars
                            isAppearanceLightNavigationBars = currentLightBars
                        }
                    }
                }
                currentContent()
            }
        }
        dialog.setContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        }
        dialog.onBackPressedDispatcher.addCallback(backCallback)
        dialog.show()
        onDispose {
            backCallback.remove()
            dialog.dismiss()
            composeView.disposeComposition()
        }
    }
}
