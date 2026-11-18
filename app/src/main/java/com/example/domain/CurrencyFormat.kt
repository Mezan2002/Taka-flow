package com.example.domain

import com.example.data.model.CURRENCY
import com.example.data.model.CURRENCY_SYMBOL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormat {
    private val wholeFormat = DecimalFormat("#,##0")
    private val decimalFormat = DecimalFormat("#,##0.##")

    fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    fun formatAmount(amount: Double, hidden: Boolean = false): String {
        if (hidden) return "$CURRENCY_SYMBOL •••••"
        val clean = round2(amount)
        val absVal = abs(clean)
        val formatted = if (absVal % 1.0 == 0.0) {
            wholeFormat.format(absVal)
        } else {
            decimalFormat.format(absVal)
        }
        val sign = if (clean < 0) "-" else ""
        return "$sign$CURRENCY_SYMBOL$formatted"
    }

    fun formatAmountWithUnit(amount: Double, hidden: Boolean = false): String {
        if (hidden) return "••••• $CURRENCY"
        val clean = round2(amount)
        val absVal = abs(clean)
        val formatted = if (absVal % 1.0 == 0.0) {
            wholeFormat.format(absVal)
        } else {
            decimalFormat.format(absVal)
        }
        val sign = if (clean < 0) "-" else ""
        return "$sign$formatted $CURRENCY"
    }

    fun formatCompact(amount: Double): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 10_000_000 -> "$sign$CURRENCY_SYMBOL${decimalFormat.format(absVal / 1_000_000)}M"
            absVal >= 100_000 -> "$sign$CURRENCY_SYMBOL${decimalFormat.format(absVal / 1_000)}K"
            else -> formatAmount(amount)
        }
    }
}

object DateUtils {
    private val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
    private val shortDateFormatter = SimpleDateFormat("d MMM", Locale.US)
    private val mediumDateFormatter = SimpleDateFormat("d MMM yyyy", Locale.US)
    private val fullHeaderFormatter = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.US)
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val shortMonthFormatter = SimpleDateFormat("MMM yy", Locale.US)

    fun toKey(millis: Long): String = keyFormatter.format(Date(millis))

    fun toMonthKey(millis: Long): String = monthKeyFormatter.format(Date(millis))

    fun fromKey(key: String): Long {
        return try {
            val parsed = keyFormatter.parse(key)
            startOfDay(parsed?.time ?: System.currentTimeMillis())
        } catch (_: Exception) {
            startOfDay(System.currentTimeMillis())
        }
    }

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    fun addMonths(millis: Long, months: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    /**
     * Bangladesh week starts on Saturday (Calendar.SATURDAY = 7).
     */
    fun startOfBangladeshWeek(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDay(millis)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offsetFromSaturday = (dayOfWeek - Calendar.SATURDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -offsetFromSaturday)
        return cal.timeInMillis
    }

    fun endOfBangladeshWeek(millis: Long): Long {
        val start = startOfBangladeshWeek(millis)
        return endOfDay(addDays(start, 6))
    }

    fun startOfMonth(year: Int, monthZeroIndexed: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, monthZeroIndexed, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfMonth(year: Int, monthZeroIndexed: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, monthZeroIndexed, 1, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return cal.timeInMillis
    }

    fun formatShortDate(millis: Long): String = shortDateFormatter.format(Date(millis))
    fun formatMediumDate(millis: Long): String = mediumDateFormatter.format(Date(millis))
    fun formatHeaderDate(millis: Long): String = fullHeaderFormatter.format(Date(millis))
    fun formatTime(millis: Long): String = timeFormatter.format(Date(millis))
    fun formatMonthYear(millis: Long): String = monthYearFormatter.format(Date(millis))
    fun formatShortMonth(millis: Long): String = shortMonthFormatter.format(Date(millis))

    fun formatRangeLabel(startMillis: Long, endMillis: Long): String {
        val sKey = toKey(startMillis)
        val eKey = toKey(endMillis)
        return if (sKey == eKey) {
            formatMediumDate(startMillis).uppercase(Locale.US)
        } else {
            "${formatShortDate(startMillis).uppercase(Locale.US)} – ${formatMediumDate(endMillis).uppercase(Locale.US)}"
        }
    }

    fun daysBetween(fromMillis: Long, toMillis: Long): Int {
        val diff = startOfDay(toMillis) - startOfDay(fromMillis)
        return (diff / 86_400_000L).toInt()
    }
}
