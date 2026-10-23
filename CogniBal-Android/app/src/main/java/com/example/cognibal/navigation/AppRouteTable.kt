package com.example.cognibal.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.cognibal.screen.feature.bill.account.AccountDetailPlaceholder
import com.example.cognibal.screen.feature.bill.account.AccountEditorRoute
import com.example.cognibal.screen.feature.bill.history.BillHistoryRoute
import com.example.cognibal.screen.feature.bill.BillRoute
import com.example.cognibal.screen.feature.expense.AddExpenseRoute
import com.example.cognibal.screen.feature.settings.appearance.AppearanceSettingsRoute
import com.example.cognibal.screen.feature.settings.backup.BackupSettingsRoute
import com.example.cognibal.screen.feature.settings.overview.OverviewRoute
import com.example.cognibal.screen.feature.settings.reports.ReportsRoute
import com.example.cognibal.screen.feature.settings.security.SecuritySettingsRoute
import com.example.cognibal.screen.feature.settings.budget.BudgetRoute
import com.example.cognibal.screen.feature.settings.assets.AssetsRoute
import com.example.cognibal.screen.feature.settings.SettingsDetailPlaceholder
import com.example.cognibal.screen.feature.settings.SettingsListRoute
import com.example.cognibal.screen.feature.settings.statistics.StatisticsRoute

/**
 * 路由表。每个目的地只在这里绑定一次界面。
 *
 * 账单和设置列表是首页标签。记一笔是普通整页。
 * 账单页和设置列表都是列表窗格：宽屏时账户表单、设置二级页显示在旁边。窄屏仍一次只显示一页。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.appRoutes(navigator: AppNavigator) {
    entry<Bill>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = { AccountDetailPlaceholder() },
        ) + homeTabMetadata(),
    ) {
        BillRoute(
            onCreateAccount = { navigator.navigate(AccountEditor()) },
            onEditAccount = { accountId -> navigator.navigate(AccountEditor(accountId)) },
            onViewAll = { accountId -> navigator.navigate(BillHistory(accountId)) },
            onOpenBudget = { navigator.navigate(Budget) },
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<BillHistory> { key ->
        BillHistoryRoute(
            accountId = key.accountId,
            onBack = dropUnlessResumed(block = navigator::back),
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<AccountEditor>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        AccountEditorRoute(
            accountId = key.accountId,
            onBack = dropUnlessResumed(block = navigator::back),
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<AddExpense> { key ->
        AddExpenseRoute(
            typeCode = key.typeCode,
            onBack = dropUnlessResumed(block = navigator::back),
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<SettingsList>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = { SettingsDetailPlaceholder() },
        ) + homeTabMetadata(),
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
    entry<Statistics>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        StatisticsRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
    entry<Overview>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        OverviewRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
    entry<Reports>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        ReportsRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
    entry<Budget>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        BudgetRoute(
            onBack = dropUnlessResumed(block = navigator::back),
        )
    }
    entry<Assets>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        AssetsRoute(
            onBack = dropUnlessResumed(block = navigator::back),
            onOpenAccount = { accountId -> navigator.navigate(AccountEditor(accountId)) },
        )
    }
}
