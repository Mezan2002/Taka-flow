package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BadgeEngine
import com.example.domain.CurrencyFormat
import com.example.domain.DateUtils
import com.example.domain.PeriodCalculator
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.PillTag
import com.example.ui.components.contrastInkFor
import com.example.ui.components.iconForKey
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainUiState

@Composable
fun WeeklyRecapScreen(
    state: MainUiState,
    onBack: () -> Unit,
) {
    val P = LocalGlassPalette.current
    var thisWeek by remember { mutableStateOf(true) }
    val stats = remember(state.transactions, thisWeek) {
        PeriodCalculator.weeklyRecap(state.transactions, thisWeek)
    }
    val maxDay = (stats.dailyTotals.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)

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
                Text("Weekly Recap (Sat–Fri)", style = MaterialTheme.typography.headlineMedium, color = P.ink)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(label = "LAST WEEK", selected = !thisWeek, accent = P.heroYellow, onClick = { thisWeek = false }, modifier = Modifier.weight(1f))
                GlassChip(label = "THIS WEEK", selected = thisWeek, accent = P.electricViolet, onClick = { thisWeek = true }, modifier = Modifier.weight(1f))
            }
        }

        item {
            GlassCard(
                tint = P.electricViolet,
                cornerRadius = 28.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PillTag(text = stats.range.label, containerColor = P.pureWhite, contentColor = P.obsidian)
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("INCOME", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                        Text(CurrencyFormat.formatAmount(stats.income), style = MaterialTheme.typography.titleLarge, color = P.acidLime)
                    }
                    Column {
                        Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                        Text(CurrencyFormat.formatAmount(stats.expense), style = MaterialTheme.typography.titleLarge, color = P.heroYellow)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("NET", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f))
                        Text(
                            CurrencyFormat.formatAmount(stats.net),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Entries: ${stats.count} · Top Expense: ${stats.topCategory ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        item {
            GlassCard(
                tint = P.acidLime,
                cornerRadius = 26.dp,
                padding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("DAILY BREAKDOWN (SAT START)", style = MaterialTheme.typography.labelSmall, color = P.obsidian.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))
                stats.dailyTotals.forEach { (dayLbl, amt) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(dayLbl, style = MaterialTheme.typography.labelMedium, color = P.obsidian, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                        val frac = (amt / maxDay).toFloat().coerceIn(0.03f, 1f)
                        Box(modifier = Modifier.weight(1f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(P.obsidian.copy(alpha = 0.16f))) {
                            if (amt > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(frac)
                                        .height(11.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(P.obsidian)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(CurrencyFormat.formatAmount(amt), style = MaterialTheme.typography.labelMedium, color = P.obsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgesScreen(
    state: MainUiState,
    onBack: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val unlockedMap = state.settings.unlockedBadges
    val badgeColors = listOf(P.heroYellow, P.acidLime, P.electricViolet, P.marigold, P.coralRed)

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
                Column {
                    Text("Badges", style = MaterialTheme.typography.headlineMedium, color = P.ink)
                    Text(
                        "${unlockedMap.size} of ${BadgeEngine.definitions.size} unlocked · Streak ${state.checkinState.bestStreak}d",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.subInk,
                    )
                }
            }
        }

        itemsIndexed(BadgeEngine.definitions, key = { _, b -> b.id }) { idx, badge ->
            val unlockedAt = unlockedMap[badge.id]
            val unlocked = unlockedAt != null
            val blockColor = if (unlocked) badgeColors[idx % badgeColors.size] else P.glassSurface
            val ink = if (unlocked) contrastInkFor(blockColor) else P.ink

            GlassCard(
                tint = blockColor,
                cornerRadius = 24.dp,
                padding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (unlocked) 1f else 0.65f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (unlocked) P.pureWhite else P.glassChip),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (unlocked) iconForKey(badge.iconKey) else Icons.Filled.Lock,
                            contentDescription = badge.title,
                            tint = if (unlocked) P.obsidian else P.mutedInk,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(badge.title, style = MaterialTheme.typography.titleMedium, color = ink)
                        Text(badge.description, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.8f))
                        if (unlockedAt != null) {
                            Spacer(Modifier.height(4.dp))
                            PillTag(
                                text = "UNLOCKED ${DateUtils.formatMediumDate(unlockedAt).uppercase()}",
                                containerColor = P.obsidian,
                                contentColor = P.heroYellow,
                            )
                        }
                    }
                }
            }
        }
    }
}
