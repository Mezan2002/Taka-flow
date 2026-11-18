package com.example.domain

import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRule
import java.util.Calendar

object RecurringEngine {
    fun nextOccurrence(fromMillis: Long, frequency: RecurringFrequency): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = DateUtils.startOfDay(fromMillis)
        when (frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            RecurringFrequency.BIWEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 14)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun getDueOccurrences(
        rule: RecurringRule,
        upToMillis: Long = System.currentTimeMillis(),
    ): List<Long> {
        if (!rule.active) return emptyList()
        val limit = DateUtils.endOfDay(
            if (rule.endDate != null) minOf(upToMillis, rule.endDate) else upToMillis
        )
        var cursor = if (rule.lastGeneratedDate != null) {
            nextOccurrence(rule.lastGeneratedDate, rule.frequency)
        } else {
            DateUtils.startOfDay(rule.startDate)
        }
        val results = mutableListOf<Long>()
        var guard = 0
        while (cursor <= limit && guard < 120) {
            results += cursor
            cursor = nextOccurrence(cursor, rule.frequency)
            guard++
        }
        return results
    }

    fun computeNextDue(rule: RecurringRule, nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!rule.active) return null
        val today = DateUtils.startOfDay(nowMillis)
        var cursor = if (rule.lastGeneratedDate != null) {
            nextOccurrence(rule.lastGeneratedDate, rule.frequency)
        } else {
            DateUtils.startOfDay(rule.startDate)
        }
        var guard = 0
        while (cursor < today && guard < 365) {
            cursor = nextOccurrence(cursor, rule.frequency)
            guard++
        }
        if (rule.endDate != null && cursor > DateUtils.endOfDay(rule.endDate)) return null
        return cursor
    }
}
