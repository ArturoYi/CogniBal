package com.cyr.cognobal.screen.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyr.cognobal.data.database.LedgerTime
import com.cyr.cognobal.data.database.entity.Account
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.data.database.model.CategoryType
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.data.repository.AccountRepository
import com.cyr.cognobal.data.repository.BillRepository
import com.cyr.cognobal.data.repository.BudgetImpact
import com.cyr.cognobal.data.repository.BudgetLevel
import com.cyr.cognobal.data.repository.BudgetRepository
import com.cyr.cognobal.data.repository.CategoryRepository
import com.cyr.cognobal.data.repository.EntryDraft
import com.cyr.cognobal.data.repository.EntryType
import com.cyr.cognobal.data.repository.LedgerException
import com.cyr.cognobal.data.repository.LedgerLimits
import com.cyr.cognobal.data.repository.TransferDraft
import com.cyr.cognobal.di.LedgerEntryPoint
import com.cyr.cognobal.screen.feature.bill.tool.minorToInput
import com.cyr.cognobal.screen.feature.bill.tool.parseMajorAmount
import com.cyr.cognobal.screen.feature.expense.model.AddBillIssue
import com.cyr.cognobal.screen.feature.expense.model.NewCategoryColorHex
import com.cyr.cognobal.screen.feature.expense.tool.resolveAccountId
import com.cyr.cognobal.screen.feature.expense.tool.resolveCounterpartyId
import com.cyr.cognobal.screen.feature.expense.tool.sanitizeAmountInput
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.cyr.cognobal.screen.feature.expense.state.AccountChoice
import com.cyr.cognobal.screen.feature.expense.state.AddExpenseUiEvent
import com.cyr.cognobal.screen.feature.expense.state.AddExpenseUiState
import com.cyr.cognobal.screen.feature.expense.state.CategoryChoice
import com.cyr.cognobal.screen.feature.expense.state.CategoryCreateResult
import com.cyr.cognobal.screen.feature.expense.state.OverBudgetPrompt
import com.cyr.cognobal.screen.feature.settings.budget.BudgetNotifier
import com.cyr.cognobal.screen.feature.expense.model.toAddBillIssue
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
    private val editingBillId: Long?,
    private val bills: BillRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val preferences: AppPreferencesRepository,
    private val notifier: BudgetNotifier,
    accounts: AccountRepository,
) : ViewModel() {
    private val openedAt = LedgerTime.now().withSecond(0).withNano(0)
    private val form = MutableStateFlow(
        EntryForm(
            type = initialType,
            billTime = openedAt,
            editing = editingBillId != null,
        ),
    )
    private val loaded = MutableStateFlow(editingBillId == null)

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
        val billId = editingBillId
        if (billId != null) {
            viewModelScope.launch { loadExisting(billId) }
        }
    }

    val uiState: StateFlow<AddExpenseUiState> = combine(
        form,
        accounts.observeAll(),
        categories.observeAll(),
        preferences.preferences,
        loaded,
    ) { draft, accountRows, categoriesRows, prefs, isLoaded ->
        val visibleAccounts = accountRows.filter { account ->
            account.isEnable ||
                (draft.editing && (account.id == draft.accountId || account.id == draft.toAccountId))
        }
        val orderedAccounts = visibleAccounts.sortedWith(
            compareByDescending<Account> { it.isDefault }.thenBy { it.id },
        )
        val accountId = resolveAccountId(draft.accountId, orderedAccounts)
        val categoryType = draft.type.categoryType()
        val matching = if (categoryType == null) {
            emptyList()
        } else {
            categoriesRows.filter { category ->
                category.type == categoryType &&
                    (category.isEnable || (draft.editing && category.id == draft.categoryId))
            }
        }
        val resolvedCategoryId = categoryType?.let { type ->
            draft.categoryId?.takeIf { id ->
                categoriesRows.any { category ->
                    category.id == id &&
                        category.type == type &&
                        (category.isEnable || draft.editing)
                }
            }
        }
        AddExpenseUiState(
            ready = isLoaded,
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
            categoryId = resolvedCategoryId,
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
            editing = draft.editing,
            missing = draft.missing,
            confirmSave = draft.saveConfirmed && !draft.saving && draft.overBudgetPrompt == null,
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
            val billId = editingBillId
            if (billId != null) {
                loaded.value = false
                viewModelScope.launch { loadExisting(billId) }
            }
        }
    }

    fun onEvent(event: AddExpenseUiEvent) {
        when (event) {
            is AddExpenseUiEvent.TypeSelected -> edit { current ->
                if (current.editing || event.type == current.type) {
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
            AddExpenseUiEvent.ConfirmSave -> save()
            AddExpenseUiEvent.DismissSaveConfirm -> form.update { it.copy(saveConfirmed = false) }
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
                    color = NewCategoryColorHex,
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
        if (form.value.saveBlocked) {
            form.update { it.copy(issue = AddBillIssue.Generic) }
            return
        }
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
                if (!awaitSaveConfirm(token)) return@launch
                val draft = EntryDraft(
                    type = entryType,
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId,
                    billTime = snapshot.billTime,
                    remark = snapshot.remark,
                )
                val billId = editingBillId
                if (billId == null) {
                    bills.insert(draft)
                } else {
                    bills.update(billId, draft)
                }
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
                if (!awaitSaveConfirm(token)) return@launch
                val draft = TransferDraft(
                    amount = amount,
                    fromAccountId = accountId,
                    toAccountId = toAccountId,
                    billTime = snapshot.billTime,
                    remark = snapshot.remark,
                )
                val billId = editingBillId
                if (billId == null) {
                    bills.insertTransfer(draft)
                } else {
                    bills.updateTransfer(billId, draft)
                }
                finishSave(token)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: LedgerException) {
                failSave(token, exception.reason.toAddBillIssue())
            }
        }
    }

    /** 改已有账单时，先等用户确认覆盖。返回 false 表示这次先停住。 */
    private fun awaitSaveConfirm(token: Int): Boolean {
        if (token != saveToken) return false
        if (editingBillId == null || form.value.saveConfirmed) return true
        form.update { it.copy(saving = false, saveConfirmed = true) }
        return false
    }

    private suspend fun loadExisting(id: Long) {
        try {
            val bill = bills.getById(id)
            if (bill == null) {
                form.value = freshForm().copy(missing = true)
                loaded.value = true
                return
            }
            val peer = if (bill.type == BillType.Transfer) {
                try {
                    bills.transferPeer(bill.id)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: LedgerException) {
                    null
                }
            } else {
                null
            }
            val outgoing = when {
                bill.type != BillType.Transfer -> null
                bill.amount < 0 -> bill
                else -> peer
            }
            val incoming = when {
                bill.type != BillType.Transfer -> null
                bill.amount > 0 -> bill
                else -> peer
            }
            val broken = bill.type == BillType.Transfer &&
                (outgoing == null || incoming == null || outgoing.amount == Long.MIN_VALUE)
            val minor = when {
                broken -> 0L
                bill.type == BillType.Transfer -> -(outgoing?.amount ?: 0L)
                else -> bill.amount
            }
            form.value = EntryForm(
                type = bill.type,
                amountText = minorToInput(minor),
                categoryId = bill.categoryId,
                accountId = if (bill.type == BillType.Transfer) outgoing?.accountId else bill.accountId,
                toAccountId = incoming?.accountId,
                billTime = bill.billTime,
                remark = bill.remark.orEmpty(),
                editing = true,
                issue = if (broken) AddBillIssue.Generic else null,
                saveBlocked = broken,
            )
            loaded.value = true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: LedgerException) {
            form.value = freshForm().copy(missing = true)
            loaded.value = true
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
        form.update { it.copy(saving = false, issue = issue, saveConfirmed = false) }
    }

    private fun freshForm(): EntryForm = EntryForm(
        type = initialType,
        billTime = LedgerTime.now().withSecond(0).withNano(0),
        editing = editingBillId != null,
    )

    companion object {
        fun key(typeCode: Int, billId: Long? = null): String =
            if (billId == null) "add-expense-$typeCode" else "add-expense-$typeCode-$billId"

        fun factory(typeCode: Int, billId: Long? = null): ViewModelProvider.Factory = viewModelFactory {
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
                    editingBillId = billId,
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
    val editing: Boolean = false,
    val missing: Boolean = false,
    val saveConfirmed: Boolean = false,
    val saveBlocked: Boolean = false,
)
