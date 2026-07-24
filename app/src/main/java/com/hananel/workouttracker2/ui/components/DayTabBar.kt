package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.ui.theme.AccentGradient
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.rememberGlassColors

/** Per-day dot state shown under each tab label. */
data class DayTabState(
    val dayIndex: Int,
    val progress: Float = 0f,     // 0..1 of that day's exercises completed
    val hasWorkout: Boolean = true,
    val isToday: Boolean = false
)

/**
 * One continuous glass bar divided into day segments, with a gradient pill that slides between
 * them. Replaces the old grid of separate cubes that scrolled vertically and ate a third of the
 * screen — this is a single fixed-height row that always fills the width, whatever the day count.
 */
@Composable
fun DayTabBar(
    selectedDayIndex: Int,
    days: List<DayTabState>,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val sorted = remember(days) { days.sortedBy { it.dayIndex } }
    val selectedPosition = sorted.indexOfFirst { it.dayIndex == selectedDayIndex }.coerceAtLeast(0)
    val glass = rememberGlassColors()
    val barShape = RoundedCornerShape(22.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(60.dp)
            .clip(barShape)
            .background(glass.fill, barShape)
            .border(1.dp, glass.border, barShape)
            .padding(5.dp)
    ) {
        val segmentWidth = maxWidth / sorted.size
        // offset() is layout-direction aware, so the pill tracks the correct segment in RTL too.
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedPosition,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
            label = "dayThumb"
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(AccentGradient))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            sorted.forEach { day ->
                DayTab(
                    state = day,
                    isSelected = day.dayIndex == selectedDayIndex,
                    onClick = { onDaySelected(day.dayIndex) },
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun DayTab(
    state: DayTabState,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            !state.hasWorkout -> glass.onGlassMuted.copy(alpha = 0.5f)
            else -> glass.onGlassMuted
        },
        animationSpec = tween(220),
        label = "dayLabel"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "dayScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayShortName(state.dayIndex),
                fontSize = (15 * scale).sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            DayStatusDot(state = state, isSelected = isSelected)
        }
    }
}

/**
 * Tiny status marker: a filled dot for a finished day, a half-lit one for work in progress, a
 * hollow ring for untouched, and nothing at all for a rest day.
 */
@Composable
private fun DayStatusDot(state: DayTabState, isSelected: Boolean) {
    val glass = rememberGlassColors()
    if (!state.hasWorkout) {
        Box(Modifier.size(6.dp))
        return
    }

    val done = state.progress >= 1f
    val started = state.progress > 0f
    val color = when {
        isSelected && done -> Color.White
        isSelected -> Color.White.copy(alpha = if (started) 0.85f else 0.4f)
        done -> Mint
        started -> MaterialTheme.colorScheme.primary
        else -> glass.onGlassMuted.copy(alpha = 0.45f)
    }

    if (done || started) {
        Box(
            Modifier
                .size(if (state.isToday) 7.dp else 6.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    } else {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .border(1.5.dp, color, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun dayShortName(dayIndex: Int): String = stringResource(
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
