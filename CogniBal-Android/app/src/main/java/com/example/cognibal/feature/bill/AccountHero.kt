package com.example.cognibal.screen.feature.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.cognibal.ui.theme.GlassRim
import com.example.cognibal.ui.theme.appColors

internal data class AccountHeroPaint(
    val base: Color,
    val wash: Color,
    val on: Color,
)

/**
 * 结余卡的底。首页和新建账户用同一层，两边才认得出是同一张卡。
 * 浅色直接铺账户色。暗色是玻璃：账户色只做一层薄染色，字色按明度切换。
 */
@Composable
internal fun AccountHeroSurface(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(AccountHeroPaint) -> Unit,
) {
    val isDark = MaterialTheme.appColors.isDark
    val paint = accountHeroPaint(color, isDark)
    val shape = MaterialTheme.shapes.extraLarge
    val elevation = if (isDark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = paint.base.copy(alpha = 0.18f),
            spotColor = paint.base.copy(alpha = 0.22f),
        )
    }
    val rim = if (isDark) Modifier.border(1.dp, GlassRim, shape) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(elevation)
            .clip(shape)
            .background(Brush.linearGradient(colors = listOf(paint.wash, paint.base)))
            .then(rim),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (isDark) drawGlassSheen(paint.on) else drawAccountHeroWave(paint.on)
        }
        content(paint)
    }
}

internal fun accountHeroPaint(accountColor: Color, isDark: Boolean): AccountHeroPaint {
    if (!isDark) {
        val wash = Color.White.copy(alpha = 0.24f).compositeOver(accountColor)
        val on = if (accountColor.luminance() > 0.62f) Color(0xFF212529) else Color.White
        return AccountHeroPaint(base = accountColor, wash = wash, on = on)
    }
    val slate = Color(0xFF243044)
    val base = accountColor.copy(alpha = 0.34f).compositeOver(slate)
    val wash = Color.White.copy(alpha = 0.14f).compositeOver(base)
    val on = if (base.luminance() > 0.62f) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    return AccountHeroPaint(base = base, wash = wash, on = on)
}

/** 玻璃顶缘的一条反光，不占布局。 */
internal fun DrawScope.drawGlassSheen(color: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0f)),
            startY = 0f,
            endY = size.height * 0.42f,
        ),
    )
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
