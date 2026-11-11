package com.cyr.cognobal.ui.background

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cyr.cognobal.ui.theme.WarmCreamLight_Background

private val DefaultPageBackgroundColor = WarmCreamLight_Background

@Immutable
data class PageBackgroundStyle(
    val color: Color = Color.Unspecified,
)

/**
 * 纯净整页底色（亮色模式为纯粹大白底 #FFFFFF），去除一切纸纹噪点与渐变遮罩。
 */
@Composable
fun PageBackground(
    modifier: Modifier = Modifier,
    style: PageBackgroundStyle = PageBackgroundStyle(),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = style.baseColor(),
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = content,
    )
}

@Composable
fun RequestPageBackground(style: PageBackgroundStyle = PageBackgroundStyle()) {
    val controller = LocalPageBackgroundController.current
    val owner = remember { Any() }
    DisposableEffect(controller) {
        onDispose { controller?.release(owner) }
    }
    SideEffect { controller?.claim(owner, style) }
}

@Composable
internal fun PageBackgroundHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val controller = remember { PageBackgroundController() }
    CompositionLocalProvider(LocalPageBackgroundController provides controller) {
        PageBackground(
            modifier = modifier,
            style = controller.style,
            content = content,
        )
    }
}

@Composable
internal fun PageSolidBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PageBackground(
        modifier = modifier,
        content = content,
    )
}

@Composable
fun currentPageBackgroundColor(): Color {
    val style = LocalPageBackgroundController.current?.style ?: PageBackgroundStyle()
    return style.baseColor()
}

@Composable
private fun PageBackgroundStyle.baseColor(): Color {
    if (color != Color.Unspecified && color.alpha > 0f) return color
    val themeBackground = MaterialTheme.colorScheme.background
    return when {
        themeBackground == Color.Unspecified || themeBackground.alpha == 0f -> DefaultPageBackgroundColor
        themeBackground.alpha < 1f -> themeBackground.copy(alpha = 1f)
        else -> themeBackground
    }
}

@Stable
internal class PageBackgroundController {
    var style by mutableStateOf(PageBackgroundStyle())
        private set

    private var owner: Any? = null

    fun claim(owner: Any, style: PageBackgroundStyle) {
        if (this.owner == owner && this.style == style) return
        this.owner = owner
        this.style = style
    }

    fun release(owner: Any) {
        if (this.owner != owner) return
        this.owner = null
        this.style = PageBackgroundStyle()
    }
}

internal val LocalPageBackgroundController = staticCompositionLocalOf<PageBackgroundController?> { null }
