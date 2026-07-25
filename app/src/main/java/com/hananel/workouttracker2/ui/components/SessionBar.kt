package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.WorkoutSession
import com.hananel.workouttracker2.ui.theme.Amber
import com.hananel.workouttracker2.ui.theme.Electric
import com.hananel.workouttracker2.ui.theme.GlassCard
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.Violet
import com.hananel.workouttracker2.ui.theme.rememberGlassColors
import com.hananel.workouttracker2.utils.DateUtils

/**
 * Live workout clock.
 *
 * The clock starts at the first logged set — not when the app opened — and it stops on its own ten
 * minutes after the last action, so the recorded length is actual training time.
 */
@Composable
fun ActiveSessionBar(
    session: WorkoutSession,
    now: Long,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    val elapsed = (now - session.startTime).coerceAtLeast(0L)
    val idleFor = now - session.lastActivityTime
    val goingStale = idleFor > WorkoutSession.IDLE_TIMEOUT_MS / 2

    val pulse = rememberInfiniteTransition(label = "sessionPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOut), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tint = if (goingStale) Amber else Mint,
        tintAlpha = 0.14f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (goingStale) Amber else Mint)
                            .alpha(dotAlpha)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.workout_in_progress),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (goingStale) Amber else Mint
                        )
                        Text(
                            text = stringResource(
                                R.string.started_at,
                                DateUtils.formatClock(session.startTime)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.onGlassMuted
                        )
                    }
                }
                // Labelled on purpose: there are two clocks on this screen and the big number
                // has to say which one it is without the user having to work it out.
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateUtils.formatDuration(elapsed),
                        fontSize = 30.sp,
                        style = MaterialTheme.typography.displaySmall,
                        color = glass.onGlass
                    )
                    Text(
                        text = stringResource(R.string.workout_duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.onGlassMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionMetric(
                    icon = Icons.Default.FitnessCenter,
                    value = "${session.totalSets}",
                    label = stringResource(R.string.sets),
                    tint = Electric,
                    modifier = Modifier.weight(1f)
                )
                SessionMetric(
                    icon = Icons.Default.CheckCircle,
                    value = "${session.completedExerciseCount}/${session.plannedExercises.coerceAtLeast(session.exercises.size)}",
                    label = stringResource(R.string.exercises_completed),
                    tint = Mint,
                    modifier = Modifier.weight(1f)
                )
                SessionMetric(
                    icon = Icons.Default.Timer,
                    value = if (session.totalVolume > 0f) formatVolume(session.totalVolume) else "—",
                    label = stringResource(R.string.total_volume),
                    tint = Violet,
                    modifier = Modifier.weight(1f)
                )
            }

            if (goingStale) {
                Text(
                    text = stringResource(
                        R.string.session_auto_close_hint,
                        ((WorkoutSession.IDLE_TIMEOUT_MS - idleFor) / 60000).coerceAtLeast(0).toInt() + 1
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber
                )
            }

            GhostButton(
                text = stringResource(R.string.end_workout),
                onClick = onEndSession,
                contentColor = glass.onGlassMuted,
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp
            )
        }
    }
}

@Composable
private fun SessionMetric(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(glass.onGlass.copy(alpha = 0.05f))
            .padding(vertical = 9.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = glass.onGlass)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.onGlassMuted,
            maxLines = 1
        )
    }
}

/**
 * Shown in place of the live bar when nothing is running: what the last workout was, when, and how
 * it went. Tapping it opens the full history.
 */
@Composable
fun LastSessionCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    val daysAgo = DateUtils.daysAgo(session.startTime)
    val whenLabel = when (daysAgo) {
        0 -> stringResource(R.string.today)
        1 -> stringResource(R.string.yesterday)
        else -> stringResource(R.string.days_ago, daysAgo)
    }

    GlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Electric.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Electric,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.last_workout),
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.onGlassMuted
                )
                Text(
                    text = session.dayName.ifBlank { stringResource(R.string.workout) } + " · $whenLabel",
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.onGlass
                )
                Text(
                    text = stringResource(
                        R.string.session_summary_line,
                        DateUtils.durationMinutes(session.durationMs),
                        session.totalSets,
                        session.completedExerciseCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.onGlassMuted
                )
            }
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = glass.onGlassMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** "1.2K" past a thousand kg — the metric tile is too narrow for six digits. */
fun formatVolume(volume: Float): String =
    if (volume >= 1000f) "%.1fK".format(volume / 1000f) else volume.toInt().toString()
