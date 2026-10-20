package com.example.cognibal.screen.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors

/**
 * 设置二级页的共用骨架：顶栏返回，下面是可滚动内容。
 *
 * 底色由共用页面底铺到屏幕边缘，这里不再另铺一层。安全区垫在内容上，返回键和条目不会被状态栏、导航栏挡住。
 * 列表和详情并排时不显示返回键，系统返回仍由导航宿主处理。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsSubpage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showBack = LocalListDetailSceneScope.current == null
    val contentPadding = listContentPadding()
    val layoutDirection = LocalLayoutDirection.current
    val scrollState = rememberScrollState()
    RequestPageBackground()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val layout = settingsLayoutSpec(maxWidth, maxHeight)
        val maxContentWidth = if (layout.isWide) 640.dp else layout.maxContentWidth
        val sectionSpacing = if (layout.compactHeight) 12.dp else 16.dp
        Column(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxSize()
                .padding(
                    start = contentPadding.calculateLeftPadding(layoutDirection),
                    end = contentPadding.calculateRightPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                )
                .consumeWindowInsets(contentPadding)
                .padding(
                    start = layout.horizontalPadding,
                    end = layout.horizontalPadding,
                    top = if (layout.compactHeight) 10.dp else 14.dp,
                ),
        ) {
            SettingsSubpageHeader(title = title, onBack = onBack, showBack = showBack)
            Spacer(modifier = Modifier.height(if (layout.compactHeight) 8.dp else 12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = contentPadding.calculateBottomPadding())
                    .padding(bottom = if (layout.compactHeight) 10.dp else 14.dp),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = content,
            )
        }
    }
}

/**
 * 宽屏还没选中二级页时，详情窗格里的空状态。安全区和列表页同一套。
 */
@Composable
internal fun SettingsDetailPlaceholder(modifier: Modifier = Modifier) {
    RequestPageBackground()
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.settings_choose),
            color = colors.textTertiary,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun SettingsSubpageHeader(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .interactiveClick(onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal data class SettingsAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
internal fun SettingsActionGroup(
    actions: List<SettingsAction>,
    onAction: (Int) -> Unit,
) {
    SettingsSectionCard {
        actions.forEachIndexed { index, action ->
            if (index > 0) SettingsDivider()
            SettingActionItem(
                icon = action.icon,
                title = action.title,
                subtitle = action.subtitle,
                onClick = { onAction(index) },
            )
        }
    }
}
