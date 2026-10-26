/*
 * Pencil motion adapted from Gustavo Sperandio's loader:
 * https://uiverse.io/gustavofusco/rare-pug-90
 *
 * Copyright (c) 2023 Gustavo Sperandio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.example.cognibal.ui.loading

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.cognibal.R
import com.example.cognibal.ui.theme.AppColors
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import kotlin.math.PI
import kotlin.math.tan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** 默认可读尺寸。再小的话笔尖和橡皮会糊在一起。 */
val LoadingIndicatorSize: Dp = 96.dp

/** 一圈的时长。原作 3 秒，本地等待显得拖，收到 1.8 秒。 */
const val LoadingIndicatorCycleMillis: Int = 1_800

/** 本地查询往往几十毫秒就结束，晚一点再出现，避免闪一下。 */
const val LoadingIndicatorAppearDelayMillis: Int = 300

private const val VIEWBOX = 200f
private const val VIEW_CENTER = 100f
private const val STROKE_RADIUS = 70f
private const val STROKE_CIRCUMFERENCE = 439.82f
private const val BODY_RADIUS = 64f
private const val BODY_CIRCUMFERENCE = 402.12f
private const val BODY_OUTER_RADIUS = 74f
private const val BODY_OUTER_CIRCUMFERENCE = 464.96f
private const val BODY_INNER_RADIUS = 54f
private const val BODY_INNER_CIRCUMFERENCE = 339.29f

/**
 * 不确定进度的铅笔指示。转两圈，前半段把圆画出来，后半段擦掉。
 *
 * 动画进度只在绘制时读取。系统把动画时长缩成 0 时停在起笔，不再空转。
 * 旁边已经有说明文字时，把 [contentDescription] 传 null。
 * 已知百分比的进度条不要换成它。
 *
 * [previewProgress] 把画面钉在 0 到 1 的某一帧，预览里不用等动画跑起来。
 */
@Composable
fun CogniBalLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = LoadingIndicatorSize,
    showDelayMillis: Int = 0,
    contentDescription: String? = stringResource(R.string.loading_indicator_cd),
    previewProgress: Float? = null,
) {
    val context = LocalContext.current
    val clock = remember(previewProgress) { Animatable(previewProgress ?: 0f) }
    var visible by remember(showDelayMillis, previewProgress) {
        mutableStateOf(showDelayMillis <= 0 || previewProgress != null)
    }
    LaunchedEffect(showDelayMillis, previewProgress) {
        if (previewProgress != null || showDelayMillis <= 0) {
            visible = true
            return@LaunchedEffect
        }
        visible = false
        delay(showDelayMillis.toLong())
        visible = true
    }
    LaunchedEffect(visible, previewProgress, context) {
        val pinned = previewProgress
        if (pinned != null) {
            clock.snapTo(pinned)
            return@LaunchedEffect
        }
        if (!visible || animationsDisabled(context)) {
            clock.snapTo(0f)
            return@LaunchedEffect
        }
        while (isActive) {
            clock.snapTo(0f)
            clock.animateTo(
                targetValue = 1f,
                animationSpec = tween(LoadingIndicatorCycleMillis, easing = LinearEasing),
            )
        }
    }
    val palette = rememberPencilPalette()
    val geometry = remember { PencilGeometry() }
    val spoken = contentDescription
    val description = if (spoken == null) {
        Modifier
    } else {
        Modifier.semantics {
            this.contentDescription = spoken
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
    }
    Canvas(
        modifier = modifier
            .size(size)
            .then(description),
    ) {
        if (previewProgress == null && !visible) return@Canvas
        drawPencil(
            progress = previewProgress ?: clock.value,
            palette = palette,
            geometry = geometry,
        )
    }
}

@Composable
private fun rememberPencilPalette(): PencilPalette {
    val colors = MaterialTheme.appColors
    return remember(colors.primary, colors.textSecondary, colors.isDark) {
        pencilPalette(colors)
    }
}

private fun pencilPalette(colors: AppColors): PencilPalette {
    val primary = colors.primary
    val ferrule = if (colors.isDark) 0.72f else 0.92f
    return PencilPalette(
        body = primary,
        bodyLight = lerpColor(primary, Color.White, 0.24f),
        bodyDark = lerpColor(primary, Color.Black, 0.20f),
        eraser = lerpColor(primary, Color.White, 0.42f),
        eraserShade = lerpColor(primary, Color.Black, 0.12f),
        ferrule = Color.hsl(220f, 0.08f, ferrule),
        ferruleShade = Color.hsl(220f, 0.08f, (ferrule - 0.18f).coerceAtLeast(0.45f)),
        ferruleEdge = Color.hsl(220f, 0.08f, (ferrule - 0.10f).coerceIn(0.5f, 0.98f)),
        ferruleLine = Color.hsl(220f, 0.10f, 0.12f).copy(alpha = 0.22f),
        wood = Color.hsl(33f, 0.90f, 0.70f),
        woodShade = Color.hsl(33f, 0.90f, 0.50f),
        lead = Color.hsl(220f, 0.10f, 0.12f),
        stroke = colors.textSecondary,
    )
}

private fun DrawScope.drawPencil(
    progress: Float,
    palette: PencilPalette,
    geometry: PencilGeometry,
) {
    if (size.minDimension <= 0f) return
    val pose = pencilPose(progress)
    val side = size.minDimension
    val scale = side / VIEWBOX
    translate(
        left = (size.width - side) / 2f,
        top = (size.height - side) / 2f,
    ) {
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
            drawStroke(pose, palette)
            drawPencilBody(pose, palette, geometry)
        }
    }
}

private fun DrawScope.drawStroke(pose: PencilPose, palette: PencilPalette) {
    translate(VIEW_CENTER, VIEW_CENTER) {
        rotate(degrees = pose.strokeDegrees, pivot = Offset.Zero) {
            drawCircle(
                color = palette.stroke,
                radius = STROKE_RADIUS,
                center = Offset.Zero,
                style = dashedStroke(
                    width = 2f,
                    phase = pose.strokeDash,
                    length = STROKE_CIRCUMFERENCE,
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}

private fun DrawScope.drawPencilBody(
    pose: PencilPose,
    palette: PencilPalette,
    geometry: PencilGeometry,
) {
    translate(VIEW_CENTER, VIEW_CENTER) {
        rotate(degrees = pose.spinDegrees, pivot = Offset.Zero) {
            rotate(degrees = pose.bodyDegrees, pivot = Offset.Zero) {
                drawCircle(
                    color = palette.body,
                    radius = BODY_RADIUS,
                    center = Offset.Zero,
                    style = dashedStroke(
                        width = 30f,
                        phase = pose.bodyDash,
                        length = BODY_CIRCUMFERENCE,
                        cap = StrokeCap.Butt,
                    ),
                )
                drawCircle(
                    color = palette.bodyLight,
                    radius = BODY_OUTER_RADIUS,
                    center = Offset.Zero,
                    style = dashedStroke(
                        width = 10f,
                        phase = pose.bodyOuterDash,
                        length = BODY_OUTER_CIRCUMFERENCE,
                        cap = StrokeCap.Butt,
                    ),
                )
                drawCircle(
                    color = palette.bodyDark,
                    radius = BODY_INNER_RADIUS,
                    center = Offset.Zero,
                    style = dashedStroke(
                        width = 10f,
                        phase = pose.bodyInnerDash,
                        length = BODY_INNER_CIRCUMFERENCE,
                        cap = StrokeCap.Butt,
                    ),
                )
            }
            rotate(degrees = pose.eraserDegrees, pivot = Offset.Zero) {
                translate(49f, 0f) {
                    skewX(pose.eraserSkewDegrees, geometry.skew) {
                        drawEraser(palette, geometry.eraserClip)
                    }
                }
            }
            rotate(degrees = pose.pointDegrees, pivot = Offset.Zero) {
                translate(49f, -30f) {
                    drawPath(geometry.wood, palette.wood)
                    drawPath(geometry.woodShade, palette.woodShade)
                    drawPath(geometry.lead, palette.lead)
                }
            }
        }
    }
}

private fun DrawScope.drawEraser(palette: PencilPalette, clip: Path) {
    drawRoundRect(
        color = palette.eraser,
        topLeft = Offset.Zero,
        size = Size(30f, 30f),
        cornerRadius = CornerRadius(5f, 5f),
    )
    clipPath(clip) {
        drawRect(
            color = palette.eraserShade,
            topLeft = Offset.Zero,
            size = Size(5f, 30f),
        )
    }
    drawRect(color = palette.ferrule, topLeft = Offset.Zero, size = Size(30f, 20f))
    drawRect(color = palette.ferruleShade, topLeft = Offset.Zero, size = Size(15f, 20f))
    drawRect(color = palette.ferruleEdge, topLeft = Offset.Zero, size = Size(5f, 20f))
    drawRect(color = palette.ferruleLine, topLeft = Offset(0f, 6f), size = Size(30f, 2f))
    drawRect(color = palette.ferruleLine, topLeft = Offset(0f, 13f), size = Size(30f, 2f))
}

private inline fun DrawScope.skewX(
    degrees: Float,
    matrix: Matrix,
    block: DrawScope.() -> Unit,
) {
    if (degrees == 0f) {
        block()
        return
    }
    matrix.reset()
    // y 乘到 x 上，和 CSS skewX 同一方向。
    matrix.values[Matrix.SkewX] = tan(degrees * (PI.toFloat() / 180f))
    withTransform({ transform(matrix) }, block)
}

private fun dashedStroke(
    width: Float,
    phase: Float,
    length: Float,
    cap: StrokeCap,
): Stroke = Stroke(
    width = width,
    cap = cap,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(length, length), phase),
)

private fun animationsDisabled(context: Context): Boolean {
    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
    return scale == 0f
}

@Immutable
private data class PencilPalette(
    val body: Color,
    val bodyLight: Color,
    val bodyDark: Color,
    val eraser: Color,
    val eraserShade: Color,
    val ferrule: Color,
    val ferruleShade: Color,
    val ferruleEdge: Color,
    val wood: Color,
    val woodShade: Color,
    val lead: Color,
    val ferruleLine: Color,
    val stroke: Color,
)

private class PencilGeometry {
    val wood = triangle(15f, 0f, 30f, 30f, 0f, 30f)
    val woodShade = triangle(15f, 0f, 6f, 30f, 0f, 30f)
    val lead = triangle(15f, 0f, 20f, 10f, 10f, 10f)
    val eraserClip = Path().apply {
        addRoundRect(RoundRect(0f, 0f, 30f, 30f, CornerRadius(5f, 5f)))
    }
    val skew = Matrix()

    private fun triangle(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ): Path = Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y1)
        lineTo(x2, y2)
        close()
    }
}

private data class PencilPose(
    val spinDegrees: Float,
    val bodyDegrees: Float,
    val bodyDash: Float,
    val bodyOuterDash: Float,
    val bodyInnerDash: Float,
    val eraserDegrees: Float,
    val eraserSkewDegrees: Float,
    val pointDegrees: Float,
    val strokeDegrees: Float,
    val strokeDash: Float,
)

private fun pencilPose(progress: Float): PencilPose {
    val fraction = progress.coerceIn(0f, 1f)
    val swing = if (fraction <= 0.5f) fraction / 0.5f else (1f - fraction) / 0.5f
    val stroke = strokePose(fraction)
    return PencilPose(
        spinDegrees = 720f * fraction,
        bodyDegrees = lerp(-90f, -225f, swing),
        bodyDash = lerp(351.86f, 150.8f, swing),
        bodyOuterDash = lerp(406.84f, 174.36f, swing),
        bodyInnerDash = lerp(296.88f, 127.23f, swing),
        eraserDegrees = lerp(-45f, 0f, swing),
        eraserSkewDegrees = eraserSkew(fraction),
        pointDegrees = lerp(-90f, -225f, swing),
        strokeDegrees = stroke.first,
        strokeDash = stroke.second,
    )
}

private fun strokePose(fraction: Float): Pair<Float, Float> = when {
    fraction <= 0.5f -> -113f to lerp(439.82f, 164.93f, fraction / 0.5f)
    fraction <= 0.75f -> {
        val weight = (fraction - 0.5f) / 0.25f
        lerp(-113f, 112f, weight) to lerp(164.93f, 439.82f, weight)
    }
    else -> 112f to 439.82f
}

private fun eraserSkew(fraction: Float): Float {
    val stops = EraserSkew
    var index = 2
    while (index < stops.size) {
        val end = stops[index]
        if (fraction <= end) {
            val start = stops[index - 2]
            val span = end - start
            val weight = if (span == 0f) 1f else (fraction - start) / span
            return lerp(stops[index - 1], stops[index + 1], smoothStep(weight))
        }
        index += 2
    }
    return 0f
}

private fun smoothStep(fraction: Float): Float {
    val t = fraction.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private val EraserSkew = floatArrayOf(
    0f, 0f,
    0.325f, 0f,
    0.35f, -4f,
    0.375f, 8f,
    0.40f, -15f,
    0.425f, 15f,
    0.45f, -15f,
    0.475f, 15f,
    0.50f, -15f,
    0.525f, 15f,
    0.55f, -15f,
    0.575f, 15f,
    0.60f, -15f,
    0.625f, 8f,
    0.65f, -4f,
    0.675f, 0f,
    1f, 0f,
)

@Preview(widthDp = 480, heightDp = 140)
@Composable
private fun CogniBalLoadingIndicatorPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LoadingPreviewCell(AppThemeStyle.WarmCream, dark = false)
        LoadingPreviewCell(AppThemeStyle.WarmCream, dark = true)
        LoadingPreviewCell(AppThemeStyle.Monochrome, dark = false)
        LoadingPreviewCell(AppThemeStyle.Monochrome, dark = true)
    }
}

@Preview(widthDp = 160, heightDp = 160)
@Composable
private fun CogniBalLoadingIndicatorMotionPreview() {
    CogniBalTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.appColors.background),
            contentAlignment = Alignment.Center,
        ) {
            CogniBalLoadingIndicator()
        }
    }
}

@Composable
private fun LoadingPreviewCell(style: AppThemeStyle, dark: Boolean) {
    CogniBalTheme(themeStyle = style, darkTheme = dark) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.appColors.background),
            contentAlignment = Alignment.Center,
        ) {
            CogniBalLoadingIndicator(previewProgress = 0.42f)
        }
    }
}
