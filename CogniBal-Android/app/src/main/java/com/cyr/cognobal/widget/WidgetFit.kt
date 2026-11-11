package com.cyr.cognobal.widget

/**
 * 按卡片宽高挑选放得下的排法。
 *
 * 结余是主数字。卡片够高时整块居中，底下并排放支出和收入两个按钮。
 * 再高一些时，收入和支出的金额留在按钮上方。矮卡片放不下按钮，就只留结余。
 * 放得下就用更大的字。金额保持原文，不截断。结余太长时只允许那一句换行。
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

/** 两侧并排、两侧上下叠、只居中结余。 */
internal enum class WidgetArrange { Board, Stack, Hero }

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
    val showBalanceLabel: Boolean,
    val balanceLabelInHeader: Boolean,
    val showLabels: Boolean,
    val showAction: Boolean,
    val centered: Boolean,
    val slots: List<WidgetSlot>,
    val arrange: WidgetArrange,
    val monthSp: Float,
    val labelSp: Float,
    val balanceSp: Float,
    val amountSp: Float,
    val actionSp: Float,
    val balanceLines: Int,
    val contentWidth: Float,
    val contentHeight: Float,
    val fits: Boolean,
)

/** 支出、收入按钮的高度。短卡片放不下时整行拿掉，不把按钮缩成一条。 */
internal const val WIDGET_ACTION_HEIGHT: Float = 48f

internal fun fitWidget(
    width: Float,
    height: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
): WidgetLayout {
    val cached = caching(measure)
    // 收入、支出两列加上底部两个按钮一共四层，少于 176dp 的矮卡片放进去会把结余挤成小字。
    val candidates = if (height >= 176f) payloads else payloads.filter { it != Payload.ActionsBoard }
    for (payload in candidates) {
        for (scale in scales) {
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
    val balance: Float,
    val amount: Float,
    val action: Float,
)

private enum class Payload {
    ActionsBoard,
    Actions,
    NoAction,
    Compact,
    Sides,
    MonthBalance,
    Balance,
    Amount,
}

private val scales: List<Scale> = listOf(
    Scale(24f, 24f, 16f, 8f, 14f, 12f, 40f, 20f, 14f),
    Scale(16f, 16f, 16f, 8f, 14f, 12f, 32f, 16f, 14f),
    Scale(16f, 16f, 8f, 8f, 12f, 12f, 28f, 16f, 14f),
    Scale(16f, 16f, 8f, 8f, 12f, 12f, 24f, 14f, 14f),
    Scale(16f, 8f, 8f, 8f, 12f, 12f, 28f, 14f, 14f),
    Scale(16f, 8f, 8f, 8f, 12f, 12f, 24f, 14f, 14f),
    Scale(16f, 8f, 8f, 4f, 12f, 12f, 24f, 16f, 14f),
    Scale(16f, 4f, 8f, 4f, 12f, 12f, 24f, 14f, 14f),
    Scale(8f, 8f, 8f, 8f, 12f, 12f, 24f, 14f, 13f),
    Scale(8f, 8f, 8f, 4f, 12f, 12f, 20f, 14f, 13f),
    Scale(8f, 8f, 4f, 4f, 12f, 11f, 16f, 13f, 12f),
    Scale(8f, 8f, 4f, 4f, 12f, 11f, 14f, 12f, 12f),
    Scale(8f, 4f, 4f, 4f, 11f, 11f, 12f, 11f, 12f),
)

private val payloads: List<Payload> = listOf(
    Payload.ActionsBoard,
    Payload.Actions,
    Payload.NoAction,
    Payload.Compact,
    Payload.Sides,
    Payload.MonthBalance,
    Payload.Balance,
    Payload.Amount,
)

private fun arrangements(payload: Payload): List<WidgetArrange> = when {
    payload.hasSides() -> listOf(WidgetArrange.Board, WidgetArrange.Stack)
    payload == Payload.Amount -> listOf(WidgetArrange.Hero)
    else -> listOf(WidgetArrange.Hero)
}

private fun Payload.showsMonth(): Boolean =
    this == Payload.ActionsBoard || this == Payload.Actions ||
        this == Payload.NoAction || this == Payload.Compact || this == Payload.MonthBalance

private fun Payload.showsAction(): Boolean = this == Payload.ActionsBoard || this == Payload.Actions

private fun Payload.centered(): Boolean = showsAction()

private fun Payload.showsBalanceLabel(): Boolean = this != Payload.Sides && this != Payload.Amount

private fun Payload.balanceLabelInHeader(): Boolean =
    this == Payload.ActionsBoard || this == Payload.Actions || this == Payload.Compact

private fun Payload.showsSideLabels(): Boolean =
    this == Payload.ActionsBoard || this == Payload.NoAction || this == Payload.Compact || this == Payload.Sides

private fun Payload.hasSides(): Boolean =
    this == Payload.ActionsBoard || this == Payload.NoAction || this == Payload.Compact || this == Payload.Sides

private fun Payload.slots(): List<WidgetSlot> = if (hasSides()) {
    listOf(WidgetSlot.Income, WidgetSlot.Expense, WidgetSlot.Balance)
} else {
    listOf(WidgetSlot.Balance)
}

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
    return layout(payload, scale, arrange, box.balanceLines, contentWidth, contentHeight, fits = true)
}

private fun fallback(
    width: Float,
    height: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
): WidgetLayout {
    val scale = scales.last()
    val payload = Payload.Amount
    val arrange = WidgetArrange.Hero
    val box = measureBox(width, height, copy, measure, payload, scale, arrange)
    val contentWidth = (box?.width ?: 0f) + scale.paddingH * 2f + BOX_SLACK
    val contentHeight = (box?.height ?: 0f) + scale.paddingV * 2f + BOX_SLACK
    val fits = box != null && contentWidth <= width + 0.05f && contentHeight <= height + 0.05f
    return layout(payload, scale, arrange, box?.balanceLines ?: 1, contentWidth, contentHeight, fits)
}

private data class BoxMeasure(val width: Float, val height: Float, val balanceLines: Int)

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
    val header = headerOf(copy, measure, payload, scale, innerW) ?: return null
    if (header.present) {
        usedH += header.height
        usedW = header.width
    }

    if (payload.showsBalanceLabel() && !payload.balanceLabelInHeader()) {
        if (usedH > 0f) usedH += scale.labelGap
        val label = measure.measure(copy.balanceLabel, scale.label, bold = false)
        if (label.width > innerW) return null
        usedH += label.height
        if (label.width > usedW) usedW = label.width
        usedH += scale.labelGap
    } else if (usedH > 0f) {
        usedH += if (payload.centered()) scale.labelGap else scale.gap
    }

    val balance = fitAmount(
        copy.balance,
        scale.balance,
        maxWidth = innerW,
        allowWrap = copy.balanceOverflow,
        measure = measure,
    ) ?: return null
    usedH += balance.height
    if (balance.width > usedW) usedW = balance.width

    if (payload.hasSides()) {
        val figures = measureSides(innerW, copy, measure, payload, scale, arrange) ?: return null
        usedH += scale.gap + figures.height
        if (figures.width > usedW) usedW = figures.width
    }
    if (payload.showsAction()) {
        val buttons = measureButtons(innerW, copy, measure, scale) ?: return null
        usedH += scale.gap + buttons
        if (innerW > usedW) usedW = innerW
    }
    if (usedH > innerH) return null
    return BoxMeasure(usedW, usedH, balance.lines)
}

private data class HeaderMeasure(val present: Boolean, val width: Float, val height: Float)

private fun headerOf(
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    payload: Payload,
    scale: Scale,
    innerW: Float,
): HeaderMeasure? {
    val showMonth = payload.showsMonth()
    val labelInHeader = payload.balanceLabelInHeader()
    if (!showMonth && !labelInHeader) return HeaderMeasure(false, 0f, 0f)
    if (payload.centered() && labelInHeader) {
        val text = if (showMonth) "${copy.month} · ${copy.balanceLabel}" else copy.balanceLabel
        val line = measure.measure(text, scale.month, bold = false)
        if (line.width > innerW) return null
        return HeaderMeasure(true, line.width, line.height)
    }
    val month = if (showMonth) measure.measure(copy.month, scale.month, bold = false) else null
    val right = if (labelInHeader) measure.measure(copy.balanceLabel, scale.label, bold = false) else null
    val monthW = month?.width ?: 0f
    val rightW = right?.width ?: 0f
    val rowW = if (month != null && right != null) monthW + scale.gap + rightW else maxOf(monthW, rightW)
    if (rowW > innerW) return null
    val rowH = maxOf(month?.height ?: 0f, right?.height ?: 0f)
    return HeaderMeasure(true, rowW, rowH)
}

/** 两个按钮并排，各至少 48dp 宽，文字左右留出空隙。放不下就整行不要。 */
private fun measureButtons(
    innerW: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    scale: Scale,
): Float? {
    val buttonW = (innerW - scale.gap) / 2f
    if (buttonW < WIDGET_ACTION_HEIGHT) return null
    val expense = measure.measure(copy.expenseLabel, scale.action, bold = false)
    val income = measure.measure(copy.incomeLabel, scale.action, bold = false)
    if (expense.width + 16f > buttonW || income.width + 16f > buttonW) return null
    return WIDGET_ACTION_HEIGHT
}

private data class FigureMeasure(val width: Float, val height: Float)

private fun measureSides(
    innerW: Float,
    copy: WidgetCopy,
    measure: WidgetTextMeasure,
    payload: Payload,
    scale: Scale,
    arrange: WidgetArrange,
): FigureMeasure? {
    val columnWidth = if (arrange == WidgetArrange.Board) (innerW - scale.gap) / 2f else innerW
    if (columnWidth <= 8f) return null
    val income = measurePiece(copy, measure, payload, scale, WidgetSlot.Income, columnWidth) ?: return null
    val expense = measurePiece(copy, measure, payload, scale, WidgetSlot.Expense, columnWidth) ?: return null
    return if (arrange == WidgetArrange.Board) {
        FigureMeasure(income.width + scale.gap + expense.width, maxOf(income.height, expense.height))
    } else {
        FigureMeasure(maxOf(income.width, expense.width), income.height + scale.gap + expense.height)
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
): PieceMeasure? {
    val amount = fitAmount(copy.amount(slot), scale.amount, maxWidth, allowWrap = false, measure) ?: return null
    if (!payload.showsSideLabels()) return PieceMeasure(amount.width, amount.height)
    val label = measure.measure(copy.label(slot), scale.label, bold = false)
    if (label.width > maxWidth) return null
    return PieceMeasure(maxOf(label.width, amount.width), label.height + scale.labelGap + amount.height)
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
    contentWidth: Float,
    contentHeight: Float,
    fits: Boolean,
): WidgetLayout = WidgetLayout(
    paddingH = scale.paddingH,
    paddingV = scale.paddingV,
    gap = scale.gap,
    labelGap = scale.labelGap,
    showMonth = payload.showsMonth(),
    showBalanceLabel = payload.showsBalanceLabel(),
    balanceLabelInHeader = payload.balanceLabelInHeader(),
    showLabels = payload.showsSideLabels(),
    showAction = payload.showsAction(),
    centered = payload.centered() || arrange == WidgetArrange.Hero,
    slots = payload.slots(),
    arrange = arrange,
    monthSp = scale.month,
    labelSp = scale.label,
    balanceSp = scale.balance,
    amountSp = scale.amount,
    actionSp = scale.action,
    balanceLines = balanceLines,
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
