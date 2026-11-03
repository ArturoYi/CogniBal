import SwiftUI

/// 窗口分档只看尺寸类别，不看机型和方向。
///
/// iPhone Duo 合上是紧凑宽；展开后宽高都是常规，和 iPad 同一套分栏。
/// 半开和完全展开的尺寸类别相同。iOS 27.0 SDK 还没有公开的折痕区域，
/// 半开时先按常规宽的分栏把内容拆开，避免一条控件横跨中线。
enum WindowTier: Equatable {
    case compact
    case compactShort
    case regular
}

private struct WindowTierKey: EnvironmentKey {
    static let defaultValue = WindowTier.compact
}

extension EnvironmentValues {
    var windowTier: WindowTier {
        get { self[WindowTierKey.self] }
        set { self[WindowTierKey.self] = newValue }
    }
}

enum HomeTab: String, Hashable, CaseIterable, Identifiable {
    case bill
    case settings

    var id: String { rawValue }
}
