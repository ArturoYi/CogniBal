package com.example.cognibal.data.repository

/** 名称、图标、备注的长度。超长直接拒绝，不在入库时悄悄截断。 */
internal object LedgerLimits {
    const val NAME: Int = 32
    const val TOKEN: Int = 64
    const val REMARK: Int = 200

    /** 单笔金额上限：9,999,999,999.99 元。再大就拒绝，避免余额加减溢出。 */
    const val MAX_MINOR_AMOUNT: Long = 999_999_999_999L
}

internal object LedgerText {
    fun requireName(raw: String): String {
        val name = raw.trim()
        if (name.isEmpty()) throw LedgerException(LedgerReject.BlankName)
        if (name.length > LedgerLimits.NAME) throw LedgerException(LedgerReject.NameTooLong)
        return name
    }

    fun requireIcon(raw: String): String = requireToken(raw, LedgerReject.BlankIcon)

    fun requireColor(raw: String): String = requireToken(raw, LedgerReject.BlankColor)

    fun cleanRemark(raw: String?): String? {
        val value = raw?.trim()?.ifEmpty { null } ?: return null
        if (value.length > LedgerLimits.REMARK) throw LedgerException(LedgerReject.RemarkTooLong)
        return value
    }

    fun requirePositiveAmount(amount: Long) {
        if (amount <= 0L || amount > LedgerLimits.MAX_MINOR_AMOUNT) {
            throw LedgerException(LedgerReject.InvalidAmount)
        }
    }

    private fun requireToken(raw: String, blank: LedgerReject): String {
        val value = raw.trim()
        if (value.isEmpty()) throw LedgerException(blank)
        if (value.length > LedgerLimits.TOKEN) throw LedgerException(blank)
        return value
    }
}
