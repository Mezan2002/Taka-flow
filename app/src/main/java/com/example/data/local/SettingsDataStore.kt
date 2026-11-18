package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "takaflow_prefs")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TOTAL_BALANCE_HIDDEN = booleanPreferencesKey("total_balance_hidden")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val RECURRING_REMINDER_ENABLED = booleanPreferencesKey("recurring_reminder_enabled")
        val LOAN_REMINDER_ENABLED = booleanPreferencesKey("loan_reminder_enabled")
        val PLAN_REMINDER_ENABLED = booleanPreferencesKey("plan_reminder_enabled")
        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")
        val HIDE_NOTIFICATION_AMOUNTS = booleanPreferencesKey("hide_notification_amounts")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val AI_AGENT_ENABLED = booleanPreferencesKey("ai_agent_enabled")
        val AI_VOICE_REPLY_ENABLED = booleanPreferencesKey("ai_voice_reply_enabled")
        val AI_TOKENS_USED = longPreferencesKey("ai_tokens_used")
        val AI_TOKEN_LIMIT = longPreferencesKey("ai_token_limit")
        val UNLOCKED_BADGES_JSON = stringPreferencesKey("unlocked_badges_json")
        val BUDGET_ALERTS_SENT_JSON = stringPreferencesKey("budget_alerts_sent_json")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        val badgesMap = parseLongMap(prefs[Keys.UNLOCKED_BADGES_JSON])
        AppSettings(
            themeMode = themeMode,
            totalBalanceHidden = prefs[Keys.TOTAL_BALANCE_HIDDEN] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: true,
            dailyReminderHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 21,
            dailyReminderMinute = prefs[Keys.DAILY_REMINDER_MINUTE] ?: 0,
            recurringReminderEnabled = prefs[Keys.RECURRING_REMINDER_ENABLED] ?: true,
            loanReminderEnabled = prefs[Keys.LOAN_REMINDER_ENABLED] ?: true,
            planReminderEnabled = prefs[Keys.PLAN_REMINDER_ENABLED] ?: true,
            morningReminderHour = prefs[Keys.MORNING_REMINDER_HOUR] ?: 9,
            morningReminderMinute = prefs[Keys.MORNING_REMINDER_MINUTE] ?: 0,
            hideNotificationAmounts = prefs[Keys.HIDE_NOTIFICATION_AMOUNTS] ?: false,
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            aiAgentEnabled = prefs[Keys.AI_AGENT_ENABLED] ?: true,
            aiVoiceReplyEnabled = prefs[Keys.AI_VOICE_REPLY_ENABLED] ?: true,
            aiTokensUsed = prefs[Keys.AI_TOKENS_USED] ?: 0L,
            aiTokenLimit = prefs[Keys.AI_TOKEN_LIMIT] ?: 0L,
            unlockedBadges = badgesMap,
        )
    }

    val sentBudgetAlertsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.BUDGET_ALERTS_SENT_JSON] ?: return@map emptySet()
        raw.split("|").filter { it.isNotBlank() }.toSet()
    }

    suspend fun markBudgetAlertSent(alertKey: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.BUDGET_ALERTS_SENT_JSON] ?: "")
                .split("|").filter { it.isNotBlank() }.toMutableSet()
            current += alertKey
            prefs[Keys.BUDGET_ALERTS_SENT_JSON] = current.toList().takeLast(120).joinToString("|")
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = update { it[Keys.THEME_MODE] = mode.name }
    suspend fun setTotalBalanceHidden(hidden: Boolean) = update { it[Keys.TOTAL_BALANCE_HIDDEN] = hidden }
    suspend fun setAppLockEnabled(enabled: Boolean) = update { it[Keys.APP_LOCK_ENABLED] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = update { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setDailyReminderEnabled(enabled: Boolean) = update { it[Keys.DAILY_REMINDER_ENABLED] = enabled }
    suspend fun setDailyReminderTime(hour: Int, minute: Int) = update {
        it[Keys.DAILY_REMINDER_HOUR] = hour.coerceIn(0, 23)
        it[Keys.DAILY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
    suspend fun setRecurringReminderEnabled(enabled: Boolean) = update { it[Keys.RECURRING_REMINDER_ENABLED] = enabled }
    suspend fun setLoanReminderEnabled(enabled: Boolean) = update { it[Keys.LOAN_REMINDER_ENABLED] = enabled }
    suspend fun setPlanReminderEnabled(enabled: Boolean) = update { it[Keys.PLAN_REMINDER_ENABLED] = enabled }
    suspend fun setMorningReminderTime(hour: Int, minute: Int) = update {
        it[Keys.MORNING_REMINDER_HOUR] = hour.coerceIn(0, 23)
        it[Keys.MORNING_REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
    suspend fun setHideNotificationAmounts(hide: Boolean) = update { it[Keys.HIDE_NOTIFICATION_AMOUNTS] = hide }
    suspend fun setBudgetAlertsEnabled(enabled: Boolean) = update { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
    suspend fun setAiAgentEnabled(enabled: Boolean) = update { it[Keys.AI_AGENT_ENABLED] = enabled }
    suspend fun setAiVoiceReplyEnabled(enabled: Boolean) = update { it[Keys.AI_VOICE_REPLY_ENABLED] = enabled }
    suspend fun setAiTokenLimit(limit: Long) = update { it[Keys.AI_TOKEN_LIMIT] = limit.coerceAtLeast(0L) }
    suspend fun addAiTokensUsed(delta: Long) = update {
        it[Keys.AI_TOKENS_USED] = (it[Keys.AI_TOKENS_USED] ?: 0L) + delta.coerceAtLeast(0L)
    }
    suspend fun resetAiUsage() = update { it[Keys.AI_TOKENS_USED] = 0L }

    suspend fun recordBadgeUnlocks(newIds: List<String>, timestamp: Long = System.currentTimeMillis()) {
        if (newIds.isEmpty()) return
        update { prefs ->
            val map = parseLongMap(prefs[Keys.UNLOCKED_BADGES_JSON]).toMutableMap()
            newIds.forEach { id -> map.putIfAbsent(id, timestamp) }
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            prefs[Keys.UNLOCKED_BADGES_JSON] = json.toString()
        }
    }

    suspend fun restoreFromBackup(settings: AppSettings) {
        update { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.TOTAL_BALANCE_HIDDEN] = settings.totalBalanceHidden
            prefs[Keys.APP_LOCK_ENABLED] = settings.appLockEnabled
            prefs[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[Keys.DAILY_REMINDER_ENABLED] = settings.dailyReminderEnabled
            prefs[Keys.DAILY_REMINDER_HOUR] = settings.dailyReminderHour
            prefs[Keys.DAILY_REMINDER_MINUTE] = settings.dailyReminderMinute
            prefs[Keys.RECURRING_REMINDER_ENABLED] = settings.recurringReminderEnabled
            prefs[Keys.LOAN_REMINDER_ENABLED] = settings.loanReminderEnabled
            prefs[Keys.PLAN_REMINDER_ENABLED] = settings.planReminderEnabled
            prefs[Keys.MORNING_REMINDER_HOUR] = settings.morningReminderHour
            prefs[Keys.MORNING_REMINDER_MINUTE] = settings.morningReminderMinute
            prefs[Keys.HIDE_NOTIFICATION_AMOUNTS] = settings.hideNotificationAmounts
            prefs[Keys.BUDGET_ALERTS_ENABLED] = settings.budgetAlertsEnabled
            prefs[Keys.AI_AGENT_ENABLED] = settings.aiAgentEnabled
            prefs[Keys.AI_VOICE_REPLY_ENABLED] = settings.aiVoiceReplyEnabled
            prefs[Keys.AI_TOKEN_LIMIT] = settings.aiTokenLimit
            val json = JSONObject()
            settings.unlockedBadges.forEach { (k, v) -> json.put(k, v) }
            prefs[Keys.UNLOCKED_BADGES_JSON] = json.toString()
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun parseLongMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k] = obj.optLong(k, 0L)
            }
            result
        }.getOrDefault(emptyMap())
    }
}
