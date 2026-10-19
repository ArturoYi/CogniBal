package com.example.cognibal.ui.picker

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

/**
 * 滚轮上的一个时间点。月份从 1 开始。
 *
 * 用整型字段而不是直接把 [LocalDateTime] 传进组合，是为了让这个类可以被标成稳定类型。
 * 调用方拿到回调后的值都已经过 [WheelDateTimeRules.clamp]，日期一定存在。
 */
@Stable
data class WheelDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
) {
    fun toLocalDateTime(): LocalDateTime = LocalDateTime.of(year, month, day, hour, minute, second)

    companion object {
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
        ): WheelDateTime = WheelDateTime(year, month, day, hour, minute, second)

        fun from(dateTime: LocalDateTime): WheelDateTime = WheelDateTime(
            year = dateTime.year,
            month = dateTime.monthValue,
            day = dateTime.dayOfMonth,
            hour = dateTime.hour,
            minute = dateTime.minute,
            second = dateTime.second,
        )

        fun now(): WheelDateTime = from(LocalDateTime.now())
    }
}

/**
 * 日期、时间，或日期加时间。隐藏的字段仍留在 [WheelDateTime] 里，切换模式时不会丢。
 * [Time] 是时、分、秒三列。[Date] 是年、月、日。[DateTime] 是年、月、日、时、分。
 */
enum class WheelDateTimeMode {
    Date,
    Time,
    DateTime,
}
