package com.cyr.cognobal.screen.feature.settings

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.data.repository.StatsRepository
import com.cyr.cognobal.di.LedgerEntryPoint
import com.cyr.cognobal.screen.feature.legal.LEGAL_ACCEPTANCE_VERSION
import com.cyr.cognobal.screen.feature.legal.LegalGate
import com.cyr.cognobal.screen.feature.settings.model.AppPreferences
import com.cyr.cognobal.screen.feature.settings.state.SettingsUiEvent
import com.cyr.cognobal.screen.feature.settings.state.SettingsUiState
import com.cyr.cognobal.time.TimeManager
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置列表和应用主题的状态。
 *
 * 挂在 Activity 上：入口用 [preferences] 决定主题，设置页用 [uiState] 画分组列表。
 * 两边拿到的是同一个 ViewModel。
 * 陪伴天数从安装日算到今天；导入的账本若更早，从最早一笔记账的创建日算起。
 * 记账笔数是支出、收入，以及按一次转账合并后的条数。
 */
internal class SettingsViewModel(
    private val repository: AppPreferencesRepository,
    stats: StatsRepository,
    private val installedOn: LocalDate,
) : ViewModel() {
    /**
     * 应用级偏好。Activity 进入组合就开始收集，启动动画期间 DataStore 有时间读完。
     * 首帧用默认值，读到磁盘后再替换。
     */
    val preferences: StateFlow<AppPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppPreferences(),
    )

    /**
     * 协议确认。首帧是 [LegalGate.Pending]，避免磁盘还没读完就闪出确认页。
     * 同意的版本和正文日期不一致时，重新确认。
     */
    val legalGate: StateFlow<LegalGate> = repository.preferences
        .map { preferences ->
            if (preferences.acceptedLegalVersion == LEGAL_ACCEPTANCE_VERSION) {
                LegalGate.Accepted
            } else {
                LegalGate.Required
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LegalGate.Pending,
        )

    private val showAboutDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        showAboutDialog,
        stats.observeUsage(),
        TimeManager.observeToday(),
    ) { about, usage, today ->
        SettingsUiState(
            daysInUse = companionshipDays(installedOn, usage.earliestCreatedOn, today),
            totalRecordsCount = usage.entryCount,
            showAboutDialog = about,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(
            daysInUse = companionshipDays(installedOn, earliestRecordOn = null, today = TimeManager.currentDay()),
        ),
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OpenAboutDialog -> showAboutDialog.value = true
            SettingsUiEvent.DismissAboutDialog -> showAboutDialog.value = false
            // 发信由界面拉起邮件客户端，这里只接住事件。
            SettingsUiEvent.SendFeedback -> Unit
        }
    }

    fun acceptLegalTerms() {
        viewModelScope.launch {
            repository.setAcceptedLegalVersion(LEGAL_ACCEPTANCE_VERSION)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("SettingsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                SettingsViewModel(
                    repository = AppPreferencesRepository.create(application),
                    stats = entry.statsRepository(),
                    installedOn = application.installedOn(),
                )
            }
        }
    }
}

/**
 * 陪伴天数。从开始使用的那天算到 [today]，两天都算在内。
 *
 * 开始日是 [installedOn]。账本里如果有更早的创建日，改用那天，
 * 换机导入后天数才不会被新的安装日截短。账单发生时间可以补记，不参与这里。
 * 开始日晚于今天时记 1 天。
 */
internal fun companionshipDays(
    installedOn: LocalDate,
    earliestRecordOn: LocalDate?,
    today: LocalDate,
): Int {
    val startedOn = if (earliestRecordOn != null && earliestRecordOn.isBefore(installedOn)) {
        earliestRecordOn
    } else {
        installedOn
    }
    val inclusive = try {
        Math.addExact(ChronoUnit.DAYS.between(startedOn, today), 1L)
    } catch (_: ArithmeticException) {
        return 1
    }
    return when {
        inclusive < 1L -> 1
        inclusive > Int.MAX_VALUE -> Int.MAX_VALUE
        else -> inclusive.toInt()
    }
}

private fun Application.installedOn(): LocalDate {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    val millis = info.firstInstallTime
    if (millis <= 0L) return TimeManager.currentDay()
    return TimeManager.toLocalDate(millis) ?: TimeManager.currentDay()
}
