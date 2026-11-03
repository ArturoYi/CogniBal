package com.example.cognibal.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * 卡片保持纯色块。不加描边，也不加投影和泛光。
 * 参数留着，是为了调用处仍然按形状声明这一块区域。
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.cardFace(shape: Shape): Modifier = this
