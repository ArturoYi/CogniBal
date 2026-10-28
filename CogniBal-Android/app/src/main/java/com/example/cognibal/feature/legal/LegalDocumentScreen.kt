package com.example.cognibal.screen.feature.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.util.Locale

/** 用户协议、隐私协议和软件许可的阅读页。正文在 assets/legal。 */
@Composable
internal fun LegalDocumentRoute(
    kind: LegalDocumentKind,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LegalDocumentScreen(
        title = stringResource(kind.titleRes()),
        blocks = rememberLegalBlocks(kind),
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun LegalDocumentScreen(
    title: String,
    blocks: List<LegalBlock>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val body = if (blocks.isEmpty()) {
        listOf(LegalBlock.Paragraph(stringResource(R.string.legal_unavailable)))
    } else {
        blocks
    }
    SettingsSubpage(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) {
        SelectionContainer(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                body.forEach { block ->
                    when (block) {
                        is LegalBlock.Heading -> Text(
                            text = block.text,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 6.dp),
                        )
                        is LegalBlock.Paragraph -> Text(
                            text = block.text,
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberLegalBlocks(kind: LegalDocumentKind): List<LegalBlock> {
    val context = LocalContext.current
    val locale = currentLocale()
    return remember(kind, locale) {
        runCatching {
            parseLegalDocument(context.readLegalDocument(kind, locale))
        }.getOrDefault(emptyList())
    }
}

@Composable
private fun currentLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}

@FormFactorPreviews
@Composable
private fun LegalDocumentPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            LegalDocumentScreen(
                title = "用户协议",
                blocks = listOf(
                    LegalBlock.Paragraph("生效日期：2026年10月28日"),
                    LegalBlock.Heading("1. 这是什么"),
                    LegalBlock.Paragraph("知余把账本保存在这台设备上。"),
                ),
                onBack = {},
            )
        }
    }
}
