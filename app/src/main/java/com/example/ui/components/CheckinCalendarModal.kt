package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.CheckinState
import com.example.domain.DateUtils
import com.example.ui.theme.LocalGlassPalette
import java.util.Calendar

@Composable
fun CheckinCalendarModal(
    checkinState: CheckinState,
    onDismiss: () -> Unit,
) {
    val P = LocalGlassPalette.current
    val nowCal = remember { Calendar.getInstance() }
    var displayYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
    val todayKey = remember { DateUtils.toKey(System.currentTimeMillis()) }

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
            Text("CHECK-IN CALENDAR", style = MaterialTheme.typography.labelMedium, color = P.warning)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${checkinState.currentStreak} day streak · ${checkinState.bestStreak} best",
                style = MaterialTheme.typography.titleLarge,
                color = P.ink,
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        set(displayYear, displayMonth, 1)
                        add(Calendar.MONTH, -1)
                    }
                    displayYear = c.get(Calendar.YEAR)
                    displayMonth = c.get(Calendar.MONTH)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = P.ink)
                }
                Text(
                    text = DateUtils.formatMonthYear(DateUtils.startOfMonth(displayYear, displayMonth)),
                    style = MaterialTheme.typography.titleMedium,
                    color = P.ink,
                )
                IconButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        set(displayYear, displayMonth, 1)
                        add(Calendar.MONTH, 1)
                    }
                    displayYear = c.get(Calendar.YEAR)
                    displayMonth = c.get(Calendar.MONTH)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = P.ink)
                }
            }

            // Saturday-first Bangladesh week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "S", "M", "T", "W", "T", "F").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = P.mutedInk,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            val monthCal = remember(displayYear, displayMonth) {
                Calendar.getInstance().apply { set(displayYear, displayMonth, 1, 0, 0, 0) }
            }
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstOffset = (monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7
            val rows = (firstOffset + daysInMonth + 6) / 7

            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (c in 0 until 7) {
                        val dayNum = (r * 7 + c) - firstOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val millis = remember(displayYear, displayMonth, dayNum) {
                                Calendar.getInstance().apply {
                                    set(displayYear, displayMonth, dayNum, 0, 0, 0)
                                }.timeInMillis
                            }
                            val key = DateUtils.toKey(millis)
                            val isNoSpend = key in checkinState.noSpendDayKeys
                            val isChecked = key in checkinState.checkedDayKeys
                            val isToday = key == todayKey

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isNoSpend -> P.marigold
                                            isChecked -> P.acidLime
                                            else -> Color.Transparent
                                        }
                                    )
                                    .let {
                                        if (isToday) it.border(1.5.dp, P.electricViolet, CircleShape) else it
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        isNoSpend || isChecked -> P.obsidian
                                        else -> P.subInk
                                    },
                                    fontWeight = if (isChecked || isToday) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(P.acidLime, "Logged")
                    LegendDot(P.marigold, "No Spend")
                }
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = P.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val P = LocalGlassPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = P.subInk)
    }
}
