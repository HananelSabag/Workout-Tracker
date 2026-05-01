package com.hananel.workouttracker2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.getDisplayName
import com.hananel.workouttracker2.ui.components.AddExerciseDialog
import com.hananel.workouttracker2.ui.components.AddExerciseMenu
import com.hananel.workouttracker2.ui.components.DaySelector
import com.hananel.workouttracker2.ui.components.ExerciseCard
import com.hananel.workouttracker2.ui.components.TimerBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToCatalog: (String) -> Unit,
    onNavigateToWorkoutBuilder: () -> Unit,
    onResetWeekly: () -> Unit,
    shouldOpenTimerDialog: Boolean = false,
    onTimerDialogOpened: () -> Unit = {},
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

    val currentWorkout = remember(selectedDayIndex, workoutDays) {
        workoutDays.find { it.dayIndex == selectedDayIndex }
    }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddExerciseMenu by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    // Tracks if user deliberately dismissed the sheet — prevents auto-reopen on state changes
    var userClosedSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Auto-open timer sheet when:
    //  - a NEW countdown starts (key on exerciseId+currentSet), OR
    //  - the user re-enters the app and rest is already complete (isRestComplete flips to true).
    // Without the second key, a user who left the app during rest and returns AFTER the rest
    // finished would see only the banner with no clear way to advance — the sheet exposes the big
    // "Start Next Set" action.
    LaunchedEffect(
        timerState?.exerciseId,
        timerState?.currentSet,
        timerState?.isRestComplete
    ) {
        val currentTimerState = timerState
        when {
            currentTimerState == null -> {
                showTimerSheet = false
                userClosedSheet = false
            }
            (currentTimerState.isRunning || currentTimerState.isRestComplete) && !userClosedSheet -> {
                showTimerSheet = true
            }
        }
    }

    // Open sheet when app is opened from a notification tap
    LaunchedEffect(shouldOpenTimerDialog) {
        if (shouldOpenTimerDialog && timerState != null) {
            userClosedSheet = false
            showTimerSheet = true
            onTimerDialogOpened() // Resets the flag in MainActivity so it doesn't re-fire
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.workout_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.app_name),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                actions = {
                    // Uniform tonal style across all 3 actions — prior version mixed three different
                    // background colors which made the bar feel cluttered. Each action keeps its
                    // tinted icon for recognizability.
                    val actionContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    FilledTonalIconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = actionContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset_week),
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalIconButton(
                        onClick = onNavigateToAlarms,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = actionContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalIconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = actionContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentWorkout != null) {
                FloatingActionButton(onClick = { showAddExerciseMenu = true }) {
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
            if (workoutDays.isNotEmpty()) {
                if (enabledDays.isEmpty()) {
                    // User disabled every weekday in Settings → DaySelector would render nothing,
                    // and the screen would silently fall through to the "Rest day" empty state with
                    // no clear way back. Show an explicit banner with a CTA into Settings.
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBEB),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFFB45309)
                            )
                            Text(
                                text = stringResource(R.string.no_days_enabled_banner),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    DaySelector(
                        selectedDayIndex = selectedDayIndex,
                        enabledDays = enabledDays,
                        onDaySelected = { viewModel.selectDay(it) }
                    )
                }
            }

            // ── Timer Running Banner ──────────────────────────────────────────
            timerState?.let { state ->
                // Use the explicit flag from the service. Falls back to the inferred condition for
                // states persisted by older versions before the flag existed.
                val isRestComplete = state.isRestComplete || (!state.isRunning && state.timeLeft == 0)

                val bannerBg by animateColorAsState(
                    targetValue = if (isRestComplete) Color(0xFFDCFCE7) else Color(0xFFFFF7ED),
                    animationSpec = tween(600),
                    label = "bannerBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isRestComplete) Color(0xFF166534) else Color(0xFF92400E),
                    animationSpec = tween(600),
                    label = "textColor"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isRestComplete) Color(0xFF22C55E) else Color(0xFFD97706),
                    animationSpec = tween(600),
                    label = "iconColor"
                )

                // Pulse the icon when rest is complete
                val infiniteTransition = rememberInfiniteTransition(label = "bannerPulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isRestComplete) 0.55f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                val progressFraction = if (state.totalTime > 0)
                    (state.totalTime - state.timeLeft).toFloat() / state.totalTime.toFloat()
                else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(900),
                    label = "timerProgress"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            userClosedSheet = false
                            showTimerSheet = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = bannerBg,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isRestComplete) Icons.Default.Check else Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .alpha(if (isRestComplete) pulseAlpha else 1f)
                                )
                                Column {
                                    Text(
                                        text = state.exerciseName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = if (isRestComplete)
                                            stringResource(R.string.tap_for_next_set)
                                        else
                                            stringResource(R.string.set_of, state.currentSet, state.totalSets),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            AnimatedContent(
                                targetState = if (isRestComplete)
                                    stringResource(R.string.rest_complete)
                                else
                                    formatTime(state.timeLeft),
                                label = "bannerTime"
                            ) { timeStr ->
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }

                        // Progress bar while counting down
                        if (!isRestComplete) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = Color(0xFFD97706),
                                trackColor = Color(0xFFD97706).copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (timerState != null) 4.dp else 16.dp))

            // ── Exercise List ─────────────────────────────────────────────────
            if (currentWorkout != null && currentWorkout.exercises.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        val completedCount = remember(workoutProgress) {
                            workoutProgress?.completedExercises?.count { it.value } ?: 0
                        }
                        val totalExercises = currentWorkout.exercises.size
                        val isAllDone = completedCount == totalExercises && totalExercises > 0
                        val progressFraction =
                            if (totalExercises > 0) completedCount.toFloat() / totalExercises else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressFraction,
                            animationSpec = tween(600),
                            label = "dayProgress"
                        )
                        val progressColor = if (isAllDone) Color(0xFF10B981) else Color(0xFF3B82F6)

                        var showEditDialog by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentWorkout.getDisplayName(context),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = progressColor,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    )
                                    Text(
                                        text = "$completedCount/$totalExercises",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllDone) Color(0xFF10B981)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (showEditDialog) {
                            RenameDaySheet(
                                initialName = currentWorkout.customName,
                                onDismiss = { showEditDialog = false },
                                onSave = { newName ->
                                    viewModel.renameWorkoutDay(currentWorkout.id, newName)
                                    showEditDialog = false
                                }
                            )
                        }
                    }

                    // Compound key — exercise IDs include a timestamp suffix and could (very rarely)
                    // collide across days when items are added back-to-back. Pairing with the day
                    // ID guarantees identity even in that edge case.
                    items(currentWorkout.exercises, key = { "${currentWorkout.id}_${it.id}" }) { exercise ->
                        val isCompleted = remember(workoutProgress) {
                            workoutProgress?.completedExercises?.get(exercise.id) ?: false
                        }
                        val completedSets = remember(workoutProgress, timerState) {
                            val ts = timerState
                            if (ts?.exerciseId == exercise.id) ts.completedSets
                            else workoutProgress?.exerciseCompletedSets?.get(exercise.id) ?: 0
                        }
                        val weight = remember(workoutProgress) {
                            workoutProgress?.exerciseWeights?.get(exercise.id) ?: 0f
                        }

                        val activeTimer = timerState
                        // True when this card's exercise is the one the timer is currently
                        // associated with AND the rest already finished — the action button should
                        // say "Start Next Set" and tapping it advances the same timer.
                        val isRestCompleteForThis = activeTimer != null
                            && activeTimer.exerciseId == exercise.id
                            && (activeTimer.isRestComplete || (!activeTimer.isRunning && activeTimer.timeLeft == 0))
                        // True while the rest countdown is actively running for THIS exercise —
                        // disables the start button to prevent double-starts.
                        val isRestRunningForThis = activeTimer != null
                            && activeTimer.exerciseId == exercise.id
                            && activeTimer.isRunning

                        ExerciseCard(
                            exercise = exercise,
                            isCompleted = isCompleted,
                            completedSets = completedSets,
                            weight = weight,
                            isRestCompleteForThis = isRestCompleteForThis,
                            isRestRunningForThis = isRestRunningForThis,
                            onWeightChange = { viewModel.updateExerciseWeight(exercise.id, it) },
                            onStartRest = {
                                when {
                                    isRestCompleteForThis -> {
                                        // Rest already finished for this exercise — advance the timer.
                                        viewModel.completeSet(context)
                                    }
                                    activeTimer == null -> viewModel.startTimer(exercise, context)
                                    else -> {
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.timer_already_running),
                                                actionLabel = context.getString(R.string.timer_view_action),
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                userClosedSheet = false
                                                showTimerSheet = true
                                            }
                                        }
                                    }
                                }
                            },
                            onToggleComplete = { viewModel.toggleExerciseCompletion(exercise.id) },
                            onEditExercise = { sets, reps, restTime, newWeight ->
                                viewModel.updateExercise(exercise.id, sets, reps, restTime)
                                if (newWeight > 0) viewModel.updateExerciseWeight(exercise.id, newWeight)
                            },
                            onDeleteExercise = {
                                currentWorkout?.let { viewModel.deleteExercise(it.id, exercise.id) }
                            }
                        )
                    }
                }
            } else if (workoutDays.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.no_workout_structure),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_workout_structure_message),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onNavigateToWorkoutBuilder,
                            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.setup_workout_plan),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
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

        // ── Timer Bottom Sheet ────────────────────────────────────────────────
        if (showTimerSheet && timerState != null) {
            TimerBottomSheet(
                timerState = timerState!!,
                onNextSet = { viewModel.completeSet(context) },
                onFinish = {
                    viewModel.finishExercise(context)
                    userClosedSheet = true
                    showTimerSheet = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.exercise_completed),
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onStop = {
                    viewModel.stopTimer(context)
                    userClosedSheet = true
                    showTimerSheet = false
                },
                onDismiss = {
                    userClosedSheet = true
                    showTimerSheet = false
                }
            )
        }

        // ── Add Exercise Menu ─────────────────────────────────────────────────
        if (showAddExerciseMenu) {
            AddExerciseMenu(
                onDismiss = { showAddExerciseMenu = false },
                onCreateNew = { showCreateExerciseDialog = true },
                onChooseFromCatalog = {
                    currentWorkout?.let { onNavigateToCatalog(it.id) }
                }
            )
        }

        if (showCreateExerciseDialog) {
            AddExerciseDialog(
                onDismiss = { showCreateExerciseDialog = false },
                onAdd = { name, sets, reps, restTime, hasWeight, weight ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.exercise_added_success, name),
                            duration = SnackbarDuration.Long
                        )
                    }
                    showCreateExerciseDialog = false
                }
            )
        }

        if (showResetDialog) {
            ResetWeekSheet(
                onDismiss = { showResetDialog = false },
                onConfirm = {
                    onResetWeekly()
                    showResetDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameDaySheet(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var dayName by remember { mutableStateOf(initialName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.rename_day),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = dayName,
                onValueChange = { dayName = it },
                placeholder = { Text(stringResource(R.string.workout_title_hint)) },
                label = { Text(stringResource(R.string.workout_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
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
                    onClick = { onSave(dayName) },
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetWeekSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.reset_confirmation),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.reset_confirmation_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
