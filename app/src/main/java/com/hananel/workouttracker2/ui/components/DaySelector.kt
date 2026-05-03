package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
 * Slim segmented day picker. Two layouts auto-selected by density:
 *  - ≤5 days: evenly distributed cells that always fill the row width.
 *  - ≥6 days: tight scrollable strip with fixed-width cells.
 *
 * Replaces the old 56.dp pill grid that wasted a third of the screen on single-letter labels.
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
    val compact = sorted.size <= 5

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sorted.forEach { dayIndex ->
                    DayCell(
                        dayIndex = dayIndex,
                        isSelected = dayIndex == selectedDayIndex,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onDaySelected(dayIndex) }
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sorted) { dayIndex ->
                    DayCell(
                        dayIndex = dayIndex,
                        isSelected = dayIndex == selectedDayIndex,
                        modifier = Modifier.width(54.dp).fillMaxHeight(),
                        onClick = { onDaySelected(dayIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayIndex: Int,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        animationSpec = tween(220),
        label = "dayBg"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "dayFg"
    )
    val elev by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "dayElev"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        shadowElevation = elev
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = getDayShortName(dayIndex),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
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
