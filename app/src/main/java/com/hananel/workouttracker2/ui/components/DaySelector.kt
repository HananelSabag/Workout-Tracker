package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R

@Composable
fun DaySelector(
    selectedDayIndex: Int,
    enabledDays: List<Int>,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = enabledDays.indexOf(selectedDayIndex).coerceAtLeast(0),
        modifier = modifier,
        edgePadding = 16.dp
    ) {
        enabledDays.forEach { dayIndex ->
            Tab(
                selected = dayIndex == selectedDayIndex,
                onClick = { onDaySelected(dayIndex) },
                text = {
                    Text(
                        text = getDayName(dayIndex),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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

