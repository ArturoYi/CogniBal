package com.example.cognibal.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.cognibal.settings.model.CurrencyPreference
import com.example.cognibal.settings.model.DarkModePreference
import com.example.cognibal.ui.theme.AppThemeStyle

/**
 * 设置项持久化存储类
 * 基于 SharedPreferences 实现轻量级无依赖本地存储
 */
class ThemePreferencesRepository(context: Context? = null) {
    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadThemeStyle(): AppThemeStyle {
        val name = prefs?.getString(KEY_THEME_STYLE, AppThemeStyle.WarmCream.name)
        return try {
            AppThemeStyle.valueOf(name ?: AppThemeStyle.WarmCream.name)
        } catch (_: Exception) {
            AppThemeStyle.WarmCream
        }
    }

    fun saveThemeStyle(style: AppThemeStyle) {
        prefs?.edit()?.putString(KEY_THEME_STYLE, style.name)?.apply()
    }

    fun loadDarkMode(): DarkModePreference {
        val id = prefs?.getString(KEY_DARK_MODE, DarkModePreference.FOLLOW_SYSTEM.id)
        return DarkModePreference.fromId(id)
    }

    fun saveDarkMode(mode: DarkModePreference) {
        prefs?.edit()?.putString(KEY_DARK_MODE, mode.id)?.apply()
    }

    fun loadDynamicBlooms(): Boolean {
        return prefs?.getBoolean(KEY_DYNAMIC_BLOOMS, true) ?: true
    }

    fun saveDynamicBlooms(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_DYNAMIC_BLOOMS, enabled)?.apply()
    }

    fun loadHapticFeedback(): Boolean {
        return prefs?.getBoolean(KEY_HAPTIC_FEEDBACK, true) ?: true
    }

    fun saveHapticFeedback(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_HAPTIC_FEEDBACK, enabled)?.apply()
    }

    fun loadCurrency(): CurrencyPreference {
        val name = prefs?.getString(KEY_CURRENCY, CurrencyPreference.CNY.name)
        return try {
            CurrencyPreference.valueOf(name ?: CurrencyPreference.CNY.name)
        } catch (_: Exception) {
            CurrencyPreference.CNY
        }
    }

    fun saveCurrency(currency: CurrencyPreference) {
        prefs?.edit()?.putString(KEY_CURRENCY, currency.name)?.apply()
    }

    companion object {
        private const val PREFS_NAME = "cognibal_theme_preferences"
        private const val KEY_THEME_STYLE = "key_theme_style"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_DYNAMIC_BLOOMS = "key_dynamic_blooms"
        private const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"
        private const val KEY_CURRENCY = "key_currency"
    }
}

/**
 * 全局主题与偏好设置状态控制器
 */
@Stable
class ThemeStateController(
    private val repository: ThemePreferencesRepository,
    initialThemeStyle: AppThemeStyle,
    initialDarkMode: DarkModePreference,
    initialDynamicBlooms: Boolean,
    initialHapticFeedback: Boolean,
    initialCurrency: CurrencyPreference,
) {
    var themeStyle by mutableStateOf(initialThemeStyle)
        private set

    var darkMode by mutableStateOf(initialDarkMode)
        private set

    var dynamicBlooms by mutableStateOf(initialDynamicBlooms)
        private set

    var hapticFeedback by mutableStateOf(initialHapticFeedback)
        private set

    var currency by mutableStateOf(initialCurrency)
        private set

    fun updateThemeStyle(style: AppThemeStyle) {
        themeStyle = style
        repository.saveThemeStyle(style)
    }

    fun updateDarkMode(mode: DarkModePreference) {
        darkMode = mode
        repository.saveDarkMode(mode)
    }

    fun updateDynamicBlooms(enabled: Boolean) {
        dynamicBlooms = enabled
        repository.saveDynamicBlooms(enabled)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        hapticFeedback = enabled
        repository.saveHapticFeedback(enabled)
    }

    fun updateCurrency(currencyPreference: CurrencyPreference) {
        currency = currencyPreference
        repository.saveCurrency(currencyPreference)
    }

    companion object {
        fun preview(): ThemeStateController {
            return ThemeStateController(
                repository = ThemePreferencesRepository(null),
                initialThemeStyle = AppThemeStyle.WarmCream,
                initialDarkMode = DarkModePreference.FOLLOW_SYSTEM,
                initialDynamicBlooms = true,
                initialHapticFeedback = true,
                initialCurrency = CurrencyPreference.CNY,
            )
        }
    }
}

/**
 * 提供全局访问当前 ThemeStateController 的 CompositionLocal
 */
val LocalThemeStateController = staticCompositionLocalOf<ThemeStateController> {
    error("ThemeStateController not provided. Make sure to wrap in CompositionLocalProvider.")
}

/**
 * 在顶层组件记住 ThemeStateController
 */
@Composable
fun rememberThemeStateController(): ThemeStateController {
    val context = LocalContext.current
    return remember {
        val repo = ThemePreferencesRepository(context)
        ThemeStateController(
            repository = repo,
            initialThemeStyle = repo.loadThemeStyle(),
            initialDarkMode = repo.loadDarkMode(),
            initialDynamicBlooms = repo.loadDynamicBlooms(),
            initialHapticFeedback = repo.loadHapticFeedback(),
            initialCurrency = repo.loadCurrency(),
        )
    }
}
