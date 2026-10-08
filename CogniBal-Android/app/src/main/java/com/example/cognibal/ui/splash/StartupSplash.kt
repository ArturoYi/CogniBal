package com.example.cognibal.ui.splash

import android.app.Activity
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.core.view.WindowCompat
import com.example.cognibal.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val STARTUP_MOTION_MILLIS = 1100
private const val CARD_VIEWPORT = 108f
private const val CARD_PIVOT_X = 43f
private const val CARD_PIVOT_Y = 48f

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
        provider.view.post {
            if (released) return@post
            iconBounds.value = provider.readIconBounds() ?: provider.view.fallbackIconBounds()
        }
    }

    fun release() {
        if (released) return
        released = true
        provider?.remove()
        provider = null
    }
}

private fun SplashScreenViewProvider.readIconBounds(): SplashIconBounds? {
    val icon = iconView
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
    val size = (108f * 1.2f * resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    return SplashIconBounds(
        left = (width - size) / 2,
        top = (height - size) / 2,
        width = size,
        height = size,
    )
}

@Composable
fun StartupSplash(
    iconBounds: SplashIconBounds?,
    onMatchedFrameDrawn: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    previewProgress: Float? = null,
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
    var aligned by remember { mutableStateOf(false) }
    val cardPath = remember { balanceCardPath() }

    DisposableEffect(view, reduceMotion, previewProgress) {
        if (reduceMotion || previewProgress != null) {
            return@DisposableEffect onDispose {}
        }
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }

    LaunchedEffect(iconBounds, previewProgress) {
        if (previewProgress != null || iconBounds == null) return@LaunchedEffect
        withTimeoutOrNull(500) {
            snapshotFlow { aligned }.first { it }
        }
        onMatchedFrameDrawn()
        withFrameNanos { }
        if (reduceMotion) {
            onFinished()
            return@LaunchedEffect
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = STARTUP_MOTION_MILLIS,
                easing = LinearEasing,
            ),
        )
        onFinished()
    }

    val splashBackground = colorResource(R.color.icon_black)
    val backColor = colorResource(R.color.icon_card_back)
    val midColor = colorResource(R.color.icon_card_mid)
    val frontColor = colorResource(R.color.icon_card_front)
    val origin = if (previewProgress != null) Offset.Zero else rootInWindow
    val bounds = iconBounds
    val fan = FastOutSlowInEasing.transform(span(progress.value, 0.08f, 0.62f))
    val backRotation = lerp(0f, -18f, fan)
    val frontRotation = lerp(0f, 16f, fan)
    val titleProgress = FastOutSlowInEasing.transform(span(progress.value, 0.48f, 0.72f))
    val overlayAlpha = 1f - span(progress.value, 0.78f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
            .background(splashBackground)
            .onGloballyPositioned { rootInWindow = it.positionInWindow() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onMatchedFrameDrawn()
                    onFinished()
                },
            ),
    ) {
        val root = origin
        if (bounds != null && root != null) {
            val icon = iconPlacement(bounds, root)
            Canvas(
                modifier = Modifier
                    .offset {
                        IntOffset(icon.canvasLeft.roundToInt(), icon.canvasTop.roundToInt())
                    }
                    .onGloballyPositioned { coordinates ->
                        if (aligned || progress.value != 0f || previewProgress != null) {
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
                val scale = size.minDimension / CARD_VIEWPORT
                val pivot = Offset(CARD_PIVOT_X, CARD_PIVOT_Y)
                withTransform({
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    rotate(backRotation, pivot) { drawPath(cardPath, backColor) }
                    drawPath(cardPath, midColor)
                    rotate(frontRotation, pivot) { drawPath(cardPath, frontColor) }
                }
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
                    .graphicsLayer { alpha = titleProgress },
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
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
