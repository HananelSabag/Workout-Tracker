package com.hananel.workouttracker2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    
    var hasRequestedPermission by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
        hasRequestedPermission = true
    }
    
    // Auto-request permission when screen appears (once)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val currentStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            notificationPermissionGranted = currentStatus
            
            // Auto-request if not granted and not yet requested
            if (!currentStatus && !hasRequestedPermission) {
                // Small delay to let the UI settle before showing dialog
                kotlinx.coroutines.delay(500)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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
            // Header - Logo centered with nice container
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo with white background
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(90.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.workout_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
            
            // Notification Permission - With status feedback
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (notificationPermissionGranted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (notificationPermissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.enable_notifications_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (notificationPermissionGranted) {
                                    stringResource(R.string.notifications_granted)
                                } else {
                                    stringResource(R.string.enable_notifications_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (notificationPermissionGranted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    
                    if (notificationPermissionGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        TextButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
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
                    // CRITICAL: Clear ALL stale data first (cloud backup restore fix)
                    repository.clearAllProgressAndTimerState()
                    
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
                            // CRITICAL: Clear ALL stale data first
                            // This fixes issues with cloud backup restoring old timer state/progress
                            repository.clearAllProgressAndTimerState()
                            
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
            
            // Skip button at bottom (for existing users who already have data)
            TextButton(
                onClick = {
                    // Skip just completes onboarding
                    // First, clear any potentially corrupted data from cloud backup restore
                    scope.launch {
                        // Check if there's valid data (existing user) or corrupted data (cloud restore)
                        val existingStructure = repository.getWorkoutStructure()
                        if (existingStructure.isNotEmpty()) {
                            // Validate the data - check if first exercise nameResId is valid
                            val firstExercise = existingStructure.firstOrNull()?.exercises?.firstOrNull()
                            if (firstExercise != null) {
                                try {
                                    // Try to get the string - if it fails or returns wrong type of string, clear
                                    val name = context.getString(firstExercise.nameResId)
                                    // Check if it's an exercise name (should start with uppercase letter and not contain formatting)
                                    if (name.contains("%") || name.length < 3) {
                                        // Corrupted data - clear it
                                        repository.saveWorkoutStructure(emptyList())
                                    }
                                } catch (e: Exception) {
                                    // Invalid resource ID - clear the data
                                    repository.saveWorkoutStructure(emptyList())
                                }
                            }
                        }
                        onFinish()
                    }
                },
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
    
    // Store existing days for reference (before user changes)
    val existingDaysMap = remember { mutableStateMapOf<Int, WorkoutDay>() }
    
    // Load existing data
    LaunchedEffect(Unit) {
        if (loadExistingData) {
            // Load existing workout structure
            val existingDays = repository.getWorkoutStructure()
            if (existingDays.isNotEmpty()) {
                // Store existing days in map for later reference
                existingDays.forEach { day ->
                    existingDaysMap[day.dayIndex] = day
                }
                // Pre-select the existing days
                selectedDayIndices = existingDays.map { it.dayIndex }.toSet()
                // Still show day selection so user can remove days
            }
        }
    }
    
    // Initialize workout days when selection is confirmed
    LaunchedEffect(showDaySelection) {
        if (!showDaySelection && workoutDays.isEmpty()) {
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
                        // Use existing day if available, otherwise create empty
                        existingDaysMap[dayIndex] ?: WorkoutDay(
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
            existingDaysMap = existingDaysMap,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
    
    // Muscle group colors
    val muscleGroupColors = mapOf(
        "Chest" to Color(0xFFEF4444),
        "Back" to Color(0xFF3B82F6),
        "Shoulders" to Color(0xFFF59E0B),
        "Biceps" to Color(0xFF8B5CF6),
        "Triceps" to Color(0xFF10B981),
        "Legs" to Color(0xFFEC4899),
        "Core" to Color(0xFF06B6D4),
        "Full Body" to Color(0xFF6366F1),
        "Cardio" to Color(0xFFF97316)
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.add_exercise),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                }
                
                // Muscle Group Filters
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedMuscleGroup == null,
                            onClick = { selectedMuscleGroup = null },
                            label = { 
                                Text(
                                    stringResource(R.string.all_exercises), 
                                    fontWeight = if (selectedMuscleGroup == null) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            leadingIcon = if (selectedMuscleGroup == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    items(muscleGroups) { muscle ->
                        val chipColor = muscleGroupColors[muscle] ?: Color(0xFF6B7280)
                        FilterChip(
                            selected = selectedMuscleGroup == muscle,
                            onClick = { selectedMuscleGroup = muscle },
                            label = { 
                                Text(
                                    muscle,
                                    fontWeight = if (selectedMuscleGroup == muscle) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (selectedMuscleGroup == muscle) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
                
                // Results count
                Text(
                    text = "${filteredExercises.size} ${stringResource(R.string.add_exercise).lowercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Exercise List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(filteredExercises) { exercise ->
                        val primaryMuscle = exercise.muscleGroups.firstOrNull() ?: "Full Body"
                        val accentColor = muscleGroupColors[primaryMuscle] ?: Color(0xFF6B7280)
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onExerciseSelected(exercise)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Muscle indicator
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = accentColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(exercise.nameResId),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Sets × Reps badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF3B82F6).copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "${exercise.defaultSets} × ${exercise.defaultReps}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        // Rest time badge
                                        if (exercise.defaultRestTime > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = "${exercise.defaultRestTime}s",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFD97706),
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Add button
                                FilledTonalIconButton(
                                    onClick = {
                                        onExerciseSelected(exercise)
                                        onDismiss()
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = accentColor.copy(alpha = 0.15f),
                                        contentColor = accentColor
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_exercise),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DaySelectionScreen(
    selectedDays: Set<Int>,
    existingDaysMap: Map<Int, WorkoutDay> = emptyMap(),
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
    
    // Warning dialog state
    var showWarningDialog by remember { mutableStateOf(false) }
    var pendingDayToRemove by remember { mutableStateOf<Int?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_days_for_week)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                val existingDay = existingDaysMap[index]
                val hasExercises = existingDay != null && existingDay.exercises.isNotEmpty()
                val exerciseCount = existingDay?.exercises?.size ?: 0
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // If unselecting a day with exercises, show warning
                        if (isSelected && hasExercises) {
                            pendingDayToRemove = index
                            showWarningDialog = true
                        } else {
                            // Otherwise just toggle
                            onDayToggle(index)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Checkbox
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null, // Handled by Surface onClick
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            Column {
                                Text(
                                    text = context.getString(nameResId),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                // Show exercise count if day has exercises
                                if (hasExercises) {
                                    Text(
                                        text = "$exerciseCount ${context.getString(R.string.add_exercise).lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Show edit icon if day has exercises and is selected
                        if (isSelected && hasExercises) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Warning dialog when removing day with exercises
        if (showWarningDialog && pendingDayToRemove != null) {
            AlertDialog(
                onDismissRequest = {
                    showWarningDialog = false
                    pendingDayToRemove = null
                },
                title = {
                    Text(stringResource(R.string.remove_day_warning_title))
                },
                text = {
                    Column {
                        Text(stringResource(R.string.remove_day_warning_message))
                        Spacer(modifier = Modifier.height(8.dp))
                        val dayName = context.getString(dayNames[pendingDayToRemove!!])
                        val exerciseCount = existingDaysMap[pendingDayToRemove!!]?.exercises?.size ?: 0
                        Text(
                            text = stringResource(R.string.day_has_exercises, dayName, exerciseCount),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDayToRemove?.let { dayIndex ->
                                onDayToggle(dayIndex)
                            }
                            showWarningDialog = false
                            pendingDayToRemove = null
                        }
                    ) {
                        Text(
                            stringResource(R.string.remove_day_confirm),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showWarningDialog = false
                            pendingDayToRemove = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
