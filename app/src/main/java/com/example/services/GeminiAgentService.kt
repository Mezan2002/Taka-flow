package com.example.services

import android.util.Log
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRule
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.PeriodCalculator
import com.example.data.model.PeriodPreset
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

sealed interface PendingAgentAction {
    val summaryTitle: String
    val summarySubtitle: String

    data class AddTransaction(
        val type: TransactionType,
        val amount: Double,
        val category: String,
        val description: String,
        val walletName: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Add ${type.name.lowercase().replaceFirstChar { it.uppercase() }}"
        override val summarySubtitle: String get() = "${CurrencyFormat.formatAmount(amount)} • $category ($walletName)"
    }

    data class DeleteTransaction(
        val transactionId: String,
        val description: String,
        val amount: Double,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Delete Entry"
        override val summarySubtitle: String get() = "$description (${CurrencyFormat.formatAmount(amount)})"
    }

    data class TransferWallets(
        val fromWalletName: String,
        val toWalletName: String,
        val amount: Double,
        val note: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Wallet Transfer"
        override val summarySubtitle: String get() = "$fromWalletName → $toWalletName • ${CurrencyFormat.formatAmount(amount)}"
    }

    data class SetBudget(
        val categoryName: String?, // null = monthly overall
        val limitAmount: Double,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = if (categoryName == null) "Set Monthly Budget" else "Set $categoryName Budget"
        override val summarySubtitle: String get() = "Limit: ${CurrencyFormat.formatAmount(limitAmount)}"
    }

    data class AddCategory(
        val name: String,
        val type: TransactionType,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Add Category"
        override val summarySubtitle: String get() = "$name (${type.name})"
    }

    data class DeleteCategory(
        val name: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Delete Category"
        override val summarySubtitle: String get() = name
    }

    data class AddRecurring(
        val type: TransactionType,
        val amount: Double,
        val category: String,
        val description: String,
        val frequency: RecurringFrequency,
        val walletName: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Add Recurring Rule"
        override val summarySubtitle: String get() = "${CurrencyFormat.formatAmount(amount)} • ${frequency.label} • $category"
    }

    data class ToggleRecurring(
        val ruleId: String,
        val description: String,
        val active: Boolean,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = if (active) "Resume Recurring Rule" else "Pause Recurring Rule"
        override val summarySubtitle: String get() = description
    }

    data class DeleteRecurring(
        val ruleId: String,
        val description: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Delete Recurring Rule"
        override val summarySubtitle: String get() = description
    }

    data class AddPlan(
        val name: String,
        val expectedAmount: Double,
        val category: String,
        val note: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Add Purchase Plan"
        override val summarySubtitle: String get() = "$name • ${CurrencyFormat.formatAmount(expectedAmount)}"
    }

    data class CompletePlan(
        val planId: String,
        val planName: String,
        val actualAmount: Double,
        val walletName: String,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Mark Plan Bought"
        override val summarySubtitle: String get() = "$planName • ${CurrencyFormat.formatAmount(actualAmount)} ($walletName)"
    }

    data class AddWallet(
        val name: String,
        val initialBalance: Double,
    ) : PendingAgentAction {
        override val summaryTitle: String get() = "Add Wallet"
        override val summarySubtitle: String get() = "$name (${CurrencyFormat.formatAmount(initialBalance)})"
    }
}

data class AgentTurnResult(
    val replyText: String,
    val proposedAction: PendingAgentAction?,
    val estimatedTokens: Long,
)

class GeminiAgentService {
    private val responseSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(description = "Friendly, concise answer in the user's language (English, Bangla, or Banglish). Always summarize any proposed write action so the user can review and confirm."),
            "actionType" to Schema.enumeration(
                values = listOf(
                    "NONE",
                    "ADD_TRANSACTION",
                    "DELETE_TRANSACTION",
                    "TRANSFER",
                    "SET_BUDGET",
                    "ADD_CATEGORY",
                    "DELETE_CATEGORY",
                    "ADD_RECURRING",
                    "TOGGLE_RECURRING",
                    "DELETE_RECURRING",
                    "ADD_PLAN",
                    "COMPLETE_PLAN",
                    "ADD_WALLET",
                ),
                description = "Use NONE for questions/read queries. Use a specific actionType when the user asks to add, edit, set, transfer, pause, or delete data.",
            ),
            "txType" to Schema.string(description = "INCOME or EXPENSE", nullable = true),
            "amount" to Schema.double(description = "Amount in TAKA", nullable = true),
            "category" to Schema.string(description = "Category name", nullable = true),
            "description" to Schema.string(description = "Short item description or name", nullable = true),
            "walletName" to Schema.string(description = "Source wallet name, e.g. Cash", nullable = true),
            "toWalletName" to Schema.string(description = "Destination wallet name for TRANSFER", nullable = true),
            "targetId" to Schema.string(description = "Exact ID of matching transaction, recurring rule, or purchase plan when deleting/toggling/completing", nullable = true),
            "frequency" to Schema.string(description = "DAILY, WEEKLY, BIWEEKLY, MONTHLY, or YEARLY", nullable = true),
            "active" to Schema.boolean(description = "Active state for TOGGLE_RECURRING", nullable = true),
            "isMonthlyBudget" to Schema.boolean(description = "True if setting overall monthly budget rather than a category budget", nullable = true),
        ),
        optionalProperties = listOf(
            "txType", "amount", "category", "description", "walletName",
            "toWalletName", "targetId", "frequency", "active", "isMonthlyBudget",
        ),
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@GeminiAgentService.responseSchema
            },
        )
    }

    suspend fun ask(
        userMessage: String,
        historyPairs: List<Pair<String, String>>,
        wallets: List<Wallet>,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        recurringRules: List<RecurringRule>,
        plans: List<PurchasePlan>,
    ): AgentTurnResult {
        val contextSnapshot = buildContextSnapshot(
            wallets = wallets,
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            recurringRules = recurringRules,
            plans = plans,
        )

        val systemPreamble = """
            You are TakaFlow AI, a helpful personal finance assistant for an Android expense tracker in Bangladesh (currency: TAKA / ৳).
            Understand English, Bangla, and Banglish fluently.
            RULES:
            1. Never claim you have already modified data; instead propose an action so the app shows a Preview & Confirm card to the user.
            2. Loans and debts tools are NOT available yet; if the user asks to add/edit/settle a loan, politely explain loan automation is coming soon and guide them to the Plans & Loans tab.
            3. Keep replies crisp, accurate, and grounded ONLY in the live ledger snapshot below.
            4. For read questions, set actionType = "NONE" and answer directly from the snapshot.

            LIVE LEDGER SNAPSHOT (Today: ${DateUtils.toKey(System.currentTimeMillis())}):
            $contextSnapshot
        """.trimIndent()

        val promptHistory = historyPairs.takeLast(6).flatMap { (userText, modelText) ->
            listOf(
                content("user") { text(userText) },
                content("model") { text(modelText) },
            )
        }

        val fullUserPrompt = "$systemPreamble\n\nUSER MESSAGE: $userMessage"

        try {
            val response = withTimeout(60_000L) {
                val chat = model.startChat(history = promptHistory)
                chat.sendMessage(fullUserPrompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
            val obj = JSONObject(rawJson)
            val reply = obj.optString("reply", "Here is what I prepared for you.")
            val action = parseAction(obj, wallets, transactions, recurringRules, plans)
            val usage = response.usageMetadata
            val tokens = (usage?.totalTokenCount ?: ((fullUserPrompt.length + rawJson.length) / 4)).toLong()
            return AgentTurnResult(
                replyText = reply,
                proposedAction = action,
                estimatedTokens = tokens,
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI call timed out", e)
            throw IllegalStateException("AI request timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI error", e)
            throw e
        }
    }

    private fun parseAction(
        obj: JSONObject,
        wallets: List<Wallet>,
        transactions: List<Transaction>,
        recurringRules: List<RecurringRule>,
        plans: List<PurchasePlan>,
    ): PendingAgentAction? {
        val actionType = obj.optString("actionType", "NONE")
        if (actionType == "NONE") return null

        val defaultWallet = wallets.firstOrNull()?.name ?: "Cash"
        val txType = if (obj.optString("txType").equals("INCOME", ignoreCase = true)) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
        val amount = obj.optDouble("amount", 0.0).takeIf { !it.isNaN() && it > 0 } ?: 0.0
        val category = obj.optString("category").takeIf { it.isNotBlank() && it != "null" }
            ?: if (txType == TransactionType.INCOME) "Salary" else "Food"
        val description = obj.optString("description").takeIf { it.isNotBlank() && it != "null" } ?: category
        val walletName = obj.optString("walletName").takeIf { it.isNotBlank() && it != "null" } ?: defaultWallet
        val toWalletName = obj.optString("toWalletName").takeIf { it.isNotBlank() && it != "null" } ?: defaultWallet
        val targetId = obj.optString("targetId").takeIf { it.isNotBlank() && it != "null" }

        return when (actionType) {
            "ADD_TRANSACTION" -> if (amount > 0) {
                PendingAgentAction.AddTransaction(txType, amount, category, description, walletName)
            } else null

            "DELETE_TRANSACTION" -> {
                val match = transactions.firstOrNull { it.id == targetId }
                    ?: transactions.firstOrNull { it.description.contains(description, ignoreCase = true) }
                    ?: transactions.firstOrNull()
                match?.let {
                    PendingAgentAction.DeleteTransaction(it.id, it.description.ifBlank { it.category }, it.amount)
                }
            }

            "TRANSFER" -> if (amount > 0) {
                PendingAgentAction.TransferWallets(walletName, toWalletName, amount, description)
            } else null

            "SET_BUDGET" -> if (amount > 0) {
                val isMonthly = obj.optBoolean("isMonthlyBudget", false)
                PendingAgentAction.SetBudget(if (isMonthly) null else category, amount)
            } else null

            "ADD_CATEGORY" -> PendingAgentAction.AddCategory(description, txType)

            "DELETE_CATEGORY" -> PendingAgentAction.DeleteCategory(description)

            "ADD_RECURRING" -> if (amount > 0) {
                val freq = runCatching {
                    RecurringFrequency.valueOf(obj.optString("frequency", "MONTHLY").uppercase())
                }.getOrDefault(RecurringFrequency.MONTHLY)
                PendingAgentAction.AddRecurring(txType, amount, category, description, freq, walletName)
            } else null

            "TOGGLE_RECURRING" -> {
                val rule = recurringRules.firstOrNull { it.id == targetId }
                    ?: recurringRules.firstOrNull { it.description.contains(description, ignoreCase = true) }
                rule?.let {
                    val nextActive = if (obj.has("active") && !obj.isNull("active")) {
                        obj.optBoolean("active", !it.active)
                    } else !it.active
                    PendingAgentAction.ToggleRecurring(it.id, it.description.ifBlank { it.category }, nextActive)
                }
            }

            "DELETE_RECURRING" -> {
                val rule = recurringRules.firstOrNull { it.id == targetId }
                    ?: recurringRules.firstOrNull { it.description.contains(description, ignoreCase = true) }
                rule?.let {
                    PendingAgentAction.DeleteRecurring(it.id, it.description.ifBlank { it.category })
                }
            }

            "ADD_PLAN" -> PendingAgentAction.AddPlan(description, amount, category, "")

            "COMPLETE_PLAN" -> {
                val itemPlans = plans.filter { it.kind == PlanKind.ITEM && !it.completed }
                val plan = itemPlans.firstOrNull { it.id == targetId }
                    ?: itemPlans.firstOrNull { it.name.contains(description, ignoreCase = true) }
                plan?.let {
                    val finalAmt = if (amount > 0) amount else it.expectedAmount
                    PendingAgentAction.CompletePlan(it.id, it.name, finalAmt, walletName)
                }
            }

            "ADD_WALLET" -> PendingAgentAction.AddWallet(description, amount)

            else -> null
        }
    }

    private fun buildContextSnapshot(
        wallets: List<Wallet>,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        recurringRules: List<RecurringRule>,
        plans: List<PurchasePlan>,
    ): String {
        val monthTotals = PeriodCalculator.rangeTotals(
            transactions,
            PeriodCalculator.presetRange(PeriodPreset.THIS_MONTH),
        )
        val todayTotals = PeriodCalculator.rangeTotals(
            transactions,
            PeriodCalculator.presetRange(PeriodPreset.TODAY),
        )
        val walletLines = wallets.joinToString("; ") { "${it.name}: ${CurrencyFormat.formatAmount(it.balance)}" }
        val budgetLines = budgets.joinToString("; ") { "${it.categoryName ?: "Monthly"}: ${CurrencyFormat.formatAmount(it.limitAmount)}" }
        val catLines = categories.joinToString(", ") { "${it.name}(${it.type.name})" }
        val recLines = recurringRules.take(10).joinToString("; ") {
            "[id=${it.id}] ${it.description.ifBlank { it.category }} ${CurrencyFormat.formatAmount(it.amount)} ${it.frequency.name} active=${it.active}"
        }
        val planLines = plans.filter { it.kind == PlanKind.ITEM }.take(12).joinToString("; ") {
            "[id=${it.id}] ${it.name} exp=${CurrencyFormat.formatAmount(it.expectedAmount)} done=${it.completed}"
        }
        val recentTxLines = transactions.take(15).joinToString("\n") {
            "  - [id=${it.id}] ${DateUtils.toKey(it.timestamp)} ${it.type.name} ${CurrencyFormat.formatAmount(it.amount)} ${it.category}: ${it.description}"
        }
        return """
            Wallets: $walletLines
            Today: income=${CurrencyFormat.formatAmount(todayTotals.income)}, expense=${CurrencyFormat.formatAmount(todayTotals.expense)}
            This Month: income=${CurrencyFormat.formatAmount(monthTotals.income)}, expense=${CurrencyFormat.formatAmount(monthTotals.expense)}, net=${CurrencyFormat.formatAmount(monthTotals.net)}
            Budgets: ${budgetLines.ifBlank { "None" }}
            Categories: $catLines
            Recurring Rules: ${recLines.ifBlank { "None" }}
            Purchase Plans (Items): ${planLines.ifBlank { "None" }}
            Recent Transactions:
            $recentTxLines
        """.trimIndent()
    }

    companion object {
        private const val TAG = "GeminiAgentService"
    }
}
