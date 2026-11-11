package com.cyr.cognobal.screen.feature.bill

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.cyr.cognobal.ui.theme.appColors

internal data class AccountHeroPaint(
    val base: Color,
    val wash: Color,
    val on: Color,
    val identity: Color,
)

/**
 * 曜石深灰圆角主卡面：细腻哑光深色底搭配温润香槟金点缀色 [AccountHeroPaint.identity]。
 */
@Composable
internal fun AccountHeroSurface(
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(AccountHeroPaint) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val spec = tween<Color>(durationMillis = 220)
    val baseTarget = if (color != Color.Unspecified) color else colors.hero
    val base by animateColorAsState(baseTarget, spec, label = "hero-base")
    val on by animateColorAsState(colors.onHero, spec, label = "hero-on")
    val accent by animateColorAsState(colors.heroAccent, spec, label = "hero-accent")
    val wash = lerp(base, Color.White, 0.045f)
    val paint = AccountHeroPaint(base = base, wash = wash, on = on, identity = accent)
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(wash, base))),
    ) {
        content(paint)
    }
}
