package com.example.cognibal.screen.feature.settings.state

/**
 * 设置列表一次性渲染所需的状态。关于弹窗只存在于 ViewModel。
 * 二级页有自己的 ViewModel，不放在这里。
 */
internal data class SettingsUiState(
    val daysInUse: Int = 18,
    val totalRecordsCount: Int = 42,
    val showAboutDialog: Boolean = false,
)

/** 设置列表向上递交的操作。外观、备份、更多设置等二级页不经过这里。 */
internal sealed interface SettingsUiEvent {
    data object OpenAboutDialog : SettingsUiEvent
    data object DismissAboutDialog : SettingsUiEvent

    /** 用系统邮件客户端给开发者写反馈。 */
    data object SendFeedback : SettingsUiEvent
}
