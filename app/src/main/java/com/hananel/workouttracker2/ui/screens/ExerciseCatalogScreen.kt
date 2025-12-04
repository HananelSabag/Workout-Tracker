package com.hananel.workouttracker2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.ExerciseBank
import com.hananel.workouttracker2.data.models.ExerciseBankItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    dayId: String,
    onNavigateBack: () -> Unit,
    onExerciseSelected: (ExerciseBankItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
    
    val muscleGroups = remember { ExerciseBank.getMuscleGroupsForFiltering() }
    
    val filteredExercises = remember(selectedMuscleGroup) {
        if (selectedMuscleGroup == null) {
            ExerciseBank.getAllExercises()
        } else {
            ExerciseBank.getExercisesByMuscleGroup(selectedMuscleGroup!!)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exercise_catalog)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Muscle Group Filter Tabs - Larger, more touch-friendly
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // "All" filter chip
                item {
                    FilterChip(
                        selected = selectedMuscleGroup == null,
                        onClick = { selectedMuscleGroup = null },
                        label = { 
                            Text(
                                stringResource(R.string.all_exercises),
                                fontSize = 15.sp,
                                fontWeight = if (selectedMuscleGroup == null) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (selectedMuscleGroup == null) {
                            {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.height(48.dp)
                    )
                }
                
                // Individual muscle group chips
                items(muscleGroups) { muscle ->
                    FilterChip(
                        selected = selectedMuscleGroup == muscle,
                        onClick = { selectedMuscleGroup = muscle },
                        label = { 
                            Text(
                                muscle,
                                fontSize = 15.sp,
                                fontWeight = if (selectedMuscleGroup == muscle) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
            
            HorizontalDivider()
            
            // Exercise List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredExercises) { exercise ->
                    ExerciseCatalogCard(
                        exercise = exercise,
                        onAddClick = { onExerciseSelected(exercise) }
                    )
                }
                
                // Empty state
                if (filteredExercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No exercises found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCatalogCard(
    exercise: ExerciseBankItem,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Exercise Name
                Text(
                    text = context.getString(exercise.nameResId),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Equipment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = exercise.equipment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Default Sets × Reps
                Text(
                    text = "${exercise.defaultSets} × ${exercise.defaultReps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Muscle Groups (tags)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    exercise.muscleGroups.forEach { muscle ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = muscle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Add Button
            FilledTonalButton(
                onClick = onAddClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_to_workout),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_to_workout),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

