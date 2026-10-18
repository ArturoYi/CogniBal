package com.example.cognibal.screen.expense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.expense.tool.AmountKey
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors

/**
 * 金额键盘。只提供数字、小数点和退格。
 * 背景铺到屏幕底边，按键垫在系统导航栏上面。
 */
@Composable
internal fun AmountKeypad(
    enabled: Boolean,
    onKey: (AmountKey) -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    includeBottomInset: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val bottomInset = screenContentWindowInsets()
        .exclude(WindowInsets.ime)
        .only(WindowInsetsSides.Bottom)
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.cardBackground)
            .then(
                if (includeBottomInset) {
                    Modifier
                        .windowInsetsPadding(bottomInset)
                        .consumeWindowInsets(bottomInset)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Icon(
                imageVector = Icons.Rounded.KeyboardHide,
                contentDescription = stringResource(R.string.bill_entry_hide_keypad),
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(40.dp)
                    .interactiveClick(enabled = enabled, onClick = onHide)
                    .padding(8.dp),
            )
        }
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    AmountKeyButton(
                        key = key,
                        enabled = enabled,
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountKeyButton(
    key: AmountKey,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spoken = key.spokenLabel()
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed && enabled) colors.tagBackground else colors.background)
            .semantics {
                if (spoken != null) contentDescription = spoken
            }
            .interactiveClick(
                enabled = enabled,
                interactionSource = interaction,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            AmountKey.Backspace -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = null,
                tint = colors.textPrimary,
            )
            AmountKey.Decimal -> Text(
                text = ".",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            is AmountKey.Digit -> Text(
                text = key.value.toString(),
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AmountKey.spokenLabel(): String? = when (this) {
    is AmountKey.Digit -> null
    AmountKey.Decimal -> stringResource(R.string.bill_entry_key_decimal)
    AmountKey.Backspace -> stringResource(R.string.bill_entry_key_delete)
}

private val keypadRows: List<List<AmountKey>> = listOf(
    listOf(AmountKey.Digit('1'), AmountKey.Digit('2'), AmountKey.Digit('3')),
    listOf(AmountKey.Digit('4'), AmountKey.Digit('5'), AmountKey.Digit('6')),
    listOf(AmountKey.Digit('7'), AmountKey.Digit('8'), AmountKey.Digit('9')),
    listOf(AmountKey.Decimal, AmountKey.Digit('0'), AmountKey.Backspace),
)
