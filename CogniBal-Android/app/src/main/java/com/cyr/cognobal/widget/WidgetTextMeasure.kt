package com.cyr.cognobal.widget

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue

/**
 * 用当前字号和字体缩放量出文字占位。
 *
 * 每次调用单独建一支画笔。Exact 模式会并行为每种尺寸排版，画笔不能共用。
 * 宽度和高度各留一点空，避开 TextView 自带的字体内边距把字裁掉。
 */
internal fun widgetTextMeasure(context: Context): WidgetTextMeasure {
    val metrics = context.resources.displayMetrics
    val density = metrics.density.coerceAtLeast(0.1f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    return WidgetTextMeasure { text, sizeSp, bold ->
        paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
        paint.typeface = if (bold) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        } else {
            Typeface.DEFAULT
        }
        val width = paint.measureText(text) / density + 2f
        val metricsHeight = paint.fontMetrics
        val height = (metricsHeight.bottom - metricsHeight.top) / density + 1f
        WidgetTextSize(width, height)
    }
}
