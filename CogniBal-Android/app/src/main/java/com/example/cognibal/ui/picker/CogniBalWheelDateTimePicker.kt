package com.example.cognibal.ui.picker

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.cognibal.R
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 平面滚轮日期时间选择器。
 *
 * 不使用第三方滚轮库。每列是一个 [LazyColumn]，吸附用 [SnapPosition.Start]。
 * 上下各补半屏空白行，使第一条和最后一条也能停在正中。没有弧形透视。
 *
 * `onValueChange` 只在某一列从滚动变为静止、并且中心值相对当前值有变化时调用。
 * 滚动过程中不回调。传入的 [value] 若超出 [minTime] / [maxTime]，会在静止时回调一次修正后的值。
 * 父组件需要把回调写回 [value]。写回后，对应的列会在空闲时滚到新位置。
 *
 * 越界的数字不会放进列表，因此滚不到边界外面。年、月、日变化后保留原来的时分秒；
 * 时分秒若不再合法，收束到该日允许的最近端点。日大于当月天数时收束到当月最后一天。
 * 颜色和列标题跟随当前主题与语言。时间模式包含秒。
 *
 * 性能：
 * - 每列的 [LazyListState] 只 [remember] 一次，不随条目数量或选中值更换实例。
 * - 透明度和缩放写在 [graphicsLayer] 里，读取发生在放置阶段，滚动不会触发重组。
 * - 分割线、选中底和边缘遮罩在 [drawWithCache] 里创建，画笔随尺寸缓存。
 * - 遮罩用 [BlendMode.DstIn] 把内容自身淡出，不依赖底色，浅色和暗色都能用。
 *
 * ```
 * var dateTime by remember { mutableStateOf(WheelDateTime.now()) }
 * CogniBalWheelDateTimePicker(
 *     value = dateTime,
 *     onValueChange = { dateTime = it },
 *     mode = WheelDateTimeMode.DateTime,
 *     minTime = WheelDateTime.of(2020, 1, 1, 0, 0),
 *     maxTime = WheelDateTime.of(2030, 12, 31, 23, 59),
 *     config = WheelPickerConfig(
 *         visibleCount = 5,
 *         selectionStyle = WheelSelectionStyle.DividerLines,
 *         fadeEnabled = true,
 *         scaleEnabled = true,
 *         hapticEnabled = true,
 *     ),
 * )
 * ```
 */
@Composable
fun CogniBalWheelDateTimePicker(
    value: WheelDateTime,
    onValueChange: (WheelDateTime) -> Unit,
    modifier: Modifier = Modifier,
    mode: WheelDateTimeMode = WheelDateTimeMode.DateTime,
    minTime: WheelDateTime? = null,
    maxTime: WheelDateTime? = null,
    config: WheelPickerConfig = WheelPickerConfig.Default,
) {
    val visibleCount = normalizedVisibleCount(config.visibleCount)
    val itemHeight = if (config.itemHeight < MinItemHeight) MinItemHeight else config.itemHeight
    val minAlpha = config.minAlpha.coerceIn(0f, 1f)
    val minScale = config.minScale.coerceIn(0.5f, 1f)
    val safeValue = remember(value, minTime, maxTime) {
        WheelDateTimeRules.clamp(value, minTime, maxTime)
    }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(safeValue, value) {
        if (safeValue != value) currentOnValueChange(safeValue)
    }

    val years = remember(minTime, maxTime) {
        WheelDateTimeRules.yearRange(minTime, maxTime).toList()
    }
    val months = remember(safeValue.year, minTime, maxTime) {
        WheelDateTimeRules.monthRange(safeValue.year, minTime, maxTime).toList()
    }
    val days = remember(safeValue.year, safeValue.month, minTime, maxTime) {
        WheelDateTimeRules.dayRange(safeValue.year, safeValue.month, minTime, maxTime).toList()
    }
    val hours = remember(safeValue.year, safeValue.month, safeValue.day, minTime, maxTime) {
        WheelDateTimeRules.hourRange(
            safeValue.year,
            safeValue.month,
            safeValue.day,
            minTime,
            maxTime,
        ).toList()
    }
    val minutes = remember(
        safeValue.year,
        safeValue.month,
        safeValue.day,
        safeValue.hour,
        minTime,
        maxTime,
    ) {
        WheelDateTimeRules.minuteRange(
            safeValue.year,
            safeValue.month,
            safeValue.day,
            safeValue.hour,
            minTime,
            maxTime,
        ).toList()
    }
    val seconds = remember(
        safeValue.year,
        safeValue.month,
        safeValue.day,
        safeValue.hour,
        safeValue.minute,
        minTime,
        maxTime,
    ) {
        WheelDateTimeRules.secondRange(
            safeValue.year,
            safeValue.month,
            safeValue.day,
            safeValue.hour,
            safeValue.minute,
            minTime,
            maxTime,
        ).toList()
    }
    val fields = mode.fields()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val wheelHeight = itemHeight * visibleCount

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            fields.forEach { field ->
                Text(
                    text = stringResource(field.labelRes),
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.appColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(wheelHeight)
                .clipToBounds()
                .wheelChrome(
                    itemHeight = itemHeight,
                    style = config.selectionStyle,
                    fadeEnabled = config.fadeEnabled,
                ),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                fields.forEach { field ->
                    val entries = when (field) {
                        WheelField.Year -> years
                        WheelField.Month -> months
                        WheelField.Day -> days
                        WheelField.Hour -> hours
                        WheelField.Minute -> minutes
                        WheelField.Second -> seconds
                    }
                    key(field) {
                        CogniBalWheelColumn(
                            values = entries,
                            selected = safeValue.component(field),
                            label = stringResource(field.labelRes),
                            format = { number -> field.format(number, locale) },
                            itemHeight = itemHeight,
                            visibleCount = visibleCount,
                            fadeEnabled = config.fadeEnabled,
                            scaleEnabled = config.scaleEnabled,
                            minAlpha = minAlpha,
                            minScale = minScale,
                            hapticEnabled = config.hapticEnabled,
                            onSettled = { number ->
                                val next = WheelDateTimeRules.clamp(
                                    safeValue.withComponent(field, number),
                                    minTime,
                                    maxTime,
                                )
                                if (next != safeValue) currentOnValueChange(next)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单列滚轮。空白行数是可见行数的一半，[SnapPosition.Start] 把某一项贴到视口顶部时，
 * 正中那一行就是它后面的第 spacer 项，也就是当前数据。
 * 因此滚到数据下标 i，等价于 [LazyListState.scrollToItem] 的 i，而不是 i + spacer。
 */
@Composable
private fun RowScope.CogniBalWheelColumn(
    values: List<Int>,
    selected: Int,
    label: String,
    format: (Int) -> String,
    itemHeight: Dp,
    visibleCount: Int,
    fadeEnabled: Boolean,
    scaleEnabled: Boolean,
    minAlpha: Float,
    minScale: Float,
    hapticEnabled: Boolean,
    onSettled: (Int) -> Unit,
) {
    val spacer = visibleCount / 2
    // 状态只创建一次。不能把 values 或 selected 放进 remember 的 key，否则列表一变就会丢掉惯性。
    val listState = remember {
        LazyListState(firstVisibleItemIndex = values.indexOf(selected).coerceAtLeast(0))
    }
    val snapLayout = remember(listState) {
        SnapLayoutInfoProvider(lazyListState = listState, snapPosition = SnapPosition.Start)
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayout)
    val currentItems by rememberUpdatedState(values)
    val currentSelected by rememberUpdatedState(selected)
    val currentSpacer by rememberUpdatedState(spacer)
    val currentOnSettled by rememberUpdatedState(onSettled)
    val currentHapticEnabled by rememberUpdatedState(hapticEnabled)
    val haptic = LocalHapticFeedback.current
    // 程序滚动和手指滚动都会让 isScrollInProgress 经历 true → false。
    // 程序滚动期间不回调，避免联动修正再触发一次选中。
    val programmatic = remember { ProgrammaticScroll() }

    LaunchedEffect(listState) {
        var userScrollStarted = false
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    userScrollStarted = true
                    return@collect
                }
                if (!userScrollStarted) return@collect
                userScrollStarted = false
                if (programmatic.depth > 0) return@collect
                val index = centerDataIndex(listState, currentSpacer, currentItems.size) ?: return@collect
                val number = currentItems.getOrNull(index) ?: return@collect
                if (number == currentSelected) return@collect
                if (currentHapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                currentOnSettled(number)
            }
    }

    LaunchedEffect(selected, values, spacer) {
        snapshotFlow {
            listState.isScrollInProgress to centerDataIndex(listState, spacer, values.size)
        }.first { (scrolling, index) -> !scrolling && index != null }
        val target = values.indexOf(selected)
        if (target < 0) return@LaunchedEffect
        val current = centerDataIndex(listState, spacer, values.size)
        if (current == target) return@LaunchedEffect
        programmatic.depth += 1
        try {
            listState.animateScrollToItem(target)
        } finally {
            programmatic.depth -= 1
        }
    }

    val scope = rememberCoroutineScope()
    val description = stringResource(R.string.wheel_column_value, label, format(selected))
    val nextLabel = stringResource(R.string.wheel_next)
    val previousLabel = stringResource(R.string.wheel_previous)
    val actions = listOf(
        CustomAccessibilityAction(nextLabel) {
            scrollByStep(scope, listState, values, selected, +1)
        },
        CustomAccessibilityAction(previousLabel) {
            scrollByStep(scope, listState, values, selected, -1)
        },
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics {
                contentDescription = description
                customActions = actions
            },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {},
            state = listState,
            flingBehavior = flingBehavior,
            userScrollEnabled = values.size > 1,
        ) {
            items(
                count = spacer,
                key = { index -> "start-$index" },
                contentType = { "pad" },
            ) {
                Spacer(modifier = Modifier.fillMaxWidth().height(itemHeight))
            }
            itemsIndexed(
                items = values,
                key = { _, value -> value },
                contentType = { _, _ -> "value" },
            ) { index, value ->
                WheelNumber(
                    text = format(value),
                    listIndex = index + spacer,
                    listState = listState,
                    spacer = spacer,
                    itemHeight = itemHeight,
                    fadeEnabled = fadeEnabled,
                    scaleEnabled = scaleEnabled,
                    minAlpha = minAlpha,
                    minScale = minScale,
                )
            }
            items(
                count = spacer,
                key = { index -> "end-$index" },
                contentType = { "pad" },
            ) {
                Spacer(modifier = Modifier.fillMaxWidth().height(itemHeight))
            }
        }
    }
}

@Composable
private fun WheelNumber(
    text: String,
    listIndex: Int,
    listState: LazyListState,
    spacer: Int,
    itemHeight: Dp,
    fadeEnabled: Boolean,
    scaleEnabled: Boolean,
    minAlpha: Float,
    minScale: Float,
) {
    val distanceModifier = if (fadeEnabled || scaleEnabled) {
        Modifier.graphicsLayer {
            applyDistance(
                listState = listState,
                listIndex = listIndex,
                spacer = spacer,
                fadeEnabled = fadeEnabled,
                scaleEnabled = scaleEnabled,
                minAlpha = minAlpha,
                minScale = minScale,
            )
        }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .then(distanceModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.appColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * 放置阶段才读列表位置。这里读到的状态变化只会让这一层重绘，不会让组合函数重跑。
 */
private fun GraphicsLayerScope.applyDistance(
    listState: LazyListState,
    listIndex: Int,
    spacer: Int,
    fadeEnabled: Boolean,
    scaleEnabled: Boolean,
    minAlpha: Float,
    minScale: Float,
) {
    val visible = listState.layoutInfo.visibleItemsInfo
    var itemOffset = 0
    var itemSize = 0
    var found = false
    for (index in visible.indices) {
        val item = visible[index]
        if (item.index == listIndex) {
            itemOffset = item.offset
            itemSize = item.size
            found = true
            break
        }
    }
    if (!found || itemSize == 0) return
    val viewportCenter = (
        listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset
        ) / 2f
    val itemCenter = itemOffset + itemSize / 2f
    val maxDistance = (itemSize * spacer).coerceAtLeast(1).toFloat()
    val fraction = (abs(itemCenter - viewportCenter) / maxDistance).coerceIn(0f, 1f)
    alpha = if (fadeEnabled) lerp(1f, minAlpha, fraction) else 1f
    val scale = if (scaleEnabled) lerp(1f, minScale, fraction) else 1f
    scaleX = scale
    scaleY = scale
}

@Composable
private fun Modifier.wheelChrome(
    itemHeight: Dp,
    style: WheelSelectionStyle,
    fadeEnabled: Boolean,
): Modifier {
    val colors = MaterialTheme.appColors
    val lineColor = colors.divider
    val highlightColor = colors.primary.copy(alpha = HighlightAlpha)
    val fadeLayer = if (fadeEnabled) {
        Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    } else {
        Modifier
    }
    return this.then(fadeLayer).drawWithCache {
        val rowHeight = itemHeight.toPx()
        val centerTop = (size.height - rowHeight) / 2f
        val stroke = 1.dp.toPx()
        val inset = 8.dp.toPx()
        val radius = 8.dp.toPx()
        val mask = if (fadeEnabled) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.45f),
                0.08f to Color.Black,
                0.92f to Color.Black,
                1f to Color.Black.copy(alpha = 0.45f),
            )
        } else {
            null
        }
        onDrawWithContent {
            if (style == WheelSelectionStyle.HighlightBackground) {
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(inset, centerTop),
                    size = Size((size.width - inset * 2f).coerceAtLeast(0f), rowHeight),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
            drawContent()
            if (mask != null) {
                drawRect(brush = mask, blendMode = BlendMode.DstIn)
            }
            if (style == WheelSelectionStyle.DividerLines) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, centerTop),
                    end = Offset(size.width, centerTop),
                    strokeWidth = stroke,
                )
                val bottom = centerTop + rowHeight
                drawLine(
                    color = lineColor,
                    start = Offset(0f, bottom),
                    end = Offset(size.width, bottom),
                    strokeWidth = stroke,
                )
            }
        }
    }
}

private fun centerDataIndex(state: LazyListState, spacer: Int, itemCount: Int): Int? {
    if (itemCount == 0) return null
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return null
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    var closestIndex = -1
    var closestDistance = Int.MAX_VALUE
    for (item in visible) {
        val distance = abs((item.offset + item.size / 2) - viewportCenter)
        if (distance < closestDistance) {
            closestDistance = distance
            closestIndex = item.index
        }
    }
    if (closestIndex < 0) return null
    val dataIndex = closestIndex - spacer
    return dataIndex.takeIf { it in 0 until itemCount }
}

private fun scrollByStep(
    scope: CoroutineScope,
    listState: LazyListState,
    items: List<Int>,
    selected: Int,
    delta: Int,
): Boolean {
    val current = items.indexOf(selected)
    if (current < 0) return false
    val target = current + delta
    if (target !in items.indices) return false
    scope.launch { listState.animateScrollToItem(target) }
    return true
}

private fun normalizedVisibleCount(count: Int): Int {
    val atLeastThree = count.coerceAtLeast(3)
    return if (atLeastThree % 2 == 0) atLeastThree - 1 else atLeastThree
}

private enum class WheelField(@get:StringRes @param:StringRes val labelRes: Int) {
    Year(R.string.wheel_year),
    Month(R.string.wheel_month),
    Day(R.string.wheel_day),
    Hour(R.string.wheel_hour),
    Minute(R.string.wheel_minute),
    Second(R.string.wheel_second),
}

private fun WheelDateTimeMode.fields(): List<WheelField> = when (this) {
    WheelDateTimeMode.Date -> listOf(WheelField.Year, WheelField.Month, WheelField.Day)
    WheelDateTimeMode.Time -> listOf(WheelField.Hour, WheelField.Minute, WheelField.Second)
    WheelDateTimeMode.DateTime -> listOf(
        WheelField.Year,
        WheelField.Month,
        WheelField.Day,
        WheelField.Hour,
        WheelField.Minute,
    )
}

private fun WheelDateTime.component(field: WheelField): Int = when (field) {
    WheelField.Year -> year
    WheelField.Month -> month
    WheelField.Day -> day
    WheelField.Hour -> hour
    WheelField.Minute -> minute
    WheelField.Second -> second
}

private fun WheelDateTime.withComponent(field: WheelField, number: Int): WheelDateTime = when (field) {
    WheelField.Year -> copy(year = number)
    WheelField.Month -> copy(month = number)
    WheelField.Day -> copy(day = number)
    WheelField.Hour -> copy(hour = number)
    WheelField.Minute -> copy(minute = number)
    WheelField.Second -> copy(second = number)
}

private fun WheelField.format(number: Int, locale: Locale): String = when (this) {
    WheelField.Year -> String.format(locale, "%d", number)
    else -> String.format(locale, "%02d", number)
}

/** 主线程上的计数器，不放进组合状态，避免程序滚动本身引起重组。 */
private class ProgrammaticScroll {
    var depth: Int = 0
}

private val MinItemHeight = 36.dp
private const val HighlightAlpha = 0.16f

@FormFactorPreviews
@Composable
private fun CogniBalWheelDateTimePickerPreview() {
    CogniBalTheme {
        Surface {
            var time by remember { mutableStateOf(WheelDateTime.of(2026, 10, 19, 21, 0)) }
            CogniBalWheelDateTimePicker(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                value = time,
                onValueChange = { time = it },
                minTime = WheelDateTime.of(2020, 1, 1, 0, 0),
                maxTime = WheelDateTime.of(2030, 12, 31, 23, 59),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun CogniBalWheelDateTimePickerDarkPreview() {
    CogniBalTheme(darkTheme = true) {
        Surface {
            CogniBalWheelDateTimePicker(
                modifier = Modifier.padding(16.dp),
                value = WheelDateTime.of(2026, 2, 28, 9, 30),
                onValueChange = {},
                mode = WheelDateTimeMode.Date,
                config = WheelPickerConfig(selectionStyle = WheelSelectionStyle.HighlightBackground),
            )
        }
    }
}
