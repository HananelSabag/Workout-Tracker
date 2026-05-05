package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.Exercise

// ─── Color tokens ────────────────────────────────────────────────────────────

private val AccentBlue   = Color(0xFF3B82F6)
private val AccentGreen  = Color(0xFF10B981)
private val AccentGreenDark = Color(0xFF059669)
private val AccentAmber  = Color(0xFFF59E0B)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentTeal   = Color(0xFF14B8A6)
private val AccentOrange = Color(0xFFF97316)
private val AccentPink   = Color(0xFFEC4899)

// Rotating palette for the top accent strip — each exercise gets its own color
private val accentPalette = listOf(AccentBlue, AccentViolet, AccentTeal, AccentOrange, AccentPink)

// ─── Card ────────────────────────────────────────────────────────────────────

/**
 * Compact exercise card with a colored top accent strip, inline stats chips
 * (including weight), and a small right-aligned action button instead of a
 * full-width primary button that dominated the layout.
 *
 * Layout:
 *  ╔══ 4 dp color accent (animated to green when done) ══╗
 *  ║  Exercise name                                  ⋮  ║
 *  ║  [5 × 10-12]  [60s]  [22.5 kg ✏]                  ║
 *  ║  ●●●○○                          [▶ Start rest]      ║
 *  ╚═════════════════════════════════════════════════════╝
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    exercise: Exercise,
    isCompleted: Boolean,
    completedSets: Int,
    weight: Float,
    onWeightChange: (Float) -> Unit,
    onStartRest: () -> Unit,
    onToggleComplete: () -> Unit,
    onEditExercise: ((sets: Int, reps: String, restTime: Int, weight: Float) -> Unit)? = null,
    onDeleteExercise: (() -> Unit)? = null,
    isRestCompleteForThis: Boolean = false,
    isRestRunningForThis: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showEditSheet   by remember { mutableStateOf(false) }
    var showWeightSheet by remember { mutableStateOf(false) }
    var showMenu        by remember { mutableStateOf(false) }

    val cardState = remember(isCompleted, isRestCompleteForThis, isRestRunningForThis, completedSets) {
        when {
            isCompleted          -> CardTone.COMPLETED
            isRestCompleteForThis -> CardTone.REST_DONE
            isRestRunningForThis || completedSets > 0 -> CardTone.ACTIVE
            else                 -> CardTone.IDLE
        }
    }

    // Per-exercise accent color (stable across recompositions)
    val baseAccent = remember(exercise.id) {
        accentPalette[Math.abs(exercise.id.hashCode()) % accentPalette.size]
    }
    val accentBarColor by animateColorAsState(
        targetValue = if (isCompleted) AccentGreen else baseAccent,
        animationSpec = tween(450),
        label = "accentBar"
    )

    val containerColor by animateColorAsState(
        targetValue = when (cardState) {
            CardTone.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
            CardTone.REST_DONE -> MaterialTheme.colorScheme.tertiaryContainer
            CardTone.ACTIVE    -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            CardTone.IDLE      -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(450),
        label = "cardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when (cardState) {
            CardTone.COMPLETED -> AccentGreen.copy(alpha = 0.38f)
            CardTone.REST_DONE -> AccentGreen.copy(alpha = 0.38f)
            CardTone.ACTIVE    -> AccentBlue.copy(alpha = 0.30f)
            CardTone.IDLE      -> MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
        },
        animationSpec = tween(450),
        label = "cardBorder"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = if (cardState == CardTone.IDLE) 1.dp else 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // ── Top accent strip ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentBarColor)
            )

            // ── Body ──────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Header: name + overflow menu / done badge ─────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = exercise.nameResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isCompleted) AccentGreenDark
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        if (isCompleted) {
                            CompletionBadge(onClick = { showMenu = true })
                        } else {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isCompleted) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mark_complete) + " ↺") },
                                    onClick = { showMenu = false; onToggleComplete() },
                                    leadingIcon = { Icon(Icons.Default.Check, null) }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mark_complete)) },
                                    onClick = { showMenu = false; onToggleComplete() },
                                    leadingIcon = { Icon(Icons.Default.Check, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = { showMenu = false; showEditSheet = true },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
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

                // ── Stats chips: sets×reps · rest · weight ────────────────────
                // Weight is always surfaced here — tapping it opens the weight sheet.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(
                        icon = Icons.Default.Repeat,
                        label = "${exercise.sets} × ${exercise.reps}",
                        tint = AccentBlue
                    )
                    if (exercise.restTime > 0) {
                        StatChip(
                            icon = Icons.Default.Timer,
                            label = "${exercise.restTime}s",
                            tint = AccentAmber
                        )
                    }
                    if (exercise.hasWeight) {
                        StatChip(
                            icon = Icons.Default.FitnessCenter,
                            label = if (weight > 0f)
                                "${formatWeight(weight)} ${stringResource(R.string.kg)}"
                            else
                                stringResource(R.string.set_weight),
                            tint = if (weight > 0f) AccentViolet else AccentAmber,
                            onClick = { showWeightSheet = true }
                        )
                    }
                }

                // ── Set dots + compact action button (single row) ─────────────
                if (!isCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SetDots(
                            completedSets = completedSets,
                            currentSet = completedSets + 1,
                            totalSets = exercise.sets,
                            isRestRunning = isRestRunningForThis
                        )
                        CompactActionButton(
                            completedSets = completedSets,
                            totalSets = exercise.sets,
                            isRestCompleteForThis = isRestCompleteForThis,
                            isRestRunningForThis = isRestRunningForThis,
                            onClick = onStartRest
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        ExerciseEditDialog(
            exercise = exercise,
            currentWeight = weight,
            onDismiss = { showEditSheet = false },
            onSave = { sets, reps, restTime, newWeight ->
                onEditExercise?.invoke(sets, reps, restTime, newWeight) ?: onWeightChange(newWeight)
            }
        )
    }
    if (showWeightSheet) {
        WeightInputSheet(
            initialWeight = weight,
            onDismiss = { showWeightSheet = false },
            onSave = {
                onWeightChange(it)
                showWeightSheet = false
            }
        )
    }
}

private enum class CardTone { IDLE, ACTIVE, REST_DONE, COMPLETED }

// ─── Completion badge ─────────────────────────────────────────────────────────

@Composable
private fun CompletionBadge(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = AccentGreen,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Stat chip ────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val bg = tint.copy(alpha = 0.12f)
    val inner = Modifier
        .clip(shape)
        .background(bg)
        .padding(horizontal = 8.dp, vertical = 4.dp)
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Text(text = label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = Color.Transparent, shape = shape) {
            Box(modifier = inner) { content() }
        }
    } else {
        Box(modifier = inner) { content() }
    }
}

// ─── Set dots ─────────────────────────────────────────────────────────────────

@Composable
private fun SetDots(
    completedSets: Int,
    currentSet: Int,
    totalSets: Int,
    isRestRunning: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSets) { index ->
            val num = index + 1
            val isDone   = num <= completedSets
            val isActive = num == currentSet && !isDone
            val color by animateColorAsState(
                targetValue = when {
                    isDone   -> AccentGreen
                    isActive -> if (isRestRunning) AccentAmber else AccentBlue
                    else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                },
                animationSpec = tween(300),
                label = "dot_$index"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.35f else 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
                label = "dotScale_$index"
            )
            Box(
                modifier = Modifier
                    .size((9 * scale).dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ─── Compact action button ────────────────────────────────────────────────────

@Composable
private fun CompactActionButton(
    completedSets: Int,
    totalSets: Int,
    isRestCompleteForThis: Boolean,
    isRestRunningForThis: Boolean,
    onClick: () -> Unit
) {
    val isEnabled = !isRestRunningForThis && completedSets < totalSets

    val label: String
    val icon: ImageVector
    val color: Color
    when {
        isRestCompleteForThis -> {
            label = stringResource(R.string.start_next_set)
            icon  = Icons.Default.SkipNext
            color = AccentGreen
        }
        isRestRunningForThis -> {
            label = stringResource(R.string.resume_rest)
            icon  = Icons.Default.PlayArrow
            color = AccentAmber
        }
        else -> {
            label = stringResource(R.string.start_rest)
            icon  = Icons.Default.PlayArrow
            color = AccentBlue
        }
    }

    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// ─── Weight input sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightInputSheet(
    initialWeight: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var value by remember {
        mutableStateOf(if (initialWeight > 0f) formatWeight(initialWeight) else "")
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FitnessCenter, null, tint = AccentViolet, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = stringResource(R.string.weight),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text("0.0") },
                suffix = { Text(stringResource(R.string.kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10", "20", "40", "60", "80").forEach { preset ->
                    AssistChip(onClick = { value = preset }, label = { Text(preset) })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = { onSave(value.toFloatOrNull() ?: 0f) },
                    enabled = value.toFloatOrNull()?.let { it > 0f } == true,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun formatWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else "%.1f".format(weight)

// ─── Edit sheet ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditDialog(
    exercise: Exercise,
    currentWeight: Float,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: String, restTime: Int, weight: Float) -> Unit
) {
    var sets     by remember { mutableStateOf(exercise.sets.toString()) }
    var reps     by remember { mutableStateOf(exercise.reps) }
    var restTime by remember { mutableStateOf(exercise.restTime.toString()) }
    var weight   by remember {
        mutableStateOf(if (currentWeight > 0) formatWeight(currentWeight) else "")
    }

    val isValid = sets.toIntOrNull()?.let { it > 0 } == true &&
            reps.isNotBlank() &&
            restTime.toIntOrNull()?.let { it >= 0 } == true &&
            (!exercise.hasWeight || weight.isEmpty() || weight.toFloatOrNull() != null)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.edit) + " — " + stringResource(exercise.nameResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text(stringResource(R.string.sets)) },
                leadingIcon = { Icon(Icons.Default.FitnessCenter, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = sets.toIntOrNull()?.let { it <= 0 } != false
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text(stringResource(R.string.reps)) },
                leadingIcon = { Icon(Icons.Default.Repeat, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = reps.isBlank()
            )
            OutlinedTextField(
                value = restTime,
                onValueChange = { restTime = it },
                label = { Text(stringResource(R.string.rest_time_seconds)) },
                leadingIcon = { Icon(Icons.Default.Timer, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = restTime.toIntOrNull()?.let { it < 0 } != false
            )
            if (exercise.hasWeight) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(stringResource(R.string.weight)) },
                    suffix = { Text(stringResource(R.string.kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isError = weight.isNotEmpty() && weight.toFloatOrNull() == null
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        if (isValid) {
                            onSave(sets.toInt(), reps, restTime.toInt(), weight.toFloatOrNull() ?: 0f)
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            }
        }
    }
}
