package com.hananel.workouttracker2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.DefaultData
import com.hananel.workouttracker2.data.ExerciseBank
import com.hananel.workouttracker2.data.models.Exercise
import com.hananel.workouttracker2.data.models.ExerciseBankItem
import com.hananel.workouttracker2.data.models.WorkoutDay
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pager - disable user scrolling, only programmatic navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = false // Prevent swiping back
            ) { page ->
                when (page) {
                    0 -> OnboardingPage1(
                        onNext = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                    1 -> OnboardingPage2(
                        onFinish = onComplete
                    )
                }
            }
            
            // Page Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (pagerState.currentPage == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage1(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }
    
    // Recheck permission status when screen appears
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header - Logo and Title in same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.workout_logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.onboarding_page1_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Features Section
            Text(
                text = stringResource(R.string.onboarding_features_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Compact Feature List
            OnboardingFeatureItem(stringResource(R.string.onboarding_feature_1))
            OnboardingFeatureItem(stringResource(R.string.onboarding_feature_2))
            OnboardingFeatureItem(stringResource(R.string.onboarding_feature_3))
            OnboardingFeatureItem(stringResource(R.string.onboarding_feature_4))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Notification Permission - Compact version
            if (notificationPermissionGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.notifications_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.allow_notifications))
                        }
                    }
                }
            }
        }
        
        // Next Button at bottom
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OnboardingFeatureItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "✓",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun OnboardingPage2(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    val scope = rememberCoroutineScope()
    
    // State for custom workout builder
    var showWorkoutBuilder by remember { mutableStateOf(false) }
    
    if (showWorkoutBuilder) {
        WorkoutBuilderScreen(
            onFinish = { workoutDays ->
                scope.launch {
                    repository.saveWorkoutStructure(workoutDays)
                    // Update enabledDays based on selected days
                    val enabledDays = workoutDays.map { it.dayIndex }.sorted()
                    val prefs = repository.getUserPreferences()
                    repository.saveUserPreferences(prefs.copy(enabledDays = enabledDays))
                    onFinish()
                }
            },
            onBack = { showWorkoutBuilder = false }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_page2_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = stringResource(R.string.onboarding_page2_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Default Program Option
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            // Load default workout structure
                            val defaultStructure = DefaultData.getDefaultWorkoutStructure()
                            repository.saveWorkoutStructure(defaultStructure)
                            onFinish()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.use_default_program),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.use_default_program_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Customize Option
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showWorkoutBuilder = true
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.customize_workouts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.customize_workouts_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.can_change_later),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Skip button at bottom
            TextButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.skip),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutBuilderScreen(
    onFinish: (List<WorkoutDay>) -> Unit,
    onBack: () -> Unit,
    loadExistingData: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    
    // Step 1: Select which days to include
    var showDaySelection by remember { mutableStateOf(true) }
    var selectedDayIndices by remember { mutableStateOf(setOf(0, 1, 2, 3, 4, 5, 6)) } // All days by default
    
    // State for each day's configuration
    val workoutDays = remember {
        mutableStateListOf<WorkoutDay>()
    }
    
    // Load existing data or initialize empty
    LaunchedEffect(Unit) {
        if (loadExistingData) {
            // Load existing workout structure
            val existingDays = repository.getWorkoutStructure()
            if (existingDays.isNotEmpty()) {
                workoutDays.addAll(existingDays)
                selectedDayIndices = existingDays.map { it.dayIndex }.toSet()
                showDaySelection = false // Skip day selection, go directly to editing
            }
        }
    }
    
    // Initialize workout days when selection is confirmed (only for new setup)
    LaunchedEffect(showDaySelection) {
        if (!showDaySelection && workoutDays.isEmpty() && !loadExistingData) {
            val allDays = listOf(
                R.string.day_sunday to 0,
                R.string.day_monday to 1,
                R.string.day_tuesday to 2,
                R.string.day_wednesday to 3,
                R.string.day_thursday to 4,
                R.string.day_friday to 5,
                R.string.day_saturday to 6
            )
            
            workoutDays.addAll(
                allDays.filter { (_, index) -> selectedDayIndices.contains(index) }
                    .map { (nameResId, dayIndex) ->
                        WorkoutDay(
                            id = "day_$dayIndex",
                            nameResId = nameResId,
                            dayIndex = dayIndex,
                            exercises = emptyList(),
                            targetMuscleGroups = emptyList(),
                            customName = ""
                        )
                    }
            )
        }
    }
    
    // State for exercise selection dialog
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    
    if (showDaySelection) {
        // Day Selection Screen
        DaySelectionScreen(
            selectedDays = selectedDayIndices,
            onDayToggle = { dayIndex ->
                selectedDayIndices = if (selectedDayIndices.contains(dayIndex)) {
                    selectedDayIndices - dayIndex
                } else {
                    selectedDayIndices + dayIndex
                }
            },
            onContinue = { showDaySelection = false },
            onBack = onBack
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_builder)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onFinish(workoutDays) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = workoutDays.any { it.exercises.isNotEmpty() }
                ) {
                    Text(
                        text = stringResource(R.string.finish_setup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(workoutDays.size) { index ->
                WorkoutDayBuilderCard(
                    day = workoutDays[index],
                    onAddExercise = { selectedDayIndex = index },
                    onWorkoutTitleChange = { newTitle ->
                        workoutDays[index] = workoutDays[index].copy(customName = newTitle)
                    },
                    onRemoveExercise = { exerciseId ->
                        workoutDays[index] = workoutDays[index].copy(
                            exercises = workoutDays[index].exercises.filter { it.id != exerciseId }
                        )
                    }
                )
            }
        }
    }
    
    // Exercise selection dialog
    selectedDayIndex?.let { dayIndex ->
        val day = workoutDays[dayIndex]
        val displayName = if (day.customName.isNotBlank()) day.customName else context.getString(day.nameResId)
        
        ExerciseSelectionDialog(
            dayName = displayName,
            onDismiss = { selectedDayIndex = null },
            onExerciseSelected = { bankItem ->
                val newExercise = Exercise(
                    id = UUID.randomUUID().toString(),
                    nameResId = bankItem.nameResId,
                    sets = bankItem.defaultSets,
                    reps = bankItem.defaultReps,
                    restTime = bankItem.defaultRestTime,
                    hasWeight = bankItem.hasWeight,
                    muscleGroups = bankItem.muscleGroups,
                    equipment = bankItem.equipment
                )
                workoutDays[dayIndex] = workoutDays[dayIndex].copy(
                    exercises = workoutDays[dayIndex].exercises + newExercise
                )
            }
        )
    }
    
}

@Composable
private fun WorkoutDayBuilderCard(
    day: WorkoutDay,
    onAddExercise: () -> Unit,
    onWorkoutTitleChange: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dayName = context.getString(day.nameResId)
    var workoutTitle by remember(day.customName) { mutableStateOf(day.customName) }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Day Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Day Name (fixed)
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Workout Title (editable)
                    OutlinedTextField(
                        value = workoutTitle,
                        onValueChange = { newTitle ->
                            workoutTitle = newTitle
                            onWorkoutTitleChange(newTitle)
                        },
                        label = { Text(stringResource(R.string.workout_title)) },
                        placeholder = { Text(stringResource(R.string.workout_title_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${day.exercises.size} ${stringResource(R.string.add_exercise).lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Add Exercise Button
            FilledTonalButton(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_exercise))
            }
            
            // Exercise List
            if (day.exercises.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    day.exercises.forEach { exercise ->
                        ExerciseChip(
                            exercise = exercise,
                            onRemove = { onRemoveExercise(exercise.id) }
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_exercises_added),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseChip(
    exercise: Exercise,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Column {
                    Text(
                        text = context.getString(exercise.nameResId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${exercise.sets} × ${exercise.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ExerciseSelectionDialog(
    dayName: String,
    onDismiss: () -> Unit,
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_exercise_to_day, dayName),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Muscle Group Filters - Scrollable
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMuscleGroup == null,
                            onClick = { selectedMuscleGroup = null },
                            label = { Text(stringResource(R.string.all_exercises), fontSize = 13.sp) },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                    items(muscleGroups) { muscle ->
                        FilterChip(
                            selected = selectedMuscleGroup == muscle,
                            onClick = { selectedMuscleGroup = muscle },
                            label = { Text(muscle, fontSize = 13.sp) },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Exercise List - Show ALL filtered exercises
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredExercises) { exercise ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onExerciseSelected(exercise)
                                onDismiss()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(exercise.nameResId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${exercise.defaultSets} × ${exercise.defaultReps}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DaySelectionScreen(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dayNames = listOf(
        R.string.day_sunday,
        R.string.day_monday,
        R.string.day_tuesday,
        R.string.day_wednesday,
        R.string.day_thursday,
        R.string.day_friday,
        R.string.day_saturday
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_days_for_week)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = selectedDays.isNotEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_days_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Day selection cards
            dayNames.forEachIndexed { index, nameResId ->
                val isSelected = selectedDays.contains(index)
                
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onDayToggle(index) },
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isSelected) 6.dp else 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(nameResId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
