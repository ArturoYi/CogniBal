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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 主题切换的圆形展开。
 *
 * 新主题先画出来。切换前的画面盖在上面，圆里用 [BlendMode.DstOut] 挖空，
 * 所以圆越大，新颜色露出来越多。半径只在 [drawWithContent] 里读，展开过程不重组。
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
                .drawWithContent {
                    // 读一次，点击后才能只请求重绘、不重组。
                    state.recordTickValue()
                    if (state.consumeRecordRequest()) {
                        val width = size.width.roundToInt().coerceAtLeast(1)
                        val height = size.height.roundToInt().coerceAtLeast(1)
                        captureLayer.record(
                            density = this,
                            layoutDirection = layoutDirection,
                            size = IntSize(width, height),
                        ) {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(captureLayer)
                        state.completeRecordRequest()
                        return@drawWithContent
                    }
                    drawContent()
                    drawOldFrameHole(state)
                },
        ) {
            content()
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
    private val layerPaint = Paint()
    private val recordTick = mutableIntStateOf(0)

    /** 只在点击时读，用来把根坐标换成这一层的坐标。 */
    internal var hostCoordinates: LayoutCoordinates? = null

    private var captureLayer: GraphicsLayer? = null
    private var revealCenter = Offset.Zero
    private var revealVisible = false
    private var captureJob: kotlinx.coroutines.Job? = null
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
     * 先留下当前画面，再执行 [apply]。
     *
     * 圆还在扩大时又点了别的颜色，底下直接换成最后一次，不再重新截一张半成品。
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

    fun completeRecordRequest() {
        val waiter = recordWaiter
        view.post {
            waiter?.complete(true)
            if (recordWaiter === waiter) recordWaiter = null
        }
    }

    internal fun overlay(): ImageBitmap? = snapshot.value

    internal fun progress(): Float = radius.value

    internal fun center(): Offset = revealCenter

    internal fun layerPaint(): Paint = layerPaint

    private suspend fun captureOldFrame(): ImageBitmap? {
        captureView(view)?.let { return it }
        val layer = captureLayer ?: return null
        val waiter = CompletableDeferred<Boolean>()
        recordWaiter = waiter
        recordArmed = true
        recordTick.intValue++
        val recorded = withTimeoutOrNull(CAPTURE_TIMEOUT_MILLIS) { waiter.await() } == true
        if (!recorded) return null
        return runCatching { layer.toImageBitmap() }.getOrNull()?.takeIf { it.hasVisiblePixel() }
    }

    private fun centerInHost(centerInRoot: Offset): Offset {
        val host = hostCoordinates
        if (host == null || !host.isAttached) return centerInRoot
        return centerInRoot - host.localToRoot(Offset.Zero)
    }
}

private fun ContentDrawScope.drawOldFrameHole(state: ThemeRevealState) {
    // 只在绘制时读。半径往前走一帧，只会让这一层重绘。
    val image = state.overlay()
    val progress = state.progress()
    if (image == null || progress >= 1f) return
    val pixels = image.asAndroidBitmap()
    if (pixels.isRecycled) return
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(Offset.Zero, size), state.layerPaint())
    try {
        drawImage(
            image = image,
            dstSize = IntSize(
                size.width.roundToInt().coerceAtLeast(1),
                size.height.roundToInt().coerceAtLeast(1),
            ),
        )
        if (progress > 0f) {
            drawCircle(
                color = Color.Black,
                radius = coverRadius(size, state.center()) * progress,
                center = state.center(),
                blendMode = BlendMode.DstOut,
            )
        }
    } finally {
        canvas.restore()
    }
}

private fun coverRadius(size: Size, center: Offset): Float {
    val horizontal = max(center.x, size.width - center.x)
    val vertical = max(center.y, size.height - center.y)
    return hypot(horizontal, vertical) + 2f
}

private fun captureView(view: View): ImageBitmap? {
    val width = view.width
    val height = view.height
    if (width <= 0 || height <= 0 || !view.isAttachedToWindow) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.density = view.resources.displayMetrics.densityDpi
    return try {
        view.draw(android.graphics.Canvas(bitmap))
        if (bitmap.hasVisiblePixel()) {
            bitmap.asImageBitmap()
        } else {
            bitmap.recycle()
            null
        }
    } catch (_: Throwable) {
        if (!bitmap.isRecycled) bitmap.recycle()
        null
    }
}

private fun Bitmap.hasVisiblePixel(): Boolean {
    if (width <= 0 || height <= 0) return false
    val samples = intArrayOf(
        width / 2, height / 2,
        width / 4, height / 3,
        width * 3 / 4, height * 2 / 3,
    )
    for (index in samples.indices step 2) {
        val x = samples[index].coerceIn(0, width - 1)
        val y = samples[index + 1].coerceIn(0, height - 1)
        if (getPixel(x, y) ushr 24 != 0) return true
    }
    return false
}

private fun ImageBitmap.hasVisiblePixel(): Boolean {
    val bitmap = asAndroidBitmap()
    return !bitmap.isRecycled && bitmap.hasVisiblePixel()
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

private const val THEME_REVEAL_MILLIS = 600
private const val CAPTURE_TIMEOUT_MILLIS = 500L
