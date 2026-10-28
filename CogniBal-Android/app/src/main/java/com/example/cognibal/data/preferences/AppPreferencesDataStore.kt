package com.example.cognibal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * 偏好 DataStore。委托挂在文件顶层，整个进程只有这一份实例。
 *
 * 文件损坏时用空配置替换，Repository 再把它解释成默认的 [com.example.cognibal.screen.feature.settings.model.AppPreferences]。
 * 旧版 SharedPreferences（`cognibal_theme_preferences`）在首次读取时迁入同名键，迁完后由 DataStore 删掉旧文件。
 */
val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_PREFERENCES_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() },
    ),
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = LEGACY_SHARED_PREFS_NAME,
                keysToMigrate = Keys.legacyKeyNames,
            ),
        )
    },
)

/** 全部偏好键。业务代码只引用这些常量，不再写死字符串。 */
internal object Keys {
    val themeStyle = stringPreferencesKey("key_theme_style")
    val darkMode = stringPreferencesKey("key_dark_mode")
    /** 旧版触感开关，只参与 SharedPreferences 迁移。 */
    val hapticFeedback = booleanPreferencesKey("key_haptic_feedback")
    val currency = stringPreferencesKey("key_currency")
    val budgetNotify = booleanPreferencesKey("key_budget_notify")
    val budgetHomeVisible = booleanPreferencesKey("key_budget_home_visible")
    val acceptedLegalVersion = stringPreferencesKey("key_accepted_legal_version")

    /** 旧版当前账户。启动时迁进 account.is_default 后删除。 */
    val selectedAccountId = longPreferencesKey("key_selected_account_id")

    val legacyKeyNames: Set<String> = setOf(
        themeStyle.name,
        darkMode.name,
        hapticFeedback.name,
        currency.name,
    )
}

private const val APP_PREFERENCES_NAME = "cognibal_preferences"
private const val LEGACY_SHARED_PREFS_NAME = "cognibal_theme_preferences"
