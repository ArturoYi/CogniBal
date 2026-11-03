import SwiftUI

struct AppearanceSettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Form {
            Section("Theme") {
                Picker("Theme", selection: $model.preferences.colorScheme) {
                    ForEach(ColorSchemePreference.allCases) { preference in
                        Text(preference.title).tag(preference)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            }
            Section("Accent") {
                Picker("Accent", selection: $model.preferences.accent) {
                    ForEach(AccentStyle.allCases) { accent in
                        HStack(spacing: 10) {
                            Circle()
                                .fill(accent.ink(for: colorScheme).primary)
                                .frame(width: 12, height: 12)
                                .accessibilityHidden(true)
                            Text(accent.title)
                        }
                        .tag(accent)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            }
        }
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
    }
}
