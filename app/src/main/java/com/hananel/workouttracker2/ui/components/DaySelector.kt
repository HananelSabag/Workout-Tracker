package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R

/**
 * Slim pill-tab day strip. No outer container surface — just a row of capsule chips.
 * Selected day = filled primary capsule. Unselected = transparent text.
 * Total height ~46 dp (vs the old ~68 dp box).
 */
@Composable
fun DaySelector(
    selectedDayIndex: Int,
    enabledDays: List<Int>,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (enabledDays.isEmpty()) return

    val sorted = remember(enabledDays) { enabledDays.sorted() }

    if (sorted.size <= 5) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            sorted.forEach { dayIndex ->
                DayTab(
                    dayIndex = dayIndex,
                    isSelected = dayIndex == selectedDayIndex,
                    onClick = { onDaySelected(dayIndex) }
                )
            }
        }
    } else {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(sorted) { dayIndex ->
                DayTab(
                    dayIndex = dayIndex,
                    isSelected = dayIndex == selectedDayIndex,
                    onClick = { onDaySelected(dayIndex) }
                )
            }
        }
    }
}

@Composable
private fun DayTab(
    dayIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "tabBg"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
        animationSpec = tween(200),
        label = "tabFg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = bg,
        modifier = Modifier
            .height(34.dp)
            .widthIn(min = 34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = getDayShortName(dayIndex),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                textAlign = TextAlign.Center
            )
        }
    }
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
