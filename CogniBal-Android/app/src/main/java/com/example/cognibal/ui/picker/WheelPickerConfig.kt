package com.example.cognibal.ui.picker

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cognibal.ui.theme.appColors

/**
 * 滚轮的行数、行高和反馈。
 *
 * 颜色不放在这里。[WheelPicker] 的选中装饰和文字颜色由 [WheelSelectionDecoration]、
 * [WheelTextAppearance] 传入，浅色和暗色都由调用方决定。
 *
 * 可见行数会收成不小于 3 的奇数，中间才有一行完整的选中行。偶数减 1，例如 4 变成 3。
 * [itemHeight] 小于 [MinWheelItemHeight] 时抬到这个高度，避免文字被行高裁掉。
 *
 * [selectionStyle] 只给 [CogniBalWheelDateTimePicker] 用，映射成一套装饰。
 * 直接使用 [WheelPicker] 时传入 [WheelSelectionDecoration]。
 */
@Stable
data class WheelPickerConfig(
    val visibleCount: Int = 5,
    val itemHeight: Dp = 48.dp,
    val selectionStyle: WheelSelectionStyle = WheelSelectionStyle.DividerLines,
    val fadeEnabled: Boolean = true,
    val fadeHeight: Dp = 48.dp,
    val hapticEnabled: Boolean = true,
) {
    companion object {
        val Default = WheelPickerConfig()
    }
}

/** 小于这个行高时，文字会被裁切。 */
val MinWheelItemHeight = 36.dp

fun WheelPickerConfig.wheelItemHeight(): Dp =
    if (itemHeight < MinWheelItemHeight) MinWheelItemHeight else itemHeight

fun WheelPickerConfig.wheelVisibleCount(): Int {
    val atLeastThree = visibleCount.coerceAtLeast(3)
    return if (atLeastThree % 2 == 0) atLeastThree - 1 else atLeastThree
}

fun WheelPickerConfig.wheelViewportHeight(): Dp = wheelItemHeight() * wheelVisibleCount()

/**
 * 中间选中区域的装饰。背景、描边、分割线可以同时生效，也可以只开其中一种。
 *
 * 颜色由调用方传入。组件不读取主题，也不写死色值。
 *
 * ```
 * WheelSelectionDecoration.background(fill).copy(
 *     borderColor = stroke,
 *     borderWidth = 1.dp,
 * )
 * ```
 */
@Stable
data class WheelSelectionDecoration(
    val backgroundColor: Color = Color.Transparent,
    val shape: Shape = RoundedCornerShape(8.dp),
    val borderColor: Color = Color.Transparent,
    val borderWidth: Dp = 0.dp,
    val dividerColor: Color = Color.Transparent,
    val dividerThickness: Dp = 0.dp,
    val horizontalInset: Dp = 8.dp,
) {
    /** 至少有一种看得见的选中标记。 */
    val marksSelection: Boolean
        get() = backgroundColor.alpha > 0f ||
            (borderWidth > 0.dp && borderColor.alpha > 0f) ||
            (dividerThickness > 0.dp && dividerColor.alpha > 0f)

    companion object {
        val None = WheelSelectionDecoration()

        fun dividers(
            color: Color,
            thickness: Dp = 1.dp,
        ): WheelSelectionDecoration = WheelSelectionDecoration(
            dividerColor = color,
            dividerThickness = thickness,
            horizontalInset = 0.dp,
        )

        fun background(
            color: Color,
            shape: Shape = RoundedCornerShape(8.dp),
            horizontalInset: Dp = 8.dp,
        ): WheelSelectionDecoration = WheelSelectionDecoration(
            backgroundColor = color,
            shape = shape,
            horizontalInset = horizontalInset,
        )

        fun border(
            color: Color,
            width: Dp = 1.dp,
            shape: Shape = RoundedCornerShape(8.dp),
            horizontalInset: Dp = 8.dp,
        ): WheelSelectionDecoration = WheelSelectionDecoration(
            borderColor = color,
            borderWidth = width,
            shape = shape,
            horizontalInset = horizontalInset,
        )
    }
}

/**
 * 选中行、未选中行和禁用行的文字。颜色写在 [TextStyle] 里，由调用方按浅色或暗色传入。
 */
@Stable
data class WheelTextAppearance(
    val selected: TextStyle,
    val unselected: TextStyle,
    val disabled: TextStyle,
)

/**
 * 跟随当前主题的装饰和文字。这是调用方可选的便捷入口，[WheelPicker] 的绘制只使用传入的值。
 */
object WheelPickerDefaults {
    @Composable
    fun textAppearance(
        selected: TextStyle = MaterialTheme.typography.titleLarge,
        unselected: TextStyle = MaterialTheme.typography.titleLarge,
        disabled: TextStyle = MaterialTheme.typography.titleLarge,
    ): WheelTextAppearance {
        val colors = MaterialTheme.appColors
        return WheelTextAppearance(
            selected = selected.copy(color = colors.textPrimary, fontWeight = FontWeight.Medium),
            unselected = unselected.copy(color = colors.textSecondary, fontWeight = FontWeight.Normal),
            disabled = disabled.copy(color = colors.textTertiary, fontWeight = FontWeight.Normal),
        )
    }

    @Composable
    fun dividerDecoration(
        color: Color = MaterialTheme.appColors.divider,
    ): WheelSelectionDecoration = WheelSelectionDecoration.dividers(color)

    @Composable
    fun highlightDecoration(
        color: Color = MaterialTheme.appColors.primary.copy(alpha = HighlightAlpha),
    ): WheelSelectionDecoration = WheelSelectionDecoration.background(color)

    @Composable
    fun borderDecoration(
        color: Color = MaterialTheme.appColors.primary,
    ): WheelSelectionDecoration = WheelSelectionDecoration.border(color)
}

@Composable
fun WheelSelectionStyle.toDecoration(): WheelSelectionDecoration = when (this) {
    WheelSelectionStyle.DividerLines -> WheelPickerDefaults.dividerDecoration()
    WheelSelectionStyle.HighlightBackground -> WheelPickerDefaults.highlightDecoration()
    WheelSelectionStyle.Border -> WheelPickerDefaults.borderDecoration()
    WheelSelectionStyle.None -> WheelSelectionDecoration.None
}

private const val HighlightAlpha = 0.16f
