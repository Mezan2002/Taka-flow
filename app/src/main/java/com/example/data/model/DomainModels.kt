package com.example.data.model

const val CURRENCY = "TAKA"
const val CURRENCY_SYMBOL = "৳"

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
}

enum class PlanKind {
    ITEM,
    LENT,
    BORROWED,
}

enum class LoanStatus {
    SETTLED,
    OVERDUE,
    DUE_SOON,
    ON_TRACK,
}

enum class RecurringFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class PeriodPreset(val label: String) {
    TODAY("TODAY"),
    YESTERDAY("YESTERDAY"),
    LAST_7_DAYS("7 DAYS"),
    THIS_MONTH("THIS MONTH"),
    LAST_MONTH("LAST MONTH"),
    CUSTOM("CUSTOM"),
}

enum class CheckinSource {
    TRANSACTION,
    NO_SPEND,
}

data class Wallet(
    val id: String,
    val name: String,
    val balance: Double,
    val colorHex: String,
    val iconKey: String,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long,
    val walletId: String,
    val toWalletId: String? = null,
    val recurringId: String? = null,
    val rawVoiceText: String? = null,
)

data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val colorHex: String,
    val isCustom: Boolean = false,
)

data class LoanRepayment(
    val id: String,
    val planId: String,
    val amount: Double,
    val note: String,
    val paidAt: Long,
)

data class PurchasePlan(
    val id: String,
    val kind: PlanKind = PlanKind.ITEM,
    val name: String,
    val personName: String = "",
    val expectedAmount: Double = 0.0,
    val category: String = "Shopping",
    val note: String = "",
    val targetDate: Long? = null,
    val dueDate: Long? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val actualAmount: Double? = null,
    val walletId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val repayments: List<LoanRepayment> = emptyList(),
)

data class RecurringRule(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val walletId: String,
    val frequency: RecurringFrequency,
    val startDate: Long,
    val endDate: Long? = null,
    val active: Boolean = true,
    val lastGeneratedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Budget(
    val id: String,
    val categoryName: String?, // null = monthly overall budget
    val limitAmount: Double,
)

data class CheckinDay(
    val dateKey: String,
    val source: CheckinSource,
    val createdAt: Long,
)

data class QuickFavorite(
    val id: String,
    val label: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val walletId: String,
    val sortOrder: Int = 0,
)

data class TransactionDraft(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Double = 0.0,
    val category: String = "Food",
    val description: String = "",
    val rawVoiceText: String = "",
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val totalBalanceHidden: Boolean = false,
    val appLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 21,
    val dailyReminderMinute: Int = 0,
    val recurringReminderEnabled: Boolean = true,
    val loanReminderEnabled: Boolean = true,
    val planReminderEnabled: Boolean = true,
    val morningReminderHour: Int = 9,
    val morningReminderMinute: Int = 0,
    val hideNotificationAmounts: Boolean = false,
    val budgetAlertsEnabled: Boolean = true,
    val aiAgentEnabled: Boolean = true,
    val aiVoiceReplyEnabled: Boolean = true,
    val aiTokensUsed: Long = 0L,
    val aiTokenLimit: Long = 0L, // 0 means show 250K EST.
    val unlockedBadges: Map<String, Long> = emptyMap(),
)

data class BadgeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val accentHex: String,
)
