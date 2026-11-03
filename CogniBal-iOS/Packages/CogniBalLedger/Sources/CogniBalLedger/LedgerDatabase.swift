import Foundation
import GRDB

/// 账本数据库。连接打开时启用外键；文件库使用 WAL，和安卓 Room 一致。
public final class LedgerDatabase {
    private let queue: DatabaseQueue

    public static func open(at url: URL) throws -> LedgerDatabase {
        do {
            let queue = try DatabaseQueue(path: url.path, configuration: fileConfiguration())
            let database = LedgerDatabase(queue: queue)
            try database.migrate()
            return database
        } catch let error as LedgerStoreError {
            throw error
        } catch {
            throw LedgerStoreError.io(error.localizedDescription)
        }
    }

    /// 预览和测试用的内存库。内存库不开 WAL。
    public static func openInMemory() throws -> LedgerDatabase {
        do {
            let queue = try DatabaseQueue(named: nil, configuration: memoryConfiguration())
            let database = LedgerDatabase(queue: queue)
            try database.migrate()
            return database
        } catch let error as LedgerStoreError {
            throw error
        } catch {
            throw LedgerStoreError.io(error.localizedDescription)
        }
    }

    public func schemaVersion() throws -> Int {
        try read { db in try LedgerMigrator.userVersion(db) }
    }

    public func identityHash() throws -> String? {
        try read { db in try LedgerMigrator.identityHash(db) }
    }

    public func read<T>(_ body: (Database) throws -> T) throws -> T {
        do {
            return try queue.read(body)
        } catch let error as LedgerStoreError {
            throw error
        } catch {
            throw LedgerStoreError.io(error.localizedDescription)
        }
    }

    public func write<T>(_ body: (Database) throws -> T) throws -> T {
        do {
            return try queue.write(body)
        } catch let error as LedgerStoreError {
            throw error
        } catch {
            throw LedgerStoreError.io(error.localizedDescription)
        }
    }

    private init(queue: DatabaseQueue) {
        self.queue = queue
    }

    private func migrate() throws {
        do {
            try queue.write { db in
                try LedgerMigrator.migrate(db)
            }
        } catch let error as LedgerStoreError {
            throw error
        } catch {
            throw LedgerStoreError.io(error.localizedDescription)
        }
    }

    private static func fileConfiguration() -> Configuration {
        var configuration = Configuration()
        configuration.foreignKeysEnabled = true
        configuration.journalMode = .wal
        return configuration
    }

    private static func memoryConfiguration() -> Configuration {
        var configuration = Configuration()
        configuration.foreignKeysEnabled = true
        return configuration
    }
}
