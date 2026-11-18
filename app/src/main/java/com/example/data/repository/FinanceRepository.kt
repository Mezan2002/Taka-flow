package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.toEntity
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.CheckinDay
import com.example.data.model.CheckinSource
import com.example.data.model.LoanRepayment
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.QuickFavorite
import com.example.data.model.RecurringRule
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.LoanCalculator
import com.example.domain.RecurringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class FinanceRepository(
    private val db: AppDatabase,
    private val settingsStore: SettingsDataStore,
) {
    private val walletDao = db.walletDao()
    private val txDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val planDao = db.planDao()
    private val recurringDao = db.recurringDao()
    private val budgetDao = db.budgetDao()
    private val checkinDao = db.checkinDao()
    private val favoriteDao = db.quickFavoriteDao()

    val walletsFlow: Flow<List<Wallet>> = walletDao.observeAll().map { list -> list.map { it.toDomain() } }
    val transactionsFlow: Flow<List<Transaction>> = txDao.observeAll().map { list -> list.map { it.toDomain() } }
    val categoriesFlow: Flow<List<Category>> = categoryDao.observeAll().map { list -> list.map { it.toDomain() } }
    val recurringFlow: Flow<List<RecurringRule>> = recurringDao.observeAll().map { list -> list.map { it.toDomain() } }
    val budgetsFlow: Flow<List<Budget>> = budgetDao.observeAll().map { list -> list.map { it.toDomain() } }
    val checkinsFlow: Flow<List<CheckinDay>> = checkinDao.observeAll().map { list -> list.map { it.toDomain() } }
    val favoritesFlow: Flow<List<QuickFavorite>> = favoriteDao.observeAll().map { list -> list.map { it.toDomain() } }

    val plansFlow: Flow<List<PurchasePlan>> = combine(
        planDao.observePlans(),
        planDao.observeRepayments(),
    ) { plans, repayments ->
        val grouped = repayments.map { it.toDomain() }.groupBy { it.planId }
        plans.map { entity -> entity.toDomain(grouped[entity.id].orEmpty()) }
    }

    suspend fun ensureSeeded() {
        db.withTransaction {
            if (walletDao.getAll().isEmpty()) {
                walletDao.upsert(AppDatabase.defaultWallet)
            }
            if (categoryDao.getAll().isEmpty()) {
                categoryDao.upsertAll(AppDatabase.defaultCategories)
            }
            if (favoriteDao.getAll().isEmpty()) {
                favoriteDao.upsertAll(AppDatabase.defaultFavorites)
            }
        }
    }

    suspend fun addTransaction(tx: Transaction) {
        db.withTransaction {
            txDao.upsert(tx.toEntity())
            applyWalletBalanceEffect(tx, multiplier = 1.0)
        }
    }

    suspend fun updateTransaction(oldTx: Transaction, newTx: Transaction) {
        db.withTransaction {
            applyWalletBalanceEffect(oldTx, multiplier = -1.0)
            txDao.upsert(newTx.toEntity())
            applyWalletBalanceEffect(newTx, multiplier = 1.0)
        }
    }

    suspend fun deleteTransaction(tx: Transaction) {
        db.withTransaction {
            txDao.deleteById(tx.id)
            applyWalletBalanceEffect(tx, multiplier = -1.0)
        }
    }

    private suspend fun applyWalletBalanceEffect(tx: Transaction, multiplier: Double) {
        val wallets = walletDao.getAll().associateBy { it.id }
        when (tx.type) {
            TransactionType.INCOME -> {
                wallets[tx.walletId]?.let { w ->
                    walletDao.upsert(w.copy(balance = CurrencyFormat.round2(w.balance + tx.amount * multiplier)))
                }
            }
            TransactionType.EXPENSE -> {
                wallets[tx.walletId]?.let { w ->
                    walletDao.upsert(w.copy(balance = CurrencyFormat.round2(w.balance - tx.amount * multiplier)))
                }
            }
            TransactionType.TRANSFER -> {
                wallets[tx.walletId]?.let { from ->
                    walletDao.upsert(from.copy(balance = CurrencyFormat.round2(from.balance - tx.amount * multiplier)))
                }
                tx.toWalletId?.let { toId ->
                    wallets[toId]?.let { to ->
                        walletDao.upsert(to.copy(balance = CurrencyFormat.round2(to.balance + tx.amount * multiplier)))
                    }
                }
            }
        }
    }

    suspend fun addWallet(wallet: Wallet) = walletDao.upsert(wallet.toEntity())
    suspend fun updateWallet(wallet: Wallet) = walletDao.upsert(wallet.toEntity())

    suspend fun deleteWallet(walletId: String) {
        db.withTransaction {
            val allWallets = walletDao.getAll()
            val fallback = allWallets.firstOrNull { it.id != walletId }
            if (fallback != null) {
                txDao.reassignWallet(walletId, fallback.id)
            }
            walletDao.deleteById(walletId)
        }
    }

    suspend fun addCategory(category: Category) = categoryDao.upsert(category.toEntity())
    suspend fun deleteCategory(id: String) = categoryDao.deleteById(id)

    suspend fun savePlan(plan: PurchasePlan) {
        db.withTransaction {
            // Auto-settle if lowered principal <= paid sum for loans
            val adjusted = if (LoanCalculator.isLoan(plan)) {
                val paid = LoanCalculator.loanPaid(plan)
                if (paid >= plan.expectedAmount && plan.expectedAmount > 0.0) {
                    plan.copy(completed = true, completedAt = plan.completedAt ?: System.currentTimeMillis())
                } else {
                    plan
                }
            } else plan
            planDao.upsertPlan(adjusted.toEntity())
        }
    }

    suspend fun restorePlanWithRepayments(plan: PurchasePlan) {
        db.withTransaction {
            planDao.upsertPlan(plan.toEntity())
            if (plan.repayments.isNotEmpty()) {
                planDao.upsertRepayments(plan.repayments.map { it.toEntity() })
            }
        }
    }

    suspend fun deletePlan(id: String) = planDao.deletePlanById(id)

    suspend fun completeItemPlan(plan: PurchasePlan, actualAmount: Double, walletId: String) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            planDao.upsertPlan(
                plan.copy(
                    completed = true,
                    completedAt = now,
                    actualAmount = actualAmount,
                    walletId = walletId,
                ).toEntity()
            )
            if (actualAmount > 0.0) {
                val tx = Transaction(
                    id = "tx_${UUID.randomUUID()}",
                    type = TransactionType.EXPENSE,
                    amount = actualAmount,
                    category = plan.category,
                    description = plan.name,
                    timestamp = now,
                    walletId = walletId,
                )
                txDao.upsert(tx.toEntity())
                applyWalletBalanceEffect(tx, multiplier = 1.0)
            }
        }
    }

    suspend fun reopenPlan(plan: PurchasePlan) {
        planDao.upsertPlan(plan.copy(completed = false, completedAt = null).toEntity())
    }

    /**
     * Repayments do NOT touch wallets or transactions; loans are a separate ledger.
     */
    suspend fun addLoanRepayment(plan: PurchasePlan, amount: Double, note: String) {
        db.withTransaction {
            val repayment = LoanRepayment(
                id = "rep_${UUID.randomUUID()}",
                planId = plan.id,
                amount = CurrencyFormat.round2(amount),
                note = note.trim(),
                paidAt = System.currentTimeMillis(),
            )
            planDao.upsertRepayment(repayment.toEntity())
            val updatedRepayments = plan.repayments + repayment
            val newPaid = CurrencyFormat.round2(updatedRepayments.sumOf { it.amount })
            val isSettled = newPaid >= plan.expectedAmount
            planDao.upsertPlan(
                plan.copy(
                    completed = isSettled,
                    completedAt = if (isSettled) System.currentTimeMillis() else null,
                ).toEntity()
            )
        }
    }

    suspend fun removeLoanRepayment(plan: PurchasePlan, repaymentId: String) {
        db.withTransaction {
            planDao.deleteRepaymentById(repaymentId)
            val remaining = plan.repayments.filterNot { it.id == repaymentId }
            val newPaid = CurrencyFormat.round2(remaining.sumOf { it.amount })
            val stillSettled = newPaid >= plan.expectedAmount && plan.expectedAmount > 0.0
            planDao.upsertPlan(
                plan.copy(
                    completed = stillSettled,
                    completedAt = if (stillSettled) plan.completedAt else null,
                ).toEntity()
            )
        }
    }

    suspend fun settleLoan(plan: PurchasePlan) {
        val remaining = LoanCalculator.loanRemaining(plan)
        db.withTransaction {
            if (remaining > 0.0) {
                val repayment = LoanRepayment(
                    id = "rep_${UUID.randomUUID()}",
                    planId = plan.id,
                    amount = remaining,
                    note = "Full settlement",
                    paidAt = System.currentTimeMillis(),
                )
                planDao.upsertRepayment(repayment.toEntity())
            }
            planDao.upsertPlan(
                plan.copy(completed = true, completedAt = System.currentTimeMillis()).toEntity()
            )
        }
    }

    suspend fun saveRecurringRule(rule: RecurringRule) = recurringDao.upsert(rule.toEntity())
    suspend fun deleteRecurringRule(id: String) = recurringDao.deleteById(id)

    suspend fun syncDueRecurringRules(nowMillis: Long = System.currentTimeMillis()) {
        val rules = recurringDao.getAll().map { it.toDomain() }
        if (rules.isEmpty()) return
        db.withTransaction {
            for (rule in rules) {
                val dueDates = RecurringEngine.getDueOccurrences(rule, nowMillis)
                if (dueDates.isNotEmpty()) {
                    for (due in dueDates) {
                        val tx = Transaction(
                            id = "tx_rec_${rule.id}_${DateUtils.toKey(due)}",
                            type = rule.type,
                            amount = rule.amount,
                            category = rule.category,
                            description = rule.description,
                            timestamp = due + 9 * 3_600_000L, // 9 AM local offset
                            walletId = rule.walletId,
                            recurringId = rule.id,
                        )
                        txDao.upsert(tx.toEntity())
                        applyWalletBalanceEffect(tx, multiplier = 1.0)
                    }
                    recurringDao.upsert(rule.copy(lastGeneratedDate = dueDates.last()).toEntity())
                }
            }
        }
    }

    suspend fun setBudget(categoryName: String?, limitAmount: Double) {
        val id = if (categoryName == null) "MONTHLY" else "CAT_${categoryName.uppercase()}"
        budgetDao.upsert(Budget(id, categoryName, CurrencyFormat.round2(limitAmount)).toEntity())
    }

    suspend fun removeBudget(id: String) = budgetDao.deleteById(id)

    suspend fun markNothingSpentToday(nowMillis: Long = System.currentTimeMillis()) {
        val key = DateUtils.toKey(nowMillis)
        checkinDao.upsert(CheckinDay(key, CheckinSource.NO_SPEND, nowMillis).toEntity())
    }

    suspend fun undoCheckinToday(nowMillis: Long = System.currentTimeMillis()) {
        checkinDao.deleteByKey(DateUtils.toKey(nowMillis))
    }

    suspend fun saveFavorite(fav: QuickFavorite) = favoriteDao.upsert(fav.toEntity())
    suspend fun deleteFavorite(id: String) = favoriteDao.deleteById(id)

    suspend fun clearAllData() {
        db.withTransaction {
            txDao.deleteAll()
            planDao.deleteAllRepayments()
            planDao.deleteAllPlans()
            recurringDao.deleteAll()
            budgetDao.deleteAll()
            checkinDao.deleteAll()
            walletDao.deleteAll()
            categoryDao.deleteAll()
            favoriteDao.deleteAll()
            walletDao.upsert(AppDatabase.defaultWallet)
            categoryDao.upsertAll(AppDatabase.defaultCategories)
            favoriteDao.upsertAll(AppDatabase.defaultFavorites)
        }
        settingsStore.clearAll()
    }
}
