package com.example.cognibal.screen.feature.expense.tool

/**
 * 金额只接受数字和一个小数点，小数最多两位。
 * 整数部分最多 10 位，对应单笔上限 9,999,999,999.99。
 */
internal fun sanitizeAmountInput(raw: String): String {
    val major = StringBuilder()
    var fraction: StringBuilder? = null
    for (char in raw) {
        when {
            char.isDigit() -> if (fraction == null) {
                if (major.toString() == "0") major.clear()
                if (major.length < MAJOR_DIGITS) major.append(char)
            } else if (fraction.length < DECIMAL_DIGITS) {
                fraction.append(char)
            }
            (char == '.' || char == ',' || char == '。' || char == '．') && fraction == null -> {
                fraction = StringBuilder()
            }
        }
    }
    if (major.isEmpty() && fraction == null) return ""
    if (major.isEmpty()) major.append('0')
    return if (fraction == null) major.toString() else "$major.$fraction"
}

private const val MAJOR_DIGITS = 10
private const val DECIMAL_DIGITS = 2
