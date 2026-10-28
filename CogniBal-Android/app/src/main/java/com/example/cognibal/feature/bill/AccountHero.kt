package com.example.cognibal.screen.feature.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.cognibal.ui.theme.appColors

internal data class AccountHeroPaint(
    val base: Color,
    val wash: Color,
    val on: Color,
)

/**
 * 结余卡的底。首页和新建账户用同一层渐变、落影和波浪，两边才认得出是同一张卡。
 * 浅色直接铺账户色，深色压到暗底上，字色按明度切换。
 */
@Composable
internal fun AccountHeroSurface(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(AccountHeroPaint) -> Unit,
) {
    val paint = accountHeroPaint(color, MaterialTheme.appColors.isDark)
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false,
                ambientColor = paint.base.copy(alpha = 0.18f),
                spotColor = paint.base.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(Brush.linearGradient(colors = listOf(paint.wash, paint.base))),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) { drawAccountHeroWave(paint.on) }
        content(paint)
    }
}

internal fun accountHeroPaint(accountColor: Color, isDark: Boolean): AccountHeroPaint {
    val paper = if (isDark) Color(0xFF101418) else Color.White
    val base = if (isDark) accountColor.copy(alpha = 0.82f).compositeOver(paper) else accountColor
    val wash = if (isDark) {
        Color.White.copy(alpha = 0.12f).compositeOver(base)
    } else {
        Color.White.copy(alpha = 0.24f).compositeOver(base)
    }
    val on = if (base.luminance() > 0.62f) Color(0xFF212529) else Color.White
    return AccountHeroPaint(base = base, wash = wash, on = on)
}

internal fun DrawScope.drawAccountHeroWave(color: Color) {
    val path = Path()
    val base = size.height * 0.72f
    val lift = size.minDimension * 0.08f
    path.moveTo(0f, size.height)
    path.lineTo(0f, base)
    path.quadraticBezierTo(size.width * 0.28f, base - lift, size.width * 0.55f, base)
    path.quadraticBezierTo(size.width * 0.82f, base + lift * 0.7f, size.width, base - lift * 0.25f)
    path.lineTo(size.width, size.height)
    path.close()
    drawPath(path, color.copy(alpha = 0.12f))
}
