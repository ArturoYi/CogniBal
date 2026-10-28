package com.example.cognibal.screen.feature.legal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/**
 * 第一次打开，或协议版本更新后，先看完再进入账本。
 *
 * 不同意就退出。三份全文在这一页里打开，返回后仍停在确认页。
 */
@Composable
internal fun LegalConsentScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var readingName by rememberSaveable { mutableStateOf("") }
    val document = readingName
        .takeIf { name -> name.isNotEmpty() }
        ?.let { name -> LegalDocumentKind.entries.firstOrNull { kind -> kind.name == name } }
    BackHandler(enabled = document != null) { readingName = "" }
    BackHandler(enabled = document == null, onBack = onDecline)
    PageBackgroundHost(modifier = modifier) {
        if (document != null) {
            LegalDocumentRoute(
                kind = document,
                onBack = { readingName = "" },
            )
        } else {
            LegalConsentContent(
                onOpenDocument = { kind -> readingName = kind.name },
                onAgree = onAgree,
                onDecline = onDecline,
            )
        }
    }
}

@Composable
private fun LegalConsentContent(
    onOpenDocument: (LegalDocumentKind) -> Unit,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    RequestPageBackground()
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.legal_consent_title),
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.legal_consent_body),
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LegalDocumentKind.entries.forEach { kind ->
                TextButton(
                    onClick = { onOpenDocument(kind) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(kind.titleRes()),
                        color = colors.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onAgree,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.legal_agree),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            TextButton(
                onClick = onDecline,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.legal_decline),
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@FormFactorPreviews
@Composable
private fun LegalConsentPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        LegalConsentScreen(onAgree = {}, onDecline = {})
    }
}
