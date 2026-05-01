package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R

/**
 * Modern bottom-sheet form for adding a custom exercise.
 *
 * Sections (instead of one undifferentiated wall of fields):
 *   • "Exercise" — name
 *   • "Set Structure" — sets, reps, rest with quick-pick chips
 *   • "Weight" — optional toggle + value
 *
 * Quick-pick chips below each numeric field cover the 80% case so users rarely have to type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, sets: Int, reps: String, restTime: Int, hasWeight: Boolean, weight: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("4") }
    var reps by remember { mutableStateOf("10-12") }
    var restTime by remember { mutableStateOf("60") }
    var hasWeight by remember { mutableStateOf(true) }
    var weight by remember { mutableStateOf("") }

    val isValid = name.isNotBlank() &&
            sets.toIntOrNull()?.let { it > 0 } == true &&
            reps.isNotBlank() &&
            restTime.toIntOrNull()?.let { it >= 0 } == true &&
            (!hasWeight || weight.isEmpty() || weight.toFloatOrNull() != null)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.add_custom_exercise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Section: Exercise ────────────────────────────────────────────
            FormSectionHeader(stringResource(R.string.exercise_name))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(R.string.exercise_name)) },
                leadingIcon = {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = name.isBlank() && name.isNotEmpty()
            )

            // ── Section: Set Structure ───────────────────────────────────────
            FormSectionHeader(stringResource(R.string.sets) + " · " + stringResource(R.string.reps) + " · " + stringResource(R.string.rest_time_seconds))

            FieldWithPresets(
                value = sets,
                onValueChange = { sets = it },
                label = stringResource(R.string.sets),
                icon = Icons.Default.FitnessCenter,
                keyboardType = KeyboardType.Number,
                presets = listOf("3", "4", "5"),
                isError = sets.toIntOrNull()?.let { it <= 0 } != false
            )

            FieldWithPresets(
                value = reps,
                onValueChange = { reps = it },
                label = stringResource(R.string.reps),
                icon = Icons.Default.Repeat,
                keyboardType = KeyboardType.Text,
                presets = listOf("8-10", "10-12", "12-15", "15-20"),
                isError = reps.isBlank()
            )

            FieldWithPresets(
                value = restTime,
                onValueChange = { restTime = it },
                label = stringResource(R.string.rest_time_seconds),
                icon = Icons.Default.Timer,
                keyboardType = KeyboardType.Number,
                presets = listOf("30", "45", "60", "90", "120"),
                isError = restTime.toIntOrNull()?.let { it < 0 } != false
            )

            // ── Section: Weight ──────────────────────────────────────────────
            FormSectionHeader(stringResource(R.string.weight))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.has_weight_question),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.has_weight_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hasWeight,
                            onCheckedChange = { hasWeight = it }
                        )
                    }
                    if (hasWeight) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            placeholder = { Text("0.0") },
                            label = { Text(stringResource(R.string.weight)) },
                            suffix = { Text(stringResource(R.string.kg)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = weight.isNotEmpty() && weight.toFloatOrNull() == null
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Actions ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        if (isValid) {
                            onAdd(
                                name,
                                sets.toInt(),
                                reps,
                                restTime.toInt(),
                                hasWeight,
                                if (hasWeight) weight.toFloatOrNull() ?: 0f else 0f
                            )
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.add_exercise),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldWithPresets(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType,
    presets: List<String>,
    isError: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            isError = isError
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp)
        ) {
            items(presets) { preset ->
                val selected = value == preset
                FilterChip(
                    selected = selected,
                    onClick = { onValueChange(preset) },
                    label = {
                        Text(
                            text = preset,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
