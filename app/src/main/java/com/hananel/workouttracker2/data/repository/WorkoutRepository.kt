package com.hananel.workouttracker2.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hananel.workouttracker2.data.models.*
import com.hananel.workouttracker2.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_tracker")

class WorkoutRepository(private val context: Context) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    companion object {
        private val WORKOUT_PROGRESS_KEY = stringPreferencesKey("workout_progress")
        private val USER_PREFERENCES_KEY = stringPreferencesKey("user_preferences")
        private val WORKOUT_STRUCTURE_KEY = stringPreferencesKey("workout_structure")
        private val TIMER_STATE_KEY = stringPreferencesKey("timer_state")
    }
    
    // Workout Progress
    suspend fun saveWorkoutProgress(progress: WorkoutProgress) {
        context.dataStore.edit { preferences ->
            preferences[WORKOUT_PROGRESS_KEY] = json.encodeToString(progress)
        }
    }
    
    fun getWorkoutProgressFlow(): Flow<WorkoutProgress> = context.dataStore.data.map { preferences ->
        preferences[WORKOUT_PROGRESS_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<WorkoutProgress>(jsonString)
            } catch (e: Exception) {
                createDefaultProgress()
            }
        } ?: createDefaultProgress()
    }
    
    suspend fun getWorkoutProgress(): WorkoutProgress {
        return getWorkoutProgressFlow().first()
    }
    
    private fun createDefaultProgress(): WorkoutProgress {
        return WorkoutProgress(
            weekStartDate = DateUtils.getCurrentWeekStart(),
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    // User Preferences
    suspend fun saveUserPreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[USER_PREFERENCES_KEY] = json.encodeToString(preferences)
        }
    }
    
    suspend fun updateUserPreferences(preferences: UserPreferences) {
        saveUserPreferences(preferences)
    }
    
    fun getUserPreferencesFlow(): Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        preferences[USER_PREFERENCES_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<UserPreferences>(jsonString)
            } catch (e: Exception) {
                UserPreferences()
            }
        } ?: UserPreferences()
    }
    
    suspend fun getUserPreferences(): UserPreferences {
        return getUserPreferencesFlow().first()
    }
    
    suspend fun updateAutoOpenCurrentDay(enabled: Boolean) {
        val currentPrefs = getUserPreferences()
        saveUserPreferences(currentPrefs.copy(autoOpenCurrentDay = enabled))
    }
    
    suspend fun updateLastViewedDay(dayIndex: Int) {
        val currentPrefs = getUserPreferences()
        saveUserPreferences(currentPrefs.copy(lastViewedDayIndex = dayIndex))
    }
    
    // Workout Structure
    suspend fun saveWorkoutStructure(workoutDays: List<WorkoutDay>) {
        context.dataStore.edit { preferences ->
            preferences[WORKOUT_STRUCTURE_KEY] = json.encodeToString(workoutDays)
        }
    }
    
    fun getWorkoutStructureFlow(): Flow<List<WorkoutDay>> = context.dataStore.data.map { preferences ->
        preferences[WORKOUT_STRUCTURE_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<List<WorkoutDay>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    suspend fun getWorkoutStructure(): List<WorkoutDay> {
        return getWorkoutStructureFlow().first()
    }
    
    // Timer State
    suspend fun saveTimerState(timerState: TimerState?) {
        context.dataStore.edit { preferences ->
            if (timerState != null) {
                preferences[TIMER_STATE_KEY] = json.encodeToString(timerState)
            } else {
                preferences.remove(TIMER_STATE_KEY)
            }
        }
    }
    
    fun getTimerStateFlow(): Flow<TimerState?> = context.dataStore.data.map { preferences ->
        preferences[TIMER_STATE_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<TimerState>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun getTimerState(): TimerState? {
        return getTimerStateFlow().first()
    }
    
    // Convenience methods for updating specific progress fields
    suspend fun toggleExerciseCompletion(exerciseId: String) {
        val progress = getWorkoutProgress()
        val currentlyCompleted = progress.completedExercises[exerciseId] ?: false
        val updatedMap = progress.completedExercises.toMutableMap()
        updatedMap[exerciseId] = !currentlyCompleted
        
        saveWorkoutProgress(
            progress.copy(
                completedExercises = updatedMap,
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun updateExerciseWeight(exerciseId: String, weight: Float) {
        val progress = getWorkoutProgress()
        val updatedMap = progress.exerciseWeights.toMutableMap()
        updatedMap[exerciseId] = weight
        
        saveWorkoutProgress(
            progress.copy(
                exerciseWeights = updatedMap,
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun updateExerciseCompletedSets(exerciseId: String, completedSets: Int, totalSets: Int) {
        val progress = getWorkoutProgress()
        val updatedSetsMap = progress.exerciseCompletedSets.toMutableMap()
        updatedSetsMap[exerciseId] = completedSets
        
        // Auto-complete exercise if all sets are done
        val updatedCompletedMap = progress.completedExercises.toMutableMap()
        if (completedSets >= totalSets) {
            updatedCompletedMap[exerciseId] = true
        } else {
            // If user manually uncompleted, make sure it stays uncompleted
            if (completedSets < totalSets) {
                updatedCompletedMap[exerciseId] = false
            }
        }
        
        saveWorkoutProgress(
            progress.copy(
                exerciseCompletedSets = updatedSetsMap,
                completedExercises = updatedCompletedMap,
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun resetWeeklyProgress() {
        val current = getWorkoutProgress()
        val reset = WorkoutProgress(
            completedExercises = emptyMap(),
            exerciseWeights = current.exerciseWeights, // Keep weights
            exerciseNotes = current.exerciseNotes, // Keep notes
            exerciseCompletedSets = emptyMap(),
            weekStartDate = DateUtils.getCurrentWeekStart(),
            lastUpdateTime = System.currentTimeMillis()
        )
        saveWorkoutProgress(reset)
    }
    
    suspend fun removeExerciseProgress(exerciseId: String) {
        val progress = getWorkoutProgress()
        val updatedCompleted = progress.completedExercises.toMutableMap()
        val updatedWeights = progress.exerciseWeights.toMutableMap()
        val updatedNotes = progress.exerciseNotes.toMutableMap()
        val updatedSets = progress.exerciseCompletedSets.toMutableMap()
        
        updatedCompleted.remove(exerciseId)
        updatedWeights.remove(exerciseId)
        updatedNotes.remove(exerciseId)
        updatedSets.remove(exerciseId)
        
        saveWorkoutProgress(
            progress.copy(
                completedExercises = updatedCompleted,
                exerciseWeights = updatedWeights,
                exerciseNotes = updatedNotes,
                exerciseCompletedSets = updatedSets,
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Clears ALL progress and timer state.
     * Use this when installing a new workout program to ensure clean slate.
     * This fixes issues with stale data restored from cloud backup.
     */
    suspend fun clearAllProgressAndTimerState() {
        context.dataStore.edit { preferences ->
            // Clear timer state completely
            preferences.remove(TIMER_STATE_KEY)
            // Reset workout progress to fresh state (keeps nothing)
            preferences[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                WorkoutProgress(
                    completedExercises = emptyMap(),
                    exerciseWeights = emptyMap(),
                    exerciseNotes = emptyMap(),
                    exerciseCompletedSets = emptyMap(),
                    weekStartDate = DateUtils.getCurrentWeekStart(),
                    lastUpdateTime = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Clears only the timer state.
     * Use to clean up stale timer from cloud backup restore.
     */
    suspend fun clearTimerState() {
        context.dataStore.edit { preferences ->
            preferences.remove(TIMER_STATE_KEY)
        }
    }
}

