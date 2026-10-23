package com.example.cognibal.ui.theme

import androidx.compose.ui.graphics.Color

/** 接近预算的黄色。超支仍用 [AppColors.expense]。 */
internal fun budgetNearColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFE4C56A) else Color(0xFF8C6A1A)
