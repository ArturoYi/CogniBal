package com.example.cognibal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 接近预算仍用中性灰，和正常进度的墨色分开。超支才用 [AppColors.expense]。
 */
internal fun budgetNearColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFD4D4D8) else Color(0xFF3F3F46)
