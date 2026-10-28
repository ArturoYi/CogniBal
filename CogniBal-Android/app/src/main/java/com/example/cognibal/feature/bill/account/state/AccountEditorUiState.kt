package com.example.cognibal.screen.feature.bill.account.state

import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.screen.feature.bill.model.AccountColor
import com.example.cognibal.screen.feature.bill.model.AccountIcon
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference

internal data class AccountEditorUiState(
    val ready: Boolean = false,
    val missing: Boolean = false,
    val isEdit: Boolean = false,
    val name: String = "",
    val type: AccountType = AccountType.Cash,
    val icon: AccountIcon = AccountIcon.Cash,
    /** 备份里不是枚举键的图标。用户重新选过图标后清空，保存时改用枚举键。 */
    val iconKeyOverride: String? = null,
    val color: AccountColor = AccountColor.Teal,
    val colorKeyOverride: String? = null,
    val openingBalanceText: String = "",
    val remark: String = "",
    val currentBalance: Long? = null,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val billCount: Int = 0,
    val enabledCount: Int = 0,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val error: LedgerReject? = null,
    val saving: Boolean = false,
    val confirmDelete: Boolean = false,
)

internal sealed interface AccountEditorUiEvent {
    data class NameChanged(val value: String) : AccountEditorUiEvent
    data class RemarkChanged(val value: String) : AccountEditorUiEvent
    data class OpeningBalanceChanged(val value: String) : AccountEditorUiEvent
    data class TypeSelected(val type: AccountType) : AccountEditorUiEvent
    data class ColorSelected(val color: AccountColor) : AccountEditorUiEvent
    /** 取色板里选中的颜色。写成 `#AARRGGBB`，不占用预设键。 */
    data class CustomColorSelected(val hex: String) : AccountEditorUiEvent
    data object Save : AccountEditorUiEvent
    data class SetEnabled(val enabled: Boolean) : AccountEditorUiEvent
    data object DeleteClicked : AccountEditorUiEvent
    data object DismissDelete : AccountEditorUiEvent
    data object ConfirmDelete : AccountEditorUiEvent
}

/** 编辑页上为什么不能出现删除按钮。仓库仍会再拦一次。 */
internal enum class AccountDeleteBlock {
    Bills,
    Default,
    LastEnabled,
}

internal fun AccountEditorUiState.deleteBlock(): AccountDeleteBlock? {
    if (!isEdit) return null
    if (billCount > 0) return AccountDeleteBlock.Bills
    if (isDefault) return AccountDeleteBlock.Default
    if (enabled && enabledCount <= 1) return AccountDeleteBlock.LastEnabled
    return null
}
