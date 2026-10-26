package com.example.cognibal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 接近预算的提示色。超支仍用 [AppColors.expense]。
 * 浅色用 orange 9：yellow 整组在浅底上对比不够。暗色用 yellow 5。
 */
internal fun budgetNearColor(isDark: Boolean): Color =
    if (isDark) OpenColor.Yellow5 else OpenColor.Orange9
