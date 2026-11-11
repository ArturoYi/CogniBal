package com.cyr.cognobal.screen.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cyr.cognobal.R
import com.cyr.cognobal.data.repository.LedgerLimits
import com.cyr.cognobal.screen.feature.expense.model.AddBillIssue
import com.cyr.cognobal.screen.feature.expense.model.categoryIcon
import com.cyr.cognobal.screen.feature.expense.model.categoryIconLabel
import com.cyr.cognobal.screen.feature.expense.model.messageRes
import com.cyr.cognobal.screen.feature.expense.model.newCategoryIconKeys
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors
import kotlinx.coroutines.launch

/**
 * 新建分类底部弹层：左侧图标实时预览 + 右侧无框输入框 + 下方图标分页矩阵 + 底部并排操作按钮。
 * 去除「写下名称，再选图标」与「图标」次级小标题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateCategorySheet(
    error: AddBillIssue?,
    onConfirm: (name: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf(newCategoryIconKeys.first()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
        contentWindowInsets = {
            WindowInsets.navigationBars.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom)
        },
    ) {
        CreateCategorySheetBody(
            name = name,
            onName = { name = it.take(LedgerLimits.NAME) },
            iconKey = iconKey,
            onIcon = { iconKey = it },
            error = error,
            onConfirm = { onConfirm(name.trim(), iconKey) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun CreateCategorySheetBody(
    name: String,
    onName: (String) -> Unit,
    iconKey: String,
    onIcon: (String) -> Unit,
    error: AddBillIssue?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val nameLabel = stringResource(R.string.bill_entry_category_name)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_entry_create_category),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(colors.primary, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIcon(iconKey),
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = onName,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .bringIntoViewRequester(bringIntoView)
                            .onFocusChanged { state ->
                                focused = state.isFocused
                                if (state.isFocused) {
                                    scope.launch { bringIntoView.bringIntoView() }
                                }
                            }
                            .then(
                                if (focused) {
                                    Modifier.border(
                                        1.dp,
                                        colors.primary.copy(alpha = 0.35f),
                                        MaterialTheme.shapes.medium,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .background(colors.background, MaterialTheme.shapes.medium)
                            .semantics { contentDescription = nameLabel }
                            .padding(horizontal = 16.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (name.isEmpty()) {
                                    Text(
                                        text = nameLabel,
                                        color = colors.textTertiary,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                if (error != null) {
                    Text(
                        text = stringResource(error.messageRes()),
                        color = colors.expense,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconSwiper(selectedKey = iconKey, onSelect = onIcon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.background,
                        contentColor = colors.textPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    )
                }
                Button(
                    onClick = onConfirm,
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.inkStrong,
                        contentColor = colors.onInkStrong,
                        disabledContainerColor = colors.inkStrong.copy(alpha = 0.4f),
                        disabledContentColor = colors.onInkStrong.copy(alpha = 0.7f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconSwiper(
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 8.dp
        val columns = ((maxWidth + gap) / (48.dp + gap)).toInt().coerceIn(4, 5)
        val rows = 3
        val pages = newCategoryIconKeys.chunked(columns * rows)
        val pagerState = rememberPagerState(pageCount = { pages.size })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp * rows + gap * (rows - 1)),
            ) { page ->
                IconPage(
                    keys = pages[page],
                    columns = columns,
                    selectedKey = selectedKey,
                    onSelect = onSelect,
                )
            }
            if (pages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    pages.indices.forEach { index ->
                        val selected = pagerState.currentPage == index
                        val label = stringResource(
                            R.string.bill_entry_icon_page,
                            index + 1,
                            pages.size,
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .semantics(mergeDescendants = true) {
                                    this.selected = selected
                                    contentDescription = label
                                }
                                .interactiveClick { scope.launch { pagerState.animateScrollToPage(index) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (selected) 16.dp else 8.dp)
                                    .background(
                                        if (selected) colors.primary else colors.divider,
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPage(
    keys: List<String>,
    columns: Int,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val gap = 8.dp
    val rows = (keys.size + columns - 1) / columns
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                for (column in 0 until columns) {
                    val index = row * columns + column
                    if (index < keys.size) {
                        val key = keys[index]
                        IconChoice(
                            iconKey = key,
                            selected = key == selectedKey,
                            onSelect = { onSelect(key) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun IconChoice(
    iconKey: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(categoryIconLabel(iconKey))
    Box(
        modifier = modifier
            .height(48.dp)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = label
            }
            .interactiveClick(role = Role.RadioButton, onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (selected) colors.inkStrong else colors.background,
                    MaterialTheme.shapes.medium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(iconKey),
                contentDescription = null,
                tint = if (selected) colors.onInkStrong else colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun CreateCategorySheetPreview() {
    CogniBalTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.appColors.cardBackground,
        ) {
            CreateCategorySheetBody(
                name = "",
                onName = {},
                iconKey = newCategoryIconKeys.first(),
                onIcon = {},
                error = null,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
