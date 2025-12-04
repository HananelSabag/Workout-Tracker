package com.hananel.workouttracker2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R

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
            sets.toIntOrNull() != null && sets.toIntOrNull()!! > 0 &&
            reps.isNotBlank() &&
            restTime.toIntOrNull() != null && restTime.toIntOrNull()!! >= 0 &&
            (!hasWeight || weight.isEmpty() || weight.toFloatOrNull() != null)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_custom_exercise),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exercise name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )
                
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
                    placeholder = { Text("10-12 או 15") },
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
                    isError = restTime.toIntOrNull() == null
                )
                
                // Has weight checkbox
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.has_weight_question),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
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
                }
                
                // Weight (if applicable)
                if (hasWeight) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text(stringResource(R.string.weight)) },
                        suffix = { Text(stringResource(R.string.kg)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("0.0") },
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
                enabled = isValid
            ) {
                Text(
                    stringResource(R.string.add_exercise),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

