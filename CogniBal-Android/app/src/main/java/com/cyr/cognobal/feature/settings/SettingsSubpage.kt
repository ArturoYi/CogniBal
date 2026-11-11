package com.cyr.cognobal.screen.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyr.cognobal.R
import com.cyr.cognobal.ui.background.RequestPageBackground
import com.cyr.cognobal.ui.insets.listContentPadding
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.theme.appColors

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsSubpage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackOverride: Boolean? = null,
    onTitleClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    scrollContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showBack = showBackOverride ?: (LocalListDetailSceneScope.current == null)
    val contentPadding = listContentPadding()
    val layoutDirection = LocalLayoutDirection.current
    val scrollState = rememberScrollState()
    RequestPageBackground()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val layout = settingsLayoutSpec(maxWidth, maxHeight)
        val maxContentWidth = if (layout.isWide) 680.dp else layout.maxContentWidth
        val sectionSpacing = 18.dp
        val bottomExtra = 24.dp
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
                    top = if (layout.compactHeight) 8.dp else 12.dp,
                ),
        ) {
            SettingsSubpageHeader(
                title = title,
                onBack = onBack,
                showBack = showBack,
                onTitleClick = onTitleClick,
                trailing = trailing,
            )
            Spacer(modifier = Modifier.height(if (layout.compactHeight) 8.dp else 14.dp))
            if (placeholder != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = contentPadding.calculateBottomPadding()),
                    contentAlignment = Alignment.Center,
                ) {
                    placeholder()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (scrollContent) {
                                Modifier.verticalScroll(scrollState)
                            } else {
                                Modifier
                            },
                        )
                        .padding(bottom = contentPadding.calculateBottomPadding())
                        .padding(bottom = bottomExtra),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                    content = content,
                )
            }
        }
    }
}

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
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SettingsSubpageHeader(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean,
    onTitleClick: (() -> Unit)?,
    trailing: (@Composable RowScope.() -> Unit)?,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .interactiveClick(onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.cardBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        val titleModifier = if (onTitleClick != null) {
            Modifier
                .weight(1f)
                .interactiveClick(onClick = onTitleClick)
        } else {
            Modifier.weight(1f)
        }
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = if (showBack) 20.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = titleModifier,
        )
        if (trailing != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
    }
}

internal data class SettingsAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
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
