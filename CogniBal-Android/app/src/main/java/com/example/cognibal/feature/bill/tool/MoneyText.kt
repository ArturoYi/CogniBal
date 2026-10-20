package com.example.cognibal.screen.feature.bill.tool

import com.example.cognibal.data.repository.LedgerLimits
import java.text.NumberFormat
import java.util.Locale

/**
 * 把分格式化成带分组的两位小数，不含货币符号。符号由偏好决定，界面另画。
 */
internal fun formatMinorAmount(amount: Long, locale: Locale = Locale.getDefault()): String {
    if (amount == Long.MIN_VALUE) return "—"
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    val major = absolute / 100
    val minor = (absolute % 100).toInt()
    val grouped = NumberFormat.getIntegerInstance(locale).format(major)
    val text = "$grouped.${minor.toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}

/**
 * 把用户输入的主单位金额收成分。
 *
 * 空字符串是 0。小数点可以用 `.` 或 `,`，最多两位。
 * 同时出现两种分隔符时，最后那个当作小数点，前面的逗号或点当作分组。
 * 超出单笔上限时返回 null，调用方提示金额无效。
 */
internal fun parseMajorAmount(raw: String): Long? {
    var text = raw.trim()
        .replace(" ", "")
        .replace("，", ",")
        .replace("。", ".")
    if (text.isEmpty()) return 0L
    val negative = text.startsWith("-")
    if (text.startsWith("-") || text.startsWith("+")) {
        text = text.substring(1)
    }
    if (text.isEmpty()) return null

    val decimalIndex = maxOf(text.lastIndexOf('.'), text.lastIndexOf(','))
    val majorDigits: String
    val fractionDigits: String
    if (decimalIndex < 0) {
        if (text.any { !it.isDigit() }) return null
        majorDigits = text
        fractionDigits = ""
    } else {
        val fractionRaw = text.substring(decimalIndex + 1)
        if (fractionRaw.any { it == '.' || it == ',' || !it.isDigit() }) return null
        if (fractionRaw.length > 2) return null
        majorDigits = text.substring(0, decimalIndex).filter { it.isDigit() }
        fractionDigits = fractionRaw
    }
    if (majorDigits.isEmpty() && fractionDigits.isEmpty()) return null
    val major = majorDigits.ifEmpty { "0" }.toLongOrNull() ?: return null
    val minor = fractionDigits.padEnd(2, '0').ifEmpty { "0" }.toInt()
    val total = try {
        Math.addExact(Math.multiplyExact(major, 100L), minor.toLong())
    } catch (_: ArithmeticException) {
        return null
    }
    if (total > LedgerLimits.MAX_MINOR_AMOUNT) return null
    return if (negative) -total else total
}
