package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.TimerState

/**
 * Modern ModalBottomSheet timer — slides up from the bottom.
 * Replaces the old center-screen Dialog with a more natural mobile UX.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerBottomSheet(
    timerState: TimerState,
    onNextSet: () -> Unit,
    onFinish: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            // Custom drag handle with a pill shape
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .let { m ->
                        m // background handled by ModalBottomSheet's containerColor
                    }
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        TimerBottomSheetContent(
            timerState = timerState,
            onNextSet = onNextSet,
            onFinish = onFinish,
            onStop = onStop
        )
    }
}

@Composable
private fun TimerBottomSheetContent(
    timerState: TimerState,
    onNextSet: () -> Unit,
    onFinish: () -> Unit,
    onStop: () -> Unit
) {
    val timeLeft = timerState.timeLeft
    val isLastSet = timerState.currentSet >= timerState.totalSets
    val isRestComplete = timeLeft == 0

    // Smooth color transition: blue (running) → green (complete)
    val accentColor by animateColorAsState(
        targetValue = if (isRestComplete) Color(0xFF10B981) else Color(0xFF3B82F6),
        animationSpec = tween(600),
        label = "accentColor"
    )
    val accentColorDark by animateColorAsState(
        targetValue = if (isRestComplete) Color(0xFF059669) else Color(0xFF2563EB),
        animationSpec = tween(600),
        label = "accentColorDark"
    )

    // Pulse scale animation when rest is complete
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRestComplete) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                // Gradient header strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { m ->
                                m // gradient drawn by Canvas below for animatable colors
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(accentColor, accentColorDark)
                                )
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.rest_timer),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = timerState.exerciseName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            // Set badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.set_of,
                                        timerState.currentSet,
                                        timerState.totalSets
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Circular Timer ────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(if (isRestComplete) pulseScale else 1f)
        ) {
            CircularTimerArc(
                timeLeft = timeLeft,
                totalTime = timerState.totalTime,
                arcColor = accentColor,
                modifier = Modifier.fillMaxSize()
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(timeLeft),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRestComplete) accentColor else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 48.sp
                )
                if (isRestComplete) {
                    Text(
                        text = if (isLastSet)
                            stringResource(R.string.all_sets_completed)
                        else
                            stringResource(R.string.rest_complete),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Set Progress Dots ─────────────────────────────────────────────────
        SetProgressIndicator(
            completedSets = timerState.completedSets,
            currentSet = timerState.currentSet,
            totalSets = timerState.totalSets
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Primary Action Button ─────────────────────────────────────────────
        Button(
            onClick = { if (isLastSet) onFinish() else onNextSet() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = if (isLastSet) Icons.Default.Check else Icons.Default.SkipNext,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isLastSet) stringResource(R.string.finish) else stringResource(R.string.next_set),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Stop (destructive, less prominent) ───────────────────────────────
        TextButton(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.stop),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CircularTimerArc(
    timeLeft: Int,
    totalTime: Int,
    arcColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "arcProgress"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val glowWidth = 22.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        // Background track
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Glow layer (wider, very transparent)
        if (animatedProgress > 0f) {
            drawArc(
                color = arcColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = glowWidth, cap = StrokeCap.Round)
            )
        }

        // Main progress arc
        if (animatedProgress > 0f) {
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

// ── Legacy alias — keeps backward compatibility if TimerDialog is referenced elsewhere ──
@Suppress("unused")
@Composable
fun TimerDialog(
    timerState: TimerState,
    onNextSet: () -> Unit,
    onFinish: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onTimerTick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    TimerBottomSheet(
        timerState = timerState,
        onNextSet = onNextSet,
        onFinish = onFinish,
        onStop = onStop,
        onDismiss = onClose,
        modifier = modifier
    )
}
