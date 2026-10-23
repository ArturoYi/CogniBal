package com.example.cognibal.screen.feature.settings.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsSliceRole
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsSliceUi
import com.example.cognibal.ui.theme.AppColors
import com.example.cognibal.ui.theme.appColors
import kotlin.math.min

/**
 * 顶部一张甜甜圈。高度固定。
 *
 * 换月份时，同一块的占比和颜色按数值过渡到新值。新出现的块从 0 长出来，消失的块收到 0。
 * 每一帧各块占比加起来仍是一整圈，不做整图淡入淡出。
 */
@Immutable
internal data class CategoryDonutChart(
    val slices: List<StatisticsSliceUi>,
    val centerLabel: String,
    val centerAmount: String,
    val centerColor: Color,
)

@Composable
internal fun CategoryDonut(
    chart: CategoryDonutChart,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val rings = remember(
        chart.slices,
        colors.expense,
        colors.income,
        colors.textTertiary,
        colors.textSecondary,
        colors.isDark,
    ) {
        ringTargets(chart.slices, colors)
    }
    val motion = remember { DonutMotion(rings) }
    LaunchedEffect(rings) { motion.go(rings) }
    val frame = motion.frame().ifEmpty {
        listOf(RingSlice(EMPTY_KEY, 1f, emptyRingColor(colors)))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .clearAndSetSemantics {
                contentDescription = "${chart.centerLabel} ${chart.centerAmount}"
            },
        contentAlignment = Alignment.Center,
    ) {
        DonutArcs(
            rings = frame,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.widthIn(max = CENTER_TEXT_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chart.centerLabel,
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = chart.centerAmount,
                color = chart.centerColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DonutArcs(
    rings: List<RingSlice>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val diameter = min(size.width, size.height)
        if (diameter <= 0f) return@Canvas
        val outerRadius = diameter / 2f
        val innerRadius = min(diameter, INNER_DIAMETER.toPx()) / 2f
        val strokeWidth = outerRadius - innerRadius
        if (strokeWidth <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val band = outerRadius + innerRadius
        val topLeft = Offset(center.x - band / 2f, center.y - band / 2f)
        val arcSize = Size(band, band)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        if (rings.size == 1 && rings[0].fraction >= FULL_RING) {
            drawCircle(
                color = rings[0].color,
                radius = band / 2f,
                center = center,
                style = stroke,
            )
            return@Canvas
        }
        var angle = START_ANGLE
        val overlap = if (rings.size > 1) SEAM_OVERLAP else 0f
        for (ring in rings) {
            val sweep = ring.fraction * 360f
            if (sweep > 0f) {
                drawArc(
                    color = ring.color,
                    startAngle = angle,
                    sweepAngle = sweep + overlap,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
            angle += sweep
        }
    }
}

/** 没数据时是一整圈灰色，占比为 1，中间的数字仍是 0。 */
private fun ringTargets(slices: List<StatisticsSliceUi>, colors: AppColors): List<RingSlice> {
    if (slices.isEmpty()) return listOf(RingSlice(EMPTY_KEY, 1f, emptyRingColor(colors)))
    var total = 0.0
    for (slice in slices) total += slice.amount.toDouble()
    if (total <= 0.0) return listOf(RingSlice(EMPTY_KEY, 1f, emptyRingColor(colors)))
    var used = 0f
    return slices.mapIndexed { index, slice ->
        val fraction = if (index == slices.lastIndex) {
            (1f - used).coerceAtLeast(0f)
        } else {
            (slice.amount.toDouble() / total).toFloat().also { used += it }
        }
        RingSlice(
            key = slice.key,
            fraction = fraction,
            color = sliceColor(slice, colors),
        )
    }
}

private fun sliceColor(slice: StatisticsSliceUi, colors: AppColors): Color = when (slice.role) {
    StatisticsSliceRole.Category -> accountColorValue(slice.colorKey)
    StatisticsSliceRole.Expense -> colors.expense
    StatisticsSliceRole.Income -> colors.income
    StatisticsSliceRole.Other -> colors.textTertiary
}

/** 浅色用三级灰，深色改用二级灰，避免环和卡片底贴在一起。 */
private fun emptyRingColor(colors: AppColors): Color =
    if (colors.isDark) colors.textSecondary else colors.textTertiary

/**
 * 按扇区身份对齐旧值和新值，再用同一个进度做插值。
 * 进度为 0 时是切换前的占比，为 1 时是切换后的占比。
 */
private class DonutMotion(initial: List<RingSlice>) {
    private var started = false
    private var from by mutableStateOf(initial)
    private var to by mutableStateOf(initial)
    private val progress = Animatable(1f)

    fun frame(): List<RingSlice> = interpolate(from, to, progress.value)

    suspend fun go(target: List<RingSlice>) {
        if (!started) {
            from = target
            to = target
            progress.snapTo(1f)
            started = true
            return
        }
        if (progress.value == 1f && to == target) return
        from = interpolate(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = VALUE_MILLIS, easing = FastOutSlowInEasing),
        )
    }
}

private fun interpolate(from: List<RingSlice>, to: List<RingSlice>, t: Float): List<RingSlice> {
    val fraction = t.coerceIn(0f, 1f)
    if (from.isEmpty()) return to
    if (to.isEmpty()) return from
    val fromByKey = from.associateBy { ring -> ring.key }
    val toByKey = to.associateBy { ring -> ring.key }
    val keys = ArrayList<String>(from.size + to.size)
    from.forEach { ring -> if (ring.key !in keys) keys.add(ring.key) }
    to.forEach { ring -> if (ring.key !in keys) keys.add(ring.key) }
    return keys.mapNotNull { key ->
        val start = fromByKey[key]
        val end = toByKey[key]
        val share = lerpFloat(start?.fraction ?: 0f, end?.fraction ?: 0f, fraction)
        if (share <= 0f) return@mapNotNull null
        RingSlice(
            key = key,
            fraction = share,
            color = lerp(start?.color ?: end!!.color, end?.color ?: start!!.color, fraction),
        )
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@Immutable
private data class RingSlice(
    val key: String,
    val fraction: Float,
    val color: Color,
)

private const val EMPTY_KEY = "empty"
private const val START_ANGLE = -90f
private const val FULL_RING = 0.999f
private const val SEAM_OVERLAP = 0.6f
private const val VALUE_MILLIS = 420

private val CHART_HEIGHT = 220.dp
private val INNER_DIAMETER = 116.dp
private val CENTER_TEXT_WIDTH = 96.dp
