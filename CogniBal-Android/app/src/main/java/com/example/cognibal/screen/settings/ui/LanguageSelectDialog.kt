package com.example.cognibal.screen.settings.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.locale.AppLanguage
import com.example.cognibal.ui.theme.appColors

/**
 * 语言列表。名字是各语言自己的写法，不随当前界面语言翻译。
 */
@Composable
internal fun LanguageSelectDialog(
    selectedTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.language),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                LanguageOptionRow(
                    label = stringResource(R.string.language_follow_system),
                    selected = selectedTag == null,
                    onClick = { onSelect(null) },
                )
                AppLanguage.entries.forEach { language ->
                    LanguageOptionRow(
                        label = stringResource(language.nameRes),
                        selected = language.matches(selectedTag),
                        onClick = { onSelect(language.languageTag) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.primary,
                unselectedColor = colors.textTertiary,
            ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * 当前应用内语言。空表示跟随系统。
 *
 * 预览没有应用进程，读 [AppCompatDelegate] 会落到空列表。
 * 语言切换会重建 Activity，重组后这里读到的就是新语言。
 */
@Composable
internal fun currentApplicationLanguageTag(): String? {
    if (LocalInspectionMode.current) return null
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return null
    return locales[0]?.toLanguageTag()
}
