package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.CheckinDay
import com.example.data.model.CheckinSource
import com.example.data.model.Category
import com.example.data.model.LoanRepayment
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.QuickFavorite
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRule
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.data.model.Budget

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: Double,
    val colorHex: String,
    val iconKey: String,
    val isHidden: Boolean,
    val sortOrder: Int,
) {
    fun toDomain() = Wallet(id, name, balance, colorHex, iconKey, isHidden, sortOrder)
}

fun Wallet.toEntity() = WalletEntity(id, name, balance, colorHex, iconKey, isHidden, sortOrder)

@Entity(
    tableName = "transactions",
    indices = [Index("timestamp"), Index("walletId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long,
    val walletId: String,
    val toWalletId: String?,
    val recurringId: String?,
    val rawVoiceText: String?,
) {
    fun toDomain() = Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        amount = amount,
        category = category,
        description = description,
        timestamp = timestamp,
        walletId = walletId,
        toWalletId = toWalletId,
        recurringId = recurringId,
        rawVoiceText = rawVoiceText,
    )
}

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    category = category,
    description = description,
    timestamp = timestamp,
    walletId = walletId,
    toWalletId = toWalletId,
    recurringId = recurringId,
    rawVoiceText = rawVoiceText,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val iconKey: String,
    val colorHex: String,
    val isCustom: Boolean,
) {
    fun toDomain() = Category(id, name, TransactionType.valueOf(type), iconKey, colorHex, isCustom)
}

fun Category.toEntity() = CategoryEntity(id, name, type.name, iconKey, colorHex, isCustom)

@Entity(tableName = "purchase_plans")
data class PurchasePlanEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    val personName: String,
    val expectedAmount: Double,
    val category: String,
    val note: String,
    val targetDate: Long?,
    val dueDate: Long?,
    val completed: Boolean,
    val completedAt: Long?,
    val actualAmount: Double?,
    val walletId: String?,
    val createdAt: Long,
) {
    fun toDomain(repayments: List<LoanRepayment>) = PurchasePlan(
        id = id,
        kind = PlanKind.valueOf(kind),
        name = name,
        personName = personName,
        expectedAmount = expectedAmount,
        category = category,
        note = note,
        targetDate = targetDate,
        dueDate = dueDate,
        completed = completed,
        completedAt = completedAt,
        actualAmount = actualAmount,
        walletId = walletId,
        createdAt = createdAt,
        repayments = repayments,
    )
}

fun PurchasePlan.toEntity() = PurchasePlanEntity(
    id = id,
    kind = kind.name,
    name = name,
    personName = personName,
    expectedAmount = expectedAmount,
    category = category,
    note = note,
    targetDate = targetDate,
    dueDate = dueDate,
    completed = completed,
    completedAt = completedAt,
    actualAmount = actualAmount,
    walletId = walletId,
    createdAt = createdAt,
)

@Entity(
    tableName = "loan_repayments",
    foreignKeys = [
        ForeignKey(
            entity = PurchasePlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("planId")]
)
data class LoanRepaymentEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val amount: Double,
    val note: String,
    val paidAt: Long,
) {
    fun toDomain() = LoanRepayment(id, planId, amount, note, paidAt)
}

fun LoanRepayment.toEntity() = LoanRepaymentEntity(id, planId, amount, note, paidAt)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Double,
    val category: String,
    val description: String,
    val walletId: String,
    val frequency: String,
    val startDate: Long,
    val endDate: Long?,
    val active: Boolean,
    val lastGeneratedDate: Long?,
    val createdAt: Long,
) {
    fun toDomain() = RecurringRule(
        id = id,
        type = TransactionType.valueOf(type),
        amount = amount,
        category = category,
        description = description,
        walletId = walletId,
        frequency = RecurringFrequency.valueOf(frequency),
        startDate = startDate,
        endDate = endDate,
        active = active,
        lastGeneratedDate = lastGeneratedDate,
        createdAt = createdAt,
    )
}

fun RecurringRule.toEntity() = RecurringRuleEntity(
    id = id,
    type = type.name,
    amount = amount,
    category = category,
    description = description,
    walletId = walletId,
    frequency = frequency.name,
    startDate = startDate,
    endDate = endDate,
    active = active,
    lastGeneratedDate = lastGeneratedDate,
    createdAt = createdAt,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryName: String?,
    val limitAmount: Double,
) {
    fun toDomain() = Budget(id, categoryName, limitAmount)
}

fun Budget.toEntity() = BudgetEntity(id, categoryName, limitAmount)

@Entity(tableName = "checkins")
data class CheckinDayEntity(
    @PrimaryKey val dateKey: String,
    val source: String,
    val createdAt: Long,
) {
    fun toDomain() = CheckinDay(dateKey, CheckinSource.valueOf(source), createdAt)
}

fun CheckinDay.toEntity() = CheckinDayEntity(dateKey, source.name, createdAt)

@Entity(tableName = "quick_favorites")
data class QuickFavoriteEntity(
    @PrimaryKey val id: String,
    val label: String,
    val amount: Double,
    val type: String,
    val category: String,
    val walletId: String,
    val sortOrder: Int,
) {
    fun toDomain() = QuickFavorite(
        id = id,
        label = label,
        amount = amount,
        type = TransactionType.valueOf(type),
        category = category,
        walletId = walletId,
        sortOrder = sortOrder,
    )
}

fun QuickFavorite.toEntity() = QuickFavoriteEntity(
    id = id,
    label = label,
    amount = amount,
    type = type.name,
    category = category,
    walletId = walletId,
    sortOrder = sortOrder,
)
