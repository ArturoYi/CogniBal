package com.cyr.cognobal.ui.privacy

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.cyr.cognobal.R

/**
 * 退到后台时用整屏深色盖住窗口。
 *
 * 从 Android 12 起，最近任务的图在 [Activity.onPause] 之前就截走了。
 * 开关打开时提前告诉系统不要截真实画面。任务背景、标题栏、状态栏、导航栏
 * 都用同一深色，否则系统会在卡片顶部铺一条浅色。
 * 窗口上的盖层在离开之后盖住真实画面，回到前台再揭开。
 */
internal class BackgroundPrivacy(context: Context) {
    private val cover = BackgroundPrivacyCover(context)
    private var overviewEnabled: Boolean? = null
    private var overviewColor: Int? = null
    private var barsCaptured = false
    private var previousStatusBarColor = 0
    private var previousNavigationBarColor = 0
    private var previousLightStatusBars = false
    private var previousLightNavigationBars = false
    private var previousStatusBarContrast = true
    private var previousNavigationBarContrast = false

    fun attach(decor: ViewGroup) {
        if (cover.parent === decor) return
        (cover.parent as? ViewGroup)?.removeView(cover)
        decor.addView(
            cover,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    /**
     * 最近任务的截图策略必须在离开之前就设好。
     * Android 13 起只影响最近任务，使用中仍可截图。更早的系统只能靠 [WindowManager.LayoutParams.FLAG_SECURE]。
     */
    fun syncOverview(activity: Activity, enabled: Boolean, backgroundColor: Int) {
        if (overviewEnabled == enabled && overviewColor == backgroundColor) return
        overviewEnabled = enabled
        overviewColor = backgroundColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(!enabled)
        } else if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        activity.setTaskDescription(overviewDescription(activity, enabled, backgroundColor))
    }

    fun show() {
        val activity = cover.context as? Activity ?: return
        val window = activity.window
        val coverColor = ContextCompat.getColor(activity, R.color.icon_black)
        if (!barsCaptured) {
            captureBars(window)
            barsCaptured = true
        }
        // 状态栏颜色视图画在内容之上。盖层若只铺在内容里，顶部会剩一条浅色。
        paintSystemBars(
            window = window,
            statusColor = coverColor,
            navigationColor = coverColor,
            lightStatusBars = false,
            lightNavigationBars = false,
            statusContrast = false,
            navigationContrast = false,
        )
        activity.findViewById<View>(android.R.id.content)?.importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        cover.isClickable = true
        cover.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        cover.visibility = View.VISIBLE
        cover.bringToFront()
        cover.post {
            if (cover.visibility == View.VISIBLE) cover.bringToFront()
        }
    }

    fun hide() {
        val activity = cover.context as? Activity
        cover.isClickable = false
        cover.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        // 保持已测量，下次显示不必再排一次版。
        cover.visibility = View.INVISIBLE
        activity?.findViewById<View>(android.R.id.content)?.importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        if (!barsCaptured || activity == null) return
        paintSystemBars(
            window = activity.window,
            statusColor = previousStatusBarColor,
            navigationColor = previousNavigationBarColor,
            lightStatusBars = previousLightStatusBars,
            lightNavigationBars = previousLightNavigationBars,
            statusContrast = previousStatusBarContrast,
            navigationContrast = previousNavigationBarContrast,
        )
        barsCaptured = false
    }

    @Suppress("DEPRECATION")
    private fun captureBars(window: Window) {
        previousStatusBarColor = window.statusBarColor
        previousNavigationBarColor = window.navigationBarColor
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        previousLightStatusBars = controller.isAppearanceLightStatusBars
        previousLightNavigationBars = controller.isAppearanceLightNavigationBars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            previousStatusBarContrast = window.isStatusBarContrastEnforced
            previousNavigationBarContrast = window.isNavigationBarContrastEnforced
        }
    }

    @Suppress("DEPRECATION")
    private fun paintSystemBars(
        window: Window,
        statusColor: Int,
        navigationColor: Int,
        lightStatusBars: Boolean,
        lightNavigationBars: Boolean,
        statusContrast: Boolean,
        navigationContrast: Boolean,
    ) {
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = statusContrast
            window.isNavigationBarContrastEnforced = navigationContrast
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatusBars
            isAppearanceLightNavigationBars = lightNavigationBars
        }
    }

    private fun overviewDescription(
        activity: Activity,
        enabled: Boolean,
        backgroundColor: Int,
    ): ActivityManager.TaskDescription {
        val label = activity.getString(R.string.app_name)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager.TaskDescription.Builder()
                .setLabel(label)
                .apply {
                    if (enabled) {
                        setBackgroundColor(backgroundColor)
                        setPrimaryColor(backgroundColor)
                        setStatusBarColor(backgroundColor)
                        setNavigationBarColor(backgroundColor)
                    }
                }
                .build()
        } else {
            @Suppress("DEPRECATION")
            ActivityManager.TaskDescription(
                label,
                null,
                if (enabled) backgroundColor else 0,
            )
        }
    }
}

private class BackgroundPrivacyCover(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    init {
        visibility = INVISIBLE
        setBackgroundColor(ContextCompat.getColor(context, R.color.icon_black))
        // 盖上之后才接住误触。藏着时不能抢点击。不拿焦点，免得把输入框的焦点抢走。
        isClickable = false
        isFocusable = false
        isSoundEffectsEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        contentDescription = context.getString(R.string.security_content_hidden)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = View::class.java.name
        info.isClickable = false
    }
}
