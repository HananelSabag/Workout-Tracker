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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = Color(0xFFEF4444)

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = if (alarm != null) stringResource(R.string.edit_alarm)
                    else stringResource(R.string.add_alarm),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Section: When ────────────────────────────────────────────────
            SheetSectionLabel(stringResource(R.string.alarm_time))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
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
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.select_time),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                        Text(
                            text = selectedTime,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // ── Section: Days ────────────────────────────────────────────────
            SheetSectionLabel(stringResource(R.string.select_days))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "א" to 0, "ב" to 1, "ג" to 2, "ד" to 3, "ה" to 4, "ו" to 5, "ש" to 6
                ).forEach { (day, index) ->
                    val selected = selectedDays.contains(index)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedDays = if (selected) selectedDays - index
                                else selectedDays + index
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Section: Custom message ──────────────────────────────────────
            SheetSectionLabel(stringResource(R.string.custom_message))
            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                placeholder = { Text(stringResource(R.string.notification_workout_reminder_message)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp)
            )

            // ── Live preview line ────────────────────────────────────────────
            if (selectedDays.isNotEmpty()) {
                val dayNames = listOf("א'", "ב'", "ג'", "ד'", "ה'", "ו'", "ש'")
                val daysSummary = selectedDays.sorted().joinToString(separator = ", ") { dayNames[it] }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "$daysSummary · $selectedTime",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Actions ──────────────────────────────────────────────────────
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
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
