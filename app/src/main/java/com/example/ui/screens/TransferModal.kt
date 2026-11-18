package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CURRENCY
import com.example.data.model.CURRENCY_SYMBOL
import com.example.data.model.Transaction
import com.example.data.model.Wallet
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.TransferSheetState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransferModal(
    sheetState: TransferSheetState,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onSave: (
        editing: Transaction?,
        fromId: String,
        toId: String,
        amount: Double,
        note: String,
        timestamp: Long,
    ) -> Unit,
) {
    if (!sheetState.visible) return
    val P = LocalGlassPalette.current
    val editing = sheetState.editingTransaction

    var fromId by remember(sheetState) {
        mutableStateOf(editing?.walletId ?: wallets.firstOrNull()?.id ?: "")
    }
    var toId by remember(sheetState) {
        mutableStateOf(
            editing?.toWalletId
                ?: wallets.firstOrNull { it.id != fromId }?.id
                ?: wallets.firstOrNull()?.id.orEmpty()
        )
    }
    var amountText by remember(sheetState) {
        mutableStateOf(editing?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var note by remember(sheetState) { mutableStateOf(editing?.description ?: "") }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val valid = wallets.size >= 2 && fromId.isNotBlank() && toId.isNotBlank() && fromId != toId && parsedAmount > 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GlassCard(
            tint = P.glassModal,
            cornerRadius = 26.dp,
            padding = PaddingValues(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = P.transfer)
                    Text(
                        text = if (editing != null) "Edit Transfer" else "Wallet Transfer",
                        style = MaterialTheme.typography.titleLarge,
                        color = P.ink,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close transfer", tint = P.subInk)
                }
            }

            if (wallets.size < 2) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Add at least 2 wallets to transfer funds between them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = P.subInk,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text("FROM WALLET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    wallets.forEach { w ->
                        GlassChip(
                            label = w.name,
                            selected = fromId == w.id,
                            accent = P.heroYellow,
                            onClick = { fromId = w.id },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("TO WALLET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    wallets.forEach { w ->
                        GlassChip(
                            label = w.name,
                            selected = toId == w.id,
                            accent = P.acidLime,
                            onClick = { toId = w.id },
                        )
                    }
                }

                if (fromId == toId) {
                    Spacer(Modifier.height(6.dp))
                    Text("Source and destination wallets must be different.", style = MaterialTheme.typography.bodyMedium, color = P.coralRed)
                }

                Spacer(Modifier.height(12.dp))
                GlassTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) amountText = it },
                    label = "Amount ($CURRENCY)",
                    leadingIcon = { Text(CURRENCY_SYMBOL, style = MaterialTheme.typography.titleLarge, color = P.electricViolet) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.testTag("transfer_amount_input"),
                )
                Spacer(Modifier.height(10.dp))
                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (optional)",
                    placeholder = "e.g., Cash out",
                )
                Spacer(Modifier.height(16.dp))
                GlassPrimaryButton(
                    text = if (editing != null) "SAVE TRANSFER" else "TRANSFER NOW",
                    enabled = valid,
                    containerColor = P.electricViolet,
                    onClick = {
                        onSave(
                            editing,
                            fromId,
                            toId,
                            parsedAmount,
                            note,
                            editing?.timestamp ?: System.currentTimeMillis(),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("transfer_save_button"),
                )
            }
        }
    }
}
