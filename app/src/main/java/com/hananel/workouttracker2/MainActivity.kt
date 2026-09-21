package com.hananel.workouttracker2

import com.hananel.workouttracker2.ui.rememberStorageScope

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hananel.workouttracker2.data.models.ThemeMode
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.ui.screens.AlarmsScreen
import com.hananel.workouttracker2.ui.screens.HistoryScreen
import com.hananel.workouttracker2.ui.screens.OnboardingScreen
import com.hananel.workouttracker2.ui.screens.SettingsScreen
import com.hananel.workouttracker2.ui.screens.WorkoutBuilderScreen
import com.hananel.workouttracker2.ui.screens.WorkoutScreen
import com.hananel.workouttracker2.ui.screens.WorkoutViewModel
import com.hananel.workouttracker2.ui.screens.WorkoutViewModelFactory
import com.hananel.workouttracker2.ui.theme.WorkoutTrackerTheme
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val isGranted = entry.value
            // Log or handle each permission
        }
    }
    
    // State to signal when notification wants to open timer dialog
    private val _shouldOpenTimerDialog = mutableStateOf(false)
    val shouldOpenTimerDialog: State<Boolean> = _shouldOpenTimerDialog
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if we should open timer dialog from notification
        _shouldOpenTimerDialog.value = intent?.getBooleanExtra("OPEN_TIMER_DIALOG", false) ?: false
        
        // Create notification channels
        NotificationHelper(this).createNotificationChannels()
        
        // Request permissions if needed
        requestNecessaryPermissions()
        
        enableEdgeToEdge()
        
        setContent {
            WorkoutTrackerApp(
                shouldOpenTimerDialog = shouldOpenTimerDialog.value,
                onTimerDialogOpened = { _shouldOpenTimerDialog.value = false }
            )
        }
    }

    override fun onNewIntent(newIntent: android.content.Intent) {
        super.onNewIntent(newIntent)
        // Handle new intent when app is already running (singleTop preserves Activity + remember state)
        _shouldOpenTimerDialog.value = newIntent.getBooleanExtra("OPEN_TIMER_DIALOG", false)
    }
    
    private fun requestNecessaryPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // POST_NOTIFICATIONS for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // SCHEDULE_EXACT_ALARM for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Note: SCHEDULE_EXACT_ALARM doesn't need runtime request, just manifest declaration
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    

}

@Composable
fun WorkoutTrackerApp(
    shouldOpenTimerDialog: Boolean = false,
    onTimerDialogOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }

    var preferences by remember { mutableStateOf<com.hananel.workouttracker2.data.models.UserPreferences?>(null) }

    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        loadFailed = false
        try {
            repository.getUserPreferencesFlow().collect { prefs -> preferences = prefs }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { loadFailed = true }
    }
    LaunchedEffect(preferences?.language) {
        val language = preferences?.language ?: return@LaunchedEffect
        val locales = android.os.LocaleList.forLanguageTags(if (language == "system") "" else language)
        val manager = context.getSystemService(android.app.LocaleManager::class.java)
        if (manager.applicationLocales != locales) manager.applicationLocales = locales
    }

    val themeMode = preferences?.getThemeMode() ?: ThemeMode.SYSTEM
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    WorkoutTrackerTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (loadFailed) {
                DataErrorScreen(onRetry = { attempt++ })
            } else if (preferences == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else AppNavigation(
                shouldOpenTimerDialog = shouldOpenTimerDialog,
                onTimerDialogOpened = onTimerDialogOpened
            )
        }
    }
}

@Composable
fun AppNavigation(
    shouldOpenTimerDialog: Boolean = false,
    onTimerDialogOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberStorageScope()
    val repository = remember { WorkoutRepository(context) }
    
    // One ViewModel for the whole graph: the workout screen and history share the session flows,
    // and the live session clock must not restart when the user navigates.
    val sharedViewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModelFactory(context))
    val failed by sharedViewModel.error.collectAsState()
    val editFailed by com.hananel.workouttracker2.ui.StorageFailure.failed.collectAsState()
    if (failed || editFailed) {
        DataErrorScreen(onRetry = { com.hananel.workouttracker2.ui.StorageFailure.failed.value = false; sharedViewModel.retry() })
        return
    }

    var hasCompletedOnboarding by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val prefs = repository.getUserPreferences()
        hasCompletedOnboarding = prefs.hasCompletedOnboarding
    }
    
    if (hasCompletedOnboarding == null) {
        // Sits on the app backdrop, not a bare Surface — otherwise every cold start flashes a
        // black rectangle before the first screen paints.
        com.hananel.workouttracker2.ui.theme.AppBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }
    
    NavHost(
        navController = navController,
        startDestination = if (hasCompletedOnboarding == true) "workout" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    scope.launch {
                        val prefs = repository.getUserPreferences()
                        repository.updateUserPreferences { it.copy(hasCompletedOnboarding = true) }
                        hasCompletedOnboarding = true
                        navController.navigate("workout") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("workout") {
            WorkoutScreen(
                viewModel = sharedViewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAlarms = {
                    navController.navigate("alarms")
                },
                onNavigateToCatalog = { dayId ->
                    navController.navigate("exerciseCatalog/$dayId")
                },
                onNavigateToWorkoutBuilder = {
                    navController.navigate("workoutBuilder")
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                },
                shouldOpenTimerDialog = shouldOpenTimerDialog,
                onTimerDialogOpened = onTimerDialogOpened
            )
        }

        composable("history") {
            // Same ViewModel instance as the workout screen — history reads the session flows it
            // already collects, so there's nothing to re-load on navigation.
            val sessions by sharedViewModel.sessionHistory.collectAsState()
            HistoryScreen(
                sessions = sessions,
                onNavigateBack = { navController.popBackStack() },
                onDeleteSession = { sharedViewModel.deleteSession(it) }
            )
        }

        composable("exerciseCatalog/{dayId}") { backStackEntry ->
            val dayId = backStackEntry.arguments?.getString("dayId") ?: ""

            com.hananel.workouttracker2.ui.screens.ExerciseCatalogScreen(
                dayId = dayId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExerciseSelected = { bankItem ->
                    sharedViewModel.addExerciseFromBank(dayId, bankItem)
                    navController.popBackStack()
                }
            )
        }
        
        composable("alarms") {
            val repository = remember { WorkoutRepository(context) }
            
            AlarmsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings") {
            val repository = remember { WorkoutRepository(context) }
            
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetWeekly = {
                    scope.launch {
                        repository.resetWeeklyProgress()
                    }
                }
            )
        }
        
        composable("workoutBuilder") {
            val repository = remember { WorkoutRepository(context) }
            
            WorkoutBuilderScreen(
                onFinish = { workoutDays ->
                    scope.launch {
                        repository.saveWorkoutStructure(workoutDays)
                        // Update enabledDays based on selected days
                        val enabledDays = workoutDays.map { it.dayIndex }.sorted()
                        val prefs = repository.getUserPreferences()
                        repository.updateUserPreferences { it.copy(enabledDays = enabledDays) }
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                loadExistingData = true
            )
        }
    }
}

@Composable
private fun DataErrorScreen(onRetry: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().then(Modifier.padding(24.dp)),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.data_load_error))
        androidx.compose.material3.Button(onClick = onRetry) {
            androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.retry_loading))
        }
    }
}
