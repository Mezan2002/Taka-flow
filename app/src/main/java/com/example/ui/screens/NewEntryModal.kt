package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CURRENCY
import com.example.data.model.CURRENCY_SYMBOL
import com.example.data.model.Category
import com.example.data.model.QuickFavorite
import com.example.data.model.Transaction
import com.example.data.model.TransactionDraft
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.OfflineVoiceParser
import com.example.services.SpeechVoiceManager
import com.example.ui.components.DateRangePickerDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.NewEntrySheetState
import com.example.ui.viewmodel.NewEntryTab

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewEntryModal(
    sheetState: NewEntrySheetState,
    wallets: List<Wallet>,
    categories: List<Category>,
    favorites: List<QuickFavorite>,
    speechManager: SpeechVoiceManager,
    onDismiss: () -> Unit,
    onSaveManual: (
        editing: Transaction?,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        walletId: String,
        timestamp: Long,
    ) -> Unit,
    onSaveDrafts: (drafts: List<TransactionDraft>, walletId: String, timestamp: Long) -> Unit,
    onTriggerFavorite: (QuickFavorite) -> Unit,
    onAddFavorite: (label: String, amount: Double, type: TransactionType, category: String, walletId: String) -> Unit,
    onDeleteFavorite: (String) -> Unit,
    onAddCategory: (name: String, type: TransactionType) -> Unit,
) {
    if (!sheetState.visible) return
    val P = LocalGlassPalette.current
    val context = LocalContext.current

    val editing = sheetState.editingTransaction
    var activeTab by remember(sheetState) { mutableStateOf(sheetState.initialTab) }
    var txType by remember(sheetState) {
        mutableStateOf(
            if (editing != null && editing.type != TransactionType.TRANSFER) editing.type else TransactionType.EXPENSE
        )
    }
    var amountText by remember(sheetState) {
        mutableStateOf(editing?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var selectedCategory by remember(sheetState) {
        mutableStateOf(editing?.category ?: if (txType == TransactionType.INCOME) "Salary" else "Food")
    }
    var description by remember(sheetState) { mutableStateOf(editing?.description ?: "") }
    var selectedWalletId by remember(sheetState) {
        mutableStateOf(editing?.walletId ?: wallets.firstOrNull()?.id ?: "wallet_cash")
    }
    var entryTimestamp by remember(sheetState) {
        mutableStateOf(editing?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Voice tab state
    var isListening by remember { mutableStateOf(false) }
    var transcript by remember(sheetState) { mutableStateOf(sheetState.voiceTranscript) }
    var drafts by remember(sheetState) { mutableStateOf(sheetState.drafts) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    fun parseTranscript(text: String) {
        transcript = text
        val parsed = OfflineVoiceParser.parse(text)
        drafts = parsed
        voiceError = if (parsed.isEmpty() && text.isNotBlank()) {
            "Could not find an amount. Try saying e.g. \"Tea 20 Rickshaw 50\"."
        } else null
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            voiceError = null
            speechManager.startListening(
                onPartial = { transcript = it },
                onFinal = { finalText ->
                    isListening = false
                    parseTranscript(finalText)
                },
                onError = { err ->
                    isListening = false
                    voiceError = err
                },
            )
        } else {
            voiceError = "Microphone permission is needed for voice capture."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.stopListening()
        }
    }

    Dialog(
        onDismissRequest = {
            speechManager.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GlassCard(
            tint = P.glassModal,
            cornerRadius = 28.dp,
            padding = PaddingValues(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .heightIn(max = 680.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (editing != null) "EDIT ENTRY" else "NEW ENTRY",
                        style = MaterialTheme.typography.labelMedium,
                        color = P.primary,
                    )
                    Text(
                        text = if (activeTab == NewEntryTab.MANUAL) "Manual & Quick-Add" else "Offline Voice Logger",
                        style = MaterialTheme.typography.titleLarge,
                        color = P.ink,
                    )
                }
                IconButton(
                    onClick = {
                        speechManager.stopListening()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("new_entry_close"),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close new entry", tint = P.subInk)
                }
            }

            if (editing == null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(
                        label = "MANUAL",
                        selected = activeTab == NewEntryTab.MANUAL,
                        accent = P.heroYellow,
                        onClick = {
                            speechManager.stopListening()
                            isListening = false
                            activeTab = NewEntryTab.MANUAL
                        },
                        modifier = Modifier.weight(1f).testTag("entry_tab_manual"),
                    )
                    GlassChip(
                        label = "VOICE",
                        selected = activeTab == NewEntryTab.VOICE,
                        accent = P.acidLime,
                        onClick = { activeTab = NewEntryTab.VOICE },
                        modifier = Modifier.weight(1f).testTag("entry_tab_voice"),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (activeTab == NewEntryTab.MANUAL) {
                    // Quick-Add Favorites (only on create)
                    if (editing == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("QUICK-ADD FAVORITES", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                            TextButton(onClick = { showAddFavoriteDialog = true }) {
                                Text("+ FAVORITE", style = MaterialTheme.typography.labelSmall, color = P.primary)
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            favorites.forEach { fav ->
                                GlassChip(
                                    label = "${fav.label} ${CurrencyFormat.formatAmount(fav.amount)}",
                                    selected = false,
                                    accent = P.gold,
                                    onClick = {
                                        onTriggerFavorite(fav)
                                        onDismiss()
                                    },
                                    modifier = Modifier.testTag("fav_${fav.id}"),
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Expense / Income switch
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(
                            label = "EXPENSE",
                            selected = txType == TransactionType.EXPENSE,
                            accent = P.coralRed,
                            onClick = {
                                txType = TransactionType.EXPENSE
                                val cats = categories.filter { it.type == TransactionType.EXPENSE }
                                if (cats.none { it.name == selectedCategory }) {
                                    selectedCategory = cats.firstOrNull()?.name ?: "Food"
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("manual_type_expense"),
                        )
                        GlassChip(
                            label = "INCOME",
                            selected = txType == TransactionType.INCOME,
                            accent = P.acidLime,
                            onClick = {
                                txType = TransactionType.INCOME
                                val cats = categories.filter { it.type == TransactionType.INCOME }
                                if (cats.none { it.name == selectedCategory }) {
                                    selectedCategory = cats.firstOrNull()?.name ?: "Salary"
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("manual_type_income"),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    GlassTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                amountText = input
                            }
                        },
                        label = "Amount ($CURRENCY)",
                        placeholder = "0.00",
                        leadingIcon = {
                            Text(CURRENCY_SYMBOL, style = MaterialTheme.typography.titleLarge, color = P.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.testTag("manual_amount_input"),
                    )

                    Spacer(Modifier.height(12.dp))
                    GlassTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description",
                        placeholder = "e.g., Tea, Rickshaw, Bazar",
                        modifier = Modifier.testTag("manual_description_input"),
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("CATEGORY", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                        TextButton(onClick = { showAddCategoryDialog = true }) {
                            Text("+ NEW", style = MaterialTheme.typography.labelSmall, color = P.primary)
                        }
                    }
                    val matchingCats = categories.filter { it.type == txType }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        matchingCats.forEach { cat ->
                            GlassChip(
                                label = cat.name,
                                selected = selectedCategory == cat.name,
                                onClick = { selectedCategory = cat.name },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("WALLET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        wallets.forEach { w ->
                            GlassChip(
                                label = w.name,
                                selected = selectedWalletId == w.id,
                                onClick = { selectedWalletId = w.id },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(P.glassChip)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = P.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(DateUtils.formatMediumDate(entryTimestamp), style = MaterialTheme.typography.bodyLarge, color = P.ink)
                        }
                        Text("CHANGE", style = MaterialTheme.typography.labelSmall, color = P.primary)
                    }

                    Spacer(Modifier.height(16.dp))
                    val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
                    GlassPrimaryButton(
                        text = if (editing != null) "SAVE CHANGES" else "SAVE ENTRY",
                        enabled = parsedAmt > 0.0,
                        onClick = {
                            onSaveManual(
                                editing,
                                txType,
                                parsedAmt,
                                selectedCategory,
                                description,
                                selectedWalletId,
                                entryTimestamp,
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("manual_save_button"),
                    )
                } else {
                    // VOICE TAB (Offline Rule-Based Parser)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val micColor by animateColorAsState(
                            if (isListening) P.coralRed else P.acidLime,
                            label = "micColor",
                        )
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .shadow(14.dp, CircleShape, ambientColor = micColor, spotColor = micColor)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(micColor, if (isListening) P.marigold else P.heroYellow)
                                    )
                                )
                                .border(2.dp, P.pureWhite, CircleShape)
                                .clickable {
                                    if (isListening) {
                                        speechManager.stopListening()
                                        isListening = false
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            isListening = true
                                            voiceError = null
                                            speechManager.startListening(
                                                onPartial = { transcript = it },
                                                onFinal = { finalText ->
                                                    isListening = false
                                                    parseTranscript(finalText)
                                                },
                                                onError = { err ->
                                                    isListening = false
                                                    voiceError = err
                                                },
                                            )
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("voice_mic_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Start voice capture",
                                tint = Color(0xFF041421),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isListening) "Listening... tap to stop" else "Tap mic to speak (100% offline parser)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = P.subInk,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("TRY TO SAY", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            "Tea 20 Rickshaw 50",
                            "Lunch 180",
                            "Salary 35000",
                            "Cha 20 ebong Bazar 450",
                        ).forEach { sample ->
                            GlassChip(
                                label = sample,
                                selected = false,
                                onClick = { parseTranscript(sample) },
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    GlassTextField(
                        value = transcript,
                        onValueChange = { parseTranscript(it) },
                        label = "Voice Transcript / Quick Text",
                        placeholder = "e.g., Tea 20 Rickshaw 50",
                        singleLine = false,
                        modifier = Modifier.testTag("voice_transcript_input"),
                    )

                    if (voiceError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(voiceError!!, style = MaterialTheme.typography.bodyMedium, color = P.expense)
                    }

                    if (drafts.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("PARSED DRAFTS (${drafts.size})", style = MaterialTheme.typography.labelSmall, color = P.income)
                        Spacer(Modifier.height(8.dp))
                        drafts.forEachIndexed { idx, draft ->
                            GlassCard(
                                tint = P.glassChip,
                                cornerRadius = 16.dp,
                                padding = PaddingValues(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${draft.description} • ${draft.category}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = P.ink,
                                        )
                                        Text(
                                            text = "${draft.type.name} • ${CurrencyFormat.formatAmount(draft.amount)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (draft.type == TransactionType.INCOME) P.income else P.expense,
                                        )
                                    }
                                    IconButton(onClick = {
                                        drafts = drafts.filterIndexed { i, _ -> i != idx }
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove draft", tint = P.subInk)
                                    }
                                }
                            }
                        }

                        Text("WALLET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            wallets.forEach { w ->
                                GlassChip(
                                    label = w.name,
                                    selected = selectedWalletId == w.id,
                                    onClick = { selectedWalletId = w.id },
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        GlassPrimaryButton(
                            text = "SAVE ${drafts.size} ${if (drafts.size == 1) "DRAFT" else "DRAFTS"}",
                            onClick = {
                                onSaveDrafts(drafts, selectedWalletId, System.currentTimeMillis())
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("voice_save_drafts"),
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            initialRange = DayRange(DateUtils.startOfDay(entryTimestamp), DateUtils.endOfDay(entryTimestamp)),
            allowRangeMode = false,
            onDismiss = { showDatePicker = false },
            onApply = { range ->
                entryTimestamp = range.startMillis + 12 * 3_600_000L
                showDatePicker = false
            },
        )
    }

    if (showAddFavoriteDialog) {
        AddQuickFavoriteDialog(
            categories = categories,
            wallets = wallets,
            favorites = favorites,
            onDismiss = { showAddFavoriteDialog = false },
            onSave = { lbl, amt, t, cat, wId ->
                onAddFavorite(lbl, amt, t, cat, wId)
                showAddFavoriteDialog = false
            },
            onDelete = onDeleteFavorite,
        )
    }

    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New ${txType.name.lowercase()} category") },
            text = {
                GlassTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = "Category name",
                    placeholder = "e.g., Pets",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newCatName.isNotBlank(),
                    onClick = {
                        onAddCategory(newCatName.trim(), txType)
                        selectedCategory = newCatName.trim()
                        showAddCategoryDialog = false
                    },
                ) { Text("ADD") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun AddQuickFavoriteDialog(
    categories: List<Category>,
    wallets: List<Wallet>,
    favorites: List<QuickFavorite>,
    onDismiss: () -> Unit,
    onSave: (String, Double, TransactionType, String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val P = LocalGlassPalette.current
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(categories.firstOrNull { it.type == TransactionType.EXPENSE }?.name ?: "Food") }
    val walletId = wallets.firstOrNull()?.id ?: "wallet_cash"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Quick Favorites") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(value = label, onValueChange = { label = it }, label = "Label (e.g., Coffee)")
                GlassTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amount = it },
                    label = "Amount ($CURRENCY)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (favorites.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Existing", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                    favorites.forEach { f ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${f.label} (${CurrencyFormat.formatAmount(f.amount)})", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDelete(f.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete favorite", tint = P.expense, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amt = amount.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = label.isNotBlank() && amt > 0.0,
                onClick = { onSave(label, amt, TransactionType.EXPENSE, cat, walletId) },
            ) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        },
    )
}
