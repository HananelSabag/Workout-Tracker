package com.hananel.workouttracker2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.getDisplayName
import com.hananel.workouttracker2.ui.components.ActiveSessionBar
import com.hananel.workouttracker2.ui.components.AddExerciseDialog
import com.hananel.workouttracker2.ui.components.AddExerciseMenu
import com.hananel.workouttracker2.ui.components.DayTabBar
import com.hananel.workouttracker2.ui.components.DayTabState
import com.hananel.workouttracker2.ui.components.ExerciseCard
import com.hananel.workouttracker2.ui.components.GhostButton
import com.hananel.workouttracker2.ui.components.GlassSheet
import com.hananel.workouttracker2.ui.components.GradientButton
import com.hananel.workouttracker2.ui.components.GlassTextField
import com.hananel.workouttracker2.ui.components.LastSessionCard
import com.hananel.workouttracker2.ui.components.TimerBottomSheet
import com.hananel.workouttracker2.ui.theme.AccentGradient
import com.hananel.workouttracker2.ui.theme.Amber
import com.hananel.workouttracker2.ui.theme.AppBackground
import com.hananel.workouttracker2.ui.theme.Electric
import com.hananel.workouttracker2.ui.theme.GlassCard
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.SuccessGradient
import com.hananel.workouttracker2.ui.theme.chipTint
import com.hananel.workouttracker2.ui.theme.rememberGlassColors
import com.hananel.workouttracker2.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToCatalog: (String) -> Unit,
    onNavigateToWorkoutBuilder: () -> Unit,
    onNavigateToHistory: () -> Unit,
    shouldOpenTimerDialog: Boolean = false,
    onTimerDialogOpened: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = rememberGlassColors()
    val repository = remember { com.hananel.workouttracker2.data.repository.WorkoutRepository(context) }

    val workoutDays by viewModel.workoutDays.collectAsState()
    val workoutProgress by viewModel.workoutProgress.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val sessionHistory by viewModel.sessionHistory.collectAsState()
    val now by viewModel.now.collectAsState()

    var enabledDays by remember { mutableStateOf(listOf(0, 1, 2, 3, 4)) }
    LaunchedEffect(Unit) {
        repository.getUserPreferencesFlow().collect { prefs -> enabledDays = prefs.enabledDays }
    }

    // If the OS killed the app mid-rest, pick the countdown back up where the clock says it should be.
    LaunchedEffect(Unit) { viewModel.recoverTimerIfInterrupted(context) }

    val currentWorkout = remember(selectedDayIndex, workoutDays) {
        workoutDays.find { it.dayIndex == selectedDayIndex }
    }

    var showAddExerciseMenu by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showEndSessionSheet by remember { mutableStateOf(false) }
    // Tracks a deliberate dismissal so state changes don't yank the sheet back open.
    var userClosedSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Say why the checkmarks went away, so a cleared day never reads as lost data.
    LaunchedEffect(Unit) {
        viewModel.dayResetNotice.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.fresh_day_reset))
        }
    }

    // Log a set and the timer comes up on its own — you see the countdown, and can send it to the
    // background with one tap. Keyed on the set number so each new rest re-opens it even if the
    // previous one was dismissed; the flag only suppresses re-opening within the same rest.
    LaunchedEffect(timerState?.exerciseId, timerState?.completedSets) {
        if (timerState != null) {
            userClosedSheet = false
            showTimerSheet = true
        }
    }

    LaunchedEffect(timerState?.isRestComplete) {
        val state = timerState
        when {
            state == null -> {
                showTimerSheet = false
                userClosedSheet = false
            }
            // Rest finishing is its own event, so this ignores an earlier "background" tap: the
            // countdown ending is exactly when you want the next set one tap away. Keyed on the
            // flag, so dismissing this sheet doesn't bounce it straight back.
            state.isRestComplete -> {
                userClosedSheet = false
                showTimerSheet = true
            }
        }
    }

    LaunchedEffect(shouldOpenTimerDialog) {
        if (shouldOpenTimerDialog && timerState != null) {
            userClosedSheet = false
            showTimerSheet = true
            onTimerDialogOpened()
        }
    }

    // Per-day completion, so the tab dots reflect that day only — the old header counted every
    // completed exercise in the whole week against the current day's total.
    val dayTabs = remember(workoutDays, workoutProgress, enabledDays) {
        val today = DateUtils.getCurrentDayIndex()
        enabledDays.sorted().map { index ->
            val day = workoutDays.find { it.dayIndex == index }
            val exercises = day?.exercises.orEmpty()
            val done = exercises.count { workoutProgress?.completedExercises?.get(it.id) == true }
            DayTabState(
                dayIndex = index,
                progress = if (exercises.isEmpty()) 0f else done.toFloat() / exercises.size,
                hasWorkout = exercises.isNotEmpty(),
                isToday = index == today
            )
        }
    }

    val dayCompletedCount = remember(currentWorkout, workoutProgress) {
        currentWorkout?.exercises?.count { workoutProgress?.completedExercises?.get(it.id) == true } ?: 0
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.workout_logo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.onGlass
                            )
                        }
                    },
                    actions = {
                        GlassIconButton(Icons.Default.History, Electric, stringResource(R.string.workout_history), onNavigateToHistory)
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.Notifications, Amber, stringResource(R.string.notifications), onNavigateToAlarms)
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.Settings, glass.onGlassMuted, stringResource(R.string.settings), onNavigateToSettings)
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                if (currentWorkout != null) {
                    FloatingActionButton(
                        onClick = { showAddExerciseMenu = true },
                        containerColor = Color.Transparent,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(AccentGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_exercise),
                                tint = Color.White
                            )
                        }
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
                        NoDaysBanner(onClick = onNavigateToSettings)
                    } else {
                        DayTabBar(
                            selectedDayIndex = selectedDayIndex,
                            days = dayTabs,
                            onDaySelected = { viewModel.selectDay(it) }
                        )
                    }
                }

                // Compact rest banner — the per-card strip disappears once the user scrolls, this
                // keeps the countdown reachable from anywhere on the screen.
                AnimatedVisibility(
                    visible = timerState != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    timerState?.let { state ->
                        RestBanner(
                            exerciseName = state.exerciseName,
                            timeLeft = state.timeLeft,
                            totalTime = state.totalTime,
                            isRestComplete = state.isRestComplete || (!state.isRunning && !state.isPaused && state.timeLeft == 0),
                            nextSet = state.nextSet,
                            totalSets = state.totalSets,
                            onClick = {
                                userClosedSheet = false
                                showTimerSheet = true
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                when {
                    currentWorkout != null && currentWorkout.exercises.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "session") {
                                val session = activeSession
                                if (session != null) {
                                    ActiveSessionBar(
                                        session = session,
                                        now = now,
                                        onEndSession = { showEndSessionSheet = true }
                                    )
                                } else {
                                    sessionHistory.firstOrNull()?.let { last ->
                                        LastSessionCard(session = last, onClick = onNavigateToHistory)
                                    }
                                }
                            }

                            item(key = "header") {
                                DayHeader(
                                    title = currentWorkout.getDisplayName(context),
                                    completedCount = dayCompletedCount,
                                    totalCount = currentWorkout.exercises.size,
                                    onEditName = { showRenameSheet = true }
                                )
                            }

                            items(
                                currentWorkout.exercises,
                                key = { "${currentWorkout.id}_${it.id}" }
                            ) { exercise ->
                                val isCompleted = workoutProgress?.completedExercises?.get(exercise.id) ?: false
                                val completedSets = workoutProgress?.exerciseCompletedSets?.get(exercise.id) ?: 0
                                val weight = workoutProgress?.exerciseWeights?.get(exercise.id) ?: 0f

                                val timer = timerState
                                val isThisExercise = timer != null && timer.exerciseId == exercise.id
                                val restComplete = isThisExercise &&
                                        (timer!!.isRestComplete || (!timer.isRunning && !timer.isPaused && timer.timeLeft == 0))
                                val restRunning = isThisExercise && !restComplete

                                ExerciseCard(
                                    exercise = exercise,
                                    isCompleted = isCompleted,
                                    completedSets = completedSets,
                                    weight = weight,
                                    restTimeLeft = if (isThisExercise) timer!!.timeLeft else 0,
                                    restTotalTime = if (isThisExercise) timer!!.totalTime else 0,
                                    isRestRunningForThis = restRunning,
                                    isRestCompleteForThis = restComplete,
                                    previousLog = remember(sessionHistory, exercise.id) {
                                        viewModel.previousLogFor(exercise.id)
                                    },
                                    onCompleteSet = { viewModel.completeSet(exercise, context) },
                                    onUndoSet = { viewModel.undoSet(exercise, context) },
                                    onSkipRest = { viewModel.skipRest(context) },
                                    onWeightChange = { viewModel.updateExerciseWeight(exercise.id, it) },
                                    onToggleComplete = { viewModel.toggleExerciseCompletion(exercise.id) },
                                    onEditExercise = { sets, reps, restTime, newWeight ->
                                        viewModel.updateExercise(exercise.id, sets, reps, restTime)
                                        if (newWeight > 0) viewModel.updateExerciseWeight(exercise.id, newWeight)
                                    },
                                    onDeleteExercise = { viewModel.deleteExercise(currentWorkout.id, exercise.id) }
                                )
                            }
                        }
                    }

                    workoutDays.isEmpty() -> EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = stringResource(R.string.no_workout_structure),
                        message = stringResource(R.string.no_workout_structure_message),
                        actionLabel = stringResource(R.string.setup_workout_plan),
                        onAction = onNavigateToWorkoutBuilder
                    )

                    else -> EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = stringResource(R.string.rest_day),
                        message = stringResource(R.string.rest_day_message),
                        actionLabel = null,
                        onAction = {}
                    )
                }
            }
        }
    }

    // ── Sheets ──────────────────────────────────────────────────────────────
    if (showTimerSheet && timerState != null) {
        val state = timerState!!
        TimerBottomSheet(
            timerState = state,
            // Reps come from the session log, so the stepper shows what was actually recorded.
            loggedReps = activeSession?.exercises
                ?.firstOrNull { it.exerciseId == state.exerciseId }
                ?.repsPerSet?.lastOrNull() ?: 0,
            onRepsChange = { viewModel.setLastSetReps(state.exerciseId, it) },
            // Deliberately does not close the sheet: logging the next set starts the next rest and
            // the countdown simply continues in place.
            onCompleteSet = { viewModel.completeSetFromTimer(context) },
            onSkipRest = {
                viewModel.skipRest(context)
                userClosedSheet = true
                showTimerSheet = false
            },
            onPauseResume = { viewModel.pauseResumeTimer(context) },
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

    // Ending a workout is the one moment worth confirming: it's the point the session becomes a
    // history entry, and the previous silent close left no sign it had been saved at all.
    if (showEndSessionSheet) {
        val session = activeSession
        if (session == null) {
            showEndSessionSheet = false
        } else {
            EndSessionSheet(
                session = session,
                now = now,
                onDismiss = { showEndSessionSheet = false },
                onConfirm = {
                    viewModel.endSession(context)
                    showEndSessionSheet = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.workout_saved),
                            actionLabel = context.getString(R.string.view_history),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) onNavigateToHistory()
                    }
                }
            )
        }
    }

    if (showRenameSheet && currentWorkout != null) {
        RenameDaySheet(
            initialName = currentWorkout.customName,
            onDismiss = { showRenameSheet = false },
            onSave = { newName ->
                viewModel.renameWorkoutDay(currentWorkout.id, newName)
                showRenameSheet = false
            }
        )
    }

    if (showAddExerciseMenu) {
        AddExerciseMenu(
            onDismiss = { showAddExerciseMenu = false },
            onCreateNew = { showCreateExerciseDialog = true },
            onChooseFromCatalog = { currentWorkout?.let { onNavigateToCatalog(it.id) } }
        )
    }

    if (showCreateExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showCreateExerciseDialog = false },
            onAdd = { name, _, _, _, _, _ ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.exercise_added_success, name)
                    )
                }
                showCreateExerciseDialog = false
            }
        )
    }
}

// ─── Top bar pieces ──────────────────────────────────────────────────────────

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val glass = rememberGlassColors()
    val shape = RoundedCornerShape(13.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(glass.fill, shape)
                .border(1.dp, glass.border, shape)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
        }
    }
}

// ─── Rest banner ─────────────────────────────────────────────────────────────

@Composable
private fun RestBanner(
    exerciseName: String,
    timeLeft: Int,
    totalTime: Int,
    isRestComplete: Boolean,
    nextSet: Int,
    totalSets: Int,
    onClick: () -> Unit
) {
    val glass = rememberGlassColors()
    val accent = if (isRestComplete) Mint else Amber
    val progress by animateFloatAsState(
        targetValue = if (totalTime > 0) timeLeft.toFloat() / totalTime else 0f,
        animationSpec = tween(900),
        label = "bannerProgress"
    )

    GlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        tint = accent,
        tintAlpha = 0.16f
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // An explicit "rest" badge, because this bar sits right above the workout
                    // clock and two bare countdowns next to each other read as noise.
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(chipTint(accent))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = accent, modifier = Modifier.size(13.dp))
                        Text(
                            text = stringResource(R.string.rest_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Column {
                        Text(
                            text = exerciseName,
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.onGlass,
                            maxLines = 1
                        )
                        Text(
                            text = if (isRestComplete)
                                stringResource(R.string.time_for_set, nextSet, totalSets)
                            else stringResource(R.string.resting_now),
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.onGlassMuted
                        )
                    }
                }
                Text(
                    text = if (isRestComplete) stringResource(R.string.rest_complete)
                    else "%d:%02d".format(timeLeft / 60, timeLeft % 60),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            }
            if (!isRestComplete) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.18f)
                )
            }
        }
    }
}

// ─── Day header ──────────────────────────────────────────────────────────────

@Composable
private fun DayHeader(
    title: String,
    completedCount: Int,
    totalCount: Int,
    onEditName: () -> Unit
) {
    val glass = rememberGlassColors()
    val allDone = completedCount == totalCount && totalCount > 0
    val accent = if (allDone) Mint else Electric
    val fraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(600),
        label = "dayProgress"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tint = if (allDone) Mint else Color.Unspecified,
        tintAlpha = 0.12f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = glass.onGlass
                    )
                    Text(
                        text = if (totalCount == 1) stringResource(R.string.exercise_count_one)
                        else stringResource(R.string.exercise_count_many, totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.onGlassMuted
                    )
                }
                Surface(
                    onClick = onEditName,
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = glass.onGlass.copy(alpha = 0.06f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = glass.onGlassMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = accent,
                    trackColor = glass.onGlass.copy(alpha = 0.08f)
                )
                Text(
                    text = "$completedCount/$totalCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }
    }
}

// ─── Empty / banner states ───────────────────────────────────────────────────

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    val glass = rememberGlassColors()
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(chipTint(Electric)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Electric, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = glass.onGlass,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = glass.onGlassMuted,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(28.dp))
                GradientButton(
                    text = actionLabel,
                    icon = Icons.Default.Add,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(0.75f),
                    height = 56.dp,
                    fontSize = 16
                )
            }
        }
    }
}

@Composable
private fun NoDaysBanner(onClick: () -> Unit) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        tint = Amber,
        tintAlpha = 0.18f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = Amber)
            Text(
                text = stringResource(R.string.no_days_enabled_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = Amber,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── End-of-workout confirmation ─────────────────────────────────────────────

@Composable
private fun EndSessionSheet(
    session: com.hananel.workouttracker2.data.models.WorkoutSession,
    now: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val glass = rememberGlassColors()
    val elapsed = (now - session.startTime).coerceAtLeast(0L)

    GlassSheet(
        title = stringResource(R.string.end_workout_title),
        subtitle = session.dayName.ifBlank { null },
        icon = Icons.Default.CheckCircle,
        accent = Mint,
        onDismiss = onDismiss
    ) {
        // A receipt of what's about to be stored, so "did it save?" never comes up.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryTile(
                value = DateUtils.formatDuration(elapsed),
                label = stringResource(R.string.workout_duration),
                tint = Electric,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                value = "${session.totalSets}",
                label = stringResource(R.string.sets),
                tint = Mint,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                value = "${session.completedExerciseCount}/${
                    session.plannedExercises.coerceAtLeast(session.exercises.size)
                }",
                label = stringResource(R.string.exercises_completed),
                tint = Amber,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = stringResource(R.string.end_workout_message),
            style = MaterialTheme.typography.bodySmall,
            color = glass.onGlassMuted
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = stringResource(R.string.end_workout_confirm),
                icon = Icons.Default.CheckCircle,
                gradient = SuccessGradient,
                onClick = onConfirm,
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

@Composable
private fun SummaryTile(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    val glass = rememberGlassColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(glass.onGlass.copy(alpha = 0.05f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.onGlassMuted,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ─── Rename sheet ────────────────────────────────────────────────────────────

@Composable
private fun RenameDaySheet(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var dayName by remember { mutableStateOf(initialName) }

    GlassSheet(
        title = stringResource(R.string.rename_day),
        icon = Icons.Default.Edit,
        onDismiss = onDismiss
    ) {
        GlassTextField(
            value = dayName,
            onValueChange = { dayName = it },
            label = stringResource(R.string.workout_title),
            placeholder = stringResource(R.string.workout_title_hint)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = stringResource(R.string.save),
                onClick = { onSave(dayName) },
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}
