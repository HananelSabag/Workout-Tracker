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
import androidx.compose.ui.graphics.Brush
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

// ─── Card-local color tokens ─────────────────────────────────────────────────
private val AccentBlue      = Color(0xFF3B82F6)
private val AccentBlueDark  = Color(0xFF1D4ED8)
private val AccentGreen     = Color(0xFF10B981)
private val AccentGreenDark = Color(0xFF059669)
private val AccentAmber     = Color(0xFFF59E0B)
private val AccentAmberSoft = Color(0xFFFEF3C7)
private val AccentViolet    = Color(0xFF8B5CF6)
private val BrandOrange     = Color(0xFFF97316)
private val BrandOrangeDark = Color(0xFFEA580C)

/**
 * Sport-premium exercise card. Redesigned layout:
 *
 *  ╭───────────────────────────────────────────────╮
 *  │▌  Exercise name                          ⋮   │  header (left accent strip, state-driven)
 *  │   equipment                                   │
 *  │   [5×10-12]  [⏱ 60s]  [🏋 22.5kg]             │  stat chips
 *  │                                               │
 *  │   ● ● ● ○ ○                        3 / 5     │  set dots (larger) + counter
 *  │                                               │
 *  │   [+ משקל]          [▶ התחל מנוחה]            │  action row
 *  ╰───────────────────────────────────────────────╯
 *
 * Visual states (left strip + container tint):
 *  - IDLE      → gray strip,   plain surface
 *  - ACTIVE    → orange strip, warm orange tint
 *  - REST_DONE → blue strip,   blue tint  (rest finished — "go")
 *  - COMPLETED → green strip,  green tint
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

    val state = remember(isCompleted, isRestCompleteForThis, isRestRunningForThis, completedSets) {
        when {
            isCompleted            -> CardTone.COMPLETED
            isRestCompleteForThis  -> CardTone.REST_DONE
            isRestRunningForThis || completedSets > 0 -> CardTone.ACTIVE
            else                   -> CardTone.IDLE
        }
    }

    // Container background tint
    val containerColor by animateColorAsState(
        targetValue = when (state) {
            CardTone.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
            CardTone.REST_DONE -> MaterialTheme.colorScheme.tertiaryContainer
            CardTone.ACTIVE    -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            CardTone.IDLE      -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(450),
        label = "cardBg"
    )

    // Left accent strip color
    val accentColor by animateColorAsState(
        targetValue = when (state) {
            CardTone.IDLE      -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            CardTone.ACTIVE    -> MaterialTheme.colorScheme.primary
            CardTone.REST_DONE -> MaterialTheme.colorScheme.tertiary
            CardTone.COMPLETED -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = tween(450),
        label = "accentStrip"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        shadowElevation = if (state == CardTone.IDLE) 2.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    ) {
        // IntrinsicSize.Min lets the left strip fill the Column's dynamic height.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // ── Left accent strip ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // ── Card body ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
                    .padding(start = 14.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Header: name + equipment + menu ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = exercise.nameResId),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = if (isCompleted) AccentGreenDark
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (exercise.equipment.isNotBlank()) {
                            Text(
                                text = exercise.equipment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Box {
                        if (isCompleted) {
                            CompletionBadge(onClick = { showMenu = true })
                        } else {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                }

                // ── Stat chips ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(
                        icon  = Icons.Default.Repeat,
                        label = "${exercise.sets} × ${exercise.reps}",
                        tint  = AccentBlue
                    )
                    if (exercise.restTime > 0) {
                        StatChip(
                            icon  = Icons.Default.Timer,
                            label = "${exercise.restTime}s",
                            tint  = AccentAmber
                        )
                    }
                    if (exercise.hasWeight && weight > 0f) {
                        StatChip(
                            icon    = Icons.Default.FitnessCenter,
                            label   = "${formatWeight(weight)} ${stringResource(R.string.kg)}",
                            tint    = AccentViolet,
                            onClick = { showWeightSheet = true }
                        )
                    }
                }

                // ── Set dots + numeric counter ─────────────────────────────
                if (!isCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SetDots(
                            completedSets  = completedSets,
                            currentSet     = completedSets + 1,
                            totalSets      = exercise.sets,
                            isRestRunning  = isRestRunningForThis
                        )
                        Text(
                            text       = "$completedSets / ${exercise.sets}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 15.sp,
                            color      = when (state) {
                                CardTone.ACTIVE    -> MaterialTheme.colorScheme.primary
                                CardTone.REST_DONE -> MaterialTheme.colorScheme.tertiary
                                else               -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // ── Action row ─────────────────────────────────────────────
                if (!isCompleted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (exercise.hasWeight && weight <= 0f) {
                            FilledTonalButton(
                                onClick = { showWeightSheet = true },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AccentAmberSoft,
                                    contentColor   = Color(0xFF92400E)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.set_weight),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp
                                )
                            }
                        }
                        PrimaryActionButton(
                            completedSets         = completedSets,
                            totalSets             = exercise.sets,
                            isRestCompleteForThis  = isRestCompleteForThis,
                            isRestRunningForThis   = isRestRunningForThis,
                            onClick               = onStartRest,
                            modifier              = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        ExerciseEditDialog(
            exercise      = exercise,
            currentWeight = weight,
            onDismiss     = { showEditSheet = false },
            onSave        = { sets, reps, restTime, newWeight ->
                onEditExercise?.invoke(sets, reps, restTime, newWeight) ?: onWeightChange(newWeight)
            }
        )
    }
    if (showWeightSheet) {
        WeightInputSheet(
            initialWeight = weight,
            onDismiss     = { showWeightSheet = false },
            onSave        = {
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
        onClick        = onClick,
        modifier       = Modifier.size(40.dp),
        shape          = CircleShape,
        color          = AccentGreen,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector    = Icons.Default.Check,
                contentDescription = null,
                tint           = Color.White,
                modifier       = Modifier.size(22.dp)
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
    val shape     = RoundedCornerShape(10.dp)
    val container = tint.copy(alpha = 0.15f)
    val baseModifier = Modifier
        .clip(shape)
        .background(container)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector    = icon,
                contentDescription = null,
                tint           = tint,
                modifier       = Modifier.size(14.dp)
            )
            Text(
                text       = label,
                color      = tint,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = Color.Transparent) {
            Box(modifier = baseModifier) { content() }
        }
    } else {
        Box(modifier = baseModifier) { content() }
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
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(totalSets) { index ->
            val setNumber = index + 1
            val isDone    = setNumber <= completedSets
            val isActive  = setNumber == currentSet && !isDone
            val color by animateColorAsState(
                targetValue = when {
                    isDone   -> AccentGreen
                    isActive -> if (isRestRunning) AccentAmber else BrandOrange
                    else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                },
                animationSpec = tween(350),
                label = "dotColor_$index"
            )
            val sizeFactor by animateFloatAsState(
                targetValue   = if (isActive) 1.35f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 380f),
                label = "dotScale_$index"
            )
            Box(
                modifier = Modifier
                    .size((12 * sizeFactor).dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ─── Primary action button ────────────────────────────────────────────────────

@Composable
private fun PrimaryActionButton(
    completedSets: Int,
    totalSets: Int,
    isRestCompleteForThis: Boolean,
    isRestRunningForThis: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, icon, gradStart, gradEnd) = when {
        isRestCompleteForThis -> Quad(
            stringResource(R.string.start_next_set),
            Icons.Default.SkipNext,
            AccentGreen, AccentGreenDark
        )
        isRestRunningForThis || completedSets > 0 -> Quad(
            stringResource(R.string.resume_rest),
            Icons.Default.PlayArrow,
            AccentBlue, AccentBlueDark
        )
        else -> Quad(
            stringResource(R.string.start_rest),
            Icons.Default.PlayArrow,
            BrandOrange, BrandOrangeDark
        )
    }

    val shape = RoundedCornerShape(14.dp)
    Surface(
        onClick        = onClick,
        modifier       = modifier.height(52.dp),
        shape          = shape,
        color          = Color.Transparent,
        shadowElevation = 3.dp,
        enabled        = !isRestRunningForThis && completedSets < totalSets
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(gradStart, gradEnd))),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector    = icon,
                    contentDescription = null,
                    tint           = Color.White,
                    modifier       = Modifier.size(18.dp)
                )
                Text(
                    text       = label,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
        }
    }
}

private data class Quad(
    val label: String,
    val icon: ImageVector,
    val gradStart: Color,
    val gradEnd: Color
)

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
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector    = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint           = AccentViolet,
                        modifier       = Modifier.size(20.dp)
                    )
                }
                Text(
                    text       = stringResource(R.string.weight),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value           = value,
                onValueChange   = { value = it },
                placeholder     = { Text("0.0") },
                suffix          = { Text(stringResource(R.string.kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine      = true,
                shape           = RoundedCornerShape(14.dp),
                modifier        = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10", "20", "40", "60", "80").forEach { preset ->
                    AssistChip(onClick = { value = preset }, label = { Text(preset) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick  = { onSave(value.toFloatOrNull() ?: 0f) },
                    enabled  = value.toFloatOrNull()?.let { it > 0f } == true,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun formatWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString()
    else "%.1f".format(weight)

// ─── Exercise edit sheet ──────────────────────────────────────────────────────

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
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text       = stringResource(R.string.edit) + " — " + stringResource(exercise.nameResId),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value           = sets,
                onValueChange   = { sets = it },
                label           = { Text(stringResource(R.string.sets)) },
                leadingIcon     = { Icon(Icons.Default.FitnessCenter, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                isError         = sets.toIntOrNull()?.let { it <= 0 } != false
            )
            OutlinedTextField(
                value         = reps,
                onValueChange = { reps = it },
                label         = { Text(stringResource(R.string.reps)) },
                leadingIcon   = { Icon(Icons.Default.Repeat, null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                isError       = reps.isBlank()
            )
            OutlinedTextField(
                value           = restTime,
                onValueChange   = { restTime = it },
                label           = { Text(stringResource(R.string.rest_time_seconds)) },
                leadingIcon     = { Icon(Icons.Default.Timer, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                isError         = restTime.toIntOrNull()?.let { it < 0 } != false
            )
            if (exercise.hasWeight) {
                OutlinedTextField(
                    value           = weight,
                    onValueChange   = { weight = it },
                    label           = { Text(stringResource(R.string.weight)) },
                    suffix          = { Text(stringResource(R.string.kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(14.dp),
                    isError         = weight.isNotEmpty() && weight.toFloatOrNull() == null
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick  = {
                        if (isValid) {
                            onSave(sets.toInt(), reps, restTime.toInt(), weight.toFloatOrNull() ?: 0f)
                            onDismiss()
                        }
                    },
                    enabled  = isValid,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            }
        }
    }
}
