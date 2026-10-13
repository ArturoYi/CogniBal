package com.example.cognibal.data.database.model

/**
 * 账户种类。数字会写进 SQLite，发出去之后不能改含义。
 *
 * 余额规则对这几种账户相同：支出减少 [com.example.cognibal.data.database.entity.Account.balance]，
 * 收入增加。信用卡余额为负时表示欠款，界面自己解释，库里不另存一个“负债”字段。
 */
enum class AccountType(val code: Int) {
    /** 现金。 */
    Cash(0),

    /** 储蓄卡。 */
    DebitCard(1),

    /** 信用卡。 */
    CreditCard(2),

    /** 支付宝、微信等电子钱包。 */
    EWallet(3),

    /** 上面没有覆盖的账户。 */
    Other(4),
    ;

    companion object {
        fun fromCode(code: Int): AccountType = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("未知账户类型: $code")
    }
}

/**
 * 分类只有支出和收入两种。转账不使用分类。
 *
 * 0 和 1 与账单类型的前两个值相同，但是两套枚举。比较时不要把 [BillType] 和这里混用。
 */
enum class CategoryType(val code: Int) {
    /** 支出分类。只能挂到 type = 0 的账单。 */
    Expense(0),

    /** 收入分类。只能挂到 type = 1 的账单。 */
    Income(1),
    ;

    companion object {
        fun fromCode(code: Int): CategoryType = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("未知分类类型: $code")
    }
}

/**
 * 账单类型。聚合 SQL 里写死了 0、1、2，改这里的数字必须同时改 DAO。
 * [com.example.cognibal.data.database.AppDatabase.open] 会在打开库时核对这三个值。
 */
enum class BillType(val code: Int) {
    /** 支出。金额为正，账户余额减少。 */
    Expense(0),

    /** 收入。金额为正，账户余额增加。 */
    Income(1),

    /**
     * 转账的一条腿。一次转账写成两条，分类必须为空。
     * 转出那条金额为负，转入那条金额为正。展示时取绝对值。
     */
    Transfer(2),
    ;

    companion object {
        fun fromCode(code: Int): BillType = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("未知账单类型: $code")
    }
}

/**
 * 预算范围。已花费不存在这张表里，看账单时再按区间求和。
 */
enum class BudgetType(val code: Int) {
    /** 统计区间内全部支出，[com.example.cognibal.data.database.entity.Budget.categoryId] 必须为空。 */
    Global(0),

    /** 只统计一个支出分类。分类必须是 [CategoryType.Expense]。 */
    Category(1),
    ;

    companion object {
        fun fromCode(code: Int): BudgetType = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("未知预算类型: $code")
    }
}

/**
 * 预算周期种类。真正参与查询的是开始、结束两个时间，这个字段只说明区间是怎么来的。
 */
enum class BudgetPeriodType(val code: Int) {
    Week(0),
    Month(1),
    Quarter(2),
    Year(3),
    ;

    companion object {
        fun fromCode(code: Int): BudgetPeriodType = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("未知预算周期: $code")
    }
}
