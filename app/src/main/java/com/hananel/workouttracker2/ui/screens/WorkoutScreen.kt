package com.hananel.workouttracker2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
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
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.ui.components.AddExerciseDialog
import com.hananel.workouttracker2.ui.components.AddExerciseMenu
import com.hananel.workouttracker2.ui.components.DaySelector
import com.hananel.workouttracker2.ui.components.ExerciseCard
import com.hananel.workouttracker2.ui.components.TimerDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToCatalog: (String) -> Unit,
    onResetWeekly: () -> Unit,
    shouldOpenTimerDialog: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { com.hananel.workouttracker2.data.repository.WorkoutRepository(context) }
    val workoutDays by viewModel.workoutDays.collectAsState()
    val workoutProgress by viewModel.workoutProgress.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    
    var enabledDays by remember { mutableStateOf(listOf(0, 1, 2, 3, 4)) }
    
    LaunchedEffect(Unit) {
        repository.getUserPreferencesFlow().collect { prefs ->
            enabledDays = prefs.enabledDays
        }
    }
    
    // Recompute currentWorkout when selectedDayIndex or workoutDays change
    val currentWorkout = remember(selectedDayIndex, workoutDays) {
        workoutDays.find { it.dayIndex == selectedDayIndex }
    }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddExerciseMenu by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Auto-open timer dialog when timer starts
    LaunchedEffect(timerState) {
        val currentTimerState = timerState
        if (currentTimerState != null && currentTimerState.isRunning) {
            showTimerDialog = true
        } else if (currentTimerState == null) {
            showTimerDialog = false
        }
    }
    
    // Open timer dialog when coming from notification
    LaunchedEffect(shouldOpenTimerDialog, timerState) {
        if (shouldOpenTimerDialog && timerState != null) {
            showTimerDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.workout_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    // Reset Week Button (Blue)
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset_week),
                            tint = Color(0xFF3B82F6)
                        )
                    }
                    // Notifications Button (Red)
                    IconButton(onClick = onNavigateToAlarms) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = Color(0xFFEF4444)
                        )
                    }
                    // Settings Button (Dark Gray)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color(0xFF374151)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentWorkout != null) {
                FloatingActionButton(
                    onClick = { showAddExerciseMenu = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_exercise))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Day Selector
            DaySelector(
                selectedDayIndex = selectedDayIndex,
                enabledDays = enabledDays,
                onDaySelected = { viewModel.selectDay(it) }
            )
            
            // Timer Running Indicator
        timerState?.let { state ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showTimerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = state.exerciseName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = stringResource(R.string.set_of, state.currentSet, state.totalSets),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA16207)
                                )
                            }
                        }
                        Text(
                            text = formatTime(state.timeLeft),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(if (timerState != null) 8.dp else 16.dp))
            
            // Content
            if (currentWorkout != null && currentWorkout.exercises.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Workout name header
                    item {
                        Text(
                            text = stringResource(currentWorkout.nameResId),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Exercises - Using reactive state from workoutProgress
                    items(currentWorkout.exercises, key = { it.id }) { exercise ->
                        // Read reactive state directly from the flow
                        val isCompleted = remember(workoutProgress) {
                            workoutProgress?.completedExercises?.get(exercise.id) ?: false
                        }
                        val completedSets = remember(workoutProgress) {
                            workoutProgress?.exerciseCompletedSets?.get(exercise.id) ?: 0
                        }
                        val weight = remember(workoutProgress) {
                            workoutProgress?.exerciseWeights?.get(exercise.id) ?: 0f
                        }
                        
                        ExerciseCard(
                            exercise = exercise,
                            isCompleted = isCompleted,
                            completedSets = completedSets,
                            weight = weight,
                            onWeightChange = { newWeight ->
                                viewModel.updateExerciseWeight(exercise.id, newWeight)
                            },
                            onStartRest = {
                                // Prevent starting new timer if one is already running
                                if (timerState == null) {
                                    viewModel.startTimer(exercise, context)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.timer_already_running),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onToggleComplete = {
                                viewModel.toggleExerciseCompletion(exercise.id)
                            },
                            onEditExercise = { sets, reps, restTime, newWeight ->
                                viewModel.updateExercise(exercise.id, sets, reps, restTime)
                                if (newWeight > 0) {
                                    viewModel.updateExerciseWeight(exercise.id, newWeight)
                                }
                            },
                            onDeleteExercise = {
                                currentWorkout?.let { workout ->
                                    viewModel.deleteExercise(workout.id, exercise.id)
                                }
                            }
                        )
                    }
                }
            } else {
                // Rest day or no exercises
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
                        Text(
                            text = stringResource(R.string.rest_day),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.rest_day_message),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Timer Dialog
        if (showTimerDialog && timerState != null) {
            TimerDialog(
                timerState = timerState!!,
                onNextSet = {
                    viewModel.completeSet(
                        context = context,
                        onAllSetsComplete = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.exercise_completed),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                },
                onStop = {
                    // Stop immediately
                    viewModel.stopTimer(context)
                    showTimerDialog = false
                },
                onClose = {
                    // Close dialog but keep timer running - don't stop the service
                    // The timer indicator banner will still show
                    showTimerDialog = false
                },
                onTimerTick = { timeLeft ->
                    // Not needed anymore - timer runs in service
                }
            )
        }
        
        
        // Add Exercise Menu
        if (showAddExerciseMenu) {
            AddExerciseMenu(
                onDismiss = { showAddExerciseMenu = false },
                onCreateNew = { showCreateExerciseDialog = true },
                onChooseFromCatalog = {
                    currentWorkout?.let { workout ->
                        onNavigateToCatalog(workout.id)
                    }
                }
            )
        }
        
        // Create Custom Exercise Dialog
        if (showCreateExerciseDialog) {
            AddExerciseDialog(
                onDismiss = { showCreateExerciseDialog = false },
                onAdd = { name, sets, reps, restTime, hasWeight, weight ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "תרגיל \"$name\" נוסף בהצלחה! (תכונה זו תשולב בגרסה הבאה)",
                            duration = SnackbarDuration.Long
                        )
                    }
                    showCreateExerciseDialog = false
                }
            )
        }
        
        // Reset Confirmation Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_confirmation)) },
                text = { Text(stringResource(R.string.reset_confirmation_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onResetWeekly()
                            showResetDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
