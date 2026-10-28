package com.example.cognibal.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cognibal.data.database.model.AccountType
import java.time.LocalDateTime

/**
 * 账户。
 *
 * [balance] 是冗余缓存，单位是货币的最小单位（人民币就是分）。
 * 新建账户时它是期初余额；之后只允许在账单事务里按收支增减，不能和账单各改各的。
 * 换币种时，这份余额和账单、预算在同一个事务里乘上同一个汇率。
 *
 * [isDefault] 是首页当前展示的账户。全表只应有一个为 true，启动时选中它。
 * 停用不清掉这个标记；这个账户本身不能物理删除。
 */
@Entity(tableName = "account")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** 图标键，例如 Material 图标名。界面负责把它画出来。 */
    val icon: String,
    /** 颜色键或 `#AARRGGBB`。数据层不解析成 Compose Color。 */
    val color: String,
    val type: AccountType,
    val balance: Long,
    val isEnable: Boolean,
    val remark: String?,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime,
    /** 当前展示的账户。列名用 is_default，和界面上的“当前账户”是同一件事。 */
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
)
