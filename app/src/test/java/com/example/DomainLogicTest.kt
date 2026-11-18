package com.example

import com.example.data.model.CheckinDay
import com.example.data.model.CheckinSource
import com.example.data.model.LoanRepayment
import com.example.data.model.LoanStatus
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.CheckinStreakEngine
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.LoanCalculator
import com.example.domain.OfflineVoiceParser
import com.example.domain.PeriodCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DomainLogicTest {

    @Test
    fun offlineVoiceParser_parsesMultiItemAndBanglishAndIncome() {
        val multi = OfflineVoiceParser.parse("Tea 20 Rickshaw 50")
        assertEquals(2, multi.size)
        assertEquals(20.0, multi[0].amount, 0.01)
        assertEquals("Food", multi[0].category)
        assertEquals(50.0, multi[1].amount, 0.01)
        assertEquals("Transport", multi[1].category)

        val banglish = OfflineVoiceParser.parse("Cha 20 ebong Bazar 450")
        assertEquals(2, banglish.size)
        assertEquals(20.0, banglish[0].amount, 0.01)
        assertEquals(450.0, banglish[1].amount, 0.01)

        val income = OfflineVoiceParser.parse("Received salary 35000")
        assertEquals(1, income.size)
        assertEquals(TransactionType.INCOME, income[0].type)
        assertEquals(35000.0, income[0].amount, 0.01)
        assertEquals("Salary", income[0].category)

        val banglaDigits = OfflineVoiceParser.parse("cha ২৫")
        assertEquals(1, banglaDigits.size)
        assertEquals(25.0, banglaDigits[0].amount, 0.01)
    }

    @Test
    fun bangladeshWeek_startsOnSaturday_andRangeTotalsExcludesTransfers() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 17, 14, 0, 0) } // Tuesday
        val weekStart = DateUtils.startOfBangladeshWeek(cal.timeInMillis)
        val startCal = Calendar.getInstance().apply { timeInMillis = weekStart }
        assertEquals(Calendar.SATURDAY, startCal.get(Calendar.DAY_OF_WEEK))

        val now = cal.timeInMillis
        val txs = listOf(
            Transaction("1", TransactionType.INCOME, 5000.0, "Salary", "Bonus", now, "w1"),
            Transaction("2", TransactionType.EXPENSE, 1200.0, "Food", "Dinner", now, "w1"),
            Transaction("3", TransactionType.TRANSFER, 2000.0, "Transfer", "To bKash", now, "w1", "w2"),
        )
        val totals = PeriodCalculator.rangeTotals(
            txs,
            DayRange(DateUtils.startOfDay(now), DateUtils.endOfDay(now)),
        )
        assertEquals(5000.0, totals.income, 0.01)
        assertEquals(1200.0, totals.expense, 0.01)
        assertEquals(3800.0, totals.net, 0.01)
        assertEquals(2, totals.count)
    }

    @Test
    fun loanCalculator_computesRemainingStatusAndDueLabel() {
        val now = DateUtils.startOfDay(System.currentTimeMillis())
        val tomorrow = DateUtils.addDays(now, 1)
        val loan = PurchasePlan(
            id = "l1",
            kind = PlanKind.LENT,
            name = "Rahim",
            personName = "Rahim",
            expectedAmount = 1000.0,
            dueDate = tomorrow,
            repayments = listOf(LoanRepayment("r1", "l1", 400.0, "Part", now)),
        )
        assertEquals(600.0, LoanCalculator.loanRemaining(loan), 0.01)
        assertEquals(LoanStatus.DUE_SOON, LoanCalculator.loanStatus(loan, now))
        assertEquals("DUE IN 1D", LoanCalculator.dueLabel(loan, now))
    }

    @Test
    fun checkinStreak_countsYesterdayStreakEvenBeforeTodayLogged() {
        val today = DateUtils.startOfDay(System.currentTimeMillis())
        val y1 = DateUtils.addDays(today, -1)
        val y2 = DateUtils.addDays(today, -2)
        val checkins = listOf(
            CheckinDay(DateUtils.toKey(y1), CheckinSource.NO_SPEND, y1),
            CheckinDay(DateUtils.toKey(y2), CheckinSource.NO_SPEND, y2),
        )
        val state = CheckinStreakEngine.evaluate(emptyList(), checkins, today)
        assertEquals(2, state.currentStreak)
        assertTrue(!state.isTodayCheckedIn)
    }
}
