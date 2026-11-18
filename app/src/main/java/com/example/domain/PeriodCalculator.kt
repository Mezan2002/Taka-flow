package com.example.domain

import com.example.data.model.PeriodPreset
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.util.Calendar

data class DayRange(
    val startMillis: Long,
    val endMillis: Long,
) {
    val label: String get() = DateUtils.formatRangeLabel(startMillis, endMillis)
    val dayCount: Int get() = DateUtils.daysBetween(startMillis, endMillis) + 1
}

data class RangeTotals(
    val income: Double,
    val expense: Double,
    val net: Double,
    val count: Int,
)

data class CategoryBreakdown(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int,
)

data class MomPoint(
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val net: Double,
)

data class WeeklyRecapStats(
    val range: DayRange,
    val income: Double,
    val expense: Double,
    val net: Double,
    val count: Int,
    val topCategory: String?,
    val dailyTotals: List<Pair<String, Double>>, // Sat..Fri
)

data class ReportSummary(
    val income: Double,
    val expense: Double,
    val net: Double,
    val savingsRate: Float,
    val topCategory: String?,
    val categories: List<CategoryBreakdown>,
    val incomeDeltaPct: Float?,
    val expenseDeltaPct: Float?,
    val momPoints: List<MomPoint>,
    val dailyTrend: List<Pair<Int, Double>>,
)

object PeriodCalculator {
    fun presetRange(preset: PeriodPreset, nowMillis: Long = System.currentTimeMillis()): DayRange {
        val todayStart = DateUtils.startOfDay(nowMillis)
        val todayEnd = DateUtils.endOfDay(nowMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (preset) {
            PeriodPreset.TODAY -> DayRange(todayStart, todayEnd)
            PeriodPreset.YESTERDAY -> {
                val y = DateUtils.addDays(todayStart, -1)
                DayRange(y, DateUtils.endOfDay(y))
            }
            PeriodPreset.LAST_7_DAYS -> DayRange(DateUtils.addDays(todayStart, -6), todayEnd)
            PeriodPreset.THIS_MONTH -> {
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                DayRange(DateUtils.startOfMonth(y, m), DateUtils.endOfMonth(y, m))
            }
            PeriodPreset.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                DayRange(DateUtils.startOfMonth(y, m), DateUtils.endOfMonth(y, m))
            }
            PeriodPreset.CUSTOM -> DayRange(todayStart, todayEnd)
        }
    }

    fun rangeTotals(transactions: List<Transaction>, range: DayRange): RangeTotals {
        val filtered = transactions.filter {
            it.type != TransactionType.TRANSFER &&
                it.timestamp in range.startMillis..range.endMillis
        }
        val income = CurrencyFormat.round2(
            filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        )
        val expense = CurrencyFormat.round2(
            filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        )
        return RangeTotals(
            income = income,
            expense = expense,
            net = CurrencyFormat.round2(income - expense),
            count = filtered.size,
        )
    }

    fun weeklyRecap(
        transactions: List<Transaction>,
        thisWeek: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): WeeklyRecapStats {
        val base = if (thisWeek) nowMillis else DateUtils.addDays(nowMillis, -7)
        val start = DateUtils.startOfBangladeshWeek(base)
        val end = DateUtils.endOfBangladeshWeek(base)
        val range = DayRange(start, end)
        val filtered = transactions.filter {
            it.type != TransactionType.TRANSFER && it.timestamp in start..end
        }
        val income = CurrencyFormat.round2(
            filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        )
        val expense = CurrencyFormat.round2(
            filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        )
        val topCategory = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .maxByOrNull { (_, list) -> list.sumOf { it.amount } }
            ?.key

        val dayLabels = listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")
        val dailyTotals = (0..6).map { offset ->
            val dStart = DateUtils.addDays(start, offset)
            val dEnd = DateUtils.endOfDay(dStart)
            val spent = CurrencyFormat.round2(
                filtered.filter { it.type == TransactionType.EXPENSE && it.timestamp in dStart..dEnd }
                    .sumOf { it.amount }
            )
            dayLabels[offset] to spent
        }
        return WeeklyRecapStats(
            range = range,
            income = income,
            expense = expense,
            net = CurrencyFormat.round2(income - expense),
            count = filtered.size,
            topCategory = topCategory,
            dailyTotals = dailyTotals,
        )
    }

    fun buildReport(
        transactions: List<Transaction>,
        range: DayRange,
        isCustomMode: Boolean,
    ): ReportSummary {
        val current = rangeTotals(transactions, range)
        val nonTransfer = transactions.filter {
            it.type != TransactionType.TRANSFER && it.timestamp in range.startMillis..range.endMillis
        }
        val expenseItems = nonTransfer.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = current.expense.coerceAtLeast(0.0)
        val categories = expenseItems
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = CurrencyFormat.round2(list.sumOf { it.amount })
                CategoryBreakdown(
                    category = cat,
                    amount = sum,
                    percentage = if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f,
                    count = list.size,
                )
            }
            .sortedByDescending { it.amount }

        val prevRange = if (isCustomMode) {
            val days = range.dayCount
            val prevEnd = DateUtils.endOfDay(DateUtils.addDays(range.startMillis, -1))
            val prevStart = DateUtils.startOfDay(DateUtils.addDays(range.startMillis, -days))
            DayRange(prevStart, prevEnd)
        } else {
            val cal = Calendar.getInstance().apply {
                timeInMillis = range.startMillis
                add(Calendar.MONTH, -1)
            }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            DayRange(DateUtils.startOfMonth(y, m), DateUtils.endOfMonth(y, m))
        }
        val prevTotals = rangeTotals(transactions, prevRange)
        val incomeDeltaPct = percentChange(prevTotals.income, current.income)
        val expenseDeltaPct = percentChange(prevTotals.expense, current.expense)
        val savingsRate = if (current.income > 0.0) {
            (((current.income - current.expense) / current.income) * 100.0).toFloat()
        } else 0f

        val momPoints = if (isCustomMode) emptyList() else {
            val cal = Calendar.getInstance().apply { timeInMillis = range.startMillis }
            (5 downTo 0).map { back ->
                val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -back) }
                val r = DayRange(
                    DateUtils.startOfMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH)),
                    DateUtils.endOfMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH)),
                )
                val t = rangeTotals(transactions, r)
                MomPoint(
                    monthLabel = DateUtils.formatShortMonth(r.startMillis),
                    income = t.income,
                    expense = t.expense,
                    net = t.net,
                )
            }
        }

        val dailyTrend = if (isCustomMode) emptyList() else {
            val cal = Calendar.getInstance().apply { timeInMillis = range.startMillis }
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            (1..maxDay).map { day ->
                val dayCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                val s = DateUtils.startOfDay(dayCal.timeInMillis)
                val e = DateUtils.endOfDay(dayCal.timeInMillis)
                val sum = expenseItems.filter { it.timestamp in s..e }.sumOf { it.amount }
                day to CurrencyFormat.round2(sum)
            }
        }

        return ReportSummary(
            income = current.income,
            expense = current.expense,
            net = current.net,
            savingsRate = savingsRate,
            topCategory = categories.firstOrNull()?.category,
            categories = categories,
            incomeDeltaPct = incomeDeltaPct,
            expenseDeltaPct = expenseDeltaPct,
            momPoints = momPoints,
            dailyTrend = dailyTrend,
        )
    }

    private fun percentChange(previous: Double, current: Double): Float? {
        if (previous <= 0.0) return null
        return (((current - previous) / previous) * 100.0).toFloat()
    }
}
