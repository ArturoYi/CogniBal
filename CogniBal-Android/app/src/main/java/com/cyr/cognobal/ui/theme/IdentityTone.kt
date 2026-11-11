package com.cyr.cognobal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.cyr.cognobal.screen.feature.bill.model.accountColorValue

internal fun seatIdentityColor(account: Color, theme: Color): Color = theme

@Composable
@ReadOnlyComposable
internal fun Color.asThemeIdentity(): Color = MaterialTheme.appColors.primary

@Immutable
internal data class SoftBadgeTone(
    val container: Color,
    val content: Color,
)

/**
 * 分类与账户圆角图标的柔和粉彩底色：
 * 在彩色主题下根据条目色值生成低饱和清透圆角底 + 清晰线性图标色；
 * 在黑白主题下自动收敛为纯净中性灰。
 */
@Composable
@ReadOnlyComposable
internal fun softBadgeTone(colorKey: String): SoftBadgeTone {
    val colors = MaterialTheme.appColors
    val style = LocalAppThemeStyle.current
    if (style == AppThemeStyle.Monochrome) {
        return SoftBadgeTone(
            container = colors.tagBackground,
            content = colors.textPrimary,
        )
    }
    val base = if (colorKey.isBlank()) colors.primary else accountColorValue(colorKey)
    return if (colors.isDark) {
        SoftBadgeTone(
            container = lerp(colors.cardBackground, base, 0.22f),
            content = lerp(base, Color.White, 0.26f),
        )
    } else {
        SoftBadgeTone(
            container = lerp(Color.White, base, 0.13f),
            content = lerp(base, Color(0xFF18191E), 0.08f),
        )
    }
}

/**
 * 统计图与分类排行条色彩：非黑白主题按分类独立柔和色值呈现，黑白主题回退同色阶明暗。
 */
internal fun categorySliceColor(
    colorKey: String,
    index: Int,
    total: Int,
    colors: AppColors,
    monochrome: Boolean,
): Color {
    if (monochrome || colorKey.isBlank()) {
        return monochromeSliceTone(
            index = index,
            total = total,
            primary = colors.primary,
            track = colors.tagBackground,
        )
    }
    val raw = accountColorValue(colorKey)
    return if (colors.isDark) lerp(raw, Color.White, 0.16f) else raw
}

/**
 * 同一主色相下的多级明度色阶，供黑白主题环形图区分扇区。
 */
internal fun monochromeSliceTone(
    index: Int,
    total: Int,
    primary: Color,
    track: Color,
): Color {
    if (total <= 1 || index <= 0) return primary
    val step = (index.toFloat() / total.coerceAtLeast(2).toFloat()) * 0.68f
    return lerp(primary, track, step.coerceIn(0f, 0.68f))
}
