package com.cyr.cognobal.screen.feature.splash

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.core.view.WindowCompat
import androidx.core.view.doOnLayout
import com.cyr.cognobal.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val STARTUP_MOTION_MILLIS = 1100

/** 扇形和标题在这之前结束，之后才淡出黑底。1100ms 线性时间轴上的 0.78。 */
private const val STARTUP_REVEAL_FRACTION = 0.78f
private const val STARTUP_BEFORE_REVEAL_MILLIS = 858
private const val STARTUP_REVEAL_MILLIS = STARTUP_MOTION_MILLIS - STARTUP_BEFORE_REVEAL_MILLIS
private const val STARTUP_HANDOFF_WAIT_MILLIS = 500L
private const val CARD_VIEWPORT = 108f
private const val CARD_PIVOT_X = 43f
private const val CARD_PIVOT_Y = 48f

/** 旋转中心在 108 视口里的比例。图层绕这一点转，卡片内容本身不用每帧重画。 */
private val CardPivotOrigin = TransformOrigin(
    CARD_PIVOT_X / CARD_VIEWPORT,
    CARD_PIVOT_Y / CARD_VIEWPORT,
)

/**
 * Android 12+ draws a plain splash drawable as an adaptive foreground: the 108 viewport is
 * mapped onto 1.5x the icon view and then clipped. Matching that scale keeps the card the
 * same size as the system splash. The compat splash below API 31 fits the drawable to the view.
 */
private val SPLASH_FOREGROUND_SCALE =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 1.5f else 1f

/**
 * Splash icon rectangle in window coordinates, the same space as [positionInWindow].
 */
data class SplashIconBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal class SplashExit {
    var provider: SplashScreenViewProvider? = null
    val iconBounds = mutableStateOf<SplashIconBounds?>(null)
    private var released = false

    fun handoff(provider: SplashScreenViewProvider) {
        if (released) {
            provider.remove()
            return
        }
        this.provider = provider
        val splashView = provider.view
        // 回调挂在启动页根视图上。新系统上图标子 View 可能已经卸掉，
        // icon.post / doOnLayout 永远不会跑，坐标也就交不出来。
        splashView.doOnLayout {
            if (!released) publishIconBounds(provider, waitForIcon = true)
        }
        splashView.post {
            if (!released && iconBounds.value == null) {
                publishIconBounds(provider, waitForIcon = true)
            }
        }
        splashView.postDelayed({
            if (!released && iconBounds.value == null) {
                publishIconBounds(provider, waitForIcon = false)
            }
        }, 100)
    }

    private fun publishIconBounds(provider: SplashScreenViewProvider, waitForIcon: Boolean) {
        val measured = provider.readIconBounds()
        if (measured != null) {
            iconBounds.value = measured
            return
        }
        val icon = provider.iconViewOrNull()
        if (icon != null && waitForIcon && !icon.isLaidOut) {
            icon.doOnLayout {
                if (!released) publishIconBounds(provider, waitForIcon = false)
            }
            return
        }
        iconBounds.value = provider.view.fallbackIconBounds()
    }

    fun release() {
        if (released) return
        released = true
        provider?.remove()
        provider = null
    }
}

private fun SplashScreenViewProvider.iconViewOrNull(): View? {
    return try {
        iconView
    } catch (_: Throwable) {
        null
    }
}

private fun SplashScreenViewProvider.readIconBounds(): SplashIconBounds? {
    val icon = iconViewOrNull() ?: return null
    val width = icon.width - icon.paddingLeft - icon.paddingRight
    val height = icon.height - icon.paddingTop - icon.paddingBottom
    if (width <= 0 || height <= 0) return null
    val location = IntArray(2)
    icon.getLocationInWindow(location)
    return SplashIconBounds(
        left = location[0] + icon.paddingLeft,
        top = location[1] + icon.paddingTop,
        width = width,
        height = height,
    )
}

private fun View.fallbackIconBounds(): SplashIconBounds {
    val location = IntArray(2)
    getLocationInWindow(location)
    return splashFallbackBounds(
        width = width,
        height = height,
        density = resources.displayMetrics.density,
        originInWindow = Offset(location[0].toFloat(), location[1].toFloat()),
    )
}

/**
 * API 31+ icon view is starting_surface_icon_size (160dp) * 192/160 when the theme has no
 * icon background. The compat ImageView is splashscreen_icon_size_no_background (288dp).
 */
private fun splashFallbackBounds(
    width: Int,
    height: Int,
    density: Float,
    originInWindow: Offset,
): SplashIconBounds {
    val sizeDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 192f else 288f
    val size = (sizeDp * density).roundToInt().coerceAtLeast(1)
    val frameW = width.coerceAtLeast(size)
    val frameH = height.coerceAtLeast(size)
    return SplashIconBounds(
        left = originInWindow.x.roundToInt() + (frameW - size) / 2,
        top = originInWindow.y.roundToInt() + (frameH - size) / 2,
        width = size,
        height = size,
    )
}

/**
 * Replaces [android.R.attr.windowBackground] once the startup overlay is gone.
 *
 * The activity theme keeps a black window so the splash handoff cannot flash the light
 * theme color. The status-bar scrim fades to transparent, so the main UI needs the window
 * behind it to match [MaterialTheme.colorScheme.background].
 */
@Composable
internal fun ApplyAppWindowBackground() {
    val view = LocalView.current
    val color = MaterialTheme.colorScheme.background.toArgb()
    val applied = remember { mutableIntStateOf(Int.MIN_VALUE) }
    if (applied.intValue == color) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.setBackgroundDrawable(ColorDrawable(color))
        applied.intValue = color
    }
}

@Composable
fun StartupSplash(
    iconBounds: SplashIconBounds?,
    onMatchedFrameDrawn: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    previewProgress: Float? = null,
    readyToReveal: Boolean = true,
    onFanSettled: () -> Unit = {},
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    val progress = remember { Animatable(previewProgress ?: 0f) }
    val view = LocalView.current
    var rootInWindow by remember { mutableStateOf<Offset?>(null) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var aligned by remember(iconBounds) { mutableStateOf(false) }

    DisposableEffect(view, reduceMotion, previewProgress) {
        if (previewProgress != null) {
            return@DisposableEffect onDispose {}
        }
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        if (!reduceMotion) {
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }

    val appBackground = MaterialTheme.colorScheme.background.toArgb()
    var motionFinished by remember { mutableStateOf(previewProgress != null) }
    var dismissRequested by remember { mutableStateOf(false) }
    LaunchedEffect(appBackground, previewProgress) {
        if (previewProgress != null) return@LaunchedEffect
        // Theme window background stays black for the gap frame. After this black overlay
        // has been drawn, switch back so any pixel the home does not cover is the app color.
        withFrameNanos { }
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        window.setBackgroundDrawable(ColorDrawable(appBackground))
    }

    val fallbackBounds = remember(rootInWindow, rootSize) {
        val origin = rootInWindow ?: return@remember null
        if (rootSize.width <= 0 || rootSize.height <= 0) return@remember null
        splashFallbackBounds(
            width = rootSize.width,
            height = rootSize.height,
            density = view.resources.displayMetrics.density,
            originInWindow = origin,
        )
    }
    val bounds = iconBounds ?: fallbackBounds
    val readyToRevealState = rememberUpdatedState(readyToReveal)
    val boundsState = rememberUpdatedState(bounds)
    LaunchedEffect(previewProgress) {
        if (previewProgress != null) return@LaunchedEffect
        // 系统页如果没交出图标坐标，也不能把扇形卡住。等一小会儿，有坐标就对齐，没有就用居中后备。
        withTimeoutOrNull(STARTUP_HANDOFF_WAIT_MILLIS) {
            snapshotFlow { boundsState.value }.first { it != null }
        }
        withTimeoutOrNull(STARTUP_HANDOFF_WAIT_MILLIS) {
            snapshotFlow { aligned }.first { it }
        }
        // `aligned` flips during layout, before draw. Removing the system splash there
        // presents a frame without either icon. Wait until the matched frame is drawn.
        withFrameNanos { }
        onMatchedFrameDrawn()
        if (!reduceMotion) {
            // 扇形这段不要同时建首页。首页第一次组合会占住主线程，中间帧会被吃掉。
            progress.animateTo(
                targetValue = STARTUP_REVEAL_FRACTION,
                animationSpec = tween(
                    durationMillis = STARTUP_BEFORE_REVEAL_MILLIS,
                    easing = LinearEasing,
                ),
            )
        }
        onFanSettled()
        // The fade shows whatever is underneath. Hold the opaque cover until the home
        // page has been drawn, otherwise this segment reveals an empty window.
        snapshotFlow { readyToRevealState.value }.first { it }
        if (!reduceMotion) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = STARTUP_REVEAL_MILLIS,
                    easing = LinearEasing,
                ),
            )
        }
        motionFinished = true
    }
    LaunchedEffect(motionFinished, dismissRequested, readyToReveal, previewProgress) {
        if (previewProgress != null) return@LaunchedEffect
        if (readyToReveal && (motionFinished || dismissRequested)) {
            onFinished()
        }
    }

    val splashBackground = colorResource(R.color.icon_black)
    val backColor = colorResource(R.color.icon_card_back)
    val midColor = colorResource(R.color.icon_card_mid)
    val frontColor = colorResource(R.color.icon_card_front)
    val origin = if (previewProgress != null) Offset.Zero else (rootInWindow ?: Offset.Zero)
    // 扇形和淡出读在 graphicsLayer 里。进度变了只改图层，不靠整棵组合重画。
    val motion = progress

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (readyToReveal || previewProgress != null) {
                    1f - span(motion.value, STARTUP_REVEAL_FRACTION, 1f)
                } else {
                    1f
                }
            }
            .background(splashBackground)
            .onGloballyPositioned {
                rootInWindow = it.positionInWindow()
                rootSize = it.size
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onMatchedFrameDrawn()
                    onFanSettled()
                    dismissRequested = true
                },
            ),
    ) {
        if (bounds != null) {
            val icon = iconPlacement(bounds, origin)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(icon.canvasLeft.roundToInt(), icon.canvasTop.roundToInt())
                    }
                    .onGloballyPositioned { coordinates ->
                        if (aligned || previewProgress != null) {
                            return@onGloballyPositioned
                        }
                        val position = coordinates.positionInWindow()
                        val centerX = position.x + coordinates.size.width / 2f
                        val centerY = position.y + coordinates.size.height / 2f
                        aligned = abs(centerX - bounds.centerX) <= 2f &&
                            abs(centerY - bounds.centerY) <= 2f &&
                            abs(coordinates.size.width - icon.canvasSize.roundToInt()) <= 2
                    }
                    .layout { measurable, _ ->
                        val size = icon.canvasSize.roundToInt().coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(size, size))
                        layout(size, size) {
                            placeable.place(0, 0)
                        }
                    },
            ) {
                FanCard(color = backColor, degrees = -18f, progress = { motion.value })
                FanCard(color = midColor, degrees = 0f, progress = { motion.value })
                FanCard(color = frontColor, degrees = 16f, progress = { motion.value })
            }
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            0,
                            (icon.slotTop + icon.slotSize + 20.dp.toPx()).roundToInt(),
                        )
                    }
                    .graphicsLayer {
                        alpha = FastOutSlowInEasing.transform(span(motion.value, 0.48f, 0.72f))
                    },
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FanCard(
    color: Color,
    degrees: Float,
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val path = remember { balanceCardPath() }
    Canvas(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = lerp(0f, degrees, fanAmount(progress()))
                transformOrigin = CardPivotOrigin
            },
    ) {
        val scale = size.minDimension / CARD_VIEWPORT
        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

private data class IconPlacement(
    val canvasLeft: Float,
    val canvasTop: Float,
    val canvasSize: Float,
    val slotTop: Float,
    val slotSize: Float,
)

private fun iconPlacement(bounds: SplashIconBounds, rootInWindow: Offset): IconPlacement {
    val slot = min(bounds.width, bounds.height).toFloat()
    val canvasSize = slot * SPLASH_FOREGROUND_SCALE
    val centerX = bounds.centerX - rootInWindow.x
    val centerY = bounds.centerY - rootInWindow.y
    return IconPlacement(
        canvasLeft = centerX - canvasSize / 2f,
        canvasTop = centerY - canvasSize / 2f,
        canvasSize = canvasSize,
        slotTop = centerY - slot / 2f,
        slotSize = slot,
    )
}

private val SplashIconBounds.centerX: Float
    get() = left + width / 2f

private val SplashIconBounds.centerY: Float
    get() = top + height / 2f

private fun balanceCardPath(): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addRoundRect(
        RoundRect(
            rect = Rect(35f, 41f, 75f, 66f),
            cornerRadius = CornerRadius(3f, 3f),
        ),
    )
    addOval(Rect(center = Offset(43f, 48f), radius = 3.3f))
}

private fun span(progress: Float, start: Float, end: Float): Float {
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun fanAmount(progress: Float): Float {
    return FastOutSlowInEasing.transform(span(progress, 0f, 0.62f))
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun StartupSplashPreview() {
    StartupSplash(
        iconBounds = SplashIconBounds(left = 108, top = 280, width = 144, height = 144),
        onMatchedFrameDrawn = {},
        onFinished = {},
        previewProgress = 0.7f,
    )
}
