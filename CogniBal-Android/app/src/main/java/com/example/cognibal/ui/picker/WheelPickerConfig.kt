package com.example.cognibal.ui.picker

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 滚轮的视觉和反馈配置。
 *
 * 可见行数在组件里会被收成不小于 3 的奇数，这样中间永远有一行选中行。
 * 偶数会减 1，例如 4 会变成 3，6 会变成 5。
 * [itemHeight] 小于 36.dp 时会抬到 36.dp，避免文字被行高裁掉。
 *
 * [fadeEnabled] 同时管两件事：文字离开中心后的透明度，以及上下边缘遮罩。
 * [scaleEnabled] 只管缩放。两者都关掉时，行仍然吸附，只是没有远近变化。
 */
@Stable
data class WheelPickerConfig(
    val visibleCount: Int = 5,
    val itemHeight: Dp = 48.dp,
    val selectionStyle: WheelSelectionStyle = WheelSelectionStyle.DividerLines,
    val fadeEnabled: Boolean = true,
    val scaleEnabled: Boolean = true,
    val minAlpha: Float = 0.32f,
    val minScale: Float = 0.86f,
    val hapticEnabled: Boolean = true,
) {
    companion object {
        val Default = WheelPickerConfig()
    }
}
