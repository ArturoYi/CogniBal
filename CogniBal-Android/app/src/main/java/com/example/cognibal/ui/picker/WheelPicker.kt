package com.example.cognibal.ui.picker

import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.example.cognibal.R
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 单列平面滚轮。
 *
 * 用 [LazyColumn] 虚拟化条目，停止后吸附到最近的一项。没有曲面、旋转或远近缩放。
 * 组件只负责这一列的滚动、吸附和选中，不解释条目的业务含义，也不做列与列之间的联动。
 *
 * [selectedIndex] 由外部持有。用户停稳后，若中心项和 [selectedIndex] 不同，调用
 * [onSelectedIndexChange]。外部把 [selectedIndex] 改成另一个下标时，滚轮会滚到那一项。
 * 下标超出 [minIndex]、[maxIndex]，或落在 [isItemEnabled] 为 false 的条目上时，会改到最近的可选下标。
 *
 * 颜色从 [decoration] 和 [textAppearance] 传入。浅色和暗色由调用方选择，这里不读取主题。
 * [WheelPickerDefaults] 只是按当前主题生成这两份参数的便捷方法。
 *
 * 多列共用一条选中线和同一块渐隐时，把 [drawChrome] 设为 false，在父布局上调用 [wheelPickerChrome]。
 *
 * ```
 * var index by remember { mutableIntStateOf(0) }
 * WheelPicker(
 *     items = items,
 *     selectedIndex = index,
 *     onSelectedIndexChange = { index = it },
 *     itemText = { it.label },
 *     isItemEnabled = { it.available },
 *     minIndex = 1,
 *     maxIndex = items.lastIndex - 1,
 * )
 * ```
 */
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemText: (T) -> String = { it.toString() },
    itemKey: (index: Int, item: T) -> Any = { index, _ -> index },
    isItemEnabled: (T) -> Boolean = { true },
    minIndex: Int? = null,
    maxIndex: Int? = null,
    config: WheelPickerConfig = WheelPickerConfig.Default,
    decoration: WheelSelectionDecoration = WheelPickerDefaults.dividerDecoration(),
    textAppearance: WheelTextAppearance = WheelPickerDefaults.textAppearance(),
    contentDescription: String? = null,
    drawChrome: Boolean = true,
) {
    val visibleCount = config.wheelVisibleCount()
    val itemHeight = config.wheelItemHeight()
    val spacer = visibleCount / 2
    val wheelHeight = itemHeight * visibleCount
    val bounds = selectableBounds(items.size, minIndex, maxIndex) { index ->
        isItemEnabled(items[index])
    }
    val resolvedIndex = bounds?.let { bound ->
        nearestSelectableIndex(selectedIndex, bound) { index ->
            isItemEnabled(items[index])
        }
    }
    val currentOnSelected by rememberUpdatedState(onSelectedIndexChange)
    LaunchedEffect(resolvedIndex, selectedIndex) {
        val target = resolvedIndex ?: return@LaunchedEffect
        if (target != selectedIndex) currentOnSelected(target)
    }

    // 列表状态只创建一次。条目数量或选中值变化时不能换实例，否则会丢掉惯性。
    val listState = remember {
        LazyListState(firstVisibleItemIndex = (resolvedIndex ?: 0).coerceAtLeast(0))
    }
    val snapLayout = remember(listState) {
        SnapLayoutInfoProvider(lazyListState = listState, snapPosition = SnapPosition.Start)
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayout)
    val programmatic = remember { ProgrammaticScroll() }
    val boundConnection = remember(listState) { WheelScrollBound(listState) }
    SideEffect {
        val active = bounds
        boundConnection.minIndex = active?.min ?: 0
        boundConnection.maxIndex = active?.max ?: 0
    }

    val currentItems by rememberUpdatedState(items)
    val currentBounds by rememberUpdatedState(bounds)
    val currentEnabled by rememberUpdatedState(isItemEnabled)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentSpacer by rememberUpdatedState(spacer)
    val currentHaptic by rememberUpdatedState(config.hapticEnabled)
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(listState) {
        var userScrollStarted = false
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    if (programmatic.depth == 0) userScrollStarted = true
                    return@collect
                }
                if (!userScrollStarted || programmatic.depth > 0) return@collect
                userScrollStarted = false
                val bound = currentBounds ?: return@collect
                val raw = centerDataIndex(listState, currentSpacer, currentItems.size) ?: return@collect
                val resolved = nearestSelectableIndex(raw, bound) { index ->
                    currentItems.getOrNull(index)?.let(currentEnabled) == true
                }
                if (raw != resolved) {
                    programmatic.depth += 1
                    try {
                        listState.animateScrollToItem(resolved)
                    } finally {
                        programmatic.depth -= 1
                    }
                }
                if (resolved != currentSelected) {
                    if (currentHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                    currentOnSelected(resolved)
                }
            }
    }

    LaunchedEffect(resolvedIndex, items.size, spacer) {
        val target = resolvedIndex ?: return@LaunchedEffect
        snapshotFlow { centerDataIndex(listState, spacer, items.size) }
            .first { it != null }
        if (centerDataIndex(listState, spacer, items.size) == target) return@LaunchedEffect
        programmatic.depth += 1
        try {
            listState.animateScrollToItem(target)
        } finally {
            programmatic.depth -= 1
        }
    }

    var centerIndex by remember {
        mutableIntStateOf(resolvedIndex ?: selectedIndex.coerceAtLeast(0))
    }
    LaunchedEffect(listState, spacer, items.size) {
        snapshotFlow { centerDataIndex(listState, spacer, items.size) }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null) centerIndex = index
            }
    }

    val spoken = contentDescription ?: resolvedIndex?.let { index ->
        items.getOrNull(index)?.let(itemText)
    }.orEmpty()
    val nextLabel = stringResource(R.string.wheel_next)
    val previousLabel = stringResource(R.string.wheel_previous)
    val scope = rememberCoroutineScope()
    val actions = listOf(
        CustomAccessibilityAction(nextLabel) {
            scrollWheelByStep(scope, listState, selectedIndex, bounds, +1) { index ->
                items.getOrNull(index)?.let(isItemEnabled) == true
            }
        },
        CustomAccessibilityAction(previousLabel) {
            scrollWheelByStep(scope, listState, selectedIndex, bounds, -1) { index ->
                items.getOrNull(index)?.let(isItemEnabled) == true
            }
        },
    )
    val chrome = if (drawChrome) {
        Modifier.wheelPickerChrome(
            itemHeight = itemHeight,
            decoration = decoration,
            fadeEnabled = config.fadeEnabled,
            fadeHeight = config.fadeHeight,
        )
    } else {
        Modifier
    }
    val entries = items

    Column(
        modifier = modifier
            .height(wheelHeight)
            .fillMaxWidth()
            .semantics {
                this.contentDescription = spoken
                customActions = actions
            }
            .clipToBounds()
            .then(chrome),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(boundConnection)
                .clearAndSetSemantics {},
            state = listState,
            flingBehavior = flingBehavior,
            userScrollEnabled = bounds != null && bounds.min < bounds.max,
        ) {
            items(
                count = spacer,
                key = { index -> "pad-start-$index" },
                contentType = { "pad" },
            ) {
                Spacer(modifier = Modifier.fillMaxWidth().height(itemHeight))
            }
            itemsIndexed(
                items = entries,
                key = { index, item -> "v-${itemKey(index, item)}" },
                contentType = { _, _ -> "value" },
            ) { index, item ->
                val style = when {
                    !isItemEnabled(item) -> textAppearance.disabled
                    index == centerIndex -> textAppearance.selected
                    else -> textAppearance.unselected
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = itemText(item),
                        style = style,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            items(
                count = spacer,
                key = { index -> "pad-end-$index" },
                contentType = { "pad" },
            ) {
                Spacer(modifier = Modifier.fillMaxWidth().height(itemHeight))
            }
        }
    }
}

/**
 * 选中行装饰和上下渐隐。
 *
 * 渐隐用 [BlendMode.DstIn] 吃掉内容自身的透明度，不依赖页面底色。
 * 遮罩里的黑色只提供透明度，不是绘制颜色。
 * 多列滚轮要把选中线连成一条时，画在包住所有列的父布局上。
 */
fun Modifier.wheelPickerChrome(
    itemHeight: Dp,
    decoration: WheelSelectionDecoration,
    fadeEnabled: Boolean,
    fadeHeight: Dp,
): Modifier {
    val fade = fadeEnabled && fadeHeight > 0.dp
    if (!fade && !decoration.marksSelection) return this
    val layer = if (fade) {
        Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    } else {
        Modifier
    }
    return this.then(layer).drawWithCache {
        val rowHeight = itemHeight.toPx()
        val centerTop = (size.height - rowHeight) / 2f
        val inset = decoration.horizontalInset.toPx().coerceAtLeast(0f)
        val markWidth = (size.width - inset * 2f).coerceAtLeast(0f)
        val outline = decoration.shape.createOutline(
            size = Size(markWidth, rowHeight.coerceAtLeast(0f)),
            layoutDirection = layoutDirection,
            density = this,
        )
        val borderPx = decoration.borderWidth.toPx()
        val dividerPx = decoration.dividerThickness.toPx()
        val fadeBrush = if (fade && size.height > 0f) {
            val fadePx = fadeHeight.toPx().coerceAtLeast(0f)
            val start = (fadePx / size.height).coerceIn(0.001f, 0.49f)
            val end = (1f - start).coerceAtLeast(start + 0.02f)
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0f),
                start to Color.Black,
                end to Color.Black,
                1f to Color.Black.copy(alpha = 0f),
            )
        } else {
            null
        }
        val drawBackground = decoration.backgroundColor.alpha > 0f
        val drawBorder = borderPx > 0f && decoration.borderColor.alpha > 0f
        val drawDivider = dividerPx > 0f && decoration.dividerColor.alpha > 0f
        onDrawWithContent {
            if (drawBackground) {
                translate(left = inset, top = centerTop) {
                    drawOutline(outline, decoration.backgroundColor)
                }
            }
            drawContent()
            if (fadeBrush != null) {
                drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
            }
            if (drawBorder) {
                translate(left = inset, top = centerTop) {
                    drawOutline(
                        outline = outline,
                        color = decoration.borderColor,
                        style = Stroke(width = borderPx),
                    )
                }
            }
            if (drawDivider) {
                drawLine(
                    color = decoration.dividerColor,
                    start = Offset(0f, centerTop),
                    end = Offset(size.width, centerTop),
                    strokeWidth = dividerPx,
                )
                val bottom = centerTop + rowHeight
                drawLine(
                    color = decoration.dividerColor,
                    start = Offset(0f, bottom),
                    end = Offset(size.width, bottom),
                    strokeWidth = dividerPx,
                )
            }
        }
    }
}

/**
 * 上下各补半屏空白后，[SnapPosition.Start] 把某项贴到视口顶部时，
 * 正中那一行是它后面的第 spacer 项。因此数据下标 i 对应 [LazyListState.scrollToItem] 的 i。
 * [LazyListState.firstVisibleItemIndex] 在吸附完成时也等于数据下标，而不是列表里的原始下标。
 */
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

private fun scrollWheelByStep(
    scope: CoroutineScope,
    listState: LazyListState,
    selectedIndex: Int,
    bounds: WheelBounds?,
    delta: Int,
    isEnabled: (Int) -> Boolean,
): Boolean {
    val bound = bounds ?: return false
    val target = nextSelectableIndex(selectedIndex, delta, bound, isEnabled) ?: return false
    scope.launch { listState.animateScrollToItem(target) }
    return true
}

private fun selectableBounds(
    itemCount: Int,
    minIndex: Int?,
    maxIndex: Int?,
    isEnabled: (Int) -> Boolean,
): WheelBounds? {
    if (itemCount <= 0) return null
    var min = (minIndex ?: 0).coerceIn(0, itemCount - 1)
    var max = (maxIndex ?: itemCount - 1).coerceIn(0, itemCount - 1)
    if (min > max) return null
    while (min <= max && !isEnabled(min)) min++
    while (max >= min && !isEnabled(max)) max--
    if (min > max) return null
    return WheelBounds(min, max)
}

private fun nearestSelectableIndex(
    preferred: Int,
    bounds: WheelBounds,
    isEnabled: (Int) -> Boolean,
): Int {
    val clamped = preferred.coerceIn(bounds.min, bounds.max)
    if (isEnabled(clamped)) return clamped
    var distance = 1
    val span = bounds.max - bounds.min
    while (distance <= span) {
        val lower = clamped - distance
        val upper = clamped + distance
        if (lower >= bounds.min && isEnabled(lower)) return lower
        if (upper <= bounds.max && isEnabled(upper)) return upper
        distance++
    }
    return bounds.min
}

private fun nextSelectableIndex(
    from: Int,
    delta: Int,
    bounds: WheelBounds,
    isEnabled: (Int) -> Boolean,
): Int? {
    if (delta == 0) return null
    var index = from + delta
    while (index in bounds.min..bounds.max) {
        if (isEnabled(index)) return index
        index += delta
    }
    return null
}

private data class WheelBounds(val min: Int, val max: Int)

/** 主线程上的计数器。不放进组合状态，避免程序滚动本身引起重组。 */
private class ProgrammaticScroll {
    var depth: Int = 0
}

/**
 * 把滚动限制在可选下标之间。
 *
 * 手指上滑时 [available] 的 y 为负，列表往更大的下标走。
 * 数据下标 i 吸附后，[LazyListState.firstVisibleItemIndex] 就是 i。
 */
private class WheelScrollBound(
    private val listState: LazyListState,
) : NestedScrollConnection {
    var minIndex: Int = 0
    var maxIndex: Int = 0

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val itemSize = listState.itemStridePx()
        if (itemSize <= 0 || available.y == 0f) return Offset.Zero
        val offset = listState.firstVisibleItemIndex * itemSize + listState.firstVisibleItemScrollOffset
        val minOffset = minIndex * itemSize
        val maxOffset = maxIndex * itemSize
        return if (available.y < 0f) {
            val room = (maxOffset - offset).coerceAtLeast(0)
            val want = -available.y
            if (want > room) Offset(0f, available.y + room) else Offset.Zero
        } else {
            val room = (offset - minOffset).coerceAtLeast(0)
            val want = available.y
            if (want > room) Offset(0f, available.y - room) else Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val itemSize = listState.itemStridePx()
        if (itemSize <= 0 || available.y == 0f) return Velocity.Zero
        val offset = listState.firstVisibleItemIndex * itemSize + listState.firstVisibleItemScrollOffset
        val minOffset = minIndex * itemSize
        val maxOffset = maxIndex * itemSize
        val blocked = (available.y < 0f && offset >= maxOffset) ||
            (available.y > 0f && offset <= minOffset)
        return if (blocked) available else Velocity.Zero
    }
}

private fun LazyListState.itemStridePx(): Int {
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return 0
    return visible[0].size
}

@Preview(name = "Wheel", showBackground = true)
@Composable
private fun WheelPickerPreview() {
    CogniBalTheme {
        Surface {
            val items = remember { (0..20).toList() }
            var index by remember { mutableIntStateOf(6) }
            val colors = MaterialTheme.appColors
            val title = MaterialTheme.typography.titleLarge
            WheelPicker(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                items = items,
                selectedIndex = index,
                onSelectedIndexChange = { index = it },
                itemText = { number -> number.toString().padStart(2, '0') },
                isItemEnabled = { number -> number % 7 != 0 },
                minIndex = 2,
                maxIndex = 16,
                decoration = WheelSelectionDecoration.border(colors.primary).copy(
                    backgroundColor = colors.primary.copy(alpha = 0.16f),
                ),
                textAppearance = WheelTextAppearance(
                    selected = title.copy(color = colors.textPrimary, fontWeight = FontWeight.Medium),
                    unselected = title.copy(color = colors.textSecondary),
                    disabled = title.copy(color = colors.textTertiary),
                ),
            )
        }
    }
}

@Preview(name = "Wheel dark", showBackground = true)
@Composable
private fun WheelPickerDarkPreview() {
    CogniBalTheme(darkTheme = true) {
        Surface {
            WheelPicker(
                modifier = Modifier.padding(16.dp),
                items = listOf("春", "夏", "秋", "冬"),
                selectedIndex = 1,
                onSelectedIndexChange = {},
            )
        }
    }
}
