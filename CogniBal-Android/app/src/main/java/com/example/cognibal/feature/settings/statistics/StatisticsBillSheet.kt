package com.example.cognibal.screen.feature.settings.statistics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.feature.bill.PressFade
import com.example.cognibal.screen.feature.bill.billTimeText
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.expense.model.categoryIcon
import com.example.cognibal.screen.feature.settings.component.moneyText
import com.example.cognibal.screen.feature.settings.statistics.state.CategoryShareUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsBillUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsKind
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDateTime

/**
 * 点开一个分类后，先列出这个月里的账单，再点某一笔看详情。
 * 系统返回从详情退回列表，列表上的返回才关掉弹层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsBillSheet(
    share: CategoryShareUi,
    bills: List<StatisticsBillUi>,
    currencySymbol: String,
    billId: Long?,
    onOpenBill: (Long) -> Unit,
    onBackToList: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val bill = bills.firstOrNull { item -> item.id == billId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    LaunchedEffect(bill?.id) {
        scrollState.scrollTo(0)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
    ) {
        BackHandler(enabled = bill != null, onBack = onBackToList)
        StatisticsBillSheetBody(
            share = share,
            bills = bills,
            currencySymbol = currencySymbol,
            bill = bill,
            onOpenBill = onOpenBill,
            onBackToList = onBackToList,
            scrollState = scrollState,
        )
    }
}

@Composable
private fun StatisticsBillSheetBody(
    share: CategoryShareUi,
    bills: List<StatisticsBillUi>,
    currencySymbol: String,
    bill: StatisticsBillUi?,
    onOpenBill: (Long) -> Unit,
    onBackToList: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        if (bill == null) {
            CategoryHeader(share = share, count = bills.size, currencySymbol = currencySymbol)
            if (bills.isEmpty()) {
                Text(
                    text = stringResource(R.string.insight_empty),
                    color = MaterialTheme.appColors.textTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                bills.forEach { item ->
                    key(item.id) {
                        BillLine(
                            bill = item,
                            currencySymbol = currencySymbol,
                            onClick = { onOpenBill(item.id) },
                        )
                    }
                }
            }
        } else {
            DetailHeader(onBack = onBackToList)
            BillDetail(
                bill = bill,
                categoryName = share.resolvedName(),
                currencySymbol = currencySymbol,
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    share: CategoryShareUi,
    count: Int,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    val meta = listOf(
        moneyText(currencySymbol, share.amount),
        percentText(share.ratio),
        pluralStringResource(R.plurals.entry_count, count, count),
    ).joinToString(separator = " · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatisticsCategoryMark(
            iconKey = share.iconKey,
            colorKey = share.colorKey,
            tile = 36.dp,
            icon = 18.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = share.resolvedName(),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun DetailHeader(onBack: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .interactiveClick(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(R.string.statistics_bill_detail),
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
    }
}

@Composable
private fun BillLine(
    bill: StatisticsBillUi,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val amountColor = if (bill.kind == StatisticsKind.Income) colors.income else colors.textPrimary
    PressFade(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = billTimeText(bill.time),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = billSubtitle(bill),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = moneyText(currencySymbol, bill.amount),
                color = amountColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(start = 12.dp),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun billSubtitle(bill: StatisticsBillUi): String {
    val account = bill.accountName ?: stringResource(R.string.bill_entry_error_missing_account)
    val remark = bill.remark
    return if (remark.isNullOrBlank()) account else "$account · $remark"
}

@Composable
private fun BillDetail(
    bill: StatisticsBillUi,
    categoryName: String,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    val amountColor = if (bill.kind == StatisticsKind.Income) colors.income else colors.textPrimary
    val typeLabel = stringResource(
        if (bill.kind == StatisticsKind.Income) R.string.bill_fab_income else R.string.bill_fab_expense,
    )
    Text(
        text = moneyText(currencySymbol, bill.amount),
        color = amountColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
        ),
        autoSize = TextAutoSize.StepBased(
            minFontSize = 18.sp,
            maxFontSize = 28.sp,
            stepSize = 1.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
    )
    DetailField(
        label = stringResource(R.string.bill_entry_type),
        value = typeLabel,
    )
    DetailField(
        label = stringResource(R.string.bill_entry_category),
        value = categoryName,
    )
    DetailField(
        label = stringResource(R.string.bill_entry_account),
        value = bill.accountName ?: stringResource(R.string.bill_entry_error_missing_account),
    )
    DetailField(
        label = stringResource(R.string.bill_entry_when),
        value = billTimeText(bill.time),
    )
    val remark = bill.remark
    if (!remark.isNullOrBlank()) {
        DetailField(
            label = stringResource(R.string.account_field_remark),
            value = remark,
        )
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = colors.textTertiary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
internal fun StatisticsCategoryMark(
    iconKey: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    tile: Dp = 32.dp,
    icon: Dp = 16.dp,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(colorKey)
    Box(
        modifier = modifier
            .size(tile)
            .background(
                tint.copy(alpha = if (colors.isDark) 0.22f else 0.16f),
                RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(icon),
        )
    }
}

@FormFactorPreviews
@Composable
private fun StatisticsBillListPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            Surface(color = MaterialTheme.appColors.cardBackground) {
                StatisticsBillSheetBody(
                    share = previewShare(),
                    bills = previewSheetBills(),
                    currencySymbol = "¥",
                    bill = null,
                    onOpenBill = {},
                    onBackToList = {},
                )
            }
        }
    }
}

@FormFactorPreviews
@Composable
private fun StatisticsBillDetailPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            Surface(color = MaterialTheme.appColors.cardBackground) {
                StatisticsBillSheetBody(
                    share = previewShare(),
                    bills = previewSheetBills(),
                    currencySymbol = "¥",
                    bill = previewSheetBills().first(),
                    onOpenBill = {},
                    onBackToList = {},
                )
            }
        }
    }
}

private fun previewShare(): CategoryShareUi =
    CategoryShareUi(1, "Food", false, "restaurant", "#FFF03E3E", 12_800L, 0.3f)

private fun previewSheetBills(): List<StatisticsBillUi> = listOf(
    StatisticsBillUi(
        id = 1,
        categoryId = 1,
        kind = StatisticsKind.Expense,
        amount = 3_200L,
        time = LocalDateTime.of(2026, 10, 21, 12, 30),
        remark = "Lunch",
        accountName = "Cash",
    ),
    StatisticsBillUi(
        id = 2,
        categoryId = 1,
        kind = StatisticsKind.Expense,
        amount = 9_600L,
        time = LocalDateTime.of(2026, 10, 3, 19, 5),
        remark = null,
        accountName = "Card",
    ),
)
