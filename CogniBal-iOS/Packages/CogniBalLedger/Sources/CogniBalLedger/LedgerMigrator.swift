import Foundation
import GRDB

/// 打开或升级 `cognibal.db`。
///
/// 版本只认 SQLite 的 `user_version`，不建 GRDB 自己的迁移表，这样安卓 Room 仍能打开这份库。
/// 新库按 schema 4 直接建表，并写入 Room 的 identity hash。
enum LedgerMigrator {
    static func migrate(_ db: Database) throws {
        let version = try userVersion(db)
        if version > LedgerSchema.version {
            throw LedgerStoreError.newerSchema(found: version)
        }
        if version == 0 {
            if try tableExists(db, "account") {
                throw LedgerStoreError.unversioned
            }
            try installVersion4(db)
            return
        }
        if version < 1 {
            throw LedgerStoreError.unversioned
        }
        if version < LedgerSchema.version {
            try upgrade(db, from: version)
            return
        }
        try requireCurrentTables(db)
        try ensureIdentityHash(db)
    }

    static func userVersion(_ db: Database) throws -> Int {
        try Int.fetchOne(db, sql: "PRAGMA user_version") ?? 0
    }

    static func identityHash(_ db: Database) throws -> String? {
        guard try tableExists(db, "room_master_table") else { return nil }
        return try String.fetchOne(
            db,
            sql: "SELECT identity_hash FROM room_master_table WHERE id = 42"
        )
    }

    private static func installVersion4(_ db: Database) throws {
        for statement in try LedgerSchema.version4Statements() {
            try db.execute(sql: statement)
        }
        try setUserVersion(db, LedgerSchema.version)
        try writeIdentityHash(db)
    }

    private static func upgrade(_ db: Database, from version: Int) throws {
        var current = version
        if current == 1 {
            try db.execute(
                sql: "ALTER TABLE `account` ADD COLUMN `is_default` INTEGER NOT NULL DEFAULT 0"
            )
            try db.execute(
                sql: """
                UPDATE `account`
                SET `is_default` = 1
                WHERE `id` = (
                    SELECT `id` FROM `account`
                    ORDER BY `isEnable` DESC, `id` ASC
                    LIMIT 1
                )
                """
            )
            current = 2
        }
        if current == 2 {
            try db.execute(
                sql: """
                CREATE INDEX IF NOT EXISTS `index_bill_type_categoryId_billTime`
                ON `bill` (`type`, `categoryId`, `billTime`)
                """
            )
            current = 3
        }
        if current == 3 {
            try db.execute(sql: "ALTER TABLE `budget` ADD COLUMN `cycleDay` INTEGER")
            current = 4
        }
        if current != LedgerSchema.version {
            throw LedgerStoreError.incomplete
        }
        try requireCurrentTables(db)
        try setUserVersion(db, LedgerSchema.version)
        try writeIdentityHash(db)
    }

    private static func requireCurrentTables(_ db: Database) throws {
        for name in ["account", "category", "bill", "budget"] {
            guard try tableExists(db, name) else {
                throw LedgerStoreError.incomplete
            }
        }
        let accountColumns = try columnNames(db, "account")
        let budgetColumns = try columnNames(db, "budget")
        if !accountColumns.contains("is_default") || !budgetColumns.contains("cycleDay") {
            throw LedgerStoreError.incomplete
        }
    }

    private static func ensureIdentityHash(_ db: Database) throws {
        if try identityHash(db) != nil { return }
        try writeIdentityHash(db)
    }

    private static func writeIdentityHash(_ db: Database) throws {
        try db.execute(
            sql: "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
        )
        try db.execute(
            sql: "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, ?)",
            arguments: [LedgerSchema.identityHashV4]
        )
    }

    private static func setUserVersion(_ db: Database, _ version: Int) throws {
        try db.execute(sql: "PRAGMA user_version = \(version)")
    }

    private static func tableExists(_ db: Database, _ name: String) throws -> Bool {
        let count = try Int.fetchOne(
            db,
            sql: "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            arguments: [name]
        ) ?? 0
        return count > 0
    }

    private static func columnNames(_ db: Database, _ table: String) throws -> Set<String> {
        let rows = try Row.fetchAll(db, sql: "PRAGMA table_info(\(table))")
        return Set(rows.compactMap { row -> String? in
            let name: String? = row["name"]
            return name
        })
    }
}
