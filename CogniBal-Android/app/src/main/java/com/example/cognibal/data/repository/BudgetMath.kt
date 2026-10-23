package com.example.cognibal.data.repository

import com.example.cognibal.data.database.model.BudgetType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * 预算比较。
 *
 * 金额用分，比较用 [BigDecimal]，不经过 float 或 double。
 * 进度条要的 0 到 1 只在最后一步变成 [Float]，不参与是否超支的判断。
 */
internal object BudgetMath {
    const val DEFAULT_ALERT_PERCENT: Int = 80
    const val MIN_ALERT_PERCENT: Int = 50
    const val MAX_ALERT_PERCENT: Int = 100

    fun alertPercentAllowed(percent: Int): Boolean =
        percent in MIN_ALERT_PERCENT..MAX_ALERT_PERCENT

    /**
     * 两个半开区间 `[start, end)` 是否相交。
     * 上一个周期正好在这个周期开始时结束，不算相交。
     */
    fun overlaps(
        start: LocalDateTime,
        end: LocalDateTime,
        otherStart: LocalDateTime,
        otherEnd: LocalDateTime,
    ): Boolean = start < otherEnd && otherStart < end

    /** 同一笔支出会命中的范围。总预算和分类预算不是同一范围。 */
    fun sameDimension(
        type: BudgetType,
        categoryId: Long?,
        otherType: BudgetType,
        otherCategoryId: Long?,
    ): Boolean = type == otherType && categoryId == otherCategoryId

    fun contains(start: LocalDateTime, end: LocalDateTime, moment: LocalDateTime): Boolean =
        !moment.isBefore(start) && moment.isBefore(end)

    /** 已花费达到上限。相等就是 100%。 */
    fun isOver(spent: Long, limit: Long): Boolean = limit > 0L && spent >= limit

    /** 已花费达到预警线，但还没到 100%。预警线本身不合法时不预警。 */
    fun isNear(spent: Long, limit: Long, percent: Int): Boolean {
        if (!alertPercentAllowed(percent) || limit <= 0L || spent <= 0L) return false
        if (isOver(spent, limit)) return false
        return BigDecimal.valueOf(spent) * BigDecimal(100) >=
            BigDecimal.valueOf(limit) * BigDecimal(percent)
    }

    fun level(spent: Long, limit: Long, percent: Int): BudgetLevel = when {
        isOver(spent, limit) -> BudgetLevel.Over
        isNear(spent, limit, percent) -> BudgetLevel.Near
        else -> BudgetLevel.Normal
    }

    /** 给进度条用。超支时停在 1，判断超支不要用这个数。 */
    fun progressFraction(spent: Long, limit: Long): Float {
        if (limit <= 0L || spent <= 0L) return 0f
        if (spent >= limit) return 1f
        return BigDecimal.valueOf(spent)
            .divide(BigDecimal.valueOf(limit), 4, RoundingMode.DOWN)
            .toFloat()
    }
}

/** 一条预算此刻的消耗。比较规则见 [BudgetMath]。 */
enum class BudgetLevel {
    Normal,
    Near,
    Over,
}
