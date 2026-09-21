package com.hananel.workouttracker2.ui.screens

import com.hananel.workouttracker2.ui.rememberStorageScope

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.UserPreferences
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.ui.components.GhostButton
import com.hananel.workouttracker2.ui.components.GlassSheet
import com.hananel.workouttracker2.ui.components.GradientButton
import com.hananel.workouttracker2.ui.theme.AccentGradient
import com.hananel.workouttracker2.ui.theme.AppBackground
import com.hananel.workouttracker2.ui.theme.GlassCard
import com.hananel.workouttracker2.ui.theme.rememberGlassColors
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
    val scope = rememberStorageScope()
    
    var preferences by remember { mutableStateOf(UserPreferences()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showWorkoutBuilder by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        repository.getUserPreferencesFlow().collect { prefs ->
            preferences = prefs
        }
    }
    
    // The builder is drawn in place of this screen rather than as its own destination, so without
    // this the system back gesture popped Settings itself and threw the unsaved plan away.
    androidx.activity.compose.BackHandler(enabled = showWorkoutBuilder) { showWorkoutBuilder = false }

    // Show Workout Builder if requested
    if (showWorkoutBuilder) {
        WorkoutBuilderScreen(
            onFinish = { workoutDays ->
                scope.launch {
                    repository.saveWorkoutStructure(workoutDays)
                    val enabledDays = workoutDays.map { day -> day.dayIndex }.sorted()
                    repository.updateUserPreferences { it.copy(enabledDays = enabledDays) }
                    showWorkoutBuilder = false
                }
            },
            onBack = { showWorkoutBuilder = false },
            loadExistingData = true
        )
        return
    }
    
    val glass = rememberGlassColors()

    AppBackground {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
                        color = glass.onGlass
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = glass.onGlass
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
            // ── App Header Banner ─────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.workout_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.settings),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // Appearance Section — language + theme as inline segmented buttons (no more dialogs)
            item {
                SettingsSection(title = stringResource(R.string.general)) {
                    InlineSegmentedSetting(
                        icon = Icons.Outlined.Language,
                        iconColor = Color(0xFF3B82F6),
                        title = stringResource(R.string.language),
                        options = listOf("system", "he", "en"),
                        labels = listOf(
                            stringResource(R.string.language_system),
                            stringResource(R.string.language_hebrew),
                            stringResource(R.string.language_english)
                        ),
                        selected = preferences.language,
                        onSelect = { lang ->
                            scope.launch {
                                repository.updateUserPreferences { it.copy(language = lang) }
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                    InlineSegmentedSetting(
                        icon = Icons.Outlined.Palette,
                        iconColor = Color(0xFF8B5CF6),
                        title = stringResource(R.string.theme),
                        options = listOf("SYSTEM", "LIGHT", "DARK"),
                        labels = listOf(
                            stringResource(R.string.theme_system),
                            stringResource(R.string.theme_light),
                            stringResource(R.string.theme_dark)
                        ),
                        selected = preferences.theme,
                        onSelect = { theme ->
                            scope.launch {
                                repository.updateUserPreferences { it.copy(theme = theme) }
                            }
                        }
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
                                repository.updateUserPreferences { it.copy(notificationsEnabled = enabled) }
                                val helper = com.hananel.workouttracker2.utils.NotificationHelper(context)
                                repository.getUserPreferences().workoutAlarms.forEach { alarm ->
                                    if (enabled && alarm.isEnabled) helper.scheduleWorkoutReminder(alarm)
                                    else helper.cancelWorkoutReminder(alarm.id)
                                }
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
                                repository.updateUserPreferences { it.copy(autoResetWeekly = enabled) }
                            }
                        }
                    )
                }
            }
            
            // Workout Section
            item {
                SettingsSection(title = stringResource(R.string.workout)) {
                    SettingsItem(
                        icon = Icons.Outlined.CalendarMonth,
                        iconColor = Color(0xFF22C55E),
                        title = stringResource(R.string.workout_schedule_title),
                        subtitle = scheduleSubtitle(preferences.enabledDays),
                        onClick = { showScheduleSheet = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

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
                                repository.updateUserPreferences { it.copy(hasCompletedOnboarding = false) }
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
    }


    // Workout Schedule bottom sheet — pick which weekdays you train. Doesn't change the workout
    // structure itself (that's the workout builder); only the visible days in the day selector.
    if (showScheduleSheet) {
        WorkoutScheduleSheet(
            currentDays = preferences.enabledDays.toSet(),
            onDismiss = { showScheduleSheet = false },
            onSave = { newDays ->
                scope.launch {
                    repository.updateUserPreferences { it.copy(enabledDays = newDays.sorted()) }
                    showScheduleSheet = false
                }
            }
        )
    }

    if (showResetDialog) {
        GlassSheet(
            title = stringResource(R.string.reset_confirmation),
            icon = Icons.Outlined.RestartAlt,
            accent = MaterialTheme.colorScheme.error,
            onDismiss = { showResetDialog = false }
        ) {
            Text(
                text = stringResource(R.string.reset_confirmation_message),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.onGlassMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showResetDialog = false },
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = stringResource(R.string.confirm),
                    gradient = listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                    ),
                    onClick = {
                        onResetWeekly()
                        showResetDialog = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
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

/**
 * A settings row with the standard icon + title on the left, and a Material3 SegmentedButton on
 * the right (or below on narrow screens) for picking between a small fixed set of options. Used
 * for language and theme — replaces the old AlertDialog selectors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineSegmentedSetting(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    options: List<String>,
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    icon = {},
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun scheduleSubtitle(enabledDays: List<Int>): String {
    if (enabledDays.isEmpty()) return stringResource(R.string.workout_schedule_none)
    // Resolve each day's short string at the @Composable layer first; joinToString runs
    // a non-composable lambda so calling stringResource inside it would fail to compile.
    val shortIds = listOf(
        R.string.sunday_short, R.string.monday_short, R.string.tuesday_short,
        R.string.wednesday_short, R.string.thursday_short, R.string.friday_short,
        R.string.saturday_short
    )
    val resolved = shortIds.map { stringResource(it) }
    val names = enabledDays.sorted().joinToString(", ") { resolved[it] }
    return stringResource(R.string.workout_schedule_summary, enabledDays.size, names)
}

/**
 * Bottom sheet for picking which weekdays the user trains. Lets the user explicitly say "I train
 * Sun/Tue/Thu" without going through the full workout builder. Saves to UserPreferences.enabledDays
 * which the day selector at the top of the workout screen reads from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutScheduleSheet(
    currentDays: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit
) {
    var selected by remember { mutableStateOf(currentDays) }
    val glass = rememberGlassColors()

    val dayLabels = listOf(
        stringResource(R.string.day_sunday),
        stringResource(R.string.day_monday),
        stringResource(R.string.day_tuesday),
        stringResource(R.string.day_wednesday),
        stringResource(R.string.day_thursday),
        stringResource(R.string.day_friday),
        stringResource(R.string.day_saturday)
    )

    GlassSheet(
        title = stringResource(R.string.workout_schedule_title),
        subtitle = stringResource(R.string.workout_schedule_hint),
        icon = Icons.Outlined.CalendarMonth,
        accent = Color(0xFF22C55E),
        onDismiss = onDismiss
    ) {
            // Quick presets so users don't have to tap 4 times for the common cases.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    stringResource(R.string.preset_3day_odd) to setOf(0, 2, 4),  // Sun, Tue, Thu
                    stringResource(R.string.preset_3day_even) to setOf(1, 3, 5), // Mon, Wed, Fri
                    stringResource(R.string.preset_4day) to setOf(0, 1, 3, 4),
                    stringResource(R.string.preset_5day) to setOf(0, 1, 2, 3, 4)
                )
                presets.forEach { (label, days) ->
                    AssistChip(
                        onClick = { selected = days },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Per-day toggles with full names so the user can't be confused.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dayLabels.forEachIndexed { index, label ->
                    val checked = selected.contains(index)
                    Surface(
                        onClick = {
                            selected = if (checked) selected - index else selected + index
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else glass.onGlass.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                                color = if (checked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selected = if (checked) selected - index else selected + index
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GhostButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = stringResource(R.string.save),
                    onClick = { onSave(selected) },
                    enabled = selected.isNotEmpty(),
                    gradient = AccentGradient,
                    modifier = Modifier.weight(1.4f)
                )
            }
    }
}
