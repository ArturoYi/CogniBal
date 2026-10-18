package com.example.cognibal.screen.expense.tool

/**
 * 金额只接受数字和一个小数点，小数最多两位。
 * 整数部分最多 10 位，对应单笔上限 9,999,999,999.99。
 */
internal sealed interface AmountKey {
    data class Digit(val value: Char) : AmountKey
    data object Decimal : AmountKey
    data object Backspace : AmountKey
}

internal fun applyAmountKey(current: String, key: AmountKey): String = when (key) {
    is AmountKey.Digit -> appendDigit(current, key.value)
    AmountKey.Decimal -> appendDecimal(current)
    AmountKey.Backspace -> current.dropLast(1)
}

private fun appendDigit(current: String, digit: Char): String {
    if (current == "0") return if (digit == '0') current else digit.toString()
    val dot = current.indexOf('.')
    if (dot >= 0) {
        if (current.length - dot - 1 >= DECIMAL_DIGITS) return current
        return current + digit
    }
    if (current.length >= MAJOR_DIGITS) return current
    return current + digit
}

private fun appendDecimal(current: String): String = when {
    current.contains('.') -> current
    current.isEmpty() -> "0."
    else -> "$current."
}

private const val MAJOR_DIGITS = 10
private const val DECIMAL_DIGITS = 2
