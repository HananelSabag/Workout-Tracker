package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(enabledDays) { dayIndex ->
            DayPillButton(
                dayName = getDayShortName(dayIndex),
                isSelected = dayIndex == selectedDayIndex,
                onClick = { onDaySelected(dayIndex) }
            )
        }
    }
}

@Composable
private fun DayPillButton(
    dayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF3B82F6)
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(250),
        label = "dayBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "dayText"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "dayElev"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "dayScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .size(width = 56.dp, height = 56.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        shadowElevation = shadowElevation
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = dayName,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor,
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
