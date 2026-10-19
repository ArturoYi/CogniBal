package com.example.cognibal.ui.picker

/** 中间选中区域的装饰。只影响绘制，不改变吸附和回调。 */
enum class WheelSelectionStyle {
    /** 选中行上下各一条分割线。 */
    DividerLines,

    /** 选中行铺一块半透明底。 */
    HighlightBackground,

    /** 不画装饰。文字远近变化仍然生效。 */
    None,
}
