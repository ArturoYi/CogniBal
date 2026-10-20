package com.example.cognibal.ui.insets

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.cognibal.ui.background.currentPageBackgroundColor
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold

/**
 * 首页底栏已经在 [HomeTabChrome] 里把底边留出来。这里再计底部 inset 会垫出两份。
 * 二级页不包那一层，底部仍按整窗安全区计算。
 */
private val LocalHomeNavigationClearsBottom = staticCompositionLocalOf { false }

/**
 * 导航骨架还没消费掉的安全绘制区：状态栏、刘海、剩余导航栏和输入法。
 *
 * [androidx.compose.foundation.layout.WindowInsets.asPaddingValues] 看不到祖先已经消费的 inset，
 * 所以列表的 contentPadding 用这个结果，而不是直接用 [WindowInsets.safeDrawing]。
 * 非列表页面用 [screenSafeDrawing]，它会沿修饰符链扣掉已消费的部分。
 */
@Composable
fun screenContentWindowInsets(): WindowInsets {
    val safeDrawing = WindowInsets.safeDrawing
    val consumed = LocalCogniBalScaffold.current?.navigationConsumedInsets
    val remaining = if (consumed == null) safeDrawing else safeDrawing.exclude(consumed)
    return if (LocalHomeNavigationClearsBottom.current) {
        remaining.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        remaining
    }
}

/**
 * 窄屏首页的外框。底栏画在这一层下面，所以底色和内容都收到栏的上方，栏才露得出来。
 *
 * 底边取栏高和系统底边（含输入法）的较大值。二级页不是首页标签，不走这里，铺满后盖住底栏。
 * 内容槽高度始终不变，底栏被盖住或露出来都不会把页面撑高或压矮。
 */
@Composable
internal fun HomeTabChrome(
    isHomeTab: Boolean,
    content: @Composable () -> Unit,
) {
    val overlay = LocalCogniBalScaffold.current?.navigationOverlayInsets
    val density = LocalDensity.current
    if (!isHomeTab || overlay == null || overlay.getBottom(density) == 0) {
        content()
        return
    }
    val bottomClearance = overlay.union(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
    )
    CompositionLocalProvider(LocalHomeNavigationClearsBottom provides true) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(bottomClearance)
                .consumeWindowInsets(bottomClearance),
        ) {
            content()
        }
    }
}

/**
 * 屏幕根节点的安全区。已经包含输入法。
 *
 * 放在 Index 内容槽里。侧栏占用的系统栏 inset 已由内容槽消费，窄屏底栏由首页外框消费。
 * 这里只会补上剩余部分。
 * 输入区若改走 [imeFit]，这条链上就只保留其中一种。
 */
fun Modifier.screenSafeDrawing(): Modifier = safeDrawingPadding()

/**
 * 整页纵向滚动。padding 在 [verticalScroll] 之前，输入法随 safe drawing 一起让出位置。
 */
@Composable
fun Modifier.screenScrollable(
    scrollState: ScrollState = rememberScrollState(),
): Modifier =
    fillMaxSize()
        .screenSafeDrawing()
        .verticalScroll(scrollState)

/**
 * 懒列表的 contentPadding。和 [androidx.compose.foundation.layout.consumeWindowInsets] 成对使用，
 * 让条目能滚到系统栏后面，同时子节点不会再垫一份：
 *
 * ```
 * val contentPadding = listContentPadding()
 * LazyColumn(
 *     modifier = Modifier.consumeWindowInsets(contentPadding),
 *     contentPadding = contentPadding,
 * )
 * ```
 *
 * 列表用这份 contentPadding。输入法已经算在里面，同一列表上再套 [screenSafeDrawing] 会垫出两份。
 */
@Composable
fun listContentPadding(): PaddingValues = screenContentWindowInsets().asPaddingValues()

/**
 * 把内容收进输入法可见区域。调用前需要有界宽高（例如 [fillMaxSize]），并放在 [verticalScroll] 之前。
 *
 * 父级若已经用 [screenSafeDrawing] 或 [listContentPadding] 处理了输入法，就不要再加这一层。
 * 会跟着页面移动重测的侧滑页不要用它，输入法关闭时用 [androidx.compose.foundation.layout.imePadding]。
 */
fun Modifier.imeFit(): Modifier = fitInside(WindowInsetsRulers.Ime.current)

/**
 * 悬浮按钮相对剩余安全区的底边和末端。导航底栏显示时底边 inset 已由骨架消费。
 * 视觉边距由调用方在这条修饰符之后加，例如 `padding(16.dp)`。
 */
@Composable
fun Modifier.floatingActionInsets(): Modifier =
    windowInsetsPadding(
        screenContentWindowInsets().only(WindowInsetsSides.Bottom + WindowInsetsSides.End),
    )

/**
 * 盖住状态栏的渐变，让滚到状态栏后面的内容仍能看清系统图标。
 *
 * 画在屏幕内容之上、导航条之外。高度比状态栏略高，渐变才会收进内容区。
 * 不传 [color] 时跟当前页面底色走，自定义纯色才不会在状态栏下露出一条主题灰。
 */
@Composable
fun StatusBarProtection(
    color: Color = currentPageBackgroundColor(),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val statusBarHeight: Dp = with(density) {
        (WindowInsets.statusBars.getTop(this) * StatusBarScrimOverscan).toDp()
    }
    Spacer(
        modifier
            .fillMaxWidth()
            .height(statusBarHeight)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 1f),
                        color.copy(alpha = 0.8f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

/** 渐变伸进内容区的比例，避免状态栏下沿被硬切一刀。 */
private const val StatusBarScrimOverscan = 1.2f
