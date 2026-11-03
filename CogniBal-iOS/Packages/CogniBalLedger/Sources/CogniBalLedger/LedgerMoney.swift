import Foundation

/// 把「分」格式化成带分组的两位小数，小数点固定为 `.`，和安卓 `formatMinorAmount` 一致。
public enum LedgerMoney {
    public static func formatMinor(_ amount: Int64, locale: Locale = .current) -> String {
        if amount == Int64.min { return "—" }
        let negative = amount < 0
        let absolute = negative ? -amount : amount
        let major = absolute / 100
        let minor = Int(absolute % 100)
        let text = "\(groupedInteger(major, locale: locale)).\(String(format: "%02d", minor))"
        return negative ? "-\(text)" : text
    }

    /// 把用户输入的主单位金额收成分。空字符串是 0。超出上限时返回 nil。
    public static func parseMajor(_ raw: String) -> Int64? {
        var text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "，", with: ",")
            .replacingOccurrences(of: "。", with: ".")
        if text.isEmpty { return 0 }
        let negative = text.hasPrefix("-")
        if text.hasPrefix("-") || text.hasPrefix("+") {
            text.removeFirst()
        }
        if text.isEmpty { return nil }

        let dot = text.lastIndex(of: ".")
        let comma = text.lastIndex(of: ",")
        let decimalIndex: String.Index?
        switch (dot, comma) {
        case let (dot?, comma?):
            decimalIndex = dot > comma ? dot : comma
        case let (dot?, nil):
            decimalIndex = dot
        case let (nil, comma?):
            decimalIndex = comma
        default:
            decimalIndex = nil
        }

        let majorDigits: String
        let fractionDigits: String
        if let decimalIndex {
            let fractionRaw = text[text.index(after: decimalIndex)...]
            if fractionRaw.contains(where: { $0 == "." || $0 == "," || !$0.isNumber }) {
                return nil
            }
            if fractionRaw.count > 2 { return nil }
            majorDigits = text[..<decimalIndex].filter(\.isNumber)
            fractionDigits = String(fractionRaw)
        } else {
            if text.contains(where: { !$0.isNumber }) { return nil }
            majorDigits = text
            fractionDigits = ""
        }
        if majorDigits.isEmpty && fractionDigits.isEmpty { return nil }
        guard let major = Int64(majorDigits.isEmpty ? "0" : majorDigits) else { return nil }
        let paddedFraction = fractionDigits.isEmpty
            ? "0"
            : fractionDigits.padding(toLength: 2, withPad: "0", startingAt: 0)
        guard let minor = Int64(paddedFraction) else { return nil }
        let (product, productOverflow) = major.multipliedReportingOverflow(by: 100)
        if productOverflow { return nil }
        let (total, totalOverflow) = product.addingReportingOverflow(minor)
        if totalOverflow || total > LedgerLimits.maxMinorAmount { return nil }
        if negative {
            let (signed, signedOverflow) = total.multipliedReportingOverflow(by: -1)
            if signedOverflow { return nil }
            return signed
        }
        return total
    }

    private static func groupedInteger(_ value: Int64, locale: Locale) -> String {
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        formatter.usesGroupingSeparator = true
        return formatter.string(from: NSNumber(value: value)) ?? String(value)
    }
}
