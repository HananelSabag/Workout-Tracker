package com.hananel.workouttracker2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.ExerciseBank
import com.hananel.workouttracker2.data.models.ExerciseBankItem
import com.hananel.workouttracker2.data.repository.WorkoutRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    dayId: String,
    onNavigateBack: () -> Unit,
    onExerciseSelected: (ExerciseBankItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }

    var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val muscleGroups = remember { ExerciseBank.getMuscleGroupsForFiltering() }

    // Observe the current day's exercises so we can mark already-added ones inline rather than
    // silently allowing duplicates.
    var existingNameResIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(dayId) {
        repository.getWorkoutStructureFlow().collect { structure ->
            existingNameResIds = structure.firstOrNull { it.id == dayId }
                ?.exercises?.map { it.nameResId }?.toSet().orEmpty()
        }
    }

    val filteredExercises = remember(selectedMuscleGroup, query, existingNameResIds) {
        val pool = if (selectedMuscleGroup == null) ExerciseBank.getAllExercises()
        else ExerciseBank.getExercisesByMuscleGroup(selectedMuscleGroup!!)
        if (query.isBlank()) pool
        else pool.filter { context.getString(it.nameResId).contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exercise_catalog), fontWeight = FontWeight.Bold) },
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
            // Search field — needed once you have 40+ exercises and a tiny chip row.
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_exercises)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Muscle Group Filter Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedMuscleGroup == null,
                        onClick = { selectedMuscleGroup = null },
                        label = { Text(stringResource(R.string.all_exercises), fontSize = 14.sp) },
                        leadingIcon = if (selectedMuscleGroup == null) {
                            { Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                items(muscleGroups) { muscle ->
                    FilterChip(
                        selected = selectedMuscleGroup == muscle,
                        onClick = { selectedMuscleGroup = muscle },
                        label = { Text(muscle, fontSize = 14.sp) }
                    )
                }
            }

            HorizontalDivider()

            if (filteredExercises.isEmpty()) {
                CatalogEmptyState(
                    hasQuery = query.isNotBlank() || selectedMuscleGroup != null,
                    onClearFilters = {
                        query = ""
                        selectedMuscleGroup = null
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(filteredExercises, key = { it.id }) { exercise ->
                        val alreadyAdded = exercise.nameResId in existingNameResIds
                        ExerciseCatalogCard(
                            exercise = exercise,
                            alreadyAdded = alreadyAdded,
                            onAddClick = { onExerciseSelected(exercise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogEmptyState(hasQuery: Boolean, onClearFilters: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.no_exercises_found),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (hasQuery) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }
    }
}

// Theme-aware accent colors for muscle groups. Keep the palette stable so cards remain visually
// distinguishable, but use surface/onSurface for chrome so dark mode stays readable.
private object MuscleAccents {
    val Chest = Color(0xFF3B82F6)
    val Back = Color(0xFF10B981)
    val Shoulders = Color(0xFF8B5CF6)
    val Arms = Color(0xFFF59E0B)
    val Legs = Color(0xFFEF4444)
    val Core = Color(0xFF06B6D4)
    val Cardio = Color(0xFF22C55E)
    val Default = Color(0xFF3B82F6)
}

private fun muscleAccentColor(muscleGroup: String?): Color {
    if (muscleGroup == null) return MuscleAccents.Default
    val m = muscleGroup.lowercase()
    return when {
        muscleGroup.contains("חזה") || m.contains("chest") -> MuscleAccents.Chest
        muscleGroup.contains("גב") || m.contains("back") -> MuscleAccents.Back
        muscleGroup.contains("כתפיים") || m.contains("shoulder") -> MuscleAccents.Shoulders
        muscleGroup.contains("ידיים") || m.contains("arm") -> MuscleAccents.Arms
        muscleGroup.contains("רגליים") || m.contains("leg") -> MuscleAccents.Legs
        muscleGroup.contains("ליבה") || m.contains("core") -> MuscleAccents.Core
        muscleGroup.contains("אירובי") || m.contains("cardio") -> MuscleAccents.Cardio
        else -> MuscleAccents.Default
    }
}

@Composable
private fun ExerciseCatalogCard(
    exercise: ExerciseBankItem,
    alreadyAdded: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = muscleAccentColor(exercise.muscleGroups.firstOrNull())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        color = accent,
                        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = context.getString(exercise.nameResId),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (alreadyAdded) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.completed_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogStatChip(
                                tint = MuscleAccents.Chest,
                                icon = Icons.Default.FitnessCenter,
                                text = "${exercise.defaultSets} × ${exercise.defaultReps}"
                            )
                            if (exercise.defaultRestTime > 0) {
                                CatalogStatChip(
                                    tint = MuscleAccents.Arms,
                                    icon = Icons.Default.Timer,
                                    text = "${exercise.defaultRestTime}s"
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (alreadyAdded)
                                MaterialTheme.colorScheme.surfaceVariant else accent,
                            contentColor = if (alreadyAdded)
                                MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (alreadyAdded) 0.dp else 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = if (alreadyAdded) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (alreadyAdded) R.string.add_again else R.string.add_to_workout
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    exercise.muscleGroups.forEach { muscle ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.13f)
                        ) {
                            Text(
                                text = muscle,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (exercise.equipment.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = exercise.equipment,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogStatChip(tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
