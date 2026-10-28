package com.example.cognibal.screen.feature.legal

import android.content.Context
import androidx.annotation.StringRes
import com.example.cognibal.R
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * 与协议正文的生效日期一致。
 *
 * 改用户协议、隐私协议或软件许可后，把这里一起改。已经同意过旧版本的人会再看到确认页。
 */
internal const val LEGAL_ACCEPTANCE_VERSION: String = "2026-10-28"

/** 协议是否已经读到磁盘。未同意当前版本时先停在确认页。 */
internal enum class LegalGate {
    Pending,
    Required,
    Accepted,
}

/** 设置里可以打开的三份文本。软件许可不按语言拆文件，中英写在同一份里。 */
@Serializable
internal enum class LegalDocumentKind {
    Terms,
    Privacy,
    License,
}

@StringRes
internal fun LegalDocumentKind.titleRes(): Int = when (this) {
    LegalDocumentKind.Terms -> R.string.security_terms
    LegalDocumentKind.Privacy -> R.string.security_privacy
    LegalDocumentKind.License -> R.string.open_source
}

internal sealed interface LegalBlock {
    data class Heading(val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
}

/** 以 `# ` 开头的行是标题，空行分开段落。单独一行横线只作分隔，不显示。 */
internal fun parseLegalDocument(source: String): List<LegalBlock> {
    val blocks = mutableListOf<LegalBlock>()
    val paragraph = StringBuilder()
    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks += LegalBlock.Paragraph(text)
        paragraph.clear()
    }
    source.lineSequence().forEach { raw ->
        val line = raw.trim()
        val heading = when {
            line.startsWith("## ") -> line.removePrefix("## ")
            line.startsWith("# ") -> line.removePrefix("# ")
            else -> null
        }
        when {
            heading != null -> {
                flushParagraph()
                if (heading.isNotEmpty()) blocks += LegalBlock.Heading(heading)
            }
            line.isEmpty() || line.all { it == '-' } -> flushParagraph()
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(line)
            }
        }
    }
    flushParagraph()
    return blocks
}

internal fun legalLanguageFolder(locale: Locale): String {
    if (!locale.language.equals("zh", ignoreCase = true)) return "en"
    val country = locale.country.uppercase(Locale.ROOT)
    if (country == "HK" || country == "MO") return "zh-HK"
    val script = locale.script
    if (script.equals("Hant", ignoreCase = true) || country == "TW") return "zh-Hant"
    return "zh-Hans"
}

internal fun LegalDocumentKind.assetPath(locale: Locale): String = when (this) {
    LegalDocumentKind.License -> "legal/license.txt"
    LegalDocumentKind.Terms -> "legal/${legalLanguageFolder(locale)}/terms.txt"
    LegalDocumentKind.Privacy -> "legal/${legalLanguageFolder(locale)}/privacy.txt"
}

internal fun Context.readLegalDocument(kind: LegalDocumentKind, locale: Locale): String {
    return assets.open(kind.assetPath(locale)).bufferedReader().use { reader -> reader.readText() }
}
