package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CURRENCY
import com.example.data.model.Category
import com.example.data.model.RecurringFrequency
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.RecurringEngine
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.components.SwipeableGlassRow
import com.example.ui.components.contrastInkFor
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState
import java.util.UUID

@Composable
fun RecurringScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onSaveRule: (RecurringRule) -> Unit,
    onToggleActive: (RecurringRule) -> Unit,
    onDeleteRule: (RecurringRule) -> Unit,
) {
    val P = LocalGlassPalette.current
    var editingRule by remember { mutableStateOf<RecurringRule?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val ruleColors = listOf(P.electricViolet, P.marigold, P.acidLime, P.obsidian)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = P.ink)
                    }
                    Text("Recurring", style = MaterialTheme.typography.headlineMedium, color = P.ink)
                }
                GlassPrimaryButton(
                    text = "ADD",
                    icon = Icons.Filled.Add,
                    onClick = {
                        editingRule = null
                        showDialog = true
                    },
                    modifier = Modifier.width(104.dp).testTag("recurring_add_button"),
                )
            }
        }

        if (state.recurringRules.isEmpty()) {
            item {
                GlassCard(tint = P.heroYellow, cornerRadius = 26.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No recurring rules yet. Automate rent, salary, internet bills, or subscriptions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.obsidian,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else {
            itemsIndexed(state.recurringRules, key = { _, r -> r.id }) { idx, rule ->
                val nextDue = remember(rule) { RecurringEngine.computeNextDue(rule) }
                val blockTint = if (rule.active) ruleColors[idx % ruleColors.size] else P.glassSurface
                val ink = if (rule.active) contrastInkFor(blockTint) else P.subInk

                SwipeableGlassRow(
                    onEdit = {
                        editingRule = rule
                        showDialog = true
                    },
                    onDelete = { onDeleteRule(rule) },
                ) {
                    GlassCard(
                        tint = blockTint,
                        cornerRadius = 26.dp,
                        padding = PaddingValues(18.dp),
                        onClick = {
                            editingRule = rule
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PillTag(
                                text = rule.frequency.label.uppercase(),
                                containerColor = P.pureWhite,
                                contentColor = P.obsidian,
                            )
                            Switch(
                                checked = rule.active,
                                onCheckedChange = { onToggleActive(rule) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = P.obsidian,
                                    checkedTrackColor = P.pureWhite,
                                ),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rule.description.ifBlank { rule.category },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ink,
                                )
                                Text(
                                    "${rule.category}${nextDue?.let { " · Next ${DateUtils.formatShortDate(it)}" } ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ink.copy(alpha = 0.8f),
                                )
                            }
                            Text(
                                text = "${if (rule.type == TransactionType.INCOME) "+" else "-"}${CurrencyFormat.formatAmount(rule.amount)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = ink,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        RecurringRuleDialog(
            editing = editingRule,
            wallets = state.wallets,
            categories = state.categories,
            onDismiss = { showDialog = false },
            onSave = { rule ->
                onSaveRule(rule)
                showDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringRuleDialog(
    editing: RecurringRule?,
    wallets: List<Wallet>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (RecurringRule) -> Unit,
) {
    val P = LocalGlassPalette.current
    var type by remember(editing) { mutableStateOf(editing?.type ?: TransactionType.EXPENSE) }
    var amountText by remember(editing) {
        mutableStateOf(editing?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var description by remember(editing) { mutableStateOf(editing?.description ?: "") }
    var category by remember(editing) { mutableStateOf(editing?.category ?: "Bills") }
    var frequency by remember(editing) { mutableStateOf(editing?.frequency ?: RecurringFrequency.MONTHLY) }
    var walletId by remember(editing) { mutableStateOf(editing?.walletId ?: wallets.firstOrNull()?.id ?: "wallet_cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "New Recurring Rule" else "Edit Recurring Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(label = "EXPENSE", selected = type == TransactionType.EXPENSE, accent = P.coralRed, onClick = { type = TransactionType.EXPENSE }, modifier = Modifier.weight(1f))
                    GlassChip(label = "INCOME", selected = type == TransactionType.INCOME, accent = P.acidLime, onClick = { type = TransactionType.INCOME }, modifier = Modifier.weight(1f))
                }
                GlassTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                    label = "Amount ($CURRENCY)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                GlassTextField(value = description, onValueChange = { description = it }, label = "Description", placeholder = "e.g., House Rent")
                Text("FREQUENCY", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurringFrequency.entries.forEach { freq ->
                        GlassChip(label = freq.label, selected = frequency == freq, onClick = { frequency = freq })
                    }
                }
                Text("CATEGORY", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.filter { it.type == type }.take(6).forEach { c ->
                        GlassChip(label = c.name, selected = category == c.name, onClick = { category = c.name })
                    }
                }
                Text("WALLET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    wallets.forEach { w ->
                        GlassChip(label = w.name, selected = walletId == w.id, onClick = { walletId = w.id })
                    }
                }
            }
        },
        confirmButton = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = amt > 0.0,
                onClick = {
                    onSave(
                        RecurringRule(
                            id = editing?.id ?: "rec_${UUID.randomUUID()}",
                            type = type,
                            amount = CurrencyFormat.round2(amt),
                            category = category,
                            description = description.trim().ifBlank { category },
                            walletId = walletId,
                            frequency = frequency,
                            startDate = editing?.startDate ?: System.currentTimeMillis(),
                            endDate = editing?.endDate,
                            active = editing?.active ?: true,
                            lastGeneratedDate = editing?.lastGeneratedDate,
                            createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                        )
                    )
                },
            ) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}
