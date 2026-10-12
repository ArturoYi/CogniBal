package com.example.cognibal.screen.index.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.cognibal.screen.expense.config.AddExpenseConfig
import com.example.cognibal.screen.index.config.IndexTab
import com.example.cognibal.screen.index.tool.IndexNavigationPlacement
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/** 底栏自身高度，不含凸起和系统栏。 */
private val BarHeight = 56.dp

/** 中间按钮高出底栏的距离。总高度要算上它，避免凸起被裁掉。 */
private val CenterProtrusion = 16.dp
private val CenterButtonSize = 52.dp
private val TabIconSize = 24.dp
private val RailWidth = 96.dp
private val IconLabelGap = 2.dp
private val InlineIconLabelGap = 8.dp

private val ShadowInk = Color(0xFF111111)

/**
 * Index 导航。窄窗口是白底底栏，中间圆形按钮向上凸起。
 *
 * 选中只改图标和文案的颜色，不铺指示底。阴影用大模糊和低透明度，没有硬边厚度。
 * 底栏背景铺到屏幕边缘，系统栏留白只垫在图标下面。
 */
@Composable
internal fun IndexNavigationBar(
    placement: IndexNavigationPlacement,
    selected: IndexTab,
    onSelect: (IndexTab) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.surfaceContainerLowest
    when (placement) {
        IndexNavigationPlacement.BottomStacked,
        IndexNavigationPlacement.BottomInline,
        ->
            Box(modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .softRectShadow(contentInset = CenterProtrusion),
                ) {
                    Spacer(Modifier.height(CenterProtrusion))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(barColor)
                            .windowInsetsPadding(bottomBarInsets()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(BarHeight),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IndexTabButton(
                                tab = IndexTab.Bill,
                                selected = selected == IndexTab.Bill,
                                inline = placement == IndexNavigationPlacement.BottomInline,
                                onClick = { onSelect(IndexTab.Bill) },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(CenterButtonSize))
                            IndexTabButton(
                                tab = IndexTab.Settings,
                                selected = selected == IndexTab.Settings,
                                inline = placement == IndexNavigationPlacement.BottomInline,
                                onClick = { onSelect(IndexTab.Settings) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                CenterAddButton(
                    onClick = onAddExpense,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        IndexNavigationPlacement.Side ->
            Row(
                modifier
                    .fillMaxHeight()
                    .softRectShadow(contentInset = 0.dp, horizontal = true),
            ) {
                Column(
                    modifier = Modifier
                        .width(RailWidth)
                        .fillMaxHeight()
                        .background(barColor)
                        .windowInsetsPadding(sideRailInsets())
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IndexTabButton(
                        tab = IndexTab.Bill,
                        selected = selected == IndexTab.Bill,
                        inline = false,
                        onClick = { onSelect(IndexTab.Bill) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CenterAddButton(onClick = onAddExpense)
                    IndexTabButton(
                        tab = IndexTab.Settings,
                        selected = selected == IndexTab.Settings,
                        inline = false,
                        onClick = { onSelect(IndexTab.Settings) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
    }
}

@Composable
private fun IndexTabButton(
    tab: IndexTab,
    selected: Boolean,
    inline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(),
        label = "indexTabColor",
    )
    val clickable = modifier
        .semantics(mergeDescendants = true) {
            role = Role.Tab
            this.selected = selected
        }
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        )
        .padding(horizontal = 4.dp, vertical = 3.dp)
    if (inline) {
        Row(
            modifier = clickable,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabIcon(tab, tint)
            Spacer(Modifier.width(InlineIconLabelGap))
            TabLabel(tab, tint)
        }
    } else {
        Column(
            modifier = clickable,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TabIcon(tab, tint)
            Spacer(Modifier.height(IconLabelGap))
            TabLabel(tab, tint)
        }
    }
}

@Composable
private fun TabIcon(tab: IndexTab, tint: Color) {
    Icon(
        imageVector = tab.icon,
        contentDescription = null,
        modifier = Modifier.size(TabIconSize),
        tint = tint,
    )
}

@Composable
private fun TabLabel(tab: IndexTab, tint: Color) {
    Text(
        text = stringResource(tab.labelRes),
        color = tint,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CenterAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(AddExpenseConfig.titleRes)
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val isDark = MaterialTheme.appColors.isDark

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "addButtonScale",
    )

    Box(
        modifier = modifier
            .size(CenterButtonSize)
            .scale(scale)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .pointerHoverIcon(PointerIcon.Hand)
            // 方案三：克制的中性灰自然环境微阴影（无彩色泛光，轻微自然落点层次）
            .neutralCircleShadow(isDark = isDark)
            .clip(CircleShape)
            .background(primary)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = onPrimary.copy(alpha = 0.35f)),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AddExpenseConfig.icon,
            contentDescription = null,
            modifier = Modifier.size(TabIconSize),
            tint = onPrimary,
        )
    }
}

/**
 * 自然中性灰微环境投影：双层软阴影（一层紧贴接触面的弱环境光，一层轻微下移的小落点），
 * 绝无任何彩色荧光或大面积弥散感。
 */
private fun Modifier.neutralCircleShadow(
    isDark: Boolean,
): Modifier = drawWithCache {
    val ambientPaint = shadowPaint(
        if (isDark) Color.Black.copy(alpha = 0.25f) else ShadowInk.copy(alpha = 0.05f),
        2.dp.toPx(),
    )
    val dropPaint = shadowPaint(
        if (isDark) Color.Black.copy(alpha = 0.35f) else ShadowInk.copy(alpha = 0.09f),
        5.dp.toPx(),
    )
    val ambientDy = 1.dp.toPx()
    val dropDy = 2.5.dp.toPx()
    onDrawBehind {
        drawIntoCanvas { canvas ->
            // 环境微阴影
            canvas.drawCircle(
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f + ambientDy),
                radius = size.minDimension / 2f,
                paint = ambientPaint,
            )
            // 实体落点微阴影
            canvas.drawCircle(
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f + dropDy),
                radius = size.minDimension / 2f,
                paint = dropPaint,
            )
        }
    }
}

/**
 * 大模糊、低透明度、略微下移。模糊画在内容一侧的留白里，不靠 elevation 挤出硬边。
 */
private fun Modifier.softRectShadow(
    contentInset: Dp,
    horizontal: Boolean = false,
): Modifier = drawWithCache {
    val far = shadowPaint(ShadowInk.copy(alpha = 0.048f), 14.4.dp.toPx())
    val near = shadowPaint(ShadowInk.copy(alpha = 0.024f), 3.2.dp.toPx())
    val inset = contentInset.toPx()
    val spread = 2.dp.toPx()
    val farShift = 6.4.dp.toPx()
    val nearShift = 1.6.dp.toPx()
    val toEnd = layoutDirection == LayoutDirection.Ltr
    onDrawBehind {
        if (horizontal) {
            val farDx = if (toEnd) farShift else -farShift
            val nearDx = if (toEnd) nearShift else -nearShift
            drawBlurredRect(far, spread, 0f, size.width - spread, size.height, farDx, 0f)
            drawBlurredRect(near, 0f, 0f, size.width, size.height, nearDx, 0f)
        } else {
            drawBlurredRect(far, spread, inset, size.width - spread, size.height, 0f, farShift)
            drawBlurredRect(near, 0f, inset, size.width, size.height, 0f, nearShift)
        }
    }
}

private fun DrawScope.drawBlurredRect(
    paint: Paint,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dx: Float,
    dy: Float,
) {
    drawIntoCanvas { canvas ->
        canvas.drawRect(left + dx, top + dy, right + dx, bottom + dy, paint)
    }
}

private fun shadowPaint(color: Color, blurRadiusPx: Float): Paint {
    return Paint().apply {
        asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            maskFilter = BlurMaskFilter(blurRadiusPx.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
        }
    }
}

/** 底栏图标躲开系统导航栏和两侧切口，背景仍铺到屏幕边缘。 */
@Composable
private fun bottomBarInsets(): WindowInsets =
    NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

/** 侧栏图标躲开起始侧系统栏和上下系统栏。 */
@Composable
private fun sideRailInsets(): WindowInsets =
    NavigationRailDefaults.windowInsets.only(
        WindowInsetsSides.Start + WindowInsetsSides.Vertical,
    )

@Preview(name = "底栏预览 - 亮色模式", showBackground = true, backgroundColor = 0xFFF8F7F2)
@Composable
private fun IndexNavigationBarLightPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        IndexNavigationBar(
            placement = IndexNavigationPlacement.BottomStacked,
            selected = IndexTab.Bill,
            onSelect = {},
            onAddExpense = {},
        )
    }
}

@Preview(name = "底栏预览 - 暗色模式", showBackground = true, backgroundColor = 0xFF191B1A)
@Composable
private fun IndexNavigationBarDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = true) {
        IndexNavigationBar(
            placement = IndexNavigationPlacement.BottomStacked,
            selected = IndexTab.Bill,
            onSelect = {},
            onAddExpense = {},
        )
    }
}

