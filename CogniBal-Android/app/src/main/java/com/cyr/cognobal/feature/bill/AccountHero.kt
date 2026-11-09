package com.cyr.cognobal.screen.feature.bill

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.seatIdentityColor

internal data class AccountHeroPaint(
    val base: Color,
    val wash: Color,
    val on: Color,
    /** 锁进当前主色相后的身份色。色点、波浪用它，仍能靠明暗认出是哪张卡。 */
    val identity: Color,
)

/**
 * 结余卡的底。首页和新建账户用同一层，两边才认得出是同一张卡。
 *
 * 账户色负责认出这是哪张卡；主题色负责这张卡长在哪个 app 里。
 * 绘制时把身份色锁进当前主色相，只留明暗差，整页仍是单色。
 */
@Composable
internal fun AccountHeroSurface(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(AccountHeroPaint) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val target = accountHeroPaint(
        accountColor = color,
        themeHero = colors.hero,
        isDark = colors.isDark,
    )
    val spec = tween<Color>(durationMillis = 280)
    val base by animateColorAsState(target.base, spec, label = "hero-base")
    val wash by animateColorAsState(target.wash, spec, label = "hero-wash")
    val on by animateColorAsState(target.on, spec, label = "hero-on")
    val identity by animateColorAsState(target.identity, spec, label = "hero-identity")
    val paint = AccountHeroPaint(base = base, wash = wash, on = on, identity = identity)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(colors = listOf(paint.wash, paint.base))),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawAccountHeroWave(lerpColor(paint.identity, paint.on, 0.28f))
        }
        content(paint)
    }
}

internal fun accountHeroPaint(
    accountColor: Color,
    themeHero: Color,
    isDark: Boolean,
): AccountHeroPaint {
    val seated = seatIdentityColor(accountColor, themeHero)
    val paper = if (isDark) Color(0xFF141414) else Color.White
    val body = if (isDark) seated.copy(alpha = 0.80f).compositeOver(paper) else seated
    val sheen = lerpColor(Color.White, themeHero, if (isDark) 0.18f else 0.10f)
    val wash = sheen.copy(alpha = if (isDark) 0.12f else 0.22f).compositeOver(body)
    val on = if (body.luminance() > 0.62f) Color(0xFF212529) else Color.White
    return AccountHeroPaint(base = body, wash = wash, on = on, identity = seated)
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
    drawPath(path, color.copy(alpha = 0.16f))
}

