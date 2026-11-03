import Foundation

/// 账本里的本地时间，固定 23 个字符：`yyyy-MM-dd'T'HH:mm:ss.SSS`。
///
/// 文本不含时区。字符串比较就是时间先后。
public struct LedgerTimestamp: Equatable, Sendable {
    public var year: Int
    public var month: Int
    public var day: Int
    public var hour: Int
    public var minute: Int
    public var second: Int
    public var millisecond: Int

    public init(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        millisecond: Int
    ) {
        self.year = year
        self.month = month
        self.day = day
        self.hour = hour
        self.minute = minute
        self.second = second
        self.millisecond = millisecond
    }

    public var text: String {
        String(
            format: "%04d-%02d-%02dT%02d:%02d:%02d.%03d",
            locale: Locale(identifier: "en_US_POSIX"),
            year,
            month,
            day,
            hour,
            minute,
            second,
            millisecond
        )
    }

    public static func now(timeZone: TimeZone = .current, date: Date = Date()) -> LedgerTimestamp {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        calendar.locale = Locale(identifier: "en_US_POSIX")
        let parts = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second, .nanosecond],
            from: date
        )
        let millisecond = (parts.nanosecond ?? 0) / 1_000_000
        return LedgerTimestamp(
            year: parts.year ?? 1,
            month: parts.month ?? 1,
            day: parts.day ?? 1,
            hour: parts.hour ?? 0,
            minute: parts.minute ?? 0,
            second: parts.second ?? 0,
            millisecond: millisecond
        )
    }

    public static func parse(_ value: String) throws -> LedgerTimestamp {
        guard value.count == 23, value.contains("T"), value.contains(".") else {
            throw LedgerTimeError.invalid
        }
        let pieces = value.split(separator: "T", maxSplits: 1, omittingEmptySubsequences: false)
        guard pieces.count == 2 else { throw LedgerTimeError.invalid }
        let date = pieces[0].split(separator: "-", omittingEmptySubsequences: false)
        let clockAndFraction = pieces[1].split(separator: ".", omittingEmptySubsequences: false)
        guard date.count == 3, clockAndFraction.count == 2 else { throw LedgerTimeError.invalid }
        let clock = clockAndFraction[0].split(separator: ":", omittingEmptySubsequences: false)
        guard clock.count == 3,
              let year = Int(date[0]), date[0].count == 4,
              let month = Int(date[1]), date[1].count == 2,
              let day = Int(date[2]), date[2].count == 2,
              let hour = Int(clock[0]), clock[0].count == 2,
              let minute = Int(clock[1]), clock[1].count == 2,
              let second = Int(clock[2]), clock[2].count == 2,
              let millisecond = Int(clockAndFraction[1]), clockAndFraction[1].count == 3
        else {
            throw LedgerTimeError.invalid
        }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .gmt
        calendar.locale = Locale(identifier: "en_US_POSIX")
        let components = DateComponents(
            calendar: calendar,
            timeZone: calendar.timeZone,
            year: year,
            month: month,
            day: day,
            hour: hour,
            minute: minute,
            second: second
        )
        guard let resolved = calendar.date(from: components) else { throw LedgerTimeError.invalid }
        let roundTrip = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: resolved)
        guard roundTrip.year == year,
              roundTrip.month == month,
              roundTrip.day == day,
              roundTrip.hour == hour,
              roundTrip.minute == minute,
              roundTrip.second == second,
              (0..<1000).contains(millisecond)
        else {
            throw LedgerTimeError.invalid
        }
        return LedgerTimestamp(
            year: year,
            month: month,
            day: day,
            hour: hour,
            minute: minute,
            second: second,
            millisecond: millisecond
        )
    }
}

public enum LedgerTimeError: Error, Equatable, Sendable {
    case invalid
}
