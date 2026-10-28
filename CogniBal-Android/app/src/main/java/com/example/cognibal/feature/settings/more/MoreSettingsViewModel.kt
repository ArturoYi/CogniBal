package com.example.cognibal.screen.feature.settings.more

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.CurrencyConversionRepository
import com.example.cognibal.data.repository.ExchangeRate
import com.example.cognibal.data.repository.LedgerException
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.screen.feature.settings.more.state.CurrencySwitchFailure
import com.example.cognibal.screen.feature.settings.more.state.DEFAULT_CACHE_SIZE
import com.example.cognibal.screen.feature.settings.more.state.MoreSettingsUiEvent
import com.example.cognibal.screen.feature.settings.more.state.MoreSettingsUiState
import com.example.cognibal.ui.process.reopenApp
import dagger.hilt.android.EntryPointAccessors
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** 更多设置。换货币时按汇率改账本，盖层至少停 [ExchangeRate.MIN_SWITCH_MILLIS]，数据库工作不超过 [ExchangeRate.MAX_SWITCH_MILLIS]。 */
internal class MoreSettingsViewModel(
    private val appContext: Context,
    private val preferenceRepository: AppPreferencesRepository,
    private val conversion: CurrencyConversionRepository,
) : ViewModel() {
    private val transient = MutableStateFlow(MoreTransient())
    private var pendingTarget: CurrencyPreference? = null
    private var pendingRate: BigDecimal? = null
    private var amountsConverted: Boolean = false
    private var switchJob: Job? = null

    val uiState: StateFlow<MoreSettingsUiState> = combine(
        preferenceRepository.preferences,
        transient,
    ) { stored, extra ->
        MoreSettingsUiState(
            currency = stored.currency,
            cacheSizeText = extra.cacheSizeText,
            showCurrencyDialog = extra.showCurrencyDialog,
            showClearCacheDialog = extra.showClearCacheDialog,
            rateTarget = extra.rateTarget,
            rateText = extra.rateText,
            rateReady = extra.rateReady,
            converting = extra.converting,
            failure = extra.failure,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MoreSettingsUiState(),
    )

    fun onEvent(event: MoreSettingsUiEvent) {
        when (event) {
            MoreSettingsUiEvent.OpenCurrencyDialog -> {
                if (transient.value.converting) return
                if (amountsConverted) {
                    launchSwitch()
                } else {
                    transient.update { it.copy(showCurrencyDialog = true, failure = null) }
                }
            }
            MoreSettingsUiEvent.DismissCurrencyDialog ->
                transient.update { it.copy(showCurrencyDialog = false) }
            is MoreSettingsUiEvent.CurrencySelected -> onCurrencySelected(event.currency)
            is MoreSettingsUiEvent.RateTextChanged -> {
                val text = ExchangeRate.sanitizeInput(event.text)
                transient.update {
                    it.copy(rateText = text, rateReady = ExchangeRate.parse(text) != null)
                }
            }
            MoreSettingsUiEvent.DismissRateDialog -> {
                if (transient.value.converting) return
                transient.update { it.copy(rateTarget = null, rateText = "", rateReady = false) }
            }
            MoreSettingsUiEvent.ConfirmRate -> confirmRate()
            MoreSettingsUiEvent.DismissCurrencyFailure ->
                transient.update { it.copy(failure = null) }
            MoreSettingsUiEvent.RetryCurrencySwitch -> launchSwitch()
            MoreSettingsUiEvent.OpenClearCacheDialog -> {
                if (transient.value.converting) return
                transient.update { it.copy(showClearCacheDialog = true) }
            }
            MoreSettingsUiEvent.DismissClearCacheDialog ->
                transient.update { it.copy(showClearCacheDialog = false) }
            MoreSettingsUiEvent.ConfirmClearCache ->
                transient.update { it.copy(cacheSizeText = "0 KB", showClearCacheDialog = false) }
        }
    }

    private fun onCurrencySelected(currency: CurrencyPreference) {
        transient.update { it.copy(showCurrencyDialog = false) }
        if (currency == uiState.value.currency || amountsConverted) return
        pendingTarget = null
        pendingRate = null
        transient.update {
            it.copy(rateTarget = currency, rateText = "", rateReady = false, failure = null)
        }
    }

    private fun confirmRate() {
        if (transient.value.converting || amountsConverted) return
        val target = transient.value.rateTarget ?: return
        val rate = ExchangeRate.parse(transient.value.rateText) ?: return
        if (target == uiState.value.currency) {
            transient.update { it.copy(rateTarget = null, rateText = "", rateReady = false) }
            return
        }
        pendingTarget = target
        pendingRate = rate
        launchSwitch()
    }

    private fun launchSwitch() {
        if (switchJob?.isActive == true) return
        val target = pendingTarget ?: return
        if (!amountsConverted && pendingRate == null) return
        transient.update {
            it.copy(
                converting = true,
                failure = null,
                showCurrencyDialog = false,
                showClearCacheDialog = false,
                rateTarget = null,
            )
        }
        switchJob = viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val failure = runSwitch(target)
            if (failure != null) {
                transient.update { it.copy(converting = false, failure = failure) }
                return@launch
            }
            val remain = ExchangeRate.MIN_SWITCH_MILLIS - (SystemClock.elapsedRealtime() - startedAt)
            withContext(NonCancellable) {
                if (remain > 0) delay(remain)
                appContext.reopenApp()
                exitProcess(0)
            }
        }
    }

    private suspend fun runSwitch(target: CurrencyPreference): CurrencySwitchFailure? {
        if (!amountsConverted) {
            val rate = pendingRate ?: return CurrencySwitchFailure.Failed
            try {
                withTimeout(ExchangeRate.MAX_SWITCH_MILLIS) {
                    conversion.apply(rate)
                }
            } catch (timeout: TimeoutCancellationException) {
                return CurrencySwitchFailure.TimedOut
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ledger: LedgerException) {
                return if (ledger.reason == LedgerReject.BalanceOverflow) {
                    CurrencySwitchFailure.Overflow
                } else {
                    CurrencySwitchFailure.Failed
                }
            } catch (_: Exception) {
                return CurrencySwitchFailure.Failed
            }
            amountsConverted = true
        }
        return try {
            withContext(NonCancellable) {
                preferenceRepository.setCurrencyOrThrow(target)
            }
            null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            CurrencySwitchFailure.Preference
        }
    }

    private data class MoreTransient(
        val cacheSizeText: String = DEFAULT_CACHE_SIZE,
        val showCurrencyDialog: Boolean = false,
        val showClearCacheDialog: Boolean = false,
        val rateTarget: CurrencyPreference? = null,
        val rateText: String = "",
        val rateReady: Boolean = false,
        val converting: Boolean = false,
        val failure: CurrencySwitchFailure? = null,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("MoreSettingsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                MoreSettingsViewModel(
                    appContext = application.applicationContext,
                    preferenceRepository = AppPreferencesRepository.create(application),
                    conversion = entry.currencyConversionRepository(),
                )
            }
        }
    }
}
