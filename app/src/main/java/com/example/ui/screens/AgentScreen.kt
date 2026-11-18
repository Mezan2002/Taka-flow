package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.SettingsDataStore
import com.example.data.model.CURRENCY
import com.example.services.PendingAgentAction
import com.example.services.SpeechVoiceManager
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassSecondaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.ActionStatus
import com.example.ui.viewmodel.AgentChatMessage
import com.example.ui.viewmodel.AgentUiState
import com.example.ui.viewmodel.AgentViewModel
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    appState: MainUiState,
    agentState: AgentUiState,
    agentViewModel: AgentViewModel,
    settingsStore: SettingsDataStore,
    speechManager: SpeechVoiceManager,
    onToggleVoiceReply: (Boolean) -> Unit,
    onExecuteAction: (PendingAgentAction) -> String,
    onBackExit: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<AgentChatMessage?>(null) }

    BackHandler {
        if (!agentState.showGuide) {
            agentViewModel.openGuide()
        } else {
            onBackExit()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            agentViewModel.setListening(true)
            speechManager.startListening(
                onPartial = { agentViewModel.setListening(true, it) },
                onFinal = { text ->
                    agentViewModel.setListening(false)
                    agentViewModel.sendPrompt(text, appState, settingsStore, speechManager)
                },
                onError = {
                    agentViewModel.setListening(false)
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!agentState.showGuide) agentViewModel.openGuide() else onBackExit()
                    },
                    modifier = Modifier.testTag("agent_back_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = P.ink)
                }
                Column {
                    Text("AI Agent", style = MaterialTheme.typography.titleLarge, color = P.ink)
                    Text(
                        if (appState.settings.aiAgentEnabled) "Gemini 3.8 Flash · Preview Mode" else "Disabled in Settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (appState.settings.aiAgentEnabled) P.electricViolet else P.coralRed,
                    )
                }
            }
            if (!agentState.showGuide) {
                Row {
                    IconButton(onClick = { agentViewModel.openGuide() }) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = "Open Guide", tint = P.subInk)
                    }
                    IconButton(
                        onClick = {
                            val next = !appState.settings.aiVoiceReplyEnabled
                            if (!next) speechManager.stopSpeaking()
                            onToggleVoiceReply(next)
                        },
                        modifier = Modifier.testTag("agent_speaker_toggle"),
                    ) {
                        Icon(
                            imageVector = if (appState.settings.aiVoiceReplyEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = "Toggle spoken replies",
                            tint = if (appState.settings.aiVoiceReplyEnabled) P.electricViolet else P.mutedInk,
                        )
                    }
                    IconButton(onClick = { agentViewModel.clearChat() }, modifier = Modifier.testTag("agent_clear_chat")) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat", tint = P.subInk)
                    }
                }
            }
        }

        // Token Usage Bar in Obsidian Bento Card
        val used = appState.settings.aiTokensUsed
        val limit = if (appState.settings.aiTokenLimit > 0) appState.settings.aiTokenLimit else 250_000L
        val isEst = appState.settings.aiTokenLimit <= 0
        val remaining = (limit - used).coerceAtLeast(0L)
        val frac = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

        GlassCard(
            tint = P.obsidian,
            cornerRadius = 18.dp,
            padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("USED: ${formatTokenCount(used)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                Text(
                    "LEFT: ${formatTokenCount(remaining)}${if (isEst) " (EST. 250K)" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = P.acidLime,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                color = P.heroYellow,
                trackColor = Color.White.copy(alpha = 0.16f),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (agentState.showGuide) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    GlassCard(tint = P.electricViolet, cornerRadius = 26.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = P.heroYellow)
                            Text("What I Can Do (Preview First)", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Speak or type in English, Bangla, or Banglish. Every change shows a Preview & Edit Card for your confirmation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }

                item {
                    GlassCard(tint = P.acidLime, cornerRadius = 26.dp, modifier = Modifier.fillMaxWidth()) {
                        PillTag(text = "READY", containerColor = P.obsidian, contentColor = P.acidLime)
                        Spacer(Modifier.height(10.dp))
                        val readyItems = listOf(
                            "Lookups & Summaries (spending, balances, top categories)",
                            "Transactions (add or delete income & expenses)",
                            "Wallets & Transfers (create wallets, transfer funds)",
                            "Budgets & Categories (set limits, add/delete categories)",
                            "Recurring Rules (add, pause/resume, delete)",
                            "PURCHASE PLANS (add item plans, mark bought)",
                        )
                        readyItems.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodyMedium, color = P.obsidian, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        PillTag(text = "COMING SOON", containerColor = P.pureWhite, contentColor = P.obsidian)
                        Spacer(Modifier.height(4.dp))
                        Text("• Loans & Debts management (use Plans & Loans tab for now)", style = MaterialTheme.typography.bodyMedium, color = P.obsidian.copy(alpha = 0.82f))
                    }
                }

                item {
                    GlassCard(tint = P.heroYellow, cornerRadius = 26.dp, modifier = Modifier.fillMaxWidth()) {
                        PillTag(text = "TRY ASKING", containerColor = P.obsidian, contentColor = P.heroYellow)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "How much did I spend this month?",
                                "Add 150 taka lunch from Cash",
                                "Set Food budget 5000",
                                "Add monthly rent 12000 recurring",
                                "Add Smart Watch 4500 purchase plan",
                                "Aajke cha 30 taka khoroch holo",
                            ).forEach { prompt ->
                                GlassChip(
                                    label = prompt,
                                    selected = false,
                                    onClick = {
                                        agentViewModel.sendPrompt(prompt, appState, settingsStore, speechManager)
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GlassPrimaryButton(
                            text = if (agentState.messages.isEmpty()) "START CHAT" else "RESUME CHAT (${agentState.messages.size})",
                            containerColor = P.obsidian,
                            onClick = { agentViewModel.openChat() },
                            modifier = Modifier.fillMaxWidth().testTag("agent_start_chat_button"),
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(agentState.messages, key = { it.id }) { msg ->
                    ChatBubble(
                        msg = msg,
                        onEditAction = { editingMessage = msg },
                        onApproveAction = {
                            agentViewModel.approveAction(
                                messageId = msg.id,
                                executor = onExecuteAction,
                                speechManager = speechManager,
                                speakReply = appState.settings.aiVoiceReplyEnabled,
                            )
                        },
                        onCancelAction = { agentViewModel.cancelAction(msg.id) },
                    )
                }
                if (agentState.isLoading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = P.electricViolet)
                            Text("Thinking...", style = MaterialTheme.typography.bodyMedium, color = P.subInk)
                        }
                    }
                }
            }
        }

        if (agentState.isListening && agentState.partialTranscript.isNotBlank()) {
            Text(
                text = agentState.partialTranscript,
                style = MaterialTheme.typography.bodyMedium,
                color = P.electricViolet,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = {
                    if (agentState.isListening) {
                        speechManager.stopListening()
                        agentViewModel.setListening(false)
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            agentViewModel.setListening(true)
                            speechManager.startListening(
                                onPartial = { agentViewModel.setListening(true, it) },
                                onFinal = { text ->
                                    agentViewModel.setListening(false)
                                    agentViewModel.sendPrompt(text, appState, settingsStore, speechManager)
                                },
                                onError = { agentViewModel.setListening(false) },
                            )
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (agentState.isListening) P.coralRed else P.acidLime)
                    .testTag("agent_mic_button"),
            ) {
                Icon(
                    imageVector = if (agentState.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "Voice prompt",
                    tint = if (agentState.isListening) Color.White else P.obsidian,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                GlassTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = "Ask or command (EN / BN)",
                    placeholder = "e.g., Tea 25 taka from Cash",
                    modifier = Modifier.testTag("agent_text_input"),
                )
            }

            IconButton(
                enabled = inputText.isNotBlank() && !agentState.isLoading,
                onClick = {
                    val prompt = inputText
                    inputText = ""
                    agentViewModel.sendPrompt(prompt, appState, settingsStore, speechManager)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(P.heroYellow)
                    .testTag("agent_send_button"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = P.obsidian)
            }
        }
    }

    editingMessage?.let { msg ->
        val action = msg.proposedAction
        if (action != null) {
            EditPendingActionDialog(
                action = action,
                onDismiss = { editingMessage = null },
                onSave = { updated ->
                    agentViewModel.updatePendingAction(msg.id, updated)
                    editingMessage = null
                },
            )
        }
    }
}

@Composable
private fun ChatBubble(
    msg: AgentChatMessage,
    onEditAction: () -> Unit,
    onApproveAction: () -> Unit,
    onCancelAction: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val bubbleTint = when {
        msg.isUser -> P.electricViolet
        msg.isError -> P.coralRed
        else -> P.glassSurface
    }
    val textColor = when {
        msg.isUser || msg.isError -> Color.White
        else -> P.ink
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start,
    ) {
        GlassCard(
            tint = bubbleTint,
            cornerRadius = 22.dp,
            padding = PaddingValues(14.dp),
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )

            val action = msg.proposedAction
            if (action != null) {
                Spacer(Modifier.height(10.dp))
                GlassCard(
                    tint = P.heroYellow,
                    cornerRadius = 18.dp,
                    padding = PaddingValues(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PillTag(text = "PREVIEW ACTION", containerColor = P.obsidian, contentColor = P.heroYellow)
                    Spacer(Modifier.height(8.dp))
                    Text(action.summaryTitle, style = MaterialTheme.typography.titleMedium, color = P.obsidian)
                    Text(action.summarySubtitle, style = MaterialTheme.typography.bodyMedium, color = P.obsidian.copy(alpha = 0.8f))
                    Spacer(Modifier.height(10.dp))
                    when (msg.actionStatus) {
                        ActionStatus.PENDING -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlassSecondaryButton(
                                    text = "EDIT",
                                    icon = Icons.Filled.Edit,
                                    onClick = onEditAction,
                                    modifier = Modifier.weight(1f),
                                )
                                GlassSecondaryButton(
                                    text = "CANCEL",
                                    icon = Icons.Filled.Close,
                                    tint = P.coralRed,
                                    onClick = onCancelAction,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            GlassPrimaryButton(
                                text = "APPROVE & APPLY",
                                icon = Icons.Filled.Check,
                                containerColor = P.obsidian,
                                onClick = onApproveAction,
                                modifier = Modifier.fillMaxWidth().testTag("agent_approve_action"),
                            )
                        }
                        ActionStatus.APPROVED -> {
                            PillTag(text = "APPROVED & APPLIED", containerColor = P.acidLime, contentColor = P.obsidian)
                        }
                        ActionStatus.CANCELLED -> {
                            PillTag(text = "CANCELLED", containerColor = P.pureWhite, contentColor = P.obsidian)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPendingActionDialog(
    action: PendingAgentAction,
    onDismiss: () -> Unit,
    onSave: (PendingAgentAction) -> Unit,
) {
    var amountText by remember(action) {
        mutableStateOf(
            when (action) {
                is PendingAgentAction.AddTransaction -> action.amount.toString()
                is PendingAgentAction.TransferWallets -> action.amount.toString()
                is PendingAgentAction.SetBudget -> action.limitAmount.toString()
                is PendingAgentAction.AddRecurring -> action.amount.toString()
                is PendingAgentAction.AddPlan -> action.expectedAmount.toString()
                is PendingAgentAction.CompletePlan -> action.actualAmount.toString()
                is PendingAgentAction.AddWallet -> action.initialBalance.toString()
                else -> ""
            }
        )
    }
    var textField by remember(action) {
        mutableStateOf(
            when (action) {
                is PendingAgentAction.AddTransaction -> action.description
                is PendingAgentAction.TransferWallets -> action.note
                is PendingAgentAction.AddCategory -> action.name
                is PendingAgentAction.AddRecurring -> action.description
                is PendingAgentAction.AddPlan -> action.name
                is PendingAgentAction.AddWallet -> action.name
                else -> action.summarySubtitle
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${action.summaryTitle}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (amountText.isNotEmpty()) {
                    GlassTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                        label = "Amount ($CURRENCY)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                GlassTextField(
                    value = textField,
                    onValueChange = { textField = it },
                    label = "Label / Description",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amountText.toDoubleOrNull()
                val updated = when (action) {
                    is PendingAgentAction.AddTransaction -> action.copy(
                        amount = amt ?: action.amount,
                        description = textField.ifBlank { action.description },
                    )
                    is PendingAgentAction.TransferWallets -> action.copy(
                        amount = amt ?: action.amount,
                        note = textField,
                    )
                    is PendingAgentAction.SetBudget -> action.copy(limitAmount = amt ?: action.limitAmount)
                    is PendingAgentAction.AddCategory -> action.copy(name = textField.ifBlank { action.name })
                    is PendingAgentAction.AddRecurring -> action.copy(
                        amount = amt ?: action.amount,
                        description = textField.ifBlank { action.description },
                    )
                    is PendingAgentAction.AddPlan -> action.copy(
                        expectedAmount = amt ?: action.expectedAmount,
                        name = textField.ifBlank { action.name },
                    )
                    is PendingAgentAction.CompletePlan -> action.copy(actualAmount = amt ?: action.actualAmount)
                    is PendingAgentAction.AddWallet -> action.copy(
                        initialBalance = amt ?: action.initialBalance,
                        name = textField.ifBlank { action.name },
                    )
                    else -> action
                }
                onSave(updated)
            }) { Text("APPLY") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

private fun formatTokenCount(tokens: Long): String =
    if (tokens >= 1_000) "${tokens / 1_000}K" else tokens.toString()
