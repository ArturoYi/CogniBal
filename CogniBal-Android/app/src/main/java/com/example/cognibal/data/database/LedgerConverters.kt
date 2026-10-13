package com.example.cognibal.data.database

import androidx.room.TypeConverter
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.database.model.CategoryType
import java.time.LocalDateTime

/**
 * Room 把枚举和时间收成 SQLite 能存的整数、文本。
 *
 * 业务代码继续用 [LocalDateTime] 和枚举。未知的整数说明库被写坏了，或者备份来自
 * 更高版本，读取时直接失败，不用 0 冒充一种合法类型。
 */
class LedgerConverters {
    @TypeConverter
    fun encodeTime(value: LocalDateTime): String = LedgerTime.encode(value)

    @TypeConverter
    fun decodeTime(value: String): LocalDateTime = LedgerTime.decode(value)

    @TypeConverter
    fun encodeAccountType(value: AccountType): Int = value.code

    @TypeConverter
    fun decodeAccountType(value: Int): AccountType = AccountType.fromCode(value)

    @TypeConverter
    fun encodeCategoryType(value: CategoryType): Int = value.code

    @TypeConverter
    fun decodeCategoryType(value: Int): CategoryType = CategoryType.fromCode(value)

    @TypeConverter
    fun encodeBillType(value: BillType): Int = value.code

    @TypeConverter
    fun decodeBillType(value: Int): BillType = BillType.fromCode(value)

    @TypeConverter
    fun encodeBudgetType(value: BudgetType): Int = value.code

    @TypeConverter
    fun decodeBudgetType(value: Int): BudgetType = BudgetType.fromCode(value)

    @TypeConverter
    fun encodeBudgetPeriodType(value: BudgetPeriodType): Int = value.code

    @TypeConverter
    fun decodeBudgetPeriodType(value: Int): BudgetPeriodType = BudgetPeriodType.fromCode(value)
}
