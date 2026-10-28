package com.example.cognibal.shortcut

import android.content.Intent
import com.example.cognibal.data.database.model.BillType

/**
 * 桌面长按图标时的快捷方式。
 *
 * 系统只认清单里的静态快捷方式，点开后用 [ACTION_ADD] 把要记的类型交给 [com.example.cognibal.MainActivity]。
 */
internal object AppShortcuts {
    const val ACTION_ADD: String = "com.example.cognibal.action.ADD_BILL"
    const val EXTRA_KIND: String = "com.example.cognibal.extra.BILL_KIND"
    const val KIND_EXPENSE: String = "expense"
    const val KIND_INCOME: String = "income"

    fun typeCode(intent: Intent?): Int? {
        if (intent?.action != ACTION_ADD) return null
        return when (intent.getStringExtra(EXTRA_KIND)) {
            KIND_INCOME -> BillType.Income.code
            KIND_EXPENSE -> BillType.Expense.code
            else -> null
        }
    }
}
