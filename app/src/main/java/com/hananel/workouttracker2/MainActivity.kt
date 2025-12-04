package com.hananel.workouttracker2

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
import com.hananel.workouttracker2.ui.screens.SettingsScreen
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create notification channels
        NotificationHelper(this).createNotificationChannels()
        
        // Request permissions if needed
        requestNecessaryPermissions()
        
        enableEdgeToEdge()
        
        setContent {
            WorkoutTrackerApp()
        }
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
    
    override fun attachBaseContext(newBase: Context) {
        // Apply language preference
        val repository = WorkoutRepository(newBase)
        val preferences = kotlinx.coroutines.runBlocking {
            repository.getUserPreferences()
        }
        
        val locale = when (preferences.language) {
            "he" -> Locale("he")
            "en" -> Locale("en")
            else -> Locale.getDefault()
        }
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}

@Composable
fun WorkoutTrackerApp() {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    val scope = rememberCoroutineScope()
    
    var preferences by remember { mutableStateOf<com.hananel.workouttracker2.data.models.UserPreferences?>(null) }
    
    LaunchedEffect(Unit) {
        repository.getUserPreferencesFlow().collect { prefs ->
            preferences = prefs
        }
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
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    NavHost(
        navController = navController,
        startDestination = "workout"
    ) {
        composable("workout") {
            val viewModel: WorkoutViewModel = remember {
                WorkoutViewModelFactory(context).create()
            }
            val repository = remember { WorkoutRepository(context) }
            
            WorkoutScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAlarms = {
                    navController.navigate("alarms")
                },
                onResetWeekly = {
                    scope.launch {
                        repository.resetWeeklyProgress()
                    }
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
    }
}
