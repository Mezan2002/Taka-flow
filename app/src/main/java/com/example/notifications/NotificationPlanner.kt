package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.AppSettings
import com.example.data.model.Budget
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.RecurringRule
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.CheckinState
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.LoanCalculator
import com.example.domain.PeriodCalculator
import com.example.domain.RecurringEngine
import com.example.data.model.PeriodPreset
import java.util.Calendar
import kotlin.math.abs

data class PlannedNotification(
    val id: Int,
    val triggerAtMillis: Long,
    val title: String,
    val body: String,
    val route: String,
)

class NotificationPlanner(private val context: Context) {

    fun syncSchedule(
        settings: AppSettings,
        plans: List<PurchasePlan>,
        recurringRules: List<RecurringRule>,
        checkinState: CheckinState,
    ) {
        ReminderReceiver.ensureChannel(context)
        cancelScheduledIds(1..60)
        if (!settings.notificationsEnabled) return

        val now = System.currentTimeMillis()
        val planned = mutableListOf<PlannedNotification>()
        val hideAmt = settings.hideNotificationAmounts

        // 1. Daily check-in reminders (14-day horizon)
        if (settings.dailyReminderEnabled) {
            for (dayOffset in 0 until 14) {
                val dayMillis = DateUtils.addDays(DateUtils.startOfDay(now), dayOffset)
                val key = DateUtils.toKey(dayMillis)
                if (dayOffset == 0 && key in checkinState.checkedDayKeys) continue
                val trigger = atTime(dayMillis, settings.dailyReminderHour, settings.dailyReminderMinute)
                if (trigger <= now) continue
                planned += PlannedNotification(
                    id = 100 + dayOffset,
                    triggerAtMillis = trigger,
                    title = "Daily TakaFlow Check-In",
                    body = "Log today's expenses or tap Nothing Spent Today to keep your streak alive.",
                    route = "add",
                )
            }
        }

        // 2. Recurring due reminders
        if (settings.recurringReminderEnabled) {
            for (rule in recurringRules.filter { it.active }) {
                val nextDue = RecurringEngine.computeNextDue(rule, now) ?: continue
                if (DateUtils.daysBetween(now, nextDue) in 0..14) {
                    val trigger = atTime(nextDue, settings.morningReminderHour, settings.morningReminderMinute)
                    if (trigger > now) {
                        val amtText = if (hideAmt) "" else " (${CurrencyFormat.formatAmount(rule.amount)})"
                        planned += PlannedNotification(
                            id = 200 + abs(rule.id.hashCode() % 100),
                            triggerAtMillis = trigger,
                            title = "Recurring ${rule.type.name.lowercase()} due",
                            body = "${rule.description.ifBlank { rule.category }}$amtText is scheduled today.",
                            route = "recurring",
                        )
                    }
                }
            }
        }

        // 3. Loan due reminders (day before & due day)
        if (settings.loanReminderEnabled) {
            for (plan in plans.filter { LoanCalculator.isLoan(it) && !it.completed && it.dueDate != null }) {
                val due = plan.dueDate!!
                val remaining = LoanCalculator.loanRemaining(plan)
                val amtText = if (hideAmt) "" else " (${CurrencyFormat.formatAmount(remaining)})"
                val verb = if (plan.kind == PlanKind.LENT) "to collect from" else "to repay"

                listOf(DateUtils.addDays(due, -1) to "tomorrow", due to "today").forEachIndexed { idx, (dayMillis, whenText) ->
                    val trigger = atTime(dayMillis, settings.morningReminderHour, settings.morningReminderMinute)
                    if (trigger > now && DateUtils.daysBetween(now, dayMillis) <= 14) {
                        planned += PlannedNotification(
                            id = 300 + abs((plan.id.hashCode() + idx) % 150),
                            triggerAtMillis = trigger,
                            title = "Loan due $whenText",
                            body = "${plan.personName.ifBlank { plan.name }}$amtText $verb.",
                            route = "plans",
                        )
                    }
                }
            }
        }

        // 4. Item purchase plan target date reminders
        if (settings.planReminderEnabled) {
            for (plan in plans.filter { it.kind == PlanKind.ITEM && !it.completed && it.targetDate != null }) {
                val target = plan.targetDate!!
                val trigger = atTime(target, settings.morningReminderHour, settings.morningReminderMinute)
                if (trigger > now && DateUtils.daysBetween(now, target) <= 14) {
                    val amtText = if (hideAmt || plan.expectedAmount <= 0.0) "" else " (${CurrencyFormat.formatAmount(plan.expectedAmount)})"
                    planned += PlannedNotification(
                        id = 500 + abs(plan.id.hashCode() % 100),
                        triggerAtMillis = trigger,
                        title = "Planned Purchase Today",
                        body = "Target day for ${plan.name}$amtText.",
                        route = "plans",
                    )
                }
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        planned.sortedBy { it.triggerAtMillis }.take(60).forEach { item ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_NOTIF_ID, item.id)
                putExtra(ReminderReceiver.EXTRA_TITLE, item.title)
                putExtra(ReminderReceiver.EXTRA_BODY, item.body)
                putExtra(ReminderReceiver.EXTRA_ROUTE, item.route)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.triggerAtMillis, pi)
            }
        }
    }

    fun scheduleTestNotification() {
        ReminderReceiver.ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTIF_ID, 999)
            putExtra(ReminderReceiver.EXTRA_TITLE, "TakaFlow Test Reminder")
            putExtra(ReminderReceiver.EXTRA_BODY, "Notifications are active and ready to keep you on track.")
            putExtra(ReminderReceiver.EXTRA_ROUTE, "home")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5_000L, pi)
    }

    suspend fun checkBudgetAlerts(
        settings: AppSettings,
        budgets: List<Budget>,
        transactions: List<Transaction>,
        alreadySent: Set<String>,
        onMarkSent: suspend (String) -> Unit,
    ) {
        if (!settings.notificationsEnabled || !settings.budgetAlertsEnabled || budgets.isEmpty()) return
        val now = System.currentTimeMillis()
        val monthRange = PeriodCalculator.presetRange(PeriodPreset.THIS_MONTH, now)
        val monthKey = DateUtils.toMonthKey(now)
        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.timestamp in monthRange.startMillis..monthRange.endMillis
        }

        for (budget in budgets) {
            if (budget.limitAmount <= 0.0) continue
            val spent = if (budget.categoryName == null) {
                monthExpenses.sumOf { it.amount }
            } else {
                monthExpenses.filter { it.category.equals(budget.categoryName, ignoreCase = true) }
                    .sumOf { it.amount }
            }
            val pct = (spent / budget.limitAmount) * 100.0
            val label = budget.categoryName ?: "Monthly Budget"

            val threshold = when {
                pct >= 100.0 -> 100
                pct >= 80.0 -> 80
                else -> null
            } ?: continue

            val key = "${monthKey}_${budget.id}_$threshold"
            if (key !in alreadySent) {
                onMarkSent(key)
                val amtBody = if (settings.hideNotificationAmounts) {
                    "$label reached $threshold% of its limit."
                } else {
                    "$label hit $threshold% (${CurrencyFormat.formatAmount(spent)} / ${CurrencyFormat.formatAmount(budget.limitAmount)})."
                }
                ReminderReceiver.showNotification(
                    context = context,
                    id = 700 + abs(key.hashCode() % 100),
                    title = if (threshold == 100) "Budget Limit Reached" else "Budget Warning (80%)",
                    body = amtBody,
                    targetRoute = "home",
                )
            }
        }
    }

    private fun cancelScheduledIds(range: IntRange) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (idx in range) {
            for (offset in listOf(100, 200, 300, 500)) {
                val id = offset + idx
                val intent = Intent(context, ReminderReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context,
                    id,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )
                if (pi != null) alarmManager.cancel(pi)
            }
        }
    }

    private fun atTime(dayMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
