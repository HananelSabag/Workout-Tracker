package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

// ─── Colour tokens ────────────────────────────────────────────────────────────
private val Blue500   = Color(0xFF3B82F6)
private val Green500  = Color(0xFF10B981)
private val Green700  = Color(0xFF059669)
private val Amber400  = Color(0xFFFBBF24)
private val Amber600  = Color(0xFFD97706)
private val Amber900  = Color(0xFF92400E)
private val Violet500 = Color(0xFF8B5CF6)
private val Violet700 = Color(0xFF6D28D9)
private val GreenBg   = Color(0xFFF0FDF4)
private val AmberBg   = Color(0xFFFFFBEB)

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
    modifier: Modifier = Modifier
) {
    var showEditDialog  by remember { mutableStateOf(false) }
    var showMenu        by remember { mutableStateOf(false) }
    var showWeightInput by remember { mutableStateOf(false) }

    // ── Animated card colours ─────────────────────────────────────────────────
    val stripColor by animateColorAsState(
        targetValue = when {
            isCompleted    -> Green500
            completedSets > 0 -> Blue500
            else           -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        },
        animationSpec = tween(400),
        label = "strip"
    )
    val cardBg by animateColorAsState(
        targetValue = if (isCompleted) GreenBg else MaterialTheme.colorScheme.surface,
        animationSpec = tween(500),
        label = "cardBg"
    )
    val setProgress by animateFloatAsState(
        targetValue = if (exercise.sets > 0) completedSets.toFloat() / exercise.sets else 0f,
        animationSpec = tween(600),
        label = "setProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 0.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // ── Left accent strip ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        color = stripColor,
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {

                // ── Header row: name + chips / menu + circle ──────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: name + stat chips
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = stringResource(id = exercise.nameResId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCompleted) Green700
                                    else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExerciseStatChip(
                                icon  = Icons.Default.FitnessCenter,
                                text  = "${exercise.sets} × ${exercise.reps}",
                                color = Blue500
                            )
                            if (exercise.restTime > 0) {
                                ExerciseStatChip(
                                    icon  = Icons.Default.Timer,
                                    text  = "${exercise.restTime}s",
                                    color = Amber600
                                )
                            }
                        }
                    }

                    // Right: three-dot menu + completion circle
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
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
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    onClick = { showMenu = false; showEditDialog = true },
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
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        CompleteCircleButton(isCompleted = isCompleted, onClick = onToggleComplete)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Progress bar + fraction ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { setProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isCompleted) Green500 else Blue500,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                    Text(
                        text = "$completedSets/${exercise.sets}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Green500
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Numbered set progress dots ────────────────────────────────
                SetProgressIndicator(
                    completedSets = completedSets,
                    currentSet    = completedSets + 1,
                    totalSets     = exercise.sets
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Weight section + action button ────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (exercise.hasWeight) {
                        WeightRow(
                            weight      = weight,
                            showInput   = showWeightInput,
                            onToggle    = { showWeightInput = it },
                            onWeightChange = onWeightChange
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    ExerciseActionButton(
                        isCompleted   = isCompleted,
                        completedSets = completedSets,
                        totalSets     = exercise.sets,
                        onClick       = onStartRest,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        ExerciseEditDialog(
            exercise      = exercise,
            currentWeight = weight,
            onDismiss     = { showEditDialog = false },
            onSave        = { sets, reps, restTime, newWeight ->
                onEditExercise?.invoke(sets, reps, restTime, newWeight) ?: onWeightChange(newWeight)
            }
        )
    }
}

// ── Weight row (compact prompt / chip / inline input) ────────────────────────

@Composable
private fun WeightRow(
    weight: Float,
    showInput: Boolean,
    onToggle: (Boolean) -> Unit,
    onWeightChange: (Float) -> Unit
) {
    AnimatedContent(
        targetState = when {
            showInput   -> WeightUiState.INPUT
            weight > 0f -> WeightUiState.CHIP
            else        -> WeightUiState.PROMPT
        },
        transitionSpec = {
            (fadeIn(tween(200)) + slideInVertically { -it / 4 })
                .togetherWith(fadeOut(tween(150)) + slideOutVertically { it / 4 })
        },
        label = "weightState"
    ) { state ->
        when (state) {
            WeightUiState.INPUT -> {
                InlineWeightInput(
                    initialWeight = if (weight > 0f) weight.toString() else "",
                    onSave = { newWeight ->
                        if (newWeight > 0) {
                            onWeightChange(newWeight)
                            onToggle(false)
                        }
                    },
                    onCancel = if (weight > 0f) ({ onToggle(false) }) else null
                )
            }
            WeightUiState.CHIP -> {
                // Tappable weight chip (purple)
                Surface(
                    onClick = { onToggle(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = Violet500.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Violet500,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$weight ${stringResource(R.string.kg)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Violet700,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Violet500.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            WeightUiState.PROMPT -> {
                // "Add weight" prompt chip (amber, soft)
                Surface(
                    onClick = { onToggle(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = AmberBg,
                    border = BorderStroke(1.dp, Amber400.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.set_weight),
                            style = MaterialTheme.typography.labelMedium,
                            color = Amber900,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private enum class WeightUiState { PROMPT, CHIP, INPUT }

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun ExerciseStatChip(icon: ImageVector, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CompleteCircleButton(isCompleted: Boolean, onClick: () -> Unit) {
    val circleScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "circleScale"
    )
    val circleBg by animateColorAsState(
        targetValue = if (isCompleted) Green500
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "circleBg"
    )
    val circleBorder by animateColorAsState(
        targetValue = if (isCompleted) Green500
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "circleBorder"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(circleScale)
            .clip(CircleShape)
            .background(circleBg)
            .border(2.dp, circleBorder, CircleShape)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isCompleted,
            enter = scaleIn() + fadeIn(),
            exit  = scaleOut() + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ExerciseActionButton(
    isCompleted: Boolean,
    completedSets: Int,
    totalSets: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isCompleted) Green500 else Blue500,
        animationSpec = tween(300),
        label = "btnBg"
    )

    Button(
        onClick = onClick,
        enabled = !isCompleted && completedSets < totalSets,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            disabledContainerColor = Green500.copy(alpha = 0.55f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when {
                isCompleted       -> stringResource(R.string.finished)
                completedSets > 0 -> stringResource(R.string.resume_rest)
                else              -> stringResource(R.string.start_rest)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun InlineWeightInput(
    initialWeight: String,
    onSave: (Float) -> Unit,
    onCancel: (() -> Unit)?
) {
    var value by remember { mutableStateOf(initialWeight) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = { Text("0.0", fontSize = 14.sp) },
            suffix = { Text(stringResource(R.string.kg), fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet500,
                unfocusedBorderColor = Violet500.copy(alpha = 0.4f)
            )
        )
        Button(
            onClick = { onSave(value.toFloatOrNull() ?: 0f) },
            enabled = value.toFloatOrNull()?.let { it > 0 } == true,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet500)
        ) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
        }
        if (onCancel != null) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────

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
    var weight   by remember { mutableStateOf(if (currentWeight > 0) currentWeight.toString() else "") }

    val isValid = sets.toIntOrNull() != null && sets.toIntOrNull()!! > 0 &&
            reps.isNotBlank() &&
            restTime.toIntOrNull() != null && restTime.toIntOrNull()!! >= 0 &&
            (!exercise.hasWeight || weight.isEmpty() || weight.toFloatOrNull() != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit) + " — " + stringResource(exercise.nameResId),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text(stringResource(R.string.sets)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = sets.toIntOrNull() == null || sets.toIntOrNull()!! <= 0
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text(stringResource(R.string.reps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = reps.isBlank()
                )
                OutlinedTextField(
                    value = restTime,
                    onValueChange = { restTime = it },
                    label = { Text(stringResource(R.string.rest_time_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = restTime.toIntOrNull() == null || restTime.toIntOrNull()!! < 0
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
                        isError = weight.isNotEmpty() && weight.toFloatOrNull() == null
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onSave(sets.toInt(), reps, restTime.toInt(), weight.toFloatOrNull() ?: 0f)
                        onDismiss()
                    }
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
