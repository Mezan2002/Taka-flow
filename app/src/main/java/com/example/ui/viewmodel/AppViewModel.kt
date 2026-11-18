package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppSettings
import com.example.data.model.BadgeDefinition
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.CheckinDay
import com.example.data.model.PeriodPreset
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.QuickFavorite
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRule
import com.example.data.model.ThemeMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionDraft
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.data.repository.FinanceRepository
import com.example.domain.BadgeEngine
import com.example.domain.CheckinState
import com.example.domain.CheckinStreakEngine
import com.example.domain.CurrencyFormat
import com.example.domain.DayRange
import com.example.domain.PeriodCalculator
import com.example.domain.RangeTotals
import com.example.notifications.NotificationPlanner
import com.example.services.BackupService
import com.example.services.PendingAgentAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class NewEntryTab {
    MANUAL,
    VOICE,
}

data class NewEntrySheetState(
    val visible: Boolean = false,
    val initialTab: NewEntryTab = NewEntryTab.MANUAL,
    val editingTransaction: Transaction? = null,
    val drafts: List<TransactionDraft> = emptyList(),
    val voiceTranscript: String = "",
)

data class TransferSheetState(
    val visible: Boolean = false,
    val editingTransaction: Transaction? = null,
)

data class UndoBannerState(
    val id: String,
    val message: String,
    val onUndo: () -> Unit,
)

data class MainUiState(
    val wallets: List<Wallet> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val plans: List<PurchasePlan> = emptyList(),
    val recurringRules: List<RecurringRule> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val checkins: List<CheckinDay> = emptyList(),
    val favorites: List<QuickFavorite> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val selectedWalletId: String? = null,
    val periodPreset: PeriodPreset = PeriodPreset.TODAY,
    val customPeriodRange: DayRange? = null,
    val checkinState: CheckinState = CheckinState(0, 0, 0, false, false, emptySet(), emptySet()),
    val isLocked: Boolean = false,
    val newEntrySheet: NewEntrySheetState = NewEntrySheetState(),
    val transferSheet: TransferSheetState = TransferSheetState(),
    val undoBanner: UndoBannerState? = null,
    val badgeToast: BadgeDefinition? = null,
    val statusMessage: String? = null,
) {
    val activePeriodRange: DayRange
        get() = if (periodPreset == PeriodPreset.CUSTOM && customPeriodRange != null) {
            customPeriodRange
        } else {
            PeriodCalculator.presetRange(periodPreset)
        }

    val activePeriodTotals: RangeTotals
        get() = PeriodCalculator.rangeTotals(transactions, activePeriodRange)

    val totalWalletBalance: Double
        get() = CurrencyFormat.round2(wallets.sumOf { it.balance })
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val settingsStore = SettingsDataStore(application)
    val repository = FinanceRepository(db, settingsStore)
    val backupService = BackupService(application, db, settingsStore)
    val notificationPlanner = NotificationPlanner(application)

    private val selectedWalletId = MutableStateFlow<String?>(null)
    private val periodPreset = MutableStateFlow(PeriodPreset.TODAY)
    private val customPeriodRange = MutableStateFlow<DayRange?>(null)
    private val isLocked = MutableStateFlow(false)
    private val newEntrySheet = MutableStateFlow(NewEntrySheetState())
    private val transferSheet = MutableStateFlow(TransferSheetState())
    private val undoBanner = MutableStateFlow<UndoBannerState?>(null)
    private val badgeToast = MutableStateFlow<BadgeDefinition?>(null)
    private val statusMessage = MutableStateFlow<String?>(null)

    private var undoJob: Job? = null
    private var badgeJob: Job? = null
    private var lockInitialized = false

    private val dataFlow = combine(
        repository.walletsFlow,
        repository.transactionsFlow,
        repository.categoriesFlow,
        repository.plansFlow,
        repository.recurringFlow,
    ) { wallets, txs, cats, plans, recurring ->
        DataBundle(wallets, txs, cats, plans, recurring)
    }

    private val secondaryDataFlow = combine(
        repository.budgetsFlow,
        repository.checkinsFlow,
        repository.favoritesFlow,
        settingsStore.settingsFlow,
    ) { budgets, checkins, favs, settings ->
        SecondaryBundle(budgets, checkins, favs, settings)
    }

    private val uiOverlayFlow = combine(
        selectedWalletId,
        periodPreset,
        customPeriodRange,
        isLocked,
        newEntrySheet,
        transferSheet,
        undoBanner,
        badgeToast,
        statusMessage,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        OverlayBundle(
            selectedWalletId = arr[0] as String?,
            periodPreset = arr[1] as PeriodPreset,
            customPeriodRange = arr[2] as DayRange?,
            isLocked = arr[3] as Boolean,
            newEntrySheet = arr[4] as NewEntrySheetState,
            transferSheet = arr[5] as TransferSheetState,
            undoBanner = arr[6] as UndoBannerState?,
            badgeToast = arr[7] as BadgeDefinition?,
            statusMessage = arr[8] as String?,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        dataFlow,
        secondaryDataFlow,
        uiOverlayFlow,
    ) { d, s, o ->
        val checkinState = CheckinStreakEngine.evaluate(d.transactions, s.checkins)
        MainUiState(
            wallets = d.wallets,
            transactions = d.transactions,
            categories = d.categories,
            plans = d.plans,
            recurringRules = d.recurringRules,
            budgets = s.budgets,
            checkins = s.checkins,
            favorites = s.favorites,
            settings = s.settings,
            selectedWalletId = o.selectedWalletId,
            periodPreset = o.periodPreset,
            customPeriodRange = o.customPeriodRange,
            checkinState = checkinState,
            isLocked = o.isLocked,
            newEntrySheet = o.newEntrySheet,
            transferSheet = o.transferSheet,
            undoBanner = o.undoBanner,
            badgeToast = o.badgeToast,
            statusMessage = o.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val settings = settingsStore.settingsFlow.first()
            if (!lockInitialized && settings.appLockEnabled) {
                isLocked.value = true
            }
            lockInitialized = true
            onAppForegrounded()
        }

        // Reactive watcher for badge unlocks, notification rescheduling, and budget alerts
        viewModelScope.launch {
            combine(dataFlow, secondaryDataFlow) { d, s -> d to s }.collect { (d, s) ->
                val checkinState = CheckinStreakEngine.evaluate(d.transactions, s.checkins)
                val newBadges = BadgeEngine.evaluateNewUnlocks(
                    alreadyUnlocked = s.settings.unlockedBadges,
                    transactions = d.transactions,
                    wallets = d.wallets,
                    plans = d.plans,
                    budgets = s.budgets,
                    recurringRules = d.recurringRules,
                    checkinState = checkinState,
                )
                if (newBadges.isNotEmpty()) {
                    settingsStore.recordBadgeUnlocks(newBadges.map { it.id })
                    showBadgeToast(newBadges.first())
                }
                notificationPlanner.syncSchedule(
                    settings = s.settings,
                    plans = d.plans,
                    recurringRules = d.recurringRules,
                    checkinState = checkinState,
                )
                val sentAlerts = settingsStore.sentBudgetAlertsFlow.first()
                notificationPlanner.checkBudgetAlerts(
                    settings = s.settings,
                    budgets = s.budgets,
                    transactions = d.transactions,
                    alreadySent = sentAlerts,
                    onMarkSent = { key -> settingsStore.markBudgetAlertSent(key) },
                )
            }
        }
    }

    fun onAppForegrounded() {
        viewModelScope.launch {
            repository.syncDueRecurringRules()
        }
    }

    fun unlockApp() {
        isLocked.value = false
    }

    fun lockAppIfEnabled() {
        if (uiState.value.settings.appLockEnabled) {
            isLocked.value = true
        }
    }

    fun selectWalletFilter(walletId: String?) {
        selectedWalletId.value = walletId
    }

    fun selectPeriodPreset(preset: PeriodPreset) {
        periodPreset.value = preset
    }

    fun setCustomPeriodRange(range: DayRange) {
        customPeriodRange.value = range
        periodPreset.value = PeriodPreset.CUSTOM
    }

    fun openNewEntryManual(editing: Transaction? = null) {
        newEntrySheet.value = NewEntrySheetState(
            visible = true,
            initialTab = NewEntryTab.MANUAL,
            editingTransaction = editing,
        )
    }

    fun openNewEntryVoice() {
        newEntrySheet.value = NewEntrySheetState(
            visible = true,
            initialTab = NewEntryTab.VOICE,
        )
    }

    fun openNewEntryWithDrafts(drafts: List<TransactionDraft>, transcript: String) {
        newEntrySheet.value = NewEntrySheetState(
            visible = true,
            initialTab = NewEntryTab.VOICE,
            drafts = drafts,
            voiceTranscript = transcript,
        )
    }

    fun closeNewEntry() {
        newEntrySheet.value = NewEntrySheetState(visible = false)
    }

    fun openTransferSheet(editing: Transaction? = null) {
        transferSheet.value = TransferSheetState(visible = true, editingTransaction = editing)
    }

    fun closeTransferSheet() {
        transferSheet.value = TransferSheetState(visible = false)
    }

    fun saveTransaction(
        editing: Transaction?,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        walletId: String,
        timestamp: Long,
        rawVoiceText: String? = null,
    ) {
        viewModelScope.launch {
            val cleanAmount = CurrencyFormat.round2(amount)
            val tx = Transaction(
                id = editing?.id ?: "tx_${UUID.randomUUID()}",
                type = type,
                amount = cleanAmount,
                category = category,
                description = description.trim().ifBlank { category },
                timestamp = timestamp,
                walletId = walletId,
                toWalletId = null,
                recurringId = editing?.recurringId,
                rawVoiceText = rawVoiceText ?: editing?.rawVoiceText,
            )
            if (editing != null) {
                repository.updateTransaction(editing, tx)
            } else {
                repository.addTransaction(tx)
            }
        }
    }

    fun saveDrafts(drafts: List<TransactionDraft>, walletId: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            drafts.forEach { d ->
                if (d.amount > 0) {
                    repository.addTransaction(
                        Transaction(
                            id = "tx_${UUID.randomUUID()}",
                            type = d.type,
                            amount = CurrencyFormat.round2(d.amount),
                            category = d.category,
                            description = d.description.ifBlank { d.category },
                            timestamp = timestamp,
                            walletId = walletId,
                            rawVoiceText = d.rawVoiceText.ifBlank { null },
                        )
                    )
                }
            }
        }
    }

    fun deleteTransactionWithUndo(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            showUndo("Deleted ${tx.description.ifBlank { tx.category }}") {
                viewModelScope.launch { repository.addTransaction(tx) }
            }
        }
    }

    fun saveTransfer(
        editing: Transaction?,
        fromWalletId: String,
        toWalletId: String,
        amount: Double,
        note: String,
        timestamp: Long,
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                id = editing?.id ?: "tx_${UUID.randomUUID()}",
                type = TransactionType.TRANSFER,
                amount = CurrencyFormat.round2(amount),
                category = "Transfer",
                description = note.trim().ifBlank { "Wallet Transfer" },
                timestamp = timestamp,
                walletId = fromWalletId,
                toWalletId = toWalletId,
            )
            if (editing != null) {
                repository.updateTransaction(editing, tx)
            } else {
                repository.addTransaction(tx)
            }
        }
    }

    fun saveWallet(wallet: Wallet) {
        viewModelScope.launch { repository.updateWallet(wallet) }
    }

    fun addWallet(name: String, initialBalance: Double, colorHex: String, iconKey: String) {
        viewModelScope.launch {
            val nextOrder = (uiState.value.wallets.maxOfOrNull { it.sortOrder } ?: 0) + 1
            repository.addWallet(
                Wallet(
                    id = "wallet_${UUID.randomUUID()}",
                    name = name.trim(),
                    balance = CurrencyFormat.round2(initialBalance),
                    colorHex = colorHex,
                    iconKey = iconKey,
                    isHidden = false,
                    sortOrder = nextOrder,
                )
            )
        }
    }

    fun toggleWalletHidden(wallet: Wallet) {
        viewModelScope.launch { repository.updateWallet(wallet.copy(isHidden = !wallet.isHidden)) }
    }

    fun deleteWalletWithUndo(wallet: Wallet) {
        if (uiState.value.wallets.size <= 1) {
            showStatus("At least one wallet must remain.")
            return
        }
        viewModelScope.launch {
            repository.deleteWallet(wallet.id)
            showUndo("Removed wallet ${wallet.name}") {
                viewModelScope.launch { repository.addWallet(wallet) }
            }
        }
    }

    fun savePlan(plan: PurchasePlan) {
        viewModelScope.launch { repository.savePlan(plan) }
    }

    fun deletePlanWithUndo(plan: PurchasePlan) {
        viewModelScope.launch {
            repository.deletePlan(plan.id)
            showUndo("Deleted ${plan.name.ifBlank { plan.personName }}") {
                viewModelScope.launch { repository.restorePlanWithRepayments(plan) }
            }
        }
    }

    fun completeItemPlan(plan: PurchasePlan, actualAmount: Double, walletId: String) {
        viewModelScope.launch { repository.completeItemPlan(plan, actualAmount, walletId) }
    }

    fun reopenPlan(plan: PurchasePlan) {
        viewModelScope.launch { repository.reopenPlan(plan) }
    }

    fun addLoanRepayment(plan: PurchasePlan, amount: Double, note: String) {
        viewModelScope.launch { repository.addLoanRepayment(plan, amount, note) }
    }

    fun removeLoanRepayment(plan: PurchasePlan, repaymentId: String) {
        viewModelScope.launch { repository.removeLoanRepayment(plan, repaymentId) }
    }

    fun settleLoan(plan: PurchasePlan) {
        viewModelScope.launch { repository.settleLoan(plan) }
    }

    fun saveRecurringRule(rule: RecurringRule) {
        viewModelScope.launch {
            repository.saveRecurringRule(rule)
            repository.syncDueRecurringRules()
        }
    }

    fun toggleRecurringActive(rule: RecurringRule) {
        viewModelScope.launch { repository.saveRecurringRule(rule.copy(active = !rule.active)) }
    }

    fun deleteRecurringWithUndo(rule: RecurringRule) {
        viewModelScope.launch {
            repository.deleteRecurringRule(rule.id)
            showUndo("Deleted recurring ${rule.description.ifBlank { rule.category }}") {
                viewModelScope.launch { repository.saveRecurringRule(rule) }
            }
        }
    }

    fun setBudget(categoryName: String?, limitAmount: Double) {
        viewModelScope.launch { repository.setBudget(categoryName, limitAmount) }
    }

    fun removeBudget(id: String) {
        viewModelScope.launch { repository.removeBudget(id) }
    }

    fun addCategory(name: String, type: TransactionType, iconKey: String = "category", colorHex: String = "#38BDF8") {
        viewModelScope.launch {
            repository.addCategory(
                Category(
                    id = "cat_${UUID.randomUUID()}",
                    name = name.trim(),
                    type = type,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    isCustom = true,
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category.id) }
    }

    fun markNothingSpentToday() {
        viewModelScope.launch { repository.markNothingSpentToday() }
    }

    fun undoCheckinToday() {
        viewModelScope.launch { repository.undoCheckinToday() }
    }

    fun saveFavorite(label: String, amount: Double, type: TransactionType, category: String, walletId: String) {
        viewModelScope.launch {
            val nextOrder = (uiState.value.favorites.maxOfOrNull { it.sortOrder } ?: 0) + 1
            repository.saveFavorite(
                QuickFavorite(
                    id = "fav_${UUID.randomUUID()}",
                    label = label.trim(),
                    amount = CurrencyFormat.round2(amount),
                    type = type,
                    category = category,
                    walletId = walletId,
                    sortOrder = nextOrder,
                )
            )
        }
    }

    fun deleteFavorite(id: String) {
        viewModelScope.launch { repository.deleteFavorite(id) }
    }

    fun triggerFavorite(fav: QuickFavorite) {
        val walletId = uiState.value.wallets.firstOrNull { it.id == fav.walletId }?.id
            ?: uiState.value.wallets.firstOrNull()?.id
            ?: "wallet_cash"
        saveTransaction(
            editing = null,
            type = fav.type,
            amount = fav.amount,
            category = fav.category,
            description = fav.label,
            walletId = walletId,
            timestamp = System.currentTimeMillis(),
        )
        showStatus("Logged ${fav.label} (${CurrencyFormat.formatAmount(fav.amount)})")
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settingsStore.setThemeMode(mode) } }
    fun toggleTotalBalanceHidden() {
        viewModelScope.launch { settingsStore.setTotalBalanceHidden(!uiState.value.settings.totalBalanceHidden) }
    }
    fun setAppLockEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setAppLockEnabled(enabled) } }
    fun setNotificationsEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) } }
    fun setDailyReminderEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setDailyReminderEnabled(enabled) } }
    fun setDailyReminderTime(h: Int, m: Int) { viewModelScope.launch { settingsStore.setDailyReminderTime(h, m) } }
    fun setRecurringReminderEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setRecurringReminderEnabled(enabled) } }
    fun setLoanReminderEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setLoanReminderEnabled(enabled) } }
    fun setPlanReminderEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setPlanReminderEnabled(enabled) } }
    fun setMorningReminderTime(h: Int, m: Int) { viewModelScope.launch { settingsStore.setMorningReminderTime(h, m) } }
    fun setHideNotificationAmounts(hide: Boolean) { viewModelScope.launch { settingsStore.setHideNotificationAmounts(hide) } }
    fun setBudgetAlertsEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setBudgetAlertsEnabled(enabled) } }
    fun setAiAgentEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setAiAgentEnabled(enabled) } }
    fun setAiVoiceReplyEnabled(enabled: Boolean) { viewModelScope.launch { settingsStore.setAiVoiceReplyEnabled(enabled) } }
    fun setAiTokenLimit(limit: Long) { viewModelScope.launch { settingsStore.setAiTokenLimit(limit) } }
    fun resetAiUsage() { viewModelScope.launch { settingsStore.resetAiUsage() } }
    fun sendTestNotification() {
        notificationPlanner.scheduleTestNotification()
        showStatus("Test notification scheduled in 5 seconds.")
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupService.exportToUri(uri)
                .onSuccess { count -> showStatus("Backup exported ($count entries).") }
                .onFailure { e -> showStatus("Export failed: ${e.localizedMessage}") }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            backupService.restoreFromUri(uri)
                .onSuccess { msg -> showStatus(msg) }
                .onFailure { e -> showStatus("Restore blocked: ${e.localizedMessage}") }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            showStatus("All data cleared.")
        }
    }

    fun executeAgentAction(action: PendingAgentAction): String {
        val state = uiState.value
        val wallets = state.wallets
        fun resolveWallet(name: String): Wallet? =
            wallets.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: wallets.firstOrNull()

        return when (action) {
            is PendingAgentAction.AddTransaction -> {
                val w = resolveWallet(action.walletName) ?: return "No wallet available."
                saveTransaction(
                    editing = null,
                    type = action.type,
                    amount = action.amount,
                    category = action.category,
                    description = action.description,
                    walletId = w.id,
                    timestamp = System.currentTimeMillis(),
                )
                "Added ${action.type.name.lowercase()} of ${CurrencyFormat.formatAmount(action.amount)} (${action.category}) to ${w.name}."
            }
            is PendingAgentAction.DeleteTransaction -> {
                val tx = state.transactions.firstOrNull { it.id == action.transactionId }
                    ?: return "Transaction not found."
                deleteTransactionWithUndo(tx)
                "Deleted ${tx.description} (${CurrencyFormat.formatAmount(tx.amount)})."
            }
            is PendingAgentAction.TransferWallets -> {
                val from = resolveWallet(action.fromWalletName) ?: return "Source wallet not found."
                val to = wallets.firstOrNull { it.name.equals(action.toWalletName, ignoreCase = true) }
                    ?: wallets.firstOrNull { it.id != from.id }
                    ?: return "Destination wallet not found."
                saveTransfer(null, from.id, to.id, action.amount, action.note, System.currentTimeMillis())
                "Transferred ${CurrencyFormat.formatAmount(action.amount)} from ${from.name} to ${to.name}."
            }
            is PendingAgentAction.SetBudget -> {
                setBudget(action.categoryName, action.limitAmount)
                "Set ${action.categoryName ?: "Monthly"} budget to ${CurrencyFormat.formatAmount(action.limitAmount)}."
            }
            is PendingAgentAction.AddCategory -> {
                addCategory(action.name, action.type)
                "Added category ${action.name} (${action.type.name.lowercase()})."
            }
            is PendingAgentAction.DeleteCategory -> {
                val cat = state.categories.firstOrNull { it.name.equals(action.name, ignoreCase = true) }
                    ?: return "Category '${action.name}' not found."
                deleteCategory(cat)
                "Deleted category ${cat.name}."
            }
            is PendingAgentAction.AddRecurring -> {
                val w = resolveWallet(action.walletName) ?: return "Wallet not found."
                saveRecurringRule(
                    RecurringRule(
                        id = "rec_${UUID.randomUUID()}",
                        type = action.type,
                        amount = CurrencyFormat.round2(action.amount),
                        category = action.category,
                        description = action.description,
                        walletId = w.id,
                        frequency = action.frequency,
                        startDate = System.currentTimeMillis(),
                    )
                )
                "Added ${action.frequency.label} recurring rule for ${action.description} (${CurrencyFormat.formatAmount(action.amount)})."
            }
            is PendingAgentAction.ToggleRecurring -> {
                val rule = state.recurringRules.firstOrNull { it.id == action.ruleId }
                    ?: return "Recurring rule not found."
                saveRecurringRule(rule.copy(active = action.active))
                "${if (action.active) "Resumed" else "Paused"} recurring rule '${rule.description}'."
            }
            is PendingAgentAction.DeleteRecurring -> {
                val rule = state.recurringRules.firstOrNull { it.id == action.ruleId }
                    ?: return "Recurring rule not found."
                deleteRecurringWithUndo(rule)
                "Deleted recurring rule '${rule.description}'."
            }
            is PendingAgentAction.AddPlan -> {
                savePlan(
                    PurchasePlan(
                        id = "plan_${UUID.randomUUID()}",
                        kind = PlanKind.ITEM,
                        name = action.name,
                        expectedAmount = CurrencyFormat.round2(action.expectedAmount),
                        category = action.category,
                        note = action.note,
                    )
                )
                "Added purchase plan '${action.name}' (${CurrencyFormat.formatAmount(action.expectedAmount)})."
            }
            is PendingAgentAction.CompletePlan -> {
                val plan = state.plans.firstOrNull { it.id == action.planId }
                    ?: return "Purchase plan not found."
                val w = resolveWallet(action.walletName) ?: return "Wallet not found."
                completeItemPlan(plan, action.actualAmount, w.id)
                "Marked '${plan.name}' bought for ${CurrencyFormat.formatAmount(action.actualAmount)} from ${w.name}."
            }
            is PendingAgentAction.AddWallet -> {
                addWallet(action.name, action.initialBalance, "#38BDF8", "wallet")
                "Created wallet '${action.name}' with ${CurrencyFormat.formatAmount(action.initialBalance)}."
            }
        }
    }

    fun dismissUndo() {
        undoBanner.value = null
    }

    fun dismissStatus() {
        statusMessage.value = null
    }

    fun showStatus(msg: String) {
        statusMessage.value = msg
    }

    private fun showUndo(message: String, onUndo: () -> Unit) {
        undoJob?.cancel()
        undoBanner.value = UndoBannerState(UUID.randomUUID().toString(), message, onUndo)
        undoJob = viewModelScope.launch {
            delay(4_000L)
            undoBanner.value = null
        }
    }

    private fun showBadgeToast(badge: BadgeDefinition) {
        badgeJob?.cancel()
        badgeToast.value = badge
        badgeJob = viewModelScope.launch {
            delay(3_500L)
            badgeToast.value = null
        }
    }

    private data class DataBundle(
        val wallets: List<Wallet>,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val plans: List<PurchasePlan>,
        val recurringRules: List<RecurringRule>,
    )

    private data class SecondaryBundle(
        val budgets: List<Budget>,
        val checkins: List<CheckinDay>,
        val favorites: List<QuickFavorite>,
        val settings: AppSettings,
    )

    private data class OverlayBundle(
        val selectedWalletId: String?,
        val periodPreset: PeriodPreset,
        val customPeriodRange: DayRange?,
        val isLocked: Boolean,
        val newEntrySheet: NewEntrySheetState,
        val transferSheet: TransferSheetState,
        val undoBanner: UndoBannerState?,
        val badgeToast: BadgeDefinition?,
        val statusMessage: String?,
    )
}
