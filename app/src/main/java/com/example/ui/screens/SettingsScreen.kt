package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ThemeMode
import com.example.domain.DateUtils
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    state: MainUiState,
    viewModel: AppViewModel,
) {
    val P = LocalGlassPalette.current
    val context = LocalContext.current
    val s = state.settings

    var showClearConfirm by remember { mutableStateOf(false) }
    var dailyHourText by remember(s.dailyReminderHour) { mutableStateOf(s.dailyReminderHour.toString().padStart(2, '0')) }
    var dailyMinText by remember(s.dailyReminderMinute) { mutableStateOf(s.dailyReminderMinute.toString().padStart(2, '0')) }
    var morningHourText by remember(s.morningReminderHour) { mutableStateOf(s.morningReminderHour.toString().padStart(2, '0')) }
    var morningMinText by remember(s.morningReminderMinute) { mutableStateOf(s.morningReminderMinute.toString().padStart(2, '0')) }
    var tokenLimitText by remember(s.aiTokenLimit) {
        mutableStateOf(if (s.aiTokenLimit > 0) s.aiTokenLimit.toString() else "")
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        } else {
            viewModel.showStatus("Notification permission denied in Android settings.")
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restoreBackup(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = P.ink)
        }

        // Theme Mode (Electric Yellow Accent Card)
        item {
            GlassCard(
                tint = P.heroYellow,
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PillTag(text = "APPEARANCE THEME", containerColor = P.obsidian, contentColor = P.heroYellow)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        GlassChip(
                            label = mode.label.uppercase(),
                            selected = s.themeMode == mode,
                            accent = P.obsidian,
                            onClick = { viewModel.setThemeMode(mode) },
                            modifier = Modifier.weight(1f).testTag("theme_${mode.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        // Notifications
        item {
            GlassCard(cornerRadius = 26.dp, padding = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
                PillTag(text = "NOTIFICATIONS", containerColor = P.marigold, contentColor = P.obsidian)
                Spacer(Modifier.height(10.dp))
                SettingSwitchRow(
                    title = "Enable Reminders",
                    subtitle = "Local alerts for check-ins, plans, loans, and budgets",
                    checked = s.notificationsEnabled,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.setNotificationsEnabled(true)
                            } else {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.setNotificationsEnabled(wantEnable)
                        }
                    },
                )
                if (s.notificationsEnabled) {
                    SettingSwitchRow("Daily Check-In Reminder", "Fires if nothing logged today", s.dailyReminderEnabled, viewModel::setDailyReminderEnabled)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Daily Time:", style = MaterialTheme.typography.bodyMedium, color = P.subInk)
                        TimeField(dailyHourText, { dailyHourText = it }, "HH", Modifier.width(68.dp))
                        TimeField(dailyMinText, { dailyMinText = it }, "MM", Modifier.width(68.dp))
                        TextButton(onClick = {
                            val h = dailyHourText.toIntOrNull() ?: 21
                            val m = dailyMinText.toIntOrNull() ?: 0
                            viewModel.setDailyReminderTime(h, m)
                            viewModel.showStatus("Daily reminder time saved.")
                        }) { Text("SET", color = P.primary, fontWeight = FontWeight.Bold) }
                    }

                    SettingSwitchRow("Recurring Due Alerts", "Notify on scheduled rule dates", s.recurringReminderEnabled, viewModel::setRecurringReminderEnabled)
                    SettingSwitchRow("Loan Due Alerts", "Day before and on due date", s.loanReminderEnabled, viewModel::setLoanReminderEnabled)
                    SettingSwitchRow("Plan Target Alerts", "Notify on item target dates", s.planReminderEnabled, viewModel::setPlanReminderEnabled)
                    SettingSwitchRow("Budget Alerts (80% & 100%)", "Alert when monthly/category limit nears", s.budgetAlertsEnabled, viewModel::setBudgetAlertsEnabled)
                    SettingSwitchRow("Hide Amounts in Alerts", "Privacy-safe notification text", s.hideNotificationAmounts, viewModel::setHideNotificationAmounts)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Morning Time:", style = MaterialTheme.typography.bodyMedium, color = P.subInk)
                        TimeField(morningHourText, { morningHourText = it }, "HH", Modifier.width(68.dp))
                        TimeField(morningMinText, { morningMinText = it }, "MM", Modifier.width(68.dp))
                        TextButton(onClick = {
                            val h = morningHourText.toIntOrNull() ?: 9
                            val m = morningMinText.toIntOrNull() ?: 0
                            viewModel.setMorningReminderTime(h, m)
                            viewModel.showStatus("Morning reminder time saved.")
                        }) { Text("SET", color = P.primary, fontWeight = FontWeight.Bold) }
                    }

                    Spacer(Modifier.height(8.dp))
                    GlassPrimaryButton(
                        text = "TEST NOTIFICATION (5s)",
                        icon = Icons.Filled.NotificationsActive,
                        containerColor = P.marigold,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.sendTestNotification()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_test_notification"),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Note: Some OEM battery savers (Xiaomi, Oppo, Samsung) may delay alarms unless TakaFlow is exempted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.mutedInk,
                    )
                }
            }
        }

        // AI Agent Settings (Electric Violet Accent)
        item {
            GlassCard(cornerRadius = 26.dp, padding = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
                PillTag(text = "AI AGENT (GEMINI)", containerColor = P.electricViolet, contentColor = Color.White)
                Spacer(Modifier.height(10.dp))
                SettingSwitchRow(
                    title = "Enable AI Agent",
                    subtitle = "Offline manual & voice flows work even when off",
                    checked = s.aiAgentEnabled,
                    onCheckedChange = viewModel::setAiAgentEnabled,
                )
                SettingSwitchRow(
                    title = "Spoken Replies (TTS)",
                    subtitle = "Read AI responses aloud",
                    checked = s.aiVoiceReplyEnabled,
                    onCheckedChange = viewModel::setAiVoiceReplyEnabled,
                )
                Text(
                    "Privacy: Only the AI Agent screen sends queries and ledger summary to Gemini via Firebase AI Logic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = P.subInk,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(
                        value = tokenLimitText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) tokenLimitText = it },
                        label = "Token Budget (Blank = 250K EST.)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        viewModel.setAiTokenLimit(tokenLimitText.toLongOrNull() ?: 0L)
                        viewModel.showStatus("AI token limit updated.")
                    }) { Text("SAVE", color = P.primary, fontWeight = FontWeight.Bold) }
                }
                TextButton(onClick = { viewModel.resetAiUsage() }) {
                    Text("RESET TOKEN COUNTER", color = P.subInk)
                }
            }
        }

        // App Lock
        item {
            GlassCard(cornerRadius = 26.dp, padding = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
                PillTag(text = "SECURITY", containerColor = P.obsidian, contentColor = P.pureWhite)
                Spacer(Modifier.height(10.dp))
                SettingSwitchRow(
                    title = "Biometric App Lock",
                    subtitle = "Require fingerprint, face, or device PIN on launch",
                    checked = s.appLockEnabled,
                    onCheckedChange = viewModel::setAppLockEnabled,
                )
            }
        }

        // Complete Backup & Restore (Acid Lime Card)
        item {
            GlassCard(
                tint = P.acidLime,
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PillTag(text = "COMPLETE BACKUP & RESTORE", containerColor = P.obsidian, contentColor = P.acidLime)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Includes wallets, transactions, plans & loans, recurring rules, custom categories, budgets, check-ins, favorites, notification settings, and theme.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = P.obsidian.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPrimaryButton(
                        text = "EXPORT JSON",
                        icon = Icons.Filled.CloudUpload,
                        containerColor = P.obsidian,
                        onClick = {
                            val fileName = "takaflow_backup_${DateUtils.toKey(System.currentTimeMillis())}.json"
                            exportBackupLauncher.launch(fileName)
                        },
                        modifier = Modifier.weight(1f).testTag("settings_export_backup"),
                    )
                    GlassPrimaryButton(
                        text = "RESTORE",
                        icon = Icons.Filled.CloudDownload,
                        containerColor = P.pureWhite,
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f).testTag("settings_restore_backup"),
                    )
                }
            }
        }

        // Danger Zone (Coral Crimson Card)
        item {
            GlassCard(
                tint = P.coralRed,
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PillTag(text = "DATA RESET", containerColor = P.pureWhite, contentColor = P.coralRed)
                Spacer(Modifier.height(12.dp))
                GlassPrimaryButton(
                    text = "CLEAR ALL DATA",
                    icon = Icons.Filled.DeleteForever,
                    containerColor = P.obsidian,
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth().testTag("settings_clear_data"),
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Data?") },
            text = { Text("This deletes all transactions, wallets, plans, loans, recurring rules, budgets, and check-ins. Export a backup first!") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearConfirm = false
                }) { Text("CLEAR ALL", color = P.coralRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val P = LocalGlassPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = P.ink)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = P.subInk)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = P.obsidian,
                checkedTrackColor = P.acidLime,
            ),
        )
    }
}

@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    GlassTextField(
        value = value,
        onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) onValueChange(it) },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
