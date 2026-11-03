import Foundation
import GRDB
@testable import CogniBalLedger
import XCTest

final class SchemaTests: XCTestCase {
    func testFreshDatabaseMatchesRoomVersion4() throws {
        let database = try LedgerDatabase.openInMemory()
        XCTAssertEqual(try database.schemaVersion(), 4)
        XCTAssertEqual(try database.identityHash(), LedgerSchema.identityHashV4)
        let indexes = try database.read { db in
            try String.fetchAll(
                db,
                sql: """
                SELECT name FROM sqlite_master
                WHERE type = 'index' AND name LIKE 'index_%'
                ORDER BY name
                """
            )
        }
        XCTAssertEqual(
            indexes,
            [
                "index_bill_accountId",
                "index_bill_billTime",
                "index_bill_categoryId",
                "index_bill_type_billTime",
                "index_bill_type_categoryId_billTime",
                "index_bill_type_createTime",
                "index_budget_categoryId",
                "index_budget_periodStart_periodEnd",
                "index_category_type_sort",
            ]
        )
        let foreignKeys = try database.read { db -> Int in
            try Int.fetchOne(db, sql: "PRAGMA foreign_keys") ?? 0
        }
        XCTAssertEqual(foreignKeys, 1)
        let accounts = try database.read { db in
            try Int.fetchOne(db, sql: "SELECT COUNT(*) FROM account") ?? -1
        }
        XCTAssertEqual(accounts, 0)
    }

    func testOpensRoomVersion4Sample() throws {
        let url = try makeRoomFile(
            statements: try LedgerSchema.version4Statements(),
            userVersion: 4,
            hash: LedgerSchema.identityHashV4
        )
        try insertSample(at: url)
        let database = try LedgerDatabase.open(at: url)
        XCTAssertEqual(try database.schemaVersion(), 4)
        let amount = try database.read { db in
            try Int64.fetchOne(db, sql: "SELECT amount FROM bill WHERE id = 1")
        }
        XCTAssertEqual(amount, 1990)
        let name = try database.read { db in
            try String.fetchOne(db, sql: "SELECT name FROM category WHERE id = 1")
        }
        XCTAssertEqual(name, "餐饮")
    }

    func testEmptyRoomVersion4StaysEmpty() throws {
        let url = try makeRoomFile(
            statements: try LedgerSchema.version4Statements(),
            userVersion: 4,
            hash: LedgerSchema.identityHashV4
        )
        let database = try LedgerDatabase.open(at: url)
        let count = try database.read { db in
            try Int.fetchOne(db, sql: "SELECT COUNT(*) FROM account") ?? -1
        }
        XCTAssertEqual(count, 0)
        XCTAssertEqual(try database.schemaVersion(), 4)
    }

    func testMigratesRoomVersion1() throws {
        let script = try String(contentsOf: fixtureURL("room-v1"), encoding: .utf8)
        let url = try makeRoomFile(
            statements: LedgerScript.statements(in: script),
            userVersion: 1,
            hash: "77df879d0324cd71db9dcfae077c9ea4"
        )
        do {
            let raw = try DatabaseQueue(path: url.path)
            try raw.write { db in
                try db.execute(
                    sql: """
                    INSERT INTO account
                    (id, name, icon, color, type, balance, isEnable, remark, createTime, updateTime)
                    VALUES (1, 'Old', 'wallet', 'teal', 0, 0, 0, NULL, '2024-01-01T00:00:00.000', '2024-01-01T00:00:00.000')
                    """
                )
                try db.execute(
                    sql: """
                    INSERT INTO account
                    (id, name, icon, color, type, balance, isEnable, remark, createTime, updateTime)
                    VALUES (2, 'Cash', 'wallet', 'teal', 0, 2500, 1, NULL, '2024-01-02T00:00:00.000', '2024-01-02T00:00:00.000')
                    """
                )
            }
        }
        let database = try LedgerDatabase.open(at: url)
        XCTAssertEqual(try database.schemaVersion(), 4)
        XCTAssertEqual(try database.identityHash(), LedgerSchema.identityHashV4)
        let defaults = try database.read { db in
            try Int64.fetchAll(db, sql: "SELECT id FROM account WHERE is_default = 1 ORDER BY id")
        }
        XCTAssertEqual(defaults, [2])
        let cycle = try database.read { db -> Int in
            try Int.fetchOne(
                db,
                sql: "SELECT COUNT(*) FROM pragma_table_info('budget') WHERE name = 'cycleDay'"
            ) ?? 0
        }
        XCTAssertEqual(cycle, 1)
        let index = try database.read { db -> Int in
            try Int.fetchOne(
                db,
                sql: """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index' AND name = 'index_bill_type_categoryId_billTime'
                """
            ) ?? 0
        }
        XCTAssertEqual(index, 1)
    }

    func testRejectsNewerSchema() throws {
        let url = try makeRoomFile(
            statements: try LedgerSchema.version4Statements(),
            userVersion: 5,
            hash: LedgerSchema.identityHashV4
        )
        XCTAssertThrowsError(try LedgerDatabase.open(at: url)) { error in
            XCTAssertEqual(error as? LedgerStoreError, .newerSchema(found: 5))
        }
    }

    func testRejectsUnversionedDatabase() throws {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("cognibal-unversioned-\(UUID().uuidString).db")
        do {
            let raw = try DatabaseQueue(path: url.path)
            try raw.write { db in
                try db.execute(
                    sql: """
                    CREATE TABLE account (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                    """
                )
            }
        }
        XCTAssertThrowsError(try LedgerDatabase.open(at: url)) { error in
            XCTAssertEqual(error as? LedgerStoreError, .unversioned)
        }
    }

    func testForeignKeyRestrictsAccountDelete() throws {
        let database = try LedgerDatabase.openInMemory()
        try database.write { db in
            try db.execute(
                sql: """
                INSERT INTO account
                (name, icon, color, type, balance, isEnable, remark, createTime, updateTime, is_default)
                VALUES ('Cash', 'wallet', 'teal', 0, 0, 1, NULL, '2026-01-01T00:00:00.000', '2026-01-01T00:00:00.000', 1)
                """
            )
            try db.execute(
                sql: """
                INSERT INTO category (name, icon, color, type, sort, isEnable)
                VALUES ('餐饮', 'restaurant', '#FFF03E3E', 0, 0, 1)
                """
            )
            try db.execute(
                sql: """
                INSERT INTO bill
                (amount, type, categoryId, accountId, billTime, remark, createTime, updateTime)
                VALUES (100, 0, 1, 1, '2026-01-01T08:00:00.000', NULL, '2026-01-01T08:00:00.000', '2026-01-01T08:00:00.000')
                """
            )
        }
        XCTAssertThrowsError(
            try database.write { db in
                try db.execute(sql: "DELETE FROM account WHERE id = 1")
            }
        )
    }

    func testStoredCodesMatchAndroid() {
        XCTAssertEqual(AccountType.cash.rawValue, 0)
        XCTAssertEqual(AccountType.other.rawValue, 4)
        XCTAssertEqual(CategoryType.expense.rawValue, 0)
        XCTAssertEqual(CategoryType.income.rawValue, 1)
        XCTAssertEqual(BillType.expense.rawValue, 0)
        XCTAssertEqual(BillType.income.rawValue, 1)
        XCTAssertEqual(BillType.transfer.rawValue, 2)
        XCTAssertEqual(BudgetType.global.rawValue, 0)
        XCTAssertEqual(BudgetType.category.rawValue, 1)
        XCTAssertEqual(BudgetPeriodType.week.rawValue, 0)
        XCTAssertEqual(BudgetPeriodType.year.rawValue, 3)
        XCTAssertEqual(LedgerLimits.maxMinorAmount, 999_999_999_999)
    }

    private func makeRoomFile(statements: [String], userVersion: Int, hash: String) throws -> URL {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("cognibal-\(UUID().uuidString).db")
        let raw = try DatabaseQueue(path: url.path)
        try raw.write { db in
            for statement in statements {
                try db.execute(sql: statement)
            }
            try db.execute(
                sql: "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            try db.execute(
                sql: "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, ?)",
                arguments: [hash]
            )
            try db.execute(sql: "PRAGMA user_version = \(userVersion)")
        }
        return url
    }

    private func insertSample(at url: URL) throws {
        let raw = try DatabaseQueue(path: url.path)
        try raw.write { db in
            try db.execute(
                sql: """
                INSERT INTO account
                (id, name, icon, color, type, balance, isEnable, remark, createTime, updateTime, is_default)
                VALUES (1, 'Cash', 'wallet', 'teal', 0, 10050, 1, NULL, '2026-01-15T08:30:00.000', '2026-01-15T08:30:00.000', 1)
                """
            )
            try db.execute(
                sql: """
                INSERT INTO category (id, name, icon, color, type, sort, isEnable)
                VALUES (1, '餐饮', 'restaurant', '#FFF03E3E', 0, 0, 1)
                """
            )
            try db.execute(
                sql: """
                INSERT INTO bill
                (id, amount, type, categoryId, accountId, billTime, remark, createTime, updateTime)
                VALUES (1, 1990, 0, 1, 1, '2026-01-15T08:30:00.000', 'coffee', '2026-01-15T08:30:00.000', '2026-01-15T08:30:00.000')
                """
            )
        }
    }

    private func fixtureURL(_ name: String) -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .appendingPathComponent("Fixtures/\(name).sql")
    }
}
