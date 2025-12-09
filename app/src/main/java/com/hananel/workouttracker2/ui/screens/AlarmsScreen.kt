package com.hananel.workouttracker2.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { 
                    Text(
                        stringResource(R.string.workout_alarms),
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_alarm), fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White
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
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = Color(0xFFEF4444)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.no_alarms),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.no_alarms_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        
        // Add/Edit Dialog
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
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { 
                    Text(
                        stringResource(R.string.delete_alarm),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(stringResource(R.string.delete_alarm_confirmation)) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val prefs = repository.getUserPreferencesFlow().first()
                                val updatedAlarms = prefs.workoutAlarms.filter { it.id != alarm.id }
                                repository.updateUserPreferences(prefs.copy(workoutAlarms = updatedAlarms))
                                notificationHelper.cancelWorkoutReminder(alarm.id)
                                alarmToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { alarmToDelete = null }) {
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Alarm icon with background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (alarm.isEnabled) {
                                    Color(0xFFEF4444).copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = if (alarm.isEnabled) {
                                Color(0xFFEF4444)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Time
                    Column {
                        Text(
                            text = alarm.time,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (alarm.customMessage.isNotEmpty()) {
                            Text(
                                text = alarm.customMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Toggle and Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_alarm),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFFEF4444),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Days row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "א" to 0, "ב" to 1, "ג" to 2, "ד" to 3, "ה" to 4, "ו" to 5, "ש" to 6
                ).forEach { (day, index) ->
                    DayChip(
                        day = day,
                        isSelected = alarm.daysOfWeek.contains(index),
                        isEnabled = alarm.isEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
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
    Surface(
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = when {
            isSelected && isEnabled -> Color(0xFFEF4444)
            isSelected -> Color(0xFFEF4444).copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected && isEnabled -> Color.White
                    isSelected -> Color(0xFFEF4444)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
                Text(
                    text = if (alarm != null) {
                        stringResource(R.string.edit_alarm)
                    } else {
                        stringResource(R.string.add_alarm)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Time Picker Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
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
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.alarm_time),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = selectedTime,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Days Selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.select_days),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "א" to 0, "ב" to 1, "ג" to 2, "ד" to 3, "ה" to 4, "ו" to 5, "ש" to 6
                        ).forEach { (day, index) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedDays = if (selectedDays.contains(index)) {
                                            selectedDays - index
                                        } else {
                                            selectedDays + index
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedDays.contains(index)) {
                                    Color(0xFFEF4444)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDays.contains(index)) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
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
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
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
                enabled = selectedDays.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
