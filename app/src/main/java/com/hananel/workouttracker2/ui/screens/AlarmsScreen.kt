package com.hananel.workouttracker2.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.WorkoutAlarm
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { WorkoutRepository(context) }
    val notificationHelper = remember { NotificationHelper(context) }
    
    var alarms by remember { mutableStateOf<List<WorkoutAlarm>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<WorkoutAlarm?>(null) }
    var alarmToDelete by remember { mutableStateOf<WorkoutAlarm?>(null) }
    
    // Load alarms
    LaunchedEffect(Unit) {
        scope.launch {
            repository.getUserPreferencesFlow().collect { prefs ->
                alarms = prefs.workoutAlarms
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_alarms)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_alarm)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (alarms.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_alarms),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_alarms_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Alarms list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { enabled ->
                            scope.launch {
                                val prefs = repository.getUserPreferencesFlow().first()
                                val updatedAlarms = prefs.workoutAlarms.map {
                                    if (it.id == alarm.id) it.copy(isEnabled = enabled) else it
                                }
                                repository.updateUserPreferences(prefs.copy(workoutAlarms = updatedAlarms))
                                
                                if (enabled) {
                                    notificationHelper.scheduleWorkoutReminder(alarm)
                                } else {
                                    notificationHelper.cancelWorkoutReminder(alarm.id)
                                }
                            }
                        },
                        onEdit = { alarmToEdit = alarm },
                        onDelete = { alarmToDelete = alarm }
                    )
                }
            }
        }
        
        // Add/Edit Alarm Dialog
        if (showAddDialog || alarmToEdit != null) {
            AlarmDialog(
                alarm = alarmToEdit,
                onDismiss = {
                    showAddDialog = false
                    alarmToEdit = null
                },
                onSave = { alarm ->
                    scope.launch {
                        val prefs = repository.getUserPreferencesFlow().first()
                        val updatedAlarms = if (alarmToEdit != null) {
                            prefs.workoutAlarms.map { if (it.id == alarm.id) alarm else it }
                        } else {
                            prefs.workoutAlarms + alarm
                        }
                        repository.updateUserPreferences(prefs.copy(workoutAlarms = updatedAlarms))
                        
                        if (alarm.isEnabled) {
                            notificationHelper.scheduleWorkoutReminder(alarm)
                        }
                        
                        showAddDialog = false
                        alarmToEdit = null
                    }
                }
            )
        }
        
        // Delete Confirmation Dialog
        alarmToDelete?.let { alarm ->
            AlertDialog(
                onDismissRequest = { alarmToDelete = null },
                title = { Text(stringResource(R.string.delete_alarm)) },
                text = { Text(stringResource(R.string.delete_alarm_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val prefs = repository.getUserPreferencesFlow().first()
                                val updatedAlarms = prefs.workoutAlarms.filter { it.id != alarm.id }
                                repository.updateUserPreferences(prefs.copy(workoutAlarms = updatedAlarms))
                                notificationHelper.cancelWorkoutReminder(alarm.id)
                                alarmToDelete = null
                            }
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { alarmToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: WorkoutAlarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Toggle and Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_alarm),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("א", "ב", "ג", "ד", "ה", "ו", "ש").forEachIndexed { index, day ->
                    DayChip(
                        day = day,
                        isSelected = alarm.daysOfWeek.contains(index),
                        isEnabled = alarm.isEnabled
                    )
                }
            }
            
            // Custom message
            if (alarm.customMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alarm.customMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DayChip(
    day: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .background(
                color = when {
                    isSelected && isEnabled -> MaterialTheme.colorScheme.primary
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected && isEnabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun AlarmDialog(
    alarm: WorkoutAlarm?,
    onDismiss: () -> Unit,
    onSave: (WorkoutAlarm) -> Unit
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(alarm?.time ?: "08:00") }
    var selectedDays by remember { mutableStateOf(alarm?.daysOfWeek ?: emptySet()) }
    var customMessage by remember { mutableStateOf(alarm?.customMessage ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (alarm != null) {
                    stringResource(R.string.edit_alarm)
                } else {
                    stringResource(R.string.add_alarm)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Picker
                OutlinedButton(
                    onClick = {
                        val timeParts = selectedTime.split(":")
                        val hour = timeParts[0].toInt()
                        val minute = timeParts[1].toInt()
                        
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                            },
                            hour,
                            minute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.alarm_time),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = selectedTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Days Selection
                Column {
                    Text(
                        text = stringResource(R.string.select_days) + " (${selectedDays.size}/7)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.select_days_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to stringResource(R.string.day_sunday),
                            1 to stringResource(R.string.day_monday),
                            2 to stringResource(R.string.day_tuesday),
                            3 to stringResource(R.string.day_wednesday),
                            4 to stringResource(R.string.day_thursday),
                            5 to stringResource(R.string.day_friday),
                            6 to stringResource(R.string.day_saturday)
                        ).forEach { (index, dayName) ->
                            FilterChip(
                                selected = selectedDays.contains(index),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(index)) {
                                        selectedDays - index
                                    } else {
                                        selectedDays + index
                                    }
                                },
                                label = { 
                                    Text(
                                        dayName,
                                        modifier = Modifier.fillMaxWidth()
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Custom Message
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text(stringResource(R.string.custom_message)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newAlarm = WorkoutAlarm(
                        id = alarm?.id ?: UUID.randomUUID().toString(),
                        time = selectedTime,
                        daysOfWeek = selectedDays,
                        isEnabled = alarm?.isEnabled ?: true,
                        customMessage = customMessage
                    )
                    onSave(newAlarm)
                },
                enabled = selectedDays.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

