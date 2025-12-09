package com.hananel.workouttracker2.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.UserPreferences
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onResetWeekly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    val scope = rememberCoroutineScope()
    
    var preferences by remember { mutableStateOf(UserPreferences()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWorkoutBuilder by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        repository.getUserPreferencesFlow().collect { prefs ->
            preferences = prefs
        }
    }
    
    // Show Workout Builder if requested
    if (showWorkoutBuilder) {
        WorkoutBuilderScreen(
            onFinish = { workoutDays ->
                scope.launch {
                    repository.saveWorkoutStructure(workoutDays)
                    val enabledDays = workoutDays.map { day -> day.dayIndex }.sorted()
                    repository.saveUserPreferences(preferences.copy(enabledDays = enabledDays))
                    showWorkoutBuilder = false
                }
            },
            onBack = { showWorkoutBuilder = false },
            loadExistingData = true
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            item {
                SettingsSection(title = stringResource(R.string.general)) {
                    SettingsItem(
                        icon = Icons.Outlined.Language,
                        iconColor = Color(0xFF3B82F6),
                        title = stringResource(R.string.language),
                        subtitle = getLanguageDisplayName(preferences.language),
                        onClick = { showLanguageDialog = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        iconColor = Color(0xFF8B5CF6),
                        title = stringResource(R.string.theme),
                        subtitle = getThemeDisplayName(preferences.theme),
                        onClick = { showThemeDialog = true }
                    )
                }
            }
            
            // Behavior Section
            item {
                SettingsSection(title = stringResource(R.string.settings)) {
                    SettingsToggleItem(
                        icon = Icons.Outlined.Today,
                        iconColor = Color(0xFF10B981),
                        title = stringResource(R.string.auto_open_current_day),
                        subtitle = stringResource(R.string.auto_open_current_day_desc),
                        checked = preferences.autoOpenCurrentDay,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                repository.updateAutoOpenCurrentDay(enabled)
                            }
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Outlined.Notifications,
                        iconColor = Color(0xFFEF4444),
                        title = stringResource(R.string.notifications),
                        subtitle = null,
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                repository.saveUserPreferences(
                                    preferences.copy(notificationsEnabled = enabled)
                                )
                            }
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Outlined.Autorenew,
                        iconColor = Color(0xFFF59E0B),
                        title = stringResource(R.string.auto_reset_weekly),
                        subtitle = stringResource(R.string.auto_reset_description),
                        checked = preferences.autoResetWeekly,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                repository.saveUserPreferences(
                                    preferences.copy(autoResetWeekly = enabled)
                                )
                            }
                        }
                    )
                }
            }
            
            // Workout Section
            item {
                SettingsSection(title = stringResource(R.string.workout)) {
                    SettingsItem(
                        icon = Icons.Outlined.FitnessCenter,
                        iconColor = Color(0xFF06B6D4),
                        title = stringResource(R.string.manage_workout_builder),
                        subtitle = stringResource(R.string.manage_days_desc),
                        onClick = { showWorkoutBuilder = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    
                    SettingsItem(
                        icon = Icons.Outlined.RestartAlt,
                        iconColor = Color(0xFFEF4444),
                        title = stringResource(R.string.reset_week),
                        subtitle = stringResource(R.string.reset_confirmation_message),
                        onClick = { showResetDialog = true }
                    )
                }
            }
            
            // Help & About Section
            item {
                SettingsSection(title = stringResource(R.string.help)) {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        iconColor = Color(0xFF6366F1),
                        title = stringResource(R.string.reopen_onboarding),
                        subtitle = null,
                        onClick = {
                            scope.launch {
                                repository.saveUserPreferences(
                                    preferences.copy(hasCompletedOnboarding = false)
                                )
                                (context as? Activity)?.let { activity ->
                                    activity.finish()
                                    activity.startActivity(Intent(activity, activity::class.java))
                                }
                            }
                        }
                    )
                }
            }
            
            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.created_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.developer_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { 
                Text(
                    stringResource(R.string.language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("system", "he", "en").forEach { lang ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        repository.saveUserPreferences(
                                            preferences.copy(language = lang)
                                        )
                                        showLanguageDialog = false
                                        (context as? Activity)?.recreate()
                                    }
                                },
                            color = if (preferences.language == lang) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getLanguageDisplayName(lang),
                                    fontWeight = if (preferences.language == lang) FontWeight.Bold else FontWeight.Normal
                                )
                                if (preferences.language == lang) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { 
                Text(
                    stringResource(R.string.theme),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { theme ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        repository.saveUserPreferences(
                                            preferences.copy(theme = theme)
                                        )
                                        showThemeDialog = false
                                    }
                                },
                            color = if (preferences.theme == theme) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp)
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when(theme) {
                                            "LIGHT" -> Icons.Outlined.LightMode
                                            "DARK" -> Icons.Outlined.DarkMode
                                            else -> Icons.Outlined.Brightness6
                                        },
                                        contentDescription = null,
                                        tint = if (preferences.theme == theme) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = getThemeDisplayName(theme),
                                        fontWeight = if (preferences.theme == theme) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (preferences.theme == theme) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { 
                Text(
                    stringResource(R.string.reset_confirmation),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.reset_confirmation_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetWeekly()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun getLanguageDisplayName(lang: String): String {
    return when (lang) {
        "he" -> stringResource(R.string.language_hebrew)
        "en" -> stringResource(R.string.language_english)
        else -> stringResource(R.string.language_system)
    }
}

@Composable
private fun getThemeDisplayName(theme: String): String {
    return when (theme) {
        "LIGHT" -> stringResource(R.string.theme_light)
        "DARK" -> stringResource(R.string.theme_dark)
        else -> stringResource(R.string.theme_system)
    }
}
