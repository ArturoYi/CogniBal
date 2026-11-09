package com.cyr.cognobal.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetFitTest {
    @Test
    fun wideCardKeepsEveryFigure() {
        val layout = fit(360f, 240f, sample())
        assertTrue(layout.fits)
        assertEquals(listOf(WidgetSlot.Income, WidgetSlot.Expense, WidgetSlot.Balance), layout.slots)
        assertTrue(layout.showMonth)
        assertTrue(layout.showLabels)
        assertTrue(layout.showAction)
        assertEquals(WidgetArrange.Columns, layout.arrange)
        assertTrue(layout.contentWidth <= 360f)
        assertTrue(layout.contentHeight <= 240f)
    }

    @Test
    fun narrowCardStacksLongAmountsInsteadOfClipping() {
        val layout = fit(140f, 280f, sample(money = "¥9,999,999.99"))
        assertTrue(layout.fits)
        assertEquals(3, layout.slots.size)
        assertTrue(layout.showLabels)
        assertTrue(layout.arrange != WidgetArrange.Columns)
        assertTrue(layout.contentWidth <= 140f)
    }

    @Test
    fun tinyCardKeepsCompleteShortAmountsAndDropsTheMonth() {
        val layout = fit(110f, 80f, sample())
        assertTrue(layout.fits)
        assertEquals(3, layout.slots.size)
        assertFalse(layout.showMonth)
        assertFalse(layout.showAction)
        assertTrue(layout.contentWidth <= 110f)
        assertTrue(layout.contentHeight <= 80f)
    }

    @Test
    fun tinyCardDropsSideFiguresWhenTheBalanceItselfIsWide() {
        val layout = fit(110f, 80f, sample(money = "¥9,999,999.99"))
        assertTrue(layout.fits)
        assertEquals(listOf(WidgetSlot.Balance), layout.slots)
        assertTrue(layout.showLabels)
        assertFalse(layout.showMonth)
        assertFalse(layout.showAction)
        assertTrue(layout.amountSp <= 12f)
    }

    @Test
    fun overflowSentenceWrapsInsteadOfBeingCut() {
        val copy = sample(balance = "金额太大，无法合计", overflow = true)
        val layout = fit(120f, 180f, copy)
        assertTrue(layout.fits)
        assertTrue(WidgetSlot.Balance in layout.slots)
        val inner = 120f - layout.paddingH * 2f - 2f
        val oneLine = estimate(copy.balance, layout.amountSp, bold = true).width
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
                    assertEquals(3, layout.slots.size)
                }
                if (!layout.showLabels) {
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

private fun score(layout: WidgetLayout): Int {
    var score = 0
    if (WidgetSlot.Income in layout.slots) score += 4
    if (WidgetSlot.Expense in layout.slots) score += 4
    if (WidgetSlot.Balance in layout.slots) score += 8
    if (layout.showLabels) score += 1
    if (layout.showMonth) score += 2
    if (layout.showAction) score += 1
    return score
}
