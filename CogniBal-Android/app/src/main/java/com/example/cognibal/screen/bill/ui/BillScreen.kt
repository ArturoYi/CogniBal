package com.example.cognibal.screen.bill.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cognibal.screen.bill.config.BillTabConfig
import com.example.cognibal.ui.insets.screenSafeDrawing

/**
 * 账单。由 Index 放进共用导航里，自己不再画底栏或侧栏。
 */
@Composable
internal fun BillScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .screenSafeDrawing(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(BillTabConfig.labelRes),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
