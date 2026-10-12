package com.example.cognibal.screen.expense.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cognibal.screen.expense.config.AddExpenseConfig
import com.example.cognibal.ui.insets.screenSafeDrawing
import com.example.cognibal.ui.interaction.interactiveClick

/**
 * 添加开支。从 Index 中间的加号进入，盖住导航条；返回后回到原来的标签。
 */
@Composable
internal fun AddExpenseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .screenSafeDrawing(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(AddExpenseConfig.backRes),
                modifier = Modifier.interactiveClick(onClick = onBack),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(AddExpenseConfig.titleRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
