package com.hananel.workouttracker2.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.ThemeMode
import com.hananel.workouttracker2.data.models.UserPreferences
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import kotlinx.coroutines.launch
import java.util.*

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
    
    LaunchedEffect(Unit) {
        repository.getUserPreferencesFlow().collect { prefs ->
            preferences = prefs
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Language Section
            item {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.language))
                        Text(
                            text = getLanguageDisplayName(preferences.language),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Theme Section
            item {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp).padding(top = 16.dp)
                )
            }
            
            item {
                Card(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.theme))
                        Text(
                            text = getThemeDisplayName(preferences.theme),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // General Section
            item {
                Text(
                    text = stringResource(R.string.general),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp).padding(top = 16.dp)
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.auto_open_current_day))
                            Text(
                                text = stringResource(R.string.auto_open_current_day_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.autoOpenCurrentDay,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    repository.updateAutoOpenCurrentDay(enabled)
                                }
                            }
                        )
                    }
                }
            }
            
            // Notifications Section
            item {
                Text(
                    text = stringResource(R.string.notifications),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp).padding(top = 16.dp)
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.notifications))
                        Switch(
                            checked = preferences.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    repository.saveUserPreferences(
                                        preferences.copy(notificationsEnabled = enabled)
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.auto_reset_weekly))
                            Text(
                                text = stringResource(R.string.auto_reset_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
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
            }
            
            // Reset Section
            item {
                Text(
                    text = stringResource(R.string.manual_reset),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp).padding(top = 16.dp)
                )
            }
            
            item {
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.reset_week))
                }
            }
            
            // Footer - Created by Hananel Sabag
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.created_by),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.developer_name),
                        style = MaterialTheme.typography.labelLarge,
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
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    listOf("system", "he", "en").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.language == lang,
                                onClick = {
                                    scope.launch {
                                        repository.saveUserPreferences(
                                            preferences.copy(language = lang)
                                        )
                                        showLanguageDialog = false
                                        // Restart activity to apply language
                                        (context as? Activity)?.recreate()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getLanguageDisplayName(lang))
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
            title = { Text(stringResource(R.string.theme)) },
            text = {
                Column {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.theme == theme,
                                onClick = {
                                    scope.launch {
                                        repository.saveUserPreferences(
                                            preferences.copy(theme = theme)
                                        )
                                        showThemeDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getThemeDisplayName(theme))
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

