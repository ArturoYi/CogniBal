package com.cyr.cognobal.screen.feature.bill.state

import com.cyr.cognobal.data.database.model.BillType
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class BillHomeGroupingTest {
    @Test
    fun groupsEachDayAndKeepsNewerBillsFirst() {
        val today = LocalDateTime.of(2026, 11, 10, 8, 0)
        val yesterday = LocalDateTime.of(2026, 11, 9, 21, 0)
        val bills = listOf(
            row(id = 1, time = today, remark = null),
            row(id = 3, time = yesterday, remark = "晚饭"),
            row(id = 2, time = today.withHour(12), remark = "午饭"),
            row(id = 4, time = today.withHour(12), remark = null),
        )

        val groups = groupBillsByDay(bills)

        assertEquals(
            listOf(today.toLocalDate(), yesterday.toLocalDate()),
            groups.map { it.first },
        )
        assertEquals(listOf(4L, 2L, 1L), groups[0].second.map { it.id })
        assertEquals(listOf("午饭"), groups[0].second.mapNotNull { it.remark })
        assertEquals("晚饭", groups[1].second.single().remark)
    }

    @Test
    fun splitsMonthsAndKeepsDaysInsideEachMonth() {
        val november = LocalDateTime.of(2026, 11, 10, 9, 0)
        val earlierNovember = LocalDateTime.of(2026, 11, 9, 18, 0)
        val october = LocalDateTime.of(2026, 10, 2, 8, 0)
        val months = groupBillsByMonth(
            listOf(
                row(id = 1, time = october, remark = null),
                row(id = 2, time = earlierNovember, remark = null),
                row(id = 3, time = november, remark = "工资"),
            ),
        )

        assertEquals(
            listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)),
            months.map { it.month },
        )
        assertEquals(
            listOf(november.toLocalDate(), earlierNovember.toLocalDate()),
            months[0].days.map { it.day },
        )
        assertEquals(listOf(3L), months[0].days[0].bills.map { it.id })
        assertEquals("工资", months[0].days[0].bills.single().remark)
        assertEquals(october.toLocalDate(), months[1].days.single().day)
    }

    private fun row(
        id: Long,
        time: LocalDateTime,
        remark: String?,
    ) = BillRowUi(
        id = id,
        categoryName = "餐饮",
        type = BillType.Expense,
        time = time,
        signedAmount = -100L,
        remark = remark,
    )
}
