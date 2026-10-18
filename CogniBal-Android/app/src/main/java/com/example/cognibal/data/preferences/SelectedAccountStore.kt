package com.example.cognibal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 旧版把当前账户记在偏好里。现在改记在账户表的 is_default。
 * 启动时读一次这个键，写进数据库后删掉，避免和数据库各记各的。
 */
internal class SelectedAccountStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun peekRememberedId(): Long? = try {
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences -> preferences[Keys.selectedAccountId] }
            .first()
    } catch (_: IOException) {
        null
    }

    suspend fun clearRememberedId() {
        try {
            dataStore.edit { preferences -> preferences.remove(Keys.selectedAccountId) }
        } catch (_: IOException) {
            // 没删掉就留到下次启动再迁一次。
        }
    }

    companion object {
        fun create(context: Context): SelectedAccountStore {
            return SelectedAccountStore(context.applicationContext.appPreferencesDataStore)
        }
    }
}
