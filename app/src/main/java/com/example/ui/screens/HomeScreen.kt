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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CURRENCY
import com.example.data.model.Category
import com.example.data.model.PeriodPreset
import com.example.data.model.TransactionType
import com.example.domain.BadgeEngine
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.PeriodCalculator
import com.example.ui.components.CheckinCalendarModal
import com.example.ui.components.CircleActionBadge
import com.example.ui.components.DateRangePickerDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.PillTag
import com.example.ui.components.SemiCircularBudgetGauge
import com.example.ui.components.contrastInkFor
import com.example.ui.components.iconForKey
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    onOpenWallets: () -> Unit,
    onOpenVoiceEntry: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenBadges: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenAgent: () -> Unit,
    onToggleTotalBalanceHidden: () -> Unit,
    onSelectPeriodPreset: (PeriodPreset) -> Unit,
    onSetCustomRange: (DayRange) -> Unit,
    onMarkNothingSpent: () -> Unit,
    onUndoCheckinToday: () -> Unit,
    onSetBudget: (String?, Double) -> Unit,
    onRemoveBudget: (String) -> Unit,
) {
    val P = LocalGlassPalette.current
    val now = remember { System.currentTimeMillis() }
    val greeting = remember(now) {
        when (Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    var showPeriodPicker by remember { mutableStateOf(false) }
    var showCheckinCalendar by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var recapThisWeek by remember { mutableStateOf(true) }

    val thisMonthRange = remember(now) { PeriodCalculator.presetRange(PeriodPreset.THIS_MONTH, now) }
    val thisMonthTotals = remember(state.transactions, thisMonthRange) {
        PeriodCalculator.rangeTotals(state.transactions, thisMonthRange)
    }
    val weeklyStats = remember(state.transactions, recapThisWeek, now) {
        PeriodCalculator.weeklyRecap(state.transactions, recapThisWeek, now)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Minimalist Bold Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = greeting.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = P.subInk,
                    )
                    Text(
                        text = DateUtils.formatHeaderDate(now),
                        style = MaterialTheme.typography.headlineMedium,
                        color = P.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleActionBadge(
                        icon = Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Open Wallets",
                        containerColor = P.glassSurface,
                        iconTint = P.ink,
                        size = 48.dp,
                        onClick = onOpenWallets,
                        modifier = Modifier
                            .border(1.dp, P.glassBorderTop, CircleShape)
                            .testTag("home_wallets_button"),
                    )
                    CircleActionBadge(
                        icon = Icons.Filled.Mic,
                        contentDescription = "Voice Quick Entry",
                        containerColor = P.acidLime,
                        iconTint = P.obsidian,
                        size = 48.dp,
                        onClick = onOpenVoiceEntry,
                        modifier = Modifier.testTag("home_voice_button"),
                    )
                }
            }
        }

        // 2. Electric Yellow Hero Financial Dashboard Card (inspired by Image 1 left screen)
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
                    PillTag(
                        text = "FINANCIAL DASHBOARD",
                        containerColor = P.obsidian,
                        contentColor = P.heroYellow,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircleActionBadge(
                            icon = if (state.settings.totalBalanceHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle balance visibility",
                            containerColor = P.pureWhite,
                            iconTint = P.obsidian,
                            size = 40.dp,
                            onClick = onToggleTotalBalanceHidden,
                        )
                        CircleActionBadge(
                            icon = Icons.Filled.BarChart,
                            contentDescription = "Open Monthly Report",
                            containerColor = P.obsidian,
                            iconTint = P.pureWhite,
                            size = 40.dp,
                            onClick = onOpenReport,
                            modifier = Modifier.testTag("home_month_report_button"),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = CurrencyFormat.formatAmount(state.totalWalletBalance, state.settings.totalBalanceHidden),
                    style = MaterialTheme.typography.displaySmall,
                    color = P.obsidian,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Total Wallet Balance · ${state.wallets.size} Wallet${if (state.wallets.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = P.obsidian.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.height(16.dp))
                // Two Crisp Pure-White Quick Action Tiles (Withdraw/Deposit style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeroWhiteActionTile(
                        title = "Wallets",
                        subtitle = "Manage & Transfer",
                        icon = Icons.AutoMirrored.Filled.CallMade,
                        onClick = onOpenWallets,
                        modifier = Modifier.weight(1f),
                    )
                    HeroWhiteActionTile(
                        title = "Voice Log",
                        subtitle = "Offline Bangla/EN",
                        icon = Icons.AutoMirrored.Filled.CallReceived,
                        onClick = onOpenVoiceEntry,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(10.dp))
                // Stepped Pure-White Bottom Summary Sheet inside Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(P.pureWhite)
                        .clickable(onClick = onOpenReport)
                        .padding(16.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "THIS MONTH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = P.obsidian.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = "${thisMonthTotals.count} Entries",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = P.obsidian,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "NET ${CurrencyFormat.formatAmount(thisMonthTotals.net, state.settings.totalBalanceHidden)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (thisMonthTotals.net >= 0) Color(0xFF15803D) else P.coralRed,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "+${CurrencyFormat.formatAmount(thisMonthTotals.income, state.settings.totalBalanceHidden)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF15803D),
                                    )
                                    Text(
                                        text = "-${CurrencyFormat.formatAmount(thisMonthTotals.expense, state.settings.totalBalanceHidden)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = P.coralRed,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Vivid Stacked Color-Block Wallet Strip (inspired by Image 2 left screen)
        item {
            val walletBlockColors = listOf(P.electricViolet, P.marigold, P.acidLime, P.obsidian)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.wallets, key = { _, w -> w.id }) { index, wallet ->
                    val blockColor = walletBlockColors[index % walletBlockColors.size]
                    val ink = contrastInkFor(blockColor)
                    val hidden = state.settings.totalBalanceHidden || wallet.isHidden
                    GlassCard(
                        tint = blockColor,
                        cornerRadius = 24.dp,
                        padding = PaddingValues(16.dp),
                        onClick = onOpenWallets,
                        modifier = Modifier.width(196.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ink.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(iconForKey(wallet.iconKey), contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
                            }
                            CircleActionBadge(
                                icon = Icons.Filled.NorthEast,
                                containerColor = if (ink == P.obsidian) P.pureWhite else P.pureWhite,
                                iconTint = P.obsidian,
                                size = 32.dp,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ink.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = CurrencyFormat.formatAmount(wallet.balance, hidden),
                            style = MaterialTheme.typography.titleLarge,
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // 4. Monthly Limit & Budgets Card (Acid Lime Speedometer Gauge Card from Image 1 middle screen!)
        item {
            val monthExpenses = remember(state.transactions, thisMonthRange) {
                state.transactions.filter {
                    it.type == TransactionType.EXPENSE &&
                        it.timestamp in thisMonthRange.startMillis..thisMonthRange.endMillis
                }
            }
            val totalSpent = monthExpenses.sumOf { it.amount }
            val monthlyBudget = state.budgets.firstOrNull { it.categoryName == null }
            val categoryBudgets = state.budgets.filter { it.categoryName != null }

            GlassCard(
                tint = P.acidLime,
                cornerRadius = 28.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Monthly Limit",
                        style = MaterialTheme.typography.headlineSmall,
                        color = P.obsidian,
                    )
                    CircleActionBadge(
                        icon = Icons.Filled.Tune,
                        contentDescription = "Manage budgets",
                        containerColor = P.obsidian.copy(alpha = 0.1f),
                        iconTint = P.obsidian,
                        size = 40.dp,
                        onClick = { showBudgetDialog = true },
                        modifier = Modifier.testTag("home_manage_budgets_button"),
                    )
                }

                if (monthlyBudget != null && monthlyBudget.limitAmount > 0) {
                    val pct = (totalSpent / monthlyBudget.limitAmount).toFloat()
                    Spacer(Modifier.height(6.dp))
                    SemiCircularBudgetGauge(
                        progress = pct,
                        trackColor = P.obsidian,
                        tickColor = P.obsidian.copy(alpha = 0.36f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                text = "SPENT / LIMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = P.obsidian.copy(alpha = 0.65f),
                            )
                            Text(
                                text = "${CurrencyFormat.formatAmount(totalSpent)} / ${CurrencyFormat.formatAmount(monthlyBudget.limitAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = P.obsidian,
                            )
                        }
                        Text(
                            text = "${(pct * 100).roundToInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = P.obsidian,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Spent ${CurrencyFormat.formatAmount(totalSpent)} this month. Tap the slider icon to set a monthly speedometer limit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.obsidian.copy(alpha = 0.82f),
                    )
                    Spacer(Modifier.height(12.dp))
                    GlassPrimaryButton(
                        text = "SET MONTHLY LIMIT",
                        containerColor = P.obsidian,
                        onClick = { showBudgetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (categoryBudgets.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    categoryBudgets.forEach { b ->
                        val catSpent = monthExpenses
                            .filter { it.category.equals(b.categoryName, ignoreCase = true) }
                            .sumOf { it.amount }
                        BudgetProgressRow(
                            label = b.categoryName ?: "",
                            spent = catSpent,
                            limit = b.limitAmount,
                            onLimeCard = true,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // 5. Period Summary Bento Card (with Image 1's dual Withdraw ↗ / Deposit ↙ sub-tiles)
        item {
            val totals = state.activePeriodTotals
            val range = state.activePeriodRange
            GlassCard(
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("PERIOD SUMMARY", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                        Text(range.label, style = MaterialTheme.typography.titleLarge, color = P.ink)
                    }
                    CircleActionBadge(
                        icon = Icons.Filled.CalendarMonth,
                        contentDescription = "Pick custom period",
                        containerColor = P.glassChip,
                        iconTint = P.ink,
                        size = 42.dp,
                        onClick = { showPeriodPicker = true },
                        modifier = Modifier.testTag("home_period_calendar_button"),
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PeriodPreset.entries.forEach { preset ->
                        GlassChip(
                            label = preset.label,
                            selected = state.periodPreset == preset,
                            accent = P.heroYellow,
                            onClick = {
                                if (preset == PeriodPreset.CUSTOM) {
                                    showPeriodPicker = true
                                } else {
                                    onSelectPeriodPreset(preset)
                                }
                            },
                            modifier = Modifier.testTag("period_chip_${preset.name.lowercase()}"),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (totals.count == 0) {
                    Text(
                        text = "NOTHING LOGGED FOR THIS PERIOD.",
                        style = MaterialTheme.typography.labelMedium,
                        color = P.mutedInk,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PeriodSplitTile(
                            label = "Expense",
                            amount = CurrencyFormat.formatAmount(totals.expense),
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            ringColor = P.heroYellow,
                            modifier = Modifier.weight(1f),
                        )
                        PeriodSplitTile(
                            label = "Income",
                            amount = CurrencyFormat.formatAmount(totals.income),
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            ringColor = P.acidLime,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${totals.count} Entries",
                            style = MaterialTheme.typography.labelMedium,
                            color = P.subInk,
                        )
                        Text(
                            text = "NET ${CurrencyFormat.formatAmount(totals.net)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (totals.net >= 0) P.income else P.expense,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // 6. Electric Violet Weekly Recap Card (Sat–Fri)
        item {
            GlassCard(
                tint = P.electricViolet,
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                onClick = onOpenRecap,
                modifier = Modifier.fillMaxWidth().testTag("home_weekly_recap_card"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        PillTag(
                            text = "WEEKLY RECAP (SAT–FRI)",
                            containerColor = P.pureWhite,
                            contentColor = P.obsidian,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(weeklyStats.range.label, style = MaterialTheme.typography.titleMedium, color = P.pureWhite)
                    }
                    CircleActionBadge(
                        icon = Icons.Filled.NorthEast,
                        contentDescription = "Open Weekly Recap",
                        containerColor = P.pureWhite,
                        iconTint = P.obsidian,
                        size = 40.dp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(
                        label = "LAST",
                        selected = !recapThisWeek,
                        accent = P.heroYellow,
                        onClick = { recapThisWeek = false },
                    )
                    GlassChip(
                        label = "THIS WEEK",
                        selected = recapThisWeek,
                        accent = P.acidLime,
                        onClick = { recapThisWeek = true },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text("SPENT", style = MaterialTheme.typography.labelSmall, color = P.pureWhite.copy(alpha = 0.72f))
                        Text(
                            CurrencyFormat.formatAmount(weeklyStats.expense),
                            style = MaterialTheme.typography.headlineSmall,
                            color = P.pureWhite,
                        )
                        weeklyStats.topCategory?.let {
                            Text("Top: $it", style = MaterialTheme.typography.bodyMedium, color = P.acidLime)
                        }
                    }
                    val maxDay = (weeklyStats.dailyTotals.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(58.dp),
                    ) {
                        weeklyStats.dailyTotals.forEach { (dayLbl, amt) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val frac = (amt / maxDay).toFloat().coerceIn(0.10f, 1f)
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height((38 * frac).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (amt > 0) P.heroYellow else P.pureWhite.copy(alpha = 0.22f))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(dayLbl.take(1), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = P.pureWhite.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }
        }

        // 7. Warm Marigold Check-In Streak Card
        item {
            val st = state.checkinState
            GlassCard(
                tint = P.marigold,
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(P.obsidian),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Whatshot, contentDescription = null, tint = P.heroYellow)
                        }
                        Column {
                            Text("CHECK-IN STREAK", style = MaterialTheme.typography.labelSmall, color = P.obsidian.copy(alpha = 0.7f))
                            Text(
                                "${st.currentStreak} Day${if (st.currentStreak == 1) "" else "s"} · Best ${st.bestStreak}",
                                style = MaterialTheme.typography.titleLarge,
                                color = P.obsidian,
                            )
                        }
                    }
                    CircleActionBadge(
                        icon = Icons.Filled.CalendarMonth,
                        contentDescription = "Check-in Calendar",
                        containerColor = P.pureWhite,
                        iconTint = P.obsidian,
                        size = 42.dp,
                        onClick = { showCheckinCalendar = true },
                        modifier = Modifier.testTag("home_checkin_calendar_button"),
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (st.isTodayCheckedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(P.pureWhite)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                            Text(
                                if (st.isTodayNoSpend) "Checked in (No Spend)" else "Checked in via entry",
                                style = MaterialTheme.typography.bodyMedium,
                                color = P.obsidian,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (st.isTodayNoSpend) {
                            TextButton(onClick = onUndoCheckinToday, modifier = Modifier.testTag("undo_checkin_button")) {
                                Text("UNDO", style = MaterialTheme.typography.labelSmall, color = P.coralRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    GlassPrimaryButton(
                        text = "NOTHING SPENT TODAY",
                        icon = Icons.Filled.CheckCircle,
                        containerColor = P.obsidian,
                        onClick = onMarkNothingSpent,
                        modifier = Modifier.fillMaxWidth().testTag("nothing_spent_today_button"),
                    )
                }
            }
        }

        // 8. Bold Color-Block Quick Access Grid
        item {
            Text("QUICK ACCESS", style = MaterialTheme.typography.labelSmall, color = P.subInk)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickBentoTile(
                    title = "AI Agent",
                    subtitle = if (state.settings.aiAgentEnabled) "Gemini Ready" else "Off in Settings",
                    icon = Icons.Filled.AutoAwesome,
                    blockColor = P.electricViolet,
                    onClick = onOpenAgent,
                    modifier = Modifier.weight(1f).testTag("tile_ai_agent"),
                )
                QuickBentoTile(
                    title = "Badges",
                    subtitle = "${state.settings.unlockedBadges.size}/${BadgeEngine.definitions.size} Unlocked",
                    icon = Icons.Filled.EmojiEvents,
                    blockColor = P.heroYellow,
                    onClick = onOpenBadges,
                    modifier = Modifier.weight(1f).testTag("tile_badges"),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickBentoTile(
                    title = "Recurring",
                    subtitle = "${state.recurringRules.count { it.active }} Active Rules",
                    icon = Icons.Filled.Repeat,
                    blockColor = P.obsidian,
                    onClick = onOpenRecurring,
                    modifier = Modifier.weight(1f).testTag("tile_recurring"),
                )
                QuickBentoTile(
                    title = "Report",
                    subtitle = "Analytics & MoM",
                    icon = Icons.Filled.BarChart,
                    blockColor = P.acidLime,
                    onClick = onOpenReport,
                    modifier = Modifier.weight(1f).testTag("tile_report"),
                )
            }
        }
    }

    if (showPeriodPicker) {
        DateRangePickerDialog(
            initialRange = state.activePeriodRange,
            allowRangeMode = true,
            onDismiss = { showPeriodPicker = false },
            onApply = { range ->
                onSetCustomRange(range)
                showPeriodPicker = false
            },
        )
    }

    if (showCheckinCalendar) {
        CheckinCalendarModal(
            checkinState = state.checkinState,
            onDismiss = { showCheckinCalendar = false },
        )
    }

    if (showBudgetDialog) {
        ManageBudgetsDialog(
            budgets = state.budgets,
            categories = state.categories.filter { it.type == TransactionType.EXPENSE },
            onDismiss = { showBudgetDialog = false },
            onSave = onSetBudget,
            onRemove = onRemoveBudget,
        )
    }
}

@Composable
private fun HeroWhiteActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(P.pureWhite)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = P.obsidian, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = P.obsidian, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = P.obsidian.copy(alpha = 0.58f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PeriodSplitTile(
    label: String,
    amount: String,
    icon: ImageVector,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(P.glassChip)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = P.subInk)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ringColor, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(amount, style = MaterialTheme.typography.titleLarge, color = P.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BudgetProgressRow(label: String, spent: Double, limit: Double, onLimeCard: Boolean = false) {
    val P = LocalGlassPalette.current
    val pct = if (limit > 0) (spent / limit).toFloat() else 0f
    val titleColor = if (onLimeCard) P.obsidian else P.ink
    val barColor = when {
        pct >= 1f -> P.coralRed
        onLimeCard -> P.obsidian
        pct >= 0.8f -> P.warning
        else -> P.income
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = titleColor, fontWeight = FontWeight.SemiBold)
            Text(
                "${CurrencyFormat.formatAmount(spent)} / ${CurrencyFormat.formatAmount(limit)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (pct >= 1f) P.coralRed else titleColor,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = if (onLimeCard) P.obsidian.copy(alpha = 0.16f) else P.glassChip,
        )
    }
}

@Composable
private fun QuickBentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    blockColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val P = LocalGlassPalette.current
    val ink = contrastInkFor(blockColor)
    GlassCard(
        tint = blockColor,
        cornerRadius = 24.dp,
        padding = PaddingValues(16.dp),
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
            }
            CircleActionBadge(
                icon = Icons.Filled.NorthEast,
                containerColor = P.pureWhite,
                iconTint = P.obsidian,
                size = 32.dp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = ink)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = ink.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageBudgetsDialog(
    budgets: List<com.example.data.model.Budget>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String?, Double) -> Unit,
    onRemove: (String) -> Unit,
) {
    val P = LocalGlassPalette.current
    var selectedCat by remember { mutableStateOf<String?>(null) }
    var limitText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Budgets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SELECT TARGET", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(label = "Monthly Overall", selected = selectedCat == null, onClick = { selectedCat = null })
                    categories.forEach { cat ->
                        GlassChip(
                            label = cat.name,
                            selected = selectedCat == cat.name,
                            onClick = { selectedCat = cat.name },
                        )
                    }
                }
                GlassTextField(
                    value = limitText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d{0,2}$"""))) limitText = it },
                    label = "Limit ($CURRENCY)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (budgets.isNotEmpty()) {
                    Text("ACTIVE BUDGETS", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                    budgets.forEach { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${b.categoryName ?: "Monthly"}: ${CurrencyFormat.formatAmount(b.limitAmount)}")
                            IconButton(onClick = { onRemove(b.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete budget", tint = P.expense, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amt = limitText.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = amt > 0.0,
                onClick = {
                    onSave(selectedCat, amt)
                    limitText = ""
                },
            ) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        },
    )
}
