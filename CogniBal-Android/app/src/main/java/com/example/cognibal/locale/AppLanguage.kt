package com.example.cognibal.locale

import androidx.annotation.StringRes
import com.example.cognibal.R
import java.util.Locale

/**
 * 应用内可选语言。标签是 BCP 47，顺序与产品清单一致。
 *
 * 资源目录不是把标签原样拼成文件夹名：
 * - 普通地区：`values-zh-rCN`、`values-pt-rBR`
 * - 没有两位地区码：`values-b+es+419`、`values-b+fil+PH`
 * - 印尼语目录用旧代码 `values-in-rID`，希伯来语用 `values-iw-rIL`
 *
 * 名字用各语言自己的写法，并且 `translatable=false`，这样界面语言不对时仍能找到母语。
 */
internal enum class AppLanguage(
    val languageTag: String,
    @StringRes val nameRes: Int,
) {
    EN_US("en-US", R.string.language_name_en_us),
    EN_GB("en-GB", R.string.language_name_en_gb),
    ZH_CN("zh-CN", R.string.language_name_zh_cn),
    ZH_TW("zh-TW", R.string.language_name_zh_tw),
    ZH_HK("zh-HK", R.string.language_name_zh_hk),
    ES_ES("es-ES", R.string.language_name_es_es),
    ES_419("es-419", R.string.language_name_es_419),
    PT_BR("pt-BR", R.string.language_name_pt_br),
    PT_PT("pt-PT", R.string.language_name_pt_pt),
    DE_DE("de-DE", R.string.language_name_de_de),
    FR_FR("fr-FR", R.string.language_name_fr_fr),
    FR_CA("fr-CA", R.string.language_name_fr_ca),
    IT_IT("it-IT", R.string.language_name_it_it),
    NL_NL("nl-NL", R.string.language_name_nl_nl),
    SV_SE("sv-SE", R.string.language_name_sv_se),
    DA_DK("da-DK", R.string.language_name_da_dk),
    NB_NO("nb-NO", R.string.language_name_nb_no),
    FI_FI("fi-FI", R.string.language_name_fi_fi),
    PL_PL("pl-PL", R.string.language_name_pl_pl),
    CS_CZ("cs-CZ", R.string.language_name_cs_cz),
    HU_HU("hu-HU", R.string.language_name_hu_hu),
    RO_RO("ro-RO", R.string.language_name_ro_ro),
    RU_RU("ru-RU", R.string.language_name_ru_ru),
    JA_JP("ja-JP", R.string.language_name_ja_jp),
    KO_KR("ko-KR", R.string.language_name_ko_kr),
    ID_ID("id-ID", R.string.language_name_id_id),
    MS_MY("ms-MY", R.string.language_name_ms_my),
    TH_TH("th-TH", R.string.language_name_th_th),
    VI_VN("vi-VN", R.string.language_name_vi_vn),
    FIL_PH("fil-PH", R.string.language_name_fil_ph),
    HI_IN("hi-IN", R.string.language_name_hi_in),
    AR_SA("ar-SA", R.string.language_name_ar_sa),
    TR_TR("tr-TR", R.string.language_name_tr_tr),
    UK_UA("uk-UA", R.string.language_name_uk_ua),
    EL_GR("el-GR", R.string.language_name_el_gr),
    HE_IL("he-IL", R.string.language_name_he_il),
    ;

    fun matches(languageTag: String?): Boolean {
        if (languageTag.isNullOrEmpty()) return false
        val current = Locale.forLanguageTag(languageTag)
        val expected = Locale.forLanguageTag(this.languageTag)
        return languageCode(current) == languageCode(expected) &&
            current.country.equals(expected.country, ignoreCase = true)
    }

    companion object {
        fun fromTag(languageTag: String?): AppLanguage? {
            if (languageTag.isNullOrEmpty()) return null
            return entries.firstOrNull { it.matches(languageTag) }
        }
    }
}

/**
 * Android 运行时会把希伯来语、印尼语收成旧代码 `iw`、`in`。比较前先折回现行代码。
 */
private fun languageCode(locale: Locale): String = when (locale.language.lowercase()) {
    "iw" -> "he"
    "in" -> "id"
    "ji" -> "yi"
    else -> locale.language.lowercase()
}
