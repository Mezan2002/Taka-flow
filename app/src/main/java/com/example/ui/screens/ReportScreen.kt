package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PeriodPreset
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.PeriodCalculator
import com.example.ui.components.DateRangePickerDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.PillTag
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun ReportScreen(
    state: MainUiState,
    onBack: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val nowCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }

    var isCustomMode by remember { mutableStateOf(false) }
    var customRange by remember { mutableStateOf<DayRange?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var walletFilterId by remember { mutableStateOf<String?>(null) }

    val activeRange = remember(isCustomMode, customRange, selectedYear, selectedMonth) {
        if (isCustomMode && customRange != null) {
            customRange!!
        } else {
            DayRange(
                DateUtils.startOfMonth(selectedYear, selectedMonth),
                DateUtils.endOfMonth(selectedYear, selectedMonth),
            )
        }
    }

    val filteredTransactions = remember(state.transactions, walletFilterId) {
        if (walletFilterId == null) state.transactions
        else state.transactions.filter { it.walletId == walletFilterId }
    }

    val report = remember(filteredTransactions, activeRange, isCustomMode) {
        PeriodCalculator.buildReport(filteredTransactions, activeRange, isCustomMode)
    }

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
                Text("Financial Report", style = MaterialTheme.typography.headlineMedium, color = P.ink)
            }
        }

        // PERIOD mode switch (MONTH / CUSTOM DAYS)
        item {
            GlassCard(cornerRadius = 26.dp, padding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(
                        label = "MONTH",
                        selected = !isCustomMode,
                        accent = P.heroYellow,
                        onClick = { isCustomMode = false },
                        modifier = Modifier.weight(1f).testTag("report_mode_month"),
                    )
                    GlassChip(
                        label = "CUSTOM DAYS",
                        selected = isCustomMode,
                        accent = P.acidLime,
                        onClick = {
                            if (customRange == null) {
                                isCustomMode = true
                                showPicker = true
                            } else {
                                isCustomMode = true
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("report_mode_custom"),
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (!isCustomMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            val c = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, 1)
                                add(Calendar.MONTH, -1)
                            }
                            selectedYear = c.get(Calendar.YEAR)
                            selectedMonth = c.get(Calendar.MONTH)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = P.ink)
                        }
                        Text(
                            DateUtils.formatMonthYear(activeRange.startMillis),
                            style = MaterialTheme.typography.titleLarge,
                            color = P.ink,
                        )
                        IconButton(onClick = {
                            val c = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, 1)
                                add(Calendar.MONTH, 1)
                            }
                            selectedYear = c.get(Calendar.YEAR)
                            selectedMonth = c.get(Calendar.MONTH)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = P.ink)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(activeRange.label, style = MaterialTheme.typography.titleMedium, color = P.ink)
                        TextButton(onClick = { showPicker = true }) {
                            Text("CHANGE", color = P.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Wallet filter
        if (state.wallets.size > 1) {
            item {
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

        // Electric Yellow Net Balance Hero + Savings Rate Pill
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
                    PillTag(text = "NET BALANCE", containerColor = P.obsidian, contentColor = P.heroYellow)
                    PillTag(
                        text = "SAVINGS ${report.savingsRate.roundToInt()}%",
                        containerColor = P.pureWhite,
                        contentColor = P.obsidian,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    CurrencyFormat.formatAmount(report.net),
                    style = MaterialTheme.typography.displaySmall,
                    color = P.obsidian,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Top Category: ${report.topCategory ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = P.obsidian.copy(alpha = 0.78f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Acid Lime Income + Coral Crimson Expense Blocks
        item {
            val compLabel = if (isCustomMode) "PREV PERIOD" else "PREV MONTH"
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(
                    tint = P.acidLime,
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("INCOME", style = MaterialTheme.typography.labelSmall, color = P.obsidian.copy(alpha = 0.72f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        CurrencyFormat.formatAmount(report.income),
                        style = MaterialTheme.typography.titleLarge,
                        color = P.obsidian,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    report.incomeDeltaPct?.let { pct ->
                        Text(
                            "${if (pct >= 0) "+" else ""}${pct.roundToInt()}% vs $compLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = P.obsidian.copy(alpha = 0.7f),
                        )
                    }
                }
                GlassCard(
                    tint = P.coralRed,
                    cornerRadius = 24.dp,
                    padding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        CurrencyFormat.formatAmount(report.expense),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    report.expenseDeltaPct?.let { pct ->
                        Text(
                            "${if (pct >= 0) "+" else ""}${pct.roundToInt()}% vs $compLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // Category Breakdown
        item {
            GlassCard(cornerRadius = 26.dp, padding = PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
                Text("CATEGORY BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = P.subInk)
                Spacer(Modifier.height(10.dp))
                if (report.categories.isEmpty()) {
                    Text("No expenses recorded in this period.", style = MaterialTheme.typography.bodyMedium, color = P.subInk)
                } else {
                    report.categories.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.category, style = MaterialTheme.typography.bodyMedium, color = P.ink, modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            LinearProgressIndicator(
                                progress = { item.percentage.coerceIn(0f, 1f) },
                                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = P.electricViolet,
                                trackColor = P.glassChip,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${CurrencyFormat.formatAmount(item.amount)} (${(item.percentage * 100).roundToInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = P.ink,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // 6-Month MoM (Electric Violet Card) & Daily Spending Trend (Hidden in Custom Days mode)
        if (!isCustomMode) {
            item {
                GlassCard(
                    tint = P.electricViolet,
                    cornerRadius = 26.dp,
                    padding = PaddingValues(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PillTag(text = "6-MONTH COMPARISON", containerColor = P.pureWhite, contentColor = P.obsidian)
                    Spacer(Modifier.height(14.dp))
                    val maxVal = report.momPoints.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
                    Row(
                        modifier = Modifier.fillMaxWidth().height(114.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        report.momPoints.forEach { pt ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .height((78 * (pt.income / maxVal)).toFloat().coerceAtLeast(4f).dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(P.acidLime)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .height((78 * (pt.expense / maxVal)).toFloat().coerceAtLeast(4f).dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(P.heroYellow)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(pt.monthLabel, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            item {
                GlassCard(
                    tint = P.marigold,
                    cornerRadius = 26.dp,
                    padding = PaddingValues(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("DAILY SPENDING TREND", style = MaterialTheme.typography.labelSmall, color = P.obsidian.copy(alpha = 0.72f))
                    Spacer(Modifier.height(12.dp))
                    val points = report.dailyTrend
                    val maxSpend = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                    Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                        if (points.size > 1) {
                            val path = Path()
                            val stepX = size.width / (points.size - 1)
                            points.forEachIndexed { idx, (_, amount) ->
                                val x = idx * stepX
                                val y = size.height - ((amount / maxSpend).toFloat() * (size.height - 14f)) - 7f
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                if (amount > 0) {
                                    drawCircle(color = P.coralRed, radius = 4.5f, center = Offset(x, y))
                                }
                            }
                            drawPath(path, color = P.obsidian, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        DateRangePickerDialog(
            initialRange = customRange ?: PeriodCalculator.presetRange(PeriodPreset.LAST_7_DAYS),
            allowRangeMode = true,
            onDismiss = {
                showPicker = false
                if (customRange == null) {
                    isCustomMode = false
                }
            },
            onApply = { range ->
                customRange = range
                isCustomMode = true
                showPicker = false
            },
        )
    }
}
