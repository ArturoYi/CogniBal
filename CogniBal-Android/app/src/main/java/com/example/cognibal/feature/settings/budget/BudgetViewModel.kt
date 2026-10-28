package com.example.cognibal.screen.feature.settings.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.BudgetDraft
import com.example.cognibal.data.repository.BudgetMath
import com.example.cognibal.data.repository.BudgetPeriods
import com.example.cognibal.data.repository.BudgetProgress
import com.example.cognibal.data.repository.BudgetRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.data.repository.LedgerException
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.bill.tool.minorToInput
import com.example.cognibal.screen.feature.bill.tool.parseMajorAmount
import com.example.cognibal.screen.feature.expense.tool.sanitizeAmountInput
import com.example.cognibal.screen.feature.settings.budget.state.BudgetEditorState
import com.example.cognibal.screen.feature.settings.budget.state.BudgetIssue
import com.example.cognibal.screen.feature.settings.budget.state.BudgetLineUi
import com.example.cognibal.screen.feature.settings.budget.state.BudgetUiEvent
import com.example.cognibal.screen.feature.settings.budget.state.BudgetUiState
import com.example.cognibal.screen.feature.settings.model.AppPreferences
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 预算页。
 *
 * 本月只有一条总预算，按自然月统计。没有时添加，有了只能编辑。
 * 保存时默认启用。以往周期只展示，改本月不会把它们的上限一起改掉。
 * 已花费来自账单合计。进入页面时，如果上一个周期已经结束，按同样的额度补上今天所在的自然月。
 */
internal class BudgetViewModel(
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    private val editor = MutableStateFlow<BudgetEditorState?>(null)
    private val issue = MutableStateFlow<BudgetIssue?>(null)
    private val progress = budgets.observeProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<BudgetUiState> = combine(
        combine(progress, categories.observeAll(), preferences.preferences, ::Triple),
        editor,
        issue,
    ) { (rows, categoryRows, prefs), editing, currentIssue ->
        toState(rows, categoryRows, prefs, editing, currentIssue)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = BudgetUiState(),
    )

    init {
        viewModelScope.launch {
            try {
                budgets.ensureCurrentPeriods()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // 没补上下个周期时，页面仍显示已经覆盖今天的预算。
            }
        }
    }

    fun onEvent(event: BudgetUiEvent) {
        when (event) {
            BudgetUiEvent.Add -> openCreate()
            is BudgetUiEvent.Edit -> openEdit(event.id)
            BudgetUiEvent.DismissEditor -> {
                editor.value = null
                issue.value = null
            }
            is BudgetUiEvent.AmountChanged -> edit {
                it.copy(amountText = sanitizeAmountInput(event.value))
            }
            is BudgetUiEvent.AlertChanged -> edit {
                it.copy(alertPercent = event.percent.coerceIn(
                    BudgetMath.MIN_ALERT_PERCENT,
                    BudgetMath.MAX_ALERT_PERCENT,
                ))
            }
            BudgetUiEvent.Save -> save()
            BudgetUiEvent.Delete -> delete()
            is BudgetUiEvent.NotifyChanged -> {
                viewModelScope.launch { preferences.setBudgetNotify(event.enabled) }
            }
            is BudgetUiEvent.HomeVisibleChanged -> {
                viewModelScope.launch { preferences.setBudgetHomeVisible(event.visible) }
            }
        }
    }

    private fun openCreate() {
        if (editor.value?.saving == true) return
        val now = LedgerTime.now()
        val existing = progress.value.firstOrNull { item ->
            item.budget.budgetType == BudgetType.Global &&
                BudgetMath.contains(item.budget.periodStart, item.budget.periodEnd, now)
        }
        if (existing != null) {
            openEdit(existing.budget.id)
            return
        }
        issue.value = null
        editor.value = BudgetEditorState()
    }

    private fun openEdit(id: Long) {
        if (editor.value?.saving == true) return
        val item = progress.value.firstOrNull { it.budget.id == id } ?: return
        val now = LedgerTime.now()
        if (!BudgetMath.contains(item.budget.periodStart, item.budget.periodEnd, now)) return
        val percent = item.budget.alertPercent
        issue.value = null
        editor.value = BudgetEditorState(
            id = id,
            amountText = minorToInput(item.budget.limitAmount),
            alertPercent = if (BudgetMath.alertPercentAllowed(percent)) {
                percent
            } else {
                BudgetMath.DEFAULT_ALERT_PERCENT
            },
            periodType = item.budget.periodType,
            periodStart = item.budget.periodStart,
            periodEnd = item.budget.periodEnd,
        )
    }

    private fun save() {
        val current = editor.value ?: return
        if (current.saving) return
        val amount = parseMajorAmount(current.amountText)
        if (amount == null || amount <= 0L) {
            issue.value = BudgetIssue.InvalidAmount
            return
        }
        val period = periodOf(current) ?: run {
            issue.value = BudgetIssue.Failed
            return
        }
        val month = current.id == null || current.periodType == BudgetPeriodType.Month
        val draft = BudgetDraft(
            budgetType = BudgetType.Global,
            categoryId = null,
            limitAmount = amount,
            alertPercent = current.alertPercent,
            periodType = if (current.id == null) BudgetPeriodType.Month else current.periodType,
            cycleDay = if (month) BudgetPeriods.CALENDAR_MONTH_DAY else null,
            periodStart = period.startInclusive,
            periodEnd = period.endExclusive,
            remark = null,
        )
        editor.update { it?.copy(saving = true) }
        issue.value = null
        viewModelScope.launch {
            try {
                val id = current.id
                if (id == null) {
                    budgets.insert(draft, enabled = true)
                } else {
                    budgets.update(id, draft, enabled = true)
                }
                editor.value = null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: LedgerException) {
                editor.update { it?.copy(saving = false) }
                issue.value = exception.reason.toBudgetIssue()
            } catch (_: Exception) {
                editor.update { it?.copy(saving = false) }
                issue.value = BudgetIssue.Failed
            }
        }
    }

    private fun delete() {
        val id = editor.value?.id ?: return
        if (editor.value?.saving == true) return
        editor.update { it?.copy(saving = true) }
        viewModelScope.launch {
            try {
                budgets.delete(id)
                editor.value = null
                issue.value = null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                editor.update { it?.copy(saving = false) }
                issue.value = BudgetIssue.Failed
            }
        }
    }

    private fun edit(block: (BudgetEditorState) -> BudgetEditorState) {
        editor.update { current ->
            if (current == null || current.saving) current else block(current)
        }
        issue.update { current -> if (current == BudgetIssue.NoScope) current else null }
    }

    private fun periodOf(editor: BudgetEditorState): com.example.cognibal.data.repository.ClosedOpenRange? {
        val month = editor.id == null || editor.periodType == BudgetPeriodType.Month
        if (month) {
            return BudgetPeriods.monthContaining(
                BudgetPeriods.CALENDAR_MONTH_DAY,
                LedgerTime.now().toLocalDate(),
            )
        }
        val start = editor.periodStart
        val end = editor.periodEnd
        if (start != null && end != null) {
            return com.example.cognibal.data.repository.ClosedOpenRange(start, end)
        }
        return null
    }

    private fun toState(
        rows: List<BudgetProgress>,
        categoryRows: List<Category>,
        prefs: AppPreferences,
        editing: BudgetEditorState?,
        currentIssue: BudgetIssue?,
    ): BudgetUiState {
        val names = categoryRows.associateBy { it.id }
        val now = LedgerTime.now()
        val lines = rows.map { item -> item.toLine(names, now) }
        val globals = lines.filter { it.global }
        return BudgetUiState(
            loading = false,
            currencySymbol = prefs.currency.symbol,
            notify = prefs.budgetNotify,
            homeVisible = prefs.budgetHomeVisible,
            current = globals.filter { it.editable }.sortedWith(lineOrder).take(1),
            history = globals.filter { !it.editable }.sortedWith(historyOrder),
            editor = editing,
            issue = currentIssue,
        )
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("BudgetViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                BudgetViewModel(
                    budgets = entry.budgetRepository(),
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}

private val lineOrder = compareBy<BudgetLineUi> { !it.global }.thenBy { it.categoryName ?: "" }.thenBy { it.id }
private val historyOrder = compareByDescending<BudgetLineUi> { it.periodStart }.thenByDescending { it.id }

private fun BudgetProgress.toLine(
    categories: Map<Long, Category>,
    now: LocalDateTime,
): BudgetLineUi {
    val categoryId = budget.categoryId
    val category = categoryId?.let { categories[it] }
    return BudgetLineUi(
        id = budget.id,
        global = budget.budgetType == BudgetType.Global,
        categoryName = category?.name,
        missingCategory = budget.budgetType == BudgetType.Category && category == null,
        spent = spentAmount,
        limit = budget.limitAmount,
        progress = progressFraction(),
        level = level(),
        alertPercent = budget.alertPercent,
        enabled = budget.isEnable,
        editable = BudgetMath.contains(budget.periodStart, budget.periodEnd, now),
        periodStart = budget.periodStart.toLocalDate(),
        periodType = budget.periodType,
        cycleDay = budget.cycleDay,
    )
}

private fun LedgerReject.toBudgetIssue(): BudgetIssue = when (this) {
    LedgerReject.InvalidLimit -> BudgetIssue.InvalidAmount
    LedgerReject.BudgetOverlap -> BudgetIssue.Overlap
    LedgerReject.BudgetCategoryRequired -> BudgetIssue.NeedCategory
    else -> BudgetIssue.Failed
}
