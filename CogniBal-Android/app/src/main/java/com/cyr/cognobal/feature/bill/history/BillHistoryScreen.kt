package com.cyr.cognobal.screen.feature.bill.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.bill.DayBillBlock
import com.cyr.cognobal.screen.feature.bill.billPageListPadding
import com.cyr.cognobal.screen.feature.bill.state.BillRowUi
import com.cyr.cognobal.screen.feature.bill.state.groupBillsByMonth
import com.cyr.cognobal.screen.feature.bill.weekDayText
import com.cyr.cognobal.ui.background.RequestPageBackground
import com.cyr.cognobal.ui.format.formatCalendarMonth
import com.cyr.cognobal.ui.insets.screenContentWindowInsets
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.theme.appColors
import java.time.LocalDate
import java.util.Locale

@Composable
internal fun BillHistoryRoute(
    accountId: Long,
    onBack: () -> Unit,
    onOpenBill: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillHistoryViewModel = viewModel(factory = BillHistoryViewModel.factory(accountId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BillHistoryScreen(
        bills = uiState.bills,
        currencySymbol = uiState.currencySymbol,
        loading = uiState.loading,
        today = uiState.today,
        onBack = onBack,
        onOpenBill = onOpenBill,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun BillHistoryScreen(
    bills: List<BillRowUi>,
    currencySymbol: String,
    loading: Boolean,
    today: LocalDate,
    onBack: () -> Unit,
    onOpenBill: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    RequestPageBackground()
    val colors = MaterialTheme.appColors
    val showBack = LocalListDetailSceneScope.current == null
    val contentPadding = billPageListPadding(extraBottom = 24.dp)
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        screenContentWindowInsets().only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .heightIn(min = 48.dp),
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
                                .size(40.dp)
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
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.bill_history_title),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            val months = remember(bills) { groupBillsByMonth(bills) }
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .consumeWindowInsets(contentPadding),
                contentPadding = contentPadding,
            ) {
                if (!loading && months.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.bill_recent_empty),
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    months.forEachIndexed { monthIndex, month ->
                        item(key = "month-${month.month}") {
                            val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
                            MonthHeader(
                                label = formatCalendarMonth(month.month, locale),
                                modifier = Modifier.padding(
                                    top = if (monthIndex == 0) 8.dp else 24.dp,
                                    bottom = 12.dp,
                                ),
                            )
                        }
                        month.days.forEach { day ->
                            item(key = "day-${day.day}") {
                                DayBillBlock(
                                    dayLabel = weekDayText(day.day, today),
                                    rows = day.bills,
                                    currencySymbol = currencySymbol,
                                    onOpenBill = onOpenBill,
                                    modifier = Modifier.padding(bottom = 20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = MaterialTheme.appColors.textPrimary,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
}
