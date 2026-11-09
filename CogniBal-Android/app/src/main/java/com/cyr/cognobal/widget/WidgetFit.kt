package com.cyr.cognobal.widget

/**
 * 按卡片实际宽高挑选放得下的排法。
 *
 * 金额、月份、标签都保持原文，不截断、不改写。放得下就用更大的字；
 * 放不下时按记账、月份、收入和支出、结余标签的顺序拿掉，结余数字留到最后。
 */
internal data class WidgetCopy(
    val month: String,
    val incomeLabel: String,
    val expenseLabel: String,
    val balanceLabel: String,
    val income: String,
    val expense: String,
    val balance: String,
    val action: String,
    val balanceOverflow: Boolean,
) {
    fun label(slot: WidgetSlot): String = when (slot) {
        WidgetSlot.Income -> incomeLabel
        WidgetSlot.Expense -> expenseLabel
        WidgetSlot.Balance -> balanceLabel
    }

    fun amount(slot: WidgetSlot): String = when (slot) {
        WidgetSlot.Income -> income
        WidgetSlot.Expense -> expense
        WidgetSlot.Balance -> balance
    }
}

internal enum class WidgetSlot { Income, Expense, Balance }

internal enum class WidgetArrange { Columns, Lines, Blocks }

internal data class WidgetTextSize(val width: Float, val height: Float)

internal fun interface WidgetTextMeasure {
    fun measure(text: String, sizeSp: Float, bold: Boolean): WidgetTextSize
}

internal data class WidgetLayout(
    val paddingH: Float,
    val paddingV: Float,
    val gap: Float,
    val labelGap: Float,
    val showMonth: Boolean,
    val showLabels: Boolean,
    val showAction: Boolean,
    val slots: List<WidgetSlot>,
    val arrange: WidgetArrange,
    val monthSp: Float,
    val labelSp: Float,
    val amountSp: Float,
    val actionSp: Float,
    val balanceLines: Int,
    val actionHeight: Float,
    val contentWidth: Float,
    val contentHeight: Float,
    val fits: Boolean,
)

/** 记账按钮在 Glance 里自带的内边距。高度至少 48dp，字号很大时跟着文字加高。 */
internal const val WIDGET_ACTION_PADDING_H: Float = 16f
internal const val WIDGET_ACTION_PADDING_V: Float = 8f
internal const val WIDGET_ACTION_HEIGHT: Float = 48f

internal fun fitWidget(
    width: Float,
    height: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
): WidgetLayout {
    val cached = caching(measure)
    for (payload in primaryPayloads) {
        for (scale in scales) {
            for (arrange in arrangements(payload)) {
                resolve(width, height, copy, cached, payload, scale, arrange)?.let { return it }
            }
        }
    }
    for (payload in tightPayloads) {
        for (scale in tightScales) {
            for (arrange in arrangements(payload)) {
                resolve(width, height, copy, cached, payload, scale, arrange)?.let { return it }
            }
        }
    }
    return fallback(width, height, copy, cached)
}

private data class Scale(
    val paddingH: Float,
    val paddingV: Float,
    val gap: Float,
    val labelGap: Float,
    val month: Float,
    val label: Float,
    val amount: Float,
    val action: Float,
)

private enum class Payload { Full, NoAction, Figures, MonthBalance, Balance, Amount }

private val scales: List<Scale> = listOf(
    Scale(24f, 24f, 16f, 8f, 32f, 18f, 40f, 16f),
    Scale(24f, 20f, 16f, 8f, 28f, 16f, 32f, 16f),
    Scale(16f, 16f, 16f, 8f, 24f, 14f, 24f, 15f),
    Scale(16f, 12f, 8f, 4f, 22f, 13f, 18f, 14f),
    Scale(16f, 12f, 8f, 4f, 20f, 12f, 16f, 14f),
    Scale(8f, 8f, 8f, 4f, 18f, 12f, 15f, 14f),
    Scale(8f, 8f, 8f, 4f, 16f, 12f, 14f, 13f),
    Scale(8f, 4f, 4f, 2f, 14f, 11f, 13f, 13f),
)

private val tightScales: List<Scale> = listOf(
    Scale(8f, 4f, 4f, 2f, 13f, 11f, 12f, 12f),
    Scale(4f, 4f, 4f, 2f, 12f, 11f, 11f, 12f),
    Scale(4f, 4f, 2f, 2f, 12f, 11f, 10f, 12f),
)

private val primaryPayloads: List<Payload> = listOf(
    Payload.Full,
    Payload.NoAction,
    Payload.Figures,
    Payload.MonthBalance,
    Payload.Balance,
)

private val tightPayloads: List<Payload> = listOf(Payload.Balance, Payload.Amount)

private fun arrangements(payload: Payload): List<WidgetArrange> {
    val count = payload.slots().size
    if (count > 1) return listOf(WidgetArrange.Columns, WidgetArrange.Lines, WidgetArrange.Blocks)
    if (payload == Payload.Amount) return listOf(WidgetArrange.Blocks)
    return listOf(WidgetArrange.Lines, WidgetArrange.Blocks)
}

private fun Payload.slots(): List<WidgetSlot> = when (this) {
    Payload.Full, Payload.NoAction, Payload.Figures -> listOf(
        WidgetSlot.Income,
        WidgetSlot.Expense,
        WidgetSlot.Balance,
    )
    Payload.MonthBalance, Payload.Balance, Payload.Amount -> listOf(WidgetSlot.Balance)
}

private fun Payload.showsMonth(): Boolean =
    this == Payload.Full || this == Payload.NoAction || this == Payload.MonthBalance

private fun Payload.showsAction(): Boolean = this == Payload.Full

private fun Payload.showsLabels(): Boolean = this != Payload.Amount

private fun resolve(
    width: Float,
    height: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    payload: Payload,
    scale: Scale,
    arrange: WidgetArrange,
): WidgetLayout? {
    val box = measureBox(width, height, copy, measure, payload, scale, arrange) ?: return null
    val contentWidth = box.width + scale.paddingH * 2f + BOX_SLACK
    val contentHeight = box.height + scale.paddingV * 2f + BOX_SLACK
    if (contentWidth > width + 0.05f || contentHeight > height + 0.05f) return null
    return layout(
        payload,
        scale,
        arrange,
        box.balanceLines,
        box.actionHeight,
        contentWidth,
        contentHeight,
        fits = true,
    )
}

private fun fallback(
    width: Float,
    height: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
): WidgetLayout {
    val scale = tightScales.last()
    val payload = Payload.Amount
    val arrange = WidgetArrange.Blocks
    val box = measureBox(width, height, copy, measure, payload, scale, arrange)
    val contentWidth = (box?.width ?: 0f) + scale.paddingH * 2f + BOX_SLACK
    val contentHeight = (box?.height ?: 0f) + scale.paddingV * 2f + BOX_SLACK
    val fits = box != null && contentWidth <= width + 0.05f && contentHeight <= height + 0.05f
    return layout(
        payload,
        scale,
        arrange,
        box?.balanceLines ?: 1,
        box?.actionHeight ?: 0f,
        contentWidth,
        contentHeight,
        fits,
    )
}

private data class BoxMeasure(
    val width: Float,
    val height: Float,
    val balanceLines: Int,
    val actionHeight: Float,
)

private fun measureBox(
    width: Float,
    height: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    payload: Payload,
    scale: Scale,
    arrange: WidgetArrange,
): BoxMeasure? {
    val innerW = width - scale.paddingH * 2f - BOX_SLACK
    val innerH = height - scale.paddingV * 2f - BOX_SLACK
    if (innerW <= 8f || innerH <= 8f) return null

    var usedW = 0f
    var usedH = 0f
    var actionHeight = 0f
    if (payload.showsMonth()) {
        val month = measure.measure(copy.month, scale.month, bold = true)
        if (month.width > innerW) return null
        usedW = month.width
        usedH = month.height
    }

    val figures = measureFigures(innerW, copy, measure, payload, scale, arrange) ?: return null
    if (payload.showsMonth()) usedH += scale.gap
    usedH += figures.height
    if (figures.width > usedW) usedW = figures.width

    if (payload.showsAction()) {
        val action = measure.measure(copy.action, scale.action, bold = false)
        val actionWidth = action.width + WIDGET_ACTION_PADDING_H * 2f
        if (actionWidth > innerW) return null
        actionHeight = maxOf(action.height + WIDGET_ACTION_PADDING_V * 2f, WIDGET_ACTION_HEIGHT)
        usedH += scale.gap + actionHeight
        if (actionWidth > usedW) usedW = actionWidth
    }
    if (usedH > innerH) return null
    return BoxMeasure(usedW, usedH, figures.balanceLines, actionHeight)
}

private data class FigureMeasure(val width: Float, val height: Float, val balanceLines: Int)

private fun measureFigures(
    innerW: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    payload: Payload,
    scale: Scale,
    arrange: WidgetArrange,
): FigureMeasure? {
    val slots = payload.slots()
    var balanceLines = 1
    when (arrange) {
        WidgetArrange.Columns -> {
            var sum = 0f
            var height = 0f
            slots.forEachIndexed { index, slot ->
                val piece = measurePiece(copy, measure, payload, scale, slot, innerW, allowWrap = false)
                    ?: return null
                if (index > 0) sum += scale.gap
                sum += piece.width
                if (piece.height > height) height = piece.height
            }
            if (sum > innerW) return null
            return FigureMeasure(sum, height, balanceLines)
        }
        WidgetArrange.Lines -> {
            var width = 0f
            var height = 0f
            slots.forEachIndexed { index, slot ->
                val label = if (payload.showsLabels()) {
                    measure.measure(copy.label(slot), scale.label, bold = false)
                } else {
                    null
                }
                val amount = fitAmount(
                    copy.amount(slot),
                    scale.amount,
                    maxWidth = innerW,
                    allowWrap = false,
                    measure = measure,
                ) ?: return null
                val rowWidth = if (label == null) {
                    amount.width
                } else {
                    label.width + scale.labelGap + amount.width
                }
                if (rowWidth > innerW) return null
                val rowHeight = if (label == null) amount.height else maxOf(label.height, amount.height)
                if (index > 0) height += scale.gap
                height += rowHeight
                if (rowWidth > width) width = rowWidth
            }
            return FigureMeasure(width, height, balanceLines)
        }
        WidgetArrange.Blocks -> {
            var width = 0f
            var height = 0f
            slots.forEachIndexed { index, slot ->
                if (index > 0) height += scale.gap
                if (payload.showsLabels()) {
                    val label = measure.measure(copy.label(slot), scale.label, bold = false)
                    if (label.width > innerW) return null
                    if (label.width > width) width = label.width
                    height += label.height + scale.labelGap
                }
                val allowWrap = slot == WidgetSlot.Balance && copy.balanceOverflow
                val amount = fitAmount(
                    copy.amount(slot),
                    scale.amount,
                    innerW,
                    allowWrap,
                    measure,
                ) ?: return null
                if (slot == WidgetSlot.Balance) balanceLines = amount.lines
                if (amount.width > width) width = amount.width
                height += amount.height
            }
            return FigureMeasure(width, height, balanceLines)
        }
    }
}

private data class PieceMeasure(val width: Float, val height: Float)

private fun measurePiece(
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    payload: Payload,
    scale: Scale,
    slot: WidgetSlot,
    maxWidth: Float,
    allowWrap: Boolean,
): PieceMeasure? {
    val amount = fitAmount(copy.amount(slot), scale.amount, maxWidth, allowWrap && slot == WidgetSlot.Balance, measure)
        ?: return null
    if (!payload.showsLabels()) return PieceMeasure(amount.width, amount.height)
    val label = measure.measure(copy.label(slot), scale.label, bold = false)
    if (label.width > maxWidth) return null
    val width = maxOf(label.width, amount.width)
    if (width > maxWidth) return null
    return PieceMeasure(width, label.height + scale.labelGap + amount.height)
}

private data class AmountFit(val width: Float, val height: Float, val lines: Int)

private fun fitAmount(
    text: String,
    sizeSp: Float,
    maxWidth: Float,
    allowWrap: Boolean,
    measure: WidgetTextMeasure,
): AmountFit? {
    val one = measure.measure(text, sizeSp, bold = true)
    if (one.width <= maxWidth) return AmountFit(one.width, one.height, lines = 1)
    if (!allowWrap || maxWidth < sizeSp * 0.8f) return null
    if (!wordsFit(text, sizeSp, maxWidth, measure)) return null
    val lines = when {
        one.width <= maxWidth * 2f -> 2
        one.width <= maxWidth * 3f -> 3
        else -> return null
    }
    val lineSpacing = (lines - 1) * 2f
    return AmountFit(maxWidth, one.height * lines + lineSpacing, lines)
}

private fun wordsFit(
    text: String,
    sizeSp: Float,
    maxWidth: Float,
    measure: WidgetTextMeasure,
): Boolean {
    val words = text.split(' ', '\n').filter { it.isNotEmpty() }
    if (words.size <= 1) return true
    return words.all { measure.measure(it, sizeSp, bold = true).width <= maxWidth }
}

private fun layout(
    payload: Payload,
    scale: Scale,
    arrange: WidgetArrange,
    balanceLines: Int,
    actionHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
    fits: Boolean,
): WidgetLayout = WidgetLayout(
    paddingH = scale.paddingH,
    paddingV = scale.paddingV,
    gap = scale.gap,
    labelGap = scale.labelGap,
    showMonth = payload.showsMonth(),
    showLabels = payload.showsLabels(),
    showAction = payload.showsAction(),
    slots = payload.slots(),
    arrange = arrange,
    monthSp = scale.month,
    labelSp = scale.label,
    amountSp = scale.amount,
    actionSp = scale.action,
    balanceLines = balanceLines,
    actionHeight = actionHeight,
    contentWidth = contentWidth,
    contentHeight = contentHeight,
    fits = fits,
)

private fun caching(measure: WidgetTextMeasure): WidgetTextMeasure {
    val cache = HashMap<String, WidgetTextSize>()
    return WidgetTextMeasure { text, sizeSp, bold ->
        cache.getOrPut("$sizeSp|$bold|$text") { measure.measure(text, sizeSp, bold) }
    }
}

private const val BOX_SLACK: Float = 2f
