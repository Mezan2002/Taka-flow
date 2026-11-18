package com.example.services

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.toEntity
import com.example.data.model.AppSettings
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.CheckinDay
import com.example.data.model.CheckinSource
import com.example.data.model.LoanRepayment
import com.example.data.model.PlanKind
import com.example.data.model.PurchasePlan
import com.example.data.model.QuickFavorite
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRule
import com.example.data.model.ThemeMode
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupService(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsStore: SettingsDataStore,
) {
    suspend fun exportToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Unable to open destination file.")
            val txCount = json.optJSONArray("transactions")?.length() ?: 0
            txCount
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Unable to read backup file.")
            val root = JSONObject(raw)
            val walletsArr = root.optJSONArray("wallets")
            if (walletsArr == null || walletsArr.length() == 0) {
                error("Backup blocked: file contains no wallets.")
            }

            val wallets = parseWallets(walletsArr)
            val transactions = parseTransactions(root.optJSONArray("transactions"))
            val categories = parseCategories(root.optJSONArray("categories"))
            val plans = parsePlans(root.optJSONArray("purchasePlans"))
            val repayments = parseRepayments(root.optJSONArray("purchasePlans"), root.optJSONArray("loanRepayments"))
            val recurring = parseRecurring(root.optJSONArray("recurringRules"))
            val budgets = parseBudgets(root.optJSONArray("budgets"))
            val checkins = parseCheckins(root.optJSONArray("checkins"))
            val favorites = parseFavorites(root.optJSONArray("quickFavorites"))
            val restoredSettings = parseSettings(root.optJSONObject("settings"))

            db.withTransaction {
                db.transactionDao().deleteAll()
                db.planDao().deleteAllRepayments()
                db.planDao().deleteAllPlans()
                db.recurringDao().deleteAll()
                db.budgetDao().deleteAll()
                db.checkinDao().deleteAll()
                db.walletDao().deleteAll()
                db.walletDao().upsertAll(wallets.map { it.toEntity() })
                if (transactions.isNotEmpty()) db.transactionDao().upsertAll(transactions.map { it.toEntity() })
                if (categories.isNotEmpty()) {
                    db.categoryDao().deleteAll()
                    db.categoryDao().upsertAll(categories.map { it.toEntity() })
                }
                if (plans.isNotEmpty()) db.planDao().upsertPlans(plans.map { it.toEntity() })
                if (repayments.isNotEmpty()) db.planDao().upsertRepayments(repayments.map { it.toEntity() })
                if (recurring.isNotEmpty()) db.recurringDao().upsertAll(recurring.map { it.toEntity() })
                if (budgets.isNotEmpty()) db.budgetDao().upsertAll(budgets.map { it.toEntity() })
                if (checkins.isNotEmpty()) db.checkinDao().upsertAll(checkins.map { it.toEntity() })
                if (favorites.isNotEmpty()) {
                    db.quickFavoriteDao().deleteAll()
                    db.quickFavoriteDao().upsertAll(favorites.map { it.toEntity() })
                }
            }
            if (restoredSettings != null) {
                settingsStore.restoreFromBackup(restoredSettings)
            }
            "Restored ${wallets.size} wallets, ${transactions.size} entries, ${plans.size} plans & loans."
        }
    }

    private suspend fun buildBackupJson(): JSONObject {
        val wallets = db.walletDao().getAll().map { it.toDomain() }
        val transactions = db.transactionDao().getAll().map { it.toDomain() }
        val categories = db.categoryDao().getAll().map { it.toDomain() }
        val plans = db.planDao().getAllPlans()
        val repayments = db.planDao().getAllRepayments().groupBy { it.planId }
        val recurring = db.recurringDao().getAll().map { it.toDomain() }
        val budgets = db.budgetDao().getAll().map { it.toDomain() }
        val checkins = db.checkinDao().getAll().map { it.toDomain() }
        val favorites = db.quickFavoriteDao().getAll().map { it.toDomain() }
        val settings = settingsStore.settingsFlow.first()

        return JSONObject().apply {
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("wallets", JSONArray().apply {
                wallets.forEach { w ->
                    put(JSONObject().apply {
                        put("id", w.id)
                        put("name", w.name)
                        put("balance", w.balance)
                        put("colorHex", w.colorHex)
                        put("iconKey", w.iconKey)
                        put("isHidden", w.isHidden)
                        put("sortOrder", w.sortOrder)
                    })
                }
            })
            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("type", t.type.name)
                        put("amount", t.amount)
                        put("category", t.category)
                        put("description", t.description)
                        put("timestamp", t.timestamp)
                        put("walletId", t.walletId)
                        t.toWalletId?.let { put("toWalletId", it) }
                        t.recurringId?.let { put("recurringId", it) }
                        t.rawVoiceText?.let { put("rawVoiceText", it) }
                    })
                }
            })
            put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("type", c.type.name)
                        put("iconKey", c.iconKey)
                        put("colorHex", c.colorHex)
                        put("isCustom", c.isCustom)
                    })
                }
            })
            put("purchasePlans", JSONArray().apply {
                plans.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("kind", p.kind)
                        put("name", p.name)
                        put("personName", p.personName)
                        put("expectedAmount", p.expectedAmount)
                        put("category", p.category)
                        put("note", p.note)
                        p.targetDate?.let { put("targetDate", it) }
                        p.dueDate?.let { put("dueDate", it) }
                        put("completed", p.completed)
                        p.completedAt?.let { put("completedAt", it) }
                        p.actualAmount?.let { put("actualAmount", it) }
                        p.walletId?.let { put("walletId", it) }
                        put("createdAt", p.createdAt)
                        put("repayments", JSONArray().apply {
                            repayments[p.id].orEmpty().forEach { r ->
                                put(JSONObject().apply {
                                    put("id", r.id)
                                    put("planId", r.planId)
                                    put("amount", r.amount)
                                    put("note", r.note)
                                    put("paidAt", r.paidAt)
                                })
                            }
                        })
                    })
                }
            })
            put("recurringRules", JSONArray().apply {
                recurring.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("type", r.type.name)
                        put("amount", r.amount)
                        put("category", r.category)
                        put("description", r.description)
                        put("walletId", r.walletId)
                        put("frequency", r.frequency.name)
                        put("startDate", r.startDate)
                        r.endDate?.let { put("endDate", it) }
                        put("active", r.active)
                        r.lastGeneratedDate?.let { put("lastGeneratedDate", it) }
                        put("createdAt", r.createdAt)
                    })
                }
            })
            put("budgets", JSONArray().apply {
                budgets.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        b.categoryName?.let { put("categoryName", it) }
                        put("limitAmount", b.limitAmount)
                    })
                }
            })
            put("checkins", JSONArray().apply {
                checkins.forEach { c ->
                    put(JSONObject().apply {
                        put("dateKey", c.dateKey)
                        put("source", c.source.name)
                        put("createdAt", c.createdAt)
                    })
                }
            })
            put("quickFavorites", JSONArray().apply {
                favorites.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("label", f.label)
                        put("amount", f.amount)
                        put("type", f.type.name)
                        put("category", f.category)
                        put("walletId", f.walletId)
                        put("sortOrder", f.sortOrder)
                    })
                }
            })
            put("settings", JSONObject().apply {
                put("themeMode", settings.themeMode.name)
                put("totalBalanceHidden", settings.totalBalanceHidden)
                put("appLockEnabled", settings.appLockEnabled)
                put("notificationsEnabled", settings.notificationsEnabled)
                put("dailyReminderEnabled", settings.dailyReminderEnabled)
                put("dailyReminderHour", settings.dailyReminderHour)
                put("dailyReminderMinute", settings.dailyReminderMinute)
                put("recurringReminderEnabled", settings.recurringReminderEnabled)
                put("loanReminderEnabled", settings.loanReminderEnabled)
                put("planReminderEnabled", settings.planReminderEnabled)
                put("morningReminderHour", settings.morningReminderHour)
                put("morningReminderMinute", settings.morningReminderMinute)
                put("hideNotificationAmounts", settings.hideNotificationAmounts)
                put("budgetAlertsEnabled", settings.budgetAlertsEnabled)
                put("aiAgentEnabled", settings.aiAgentEnabled)
                put("aiVoiceReplyEnabled", settings.aiVoiceReplyEnabled)
                put("aiTokenLimit", settings.aiTokenLimit)
                put("unlockedBadges", JSONObject().apply {
                    settings.unlockedBadges.forEach { (k, v) -> put(k, v) }
                })
            })
        }
    }

    private fun parseWallets(arr: JSONArray): List<Wallet> =
        (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Wallet(
                id = o.getString("id"),
                name = o.getString("name"),
                balance = o.optDouble("balance", 0.0),
                colorHex = o.optString("colorHex", "#FBBF24"),
                iconKey = o.optString("iconKey", "cash"),
                isHidden = o.optBoolean("isHidden", false),
                sortOrder = o.optInt("sortOrder", idx),
            )
        }

    private fun parseTransactions(arr: JSONArray?): List<Transaction> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Transaction(
                id = o.getString("id"),
                type = runCatching { TransactionType.valueOf(o.getString("type").uppercase()) }
                    .getOrDefault(TransactionType.EXPENSE),
                amount = o.getDouble("amount"),
                category = o.optString("category", "Other"),
                description = o.optString("description", ""),
                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                walletId = o.optString("walletId", "wallet_cash"),
                toWalletId = if (o.has("toWalletId") && !o.isNull("toWalletId")) o.getString("toWalletId") else null,
                recurringId = if (o.has("recurringId") && !o.isNull("recurringId")) o.getString("recurringId") else null,
                rawVoiceText = if (o.has("rawVoiceText") && !o.isNull("rawVoiceText")) o.getString("rawVoiceText") else null,
            )
        }
    }

    private fun parseCategories(arr: JSONArray?): List<Category> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Category(
                id = o.getString("id"),
                name = o.getString("name"),
                type = runCatching { TransactionType.valueOf(o.getString("type").uppercase()) }
                    .getOrDefault(TransactionType.EXPENSE),
                iconKey = o.optString("iconKey", "category"),
                colorHex = o.optString("colorHex", "#38BDF8"),
                isCustom = o.optBoolean("isCustom", true),
            )
        }
    }

    private fun parsePlans(arr: JSONArray?): List<PurchasePlan> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            PurchasePlan(
                id = o.getString("id"),
                kind = runCatching { PlanKind.valueOf(o.optString("kind", "ITEM").uppercase()) }
                    .getOrDefault(PlanKind.ITEM),
                name = o.optString("name", ""),
                personName = o.optString("personName", ""),
                expectedAmount = o.optDouble("expectedAmount", 0.0),
                category = o.optString("category", "Shopping"),
                note = o.optString("note", ""),
                targetDate = if (o.has("targetDate") && !o.isNull("targetDate")) o.getLong("targetDate") else null,
                dueDate = if (o.has("dueDate") && !o.isNull("dueDate")) o.getLong("dueDate") else null,
                completed = o.optBoolean("completed", false),
                completedAt = if (o.has("completedAt") && !o.isNull("completedAt")) o.getLong("completedAt") else null,
                actualAmount = if (o.has("actualAmount") && !o.isNull("actualAmount")) o.getDouble("actualAmount") else null,
                walletId = if (o.has("walletId") && !o.isNull("walletId")) o.getString("walletId") else null,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }

    private fun parseRepayments(plansArr: JSONArray?, flatArr: JSONArray?): List<LoanRepayment> {
        val list = mutableListOf<LoanRepayment>()
        if (plansArr != null) {
            for (i in 0 until plansArr.length()) {
                val p = plansArr.getJSONObject(i)
                val reps = p.optJSONArray("repayments") ?: continue
                for (j in 0 until reps.length()) {
                    val r = reps.getJSONObject(j)
                    list += LoanRepayment(
                        id = r.getString("id"),
                        planId = r.optString("planId", p.getString("id")),
                        amount = r.getDouble("amount"),
                        note = r.optString("note", ""),
                        paidAt = r.optLong("paidAt", System.currentTimeMillis()),
                    )
                }
            }
        }
        if (flatArr != null) {
            for (i in 0 until flatArr.length()) {
                val r = flatArr.getJSONObject(i)
                list += LoanRepayment(
                    id = r.getString("id"),
                    planId = r.getString("planId"),
                    amount = r.getDouble("amount"),
                    note = r.optString("note", ""),
                    paidAt = r.optLong("paidAt", System.currentTimeMillis()),
                )
            }
        }
        return list.distinctBy { it.id }
    }

    private fun parseRecurring(arr: JSONArray?): List<RecurringRule> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            RecurringRule(
                id = o.getString("id"),
                type = runCatching { TransactionType.valueOf(o.getString("type").uppercase()) }
                    .getOrDefault(TransactionType.EXPENSE),
                amount = o.getDouble("amount"),
                category = o.optString("category", "Bills"),
                description = o.optString("description", ""),
                walletId = o.optString("walletId", "wallet_cash"),
                frequency = runCatching { RecurringFrequency.valueOf(o.getString("frequency").uppercase()) }
                    .getOrDefault(RecurringFrequency.MONTHLY),
                startDate = o.optLong("startDate", System.currentTimeMillis()),
                endDate = if (o.has("endDate") && !o.isNull("endDate")) o.getLong("endDate") else null,
                active = o.optBoolean("active", true),
                lastGeneratedDate = if (o.has("lastGeneratedDate") && !o.isNull("lastGeneratedDate")) o.getLong("lastGeneratedDate") else null,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }

    private fun parseBudgets(arr: JSONArray?): List<Budget> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            Budget(
                id = o.getString("id"),
                categoryName = if (o.has("categoryName") && !o.isNull("categoryName")) o.getString("categoryName") else null,
                limitAmount = o.getDouble("limitAmount"),
            )
        }
    }

    private fun parseCheckins(arr: JSONArray?): List<CheckinDay> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            CheckinDay(
                dateKey = o.getString("dateKey"),
                source = runCatching { CheckinSource.valueOf(o.optString("source", "NO_SPEND").uppercase()) }
                    .getOrDefault(CheckinSource.NO_SPEND),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }

    private fun parseFavorites(arr: JSONArray?): List<QuickFavorite> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val o = arr.getJSONObject(idx)
            QuickFavorite(
                id = o.getString("id"),
                label = o.getString("label"),
                amount = o.getDouble("amount"),
                type = runCatching { TransactionType.valueOf(o.optString("type", "EXPENSE").uppercase()) }
                    .getOrDefault(TransactionType.EXPENSE),
                category = o.optString("category", "Food"),
                walletId = o.optString("walletId", "wallet_cash"),
                sortOrder = o.optInt("sortOrder", idx),
            )
        }
    }

    private fun parseSettings(o: JSONObject?): AppSettings? {
        if (o == null) return null
        val badgesObj = o.optJSONObject("unlockedBadges")
        val badges = mutableMapOf<String, Long>()
        if (badgesObj != null) {
            val keys = badgesObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                badges[k] = badgesObj.optLong(k, System.currentTimeMillis())
            }
        }
        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(o.optString("themeMode", "SYSTEM").uppercase()) }
                .getOrDefault(ThemeMode.SYSTEM),
            totalBalanceHidden = o.optBoolean("totalBalanceHidden", false),
            appLockEnabled = o.optBoolean("appLockEnabled", false),
            notificationsEnabled = o.optBoolean("notificationsEnabled", true),
            dailyReminderEnabled = o.optBoolean("dailyReminderEnabled", true),
            dailyReminderHour = o.optInt("dailyReminderHour", 21),
            dailyReminderMinute = o.optInt("dailyReminderMinute", 0),
            recurringReminderEnabled = o.optBoolean("recurringReminderEnabled", true),
            loanReminderEnabled = o.optBoolean("loanReminderEnabled", true),
            planReminderEnabled = o.optBoolean("planReminderEnabled", true),
            morningReminderHour = o.optInt("morningReminderHour", 9),
            morningReminderMinute = o.optInt("morningReminderMinute", 0),
            hideNotificationAmounts = o.optBoolean("hideNotificationAmounts", false),
            budgetAlertsEnabled = o.optBoolean("budgetAlertsEnabled", true),
            aiAgentEnabled = o.optBoolean("aiAgentEnabled", true),
            aiVoiceReplyEnabled = o.optBoolean("aiVoiceReplyEnabled", true),
            aiTokenLimit = o.optLong("aiTokenLimit", 0L),
            unlockedBadges = badges,
        )
    }
}
