package com.example.cognibal.screen.feature.settings.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.toAssetSnapshot
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.assets.state.AssetsUiState
import com.example.cognibal.screen.feature.settings.assets.state.toAssetAccountUi
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 资产页。只读账户余额，不改账。
 *
 * 净资产和首页总资产用同一套加法。信用卡欠款单独加总成负债。
 */
internal class AssetsViewModel(
    accounts: AccountRepository,
    preferences: AppPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<AssetsUiState> = combine(
        accounts.observeAll(),
        preferences.preferences,
    ) { rows, prefs ->
        val snapshot = rows.toAssetSnapshot()
        AssetsUiState(
            loading = false,
            currencySymbol = prefs.currency.symbol,
            net = snapshot.net,
            netOverflow = snapshot.netOverflow,
            assets = snapshot.assets,
            assetsOverflow = snapshot.assetsOverflow,
            liabilities = snapshot.liabilities,
            liabilitiesOverflow = snapshot.liabilitiesOverflow,
            assetAccounts = snapshot.assetAccounts.map { it.toAssetAccountUi(debt = false) },
            liabilityAccounts = snapshot.liabilityAccounts.map { it.toAssetAccountUi(debt = true) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AssetsUiState(),
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("AssetsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                AssetsViewModel(
                    accounts = entry.accountRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}
