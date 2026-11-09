package com.cyr.cognobal.ui.background

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyr.cognobal.ui.theme.WarmCreamLight_Background
import com.cyr.cognobal.ui.theme.appColors

/**
 * 主题背景不可用时的不透明兜底。
 *
 * 窗口主题是启动页黑底。页面底若是 [Color.Unspecified] 或全透明，会透出这条黑。
 */
private val DefaultPageBackgroundColor = WarmCreamLight_Background

/**
 * 一整页的底色。
 *
 * 不传 [color] 时用当前主题的不透明背景；主题背景本身透明时改用 [DefaultPageBackgroundColor]。
 * 内容字色和状态栏图标仍按浅色底处理，自定义颜色保持浅色，深色字才读得清。
 */
@Immutable
data class PageBackgroundStyle(
    val color: Color = Color.Unspecified,
)

/**
 * 共用页面底。导航和内容都画在这层上面，底栏凸起旁边的缝才跟页面是同一块颜色。
 *
 * 这一层在导航外面，侧滑时不会跟着页面走。每个目的地还要再包一层 [PageSolidBackground]，
 * 滑动中的那一页才是不透明的，不会透出上一页。
 */
@Composable
fun PageBackground(
    modifier: Modifier = Modifier,
    style: PageBackgroundStyle = PageBackgroundStyle(),
    content: @Composable () -> Unit,
) {
    val ink = MaterialTheme.appColors.primary
    val isDark = MaterialTheme.appColors.isDark
    Surface(
        modifier = modifier.fillMaxSize(),
        color = style.baseColor(),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .paperGrain(ink = ink, isDark = isDark),
            )
            content()
        }
    }
}

/**
 * 同色相的轻纸纹：斜向薄洗加上稀疏点，不另引入色相，也不压过正文。
 */
private fun Modifier.paperGrain(ink: Color, isDark: Boolean): Modifier = drawWithCache {
    val wash = ink.copy(alpha = if (isDark) 0.05f else 0.03f)
    val speckle = ink.copy(alpha = if (isDark) 0.045f else 0.028f)
    val step = 16.dp.toPx()
    onDrawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(wash, Color.Transparent, wash.copy(alpha = wash.alpha * 0.55f)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else step / 2f
            while (x < size.width) {
                drawCircle(color = speckle, radius = 0.7f, center = Offset(x, y))
                x += step
            }
            y += step
            row++
        }
    }
}

/**
 * 声明当前页面的底色。真正绘制在 [PageBackground]，页面离开组合后恢复主题纯色。
 *
 * 不传样式时用主题底色。每个页面都要声明一次，离开后才不会留下上一页的颜色。
 *
 * ```
 * RequestPageBackground()
 * RequestPageBackground(PageBackgroundStyle(Color(0xFFEAF2FF)))
 * ```
 *
 * 调用方需要处在 [PageBackgroundHost] 里。主页已经包了一层；单独预览时再包一次。
 */
@Composable
fun RequestPageBackground(style: PageBackgroundStyle = PageBackgroundStyle()) {
    val controller = LocalPageBackgroundController.current
    val owner = remember { Any() }
    DisposableEffect(controller) {
        onDispose { controller?.release(owner) }
    }
    SideEffect { controller?.claim(owner, style) }
}

/**
 * 接上页面底的控制器并画出来。
 *
 * 主页包住导航和内容。单独预览一个页面时也包这一层，[RequestPageBackground] 才会画到屏幕上。
 */
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

/**
 * 跟着单个页面走的不透明底。
 *
 * 颜色用当前主题背景，主题一切换就跟着变。侧滑返回时这一层和页面一起移动，
 * 盖住下面那一页，只在滑开的区域露出它。
 */
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

/** 当前页面底的纯色。状态栏遮罩用它，才不会在自定义底色下露出一条主题灰。 */
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
