package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.TimerState
import kotlinx.coroutines.delay

@Composable
fun TimerDialog(
    timerState: TimerState,
    onNextSet: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onTimerTick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use timeLeft directly from timerState - no local state needed
    // The timer is running in TimerService, not here
    val timeLeft = timerState.timeLeft
    
    Dialog(
        onDismissRequest = { /* Do nothing - only close button should dismiss */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with stop button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.rest_timer),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Exercise name
                Text(
                    text = timerState.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Circular timer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    CircularTimer(
                        timeLeft = timeLeft,
                        totalTime = timerState.totalTime,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(timeLeft),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 56.sp
                        )
                        Text(
                            text = stringResource(
                                R.string.set_of,
                                timerState.currentSet,
                                timerState.totalSets
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Set progress indicator
                SetProgressIndicator(
                    completedSets = timerState.completedSets,
                    currentSet = timerState.currentSet,
                    totalSets = timerState.totalSets,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                val isLastSet = timerState.currentSet >= timerState.totalSets
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Run in Background button
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.minimize))
                    }
                    
                    // Next Set / Finish button
                    Button(
                        onClick = onNextSet,
                        modifier = Modifier.weight(1f),
                        colors = if (isLastSet) {
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        if (isLastSet) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isLastSet) {
                                stringResource(R.string.finish)
                            } else {
                                stringResource(R.string.next_set)
                            }
                        )
                    }
                }
                
                // Show message only when timer finished AND it's the last set
                if (timeLeft == 0 && isLastSet) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.all_sets_completed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularTimer(
    timeLeft: Int,
    totalTime: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    
    Canvas(modifier = modifier) {
        val strokeWidth = 12.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val radius = diameter / 2f
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        
        // Background circle
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Progress arc
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

