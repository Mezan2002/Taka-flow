package com.example.domain

import com.example.data.model.BadgeDefinition
import com.example.data.model.Budget
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.RecurringRule
import com.example.data.model.Transaction
import com.example.data.model.Wallet

object BadgeEngine {
    val definitions: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "first_entry",
            title = "First Entry",
            description = "Logged your very first transaction in TakaFlow.",
            iconKey = "bolt",
            accentHex = "#38BDF8",
        ),
        BadgeDefinition(
            id = "voice_logger",
            title = "Voice Pioneer",
            description = "Captured an entry hands-free using voice logging.",
            iconKey = "mic",
            accentHex = "#34D399",
        ),
        BadgeDefinition(
            id = "consistency_7",
            title = "Consistency",
            description = "Reached a 7-day check-in streak.",
            iconKey = "flame",
            accentHex = "#FBBF24",
        ),
        BadgeDefinition(
            id = "consistency_30",
            title = "Streak Master",
            description = "Maintained a 30-day check-in streak.",
            iconKey = "crown",
            accentHex = "#F59E0B",
        ),
        BadgeDefinition(
            id = "multi_wallet",
            title = "Multi-Wallet",
            description = "Managed 2 or more active wallets.",
            iconKey = "wallet",
            accentHex = "#818CF8",
        ),
        BadgeDefinition(
            id = "budget_planner",
            title = "Budget Architect",
            description = "Set a monthly or category budget.",
            iconKey = "target",
            accentHex = "#2DD4BF",
        ),
        BadgeDefinition(
            id = "loan_closer",
            title = "Loan Closer",
            description = "Settled a lent or borrowed loan completely.",
            iconKey = "handshake",
            accentHex = "#60A5FA",
        ),
        BadgeDefinition(
            id = "century_club",
            title = "Century Club",
            description = "Recorded 100 transactions in your ledger.",
            iconKey = "trophy",
            accentHex = "#FB7185",
        ),
        BadgeDefinition(
            id = "automation_pro",
            title = "Auto Pilot",
            description = "Created a recurring income or expense rule.",
            iconKey = "repeat",
            accentHex = "#A78BFA",
        ),
    )

    fun evaluateNewUnlocks(
        alreadyUnlocked: Map<String, Long>,
        transactions: List<Transaction>,
        wallets: List<Wallet>,
        plans: List<PurchasePlan>,
        budgets: List<Budget>,
        recurringRules: List<RecurringRule>,
        checkinState: CheckinState,
    ): List<BadgeDefinition> {
        val earnedIds = mutableSetOf<String>()
        if (transactions.isNotEmpty()) earnedIds += "first_entry"
        if (transactions.any { !it.rawVoiceText.isNullOrBlank() }) earnedIds += "voice_logger"
        if (checkinState.bestStreak >= 7) earnedIds += "consistency_7"
        if (checkinState.bestStreak >= 30) earnedIds += "consistency_30"
        if (wallets.size >= 2) earnedIds += "multi_wallet"
        if (budgets.isNotEmpty()) earnedIds += "budget_planner"
        if (plans.any { (it.kind == PlanKind.LENT || it.kind == PlanKind.BORROWED) && it.completed }) {
            earnedIds += "loan_closer"
        }
        if (transactions.size >= 100) earnedIds += "century_club"
        if (recurringRules.isNotEmpty()) earnedIds += "automation_pro"

        return definitions.filter { it.id in earnedIds && it.id !in alreadyUnlocked }
    }
}
