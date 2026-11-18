package com.example.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PeriodPreset
import com.example.domain.DateUtils
import com.example.domain.DayRange
import com.example.domain.PeriodCalculator
import com.example.ui.theme.LocalGlassPalette
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateRangePickerDialog(
    initialRange: DayRange,
    allowRangeMode: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (DayRange) -> Unit,
) {
    val P = LocalGlassPalette.current
    val initialCal = remember(initialRange) {
        Calendar.getInstance().apply { timeInMillis = initialRange.startMillis }
    }
    var displayYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

    var isRangeMode by remember {
        mutableStateOf(
            allowRangeMode && DateUtils.toKey(initialRange.startMillis) != DateUtils.toKey(initialRange.endMillis)
        )
    }
    var tapA by remember { mutableStateOf<Long?>(DateUtils.startOfDay(initialRange.startMillis)) }
    var tapB by remember {
        mutableStateOf<Long?>(
            if (isRangeMode) DateUtils.startOfDay(initialRange.endMillis) else null
        )
    }

    val todayKey = remember { DateUtils.toKey(System.currentTimeMillis()) }

    val resolvedRange = remember(isRangeMode, tapA, tapB) {
        val a = tapA
        val b = tapB
        when {
            !isRangeMode && a != null -> DayRange(DateUtils.startOfDay(a), DateUtils.endOfDay(a))
            isRangeMode && a != null && b != null -> {
                val start = minOf(a, b)
                val end = maxOf(a, b)
                DayRange(DateUtils.startOfDay(start), DateUtils.endOfDay(end))
            }
            isRangeMode && a != null -> DayRange(DateUtils.startOfDay(a), DateUtils.endOfDay(a))
            else -> initialRange
        }
    }

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
            Text(
                text = if (isRangeMode) "SELECT DATE RANGE" else "SELECT SINGLE DAY",
                style = MaterialTheme.typography.labelMedium,
                color = P.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = resolvedRange.label,
                style = MaterialTheme.typography.titleLarge,
                color = P.ink,
            )

            if (allowRangeMode) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(
                        label = "SINGLE DAY",
                        selected = !isRangeMode,
                        onClick = {
                            isRangeMode = false
                            tapB = null
                        },
                        modifier = Modifier.weight(1f).testTag("picker_single_day"),
                    )
                    GlassChip(
                        label = "RANGE",
                        selected = isRangeMode,
                        onClick = { isRangeMode = true },
                        modifier = Modifier.weight(1f).testTag("picker_range"),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    PeriodPreset.TODAY,
                    PeriodPreset.YESTERDAY,
                    PeriodPreset.LAST_7_DAYS,
                    PeriodPreset.THIS_MONTH,
                    PeriodPreset.LAST_MONTH,
                ).forEach { preset ->
                    GlassChip(
                        label = preset.label,
                        selected = false,
                        onClick = {
                            val r = PeriodCalculator.presetRange(preset)
                            val startDay = DateUtils.startOfDay(r.startMillis)
                            val endDay = DateUtils.startOfDay(r.endMillis)
                            tapA = startDay
                            if (DateUtils.toKey(startDay) == DateUtils.toKey(endDay)) {
                                tapB = null
                                isRangeMode = false
                            } else {
                                isRangeMode = allowRangeMode
                                tapB = if (allowRangeMode) endDay else null
                            }
                            val c = Calendar.getInstance().apply { timeInMillis = startDay }
                            displayYear = c.get(Calendar.YEAR)
                            displayMonth = c.get(Calendar.MONTH)
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            // Month navigation header
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

            // Saturday-first weekday headers
            val weekHeaders = listOf("S", "S", "M", "T", "W", "T", "F")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = P.mutedInk,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Calendar Grid (Saturday-first)
            val monthCal = remember(displayYear, displayMonth) {
                Calendar.getInstance().apply { set(displayYear, displayMonth, 1, 0, 0, 0) }
            }
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOffset = (monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7
            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            val startKey = DateUtils.toKey(resolvedRange.startMillis)
            val endKey = DateUtils.toKey(resolvedRange.endMillis)

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - firstDayOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val cellMillis = remember(displayYear, displayMonth, dayNum) {
                                Calendar.getInstance().apply {
                                    set(displayYear, displayMonth, dayNum, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            val cellKey = DateUtils.toKey(cellMillis)
                            val isEndpoint = cellKey == startKey || cellKey == endKey
                            val inRange = cellMillis in DateUtils.startOfDay(resolvedRange.startMillis)..DateUtils.endOfDay(resolvedRange.endMillis)
                            val isToday = cellKey == todayKey

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isEndpoint -> P.heroYellow
                                            inRange -> P.heroYellow.copy(alpha = 0.25f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .let {
                                        if (isToday && !isEndpoint) {
                                            it.border(1.5.dp, P.electricViolet, CircleShape)
                                        } else it
                                    }
                                    .clickable {
                                        if (!isRangeMode) {
                                            tapA = cellMillis
                                            tapB = null
                                        } else {
                                            if (tapA == null || tapB != null) {
                                                tapA = cellMillis
                                                tapB = null
                                            } else {
                                                tapB = cellMillis
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isEndpoint) P.obsidian else P.ink,
                                    fontWeight = if (isEndpoint || isToday) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isRangeMode) "Tap any two dates in either order to set range." else "Tap any date to pick a single day.",
                style = MaterialTheme.typography.bodyMedium,
                color = P.subInk,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("picker_cancel")) {
                    Text("CANCEL", color = P.subInk)
                }
                Spacer(Modifier.width(8.dp))
                GlassPrimaryButton(
                    text = "APPLY",
                    onClick = { onApply(resolvedRange) },
                    modifier = Modifier.width(110.dp).testTag("picker_apply"),
                )
            }
        }
    }
}
