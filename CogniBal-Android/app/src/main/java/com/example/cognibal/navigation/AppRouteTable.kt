package com.example.cognibal.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.cognibal.screen.settings.ui.AppearanceSettingsRoute
import com.example.cognibal.screen.settings.ui.BackupSettingsRoute
import com.example.cognibal.screen.settings.ui.SecuritySettingsRoute
import com.example.cognibal.screen.settings.ui.SettingsDetailPlaceholder
import com.example.cognibal.screen.settings.ui.SettingsListRoute

/**
 * 路由表。每个目的地只在这里绑定一次界面。
 *
 * 设置列表是列表窗格，三个二级页是详情窗格。窄屏退回单页，由 [AppNavHost] 做滑动。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.appRoutes(navigator: AppNavigator) {
    entry<SettingsList>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = { SettingsDetailPlaceholder() },
        ),
    ) {
        SettingsListRoute(
            onNavigate = navigator::navigate,
        )
    }
    entry<AppearanceSettings>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        AppearanceSettingsRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
    entry<BackupSettings>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        BackupSettingsRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
    entry<SecuritySettings>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        SecuritySettingsRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
}
