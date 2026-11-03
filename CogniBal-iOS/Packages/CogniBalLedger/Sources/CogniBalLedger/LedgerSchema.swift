import Foundation

/// 账本库能打开的结构版本，和安卓 Room `AppDatabase.VERSION` 对齐。
public enum LedgerSchema {
    public static let version = 4
    public static let databaseName = "cognibal.db"

    /// Room schema 4 的 identity hash。安卓打开 iOS 新建的库时靠它核对。
    public static let identityHashV4 = "0cc68dfad0b1054abf4c5c1e56a11b86"

    static func version4Statements() throws -> [String] {
        try statements(resource: "room-v4")
    }

    static func statements(resource name: String) throws -> [String] {
        guard let url = resourceURL(name: name) else {
            throw LedgerStoreError.incomplete
        }
        let script = try String(contentsOf: url, encoding: .utf8)
        let statements = LedgerScript.statements(in: script)
        if statements.isEmpty {
            throw LedgerStoreError.incomplete
        }
        return statements
    }

    private static func resourceURL(name: String) -> URL? {
        if let url = Bundle.module.url(
            forResource: name,
            withExtension: "sql",
            subdirectory: "SchemaSQL"
        ) {
            return url
        }
        return Bundle.module.url(forResource: name, withExtension: "sql")
    }
}

enum LedgerScript {
    /// 按分号切开建表脚本。整行注释丢掉，语句内部不包含分号。
    static func statements(in script: String) -> [String] {
        script.components(separatedBy: ";").compactMap { raw in
            let lines = raw.split(separator: "\n", omittingEmptySubsequences: false).filter { line in
                !line.trimmingCharacters(in: .whitespaces).hasPrefix("--")
            }
            let sql = lines.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
            return sql.isEmpty ? nil : sql
        }
    }
}

public enum LedgerStoreError: Error, Equatable, Sendable {
    /// 文件里的 `user_version` 比这版应用新。
    case newerSchema(found: Int)
    /// 已经有业务表，但没有 Room / iOS 写入的版本号。
    case unversioned
    /// 版本号是当前版本，表却不齐。
    case incomplete
    case io(String)
}
