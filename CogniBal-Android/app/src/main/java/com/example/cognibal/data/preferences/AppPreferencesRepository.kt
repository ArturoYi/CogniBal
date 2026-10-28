package com.example.cognibal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.example.cognibal.screen.feature.settings.model.AppPreferences
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.screen.feature.settings.model.DarkModePreference
import com.example.cognibal.ui.theme.AppThemeStyle
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 偏好仓库。对外只有冷流和挂起写入，调用方不知道底层是 DataStore。
 *
 * 读取遇到 IO 或损坏时发出空配置，映射层再落成默认值。
 * 写入切到 [Dispatchers.IO]，不在主线程碰磁盘；失败时保留上一次成功的值。
 */
internal class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { it.toAppPreferences() }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        update { it[Keys.themeStyle] = style.name }
    }

    suspend fun setDarkMode(mode: DarkModePreference) {
        update { it[Keys.darkMode] = mode.id }
    }

    suspend fun setCurrency(currency: CurrencyPreference) {
        update { it[Keys.currency] = currency.name }
    }

    /** 换币种用。写入失败时抛出，不把磁盘错误吞掉。 */
    suspend fun setCurrencyOrThrow(currency: CurrencyPreference) {
        withContext(Dispatchers.IO) {
            dataStore.edit { it[Keys.currency] = currency.name }
        }
    }

    suspend fun setBudgetNotify(enabled: Boolean) {
        update { it[Keys.budgetNotify] = enabled }
    }

    suspend fun setBudgetHomeVisible(visible: Boolean) {
        update { it[Keys.budgetHomeVisible] = visible }
    }

    suspend fun setAcceptedLegalVersion(version: String) {
        update { it[Keys.acceptedLegalVersion] = version }
    }

    private suspend fun update(transform: (MutablePreferences) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                dataStore.edit(transform)
            } catch (_: IOException) {
                // 磁盘写入失败时不覆盖内存里的上一次成功值。
            }
        }
    }

    companion object {
        fun create(context: Context): AppPreferencesRepository {
            return AppPreferencesRepository(context.applicationContext.appPreferencesDataStore)
        }
    }
}

private fun Preferences.toAppPreferences(): AppPreferences {
    return AppPreferences(
        themeStyle = enumValueOrDefault(this[Keys.themeStyle], AppThemeStyle.WarmCream),
        darkMode = DarkModePreference.fromId(this[Keys.darkMode]),
        currency = enumValueOrDefault(this[Keys.currency], CurrencyPreference.CNY),
        budgetNotify = this[Keys.budgetNotify] ?: false,
        budgetHomeVisible = this[Keys.budgetHomeVisible] ?: true,
        acceptedLegalVersion = this[Keys.acceptedLegalVersion].orEmpty(),
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T {
    if (raw.isNullOrEmpty()) return fallback
    return enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
