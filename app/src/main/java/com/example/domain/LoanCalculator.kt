package com.example.domain

import com.example.data.model.LoanStatus
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import kotlin.math.abs

object LoanCalculator {
    const val DUE_SOON_DAYS = 7

    fun isLoanKind(kind: PlanKind): Boolean = kind == PlanKind.LENT || kind == PlanKind.BORROWED

    fun isLoan(plan: PurchasePlan): Boolean = isLoanKind(plan.kind)

    fun loanPrincipal(plan: PurchasePlan): Double = CurrencyFormat.round2(plan.expectedAmount)

    fun loanPaid(plan: PurchasePlan): Double =
        CurrencyFormat.round2(plan.repayments.sumOf { it.amount })

    fun loanRemaining(plan: PurchasePlan): Double =
        CurrencyFormat.round2((loanPrincipal(plan) - loanPaid(plan)).coerceAtLeast(0.0))

    fun loanProgress(plan: PurchasePlan): Float {
        val principal = loanPrincipal(plan)
        if (principal <= 0.0) return if (plan.completed) 1f else 0f
        return (loanPaid(plan) / principal).toFloat().coerceIn(0f, 1f)
    }

    fun daysUntil(dueDate: Long?, nowMillis: Long = System.currentTimeMillis()): Int? {
        if (dueDate == null) return null
        return DateUtils.daysBetween(nowMillis, dueDate)
    }

    fun loanStatus(plan: PurchasePlan, nowMillis: Long = System.currentTimeMillis()): LoanStatus {
        if (plan.completed || loanRemaining(plan) <= 0.0) return LoanStatus.SETTLED
        val days = daysUntil(plan.dueDate, nowMillis) ?: return LoanStatus.ON_TRACK
        return when {
            days < 0 -> LoanStatus.OVERDUE
            days <= DUE_SOON_DAYS -> LoanStatus.DUE_SOON
            else -> LoanStatus.ON_TRACK
        }
    }

    fun dueLabel(plan: PurchasePlan, nowMillis: Long = System.currentTimeMillis()): String {
        if (plan.completed || loanRemaining(plan) <= 0.0) return "SETTLED"
        val days = daysUntil(plan.dueDate, nowMillis) ?: return "NO DUE DATE"
        return when {
            days < 0 -> "OVERDUE ${abs(days)}D"
            days == 0 -> "DUE TODAY"
            else -> "DUE IN ${days}D"
        }
    }

    fun loanTotals(plans: List<PurchasePlan>): Pair<Double, Double> {
        val owedToYou = plans
            .filter { it.kind == PlanKind.LENT && !it.completed }
            .sumOf { loanRemaining(it) }
        val youOwe = plans
            .filter { it.kind == PlanKind.BORROWED && !it.completed }
            .sumOf { loanRemaining(it) }
        return CurrencyFormat.round2(owedToYou) to CurrencyFormat.round2(youOwe)
    }

    fun personNames(plans: List<PurchasePlan>): List<String> =
        plans.filter { isLoan(it) && it.personName.isNotBlank() }
            .map { it.personName.trim() }
            .distinct()
            .sorted()
}
