import XCTest
@testable import CogniBalLedger

final class LedgerFormatTests: XCTestCase {
    func testTimestampRoundTrip() throws {
        let stamp = LedgerTimestamp(
            year: 2026,
            month: 11,
            day: 3,
            hour: 9,
            minute: 5,
            second: 7,
            millisecond: 8
        )
        XCTAssertEqual(stamp.text, "2026-11-03T09:05:07.008")
        XCTAssertEqual(try LedgerTimestamp.parse(stamp.text), stamp)
        let earlier = try LedgerTimestamp.parse("2024-01-01T00:00:00.009")
        let later = try LedgerTimestamp.parse("2024-01-01T00:00:00.010")
        XCTAssertLessThan(earlier.text, later.text)
    }

    func testTimestampDropsSubMillisecond() {
        let date = Date(timeIntervalSince1970: 1_700_000_000.123_9)
        let stamp = LedgerTimestamp.now(timeZone: TimeZone(secondsFromGMT: 0)!, date: date)
        XCTAssertEqual(stamp.millisecond, 123)
        XCTAssertEqual(stamp.text.count, 23)
    }

    func testTimestampRejectsInvalidDates() {
        XCTAssertThrowsError(try LedgerTimestamp.parse("2024-13-01T00:00:00.000"))
        XCTAssertThrowsError(try LedgerTimestamp.parse("2024-01-01T00:00:00.12"))
        XCTAssertThrowsError(try LedgerTimestamp.parse("2024-01-01 00:00:00.000"))
    }

    func testMoneyFormatUsesDotAndGrouping() {
        let locale = Locale(identifier: "en_US")
        XCTAssertEqual(LedgerMoney.formatMinor(123_456, locale: locale), "1,234.56")
        XCTAssertEqual(LedgerMoney.formatMinor(-50, locale: locale), "-0.50")
        XCTAssertEqual(LedgerMoney.formatMinor(0, locale: locale), "0.00")
        XCTAssertEqual(LedgerMoney.formatMinor(Int64.min, locale: locale), "—")
    }

    func testMoneyParseMatchesAndroid() {
        XCTAssertEqual(LedgerMoney.parseMajor(""), 0)
        XCTAssertEqual(LedgerMoney.parseMajor("  "), 0)
        XCTAssertEqual(LedgerMoney.parseMajor("1,234.50"), 123_450)
        XCTAssertEqual(LedgerMoney.parseMajor("1.234,5"), 123_450)
        XCTAssertEqual(LedgerMoney.parseMajor("1.2.3"), 1_230)
        XCTAssertEqual(LedgerMoney.parseMajor("9999999999.99"), 999_999_999_999)
        XCTAssertNil(LedgerMoney.parseMajor("10000000000.00"))
        XCTAssertNil(LedgerMoney.parseMajor("12.345"))
        XCTAssertNil(LedgerMoney.parseMajor("-"))
        XCTAssertNil(LedgerMoney.parseMajor("12a"))
        XCTAssertEqual(LedgerMoney.parseMajor("-0.50"), -50)
    }

    func testColorKeysAndHex() {
        let teal = LedgerColor.resolve(storageKey: "teal")
        let hex = LedgerColor.resolve(storageKey: "#FF0CA678")
        XCTAssertEqual(teal, hex)
        XCTAssertEqual(LedgerColor.resolve(storageKey: "0CA678"), teal)
        XCTAssertEqual(LedgerColor.resolve(storageKey: "nope"), teal)
        XCTAssertFalse(teal.prefersDarkForeground)
        XCTAssertTrue(LedgerColor.resolve(storageKey: "#FFFFFFFF").prefersDarkForeground)
    }

    func testSymbolMapFallsBack() {
        XCTAssertEqual(LedgerSymbol.accountSystemName(for: "wallet"), "wallet.pass")
        XCTAssertEqual(LedgerSymbol.accountSystemName(for: "missing"), "wallet.pass")
        XCTAssertEqual(LedgerSymbol.categorySystemName(for: "restaurant"), "fork.knife")
        XCTAssertEqual(LedgerSymbol.categorySystemName(for: "missing"), "square.grid.2x2")
    }
}
