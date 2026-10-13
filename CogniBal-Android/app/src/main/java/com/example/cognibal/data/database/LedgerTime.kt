package com.example.cognibal.data.database

import com.example.cognibal.time.TimeManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * 账单库里的本地时间。
 *
 * 存成固定 23 个字符的 `yyyy-MM-dd'T'HH:mm:ss.SSS`。位数对齐之后，SQLite 的字符串比较
 * 就是时间先后，日历可以用 `substr(billTime, 1, 10)` 取日期，不必再解析。
 *
 * 只保留到毫秒，和 [TimeManager.nowMillis] 一致。纳秒会被丢掉，避免
 * `05.9` 和 `05.10` 这种变长小数把顺序排错。
 *
 * 文本不含时区。写入时用的是当时设备时区的本地时间；用户以后修改系统时区，
 * 已经入库的日历归属不会跟着改。
 */
internal object LedgerTime {
    private val formatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
        .withLocale(Locale.ROOT)

    /** 去掉纳秒里不足 1 毫秒的部分。Converter 和仓库都从这里走，避免各写一种精度。 */
    fun normalize(value: LocalDateTime): LocalDateTime {
        val millis = value.nano / NANOS_PER_MILLI
        return value.withNano(millis * NANOS_PER_MILLI)
    }

    fun encode(value: LocalDateTime): String = formatter.format(normalize(value))

    fun decode(value: String): LocalDateTime = try {
        LocalDateTime.parse(value, formatter)
    } catch (exception: DateTimeParseException) {
        throw IllegalArgumentException("无法解析账单时间: $value", exception)
    }

    /** 当前本地时间，已经收成毫秒。给 createTime、updateTime 用。 */
    fun now(): LocalDateTime {
        val millis = TimeManager.nowMillis()
        val local = TimeManager.toLocalDateTime(millis) ?: LocalDateTime.now()
        return normalize(local)
    }

    private const val NANOS_PER_MILLI: Int = 1_000_000
}
