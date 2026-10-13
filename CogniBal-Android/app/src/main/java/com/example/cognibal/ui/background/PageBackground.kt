package com.example.cognibal.ui.background

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

/**
 * 一整页的底色。
 *
 * [color] 为 [Color.Unspecified] 时用 [MaterialTheme.colorScheme.background]。
 * 内容字色和状态栏图标仍按浅色底处理，自定义颜色保持浅色，深色字才读得清。
 */
@Immutable
data class PageBackgroundStyle(
    val color: Color = Color.Unspecified,
)

/**
 * 共用页面底。导航和内容都画在这层上面，底栏凸起旁边的缝才跟页面是同一块颜色。
 *
 * 各页面不要自己再铺一层，用 [RequestPageBackground] 声明即可。
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

/**
 * 声明当前页面的底色。真正绘制在 [PageBackground]，页面离开组合后恢复主题纯色。
 *
 * ```
 * RequestPageBackground(PageBackgroundStyle(Color(0xFFEAF2FF)))
 * ```
 *
 * 没有共用骨架时（例如单独预览）这条声明不会生效，把样式直接传给 [PageBackground]。
 */
@Composable
fun RequestPageBackground(style: PageBackgroundStyle) {
    val controller = LocalPageBackgroundController.current
    val owner = remember { Any() }
    DisposableEffect(controller) {
        onDispose { controller?.release(owner) }
    }
    SideEffect { controller?.claim(owner, style) }
}

/** 当前页面底的纯色。状态栏遮罩用它，才不会在自定义底色下露出一条主题灰。 */
@Composable
fun currentPageBackgroundColor(): Color {
    val style = LocalPageBackgroundController.current?.style ?: PageBackgroundStyle()
    return style.baseColor()
}

@Composable
private fun PageBackgroundStyle.baseColor(): Color {
    return if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.background
    } else {
        color
    }
}

/**
 * 页面声明的底。绘制层在声明者的外层，所以用持有者区分先后：
 * 后声明的先画上时，先离开的页面不能把新底清掉。
 */
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
