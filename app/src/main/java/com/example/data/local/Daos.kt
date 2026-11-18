package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, name ASC")
    suspend fun getAll(): List<WalletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(wallets: List<WalletEntity>)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(txs: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE transactions SET walletId = :toWalletId WHERE walletId = :fromWalletId")
    suspend fun reassignWallet(fromWalletId: String, toWalletId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isCustom ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM purchase_plans ORDER BY createdAt DESC")
    fun observePlans(): Flow<List<PurchasePlanEntity>>

    @Query("SELECT * FROM loan_repayments ORDER BY paidAt DESC")
    fun observeRepayments(): Flow<List<LoanRepaymentEntity>>

    @Query("SELECT * FROM purchase_plans")
    suspend fun getAllPlans(): List<PurchasePlanEntity>

    @Query("SELECT * FROM loan_repayments")
    suspend fun getAllRepayments(): List<LoanRepaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: PurchasePlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlans(plans: List<PurchasePlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRepayment(repayment: LoanRepaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRepayments(repayments: List<LoanRepaymentEntity>)

    @Query("DELETE FROM purchase_plans WHERE id = :id")
    suspend fun deletePlanById(id: String)

    @Query("DELETE FROM loan_repayments WHERE id = :id")
    suspend fun deleteRepaymentById(id: String)

    @Query("DELETE FROM loan_repayments")
    suspend fun deleteAllRepayments()

    @Query("DELETE FROM purchase_plans")
    suspend fun deleteAllPlans()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAll(): List<RecurringRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<RecurringRuleEntity>)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface CheckinDao {
    @Query("SELECT * FROM checkins ORDER BY dateKey DESC")
    fun observeAll(): Flow<List<CheckinDayEntity>>

    @Query("SELECT * FROM checkins ORDER BY dateKey DESC")
    suspend fun getAll(): List<CheckinDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: CheckinDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<CheckinDayEntity>)

    @Query("DELETE FROM checkins WHERE dateKey = :dateKey")
    suspend fun deleteByKey(dateKey: String)

    @Query("DELETE FROM checkins")
    suspend fun deleteAll()
}

@Dao
interface QuickFavoriteDao {
    @Query("SELECT * FROM quick_favorites ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<QuickFavoriteEntity>>

    @Query("SELECT * FROM quick_favorites ORDER BY sortOrder ASC")
    suspend fun getAll(): List<QuickFavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fav: QuickFavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(favs: List<QuickFavoriteEntity>)

    @Query("DELETE FROM quick_favorites WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM quick_favorites")
    suspend fun deleteAll()
}
