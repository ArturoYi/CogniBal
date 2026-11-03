import CogniBalLedger
import SwiftUI

@MainActor
final class AppModel: ObservableObject {
    @Published var preferences: AppPreferences {
        didSet {
            if persistsPreferences {
                preferences.save()
            }
        }
    }

    @Published var storeError: LedgerStoreError?
    let database: LedgerDatabase?
    private let persistsPreferences: Bool

    init() {
        persistsPreferences = true
        preferences = AppPreferences.load()
        do {
            database = try LedgerDatabase.open(at: try LedgerPaths.databaseURL())
            storeError = nil
        } catch let error as LedgerStoreError {
            database = nil
            storeError = error
        } catch {
            database = nil
            storeError = .io(error.localizedDescription)
        }
    }

    init(preview database: LedgerDatabase) {
        persistsPreferences = false
        preferences = AppPreferences(colorScheme: .dark, accent: .warmCream)
        self.database = database
        storeError = nil
    }

    func dismissStoreError() {
        storeError = nil
    }

    static var preview: AppModel {
        do {
            return AppModel(preview: try LedgerDatabase.openInMemory())
        } catch {
            preconditionFailure("预览账本没有打开: \(error)")
        }
    }
}

extension LedgerStoreError {
    var alertMessage: String {
        switch self {
        case .newerSchema:
            String(localized: "This ledger was written by a newer version of CogniBal.")
        case .unversioned:
            String(localized: "This ledger file has no schema version.")
        case .incomplete:
            String(localized: "This ledger is missing tables.")
        case .io:
            String(localized: "The ledger file couldn't be read.")
        }
    }
}
