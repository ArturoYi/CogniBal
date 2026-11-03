import Foundation

/// 把账本里的图标键映射到 SF Symbol。认不出的键用各自的通用符号。
public enum LedgerSymbol {
    public static func accountSystemName(for storageKey: String) -> String {
        switch storageKey {
        case "wallet": "wallet.pass"
        case "cash": "banknote"
        case "card": "creditcard"
        case "bank": "building.columns"
        case "savings": "dollarsign.circle"
        case "phone": "iphone"
        case "bag": "bag"
        case "other": "ellipsis"
        default: "wallet.pass"
        }
    }

    public static func categorySystemName(for storageKey: String) -> String {
        switch storageKey {
        case "restaurant": "fork.knife"
        case "directions_bus": "bus"
        case "shopping_bag": "bag"
        case "home": "house"
        case "movie": "film"
        case "medical_services": "cross.case"
        case "school": "graduationcap"
        case "groups": "person.3"
        case "chat": "bubble.left.and.bubble.right"
        case "pets": "pawprint"
        case "credit_card": "creditcard"
        case "account_balance_wallet": "wallet.pass"
        case "account_balance": "building.columns"
        case "work": "briefcase"
        case "volunteer_activism": "heart"
        case "redeem": "gift"
        case "more_horiz": "ellipsis"
        case "payments": "banknote"
        case "emoji_events": "trophy"
        case "savings": "dollarsign.circle"
        default: "square.grid.2x2"
        }
    }
}
