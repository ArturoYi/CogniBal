package com.example.cognibal.ui.picker

import java.time.YearMonth

/**
 * 时间边界和跨列修正。
 *
 * 修正顺序是年、月、日、时、分、秒。每一级都先按更高级的结果算出合法区间，
 * 再把当前值收进这个区间。区间是连续的，所以收束就是钳到最近的端点。
 * 时分秒如果还在新的日期允许的范围内，会原样保留。
 *
 * 日超出当月天数时收束到当月最后一天。这是日历本身的合法范围。
 * [minTime] 和 [maxTime] 都是闭区间。两者都为空时，年份用 [DefaultMinYear] 到 [DefaultMaxYear]，
 * 月日时分不做额外限制。若 min 晚于 max，结果固定为规范化后的 min。
 *
 * 不合法的月日时分会先规范化，再参与比较。例如 2 月 31 日会先变成当月最后一天。
 */
object WheelDateTimeRules {
    const val DefaultMinYear: Int = 1900
    const val DefaultMaxYear: Int = 2100

    fun clamp(
        value: WheelDateTime,
        minTime: WheelDateTime?,
        maxTime: WheelDateTime?,
    ): WheelDateTime {
        val normalized = value.normalized()
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        if (min != null && max != null && min.isAfter(max)) return min

        val year = normalized.year.clampTo(yearRange(min, max))
        val month = normalized.month.clampTo(monthRange(year, min, max))
        val day = normalized.day.clampTo(dayRange(year, month, min, max))
        val hour = normalized.hour.clampTo(hourRange(year, month, day, min, max))
        val minute = normalized.minute.clampTo(minuteRange(year, month, day, hour, min, max))
        val second = normalized.second.clampTo(secondRange(year, month, day, hour, minute, min, max))
        return if (
            year == value.year &&
            month == value.month &&
            day == value.day &&
            hour == value.hour &&
            minute == value.minute &&
            second == value.second
        ) {
            value
        } else {
            WheelDateTime(year, month, day, hour, minute, second)
        }
    }

    fun yearRange(minTime: WheelDateTime?, maxTime: WheelDateTime?): IntRange {
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        val start = min?.year ?: DefaultMinYear
        val end = max?.year ?: DefaultMaxYear
        return contiguous(start, end)
    }

    fun monthRange(year: Int, minTime: WheelDateTime?, maxTime: WheelDateTime?): IntRange {
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        var start = 1
        var end = 12
        if (min != null && year == min.year) start = min.month
        if (max != null && year == max.year) end = max.month
        if (min != null && year < min.year) return min.month..min.month
        if (max != null && year > max.year) return max.month..max.month
        return contiguous(start, end)
    }

    fun dayRange(
        year: Int,
        month: Int,
        minTime: WheelDateTime?,
        maxTime: WheelDateTime?,
    ): IntRange {
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        val safeMonth = month.coerceIn(1, 12)
        val length = YearMonth.of(year, safeMonth).lengthOfMonth()
        var start = 1
        var end = length
        if (min != null && year == min.year && safeMonth == min.month) {
            start = min.day.coerceIn(1, length)
        }
        if (max != null && year == max.year && safeMonth == max.month) {
            end = minOf(end, max.day.coerceIn(1, length))
        }
        return contiguous(start, end)
    }

    fun hourRange(
        year: Int,
        month: Int,
        day: Int,
        minTime: WheelDateTime?,
        maxTime: WheelDateTime?,
    ): IntRange {
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        var start = 0
        var end = 23
        if (min != null && sameDate(year, month, day, min)) start = min.hour
        if (max != null && sameDate(year, month, day, max)) end = max.hour
        return contiguous(start, end)
    }

    fun minuteRange(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minTime: WheelDateTime?,
        maxTime: WheelDateTime?,
    ): IntRange {
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        var start = 0
        var end = 59
        if (min != null && sameDate(year, month, day, min) && hour == min.hour) start = min.minute
        if (max != null && sameDate(year, month, day, max) && hour == max.hour) end = max.minute
        return contiguous(start, end)
    }

    fun secondRange(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        minTime: WheelDateTime?,
        maxTime: WheelDateTime?,
    ): IntRange {
        val min = minTime?.normalized()
        val max = maxTime?.normalized()
        var start = 0
        var end = 59
        if (min != null && sameMoment(year, month, day, hour, minute, min)) start = min.second
        if (max != null && sameMoment(year, month, day, hour, minute, max)) end = max.second
        return contiguous(start, end)
    }
}

private fun WheelDateTime.normalized(): WheelDateTime {
    val safeMonth = month.coerceIn(1, 12)
    val safeDay = day.coerceIn(1, YearMonth.of(year, safeMonth).lengthOfMonth())
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val safeSecond = second.coerceIn(0, 59)
    if (
        safeMonth == month &&
        safeDay == day &&
        safeHour == hour &&
        safeMinute == minute &&
        safeSecond == second
    ) {
        return this
    }
    return copy(
        month = safeMonth,
        day = safeDay,
        hour = safeHour,
        minute = safeMinute,
        second = safeSecond,
    )
}

private fun WheelDateTime.isAfter(other: WheelDateTime): Boolean {
    if (year != other.year) return year > other.year
    if (month != other.month) return month > other.month
    if (day != other.day) return day > other.day
    if (hour != other.hour) return hour > other.hour
    if (minute != other.minute) return minute > other.minute
    return second > other.second
}

private fun sameMoment(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    bound: WheelDateTime,
): Boolean = sameDate(year, month, day, bound) && hour == bound.hour && minute == bound.minute

private fun sameDate(year: Int, month: Int, day: Int, bound: WheelDateTime): Boolean =
    year == bound.year && month == bound.month && day == bound.day

private fun contiguous(start: Int, end: Int): IntRange =
    if (start <= end) start..end else start..start

private fun Int.clampTo(range: IntRange): Int = coerceIn(range.first, range.last)
