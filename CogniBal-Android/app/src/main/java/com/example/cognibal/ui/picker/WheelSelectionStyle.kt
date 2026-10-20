package com.example.cognibal.ui.picker

/**
 * [CogniBalWheelDateTimePicker] 的选中装饰预设。
 *
 * 只决定中间那一行怎么画，不改变吸附和回调。实际色值在 [toDecoration] 里按当前主题生成。
 * 基础滚轮要自定义边框、背景或同时使用两者时，直接传 [WheelSelectionDecoration]。
 */
enum class WheelSelectionStyle {
    /** 选中行上下各一条分割线。 */
    DividerLines,

    /** 选中行铺一块半透明底。 */
    HighlightBackground,

    /** 选中行描一圈边框。 */
    Border,

    /** 不画装饰。 */
    None,
}
