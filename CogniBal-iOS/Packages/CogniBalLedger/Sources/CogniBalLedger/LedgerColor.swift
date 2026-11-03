import Foundation

/// 账户和分类的颜色。库存的是键或 `#AARRGGBB`，界面再把它画出来。
public struct LedgerColor: Equatable, Sendable {
    public var red: Double
    public var green: Double
    public var blue: Double
    public var alpha: Double

    public init(red: Double, green: Double, blue: Double, alpha: Double) {
        self.red = red
        self.green = green
        self.blue = blue
        self.alpha = alpha
    }

    /// 浅色底上用深色前景。阈值和安卓分类图标一致。
    public var prefersDarkForeground: Bool {
        let luminance = 0.299 * red + 0.587 * green + 0.114 * blue
        return luminance > 0.62
    }

    public static func resolve(storageKey: String) -> LedgerColor {
        if let named = named[storageKey] { return named }
        if let parsed = parseHex(storageKey) { return parsed }
        return named["teal"] ?? LedgerColor(red: 0.047, green: 0.651, blue: 0.471, alpha: 1)
    }

    private static func parseHex(_ raw: String) -> LedgerColor? {
        let hex = raw.hasPrefix("#") ? String(raw.dropFirst()) : raw
        guard let value = UInt64(hex, radix: 16) else { return nil }
        switch hex.count {
        case 6:
            return color(rgb: value, alpha: 255)
        case 8:
            let alpha = (value & 0xFF00_0000) >> 24
            return color(rgb: value & 0x00FF_FFFF, alpha: alpha)
        default:
            return nil
        }
    }

    private static func color(rgb: UInt64, alpha: UInt64) -> LedgerColor {
        LedgerColor(
            red: Double((rgb & 0xFF0000) >> 16) / 255,
            green: Double((rgb & 0x00FF00) >> 8) / 255,
            blue: Double(rgb & 0x0000FF) / 255,
            alpha: Double(alpha) / 255
        )
    }

    /// Open Color 里账户用的那几档。键写进 `account.color`。
    private static let named: [String: LedgerColor] = [
        "teal": hex("0CA678"),
        "clay": hex("F03E3E"),
        "sand": hex("FD7E14"),
        "blue": hex("228BE6"),
        "lilac": hex("BE4BDB"),
        "ink": hex("495057"),
        "gray": hex("868E96"),
    ]

    private static func hex(_ rgb: String) -> LedgerColor {
        parseHex(rgb) ?? LedgerColor(red: 0, green: 0, blue: 0, alpha: 1)
    }
}
