# WorkoutTracker Upgrade - Implementation Blueprint

**Author:** Hananel Sabag  
**Date:** December 4, 2025  
**Version:** 2.0 Upgrade Plan

---

## Overview

Transform WorkoutTracker from a personal app (built for brother) into a production-ready, user-friendly fitness application suitable for all users.

**Current State:** Stable v1.0 with excellent architecture, clean code, working timer, and solid data persistence.

**Target State:** Generalized, configurable app with onboarding, exercise bank, enhanced notifications, and flexible workout customization.

---

## Plan

### Phase 1: Auto-Open Current Day Toggle (Feature #1)

#### 1.1 Data Model Changes

**File:** `app/src/main/java/com/hananel/workouttracker2/data/models/UserPreferences.kt`

```kotlin
@Serializable
data class UserPreferences(
    val language: String = "system",
    val theme: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val workoutAlarms: List<WorkoutAlarm> = emptyList(),
    val autoResetWeekly: Boolean = false,
    val enabledDays: List<Int> = listOf(0, 1, 2, 3, 4),
    // NEW:
    val autoOpenCurrentDay: Boolean = true,  // Default: enabled (current behavior)
    val lastViewedDayIndex: Int = 0           // Persist last viewed day
)
```

#### 1.2 Repository Changes

**File:** `app/src/main/java/com/hananel/workouttracker2/data/repository/WorkoutRepository.kt`

Add methods:
- `suspend fun updateAutoOpenCurrentDay(enabled: Boolean)`
- `suspend fun updateLastViewedDay(dayIndex: Int)`

#### 1.3 ViewModel Changes

**File:** `app/src/main/java/com/hananel/workouttracker2/ui/screens/WorkoutViewModel.kt`

Update `init` block:
```kotlin
init {
    loadData()
    // Set initial day based on preferences
    viewModelScope.launch {
        val prefs = repository.getUserPreferences()
        _selectedDayIndex.value = if (prefs.autoOpenCurrentDay) {
            DateUtils.getCurrentDayIndex()
        } else {
            prefs.lastViewedDayIndex
        }
    }
}
```

Add method:
```kotlin
fun selectDay(dayIndex: Int) {
    _selectedDayIndex.value = dayIndex
    viewModelScope.launch {
        repository.updateLastViewedDay(dayIndex)
    }
}
```

#### 1.4 UI Changes

**File:** `app/src/main/java/com/hananel/workouttracker2/ui/screens/SettingsScreen.kt`

Add toggle switch:
```kotlin
// Auto-open current day toggle
SettingsToggleItem(
    title = stringResource(R.string.auto_open_current_day),
    subtitle = stringResource(R.string.auto_open_current_day_desc),
    checked = autoOpenCurrentDay,
    onCheckedChange = { enabled ->
        onAutoOpenCurrentDayChange(enabled)
    }
)
```

#### 1.5 Strings (i18n)

**Hebrew (`values/strings.xml`):**
```xml
<string name="auto_open_current_day">פתח אוטומטית ליום הנוכחי</string>
<string name="auto_open_current_day_desc">בפתיחת האפליקציה, עבור לאימון של היום</string>
```

**English (`values-en/strings.xml`):**
```xml
<string name="auto_open_current_day">Auto-open current day</string>
<string name="auto_open_current_day_desc">When opening the app, jump to today\'s workout</string>
```

---

### Phase 2: Exercise Bank System (Feature #2)

#### 2.1 Create ExerciseBank Data Model

**New File:** `app/src/main/java/com/hananel/workouttracker2/data/models/ExerciseBank.kt`

```kotlin
@Serializable
data class ExerciseBankItem(
    val id: String,
    val nameResId: Int,
    val muscleGroups: List<String>,  // e.g., "Chest", "Back", "Shoulders"
    val equipment: String,            // e.g., "Dumbbells", "Cable", "Machine"
    val category: ExerciseCategory,   // Anaerobic, Aerobic
    val defaultSets: Int = 4,
    val defaultReps: String = "10-12",
    val defaultRestTime: Int = 60,
    val hasWeight: Boolean = true
)

enum class ExerciseCategory {
    ANAEROBIC, AEROBIC
}

enum class MuscleGroup(val displayNameResId: Int) {
    CHEST(R.string.muscle_chest),
    BACK(R.string.muscle_back),
    SHOULDERS(R.string.muscle_shoulders),
    ARMS(R.string.muscle_arms),
    LEGS(R.string.muscle_legs),
    CORE(R.string.muscle_core),
    FULL_BODY(R.string.muscle_full_body)
}
```

#### 2.2 Create Exercise Bank Data

**New File:** `app/src/main/java/com/hananel/workouttracker2/data/ExerciseBank.kt`

```kotlin
object ExerciseBank {
    
    fun getAllExercises(): List<ExerciseBankItem> {
        return listOf(
            // CHEST
            ExerciseBankItem(
                id = "bank_chest_dumbbell_press",
                nameResId = R.string.exercise_chest_dumbbell_press,
                muscleGroups = listOf("Chest"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_chest_cable_press",
                nameResId = R.string.exercise_chest_cable_press,
                muscleGroups = listOf("Chest"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_chest_flyes",
                nameResId = R.string.exercise_chest_flyes,
                muscleGroups = listOf("Chest"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            
            // BACK
            ExerciseBankItem(
                id = "bank_back_lat_pulldown",
                nameResId = R.string.exercise_back_lat_pulldown,
                muscleGroups = listOf("Back"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_back_cable_row",
                nameResId = R.string.exercise_back_cable_row,
                muscleGroups = listOf("Back"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            
            // SHOULDERS
            ExerciseBankItem(
                id = "bank_shoulders_dumbbell_press",
                nameResId = R.string.exercise_shoulders_dumbbell_press,
                muscleGroups = listOf("Shoulders"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_shoulders_lateral_raise",
                nameResId = R.string.exercise_shoulders_lateral_raise,
                muscleGroups = listOf("Shoulders"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            
            // LEGS
            ExerciseBankItem(
                id = "bank_legs_squats",
                nameResId = R.string.exercise_legs_squats,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_legs_leg_curl",
                nameResId = R.string.exercise_legs_leg_curl,
                muscleGroups = listOf("Hamstrings"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_legs_lunges",
                nameResId = R.string.exercise_legs_lunges,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12 steps each",
                defaultRestTime = 45
            ),
            
            // ARMS
            ExerciseBankItem(
                id = "bank_arms_dumbbell_curl",
                nameResId = R.string.exercise_arms_dumbbell_curl,
                muscleGroups = listOf("Biceps"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_arms_cable_tricep_extension",
                nameResId = R.string.exercise_arms_cable_tricep_extension,
                muscleGroups = listOf("Triceps"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            
            // CARDIO
            ExerciseBankItem(
                id = "bank_cardio_hiit",
                nameResId = R.string.exercise_cardio_hiit,
                muscleGroups = listOf("Full Body"),
                equipment = "Treadmill/Bike",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "30 min",
                defaultRestTime = 0,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_cardio_running",
                nameResId = R.string.exercise_cardio_running,
                muscleGroups = listOf("Full Body"),
                equipment = "Treadmill",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "30-40 min",
                defaultRestTime = 0,
                hasWeight = false
            )
            
            // TODO: Add 15+ more exercises to cover all muscle groups
        )
    }
    
    fun getExercisesByMuscleGroup(group: String): List<ExerciseBankItem> {
        return getAllExercises().filter { it.muscleGroups.contains(group) }
    }
    
    fun getExercisesByCategory(category: ExerciseCategory): List<ExerciseBankItem> {
        return getAllExercises().filter { it.category == category }
    }
}
```

#### 2.3 Create Exercise Catalog Screen

**New File:** `app/src/main/java/com/hananel/workouttracker2/ui/screens/ExerciseCatalogScreen.kt`

UI Structure:
- Top app bar with back button
- Horizontal scrollable tabs for muscle groups (Chest, Back, Shoulders, Arms, Legs, Cardio)
- Lazy column of exercises grouped by muscle
- Each exercise card shows:
  - Exercise name
  - Equipment icon/text
  - Default sets × reps
  - "Add" button

Material 3 design:
- Use FilterChip for muscle group tabs
- Use Card with elevation for exercise items
- Add icon for equipment type
- Smooth animations

#### 2.4 Update AddExerciseDialog

**File:** `app/src/main/java/com/hananel/workouttracker2/ui/components/AddExerciseDialog.kt`

Change to show menu first:
```kotlin
@Composable
fun AddExerciseMenu(
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onChooseFromCatalog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_exercise)) },
        text = {
            Column {
                TextButton(
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.create_new_exercise))
                }
                
                TextButton(
                    onClick = onChooseFromCatalog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.choose_from_catalog))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
```

---

### Phase 3: Onboarding System (Feature #3)

#### 3.1 Create Onboarding Data Model

**Update:** `UserPreferences.kt`
```kotlin
@Serializable
data class UserPreferences(
    // ... existing fields ...
    val hasCompletedOnboarding: Boolean = false
)
```

#### 3.2 Create Onboarding Screens

**New File:** `app/src/main/java/com/hananel/workouttracker2/ui/screens/OnboardingScreen.kt`

4 Screens using HorizontalPager:

**Screen 1 - Welcome:**
- App logo (large)
- App name
- Short tagline: "Track your workouts, reach your goals"
- "Get Started" button

**Screen 2 - Permissions:**
- Icon + title: "Enable Notifications"
- Description: "Stay on track with workout reminders and rest timer alerts"
- "Allow" button (requests permissions)
- "Skip" button

**Screen 3 - Exercise Setup:**
- Title: "Build Your Workout"
- Description: "Choose exercises for each day or start with our default program"
- Two options:
  1. "Use Default Program" (loads DefaultData)
  2. "Customize" → Opens simplified exercise selection
- For customization:
  - Show 7 days (Sun-Sat)
  - Each day has "+ Add Exercise" button
  - Opens ExerciseCatalogScreen in selection mode
  - Can assign multiple exercises to each day

**Screen 4 - Ready:**
- Success icon
- "You're All Set!"
- "Your personalized workout tracker is ready"
- "Start Training" button → Main app

#### 3.3 Onboarding Navigation

**Update:** `MainActivity.kt`

```kotlin
NavHost(
    navController = navController,
    startDestination = if (hasCompletedOnboarding) "workout" else "onboarding"
) {
    composable("onboarding") {
        OnboardingScreen(
            onOnboardingComplete = {
                scope.launch {
                    repository.updateOnboardingComplete(true)
                    navController.navigate("workout") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }
        )
    }
    
    composable("workout") {
        // ... existing workout screen
    }
    
    // ... other screens
}
```

---

### Phase 4: Enhanced Timer Notifications (Feature #4)

#### 4.1 Update TimerService

**File:** `app/src/main/java/com/hananel/workouttracker2/service/TimerService.kt`

Add countdown to notification:
```kotlin
private fun updateNotification(timeLeft: Int) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Rest Timer - $exerciseName")
        .setContentText("Set $currentSet/$totalSets • $timeText remaining")
        .setSmallIcon(R.drawable.ic_timer)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setProgress(totalTime, timeLeft, false)  // Progress bar
        .addAction(
            R.drawable.ic_pause,
            if (isRunning) "Pause" else "Resume",
            pauseResumePendingIntent
        )
        .addAction(
            R.drawable.ic_stop,
            "Stop",
            stopPendingIntent
        )
        .build()
    
    notificationManager.notify(NOTIFICATION_ID, notification)
}
```

Add pause/resume functionality:
```kotlin
companion object {
    fun pauseTimer(context: Context) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = ACTION_PAUSE_RESUME
        }
        context.startService(intent)
    }
    
    fun resumeTimer(context: Context) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = ACTION_PAUSE_RESUME
        }
        context.startService(intent)
    }
}

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
        ACTION_PAUSE_RESUME -> {
            isRunning = !isRunning
            updateNotification(currentTimeLeft)
        }
        // ... other actions
    }
    return START_STICKY
}
```

---

### Phase 5: Additional Editing Capabilities (Feature #5)

#### 5.1 Rename Workout Days

**Update:** `WorkoutDay.kt`
```kotlin
@Serializable
data class WorkoutDay(
    val id: String,
    val nameResId: Int,
    val customName: String? = null,  // NEW: User can override default name
    val dayIndex: Int,
    val exercises: List<Exercise>,
    val targetMuscleGroups: List<String>,
    val enabled: Boolean = true  // NEW: Can disable days
)

fun getDisplayName(context: Context): String {
    return customName ?: context.getString(nameResId)
}
```

**Add to ViewModel:**
```kotlin
fun renameWorkoutDay(dayId: String, newName: String) {
    viewModelScope.launch {
        val currentDays = _workoutDays.value.toMutableList()
        val index = currentDays.indexOfFirst { it.id == dayId }
        
        if (index != -1) {
            currentDays[index] = currentDays[index].copy(customName = newName)
            _workoutDays.value = currentDays
            repository.saveWorkoutStructure(currentDays)
        }
    }
}
```

**UI:** Long-press on day tab → Show rename dialog

#### 5.2 Add/Remove Days

**Add to ViewModel:**
```kotlin
fun addWorkoutDay(dayIndex: Int, name: String) {
    viewModelScope.launch {
        val newDay = WorkoutDay(
            id = "day_$dayIndex",
            nameResId = R.string.custom_workout_day,
            customName = name,
            dayIndex = dayIndex,
            exercises = emptyList(),
            targetMuscleGroups = emptyList()
        )
        
        val currentDays = _workoutDays.value.toMutableList()
        currentDays.add(newDay)
        currentDays.sortBy { it.dayIndex }
        
        _workoutDays.value = currentDays
        repository.saveWorkoutStructure(currentDays)
    }
}

fun removeWorkoutDay(dayId: String) {
    viewModelScope.launch {
        val currentDays = _workoutDays.value.toMutableList()
        currentDays.removeIf { it.id == dayId }
        
        _workoutDays.value = currentDays
        repository.saveWorkoutStructure(currentDays)
    }
}
```

**UI in Settings:**
- Section: "Manage Workout Days"
- List of all 7 days with toggles
- Can add custom name for each day
- "+" button to add new day (Friday=5, Saturday=6)

---

## Implementation Order

1. ✅ **Phase 1** - Auto-open current day (simplest, good warm-up)
2. ✅ **Phase 2** - Exercise bank (core infrastructure)
3. ✅ **Phase 5.1** - Rename workout days (quick, builds on existing)
4. ✅ **Phase 5.2** - Add/remove days (extends day management)
5. ✅ **Phase 4** - Enhanced notifications (timer improvements)
6. ✅ **Phase 3** - Onboarding (ties everything together)
7. ✅ **i18n** - Add all strings for Hebrew + English
8. ✅ **Testing** - End-to-end testing of all features
9. ✅ **README** - Update documentation with new features

---

## i18n Requirements

All new strings must be added to:
- `app/src/main/res/values/strings.xml` (Hebrew)
- `app/src/main/res/values-en/strings.xml` (English)

String categories needed:
- Auto-open settings (2 strings)
- Exercise bank (30+ exercise names)
- Muscle groups (7 strings)
- Exercise catalog UI (10 strings)
- Onboarding screens (15+ strings)
- Timer notification actions (3 strings)
- Day management (5 strings)

Total: ~75 new string resources

---

## Testing Checklist

After implementation:

- [ ] Auto-open toggle works in both states
- [ ] Last viewed day persists correctly
- [ ] Exercise bank loads all exercises
- [ ] Catalog screen groups by muscle correctly
- [ ] Can add exercises from catalog
- [ ] Can create custom exercises
- [ ] Onboarding shows on first launch only
- [ ] Onboarding permissions work
- [ ] Onboarding exercise setup works
- [ ] Timer notification shows countdown
- [ ] Pause/resume from notification works
- [ ] Can rename workout days
- [ ] Can add Friday/Saturday
- [ ] Can remove days
- [ ] All features work in Hebrew
- [ ] All features work in English
- [ ] Dark mode looks good for all new UI
- [ ] No crashes or data loss

---

## Log

**December 4, 2025:**
- Created comprehensive implementation blueprint
- Analyzed current codebase structure
- Defined 5 main features with detailed pseudocode
- Established implementation order
- Identified i18n requirements

**Next Steps:**
1. Start with Phase 1 (auto-open toggle) - simplest feature
2. Test thoroughly before moving to next phase
3. Commit after each phase completion

---

## Notes

- Maintain existing architecture (Clean Architecture + MVVM)
- All data persistence through DataStore
- No breaking changes to existing features
- Backward compatible with existing user data
- Material 3 design consistency
- Full RTL support for Hebrew
- Accessibility considerations for all new UI

