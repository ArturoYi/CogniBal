package com.example.cognibal.screen.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.CategoryType
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.BillRepository
import com.example.cognibal.data.repository.BudgetImpact
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.data.repository.BudgetRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.data.repository.EntryDraft
import com.example.cognibal.data.repository.EntryType
import com.example.cognibal.data.repository.LedgerException
import com.example.cognibal.data.repository.LedgerLimits
import com.example.cognibal.data.repository.TransferDraft
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.bill.tool.parseMajorAmount
import com.example.cognibal.screen.feature.expense.model.AddBillIssue
import com.example.cognibal.screen.feature.expense.tool.resolveAccountId
import com.example.cognibal.screen.feature.expense.tool.resolveCounterpartyId
import com.example.cognibal.screen.feature.expense.tool.sanitizeAmountInput
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.example.cognibal.screen.feature.expense.state.AccountChoice
import com.example.cognibal.screen.feature.expense.state.AddExpenseUiEvent
import com.example.cognibal.screen.feature.expense.state.AddExpenseUiState
import com.example.cognibal.screen.feature.expense.state.CategoryChoice
import com.example.cognibal.screen.feature.expense.state.CategoryCreateResult
import com.example.cognibal.screen.feature.expense.state.OverBudgetPrompt
import com.example.cognibal.screen.feature.settings.budget.BudgetNotifier
import com.example.cognibal.screen.feature.expense.model.toAddBillIssue
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 记一笔。
 *
 * 支出和收入写成一条 [EntryDraft]：金额为正，分类必须是同类型且启用。
 * 转账写成 [TransferDraft]：金额为正，两个账户不同，仓库再拆成一负一正两条，分类为空。
 * 创建时间和更新时间由仓库填写。账单时间是交易发生时间，可以改。
 *
 * 分类表是空的时候补一套默认分类。旧库只补这一版新增的默认项，用户删掉的不会再填回来。
 * 支出和收入记入首页当前选中的账户。转账才使用单独选的转出账户。
 */
internal class AddExpenseViewModel(
    private val initialType: BillType,
    private val bills: BillRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val preferences: AppPreferencesRepository,
    private val notifier: BudgetNotifier,
    accounts: AccountRepository,
) : ViewModel() {
    private val openedAt = LedgerTime.now().withSecond(0).withNano(0)
    private val form = MutableStateFlow(EntryForm(type = initialType, billTime = openedAt))

    init {
        viewModelScope.launch {
            try {
                categories.seedDefaultsIfEmpty()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // 默认分类没写上时，页面按当前分类表继续。
            }
        }
    }

    val uiState: StateFlow<AddExpenseUiState> = combine(
        form,
        accounts.observeEnabled(),
        categories.observeAll(),
        preferences.preferences,
    ) { draft, accountRows, categoriesRows, prefs ->
        val orderedAccounts = accountRows.sortedWith(
            compareByDescending<Account> { it.isDefault }.thenBy { it.id },
        )
        val accountId = if (draft.type == BillType.Transfer) {
            resolveAccountId(draft.accountId, orderedAccounts)
        } else {
            resolveAccountId(null, orderedAccounts)
        }
        val categoryType = draft.type.categoryType()
        val matching = if (categoryType == null) {
            emptyList()
        } else {
            categoriesRows.filter { it.isEnable && it.type == categoryType }
        }
        AddExpenseUiState(
            ready = true,
            type = draft.type,
            amountText = draft.amountText,
            categories = matching.map { category ->
                CategoryChoice(
                    id = category.id,
                    name = category.name,
                    iconKey = category.icon,
                    colorKey = category.color,
                )
            },
            categoryId = categoryType?.let { type ->
                draft.categoryId?.takeIf { id ->
                    categoriesRows.any { it.id == id && it.isEnable && it.type == type }
                }
            },
            accounts = orderedAccounts.map { account ->
                AccountChoice(
                    id = account.id,
                    name = account.name,
                    iconKey = account.icon,
                    colorKey = account.color,
                )
            },
            accountId = accountId,
            toAccountId = if (draft.type == BillType.Transfer) {
                resolveCounterpartyId(draft.toAccountId, accountId, orderedAccounts)
            } else {
                null
            },
            billTime = draft.billTime,
            remark = draft.remark,
            currencySymbol = prefs.currency.symbol,
            issue = draft.issue,
            saving = draft.saving,
            overBudgetPrompt = draft.overBudgetPrompt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AddExpenseUiState(type = initialType, billTime = openedAt),
    )

    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    private val _categoryCreated = MutableSharedFlow<CategoryCreateResult>(extraBufferCapacity = 1)
    val categoryCreated: SharedFlow<CategoryCreateResult> = _categoryCreated.asSharedFlow()

    private var visible = false
    private var completed = false
    private var saveToken = 0
    private var creatingCategory = false

    /** 页面进入组合。空白表在离开时已经换好，这里不再改状态，避免第一帧先画出上一张草稿。 */
    fun onEnter() {
        visible = true
    }

    /**
     * 页面离开组合。未保存的草稿留着。
     * 已经保存、或保存过程中离开，这里立刻换成空白表。库里的事务仍会提交。
     * 下次进入时 [uiState] 已经是新表，界面不用先闪一下旧内容。
     */
    fun onLeave() {
        visible = false
        if (completed || form.value.saving) {
            completed = false
            saveToken += 1
            form.value = freshForm()
        }
    }

    fun onEvent(event: AddExpenseUiEvent) {
        when (event) {
            is AddExpenseUiEvent.TypeSelected -> edit { current ->
                if (event.type == current.type) {
                    current
                } else {
                    current.copy(type = event.type, categoryId = null)
                }
            }
            is AddExpenseUiEvent.AmountChanged -> edit {
                it.copy(amountText = sanitizeAmountInput(event.value))
            }
            is AddExpenseUiEvent.CategorySelected -> edit { it.copy(categoryId = event.categoryId) }
            is AddExpenseUiEvent.AccountSelected -> edit { it.copy(accountId = event.accountId) }
            is AddExpenseUiEvent.ToAccountSelected -> edit { it.copy(toAccountId = event.accountId) }
            is AddExpenseUiEvent.DatePicked -> edit { state ->
                val time = state.billTime.toLocalTime().withSecond(0).withNano(0)
                state.copy(billTime = LocalDateTime.of(event.date, time))
            }
            is AddExpenseUiEvent.TimePicked -> edit { state ->
                state.copy(
                    billTime = state.billTime
                        .withHour(event.hour)
                        .withMinute(event.minute)
                        .withSecond(0)
                        .withNano(0),
                )
            }
            is AddExpenseUiEvent.RemarkChanged -> edit {
                it.copy(remark = event.value.take(LedgerLimits.REMARK))
            }
            is AddExpenseUiEvent.CreateCategory -> createCategory(event)
            AddExpenseUiEvent.Save -> save()
            AddExpenseUiEvent.ConfirmOverBudget -> {
                form.update { it.copy(acceptOverBudget = true, overBudgetPrompt = null) }
                save()
            }
            AddExpenseUiEvent.DismissOverBudget -> form.update {
                it.copy(overBudgetPrompt = null, acceptOverBudget = false)
            }
        }
    }

    private fun edit(block: (EntryForm) -> EntryForm) {
        form.update { current ->
            if (current.saving) {
                current
            } else {
                block(current).copy(issue = null, acceptOverBudget = false, overBudgetPrompt = null)
            }
        }
    }

    private fun createCategory(event: AddExpenseUiEvent.CreateCategory) {
        if (form.value.saving || creatingCategory) return
        val type = form.value.type.categoryType() ?: return
        val name = event.name.trim()
        if (name.isEmpty()) {
            _categoryCreated.tryEmit(CategoryCreateResult.Failed(AddBillIssue.BlankName))
            return
        }
        if (name.length > LedgerLimits.NAME) {
            _categoryCreated.tryEmit(CategoryCreateResult.Failed(AddBillIssue.NameTooLong))
            return
        }
        creatingCategory = true
        viewModelScope.launch {
            try {
                val id = categories.insertInFront(
                    name = name,
                    icon = event.iconKey,
                    color = event.colorHex,
                    type = type,
                )
                edit { it.copy(categoryId = id) }
                _categoryCreated.tryEmit(CategoryCreateResult.Saved(id))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: LedgerException) {
                _categoryCreated.tryEmit(CategoryCreateResult.Failed(exception.reason.toAddBillIssue()))
            } finally {
                creatingCategory = false
            }
        }
    }

    private fun save() {
        if (form.value.saving) return
        val snapshot = uiState.value
        if (!snapshot.ready) return
        val amount = parseMajorAmount(snapshot.amountText)
        if (amount == null || amount <= 0L) {
            form.update { it.copy(issue = AddBillIssue.InvalidAmount) }
            return
        }
        val accountId = snapshot.accountId
        if (accountId == null) {
            form.update { it.copy(issue = AddBillIssue.NoAccount) }
            return
        }
        when (snapshot.type) {
            BillType.Transfer -> saveTransfer(snapshot, amount, accountId)
            BillType.Expense, BillType.Income -> saveEntry(snapshot, amount, accountId)
        }
    }

    private fun saveEntry(snapshot: AddExpenseUiState, amount: Long, accountId: Long) {
        val categoryId = snapshot.categoryId
        if (categoryId == null) {
            form.update { it.copy(issue = AddBillIssue.NoCategory) }
            return
        }
        val entryType = when (snapshot.type) {
            BillType.Expense -> EntryType.Expense
            BillType.Income -> EntryType.Income
            BillType.Transfer -> return
        }
        val token = beginSave()
        viewModelScope.launch {
            try {
                val impacts = if (entryType == EntryType.Expense) {
                    budgets.impactsOfExpense(categoryId, snapshot.billTime, amount)
                } else {
                    emptyList()
                }
                if (token != saveToken) return@launch
                val over = impacts.filter { it.levelAfter() == BudgetLevel.Over }
                if (over.isNotEmpty() && !form.value.acceptOverBudget) {
                    form.update {
                        it.copy(
                            saving = false,
                            overBudgetPrompt = over.toPrompt(snapshot.categories),
                        )
                    }
                    return@launch
                }
                bills.insert(
                    EntryDraft(
                        type = entryType,
                        amount = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        billTime = snapshot.billTime,
                        remark = snapshot.remark,
                    ),
                )
                if (token != saveToken) return@launch
                if (entryType == EntryType.Expense && preferences.preferences.first().budgetNotify) {
                    notifier.publish(impacts, snapshot.categories.associate { it.id to it.name })
                }
                finishSave(token)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: LedgerException) {
                failSave(token, exception.reason.toAddBillIssue())
            }
        }
    }

    private fun saveTransfer(snapshot: AddExpenseUiState, amount: Long, accountId: Long) {
        val toAccountId = snapshot.toAccountId
        if (toAccountId == null) {
            form.update { it.copy(issue = AddBillIssue.NeedSecondAccount) }
            return
        }
        if (toAccountId == accountId) {
            form.update { it.copy(issue = AddBillIssue.SameAccount) }
            return
        }
        val token = beginSave()
        viewModelScope.launch {
            try {
                bills.insertTransfer(
                    TransferDraft(
                        amount = amount,
                        fromAccountId = accountId,
                        toAccountId = toAccountId,
                        billTime = snapshot.billTime,
                        remark = snapshot.remark,
                    ),
                )
                finishSave(token)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: LedgerException) {
                failSave(token, exception.reason.toAddBillIssue())
            }
        }
    }

    private fun beginSave(): Int {
        form.update { it.copy(saving = true, issue = null) }
        return saveToken
    }

    private fun finishSave(token: Int) {
        if (token != saveToken) return
        completed = true
        if (visible) {
            _finished.tryEmit(Unit)
        } else {
            completed = false
            form.value = freshForm()
        }
    }

    private fun failSave(token: Int, issue: AddBillIssue) {
        if (token != saveToken) return
        form.update { it.copy(saving = false, issue = issue) }
    }

    private fun freshForm(): EntryForm = EntryForm(
        type = initialType,
        billTime = LedgerTime.now().withSecond(0).withNano(0),
    )

    companion object {
        fun key(typeCode: Int): String = "add-expense-$typeCode"

        fun factory(typeCode: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("AddExpenseViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                val type = BillType.entries.firstOrNull { it.code == typeCode } ?: BillType.Expense
                AddExpenseViewModel(
                    initialType = type,
                    bills = entry.billRepository(),
                    budgets = entry.budgetRepository(),
                    accounts = entry.accountRepository(),
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                    notifier = BudgetNotifier(application),
                )
            }
        }
    }
}

private fun List<BudgetImpact>.toPrompt(categories: List<CategoryChoice>): OverBudgetPrompt {
    val names = mapNotNull { impact ->
        val categoryId = impact.budget.categoryId ?: return@mapNotNull null
        categories.firstOrNull { it.id == categoryId }?.name
    }
    return OverBudgetPrompt(
        includesGlobal = any { it.budget.categoryId == null },
        categoryNames = names,
    )
}

private fun BillType.categoryType(): CategoryType? = when (this) {
    BillType.Expense -> CategoryType.Expense
    BillType.Income -> CategoryType.Income
    BillType.Transfer -> null
}

private data class EntryForm(
    val type: BillType,
    val amountText: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val billTime: LocalDateTime,
    val remark: String = "",
    val issue: AddBillIssue? = null,
    val saving: Boolean = false,
    val overBudgetPrompt: OverBudgetPrompt? = null,
    val acceptOverBudget: Boolean = false,
)
