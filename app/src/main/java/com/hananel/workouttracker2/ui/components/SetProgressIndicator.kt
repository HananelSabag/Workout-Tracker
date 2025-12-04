package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSets) { index ->
            val setNumber = index + 1
            val color = when {
                setNumber < currentSet || setNumber <= completedSets -> CompletedGreen
                setNumber == currentSet -> CurrentBlue
                else -> PendingGray
            }
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

