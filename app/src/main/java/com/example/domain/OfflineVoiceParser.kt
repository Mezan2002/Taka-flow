package com.example.domain

import com.example.data.model.TransactionDraft
import com.example.data.model.TransactionType
import java.util.Locale

/**
 * 100% offline rule-based parser supporting English, Banglish, and numeric Bangla/English voice strings.
 * Examples:
 * - "tea 20 rickshaw 50" -> 2 expense drafts (Food: Tea 20, Transport: Rickshaw 50)
 * - "received salary 25000" -> 1 income draft (Salary: 25000)
 * - "cha 20 taka and lunch 150" -> 2 expense drafts
 */
object OfflineVoiceParser {

    private val incomeKeywords = setOf(
        "salary", "beton", "income", "earned", "earn", "received", "receive",
        "bonus", "freelance", "client", "refund", "gift", "pocket money",
        "tution", "tuition", "profit", "dividend", "commission", "deposit",
        "pelam", "paisi", "aslo", "ashlo", "ay", "aay",
    )

    private val categoryKeywords = listOf(
        "Salary" to listOf("salary", "beton", "paycheck", "stipend"),
        "Freelance" to listOf("freelance", "client", "project", "fiverr", "upwork", "tuition", "tution"),
        "Bonus" to listOf("bonus", "eid", "salami", "gift", "reward"),
        "Food" to listOf(
            "tea", "cha", "coffee", "kofi", "lunch", "dinner", "breakfast", "nasta",
            "snack", "snacks", "biryani", "burger", "pizza", "rice", "bhat", "meal",
            "restaurant", "hotel", "khabar", "singara", "samosa", "fuchka", "water", "pani",
            "bazar", "groceries", "grocery", "fruit", "milk", "dudh", "egg", "dim",
        ),
        "Transport" to listOf(
            "rickshaw", "rikshaw", "cng", "uber", "pathao", "bus", "train", "metro",
            "bike", "fuel", "petrol", "octane", "gas", "fare", "vara", "bhara",
            "ticket", "auto", "leguna", "launch", "flight",
        ),
        "Shopping" to listOf(
            "shirt", "pant", "shoes", "juta", "dress", "shopping", "daraz", "market",
            "clothes", "kapod", "kapor", "bag", "watch", "perfume", "cosmetics",
        ),
        "Bills" to listOf(
            "bill", "electricity", "current", "wifi", "internet", "broadband",
            "recharge", "flexiload", "mobile", "mb", "gas bill", "water bill",
            "rent", "basha bhara", "basa vara", "house rent",
        ),
        "Health" to listOf(
            "medicine", "oshudh", "osudh", "doctor", "pharmacy", "hospital",
            "clinic", "test", "dental", "health", "gym",
        ),
        "Entertainment" to listOf(
            "movie", "cinema", "netflix", "spotify", "game", "hangout", "adda",
            "tour", "trip", "outing", "concert",
        ),
        "Education" to listOf(
            "book", "boi", "pen", "khata", "course", "exam", "fee", "school",
            "college", "university", "varsity", "tuition",
        ),
    )

    private val banglaDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9',
    )

    private val splitRegex = Regex("""(?:\s+and\s+|\s+ebong\s+|\s+r\s+|,|;|\n)+""", RegexOption.IGNORE_CASE)
    private val numberRegex = Regex("""(\d+(?:[.,]\d{1,2})?)""")
    private val noiseWords = setOf(
        "taka", "tk", "bdt", "spent", "spend", "paid", "pay", "for", "on", "of",
        "in", "at", "to", "from", "rs", "got", "received", "earned", "i", "my",
        "diyesi", "disi", "khoroch", "khoros", "pelam", "paisi", "hoyeche",
    )

    fun parse(rawInput: String): List<TransactionDraft> {
        val normalized = normalizeBanglaDigits(rawInput).trim()
        if (normalized.isBlank()) return emptyList()

        // First split by explicit separators ("and", ",", ";")
        val initialChunks = splitRegex.split(normalized).map { it.trim() }.filter { it.isNotBlank() }

        // If a single chunk contains multiple "<label> <amount>" pairs (e.g. "tea 20 rickshaw 50"), split them
        val segments = mutableListOf<String>()
        for (chunk in initialChunks) {
            val matches = numberRegex.findAll(chunk).toList()
            if (matches.size <= 1) {
                segments += chunk
            } else {
                var start = 0
                matches.forEachIndexed { idx, match ->
                    val end = if (idx == matches.lastIndex) chunk.length else match.range.last + 1
                    val sub = chunk.substring(start, end).trim()
                    if (sub.isNotBlank()) segments += sub
                    start = match.range.last + 1
                }
            }
        }

        return segments.mapNotNull { segment -> parseSingleSegment(segment, rawInput) }
    }

    private fun parseSingleSegment(segment: String, rawFull: String): TransactionDraft? {
        val numberMatch = numberRegex.find(segment) ?: return null
        val amount = numberMatch.value.replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val lower = segment.lowercase(Locale.US)
        val isIncome = incomeKeywords.any { kw -> lower.contains(kw) }
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val category = detectCategory(lower, type)
        val cleanedWords = segment
            .replace(numberMatch.value, " ")
            .split(Regex("""\s+"""))
            .map { it.trim(',', '.', ':', ';') }
            .filter { it.isNotBlank() && it.lowercase(Locale.US) !in noiseWords }

        val description = if (cleanedWords.isNotEmpty()) {
            cleanedWords.joinToString(" ").replaceFirstChar { it.titlecase(Locale.US) }
        } else {
            category
        }

        return TransactionDraft(
            type = type,
            amount = CurrencyFormat.round2(amount),
            category = category,
            description = description,
            rawVoiceText = rawFull,
        )
    }

    private fun detectCategory(lower: String, type: TransactionType): String {
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { kw -> lower.contains(kw) }) {
                if (type == TransactionType.INCOME && category !in listOf("Salary", "Freelance", "Bonus")) {
                    return "Other Income"
                }
                return category
            }
        }
        return if (type == TransactionType.INCOME) "Salary" else "Other"
    }

    private fun normalizeBanglaDigits(input: String): String =
        buildString(input.length) {
            for (ch in input) {
                append(banglaDigits[ch] ?: ch)
            }
        }
}
