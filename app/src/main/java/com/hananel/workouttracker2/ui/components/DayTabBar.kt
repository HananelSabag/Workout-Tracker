package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.ui.theme.rememberGlassColors
import java.time.LocalDate

data class DayTabState(val dayIndex: Int, val progress: Float = 0f, val hasWorkout: Boolean = true,
    val isToday: Boolean = false, val workoutName: String = "")

@Composable
fun DayTabBar(selectedDayIndex: Int, days: List<DayTabState>, onDaySelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    val glass = rememberGlassColors()
    val today = LocalDate.now()
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    Surface(modifier.fillMaxWidth().padding(horizontal = 16.dp).background(glass.fill, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, glass.border)) {
        Row(Modifier.padding(5.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            days.sortedBy { it.dayIndex }.forEach { day ->
                val chosen = day.dayIndex == selectedDayIndex
                val dayName = stringResource(when(day.dayIndex) {
                    1 -> R.string.monday_short; 2 -> R.string.tuesday_short; 3 -> R.string.wednesday_short
                    4 -> R.string.thursday_short; 5 -> R.string.friday_short; 6 -> R.string.saturday_short
                    else -> R.string.sunday_short
                })
                val label = day.workoutName.ifBlank { stringResource(R.string.rest_day) }
                val foreground = if (chosen) MaterialTheme.colorScheme.onPrimary else glass.onGlass
                Surface(onClick = { onDaySelected(day.dayIndex) },
                    modifier = Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = "$dayName, $label"; selected = chosen },
                    shape = RoundedCornerShape(16.dp),
                    color = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = if (day.isToday && !chosen) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
                    Column(Modifier.heightIn(min = 104.dp).padding(horizontal = 2.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(dayName, style = MaterialTheme.typography.labelSmall, color = foreground, maxLines = 1)
                        Text(sunday.plusDays(day.dayIndex.toLong()).dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = foreground)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground, minLines = 2, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        if (day.hasWorkout) Text(if (day.progress >= 1f) "✓" else if (day.progress > 0f) "◐" else "·", color = foreground)
                    }
                }
            }
        }
    }
}
