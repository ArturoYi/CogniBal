package com.example.cognibal.data.repository

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 汇率：1 个旧货币主单位等于多少个新货币主单位。
 *
 * 账单、余额、预算都按最小单位（分）保存，两种货币都是两位小数，
 * 所以新的分数 = 旧的分数 × 汇率，再四舍五入到整数分。
 */
internal object ExchangeRate {
    const val MAX_INTEGER_DIGITS: Int = 7
    const val MAX_FRACTION_DIGITS: Int = 8

    /** 全屏等待至少停留这么久，避免换算太快时一闪而过。 */
    const val MIN_SWITCH_MILLIS: Long = 1_500L

    /**
     * 数据库换算最长运行这么久。超时就取消，事务回滚，不会留下改了一半的金额。
     */
    const val MAX_SWITCH_MILLIS: Long = 20_000L

    fun sanitizeInput(raw: String): String {
        val builder = StringBuilder()
        var dot = false
        var fractionDigits = 0
        var integerDigits = 0
        for (character in raw.trim()) {
            when {
                character.isDigit() -> {
                    if (dot) {
                        if (fractionDigits >= MAX_FRACTION_DIGITS) continue
                        fractionDigits += 1
                    } else {
                        if (integerDigits >= MAX_INTEGER_DIGITS) continue
                        if (!(integerDigits == 0 && character == '0' && builder.isEmpty())) {
                            integerDigits += 1
                        }
                    }
                    builder.append(character)
                }
                (character == '.' || character == ',' || character == '，') && !dot -> {
                    if (builder.isEmpty()) builder.append('0')
                    dot = true
                    builder.append('.')
                }
            }
        }
        return builder.toString()
    }

    fun parse(raw: String): BigDecimal? {
        val text = sanitizeInput(raw)
        if (text.isEmpty() || text.endsWith('.')) return null
        val rate = try {
            BigDecimal(text)
        } catch (_: NumberFormatException) {
            return null
        }
        if (rate.signum() <= 0) return null
        if (rate.precision() - rate.scale() > MAX_INTEGER_DIGITS) return null
        if (rate.scale() > MAX_FRACTION_DIGITS) return null
        return rate
    }

    /**
     * 把一笔最小单位金额乘上汇率。
     * 结果放不进 Long 时抛出 [LedgerException]，调用方应回滚整个事务。
     */
    fun convertMinorUnits(amount: Long, rate: BigDecimal): Long {
        if (amount == 0L) return 0L
        check(rate.signum() > 0) { "汇率必须大于 0" }
        return try {
            BigDecimal.valueOf(amount)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: ArithmeticException) {
            throw LedgerException(LedgerReject.BalanceOverflow)
        }
    }
}
