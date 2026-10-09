package com.example.cognibal.ui.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme

/**
 * 一整页的底。
 *
 * [color] 为 [Color.Unspecified] 时用 [MaterialTheme.colorScheme.background]。
 * 内容字色和状态栏图标仍按浅色底处理，自定义颜色保持浅色，深色字才读得清。
 */
@Immutable
sealed interface PageBackgroundStyle {
    val color: Color

    /** 纯色。这是各页面的默认。 */
    @Immutable
    data class Solid(
        override val color: Color = Color.Unspecified,
    ) : PageBackgroundStyle

    /**
     * 纯色上再铺静态光斑。
     *
     * [blooms] 为空时用 [brandPageBlooms]。光斑是径向渐变，不是整屏模糊：模糊要么只在
     * 较新系统上可用，要么会走软件渲染，而且容易把前景一起糊掉。这里也不做持续动画。
     */
    @Immutable
    data class Diffusion(
        override val color: Color = Color.Unspecified,
        val blooms: List<PageBloom> = emptyList(),
    ) : PageBackgroundStyle
}

/**
 * 一团弥散光。
 *
 * [center] 是相对页面宽高的位置，左上角为 (0, 0)，可以小于 0 或大于 1，让颜色从屏幕外渗进来。
 * 从右到左布局时水平位置会镜像。[radius] 是相对较长边的比例，手机和平板用同一组数字。
 * [color] 的透明度是中心最浓处，边缘收到 0。
 */
@Immutable
data class PageBloom(
    val color: Color,
    val center: Offset,
    val radius: Float,
)

/**
 * 主题色的一组静态光斑，给 [PageBackgroundStyle.Diffusion] 在未自定义时使用。
 * 透明度压低，是为了让浅底上的深色字仍然清楚。
 */
@Composable
fun brandPageBlooms(): List<PageBloom> {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    return listOf(
        PageBloom(primary.copy(alpha = 0.30f), Offset(-0.1f, -0.06f), 0.75f),
        PageBloom(secondary.copy(alpha = 0.26f), Offset(1.06f, 0.08f), 0.68f),
        PageBloom(secondary.copy(alpha = 0.16f), Offset(0.2f, 0.78f), 0.55f),
    )
}

/**
 * 共用页面底。导航和内容都画在这层上面，底栏凸起旁边的缝才跟页面是同一块颜色。
 *
 * 各页面不要自己再铺一层，用 [RequestPageBackground] 声明即可。
 */
@Composable
fun PageBackground(
    modifier: Modifier = Modifier,
    style: PageBackgroundStyle = PageBackgroundStyle.Solid(),
    content: @Composable () -> Unit,
) {
    val blooms = style.resolvedBlooms()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = style.baseColor(),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        if (blooms.isEmpty()) {
            content()
        } else {
            // 把最小约束继续传给导航骨架，避免光斑层把整页量成内容包裹。
            Box(
                modifier = Modifier.fillMaxSize(),
                propagateMinConstraints = true,
            ) {
                Spacer(Modifier.matchParentSize().pageBlooms(blooms))
                content()
            }
        }
    }
}

/**
 * 声明当前页面的底。真正绘制在 [PageBackground]，页面离开组合后恢复主题纯色。
 *
 * ```
 * RequestPageBackground(PageBackgroundStyle.Solid(Color(0xFFEAF2FF)))
 * RequestPageBackground(PageBackgroundStyle.Diffusion())
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

/** 当前页面底的纯色部分。状态栏遮罩用它，才不会在自定义底色下露出一条主题灰。 */
@Composable
fun currentPageBackgroundColor(): Color {
    val style = LocalPageBackgroundController.current?.style ?: PageBackgroundStyle.Solid()
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

@Composable
private fun PageBackgroundStyle.resolvedBlooms(): List<PageBloom> = when (this) {
    is PageBackgroundStyle.Solid -> emptyList()
    is PageBackgroundStyle.Diffusion -> blooms.ifEmpty { brandPageBlooms() }
}

/**
 * 页面声明的底。绘制层在声明者的外层，所以用持有者区分先后：
 * 后声明的先画上时，先离开的页面不能把新底清掉。
 */
@Stable
internal class PageBackgroundController {
    var style by mutableStateOf<PageBackgroundStyle>(PageBackgroundStyle.Solid())
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
        this.style = PageBackgroundStyle.Solid()
    }
}

internal val LocalPageBackgroundController = staticCompositionLocalOf<PageBackgroundController?> { null }

@Composable
private fun Modifier.pageBlooms(blooms: List<PageBloom>): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    return drawWithCache {
        val placed = placeBlooms(blooms, size, layoutDirection)
        onDrawBehind {
            placed.forEach { bloom ->
                drawCircle(
                    brush = bloom.brush,
                    radius = bloom.radius,
                    center = bloom.center,
                )
            }
        }
    }
}

private class PlacedBloom(
    val center: Offset,
    val radius: Float,
    val brush: Brush,
)

private fun placeBlooms(
    blooms: List<PageBloom>,
    size: Size,
    layoutDirection: LayoutDirection,
): List<PlacedBloom> {
    if (size.minDimension <= 0f) return emptyList()
    val longSide = size.maxDimension
    return blooms.mapNotNull { bloom ->
        if (bloom.radius <= 0f || !bloom.radius.isFinite()) return@mapNotNull null
        val fractionX = if (layoutDirection == LayoutDirection.Rtl) {
            1f - bloom.center.x
        } else {
            bloom.center.x
        }
        val center = Offset(fractionX * size.width, bloom.center.y * size.height)
        val radius = bloom.radius * longSide
        if (radius <= 0f) return@mapNotNull null
        val peak = bloom.color
        PlacedBloom(
            center = center,
            radius = radius,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to peak,
                    0.45f to peak.copy(alpha = peak.alpha * 0.35f),
                    1f to peak.copy(alpha = 0f),
                ),
                center = center,
                radius = radius,
            ),
        )
    }
}

@FormFactorPreviews
@Composable
private fun DiffusionPageBackgroundPreview() {
    CogniBalTheme {
        PageBackground(style = PageBackgroundStyle.Diffusion()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "弥散背景",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
