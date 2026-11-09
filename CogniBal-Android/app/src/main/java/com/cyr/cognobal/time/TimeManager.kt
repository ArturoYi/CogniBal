package com.cyr.cognobal.time

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 墙上时间。界面、入库和跨日判断都从这里取，不在业务里直接写 `System.currentTimeMillis()`。
 *
 * 用户修改系统时间或时区后，下一次调用就会看到新值。时间戳超出 `java.time` 能表示的范围时返回 null。
 */
object TimeManager {
    /** 1 小时的毫秒数。 */
    const val MILLIS_PER_HOUR: Long = 60 * 60 * 1_000L

    /**
     * 1 天的毫秒数，固定 24 小时。
     * 等到本地午夜时，真正的等待按下一自然日零点计算；这个长度只作为等待上限。
     */
    const val MILLIS_PER_DAY: Long = 24 * MILLIS_PER_HOUR

    /** 当前墙上时间，单位毫秒。 */
    fun nowMillis(): Long = System.currentTimeMillis()

    /** 设备当前时区。每次都读系统设置，不缓存。 */
    fun deviceZone(): ZoneId = ZoneId.systemDefault()

    /**
     * 毫秒时间戳在 [zone] 里的本地日期时间，不含时区偏移。默认设备时区。
     * 无法换算时返回 null。
     */
    fun toLocalDateTime(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDateTime? = zoned(epochMillis, zone)?.toLocalDateTime()

    /**
     * 毫秒时间戳在 [zone] 里的本地日期，不含时刻。默认设备时区。
     * 无法换算时返回 null。
     */
    fun toLocalDate(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDate? = zoned(epochMillis, zone)?.toLocalDate()

    /** 设备时区里的今天。换算失败时退回系统默认日历。 */
    fun currentDay(): LocalDate = toLocalDate(nowMillis()) ?: LocalDate.now()

    /**
     * 设备时区里的今天。过了本地午夜再发一次。
     *
     * 等待时间算到下一自然日的零点，夏令时当天是 23 或 25 小时，不是固定的 24 小时。
     * 时区在每次醒来时重读。
     */
    fun observeToday(): Flow<LocalDate> = flow {
        while (true) {
            val zone = deviceZone()
            val today = toLocalDate(nowMillis(), zone) ?: LocalDate.now()
            emit(today)
            delay(millisUntilNextDay(today, zone))
        }
    }

    private fun millisUntilNextDay(today: LocalDate, zone: ZoneId): Long {
        val nextMidnight = try {
            today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        } catch (_: DateTimeException) {
            return MILLIS_PER_HOUR
        }
        val remain = nextMidnight - nowMillis()
        return remain.coerceIn(1_000L, MILLIS_PER_DAY + MILLIS_PER_HOUR)
    }

    private fun zoned(epochMillis: Long, zone: ZoneId): ZonedDateTime? = try {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
    } catch (_: DateTimeException) {
        null
    }
}
