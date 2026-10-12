package com.example.cognibal.time

import android.os.SystemClock
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * UTC 上的 ISO-8601 文本样式。
 *
 * [Standard] 去掉末尾多余的 0。整秒是 `2020-01-02T03:04:05Z`，120 毫秒是 `2020-01-02T03:04:05.120Z`。
 * [Millis] 始终保留 3 位毫秒。整秒是 `2020-01-02T03:04:05.000Z`。
 * 两种都是 UTC，后缀是字面量 `Z`，不是 `+00:00`。
 */
enum class IsoUtcStyle {
    Standard,
    Millis,
}

/**
 * 一次进程内计时的起点。
 *
 * 只保存“从哪个开机时刻开始”，不把开机时长交给调用方。
 * [toString] 也不打印这个数值，避免日志里泄露出原始读数。
 *
 * 不能写入数据库、Bundle 或偏好：手机关机后开机时长会从 0 重新累计，旧标记没有意义。
 * 进程被杀掉后同样作废，下次进入流程要重新 [TimeManager.mark]。
 * 算经过了多久时，把标记交回 [TimeManager.elapsedMillis] 或 [TimeManager.isElapsed]。
 */
class MonotonicMark private constructor(
    private val startedAtMonotonicMillis: Long,
) {
    /**
     * 当前开机时长减去起点。
     * 时钟回退时返回 0，避免出现负的耗时；差值超出长整型时返回 [Long.MAX_VALUE]。
     */
    internal fun elapsedMillis(currentMonotonicMillis: Long): Long {
        return try {
            val delta = Math.subtractExact(currentMonotonicMillis, startedAtMonotonicMillis)
            if (delta < 0L) 0L else delta
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is MonotonicMark &&
            startedAtMonotonicMillis == other.startedAtMonotonicMillis
    }

    override fun hashCode(): Int = startedAtMonotonicMillis.hashCode()

    override fun toString(): String = "MonotonicMark"

    companion object {
        /** 仅 [TimeManager.mark] 使用。业务代码不要自己传入开机时长。 */
        internal fun capture(monotonicMillis: Long): MonotonicMark = MonotonicMark(monotonicMillis)
    }
}

/**
 * 全应用的时间工具。取“现在”、格式化、时区换算和时间运算都从这里进。
 *
 * 有两种时钟，用途不要混：
 * - 墙上时间来自 `System.currentTimeMillis()`。给界面、入库和业务时间戳。
 *   用户修改手机时间后，这个值会跟着变。
 * - 开机单调时间来自 `SystemClock.elapsedRealtime()`。只用于进程内的耗时和超时，
 *   包含休眠，也不受用户修改系统时间影响。原始读数不对外提供，调用方只能拿到 [MonotonicMark]。
 *   不要改用 `uptimeMillis()`，它不包含深度休眠。
 *
 * 取当前时间用 [nowMillis]、[nowInstant] 或 [nowDate]，不要在业务里写 `Date()` 或
 * `System.currentTimeMillis()`。
 *
 * 天、小时、分钟、秒的加减是固定时长：1 天就是 24 小时，不随夏令时变成 23 或 25 小时。
 * 判断是不是同一天才把时间戳折成某个时区里的本地日期。
 *
 * 解析失败、样式非法或运算溢出时返回 null 或 false，不把异常抛给调用方。
 * 这里没有可变的时间偏移，读当前时间不加锁。
 * 格式化器本身不可变，按样式、语言和时区放在并发映射里复用。
 *
 * 服务器校时还没接入。当前业务时间就是墙上时间。
 */
object TimeManager {
    /** 1 秒的毫秒数。 */
    const val MILLIS_PER_SECOND: Long = 1_000L

    /** 1 分钟的毫秒数。 */
    const val MILLIS_PER_MINUTE: Long = 60 * MILLIS_PER_SECOND

    /** 1 小时的毫秒数。 */
    const val MILLIS_PER_HOUR: Long = 60 * MILLIS_PER_MINUTE

    /**
     * 1 天的毫秒数。
     * 固定 24 小时。加减天数用这个长度，不按当地日历的夏令时伸缩。
     */
    const val MILLIS_PER_DAY: Long = 24 * MILLIS_PER_HOUR

    /**
     * 可接受时间戳的下界：2000-01-01T00:00:00Z，含这一毫秒。
     * 更早的值多半是未初始化，或把秒当成了毫秒。
     */
    const val EARLIEST_ACCEPTED_EPOCH_MILLIS: Long = 946_684_800_000L

    /**
     * 可接受时间戳的上界：2100-01-01T00:00:00Z，含这一毫秒。
     * 更晚的值多半是单位用错。
     */
    const val LATEST_ACCEPTED_EPOCH_MILLIS: Long = 4_102_444_800_000L

    /**
     * 界面上固定的本地日期时间样式：`yyyy-MM-dd HH:mm:ss`。
     * 例如上海时区的 `2020-01-02T03:04:05Z` 显示为 `2020-01-02 11:04:05`。
     */
    const val LOCAL_DATE_TIME_PATTERN: String = "yyyy-MM-dd HH:mm:ss"

    /**
     * 当前墙上时间，单位毫秒。
     * 界面展示、持久化和业务时间戳用这个值。用户修改系统时间后会变化。
     */
    fun nowMillis(): Long = System.currentTimeMillis()

    /** 当前墙上时间对应的 [Instant]。内存里优先用它，而不是 [Date]。 */
    fun nowInstant(): Instant = Instant.ofEpochMilli(nowMillis())

    /**
     * 当前墙上时间对应的 [Date]。
     * 只在旧接口要求 [Date] 时使用。新代码用 [nowInstant]。
     */
    fun nowDate(): Date = Date(nowMillis())

    /**
     * 当前墙上时间的 ISO-8601 UTC 文本。
     * 默认 [IsoUtcStyle.Standard]。需要固定 3 位毫秒时传 [IsoUtcStyle.Millis]。
     */
    fun nowIsoUtc(style: IsoUtcStyle = IsoUtcStyle.Standard): String = toIsoUtc(nowInstant(), style)

    /**
     * 设备当前时区。
     * 每次调用都读系统设置，用户改时区后下一次就会拿到新值，不会缓存旧时区。
     */
    fun deviceZone(): ZoneId = ZoneId.systemDefault()

    /**
     * 记下进程内计时的起点。
     *
     * 内部读 `SystemClock.elapsedRealtime()`，这个原始值不会返回。
     * 标记只在本次开机、本次进程内有效，不能落盘，也不能放进 Bundle。
     */
    fun mark(): MonotonicMark = MonotonicMark.capture(SystemClock.elapsedRealtime())

    /**
     * 从 [mark] 到现在经过的毫秒。
     * 只反映开机单调时钟，用户中途修改系统时间不会改变结果。
     * 单调时钟回退时返回 0。
     */
    fun elapsedMillis(mark: MonotonicMark): Long = mark.elapsedMillis(SystemClock.elapsedRealtime())

    /**
     * 进程内是否已经超过 [timeoutMillis]。
     *
     * 用开机单调时钟比较，适合加载超时、按钮冷却和请求耗时。
     * 用户修改系统时间不会让它提前或延后。日历意义上的截止时间用 [isTimedOut]。
     * [timeoutMillis] 小于 0 时返回 false；等于 0 时表示不需要等待，立即视为已到。
     */
    fun isElapsed(mark: MonotonicMark, timeoutMillis: Long): Boolean {
        if (timeoutMillis < 0L) return false
        return elapsedMillis(mark) >= timeoutMillis
    }

    /**
     * 毫秒时间戳转 [Instant]。
     * 超出 `java.time` 能表示的范围时返回 null，不抛异常。
     */
    fun toInstant(epochMillis: Long): Instant? = instantOf(epochMillis)

    /** [Date] 转 [Instant]。[Date] 的毫秒值都能转成瞬时。 */
    fun toInstant(date: Date): Instant = date.toInstant()

    /**
     * 把 [zone] 里的本地日期时间折成 UTC 瞬时。
     * 默认设备时区。本地时间落在夏令时缺口或结果超出范围时返回 null。
     */
    fun toInstant(
        localDateTime: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant? = toEpochMillis(localDateTime, zone)?.let(Instant::ofEpochMilli)

    /**
     * [Instant] 转毫秒时间戳。
     * 瞬时太大或太小，装不进长整型时返回 null。
     */
    fun toEpochMillis(instant: Instant): Long? = try {
        instant.toEpochMilli()
    } catch (_: ArithmeticException) {
        null
    }

    /** [Date] 转毫秒时间戳，就是 [Date.getTime]。 */
    fun toEpochMillis(date: Date): Long = date.time

    /**
     * 指定时区的本地日期时间对应的 UTC 毫秒。默认设备时区。
     *
     * 落在夏令时缺口里时，按该时区的规则拨到缺口之后。
     * 同一本地时间对应两个偏移的重叠区间，取较早的那个偏移。
     * 换算结果超出长整型时返回 null。
     */
    fun toEpochMillis(
        localDateTime: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? = try {
        localDateTime.atZone(zone).toInstant().toEpochMilli()
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    /**
     * 毫秒时间戳转 [Date]。
     * 时间戳无法表示成 [Instant] 时返回 null。
     */
    fun toDate(epochMillis: Long): Date? = toInstant(epochMillis)?.let { toDate(it) }

    /**
     * [Instant] 转 [Date]。
     * 瞬时装不进 [Date] 的毫秒范围时返回 null。
     */
    fun toDate(instant: Instant): Date? = try {
        Date.from(instant)
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: ArithmeticException) {
        null
    }

    /**
     * [Instant] 转 ISO-8601 UTC 文本。
     * 默认 [IsoUtcStyle.Standard]。合法瞬时都能格式化。
     */
    fun toIsoUtc(instant: Instant, style: IsoUtcStyle = IsoUtcStyle.Standard): String {
        return IsoUtc.format(instant, style)
    }

    /** [Date] 转 ISO-8601 UTC 文本。规则与 [toIsoUtc] 的瞬时版本相同。 */
    fun toIsoUtc(date: Date, style: IsoUtcStyle = IsoUtcStyle.Standard): String {
        return toIsoUtc(date.toInstant(), style)
    }

    /**
     * 毫秒时间戳转 ISO-8601 UTC 文本。
     * 时间戳无法表示成瞬时时返回 null。
     */
    fun toIsoUtc(epochMillis: Long, style: IsoUtcStyle = IsoUtcStyle.Standard): String? {
        return toInstant(epochMillis)?.let { toIsoUtc(it, style) }
    }

    /**
     * 解析 ISO-8601，得到 UTC 瞬时。
     *
     * 接受 `2020-01-02T03:04:05Z`、固定毫秒 `2020-01-02T03:04:05.000Z`，
     * 以及数值偏移 `2020-01-02T11:04:05+08:00`。偏移会折成同一个 UTC 瞬时。
     * 前后空白会先去掉。空字符串、缺时区或格式不对时返回 null。
     */
    fun parseInstant(isoUtc: String): Instant? = IsoUtc.parse(isoUtc)

    /**
     * 解析 ISO-8601，得到毫秒时间戳。
     * 文本无法解析，或瞬时装不进长整型时返回 null。
     */
    fun parseEpochMillis(isoUtc: String): Long? = parseInstant(isoUtc)?.let { toEpochMillis(it) }

    /**
     * 解析 ISO-8601，得到 [Date]。
     * 文本无法解析，或瞬时装不进 [Date] 时返回 null。
     */
    fun parseDate(isoUtc: String): Date? = parseInstant(isoUtc)?.let { toDate(it) }

    /**
     * 按 [pattern] 格式化时间戳。默认设备时区和当前语言。
     *
     * 年份固定为 ISO 历法，`yyyy` 不会因泰语佛历或日本年号变成另一个数字。
     * 月份、星期这类文字仍跟随 [locale]。
     * 样式字符串非法，或时间戳无法表示成瞬时，返回 null。
     */
    fun format(
        epochMillis: Long,
        pattern: String,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String? {
        val formatter = LocalText.pattern(pattern, locale, zone) ?: return null
        return LocalText.format(epochMillis, formatter, this::instantOf)
    }

    /**
     * 格式化成 [LOCAL_DATE_TIME_PATTERN]，也就是 `yyyy-MM-dd HH:mm:ss`。
     * 默认设备时区和当前语言。这是界面上稳定的数字日期，不带上午、下午或月份名称。
     */
    fun formatLocalDateTime(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String? = format(epochMillis, LOCAL_DATE_TIME_PATTERN, zone, locale)

    /**
     * 按语言和地区输出日期加时间。默认设备语言、设备时区和 [FormatStyle.MEDIUM]。
     *
     * 文案跟随 [locale]，覆盖英语、中文以及系统能提供的其他语言。
     * 例如中文长日期会带“年/月/日”，英文长日期会写成 `January 2, 2020` 这种顺序。
     * 时间戳无法格式化时返回 null。
     */
    fun formatLocalizedDateTime(
        epochMillis: Long,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
        timeStyle: FormatStyle = FormatStyle.MEDIUM,
    ): String? {
        val formatter = LocalText.localizedDateTime(locale, zone, dateStyle, timeStyle)
        return LocalText.format(epochMillis, formatter, this::instantOf)
    }

    /**
     * 只输出本地化日期，不带时刻。
     * 默认设备语言、设备时区和 [FormatStyle.MEDIUM]。无法格式化时返回 null。
     */
    fun formatLocalizedDate(
        epochMillis: Long,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
    ): String? {
        val formatter = LocalText.localizedDate(locale, zone, dateStyle)
        return LocalText.format(epochMillis, formatter, this::instantOf)
    }

    /**
     * 只输出本地化时刻，不带日期。
     * 默认设备语言、设备时区和 [FormatStyle.MEDIUM]。无法格式化时返回 null。
     */
    fun formatLocalizedTime(
        epochMillis: Long,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
        timeStyle: FormatStyle = FormatStyle.MEDIUM,
    ): String? {
        val formatter = LocalText.localizedTime(locale, zone, timeStyle)
        return LocalText.format(epochMillis, formatter, this::instantOf)
    }

    /**
     * 毫秒时间戳转到 [zone] 里的带时区日期时间。默认设备时区。
     * UTC 与本地时区的换算用这个方法，或用 [toLocalDateTime]。无法换算时返回 null。
     */
    fun toZonedDateTime(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ZonedDateTime? {
        val instant = toInstant(epochMillis) ?: return null
        return try {
            ZonedDateTime.ofInstant(instant, zone)
        } catch (_: DateTimeException) {
            null
        }
    }

    /**
     * 毫秒时间戳在 [zone] 里的本地日期时间，不含时区偏移。默认设备时区。
     * 同一瞬时在上海和 UTC 会得到不同的本地时间。无法换算时返回 null。
     */
    fun toLocalDateTime(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDateTime? = toZonedDateTime(epochMillis, zone)?.toLocalDateTime()

    /**
     * 毫秒时间戳在 [zone] 里的本地日期，不含时刻。默认设备时区。
     * [isSameDay] 用的就是这个日期。无法换算时返回 null。
     */
    fun toLocalDate(
        epochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LocalDate? = toZonedDateTime(epochMillis, zone)?.toLocalDate()

    /**
     * 把 [from] 时区里的本地日期时间，换成同一瞬时在 [to] 时区的本地日期时间。
     *
     * 例如上海 `2020-01-02 11:04:05` 换到 UTC 是 `2020-01-02 03:04:05`。
     * 时区规则无法解析时返回 null。
     */
    fun convert(
        localDateTime: LocalDateTime,
        from: ZoneId,
        to: ZoneId,
    ): LocalDateTime? = try {
        localDateTime.atZone(from).withZoneSameInstant(to).toLocalDateTime()
    } catch (_: DateTimeException) {
        null
    }

    /**
     * [zone] 在这个瞬时相对 UTC 的偏移。
     * 上海是 `+08:00`，印度是 `+05:30`，不一定是整小时。时间戳无效时返回 null。
     */
    fun offsetAt(zone: ZoneId, epochMillis: Long): ZoneOffset? {
        val instant = toInstant(epochMillis) ?: return null
        return try {
            zone.rules.getOffset(instant)
        } catch (_: DateTimeException) {
            null
        }
    }

    /**
     * 时间戳加上 [days] 天。1 天是 [MILLIS_PER_DAY]，也就是固定的 24 小时。
     * [days] 为负时相当于减去。乘积或加法溢出长整型时返回 null。
     */
    fun plusDays(epochMillis: Long, days: Long): Long? = shift(epochMillis, days, MILLIS_PER_DAY)

    /**
     * 时间戳加上 [hours] 小时，1 小时是 [MILLIS_PER_HOUR]。
     * [hours] 为负时相当于减去。溢出时返回 null。
     */
    fun plusHours(epochMillis: Long, hours: Long): Long? = shift(epochMillis, hours, MILLIS_PER_HOUR)

    /**
     * 时间戳加上 [minutes] 分钟，1 分钟是 [MILLIS_PER_MINUTE]。
     * [minutes] 为负时相当于减去。溢出时返回 null。
     */
    fun plusMinutes(epochMillis: Long, minutes: Long): Long? {
        return shift(epochMillis, minutes, MILLIS_PER_MINUTE)
    }

    /**
     * 时间戳加上 [seconds] 秒，1 秒是 [MILLIS_PER_SECOND]。
     * [seconds] 为负时相当于减去。溢出时返回 null。
     */
    fun plusSeconds(epochMillis: Long, seconds: Long): Long? {
        return shift(epochMillis, seconds, MILLIS_PER_SECOND)
    }

    /**
     * 时间戳减去 [days] 天，长度与 [plusDays] 相同。
     * [days] 为 [Long.MIN_VALUE] 时无法取反，返回 null。
     */
    fun minusDays(epochMillis: Long, days: Long): Long? = shiftBackward(epochMillis, days, MILLIS_PER_DAY)

    /** 时间戳减去 [hours] 小时。溢出或 [hours] 无法取反时返回 null。 */
    fun minusHours(epochMillis: Long, hours: Long): Long? {
        return shiftBackward(epochMillis, hours, MILLIS_PER_HOUR)
    }

    /** 时间戳减去 [minutes] 分钟。溢出或 [minutes] 无法取反时返回 null。 */
    fun minusMinutes(epochMillis: Long, minutes: Long): Long? {
        return shiftBackward(epochMillis, minutes, MILLIS_PER_MINUTE)
    }

    /** 时间戳减去 [seconds] 秒。溢出或 [seconds] 无法取反时返回 null。 */
    fun minusSeconds(epochMillis: Long, seconds: Long): Long? {
        return shiftBackward(epochMillis, seconds, MILLIS_PER_SECOND)
    }

    /**
     * [epochMillis] 是否严格早于 [otherEpochMillis]。
     * 相等时返回 false。比较的是时间戳数值，不看时区。
     */
    fun isBefore(epochMillis: Long, otherEpochMillis: Long): Boolean = epochMillis < otherEpochMillis

    /**
     * [epochMillis] 是否严格晚于 [otherEpochMillis]。
     * 相等时返回 false。比较的是时间戳数值，不看时区。
     */
    fun isAfter(epochMillis: Long, otherEpochMillis: Long): Boolean = epochMillis > otherEpochMillis

    /**
     * 两个时间戳在 [zone] 里是否同一天。默认设备时区。
     *
     * 同一毫秒在 UTC 和上海可能落在不同日期。例如 `2020-01-01T16:00:00Z`
     * 在 UTC 仍是 1 月 1 日，在上海已经是 1 月 2 日 00:00。
     * 任一时间戳无法折算成本地日期时返回 false。
     */
    fun isSameDay(
        epochMillis: Long,
        otherEpochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val day = toLocalDate(epochMillis, zone) ?: return false
        val otherDay = toLocalDate(otherEpochMillis, zone) ?: return false
        return day == otherDay
    }

    /**
     * 两个时间戳相差的毫秒：[endEpochMillis] 减 [startEpochMillis]。
     * 结束更早时为负数。差值超出长整型范围时返回 null。
     */
    fun betweenMillis(startEpochMillis: Long, endEpochMillis: Long): Long? = try {
        Math.subtractExact(endEpochMillis, startEpochMillis)
    } catch (_: ArithmeticException) {
        null
    }

    /**
     * 两个时间戳相差的整秒，不足 1 秒的部分丢掉。
     * 负间隔同样向 0 取整，例如相差 -1500 毫秒得到 -1 秒。毫秒差溢出时返回 null。
     */
    fun betweenSeconds(startEpochMillis: Long, endEpochMillis: Long): Long? {
        return betweenMillis(startEpochMillis, endEpochMillis)?.div(MILLIS_PER_SECOND)
    }

    /**
     * 两个时间戳相差的整分钟，不足 1 分钟的部分丢掉。
     * 负间隔同样向 0 取整。毫秒差溢出时返回 null。
     */
    fun betweenMinutes(startEpochMillis: Long, endEpochMillis: Long): Long? {
        return betweenMillis(startEpochMillis, endEpochMillis)?.div(MILLIS_PER_MINUTE)
    }

    /**
     * 墙上时间是否已经超过 [timeoutMillis]。
     *
     * 用当前系统时间减 [startEpochMillis]。用户把系统时间改早会推迟超时，改晚会提前超时。
     * 进程内计时、不希望被改表影响时，用 [mark] 和 [isElapsed]。
     * [timeoutMillis] 小于 0 时返回 false；等于 0 且当前时间不早于起点时返回 true。
     */
    fun isTimedOut(startEpochMillis: Long, timeoutMillis: Long): Boolean {
        if (timeoutMillis < 0L) return false
        val now = nowMillis()
        val elapsed = try {
            Math.subtractExact(now, startEpochMillis)
        } catch (_: ArithmeticException) {
            return now > startEpochMillis
        }
        return elapsed >= timeoutMillis
    }

    /**
     * 时间戳是否落在 [EARLIEST_ACCEPTED_EPOCH_MILLIS] 和 [LATEST_ACCEPTED_EPOCH_MILLIS] 之间，含两端。
     *
     * 用来拦住明显过早或过远的未来时间。格式化和解析不过滤这个范围，
     * 入库前由调用方自己判断。1970-01-01 会返回 false，但仍能被格式化出来。
     */
    fun isAcceptedEpochMillis(epochMillis: Long): Boolean {
        return epochMillis in EARLIEST_ACCEPTED_EPOCH_MILLIS..LATEST_ACCEPTED_EPOCH_MILLIS
    }

    /** 毫秒转瞬时。给格式化缓存用，避免和公开的 [toInstant] 重载搅在一起。 */
    private fun instantOf(epochMillis: Long): Instant? = try {
        Instant.ofEpochMilli(epochMillis)
    } catch (_: DateTimeException) {
        null
    }

    /** 按固定毫秒长度平移。乘法或加法溢出时返回 null，不回绕成另一个时间。 */
    private fun shift(epochMillis: Long, amount: Long, unitMillis: Long): Long? {
        val delta = try {
            Math.multiplyExact(amount, unitMillis)
        } catch (_: ArithmeticException) {
            return null
        }
        return try {
            Math.addExact(epochMillis, delta)
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** 先把数量取反，再交给 [shift]。[Long.MIN_VALUE] 无法取反，返回 null。 */
    private fun shiftBackward(epochMillis: Long, amount: Long, unitMillis: Long): Long? {
        val negative = try {
            Math.negateExact(amount)
        } catch (_: ArithmeticException) {
            return null
        }
        return shift(epochMillis, negative, unitMillis)
    }
}

/**
 * ISO-8601 UTC 的格式化与解析。
 * 标准样式交给 `DateTimeFormatter.ISO_INSTANT`；固定 3 位毫秒用字面量 `Z`，避免打印成 `+00:00`。
 */
private object IsoUtc {
    private val millis: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC)

    fun format(instant: Instant, style: IsoUtcStyle): String = when (style) {
        IsoUtcStyle.Standard -> DateTimeFormatter.ISO_INSTANT.format(instant)
        IsoUtcStyle.Millis -> millis.format(instant)
    }

    fun parse(text: String): Instant? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            OffsetDateTime.parse(trimmed).toInstant()
        } catch (_: DateTimeException) {
            null
        }
    }
}

/**
 * 本地与本地化文本。
 * 格式化器不可变，可以跨线程复用；缓存用并发映射，创建时不必再加一把大锁。
 */
private object LocalText {
    private val patterns = ConcurrentHashMap<PatternKey, DateTimeFormatter>()
    private val localized = ConcurrentHashMap<LocalizedKey, DateTimeFormatter>()

    fun pattern(pattern: String, locale: Locale, zone: ZoneId): DateTimeFormatter? {
        val key = PatternKey(pattern, locale, zone.id)
        patterns[key]?.let { return it }
        val formatter = try {
            DateTimeFormatter.ofPattern(pattern, locale)
                .withChronology(IsoChronology.INSTANCE)
                .withZone(zone)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return patterns.putIfAbsent(key, formatter) ?: formatter
    }

    fun localizedDateTime(
        locale: Locale,
        zone: ZoneId,
        dateStyle: FormatStyle,
        timeStyle: FormatStyle,
    ): DateTimeFormatter = localized(locale, zone, LocalizedKind.DateTime, dateStyle, timeStyle)

    fun localizedDate(
        locale: Locale,
        zone: ZoneId,
        dateStyle: FormatStyle,
    ): DateTimeFormatter = localized(locale, zone, LocalizedKind.Date, dateStyle, timeStyle = null)

    fun localizedTime(
        locale: Locale,
        zone: ZoneId,
        timeStyle: FormatStyle,
    ): DateTimeFormatter = localized(locale, zone, LocalizedKind.Time, dateStyle = null, timeStyle)

    fun format(
        epochMillis: Long,
        formatter: DateTimeFormatter,
        toInstant: (Long) -> Instant?,
    ): String? {
        val instant = toInstant(epochMillis) ?: return null
        return try {
            formatter.format(instant)
        } catch (_: DateTimeException) {
            null
        }
    }

    private fun localized(
        locale: Locale,
        zone: ZoneId,
        kind: LocalizedKind,
        dateStyle: FormatStyle?,
        timeStyle: FormatStyle?,
    ): DateTimeFormatter {
        val key = LocalizedKey(kind, dateStyle, timeStyle, locale, zone.id)
        return localized.computeIfAbsent(key) {
            val base = when (kind) {
                LocalizedKind.DateTime -> DateTimeFormatter.ofLocalizedDateTime(
                    requireNotNull(dateStyle),
                    requireNotNull(timeStyle),
                )
                LocalizedKind.Date -> DateTimeFormatter.ofLocalizedDate(requireNotNull(dateStyle))
                LocalizedKind.Time -> DateTimeFormatter.ofLocalizedTime(requireNotNull(timeStyle))
            }
            base.withLocale(locale).withZone(zone)
        }
    }
}

private enum class LocalizedKind {
    DateTime,
    Date,
    Time,
}

private data class PatternKey(
    val pattern: String,
    val locale: Locale,
    val zoneId: String,
)

private data class LocalizedKey(
    val kind: LocalizedKind,
    val dateStyle: FormatStyle?,
    val timeStyle: FormatStyle?,
    val locale: Locale,
    val zoneId: String,
)
