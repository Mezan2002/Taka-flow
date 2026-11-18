package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CURRENCY
import com.example.data.model.Wallet
import com.example.domain.CurrencyFormat
import com.example.ui.components.CircleActionBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.components.SwipeableGlassRow
import com.example.ui.components.contrastInkFor
import com.example.ui.components.iconForKey
import com.example.ui.components.parseHexColor
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState

@Composable
fun WalletsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onOpenTransfer: () -> Unit,
    onToggleTotalHidden: () -> Unit,
    onToggleWalletHidden: (Wallet) -> Unit,
    onAddWallet: (name: String, balance: Double, colorHex: String, iconKey: String) -> Unit,
    onUpdateWallet: (Wallet) -> Unit,
    onDeleteWallet: (Wallet) -> Unit,
) {
    val P = LocalGlassPalette.current
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }
    var showWalletDialog by remember { mutableStateOf(false) }

    val blockPalette = listOf(P.electricViolet, P.marigold, P.obsidian, P.acidLime)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = P.ink)
                }
                Text("Wallets & Transfers", style = MaterialTheme.typography.headlineMedium, color = P.ink)
            }
        }

        // Electric Yellow Total Balance Hero Card
        item {
            GlassCard(
                tint = P.heroYellow,
                cornerRadius = 28.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        PillTag(text = "TOTAL BALANCE", containerColor = P.obsidian, contentColor = P.heroYellow)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            CurrencyFormat.formatAmount(state.totalWalletBalance, state.settings.totalBalanceHidden),
                            style = MaterialTheme.typography.displaySmall,
                            color = P.obsidian,
                        )
                    }
                    CircleActionBadge(
                        icon = if (state.settings.totalBalanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle total balance privacy",
                        containerColor = P.pureWhite,
                        iconTint = P.obsidian,
                        size = 44.dp,
                        onClick = onToggleTotalHidden,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPrimaryButton(
                        text = "TRANSFER",
                        icon = Icons.Filled.SwapHoriz,
                        containerColor = P.pureWhite,
                        onClick = onOpenTransfer,
                        modifier = Modifier.weight(1f).testTag("wallets_transfer_button"),
                    )
                    GlassPrimaryButton(
                        text = "NEW WALLET",
                        icon = Icons.Filled.Add,
                        containerColor = P.obsidian,
                        onClick = {
                            editingWallet = null
                            showWalletDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("wallets_add_button"),
                    )
                }
            }
        }

        itemsIndexed(state.wallets, key = { _, w -> w.id }) { index, wallet ->
            val blockColor = blockPalette[index % blockPalette.size]
            val ink = contrastInkFor(blockColor)
            val hidden = state.settings.totalBalanceHidden || wallet.isHidden
            SwipeableGlassRow(
                onEdit = {
                    editingWallet = wallet
                    showWalletDialog = true
                },
                onDelete = { onDeleteWallet(wallet) },
            ) {
                GlassCard(
                    tint = blockColor,
                    cornerRadius = 26.dp,
                    padding = PaddingValues(18.dp),
                    onClick = {
                        editingWallet = wallet
                        showWalletDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(P.pureWhite),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(iconForKey(wallet.iconKey), contentDescription = null, tint = P.obsidian, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(wallet.name, style = MaterialTheme.typography.titleMedium, color = ink.copy(alpha = 0.82f))
                                Text(
                                    CurrencyFormat.formatAmount(wallet.balance, hidden),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ink,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircleActionBadge(
                                icon = if (wallet.isHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle wallet balance visibility",
                                containerColor = ink.copy(alpha = 0.14f),
                                iconTint = ink,
                                size = 38.dp,
                                onClick = { onToggleWalletHidden(wallet) },
                            )
                            CircleActionBadge(
                                icon = Icons.Filled.NorthEast,
                                containerColor = P.pureWhite,
                                iconTint = P.obsidian,
                                size = 38.dp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWalletDialog) {
        WalletEditorDialog(
            editing = editingWallet,
            onDismiss = { showWalletDialog = false },
            onSave = { name, balance, colorHex, iconKey ->
                val ed = editingWallet
                if (ed == null) {
                    onAddWallet(name, balance, colorHex, iconKey)
                } else {
                    onUpdateWallet(ed.copy(name = name, balance = balance, colorHex = colorHex, iconKey = iconKey))
                }
                showWalletDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletEditorDialog(
    editing: Wallet?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit,
) {
    val P = LocalGlassPalette.current
    var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var balanceText by remember(editing) {
        mutableStateOf(editing?.balance?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "0")
    }
    var colorHex by remember(editing) { mutableStateOf(editing?.colorHex ?: "#6741FF") }
    var iconKey by remember(editing) { mutableStateOf(editing?.iconKey ?: "wallet") }

    val colors = listOf("#6741FF", "#FCEB4E", "#9BF063", "#F8C957", "#DF574C", "#111216")
    val icons = listOf("cash", "wallet", "phone", "card", "bank")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "New Wallet" else "Edit Wallet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(value = name, onValueChange = { name = it }, label = "Wallet Name", placeholder = "e.g., bKash, Bank")
                GlassTextField(
                    value = balanceText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^-?\d*\.?\d{0,2}$"""))) balanceText = it },
                    label = "Balance ($CURRENCY)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("COLOR", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val c = parseHexColor(hex, P.primary)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .let { if (colorHex == hex) it.border(2.5.dp, Color.White, CircleShape) else it }
                                .clickable { colorHex = hex }
                        )
                    }
                }
                Text("ICON", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    icons.forEach { key ->
                        GlassChip(label = key.uppercase(), selected = iconKey == key, onClick = { iconKey = key })
                    }
                }
            }
        },
        confirmButton = {
            val bal = balanceText.toDoubleOrNull() ?: 0.0
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), bal, colorHex, iconKey) }) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}
