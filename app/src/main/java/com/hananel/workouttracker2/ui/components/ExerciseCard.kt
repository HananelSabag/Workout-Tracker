package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.Exercise

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
    var showEditDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Animated border color
    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> Color(0xFF10B981) // Green
            completedSets > 0 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        label = "borderColor"
    )
    
    val containerColor = if (isCompleted) {
        Color(0xFFF0FDF4) // Light green background
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 3.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title + Menu + Complete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Title with completion badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = exercise.nameResId),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    // Completion badge below title
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCompleted,
                        enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.completed_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                
                // Right side: Menu + Complete Circle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Three-dot menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            if (onDeleteExercise != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteExercise()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    // Complete Circle Button with animation
                    val scale by animateFloatAsState(
                        targetValue = if (isCompleted) 1.15f else 1f,
                        label = "scale"
                    )
                    
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isCompleted) Color(0xFF10B981) 
                        else MaterialTheme.colorScheme.surfaceVariant,
                        label = "backgroundColor"
                    )
                    
                    val borderColor by animateColorAsState(
                        targetValue = if (isCompleted) Color(0xFF10B981)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        label = "borderColor"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale)
                            .clickable(
                                onClick = { onToggleComplete() },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .border(
                                width = if (isCompleted) 3.dp else 2.dp,
                                color = borderColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isCompleted,
                            enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info chips row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sets × Reps
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${exercise.sets} × ${exercise.reps}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Rest time
                if (exercise.restTime > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${exercise.restTime}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // Weight section
            if (exercise.hasWeight) {
                Spacer(modifier = Modifier.height(12.dp))
                
                if (weight > 0f) {
                    // Weight display - beautiful box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                            .border(
                                width = 2.dp,
                                color = Color(0xFF8B5CF6).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.weight) + ": ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6D28D9),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$weight ${stringResource(R.string.kg)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF6D28D9),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Weight not set - inline input
                    WeightInputSection(
                        onWeightSaved = { newWeight ->
                            if (newWeight > 0) {
                                onWeightChange(newWeight)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress + Start Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$completedSets/${exercise.sets} ${stringResource(R.string.sets_completed)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SetProgressIndicator(
                        completedSets = completedSets,
                        currentSet = completedSets + 1,
                        totalSets = exercise.sets
                    )
                }
                
                // Start Rest / Resume Rest / Finished Button
                Button(
                    onClick = onStartRest,
                    enabled = !isCompleted && completedSets < exercise.sets,
                    modifier = Modifier
                        .height(52.dp)
                        .padding(start = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Color(0xFF10B981) else Color(0xFF3B82F6),
                        disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isCompleted) Color.White else LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isCompleted -> stringResource(R.string.finished)
                            completedSets > 0 -> stringResource(R.string.resume_rest)
                            else -> stringResource(R.string.start_rest)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Full edit dialog
    if (showEditDialog) {
        ExerciseEditDialog(
            exercise = exercise,
            currentWeight = weight,
            onDismiss = { showEditDialog = false },
            onSave = { sets, reps, restTime, newWeight ->
                onEditExercise?.invoke(sets, reps, restTime, newWeight) ?: onWeightChange(newWeight)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditDialog(
    exercise: Exercise,
    currentWeight: Float,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: String, restTime: Int, weight: Float) -> Unit
) {
    var sets by remember { mutableStateOf(exercise.sets.toString()) }
    var reps by remember { mutableStateOf(exercise.reps) }
    var restTime by remember { mutableStateOf(exercise.restTime.toString()) }
    var weight by remember { mutableStateOf(if (currentWeight > 0) currentWeight.toString() else "") }
    
    val isValid = sets.toIntOrNull() != null && sets.toIntOrNull()!! > 0 &&
                  reps.isNotBlank() &&
                  restTime.toIntOrNull() != null && restTime.toIntOrNull()!! >= 0 &&
                  (!exercise.hasWeight || weight.isEmpty() || weight.toFloatOrNull() != null)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit) + " " + stringResource(exercise.nameResId),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sets
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text(stringResource(R.string.sets)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = sets.toIntOrNull() == null || sets.toIntOrNull()!! <= 0
                )
                
                // Reps
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text(stringResource(R.string.reps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = reps.isBlank()
                )
                
                // Rest time
                OutlinedTextField(
                    value = restTime,
                    onValueChange = { restTime = it },
                    label = { Text(stringResource(R.string.rest_time_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = restTime.toIntOrNull() == null || restTime.toIntOrNull()!! < 0
                )
                
                // Weight
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
                        onSave(
                            sets.toInt(),
                            reps,
                            restTime.toInt(),
                            weight.toFloatOrNull() ?: 0f
                        )
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

@Composable
fun WeightInputSection(
    onWeightSaved: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var tempWeight by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF2F2))
            .border(
                width = 2.dp,
                color = Color(0xFFF87171).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.weight_not_set),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tempWeight,
                    onValueChange = { tempWeight = it },
                    placeholder = { Text("0.0", fontSize = 14.sp) },
                    suffix = { Text(stringResource(R.string.kg), fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFFF87171)
                    )
                )
                Button(
                    onClick = {
                        val weight = tempWeight.toFloatOrNull()
                        if (weight != null && weight > 0) {
                            onWeightSaved(weight)
                        }
                    },
                    modifier = Modifier.height(50.dp),
                    enabled = tempWeight.toFloatOrNull()?.let { it > 0 } == true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
