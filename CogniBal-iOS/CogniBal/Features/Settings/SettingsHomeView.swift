import SwiftUI

enum SettingsDestination: String, CaseIterable, Hashable, Identifiable {
    case overview
    case statistics
    case reports
    case budget
    case assets
    case backup
    case security
    case appearance
    case more
    case about

    var id: String { rawValue }

    static let tools: [SettingsDestination] = [.overview, .statistics, .reports, .budget, .assets]
    static let preferences: [SettingsDestination] = [.backup, .security, .appearance, .more]
    static let support: [SettingsDestination] = [.about]

    var title: LocalizedStringKey {
        switch self {
        case .overview: "Overview"
        case .statistics: "Statistics"
        case .reports: "Reports"
        case .budget: "Budget"
        case .assets: "Assets"
        case .backup: "Backup and migration"
        case .security: "Security and privacy"
        case .appearance: "Appearance"
        case .more: "More"
        case .about: "About CogniBal"
        }
    }

    var symbol: String {
        switch self {
        case .overview: "calendar"
        case .statistics: "chart.pie"
        case .reports: "chart.bar"
        case .budget: "target"
        case .assets: "building.columns"
        case .backup: "externaldrive"
        case .security: "lock"
        case .appearance: "paintpalette"
        case .more: "ellipsis.circle"
        case .about: "info.circle"
        }
    }
}

struct SettingsList: View {
    private var selection: Binding<SettingsDestination?>?

    init() {
        selection = nil
    }

    init(selection: Binding<SettingsDestination?>) {
        self.selection = selection
    }

    var body: some View {
        List(selection: selection) {
            Section("Ledger tools") {
                ForEach(SettingsDestination.tools) { destination in
                    row(destination)
                }
            }
            Section("Preferences") {
                ForEach(SettingsDestination.preferences) { destination in
                    row(destination)
                }
            }
            Section("Support") {
                ForEach(SettingsDestination.support) { destination in
                    row(destination)
                }
            }
        }
        .navigationTitle("Settings")
        .listStyle(.insetGrouped)
    }

    @ViewBuilder
    private func row(_ destination: SettingsDestination) -> some View {
        if selection == nil {
            NavigationLink {
                SettingsPage(destination: destination)
            } label: {
                Label(destination.title, systemImage: destination.symbol)
            }
        } else {
            Label(destination.title, systemImage: destination.symbol)
                .tag(destination)
        }
    }
}

struct SettingsPage: View {
    let destination: SettingsDestination

    var body: some View {
        switch destination {
        case .appearance:
            AppearanceSettingsView()
        case .about:
            AboutView()
        default:
            PlaceholderPage(title: destination.title)
        }
    }
}

struct SettingsEmptyDetail: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "gearshape")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("Choose a setting")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
    }
}

struct PlaceholderPage: View {
    let title: LocalizedStringKey

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "tray")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("Nothing here yet.")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct AboutView: View {
    private var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }

    var body: some View {
        Form {
            Section {
                LabeledContent("Version", value: version)
            } footer: {
                Text("CogniBal keeps the ledger on this device.")
            }
        }
        .navigationTitle("About CogniBal")
        .navigationBarTitleDisplayMode(.inline)
    }
}
