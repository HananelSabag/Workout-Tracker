package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R

@Composable
fun DaySelector(
    selectedDayIndex: Int,
    enabledDays: List<Int>,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Compact grid layout - all days visible without scrolling
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        enabledDays.forEach { dayIndex ->
            FilterChip(
                selected = dayIndex == selectedDayIndex,
                onClick = { onDaySelected(dayIndex) },
                label = {
                    Text(
                        text = getDayShortName(dayIndex),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (dayIndex == selectedDayIndex) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = if (dayIndex == selectedDayIndex) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else null,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            )
        }
    }
}

@Composable
private fun getDayName(dayIndex: Int): String {
    return stringResource(
        id = when (dayIndex) {
            0 -> R.string.day_sunday
            1 -> R.string.day_monday
            2 -> R.string.day_tuesday
            3 -> R.string.day_wednesday
            4 -> R.string.day_thursday
            5 -> R.string.day_friday
            6 -> R.string.day_saturday
            else -> R.string.day_sunday
        }
    )
}

@Composable
private fun getDayShortName(dayIndex: Int): String {
    return stringResource(
        id = when (dayIndex) {
            0 -> R.string.sunday_short
            1 -> R.string.monday_short
            2 -> R.string.tuesday_short
            3 -> R.string.wednesday_short
            4 -> R.string.thursday_short
            5 -> R.string.friday_short
            6 -> R.string.saturday_short
            else -> R.string.sunday_short
        }
    )
}

