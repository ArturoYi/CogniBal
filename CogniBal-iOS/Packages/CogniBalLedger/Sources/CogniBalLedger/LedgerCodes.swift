import Foundation

/// 写入 SQLite 的类型码。数字和安卓 `LedgerCodes` 一致，发出去之后不能改含义。
public enum AccountType: Int64, CaseIterable, Sendable, Codable {
    case cash = 0
    case debitCard = 1
    case creditCard = 2
    case eWallet = 3
    case other = 4
}

public enum CategoryType: Int64, CaseIterable, Sendable, Codable {
    case expense = 0
    case income = 1
}

public enum BillType: Int64, CaseIterable, Sendable, Codable {
    case expense = 0
    case income = 1
    case transfer = 2
}

public enum BudgetType: Int64, CaseIterable, Sendable, Codable {
    case global = 0
    case category = 1
}

public enum BudgetPeriodType: Int64, CaseIterable, Sendable, Codable {
    case week = 0
    case month = 1
    case quarter = 2
    case year = 3
}

public enum LedgerLimits {
    public static let name = 32
    public static let token = 64
    public static let remark = 200
    /// 单笔金额上限：9,999,999,999.99 元。
    public static let maxMinorAmount: Int64 = 999_999_999_999
}
