package com.cyr.cognobal.shortcut

import android.content.Intent
import com.cyr.cognobal.data.database.model.BillType

/**
 * 桌面长按图标时的快捷方式，以及桌面卡片点按后打开记账页。
 *
 * 系统只认清单里的静态快捷方式，点开后用 [ACTION_ADD] 把要记的类型交给 [com.cyr.cognobal.MainActivity]。
 * 卡片用 [ACTION_OPEN_BILL]，直接回到记账首页。
 */
internal object AppShortcuts {
    const val ACTION_ADD: String = "com.cyr.cognobal.action.ADD_BILL"
    const val ACTION_OPEN_BILL: String = "com.cyr.cognobal.action.OPEN_BILL"
    const val EXTRA_KIND: String = "com.cyr.cognobal.extra.BILL_KIND"
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

    fun opensBill(intent: Intent?): Boolean = intent?.action == ACTION_OPEN_BILL
}
