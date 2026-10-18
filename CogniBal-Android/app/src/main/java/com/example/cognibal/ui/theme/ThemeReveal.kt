package com.example.cognibal.ui.theme

import android.graphics.Bitmap
import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 主题切换的圆形展开。
 *
 * 旧画面铺在底层，新主题画在上面，只用一个越来越大的圆裁出新主题。
 * 半径在 [graphicsLayer] 里读，展开时不重组。
 * 不用 [View.draw]：那会把界面的硬件层画进软件画布，按钮会一直空白。
 */
internal val LocalThemeReveal = staticCompositionLocalOf<ThemeRevealState?> { null }

@Composable
internal fun rememberThemeRevealState(): ThemeRevealState {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    return remember(view, scope) { ThemeRevealState(scope, view) }
}

@Composable
internal fun ThemeRevealHost(
    state: ThemeRevealState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val captureLayer = rememberGraphicsLayer()
    state.attachCaptureLayer(captureLayer)
    CompositionLocalProvider(LocalThemeReveal provides state) {
        Box(
            modifier
                .fillMaxSize()
                .onGloballyPositioned { state.hostCoordinates = it }
                .drawBehind { drawFrozenFrame(state) }
                .drawWithContent {
                    state.recordTickValue()
                    if (!state.consumeRecordRequest()) {
                        drawContent()
                        return@drawWithContent
                    }
                    val width = size.width.roundToInt().coerceAtLeast(1)
                    val height = size.height.roundToInt().coerceAtLeast(1)
                    try {
                        captureLayer.record(
                            density = this,
                            layoutDirection = layoutDirection,
                            size = IntSize(width, height),
                        ) {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(captureLayer)
                        state.completeRecordRequest(recorded = true)
                    } catch (_: Throwable) {
                        drawContent()
                        state.completeRecordRequest(recorded = false)
                    }
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { applyRevealClip(state) },
            ) {
                content()
            }
        }
    }
}

internal fun ThemeRevealState?.revealOrRun(
    centerInRoot: Offset,
    colorsWillChange: Boolean,
    apply: () -> Unit,
) {
    if (this != null && colorsWillChange) {
        reveal(centerInRoot, apply)
    } else {
        apply()
    }
}

@Stable
internal class ThemeRevealState(
    private val scope: CoroutineScope,
    private val view: View,
) {
    private val snapshot = mutableStateOf<ImageBitmap?>(null)
    private val radius = Animatable(0f)
    private val recordTick = mutableIntStateOf(0)

    /** 只在点击时读，用来把根坐标换成这一层的坐标。 */
    internal var hostCoordinates: LayoutCoordinates? = null

    private var captureLayer: GraphicsLayer? = null
    private var revealCenter = Offset.Zero
    private var revealVisible = false
    private var captureJob: Job? = null
    private var pendingCenter = Offset.Zero
    private var pendingApply: (() -> Unit)? = null
    private var recordArmed = false
    private var recordWaiter: CompletableDeferred<Boolean>? = null
    private var heldStyle by mutableStateOf<AppThemeStyle?>(null)
    private var heldDark by mutableStateOf<Boolean?>(null)

    /** 偏好写在 DataStore 上，要等它回来颜色才会变。圆张开前先把目标记在这里。 */
    fun hold(style: AppThemeStyle? = null, dark: Boolean? = null) {
        if (style != null) heldStyle = style
        if (dark != null) heldDark = dark
    }

    fun releaseHold(style: AppThemeStyle, dark: Boolean) {
        if (heldStyle == style) heldStyle = null
        if (heldDark == dark) heldDark = null
    }

    fun styleOr(fallback: AppThemeStyle): AppThemeStyle = heldStyle ?: fallback

    fun darkOr(fallback: Boolean): Boolean = heldDark ?: fallback

    fun attachCaptureLayer(layer: GraphicsLayer) {
        captureLayer = layer
    }

    /**
     * 先录下当前这一帧，再执行 [apply]。
     *
     * 圆还在扩大时又点了别的颜色，只换底下的主题，不再截一张已经挖了洞的画面。
     */
    fun reveal(centerInRoot: Offset, apply: () -> Unit) {
        if (view.animationsDisabled()) {
            apply()
            return
        }
        val origin = centerInHost(centerInRoot)
        if (revealVisible) {
            revealCenter = origin
            apply()
            return
        }
        pendingCenter = origin
        pendingApply = apply
        if (captureJob?.isActive == true) return
        captureJob = scope.launch(Dispatchers.Main.immediate) {
            val image = captureOldFrame()
            val action = pendingApply
            val center = pendingCenter
            pendingApply = null
            if (!isActive || image == null || action == null) {
                image?.recyclePixels()
                action?.invoke()
                return@launch
            }
            try {
                radius.snapTo(0f)
                revealCenter = center
                snapshot.value = image
                revealVisible = true
                action()
                radius.animateTo(1f, RevealSpec)
            } finally {
                if (snapshot.value === image) {
                    snapshot.value = null
                    revealVisible = false
                }
                // 等这一帧不再画旧画面，再释放像素，避免按钮还拿着已回收的位图。
                withFrameNanos { }
                image.recyclePixels()
            }
        }
    }

    fun recordTickValue(): Int = recordTick.intValue

    fun consumeRecordRequest(): Boolean {
        if (!recordArmed) return false
        recordArmed = false
        return captureLayer != null
    }

    fun completeRecordRequest(recorded: Boolean) {
        val waiter = recordWaiter
        view.post {
            waiter?.complete(recorded)
            if (recordWaiter === waiter) recordWaiter = null
        }
    }

    internal fun overlay(): ImageBitmap? = snapshot.value

    internal fun progress(): Float = radius.value

    internal fun center(): Offset = revealCenter

    private suspend fun captureOldFrame(): ImageBitmap? {
        val layer = captureLayer ?: return null
        val waiter = CompletableDeferred<Boolean>()
        recordWaiter = waiter
        recordArmed = true
        recordTick.intValue++
        val recorded = withTimeoutOrNull(CAPTURE_TIMEOUT_MILLIS) { waiter.await() } == true
        if (!recorded) return null
        val captured = runCatching { layer.toImageBitmap() }.getOrNull() ?: return null
        return captured.duplicate()
    }

    private fun centerInHost(centerInRoot: Offset): Offset {
        val host = hostCoordinates
        if (host == null || !host.isAttached) return centerInRoot
        return centerInRoot - host.localToRoot(Offset.Zero)
    }
}

private fun GraphicsLayerScope.applyRevealClip(state: ThemeRevealState) {
    val image = state.overlay()
    val progress = state.progress()
    if (image == null || progress >= 1f) {
        alpha = 1f
        clip = false
        shape = RectangleShape
        compositingStrategy = CompositingStrategy.Auto
        return
    }
    compositingStrategy = CompositingStrategy.Offscreen
    if (progress <= 0f) {
        alpha = 0f
        clip = false
        shape = RectangleShape
        return
    }
    alpha = 1f
    clip = true
    val origin = state.center()
    val fraction = progress
    shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val path = Path()
            path.addOval(Rect(origin, coverRadius(size, origin) * fraction))
            return Outline.Generic(path)
        }
    }
}

private fun DrawScope.drawFrozenFrame(state: ThemeRevealState) {
    val image = state.overlay() ?: return
    val pixels = runCatching { image.asAndroidBitmap() }.getOrNull() ?: return
    if (pixels.isRecycled) return
    drawImage(
        image = image,
        dstSize = IntSize(
            size.width.roundToInt().coerceAtLeast(1),
            size.height.roundToInt().coerceAtLeast(1),
        ),
    )
}

private fun coverRadius(size: Size, center: Offset): Float {
    val horizontal = max(center.x, size.width - center.x)
    val vertical = max(center.y, size.height - center.y)
    return hypot(horizontal, vertical) + 2f
}

private fun ImageBitmap.duplicate(): ImageBitmap? {
    val source = runCatching { asAndroidBitmap() }.getOrNull() ?: return null
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    return try {
        val copy = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(copy).drawBitmap(source, 0f, 0f, null)
        copy.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}

private fun View.animationsDisabled(): Boolean {
    val scale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
    }.getOrNull() ?: return false
    return scale == 0f
}

private fun ImageBitmap.recyclePixels() {
    val bitmap = runCatching { asAndroidBitmap() }.getOrNull() ?: return
    if (!bitmap.isRecycled) bitmap.recycle()
}

private val RevealSpec = tween<Float>(
    durationMillis = THEME_REVEAL_MILLIS,
    easing = LinearOutSlowInEasing,
)

private const val THEME_REVEAL_MILLIS = 560
private const val CAPTURE_TIMEOUT_MILLIS = 500L
