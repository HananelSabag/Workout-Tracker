package com.hananel.workouttracker2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.SessionExerciseLog
import com.hananel.workouttracker2.data.models.WorkoutSession
import com.hananel.workouttracker2.ui.components.GhostButton
import com.hananel.workouttracker2.ui.components.GlassSheet
import com.hananel.workouttracker2.ui.components.GradientButton
import com.hananel.workouttracker2.ui.components.formatVolume
import com.hananel.workouttracker2.ui.components.formatWeightValue
import com.hananel.workouttracker2.ui.theme.AppBackground
import com.hananel.workouttracker2.ui.theme.Electric
import com.hananel.workouttracker2.ui.theme.GlassCard
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.Violet
import com.hananel.workouttracker2.ui.theme.rememberGlassColors
import com.hananel.workouttracker2.utils.DateUtils

/**
 * Workout history — "what did I do last time".
 *
 * Each row expands into the exercise-by-exercise log that was captured live during the session:
 * sets actually completed vs planned, the weight used, and which exercises were finished.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<WorkoutSession>,
    onNavigateBack: () -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    var pendingDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.workout_history),
                            style = MaterialTheme.typography.headlineSmall,
                            color = glass.onGlass
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = glass.onGlass
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            modifier = modifier
        ) { padding ->
            if (sessions.isEmpty()) {
                EmptyHistory(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { HistoryTotals(sessions = sessions) }
                    items(sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            onDelete = { pendingDelete = session }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { session ->
        GlassSheet(
            title = stringResource(R.string.delete_session),
            subtitle = session.dayName.ifBlank { DateUtils.formatShortDate(session.startTime) },
            icon = Icons.Default.Delete,
            accent = MaterialTheme.colorScheme.error,
            onDismiss = { pendingDelete = null }
        ) {
            Text(
                text = stringResource(R.string.delete_session_message),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.onGlassMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostButton(
                    text = stringResource(R.string.cancel),
                    onClick = { pendingDelete = null },
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = stringResource(R.string.delete),
                    gradient = listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                    ),
                    onClick = {
                        onDeleteSession(session.id)
                        pendingDelete = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Rolling totals across everything recorded — the "am I actually showing up" number. */
@Composable
private fun HistoryTotals(sessions: List<WorkoutSession>) {
    val glass = rememberGlassColors()
    val weekStart = remember { DateUtils.getCurrentWeekStart() }
    val thisWeek = sessions.count { it.dateIso >= weekStart }
    val totalMinutes = sessions.sumOf { DateUtils.durationMinutes(it.durationMs) }
    val totalSets = sessions.sumOf { it.totalSets }

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TotalTile(
                value = "$thisWeek",
                label = stringResource(R.string.workouts_this_week),
                tint = Electric,
                modifier = Modifier.weight(1f)
            )
            TotalTile(
                value = "${sessions.size}",
                label = stringResource(R.string.total_workouts),
                tint = Mint,
                modifier = Modifier.weight(1f)
            )
            TotalTile(
                value = "$totalSets",
                label = stringResource(R.string.total_sets),
                tint = Violet,
                modifier = Modifier.weight(1f)
            )
            TotalTile(
                // Bare minutes under an hour used to render as a unit-less number under a
                // "total time" label, which reads as anything at all.
                value = if (totalMinutes >= 60) "${totalMinutes / 60}${stringResource(R.string.hours_short)}"
                else "$totalMinutes${stringResource(R.string.minutes_short)}",
                label = stringResource(R.string.total_time),
                tint = glass.onGlassMuted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TotalTile(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    val glass = rememberGlassColors()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.onGlassMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun SessionRow(session: WorkoutSession, onDelete: () -> Unit) {
    val glass = rememberGlassColors()
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "chevron"
    )
    val daysAgo = DateUtils.daysAgo(session.startTime)
    val whenLabel = when (daysAgo) {
        0 -> stringResource(R.string.today)
        1 -> stringResource(R.string.yesterday)
        else -> DateUtils.formatFullDate(session.startTime)
    }

    GlassCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.dayName.ifBlank { stringResource(R.string.workout) },
                        style = MaterialTheme.typography.titleLarge,
                        color = glass.onGlass
                    )
                    Text(
                        text = "$whenLabel · ${DateUtils.formatClock(session.startTime)}–" +
                                DateUtils.formatClock(session.endTime ?: session.lastActivityTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.onGlassMuted
                    )
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = glass.onGlassMuted,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionStat(
                    icon = Icons.Default.Schedule,
                    text = stringResource(R.string.minutes_value, DateUtils.durationMinutes(session.durationMs)),
                    tint = Electric,
                    modifier = Modifier.weight(1f)
                )
                SessionStat(
                    icon = Icons.Default.FitnessCenter,
                    text = stringResource(R.string.sets_value, session.totalSets),
                    tint = Violet,
                    modifier = Modifier.weight(1f)
                )
                SessionStat(
                    icon = Icons.Default.Check,
                    text = "${session.completedExerciseCount}/${
                        session.plannedExercises.coerceAtLeast(session.exercises.size)
                    }",
                    tint = Mint,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = glass.hairline)
                    session.exercises.forEach { entry -> ExerciseLogRow(entry) }
                    if (session.totalVolume > 0f) {
                        Text(
                            text = stringResource(
                                R.string.total_volume_value,
                                formatVolume(session.totalVolume)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.onGlassMuted
                        )
                    }
                    GhostButton(
                        text = stringResource(R.string.delete),
                        icon = Icons.Default.Delete,
                        onClick = onDelete,
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        height = 42.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionStat(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier) {
    val glass = rememberGlassColors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(glass.onGlass.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = glass.onGlass,
            maxLines = 1
        )
    }
}

@Composable
private fun ExerciseLogRow(entry: SessionExerciseLog) {
    val glass = rememberGlassColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (entry.completed) Mint else glass.onGlassMuted.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (entry.completed) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            color = glass.onGlass,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = buildString {
                append("${entry.completedSets}/${entry.plannedSets}")
                if (entry.weight > 0f) append(" · ${formatWeightValue(entry.weight)}")
            },
            style = MaterialTheme.typography.labelMedium,
            color = glass.onGlassMuted
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    val glass = rememberGlassColors()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = glass.onGlassMuted.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.no_history),
                style = MaterialTheme.typography.headlineSmall,
                color = glass.onGlass,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.no_history_message),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.onGlassMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
