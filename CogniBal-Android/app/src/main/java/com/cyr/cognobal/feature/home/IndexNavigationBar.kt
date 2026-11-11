package com.cyr.cognobal.screen.feature.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyr.cognobal.screen.feature.expense.config.AddExpenseConfig
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors

private val BarHeight = 64.dp
private val TabIconSize = 22.dp
private val RailWidth = 84.dp

@Composable
internal fun bottomNavigationOverlayInsets(): WindowInsets {
    val density = LocalDensity.current
    val systemBottom = bottomBarInsets().getBottom(density)
    val chrome = with(density) { BarHeight.roundToPx() }
    return WindowInsets(left = 0, top = 0, right = 0, bottom = systemBottom + chrome)
}

/**
 * 对称五格底栏 / 侧栏导航：
 * 「首页 · 统计 · (+) · 资产 · 设置」
 * 纯白净底 + 1dp 柔和顶部分割线 + 中央曜石黑圆钮，复刻参考设计的呼吸感。
 */
@Composable
internal fun IndexNavigationBar(
    placement: IndexNavigationPlacement,
    selected: IndexTab,
    onSelect: (IndexTab) -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val barColor = colors.navBarBackground
    val barModifier = if (interactive) modifier else modifier.clearAndSetSemantics {}
    val inline = placement == IndexNavigationPlacement.BottomInline

    when (placement) {
        IndexNavigationPlacement.BottomStacked,
        IndexNavigationPlacement.BottomInline,
        ->
            Column(
                modifier = barModifier
                    .fillMaxWidth()
                    .background(barColor)
                    .windowInsetsPadding(bottomBarInsets()),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarHeight - 1.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IndexTabButton(
                        tab = IndexTab.Bill,
                        selected = selected == IndexTab.Bill,
                        inline = inline,
                        enabled = interactive,
                        onClick = { onSelect(IndexTab.Bill) },
                        modifier = Modifier.weight(1f),
                    )
                    IndexTabButton(
                        tab = IndexTab.Statistics,
                        selected = selected == IndexTab.Statistics,
                        inline = inline,
                        enabled = interactive,
                        onClick = { onSelect(IndexTab.Statistics) },
                        modifier = Modifier.weight(1f),
                    )
                    CenterAddButton(
                        onClick = onAddExpense,
                        enabled = interactive,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    IndexTabButton(
                        tab = IndexTab.Assets,
                        selected = selected == IndexTab.Assets,
                        inline = inline,
                        enabled = interactive,
                        onClick = { onSelect(IndexTab.Assets) },
                        modifier = Modifier.weight(1f),
                    )
                    IndexTabButton(
                        tab = IndexTab.Settings,
                        selected = selected == IndexTab.Settings,
                        inline = inline,
                        enabled = interactive,
                        onClick = { onSelect(IndexTab.Settings) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        IndexNavigationPlacement.Side ->
            Row(barModifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .width(RailWidth)
                        .fillMaxHeight()
                        .background(barColor)
                        .windowInsetsPadding(sideRailInsets())
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CenterAddButton(onClick = onAddExpense, enabled = interactive)
                    Spacer(Modifier.height(4.dp))
                    IndexTab.entries.forEach { tab ->
                        IndexTabButton(
                            tab = tab,
                            selected = selected == tab,
                            inline = false,
                            enabled = interactive,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.divider),
                )
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
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(tab.labelRes)
    val tint by animateColorAsState(
        targetValue = if (selected) {
            colors.inkStrong
        } else {
            colors.textTertiary
        },
        animationSpec = tween(durationMillis = 180),
        label = "indexTabColor",
    )
    val hitModifier = modifier
        .height(52.dp)
        .semantics(mergeDescendants = true) {
            role = Role.Tab
            this.selected = selected
        }
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            role = Role.Tab,
            onClick = onClick,
        )

    if (inline) {
        Row(
            modifier = hitModifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.icon,
                contentDescription = null,
                modifier = Modifier.size(TabIconSize),
                tint = tint,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Column(
            modifier = hitModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.icon,
                contentDescription = null,
                modifier = Modifier.size(TabIconSize),
                tint = tint,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CenterAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val description = stringResource(AddExpenseConfig.titleRes)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "addButtonScale",
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.inkStrong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AddExpenseConfig.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = colors.onInkStrong,
            )
        }
    }
}

@Composable
private fun bottomBarInsets(): WindowInsets =
    NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

@Composable
private fun sideRailInsets(): WindowInsets =
    NavigationRailDefaults.windowInsets.only(
        WindowInsetsSides.Start + WindowInsetsSides.Vertical,
    )

@Preview(name = "底栏预览 - 亮色模式", showBackground = true, backgroundColor = 0xFFF6F7F9)
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

@Preview(name = "底栏预览 - 暗色模式", showBackground = true, backgroundColor = 0xFF101216)
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
