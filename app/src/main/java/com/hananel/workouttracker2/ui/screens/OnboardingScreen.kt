package com.hananel.workouttracker2.ui.screens

import com.hananel.workouttracker2.ui.rememberStorageScope

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.hananel.workouttracker2.data.models.getDisplayName
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
    val scope = rememberStorageScope()
    
    com.hananel.workouttracker2.ui.theme.AppBackground(modifier = modifier) {
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

    // rememberSaveable so a config change (rotation, language switch) doesn't trigger a second
    // permission popup behind the user's back.
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
        hasRequestedPermission = true
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val currentStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationPermissionGranted = currentStatus
            if (!currentStatus && !hasRequestedPermission) {
                kotlinx.coroutines.delay(500)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Gradient hero ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED))
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(86.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.workout_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.onboarding_page1_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Features + permission + CTA ───────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.onboarding_features_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                OnboardingFeatureCard(
                    icon = Icons.Default.FitnessCenter,
                    iconColor = Color(0xFF3B82F6),
                    text = stringResource(R.string.onboarding_feature_1)
                )
            }
            item {
                OnboardingFeatureCard(
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFFF59E0B),
                    text = stringResource(R.string.onboarding_feature_2)
                )
            }
            item {
                OnboardingFeatureCard(
                    icon = Icons.Default.List,
                    iconColor = Color(0xFF10B981),
                    text = stringResource(R.string.onboarding_feature_3)
                )
            }
            item {
                OnboardingFeatureCard(
                    icon = Icons.Default.Language,
                    iconColor = Color(0xFF8B5CF6),
                    text = stringResource(R.string.onboarding_feature_4)
                )
            }

            // Notification permission card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        // Translucent tint rather than a hardcoded light-green fill — the solid
                        // colour left the title unreadable in dark mode.
                        containerColor = if (notificationPermissionGranted)
                            Color(0xFF10B981).copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (notificationPermissionGranted)
                                            Color(0xFF10B981).copy(alpha = 0.15f)
                                        else Color(0xFFEF4444).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (notificationPermissionGranted)
                                        Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.enable_notifications_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (notificationPermissionGranted)
                                        stringResource(R.string.notifications_granted)
                                    else
                                        stringResource(R.string.enable_notifications_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (notificationPermissionGranted)
                                        Color(0xFF10B981)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (notificationPermissionGranted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFF10B981)
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            FilledTonalButton(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.allow_notifications),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // CTA button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector,
    iconColor: Color,
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = iconColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OnboardingPage2(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    val scope = rememberStorageScope()
    
    // State for custom workout builder
    var showWorkoutBuilder by remember { mutableStateOf(false) }

    // Onboarding also opens from Settings → Help over a real plan, and either card replaces that
    // plan — so a returning user is asked first. A first install goes straight through.
    var pendingChoice by remember { mutableStateOf<ProgramChoice?>(null) }
    suspend fun installDefault() {
        // Same reset as the builder path below — never wipes weights.
        repository.resetWeeklyProgress()
        repository.saveWorkoutStructure(DefaultData.getDefaultWorkoutStructure())
        onFinish()
    }
    fun choose(choice: ProgramChoice) {
        scope.launch {
            // Read at tap time rather than on first composition, so a quick tap can't slip past.
            val hasProgram = repository.getWorkoutStructure().any { it.exercises.isNotEmpty() }
            when {
                hasProgram -> pendingChoice = choice
                choice == ProgramChoice.DEFAULT -> installDefault()
                else -> showWorkoutBuilder = true
            }
        }
    }

    // Drawn in place, not navigated to: system back must close it like its own back arrow does.
    androidx.activity.compose.BackHandler(enabled = showWorkoutBuilder) { showWorkoutBuilder = false }
    
    if (showWorkoutBuilder) {
        WorkoutBuilderScreen(
            onFinish = { workoutDays ->
                scope.launch {
                    // Clears checkmarks, set counts and any timer a cloud restore brought back, but
                    // keeps weights and notes and files a running workout into history. Onboarding is
                    // also reachable from Settings → Help, so this runs for users with real data.
                    repository.resetWeeklyProgress()
                    
                    repository.saveWorkoutStructure(workoutDays)
                    // Update enabledDays based on selected days
                    val enabledDays = workoutDays.map { it.dayIndex }.sorted()
                    val prefs = repository.getUserPreferences()
                    repository.updateUserPreferences { it.copy(enabledDays = enabledDays) }
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
                
                // Default Program Option — emphasized as the recommended path with a primary
                // tint and a "Recommended" badge so first-time users default to a working setup.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { choose(ProgramChoice.DEFAULT) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = stringResource(R.string.recommended),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.use_default_program),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.use_default_program_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Customize Option — secondary visual weight (outlined)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { choose(ProgramChoice.CUSTOM) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
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
                    // Skipping setup preserves all existing plans, including custom exercises.
                    onFinish()
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

    pendingChoice?.let { choice ->
        AlertDialog(
            onDismissRequest = { pendingChoice = null },
            title = { Text(stringResource(R.string.replace_program_title)) },
            text = { Text(stringResource(R.string.replace_program_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingChoice = null
                        if (choice == ProgramChoice.DEFAULT) scope.launch { installDefault() }
                        else showWorkoutBuilder = true
                    }
                ) {
                    Text(
                        stringResource(R.string.replace_program_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingChoice = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private enum class ProgramChoice { DEFAULT, CUSTOM }

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
        val displayName = if (day.customName.isNotBlank()) day.customName else day.getDisplayName(context)
        
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
    val dayName = day.getDisplayName(context)
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
                        text = exercise.getDisplayName(context),
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
