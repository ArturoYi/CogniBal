package com.cyr.cognobal.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 接近预算保持在当前主色相体系内，以稍浅一档与正常进度区分；仅超支时使用 [AppColors.expense]。
 */
internal fun budgetNearColor(colors: AppColors): Color =
    lerp(colors.primary, colors.textTertiary, 0.35f)

internal fun budgetNearColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFD4D4D8) else Color(0xFF3F3F46)
