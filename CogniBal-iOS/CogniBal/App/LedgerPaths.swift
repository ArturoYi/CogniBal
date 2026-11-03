import CogniBalLedger
import Foundation

/// 账本文件放在 App Group 里，桌面组件以后读同一份库。
/// 模拟器还没签上 App Group 时，退回应用自己的 Application Support。
enum LedgerPaths {
    static let appGroupIdentifier = "group.app.cognibal.ledger"

    static func databaseURL(fileManager: FileManager = .default) throws -> URL {
        let base: URL
        if let container = fileManager.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupIdentifier
        ) {
            base = container
        } else {
            base = try fileManager.url(
                for: .applicationSupportDirectory,
                in: .userDomainMask,
                appropriateFor: nil,
                create: true
            )
        }
        let folder = base.appendingPathComponent("CogniBal", isDirectory: true)
        try fileManager.createDirectory(at: folder, withIntermediateDirectories: true)
        return folder.appendingPathComponent(LedgerSchema.databaseName)
    }
}
