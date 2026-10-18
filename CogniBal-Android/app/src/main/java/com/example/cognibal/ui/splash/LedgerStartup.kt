package com.example.cognibal.ui.splash

import android.content.Context
import android.util.Log
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.preferences.SelectedAccountStore
import com.example.cognibal.data.repository.AccountDraft
import com.example.cognibal.data.repository.NewAccount
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.bill.model.AccountColor
import com.example.cognibal.screen.bill.model.AccountIcon
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException

/**
 * 开屏期间要做完的账本准备。
 *
 * 没有任何账户时写入一个现金账户，并标成当前展示。已经有账户就不再创建。
 * 旧版偏好里记过的账户会改写到 is_default，然后删掉那个偏好。
 * 失败只记日志，避免开屏永远停住。
 */
internal object LedgerStartup {
    suspend fun ensureDefaultAccount(context: Context) {
        try {
            val app = context.applicationContext
            val repository = EntryPointAccessors.fromApplication(
                app,
                LedgerEntryPoint::class.java,
            ).accountRepository()
            val remembered = SelectedAccountStore.create(app)
            val rememberedId = remembered.peekRememberedId()
            repository.prepareForStartup(
                NewAccount(
                    profile = AccountDraft(
                        name = app.getString(R.string.account_default_name),
                        icon = AccountIcon.Wallet.storageKey,
                        color = AccountColor.Teal.storageKey,
                        type = AccountType.Cash,
                        remark = null,
                    ),
                    openingBalance = 0L,
                ),
                rememberedDisplayId = rememberedId,
            )
            remembered.clearRememberedId()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "启动时没能准备默认账户", exception)
        }
    }

    private const val TAG = "LedgerStartup"
}
