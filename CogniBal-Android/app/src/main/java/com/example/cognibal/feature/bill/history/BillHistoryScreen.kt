package com.example.cognibal.screen.feature.bill.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.screen.feature.bill.BillEntryRow
import com.example.cognibal.screen.feature.bill.billPageListPadding

@Composable
internal fun BillHistoryRoute(
    accountId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillHistoryViewModel = viewModel(factory = BillHistoryViewModel.factory(accountId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BillHistoryScreen(
        bills = uiState.bills,
        currencySymbol = uiState.currencySymbol,
        loading = uiState.loading,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * 当前账户的全部账单。顶栏贴在状态栏下面，列表用 contentPadding 躲开系统栏。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun BillHistoryScreen(
    bills: List<BillRowUi>,
    currencySymbol: String,
    loading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RequestPageBackground()
    val showBack = LocalListDetailSceneScope.current == null
    val contentPadding = billPageListPadding(extraBottom = 16.dp)
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    screenContentWindowInsets().only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(start = 4.dp, end = 16.dp, top = 4.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = MaterialTheme.appColors.textPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .interactiveClick(onClick = onBack)
                        .padding(12.dp),
                )
            }
            Text(
                text = stringResource(R.string.bill_history_title),
                color = MaterialTheme.appColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = if (showBack) 0.dp else 12.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .consumeWindowInsets(contentPadding),
                contentPadding = contentPadding,
            ) {
                if (!loading && bills.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.bill_recent_empty),
                            color = MaterialTheme.appColors.textTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(items = bills, key = { bill -> bill.id }) { bill ->
                        BillEntryRow(
                            bill = bill,
                            currencySymbol = currencySymbol,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
