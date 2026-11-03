import SwiftUI

struct AccentInk: Equatable {
    var primary: Color
    var onPrimary: Color
    var secondary: Color
}

enum ColorSchemePreference: String, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .system: "Match system"
        case .light: "Light"
        case .dark: "Dark"
        }
    }

    var scheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

enum AccentStyle: String, CaseIterable, Identifiable {
    case warmCream = "WarmCream"
    case vitality = "Vitality"
    case neon = "Neon"
    case lime = "Lime"
    case cream = "Cream"

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .warmCream: "Black and white"
        case .vitality: "Orange and teal"
        case .neon: "Purple and pink"
        case .lime: "Lime and cyan"
        case .cream: "Cream sage"
        }
    }

    func ink(for colorScheme: ColorScheme) -> AccentInk {
        switch (self, colorScheme) {
        case (.warmCream, .light):
            AccentInk(primary: rgb(0x1A1A1A), onPrimary: rgb(0xFAFAFA), secondary: rgb(0x52525B))
        case (.warmCream, .dark):
            AccentInk(primary: rgb(0xF5F5F5), onPrimary: rgb(0x141414), secondary: rgb(0xA1A1AA))
        case (.vitality, .light):
            AccentInk(primary: rgb(0xC2410C), onPrimary: .white, secondary: rgb(0x0F766E))
        case (.vitality, .dark):
            AccentInk(primary: rgb(0xFF6B35), onPrimary: rgb(0x1A1A1A), secondary: rgb(0x4ECDC4))
        case (.neon, .light):
            AccentInk(primary: rgb(0x7E22CE), onPrimary: .white, secondary: rgb(0xBE185D))
        case (.neon, .dark):
            AccentInk(primary: rgb(0xA855F7), onPrimary: rgb(0x1A1A1A), secondary: rgb(0xEC4899))
        case (.lime, .light):
            AccentInk(primary: rgb(0x4D7C0F), onPrimary: .white, secondary: rgb(0x0E7490))
        case (.lime, .dark):
            AccentInk(primary: rgb(0x84CC16), onPrimary: rgb(0x1A2E05), secondary: rgb(0x06B6D4))
        case (.cream, .light):
            AccentInk(primary: rgb(0x3D6B5A), onPrimary: .white, secondary: rgb(0x6F5E48))
        case (.cream, .dark):
            AccentInk(primary: rgb(0x7D9A86), onPrimary: rgb(0x1A1A1A), secondary: rgb(0xC4B59A))
        case (_, _):
            AccentInk(primary: rgb(0x1A1A1A), onPrimary: rgb(0xFAFAFA), secondary: rgb(0x52525B))
        }
    }
}

struct AppPreferences: Equatable {
    var colorScheme: ColorSchemePreference
    var accent: AccentStyle

    private static let schemeKey = "cognibal.colorScheme"
    private static let accentKey = "cognibal.accent"

    static func load(defaults: UserDefaults = .standard) -> AppPreferences {
        let scheme = ColorSchemePreference(rawValue: defaults.string(forKey: schemeKey) ?? "") ?? .dark
        let accent = AccentStyle(rawValue: defaults.string(forKey: accentKey) ?? "") ?? .warmCream
        return AppPreferences(colorScheme: scheme, accent: accent)
    }

    func save(defaults: UserDefaults = .standard) {
        defaults.set(colorScheme.rawValue, forKey: Self.schemeKey)
        defaults.set(accent.rawValue, forKey: Self.accentKey)
    }
}

private func rgb(_ hex: UInt32) -> Color {
    Color(
        red: Double((hex & 0xFF0000) >> 16) / 255,
        green: Double((hex & 0x00FF00) >> 8) / 255,
        blue: Double(hex & 0x0000FF) / 255
    )
}
