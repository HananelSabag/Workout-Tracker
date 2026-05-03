package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Day picker with real calendar dates. Two auto-selected layouts:
 *  - ≤5 enabled days → evenly distributed cells that fill the row.
 *  - ≥6 enabled days → scrollable strip with fixed-width cells.
 *
 * Each cell shows the abbreviated day name, the calendar day-of-month, and a dot when
 * that day is today. Selected day gets a solid primary-color fill; today (unselected)
 * gets a primary-colored border ring.
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

    // Compute today's day index (0=Sun … 6=Sat) and this week's Sunday.
    val today = remember { LocalDate.now() }
    val todayDayIndex = remember(today) {
        when (today.dayOfWeek) {
            DayOfWeek.SUNDAY    -> 0
            DayOfWeek.MONDAY    -> 1
            DayOfWeek.TUESDAY   -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY  -> 4
            DayOfWeek.FRIDAY    -> 5
            else                -> 6 // SATURDAY
        }
    }
    val thisSunday = remember(today) {
        val isoDay = today.dayOfWeek.value          // ISO: Mon=1, Sun=7
        val daysBack = if (isoDay == 7) 0L else isoDay.toLong()
        today.minusDays(daysBack)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sorted.forEach { dayIndex ->
                    DayCell(
                        dayIndex  = dayIndex,
                        dayNumber = thisSunday.plusDays(dayIndex.toLong()).dayOfMonth,
                        isSelected = dayIndex == selectedDayIndex,
                        isToday    = dayIndex == todayDayIndex,
                        modifier   = Modifier.weight(1f).fillMaxHeight(),
                        onClick    = { onDaySelected(dayIndex) }
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sorted) { dayIndex ->
                    DayCell(
                        dayIndex  = dayIndex,
                        dayNumber = thisSunday.plusDays(dayIndex.toLong()).dayOfMonth,
                        isSelected = dayIndex == selectedDayIndex,
                        isToday    = dayIndex == todayDayIndex,
                        modifier   = Modifier.width(62.dp).fillMaxHeight(),
                        onClick    = { onDaySelected(dayIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayIndex: Int,
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "dayBg"
    )
    val nameColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            isToday    -> MaterialTheme.colorScheme.primary
            else       -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "dayNameColor"
    )
    val numberColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday    -> MaterialTheme.colorScheme.primary
            else       -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(220),
        label = "dayNumColor"
    )
    val todayDotColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.primary,
        animationSpec = tween(220),
        label = "todayDot"
    )
    // Border ring for "today but not selected"
    val borderColor by animateColorAsState(
        targetValue = if (isToday && !isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else Color.Transparent,
        animationSpec = tween(220),
        label = "dayBorder"
    )
    val elev by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "dayElev"
    )

    Surface(
        onClick        = onClick,
        modifier       = modifier,
        shape          = RoundedCornerShape(14.dp),
        color          = bg,
        shadowElevation = elev,
        border         = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = getDayShortName(dayIndex),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                color      = nameColor,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text       = dayNumber.toString(),
                fontSize   = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = numberColor,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            // Dot visible only for today; invisible placeholder keeps layout stable.
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isToday) todayDotColor else Color.Transparent)
            )
        }
    }
}

@Composable
private fun getDayShortName(dayIndex: Int): String = stringResource(
    id = when (dayIndex) {
        0    -> R.string.sunday_short
        1    -> R.string.monday_short
        2    -> R.string.tuesday_short
        3    -> R.string.wednesday_short
        4    -> R.string.thursday_short
        5    -> R.string.friday_short
        6    -> R.string.saturday_short
        else -> R.string.sunday_short
    }
)
