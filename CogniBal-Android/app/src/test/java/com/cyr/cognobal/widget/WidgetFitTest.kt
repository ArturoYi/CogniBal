package com.cyr.cognobal.widget

import kotlin.math.ceil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFitTest {
    @Test
    fun responsiveSizesStayInsideTheRemoteViewsLimit() {
        assertTrue(LedgerWidgetSizes.isNotEmpty())
        assertTrue(LedgerWidgetSizes.size <= 16)
        val areas = LedgerWidgetSizes.map { it.width.value * it.height.value }
        assertEquals(areas.size, areas.distinct().size)
        LedgerWidgetSizes.forEach { size ->
            assertTrue(size.width.value > 0f)
            assertTrue(size.height.value > 0f)
        }
    }

    @Test
    fun wideCardKeepsEveryFigure() {
        val layout = fit(360f, 240f, sample())
        assertTrue(layout.fits)
        assertEquals(listOf(WidgetSlot.Income, WidgetSlot.Expense, WidgetSlot.Balance), layout.slots)
        assertTrue(layout.showMonth)
        assertTrue(layout.showLabels)
        assertTrue(layout.showAction)
        assertTrue(layout.centered)
        assertTrue(layout.balanceLabelInHeader)
        assertEquals(WidgetArrange.Board, layout.arrange)
        assertTrue(layout.balanceSp >= layout.amountSp)
        assertTrue(layout.contentWidth <= 360f)
        assertTrue(layout.contentHeight <= 240f)
    }

    @Test
    fun narrowCardStacksLongAmountsInsteadOfClipping() {
        val layout = fit(140f, 280f, sample(money = "¥9,999,999.99"))
        assertTrue(layout.fits)
        assertEquals(3, layout.slots.size)
        assertTrue(layout.showLabels)
        assertEquals(WidgetArrange.Stack, layout.arrange)
        assertTrue(layout.contentWidth <= 140f)
    }

    @Test
    fun tinyCardKeepsTheBalanceReadable() {
        val layout = fit(110f, 80f, sample())
        assertTrue(layout.fits)
        assertTrue(WidgetSlot.Balance in layout.slots)
        assertFalse(layout.showAction)
        assertTrue(layout.contentWidth <= 110f)
        assertTrue(layout.contentHeight <= 80f)
    }

    @Test
    fun tinyCardShrinksAWideBalanceInsteadOfClippingIt() {
        val layout = fit(110f, 80f, sample(money = "¥9,999,999.99"))
        assertTrue(layout.fits)
        assertTrue(WidgetSlot.Balance in layout.slots)
        assertFalse(layout.showAction)
        assertTrue(layout.balanceSp <= 16f)
        assertTrue(layout.contentWidth <= 110f)
        assertTrue(layout.contentHeight <= 80f)
    }

    @Test
    fun phoneSizedCardLeadsWithTheBalance() {
        val layout = fit(300f, 136f, sample(money = "¥11,431.10", balance = "¥10,973.03"))
        assertTrue(layout.fits)
        assertTrue(layout.centered)
        assertTrue(layout.showMonth)
        assertTrue(layout.showBalanceLabel)
        assertTrue(layout.balanceLabelInHeader)
        assertTrue(layout.showAction)
        assertTrue(layout.balanceSp >= 28f)
        assertEquals(listOf(WidgetSlot.Balance), layout.slots)
    }

    @Test
    fun thisPhonePicksTheBreakpointClosestToItsCell() {
        val chosen = LedgerWidgetSizes
            .filter { size -> fitsIn(size.width.value, size.height.value, widgetWidth = 305f, widgetHeight = 144f) }
            .maxBy { it.width.value * it.height.value }
        assertEquals(300f, chosen.width.value)
        assertEquals(136f, chosen.height.value)
    }

    @Test
    fun overflowSentenceWrapsInsteadOfBeingCut() {
        val copy = sample(balance = "金额太大，无法合计", overflow = true)
        val layout = fit(120f, 180f, copy)
        assertTrue(layout.fits)
        assertTrue(WidgetSlot.Balance in layout.slots)
        val inner = 120f - layout.paddingH * 2f - 2f
        val oneLine = estimate(copy.balance, layout.balanceSp, bold = true).width
        if (oneLine > inner) assertTrue(layout.balanceLines > 1)
    }

    @Test
    fun largerCardNeverDropsInformation() {
        val widths = floatArrayOf(110f, 140f, 180f, 250f, 360f, 520f)
        val heights = floatArrayOf(80f, 110f, 150f, 220f, 360f)
        val copies = listOf(
            sample(),
            sample(money = "¥9,999,999.99"),
            sample(balance = "金额太大，无法合计", overflow = true),
            english(),
        )
        for (copy in copies) {
            val layouts = widths.flatMap { width ->
                heights.map { height -> (width to height) to fit(width, height, copy) }
            }
            for ((size, layout) in layouts) {
                assertTrue(WidgetSlot.Balance in layout.slots)
                assertEquals(WidgetSlot.Income in layout.slots, WidgetSlot.Expense in layout.slots)
                if (layout.showAction) {
                    assertTrue(layout.showMonth)
                    assertTrue(layout.centered)
                }
                if (!layout.showLabels && !layout.showBalanceLabel) {
                    assertEquals(listOf(WidgetSlot.Balance), layout.slots)
                    assertFalse(layout.showMonth)
                    assertFalse(layout.showAction)
                }
                if (layout.fits) {
                    assertTrue(layout.contentWidth <= size.first + 0.05f)
                    assertTrue(layout.contentHeight <= size.second + 0.05f)
                }
            }
            for (i in layouts.indices) {
                for (j in layouts.indices) {
                    val (leftSize, left) = layouts[i]
                    val (rightSize, right) = layouts[j]
                    val leftInsideRight = leftSize.first <= rightSize.first && leftSize.second <= rightSize.second
                    if (leftInsideRight && left.fits) {
                        assertTrue(score(right) >= score(left))
                    }
                }
            }
        }
    }

    @Test
    fun emptySizeFallsBackToTheBalanceAmount() {
        val layout = fit(0f, 0f, sample())
        assertFalse(layout.fits)
        assertEquals(listOf(WidgetSlot.Balance), layout.slots)
        assertFalse(layout.showLabels)
    }
}

private fun fit(width: Float, height: Float, copy: WidgetCopy): WidgetLayout =
    fitWidget(width, height, copy, ::estimate)

private fun sample(
    money: String = "¥12.00",
    balance: String = money,
    overflow: Boolean = false,
): WidgetCopy = WidgetCopy(
    month = "2026年11月",
    incomeLabel = "收入",
    expenseLabel = "支出",
    balanceLabel = "结余",
    income = money,
    expense = money,
    balance = balance,
    action = "记一笔",
    balanceOverflow = overflow,
)

private fun english(): WidgetCopy = WidgetCopy(
    month = "Nov 2026",
    incomeLabel = "Income",
    expenseLabel = "Expense",
    balanceLabel = "Balance",
    income = "${'$'}1,234.56",
    expense = "${'$'}64.00",
    balance = "The total is too large to display",
    action = "Add a bill",
    balanceOverflow = true,
)

private fun estimate(text: String, sizeSp: Float, bold: Boolean): WidgetTextSize {
    var width = 0f
    for (ch in text) {
        val em = if (ch.code >= 0x2E80) 1f else 0.56f
        width += sizeSp * em
    }
    if (bold) width *= 1.05f
    return WidgetTextSize(width, sizeSp * 1.25f)
}

private fun fitsIn(layoutWidth: Float, layoutHeight: Float, widgetWidth: Float, widgetHeight: Float): Boolean =
    ceil(widgetWidth) + 1f > layoutWidth && ceil(widgetHeight) + 1f > layoutHeight

private fun score(layout: WidgetLayout): Int {
    var score = 0
    if (WidgetSlot.Income in layout.slots) score += 4
    if (WidgetSlot.Expense in layout.slots) score += 4
    if (WidgetSlot.Balance in layout.slots) score += 8
    if (layout.showLabels || layout.showBalanceLabel) score += 1
    if (layout.showMonth) score += 2
    // 两个按钮代替一列收入、一列支出。更大的卡片如果两者都放下，分数更高。
    if (layout.showAction) score += 8
    return score
}
