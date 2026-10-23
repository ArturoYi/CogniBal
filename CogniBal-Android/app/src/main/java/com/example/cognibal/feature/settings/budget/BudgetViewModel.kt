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
import com.example.cognibal.data.database.model.CategoryType
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
import com.example.cognibal.screen.feature.settings.budget.state.BudgetCategoryOption
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
 * 本月可以改上限和预警线。以往周期只展示，改本月不会把它们的上限一起改掉。
 * 已花费来自账单合计。进入页面时，如果上一个周期已经结束，按同样的额度补上今天所在的周期。
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
            is BudgetUiEvent.ScopeGlobal -> edit { current ->
                if (event.global && !current.allowGlobal) current
                else if (!event.global && !current.allowCategory) current
                else current.copy(global = event.global)
            }
            is BudgetUiEvent.CategorySelected -> edit { it.copy(categoryId = event.id) }
            is BudgetUiEvent.AmountChanged -> edit {
                it.copy(amountText = sanitizeAmountInput(event.value))
            }
            is BudgetUiEvent.AlertChanged -> edit {
                it.copy(alertPercent = event.percent.coerceIn(
                    BudgetMath.MIN_ALERT_PERCENT,
                    BudgetMath.MAX_ALERT_PERCENT,
                ))
            }
            is BudgetUiEvent.CycleDayChanged -> edit {
                it.copy(cycleDay = event.day.coerceIn(1, 31))
            }
            is BudgetUiEvent.EnabledChanged -> edit { it.copy(enabled = event.enabled) }
            BudgetUiEvent.Save -> save()
            BudgetUiEvent.Delete -> delete()
            is BudgetUiEvent.NotifyChanged -> {
                viewModelScope.launch { preferences.setBudgetNotify(event.enabled) }
            }
        }
    }

    private fun openCreate() {
        if (editor.value?.saving == true) return
        val now = LedgerTime.now()
        val covering = progress.value.filter { item ->
            BudgetMath.contains(item.budget.periodStart, item.budget.periodEnd, now)
        }
        val globalTaken = covering.any { it.budget.budgetType == BudgetType.Global }
        val takenCategories = covering.mapNotNull { it.budget.categoryId }.toSet()
        val options = expenseOptions(latestCategories).filter { it.id !in takenCategories }
        if (globalTaken && options.isEmpty()) {
            issue.value = BudgetIssue.NoScope
            editor.value = null
            return
        }
        issue.value = null
        editor.value = BudgetEditorState(
            global = !globalTaken,
            categoryId = options.firstOrNull()?.id,
            allowGlobal = !globalTaken,
            allowCategory = options.isNotEmpty(),
        )
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
            global = item.budget.budgetType == BudgetType.Global,
            categoryId = item.budget.categoryId,
            amountText = minorToInput(item.budget.limitAmount),
            alertPercent = if (BudgetMath.alertPercentAllowed(percent)) {
                percent
            } else {
                BudgetMath.DEFAULT_ALERT_PERCENT
            },
            enabled = item.budget.isEnable,
            allowGlobal = false,
            allowCategory = false,
            periodType = item.budget.periodType,
            cycleDay = item.budget.cycleDay ?: item.budget.periodStart.dayOfMonth.coerceIn(1, 31),
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
        if (!current.global && current.categoryId == null) {
            issue.value = BudgetIssue.NeedCategory
            return
        }
        val period = periodOf(current) ?: run {
            issue.value = BudgetIssue.Failed
            return
        }
        val month = current.id == null || current.periodType == BudgetPeriodType.Month
        val draft = BudgetDraft(
            budgetType = if (current.global) BudgetType.Global else BudgetType.Category,
            categoryId = if (current.global) null else current.categoryId,
            limitAmount = amount,
            alertPercent = current.alertPercent,
            periodType = if (current.id == null) BudgetPeriodType.Month else current.periodType,
            cycleDay = if (month) current.cycleDay else null,
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
                    budgets.insert(draft, enabled = current.enabled)
                } else {
                    budgets.update(id, draft, enabled = current.enabled)
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
            return BudgetPeriods.monthContaining(editor.cycleDay.coerceIn(1, 31), LedgerTime.now().toLocalDate())
        }
        val start = editor.periodStart
        val end = editor.periodEnd
        if (start != null && end != null) {
            return com.example.cognibal.data.repository.ClosedOpenRange(start, end)
        }
        return null
    }

    private var latestCategories: List<Category> = emptyList()

    private fun toState(
        rows: List<BudgetProgress>,
        categoryRows: List<Category>,
        prefs: AppPreferences,
        editing: BudgetEditorState?,
        currentIssue: BudgetIssue?,
    ): BudgetUiState {
        latestCategories = categoryRows
        val names = categoryRows.associateBy { it.id }
        val now = LedgerTime.now()
        val lines = rows.map { item -> item.toLine(names, now) }
        val options = expenseOptions(categoryRows)
        val taken = lines.filter { it.editable }.mapNotNull { line ->
            rows.firstOrNull { it.budget.id == line.id }?.budget?.categoryId
        }.toSet()
        val picker = if (editing?.id == null) options.filter { it.id !in taken } else options
        return BudgetUiState(
            loading = false,
            currencySymbol = prefs.currency.symbol,
            notify = prefs.budgetNotify,
            current = lines.filter { it.editable }.sortedWith(lineOrder),
            history = lines.filter { !it.editable }.sortedWith(historyOrder),
            categories = picker,
            editor = editing,
            issue = currentIssue,
        )
    }

    private fun expenseOptions(categoryRows: List<Category>): List<BudgetCategoryOption> {
        return categoryRows
            .filter { it.type == CategoryType.Expense && it.isEnable }
            .sortedWith(compareBy<Category> { it.sort }.thenBy { it.id })
            .map { BudgetCategoryOption(id = it.id, name = it.name) }
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
