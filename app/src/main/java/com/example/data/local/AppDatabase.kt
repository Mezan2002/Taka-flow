package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TransactionType

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        PurchasePlanEntity::class,
        LoanRepaymentEntity::class,
        RecurringRuleEntity::class,
        BudgetEntity::class,
        CheckinDayEntity::class,
        QuickFavoriteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun planDao(): PlanDao
    abstract fun recurringDao(): RecurringDao
    abstract fun budgetDao(): BudgetDao
    abstract fun checkinDao(): CheckinDao
    abstract fun quickFavoriteDao(): QuickFavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "takaflow_db",
                ).build().also { INSTANCE = it }
            }
        }

        val defaultCategories: List<CategoryEntity> = listOf(
            CategoryEntity("cat_food", "Food", TransactionType.EXPENSE.name, "restaurant", "#FB7185", false),
            CategoryEntity("cat_transport", "Transport", TransactionType.EXPENSE.name, "directions_car", "#38BDF8", false),
            CategoryEntity("cat_shopping", "Shopping", TransactionType.EXPENSE.name, "shopping_bag", "#A78BFA", false),
            CategoryEntity("cat_bills", "Bills", TransactionType.EXPENSE.name, "receipt_long", "#FBBF24", false),
            CategoryEntity("cat_health", "Health", TransactionType.EXPENSE.name, "medical_services", "#34D399", false),
            CategoryEntity("cat_entertainment", "Entertainment", TransactionType.EXPENSE.name, "movie", "#F472B6", false),
            CategoryEntity("cat_education", "Education", TransactionType.EXPENSE.name, "school", "#60A5FA", false),
            CategoryEntity("cat_other_exp", "Other", TransactionType.EXPENSE.name, "category", "#94A3B8", false),
            CategoryEntity("cat_salary", "Salary", TransactionType.INCOME.name, "payments", "#34D399", false),
            CategoryEntity("cat_freelance", "Freelance", TransactionType.INCOME.name, "laptop_mac", "#38BDF8", false),
            CategoryEntity("cat_bonus", "Bonus", TransactionType.INCOME.name, "card_giftcard", "#FBBF24", false),
            CategoryEntity("cat_other_inc", "Other Income", TransactionType.INCOME.name, "account_balance_wallet", "#2DD4BF", false),
        )

        val defaultWallet = WalletEntity(
            id = "wallet_cash",
            name = "Cash",
            balance = 0.0,
            colorHex = "#FBBF24",
            iconKey = "cash",
            isHidden = false,
            sortOrder = 0,
        )

        val defaultFavorites: List<QuickFavoriteEntity> = listOf(
            QuickFavoriteEntity("fav_tea", "Tea", 20.0, TransactionType.EXPENSE.name, "Food", "wallet_cash", 0),
            QuickFavoriteEntity("fav_rickshaw", "Rickshaw", 50.0, TransactionType.EXPENSE.name, "Transport", "wallet_cash", 1),
            QuickFavoriteEntity("fav_lunch", "Lunch", 150.0, TransactionType.EXPENSE.name, "Food", "wallet_cash", 2),
        )
    }
}
