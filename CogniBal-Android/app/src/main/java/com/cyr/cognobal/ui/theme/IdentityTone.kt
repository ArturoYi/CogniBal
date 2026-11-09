package com.cyr.cognobal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp as lerpFloat

/**
 * 把账户/分类身份色锁进当前主色相。
 * 库存仍是用户选的那支；绘制时只保留明度和饱和度差，用来认出是哪一张卡。
 */
internal fun seatIdentityColor(account: Color, theme: Color): Color {
    val from = account.toHsv()
    val toward = theme.toHsv()
    val colorful = toward.saturation >= 0.18f
    val saturation = if (colorful) {
        lerpFloat(from.saturation, toward.saturation, 0.42f).coerceIn(0.22f, 0.82f)
    } else if (from.saturation < 0.16f) {
        from.saturation.coerceIn(0f, 0.14f)
    } else {
        lerpFloat(from.saturation, 0.08f, 0.82f).coerceIn(0f, 0.16f)
    }
    val value = lerpFloat(from.value, toward.value, 0.28f).coerceIn(0.28f, 0.90f)
    return hsv(hue = toward.hue, saturation = saturation, value = value)
}

@Composable
@ReadOnlyComposable
internal fun Color.asThemeIdentity(): Color = seatIdentityColor(this, MaterialTheme.appColors.hero)

internal data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

internal fun Color.toHsv(): HsvColor {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (max == 0f) 0f else delta / max
    return HsvColor(hue = hue, saturation = saturation, value = max)
}

internal fun hsv(hue: Float, saturation: Float, value: Float): Color {
    return Color.hsv(
        hue = ((hue % 360f) + 360f) % 360f,
        saturation = saturation.coerceIn(0f, 1f),
        value = value.coerceIn(0f, 1f),
    )
}
