package com.cyr.cognobal.screen.feature.settings.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsSliceRole
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsSliceUi
import com.cyr.cognobal.ui.theme.AppColors
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.LocalAppThemeStyle
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.categorySliceColor
import kotlin.math.min

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
    modifier: Modifier = Modifier.size(CHART_DIAMETER),
) {
    val colors = MaterialTheme.appColors
    val monochrome = LocalAppThemeStyle.current == AppThemeStyle.Monochrome
    val rings = remember(
        chart.slices,
        colors.chartExpense,
        colors.chartIncome,
        colors.textTertiary,
        colors.tagBackground,
        colors.isDark,
        monochrome,
    ) {
        ringTargets(chart.slices, colors, monochrome)
    }
    val motion = remember { DonutMotion(rings) }
    LaunchedEffect(rings) { motion.go(rings) }
    val frame = motion.frame().ifEmpty {
        listOf(RingSlice(EMPTY_KEY, 1f, colors.tagBackground))
    }
    Box(modifier = modifier.clearAndSetSemantics {}) {
        DonutArcs(
            rings = frame,
            modifier = Modifier.fillMaxSize(),
        )
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
        val innerRadius = min(diameter * 0.58f, INNER_DIAMETER.toPx() / 2f)
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
        val gap = if (rings.size > 1) SLICE_GAP_DEGREES else 0f
        for (ring in rings) {
            val sweep = ring.fraction * 360f
            val visibleSweep = (sweep - gap).coerceAtLeast(0.8f)
            if (sweep > 0f) {
                drawArc(
                    color = ring.color,
                    startAngle = angle + gap / 2f,
                    sweepAngle = visibleSweep,
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

private fun ringTargets(
    slices: List<StatisticsSliceUi>,
    colors: AppColors,
    monochrome: Boolean,
): List<RingSlice> {
    if (slices.isEmpty()) return listOf(RingSlice(EMPTY_KEY, 1f, colors.tagBackground))
    var total = 0.0
    for (slice in slices) total += slice.amount.toDouble()
    if (total <= 0.0) return listOf(RingSlice(EMPTY_KEY, 1f, colors.tagBackground))
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
            color = sliceColor(slice, index, slices.size, colors, monochrome),
        )
    }
}

private fun sliceColor(
    slice: StatisticsSliceUi,
    index: Int,
    total: Int,
    colors: AppColors,
    monochrome: Boolean,
): Color = when (slice.role) {
    StatisticsSliceRole.Category -> categorySliceColor(
        colorKey = slice.colorKey,
        index = index,
        total = total,
        colors = colors,
        monochrome = monochrome,
    )
    StatisticsSliceRole.Expense -> if (monochrome) colors.chartExpense else Color(0xFFEA580C)
    StatisticsSliceRole.Income -> if (monochrome) colors.chartIncome else colors.income
    StatisticsSliceRole.Other -> colors.textTertiary
}

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
private const val SLICE_GAP_DEGREES = 2.0f
private const val VALUE_MILLIS = 420

internal val CHART_DIAMETER = 114.dp
private val INNER_DIAMETER = 66.dp
