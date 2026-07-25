package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.Exercise
import com.hananel.workouttracker2.data.models.SessionExerciseLog
import com.hananel.workouttracker2.ui.theme.AccentGradient
import com.hananel.workouttracker2.ui.theme.Amber
import com.hananel.workouttracker2.ui.theme.Electric
import com.hananel.workouttracker2.ui.theme.GlassCard
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.SuccessGradient
import com.hananel.workouttracker2.ui.theme.Violet
import com.hananel.workouttracker2.ui.theme.chipTint
import com.hananel.workouttracker2.ui.theme.rememberGlassColors
import com.hananel.workouttracker2.utils.RepsUtils

/**
 * Exercise card.
 *
 * The card has exactly one job during a workout: let the user log the set they just did. That
 * button is always present and always enabled until the exercise is finished — the old design
 * disabled it while the rest countdown ran, which is what forced people to tick exercises by hand.
 *
 * States: IDLE → ACTIVE (a set is logged) → RESTING (countdown under the button) → COMPLETED.
 */
@Composable
fun ExerciseCard(
    exercise: Exercise,
    isCompleted: Boolean,
    completedSets: Int,
    weight: Float,
    onCompleteSet: () -> Unit,
    onUndoSet: () -> Unit,
    onWeightChange: (Float) -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    restTimeLeft: Int = 0,
    restTotalTime: Int = 0,
    isRestRunningForThis: Boolean = false,
    isRestCompleteForThis: Boolean = false,
    previousLog: SessionExerciseLog? = null,
    onSkipRest: () -> Unit = {},
    onEditExercise: ((sets: Int, reps: String, restTime: Int, weight: Float) -> Unit)? = null,
    onDeleteExercise: (() -> Unit)? = null
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var showWeightSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val glass = rememberGlassColors()
    val tint = when {
        isCompleted -> Mint
        isRestCompleteForThis -> Mint
        isRestRunningForThis -> Amber
        completedSets > 0 -> Electric
        else -> Color.Unspecified
    }
    val nextSet = (completedSets + 1).coerceAtMost(exercise.sets)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        tint = tint,
        tintAlpha = if (isCompleted) 0.20f else 0.14f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // ── Header: name + overflow menu ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.customName.ifBlank { stringResource(id = exercise.nameResId) },
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 19.sp,
                        color = glass.onGlass,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (exercise.equipment.isNotBlank()) {
                        Text(
                            text = exercise.equipment,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.onGlassMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Box {
                    if (isCompleted) {
                        CompletionBadge(onClick = { showMenu = true })
                    } else {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = glass.onGlassMuted
                            )
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isCompleted) stringResource(R.string.unmark_complete)
                                    else stringResource(R.string.mark_complete)
                                )
                            },
                            onClick = { showMenu = false; onToggleComplete() },
                            leadingIcon = {
                                Icon(if (isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = { showMenu = false; showEditSheet = true },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.weight)) },
                            onClick = { showMenu = false; showWeightSheet = true },
                            leadingIcon = { Icon(Icons.Default.FitnessCenter, null) }
                        )
                        if (onDeleteExercise != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { showMenu = false; onDeleteExercise() },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }

            // ── Stat chips ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip(
                    icon = Icons.Default.Repeat,
                    label = "${exercise.sets} × ${exercise.reps}",
                    tint = Electric,
                    numeric = true
                )
                if (exercise.restTime > 0) {
                    StatChip(
                        icon = Icons.Default.Timer,
                        label = "${exercise.restTime} ${stringResource(R.string.seconds_short)}",
                        tint = Amber,
                        numeric = true
                    )
                }
                if (exercise.hasWeight) {
                    StatChip(
                        icon = Icons.Default.FitnessCenter,
                        label = if (weight > 0f) "${formatWeightValue(weight)} ${stringResource(R.string.kg)}"
                        else stringResource(R.string.set_weight),
                        tint = Violet,
                        onClick = { showWeightSheet = true }
                    )
                }
            }

            // ── What happened last time ─────────────────────────────────────
            if (previousLog != null && previousLog.completedSets > 0) {
                PreviousSessionLine(log = previousLog)
            }

            // ── Set progress ────────────────────────────────────────────────
            SetTrack(
                completedSets = completedSets,
                totalSets = exercise.sets,
                isResting = isRestRunningForThis,
                isCompleted = isCompleted
            )

            // ── Rest countdown strip ────────────────────────────────────────
            AnimatedVisibility(
                visible = isRestRunningForThis || isRestCompleteForThis,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RestStrip(
                    timeLeft = restTimeLeft,
                    totalTime = restTotalTime,
                    isComplete = isRestCompleteForThis,
                    nextSet = nextSet,
                    totalSets = exercise.sets,
                    onSkip = onSkipRest
                )
            }

            // ── Primary action ──────────────────────────────────────────────
            if (isCompleted) {
                GhostButton(
                    text = stringResource(R.string.unmark_complete),
                    icon = Icons.AutoMirrored.Filled.Undo,
                    onClick = onToggleComplete,
                    contentColor = Mint,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (completedSets > 0) {
                        UndoSetButton(onClick = onUndoSet)
                    }
                    GradientButton(
                        text = if (nextSet >= exercise.sets && exercise.sets > 0)
                            stringResource(R.string.finish_last_set)
                        else
                            stringResource(R.string.finish_set_n, nextSet, exercise.sets),
                        icon = Icons.Default.Check,
                        gradient = if (nextSet >= exercise.sets) SuccessGradient else AccentGradient,
                        onClick = onCompleteSet,
                        modifier = Modifier.weight(1f),
                        height = 50.dp,
                        fontSize = 15
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        ExerciseEditSheet(
            exercise = exercise,
            currentWeight = weight,
            onDismiss = { showEditSheet = false },
            onSave = { sets, reps, restTime, newWeight ->
                onEditExercise?.invoke(sets, reps, restTime, newWeight) ?: onWeightChange(newWeight)
            }
        )
    }
    if (showWeightSheet) {
        WeightSheet(
            exerciseName = exercise.customName.ifBlank { stringResource(id = exercise.nameResId) },
            initialWeight = weight,
            onDismiss = { showWeightSheet = false },
            onSave = {
                onWeightChange(it)
                showWeightSheet = false
            }
        )
    }
}

// ─── Pieces ──────────────────────────────────────────────────────────────────

@Composable
private fun CompletionBadge(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(SuccessGradient))
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * One quiet line of the previous session's numbers. This is the question every lifter asks between
 * sets, and it used to require digging through history.
 */
@Composable
private fun PreviousSessionLine(log: SessionExerciseLog) {
    val glass = rememberGlassColors()
    val reps = RepsUtils.formatSetReps(log.repsPerSet)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(glass.onGlass.copy(alpha = 0.04f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = glass.onGlassMuted,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.last_time),
            style = MaterialTheme.typography.labelSmall,
            color = glass.onGlassMuted
        )
        Text(
            text = buildString {
                append(if (reps.isNotBlank()) reps else "${log.completedSets}×")
                if (log.weight > 0f) append("  ·  ${formatWeightValue(log.weight)}")
            },
            // Numbers only — keep them left-to-right under Hebrew.
            style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr),
            color = glass.onGlass,
            modifier = Modifier.weight(1f)
        )
        if (log.weight > 0f) {
            Text(
                text = stringResource(R.string.kg),
                style = MaterialTheme.typography.labelSmall,
                color = glass.onGlassMuted
            )
        }
    }
}

/**
 * Sets as a segmented bar rather than dots — with 5+ sets, dots become an unreadable line of specks
 * and give no sense of "how much is left".
 */
@Composable
private fun SetTrack(
    completedSets: Int,
    totalSets: Int,
    isResting: Boolean,
    isCompleted: Boolean
) {
    val glass = rememberGlassColors()
    val safeTotal = totalSets.coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(safeTotal) { index ->
                val setNumber = index + 1
                val isDone = setNumber <= completedSets
                val isNext = setNumber == completedSets + 1 && !isCompleted

                val color by animateColorAsState(
                    targetValue = when {
                        isDone -> Mint
                        isNext && isResting -> Amber.copy(alpha = 0.55f)
                        isNext -> Electric.copy(alpha = 0.55f)
                        else -> glass.onGlassMuted.copy(alpha = 0.22f)
                    },
                    animationSpec = tween(320),
                    label = "segment_$index"
                )
                val height by animateFloatAsState(
                    targetValue = if (isNext) 10f else 7f,
                    animationSpec = spring(dampingRatio = 0.6f),
                    label = "segH_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }
        }
        Text(
            text = "$completedSets/$safeTotal",
            style = MaterialTheme.typography.labelLarge,
            color = if (isCompleted) Mint else glass.onGlassMuted
        )
    }
}

/** Inline rest countdown so the user never has to open the sheet to see how long is left. */
@Composable
private fun RestStrip(
    timeLeft: Int,
    totalTime: Int,
    isComplete: Boolean,
    nextSet: Int,
    totalSets: Int,
    onSkip: () -> Unit
) {
    val glass = rememberGlassColors()
    val accent = if (isComplete) Mint else Amber
    val shape = RoundedCornerShape(16.dp)

    val pulse = rememberInfiniteTransition(label = "restPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isComplete) 0.5f else 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = EaseInOut), RepeatMode.Reverse),
        label = "restPulseAlpha"
    )
    val progress by animateFloatAsState(
        targetValue = if (totalTime > 0) timeLeft.toFloat() / totalTime else 0f,
        animationSpec = tween(900),
        label = "restProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(chipTint(accent))
            .border(1.dp, accent.copy(alpha = 0.3f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isComplete) Icons.Default.Check else Icons.Default.Timer,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(17.dp)
                        .alpha(if (isComplete) pulseAlpha else 1f)
                )
                Text(
                    text = if (isComplete) stringResource(R.string.time_for_set, nextSet, totalSets)
                    else stringResource(R.string.resting_now),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            if (isComplete) {
                Text(
                    text = stringResource(R.string.rest_complete),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%d:%02d".format(timeLeft / 60, timeLeft % 60),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = onSkip,
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.FastForward,
                                contentDescription = stringResource(R.string.skip_rest),
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        if (!isComplete) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
private fun UndoSetButton(onClick: () -> Unit) {
    val glass = rememberGlassColors()
    Surface(
        onClick = onClick,
        modifier = Modifier.size(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(glass.fill, RoundedCornerShape(16.dp))
                .border(1.dp, glass.border, RoundedCornerShape(16.dp))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.undo_set),
                tint = glass.onGlassMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Sheets ──────────────────────────────────────────────────────────────────

@Composable
fun WeightSheet(
    exerciseName: String,
    initialWeight: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var value by remember { mutableFloatStateOf(initialWeight) }

    GlassSheet(
        title = stringResource(R.string.weight),
        subtitle = exerciseName,
        icon = Icons.Default.FitnessCenter,
        accent = Violet,
        onDismiss = onDismiss
    ) {
        WeightStepper(
            value = value,
            onValueChange = { value = it },
            label = stringResource(R.string.working_weight),
            unit = stringResource(R.string.kg),
            accent = Violet,
            modifier = Modifier.fillMaxWidth()
        )

        // Common plate loads — faster than nudging 2.5 at a time from zero.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10f, 20f, 30f, 40f, 60f).forEach { preset ->
                val selected = value == preset
                Surface(
                    onClick = { value = preset },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) Violet.copy(alpha = 0.25f) else chipTint(Violet),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formatWeightValue(preset),
                        color = Violet,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = stringResource(R.string.save),
                onClick = { onSave(value) },
                gradient = listOf(Violet, Color(0xFF6D28D9)),
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

@Composable
fun ExerciseEditSheet(
    exercise: Exercise,
    currentWeight: Float,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: String, restTime: Int, weight: Float) -> Unit
) {
    var sets by remember { mutableIntStateOf(exercise.sets.coerceIn(1, 20)) }
    var reps by remember { mutableStateOf(exercise.reps) }
    var restTime by remember { mutableIntStateOf(exercise.restTime.coerceIn(0, 600)) }
    var weight by remember { mutableFloatStateOf(currentWeight) }

    GlassSheet(
        title = stringResource(R.string.edit),
        subtitle = exercise.customName.ifBlank { stringResource(id = exercise.nameResId) },
        icon = Icons.Default.Edit,
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NumberStepper(
                label = stringResource(R.string.sets),
                value = sets,
                onValueChange = { sets = it },
                range = 1..20,
                icon = Icons.Default.Repeat,
                accent = Electric,
                modifier = Modifier.weight(1f)
            )
            NumberStepper(
                label = stringResource(R.string.rest_time_seconds),
                value = restTime,
                onValueChange = { restTime = it },
                step = 15,
                range = 0..600,
                icon = Icons.Default.Timer,
                accent = Amber,
                modifier = Modifier.weight(1f)
            )
        }

        GlassTextField(
            value = reps,
            onValueChange = { reps = it },
            label = stringResource(R.string.reps),
            placeholder = "10-12",
            leadingIcon = Icons.Default.Repeat,
            isError = reps.isBlank()
        )

        if (exercise.hasWeight) {
            WeightStepper(
                value = weight,
                onValueChange = { weight = it },
                label = stringResource(R.string.working_weight),
                unit = stringResource(R.string.kg),
                accent = Violet,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = stringResource(R.string.save),
                onClick = {
                    if (reps.isNotBlank()) {
                        onSave(sets, reps, restTime, weight)
                        onDismiss()
                    }
                },
                enabled = reps.isNotBlank(),
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

/** Legacy alias — older call sites (onboarding / builder) still reference the dialog name. */
@Composable
fun ExerciseEditDialog(
    exercise: Exercise,
    currentWeight: Float,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: String, restTime: Int, weight: Float) -> Unit
) = ExerciseEditSheet(exercise, currentWeight, onDismiss, onSave)
