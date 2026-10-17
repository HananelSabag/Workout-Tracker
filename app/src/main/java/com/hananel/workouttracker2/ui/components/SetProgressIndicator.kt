package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.ui.theme.CompletedGreen
import com.hananel.workouttracker2.ui.theme.CurrentBlue
import com.hananel.workouttracker2.ui.theme.PendingGray

@Composable
fun SetProgressIndicator(
    completedSets: Int,
    currentSet: Int,
    totalSets: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSets) { index ->
            val setNumber = index + 1
            val isCurrent = setNumber == currentSet
            val isDone = setNumber < currentSet || setNumber <= completedSets

            val dotColor by animateColorAsState(
                targetValue = when {
                    isDone -> CompletedGreen
                    isCurrent -> CurrentBlue
                    else -> PendingGray
                },
                animationSpec = tween(400),
                label = "dotColor_$index"
            )

            // Current set dot is slightly larger
            val dotSize by animateDpAsState(
                targetValue = if (isCurrent) 14.dp else 10.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label = "dotSize_$index"
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
