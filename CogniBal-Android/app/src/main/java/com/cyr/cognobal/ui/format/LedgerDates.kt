package com.cyr.cognobal.ui.format

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 首页和桌面卡片上的月份。英文用缩写，避免窄宽度放不下全称。 */
internal fun formatCalendarMonth(month: YearMonth, locale: Locale): String =
    month.format(locale.ledgerDateFormatter("yyyy年M月", "MMM yyyy"))

/** 月日加星期。统计页和首页最近列表共用。 */
internal fun formatCalendarDay(day: LocalDate, locale: Locale): String =
    day.format(locale.ledgerDateFormatter("M月d日 EEE", "EEE, MMM d"))

/** 账单行上的月日和时刻。 */
internal fun formatBillTime(time: LocalDateTime, locale: Locale): String =
    time.format(locale.ledgerDateFormatter("M月d日 HH:mm", "MMM d, HH:mm"))

internal fun Locale.ledgerDateFormatter(chinese: String, other: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (language == "zh") chinese else other, this)
