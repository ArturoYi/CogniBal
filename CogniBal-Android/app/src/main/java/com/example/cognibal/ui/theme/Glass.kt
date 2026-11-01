package com.example.cognibal.ui.theme

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 暗色玻璃描边。相对石板页面底的对比超过 3:1。
 * 浅色卡片靠白底和阴影分开，不加这条线。
 */
val GlassRim = Color(0xFF8B9BB0)

fun Modifier.glassEdge(shape: Shape, enabled: Boolean): Modifier =
    if (enabled) border(width = 1.dp, color = GlassRim, shape = shape) else this
