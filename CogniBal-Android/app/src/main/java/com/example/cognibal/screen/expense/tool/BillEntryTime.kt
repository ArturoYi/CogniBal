package com.example.cognibal.screen.expense.tool

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 日期选择器用 UTC 当天 0 点的毫秒。
 * 若按设备时区换算，东八区的午夜会落到前一天。
 */
internal fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun pickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
