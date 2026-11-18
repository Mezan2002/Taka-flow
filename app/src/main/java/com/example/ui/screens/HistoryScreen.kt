package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PeriodPreset
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.PeriodCalculator
import com.example.ui.components.DateRangePickerDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.components.SwipeableGlassRow
import com.example.ui.components.directionArrow
import com.example.ui.components.iconForKey
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    state: MainUiState,
    onEditTransaction: (Transaction) -> Unit,
    onEditTransfer: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
) {
    val P = LocalGlassPalette.current
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var walletFilterId by remember { mutableStateOf<String?>(null) }
    var dateRange by remember { mutableStateOf<DayRange?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val filtered = remember(state.transactions, searchQuery, typeFilter, walletFilterId, dateRange) {
        state.transactions.filter { tx ->
            val matchesWallet = walletFilterId == null || tx.walletId == walletFilterId || tx.toWalletId == walletFilterId
            val matchesDate = dateRange == null || tx.timestamp in dateRange!!.startMillis..dateRange!!.endMillis
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesQuery = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)
            matchesWallet && matchesDate && matchesType && matchesQuery
        }
    }

    val incomeSum = remember(filtered) {
        CurrencyFormat.round2(filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount })
    }
    val expenseSum = remember(filtered) {
        CurrencyFormat.round2(filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
    }
    val categoryTop = remember(filtered) {
        filtered.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.take(4)
    }
    val groupedByDay = remember(filtered) {
        filtered.groupBy { DateUtils.toKey(it.timestamp) }
    }
    val walletsById = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val categoriesByName = remember(state.categories) { state.categories.associateBy { it.name } }

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
                Text("Transactions", style = MaterialTheme.typography.headlineMedium, color = P.ink)
                PillTag(
                    text = "${filtered.size} ENTRIES",
                    containerColor = P.heroYellow,
                    contentColor = P.obsidian,
                )
            }
        }

        // Search + Date Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search entries",
                    placeholder = "Description or category",
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = P.subInk) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = P.subInk)
                            }
                        }
                    } else null,
                    modifier = Modifier.testTag("history_search_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassChip(
                        label = dateRange?.label ?: "ALL DATES",
                        selected = dateRange != null,
                        accent = P.heroYellow,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).testTag("history_date_filter_button"),
                    )
                    if (dateRange != null) {
                        IconButton(
                            onClick = { dateRange = null },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(P.glassChip)
                                .testTag("history_date_filter_clear"),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear date filter", tint = P.ink)
                        }
                    }
                }
            }
        }

        // Type & Wallet Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(label = "ALL", selected = typeFilter == null, accent = P.heroYellow, onClick = { typeFilter = null })
                    GlassChip(label = "EXPENSE", selected = typeFilter == TransactionType.EXPENSE, accent = P.coralRed, onClick = { typeFilter = TransactionType.EXPENSE })
                    GlassChip(label = "INCOME", selected = typeFilter == TransactionType.INCOME, accent = P.acidLime, onClick = { typeFilter = TransactionType.INCOME })
                    GlassChip(label = "TRANSFER", selected = typeFilter == TransactionType.TRANSFER, accent = P.electricViolet, onClick = { typeFilter = TransactionType.TRANSFER })
                }

                if (state.wallets.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            GlassChip(label = "All Wallets", selected = walletFilterId == null, accent = P.heroYellow, onClick = { walletFilterId = null })
                        }
                        items(state.wallets, key = { it.id }) { w ->
                            GlassChip(label = w.name, selected = walletFilterId == w.id, accent = P.acidLime, onClick = { walletFilterId = w.id })
                        }
                    }
                }
            }
        }

        // Bold Side-by-Side Withdraw/Expense & Deposit/Income Summary Blocks + Top Categories
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(
                    tint = P.heroYellow,
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Expense", style = MaterialTheme.typography.labelMedium, color = P.obsidian.copy(alpha = 0.75f))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(P.obsidian),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = P.heroYellow, modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        CurrencyFormat.formatAmount(expenseSum),
                        style = MaterialTheme.typography.titleLarge,
                        color = P.obsidian,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                GlassCard(
                    tint = P.acidLime,
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Income", style = MaterialTheme.typography.labelMedium, color = P.obsidian.copy(alpha = 0.75f))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(P.obsidian),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = P.acidLime, modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        CurrencyFormat.formatAmount(incomeSum),
                        style = MaterialTheme.typography.titleLarge,
                        color = P.obsidian,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (categoryTop.isNotEmpty() && expenseSum > 0) {
            item {
                GlassCard(
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("TOP CATEGORIES", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                    Spacer(Modifier.height(8.dp))
                    categoryTop.forEach { (cat, amt) ->
                        val frac = (amt / expenseSum).toFloat().coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cat, style = MaterialTheme.typography.bodyMedium, color = P.ink, modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                                color = P.electricViolet,
                                trackColor = P.glassChip,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(CurrencyFormat.formatAmount(amt), style = MaterialTheme.typography.labelMedium, color = P.ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (groupedByDay.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = P.mutedInk, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No matching entries", style = MaterialTheme.typography.titleMedium, color = P.ink)
                        Text("Try clearing filters or log a new entry with +.", style = MaterialTheme.typography.bodyMedium, color = P.subInk)
                    }
                }
            }
        } else {
            groupedByDay.forEach { (dayKey, dayItems) ->
                item(key = "hdr_$dayKey") {
                    Text(
                        text = DateUtils.formatMediumDate(DateUtils.fromKey(dayKey)),
                        style = MaterialTheme.typography.labelMedium,
                        color = P.subInk,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                    )
                }
                items(dayItems, key = { it.id }) { tx ->
                    val walletName = walletsById[tx.walletId]?.name ?: "Wallet"
                    val toWalletName = tx.toWalletId?.let { walletsById[it]?.name }
                    val catIconKey = categoriesByName[tx.category]?.iconKey ?: "category"

                    SwipeableGlassRow(
                        onEdit = {
                            if (tx.type == TransactionType.TRANSFER) onEditTransfer(tx) else onEditTransaction(tx)
                        },
                        onDelete = { onDeleteTransaction(tx) },
                    ) {
                        GlassCard(
                            cornerRadius = 22.dp,
                            padding = PaddingValues(14.dp),
                            onClick = {
                                if (tx.type == TransactionType.TRANSFER) onEditTransfer(tx) else onEditTransaction(tx)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    val arrowBadgeBg = when (tx.type) {
                                        TransactionType.INCOME -> P.acidLime
                                        TransactionType.EXPENSE -> P.heroYellow
                                        TransactionType.TRANSFER -> P.electricViolet
                                    }
                                    val arrowTint = if (tx.type == TransactionType.TRANSFER) Color.White else P.obsidian

                                    // Circular category icon with overlapping direction arrow badge (Image 1 right screen)
                                    Box(modifier = Modifier.size(48.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(P.glassChip),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (tx.type == TransactionType.TRANSFER) Icons.Filled.SwapHoriz else iconForKey(catIconKey),
                                                contentDescription = null,
                                                tint = P.ink,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(arrowBadgeBg)
                                                .border(1.5.dp, P.glassSurface, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (tx.type == TransactionType.TRANSFER) Icons.Filled.SwapHoriz else directionArrow(tx.type == TransactionType.EXPENSE),
                                                contentDescription = null,
                                                tint = arrowTint,
                                                modifier = Modifier.size(11.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tx.description.ifBlank { tx.category },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = P.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (tx.type == TransactionType.TRANSFER && toWalletName != null) {
                                                "$walletName → $toWalletName"
                                            } else {
                                                "${tx.category} · $walletName"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = P.subInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    text = when (tx.type) {
                                        TransactionType.INCOME -> "+${CurrencyFormat.formatAmount(tx.amount)}"
                                        TransactionType.EXPENSE -> "-${CurrencyFormat.formatAmount(tx.amount)}"
                                        TransactionType.TRANSFER -> CurrencyFormat.formatAmount(tx.amount)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (tx.type) {
                                        TransactionType.INCOME -> P.income
                                        TransactionType.EXPENSE -> P.ink
                                        TransactionType.TRANSFER -> P.transfer
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            initialRange = dateRange ?: PeriodCalculator.presetRange(PeriodPreset.THIS_MONTH),
            allowRangeMode = true,
            onDismiss = { showDatePicker = false },
            onApply = { range ->
                dateRange = range
                showDatePicker = false
            },
        )
    }
}
