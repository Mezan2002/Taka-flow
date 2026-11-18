package com.example.domain

import com.example.data.model.CheckinDay
import com.example.data.model.CheckinSource
import com.example.data.model.Transaction
import com.example.data.model.TransactionType

data class CheckinState(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCheckinDays: Int,
    val isTodayCheckedIn: Boolean,
    val isTodayNoSpend: Boolean,
    val checkedDayKeys: Set<String>,
    val noSpendDayKeys: Set<String>,
)

object CheckinStreakEngine {
    fun evaluate(
        transactions: List<Transaction>,
        manualCheckins: List<CheckinDay>,
        nowMillis: Long = System.currentTimeMillis(),
    ): CheckinState {
        val todayKey = DateUtils.toKey(nowMillis)
        val txDayKeys = transactions
            .filter { it.recurringId == null && it.type != TransactionType.TRANSFER }
            .map { DateUtils.toKey(it.timestamp) }
            .toSet()

        val noSpendKeys = manualCheckins
            .filter { it.source == CheckinSource.NO_SPEND }
            .map { it.dateKey }
            .toSet()

        val manualKeys = manualCheckins.map { it.dateKey }.toSet()
        val allChecked = (txDayKeys + manualKeys).sorted()
        val checkedSet = allChecked.toSet()

        val isTodayChecked = todayKey in checkedSet
        val isTodayNoSpend = todayKey in noSpendKeys && todayKey !in txDayKeys

        // Today never breaks the streak; start counting backwards from today if checked, else yesterday.
        var cursorMillis = if (isTodayChecked) {
            DateUtils.startOfDay(nowMillis)
        } else {
            DateUtils.addDays(DateUtils.startOfDay(nowMillis), -1)
        }
        var currentStreak = 0
        while (DateUtils.toKey(cursorMillis) in checkedSet && currentStreak < 800) {
            currentStreak++
            cursorMillis = DateUtils.addDays(cursorMillis, -1)
        }

        // Compute best streak across all recorded check-ins
        var bestStreak = currentStreak
        if (allChecked.isNotEmpty()) {
            var run = 1
            var prevMillis = DateUtils.fromKey(allChecked.first())
            for (i in 1 until allChecked.size) {
                val curMillis = DateUtils.fromKey(allChecked[i])
                val diff = DateUtils.daysBetween(prevMillis, curMillis)
                if (diff == 1) {
                    run++
                } else if (diff > 1) {
                    if (run > bestStreak) bestStreak = run
                    run = 1
                }
                prevMillis = curMillis
            }
            if (run > bestStreak) bestStreak = run
        }

        return CheckinState(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCheckinDays = checkedSet.size,
            isTodayCheckedIn = isTodayChecked,
            isTodayNoSpend = isTodayNoSpend,
            checkedDayKeys = checkedSet,
            noSpendDayKeys = noSpendKeys,
        )
    }
}
