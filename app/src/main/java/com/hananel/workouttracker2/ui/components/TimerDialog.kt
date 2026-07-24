package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.hananel.workouttracker2.ui.theme.AccentGradient
import com.hananel.workouttracker2.ui.theme.Amber
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.SuccessGradient
import com.hananel.workouttracker2.ui.theme.chipTint
import com.hananel.workouttracker2.ui.theme.glassSheetColor
import com.hananel.workouttracker2.ui.theme.rememberGlassColors

/**
 * Rest timer sheet.
 *
 * While the countdown runs the primary action skips the rest; once it finishes the primary action
 * logs the set the user has just performed — the same repository call the card uses, so the two
 * can't disagree about how many sets are done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerBottomSheet(
    timerState: TimerState,
    onCompleteSet: () -> Unit,
    onSkipRest: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassSheetColor(),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 2.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(glass.onGlassMuted.copy(alpha = 0.35f))
            )
        },
        modifier = modifier
    ) {
        TimerSheetContent(
            timerState = timerState,
            onCompleteSet = onCompleteSet,
            onSkipRest = onSkipRest,
            onPauseResume = onPauseResume,
            onStop = onStop
        )
    }
}

@Composable
private fun TimerSheetContent(
    timerState: TimerState,
    onCompleteSet: () -> Unit,
    onSkipRest: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val glass = rememberGlassColors()
    val timeLeft = timerState.timeLeft
    // Trust the explicit flag; fall back to the legacy condition for states persisted before it.
    val isRestComplete = timerState.isRestComplete || (timeLeft == 0 && !timerState.isRunning && !timerState.isPaused)
    val isPaused = timerState.isPaused
    val nextSet = timerState.nextSet

    val accent by animateColorAsState(
        targetValue = when {
            isRestComplete -> Mint
            isPaused -> glass.onGlassMuted
            else -> Amber
        },
        animationSpec = tween(600),
        label = "timerAccent"
    )

    val pulse = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isRestComplete) 1.05f else 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.rest_timer),
                    style = MaterialTheme.typography.headlineSmall,
                    color = glass.onGlass
                )
                Text(
                    text = timerState.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.onGlassMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(chipTint(accent))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.set_of, nextSet, timerState.totalSets),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Circular countdown ──────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(210.dp)
                .scale(if (isRestComplete) pulseScale else 1f)
        ) {
            CircularTimerArc(
                timeLeft = timeLeft,
                totalTime = timerState.totalTime,
                arcColor = accent,
                trackColor = glass.onGlassMuted.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxSize()
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(targetState = formatTime(timeLeft), label = "timeText") { text ->
                    Text(
                        text = text,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isRestComplete) accent else glass.onGlass,
                        lineHeight = 54.sp
                    )
                }
                Text(
                    text = when {
                        isRestComplete -> stringResource(R.string.rest_complete)
                        isPaused -> stringResource(R.string.paused)
                        else -> stringResource(R.string.resting_now)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRestComplete) accent else glass.onGlassMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SetProgressIndicator(
            completedSets = timerState.completedSets,
            currentSet = nextSet,
            totalSets = timerState.totalSets
        )

        Spacer(modifier = Modifier.height(26.dp))

        // ── Actions ─────────────────────────────────────────────────────────
        if (isRestComplete) {
            GradientButton(
                text = stringResource(R.string.finish_set_n, nextSet, timerState.totalSets),
                icon = Icons.Default.Check,
                gradient = SuccessGradient,
                onClick = onCompleteSet,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                fontSize = 16
            )
        } else {
            GradientButton(
                text = stringResource(R.string.skip_rest),
                icon = Icons.Default.FastForward,
                gradient = AccentGradient,
                onClick = onSkipRest,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                fontSize = 16
            )
            Spacer(modifier = Modifier.height(10.dp))
            GhostButton(
                text = if (isPaused) stringResource(R.string.resume) else stringResource(R.string.pause),
                icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                onClick = onPauseResume,
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.stop),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CircularTimerArc(
    timeLeft: Int,
    totalTime: Int,
    arcColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "arcProgress"
    )
    val sweepBrush = Brush.sweepGradient(listOf(arcColor, arcColor.copy(alpha = 0.55f), arcColor))

    Canvas(modifier = modifier) {
        val strokeWidth = 15.dp.toPx()
        val glowWidth = 26.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        if (animatedProgress > 0f) {
            // Wide, faint pass first — reads as a glow bleeding through the glass.
            drawArc(
                color = arcColor.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = glowWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = sweepBrush,
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

private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
