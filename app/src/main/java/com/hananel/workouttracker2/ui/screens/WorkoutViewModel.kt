package com.hananel.workouttracker2.ui.screens

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.Exercise
import com.hananel.workouttracker2.data.models.TimerState
import com.hananel.workouttracker2.data.models.WorkoutDay
import com.hananel.workouttracker2.data.models.WorkoutProgress
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val resources: Resources? = null
) : ViewModel() {
    
    private val _workoutDays = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val workoutDays: StateFlow<List<WorkoutDay>> = _workoutDays.asStateFlow()
    
    private val _workoutProgress = MutableStateFlow<WorkoutProgress?>(null)
    val workoutProgress: StateFlow<WorkoutProgress?> = _workoutProgress.asStateFlow()
    
    private val _timerState = MutableStateFlow<TimerState?>(null)
    val timerState: StateFlow<TimerState?> = _timerState.asStateFlow()
    
    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()
    
    init {
        validateAndClearStaleTimerState()
        loadData()
        setInitialDay()
    }
    
    /**
     * Validates timer state on app startup.
     * Clears stale timer state that may have been restored from cloud backup.
     * A timer is considered stale if:
     * - It claims to be running but there's no actual service running
     * - The exercise ID doesn't exist in current workout structure
     */
    private fun validateAndClearStaleTimerState() {
        viewModelScope.launch {
            val timerState = repository.getTimerState()
            if (timerState != null) {
                // Timer state exists - validate it
                val workoutStructure = repository.getWorkoutStructure()
                
                // Check if exercise exists in current workout structure
                val exerciseExists = workoutStructure
                    .flatMap { it.exercises }
                    .any { it.id == timerState.exerciseId }
                
                // Check if timer state is stale (from a different session/cloud restore)
                // If no exercises exist, or the timer's exercise doesn't exist, clear it
                val shouldClear = workoutStructure.isEmpty() || !exerciseExists
                
                if (shouldClear) {
                    // Stale timer state - clear it
                    repository.clearTimerState()
                }
            }
        }
    }
    
    /**
     * Validates workout data integrity.
     * Returns true if data is valid, false if corrupted (should be cleared).
     */
    private fun isWorkoutDataValid(workoutDays: List<WorkoutDay>): Boolean {
        if (workoutDays.isEmpty()) return true // Empty is valid
        
        val res = resources ?: return true // Can't validate without resources
        
        // Check first exercise nameResId to verify data integrity
        val firstExercise = workoutDays.firstOrNull()?.exercises?.firstOrNull() ?: return true
        
        return try {
            val name = res.getString(firstExercise.nameResId)
            // Valid exercise names should NOT contain format placeholders
            // and should be reasonable length
            !name.contains("%") && name.length >= 3 && name.length < 100
        } catch (e: Resources.NotFoundException) {
            false // Invalid resource ID
        } catch (e: Exception) {
            false
        }
    }
    
    private fun setInitialDay() {
        viewModelScope.launch {
            val prefs = repository.getUserPreferences()
            // Auto-open logic: Only use current day if toggle is ON AND this is first launch (lastViewedDay is 0)
            // After first use, ALWAYS restore last viewed day (even if toggle is on)
            // This prevents annoying jumps when user minimizes app during workout
            _selectedDayIndex.value = if (prefs.autoOpenCurrentDay && prefs.lastViewedDayIndex == 0) {
                DateUtils.getCurrentDayIndex()
            } else {
                prefs.lastViewedDayIndex
            }
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Load saved workout structure - do NOT auto-create default data
            // If empty, the UI will show the empty state with "Setup Workout" button
            repository.getWorkoutStructureFlow().collect { savedStructure ->
                // Validate data integrity (can be corrupted by cloud backup restore with shifted R values)
                if (isWorkoutDataValid(savedStructure)) {
                    _workoutDays.value = savedStructure
                } else {
                    // Data is corrupted - clear EVERYTHING and show empty state
                    // This includes progress and timer state which may also be stale
                    repository.clearAllProgressAndTimerState()
                    repository.saveWorkoutStructure(emptyList())
                    _workoutDays.value = emptyList()
                }
            }
        }
        
        viewModelScope.launch {
            // Load progress
            repository.getWorkoutProgressFlow().collect { progress ->
                _workoutProgress.value = progress
            }
        }
        
        viewModelScope.launch {
            // Load timer state
            repository.getTimerStateFlow().collect { timerState ->
                _timerState.value = timerState
            }
        }
    }
    
    fun selectDay(dayIndex: Int) {
        _selectedDayIndex.value = dayIndex
        viewModelScope.launch {
            repository.updateLastViewedDay(dayIndex)
        }
    }
    
    fun getCurrentDayWorkout(): WorkoutDay? {
        return _workoutDays.value.find { it.dayIndex == _selectedDayIndex.value }
    }
    
    fun isExerciseCompleted(exerciseId: String): Boolean {
        return _workoutProgress.value?.completedExercises?.get(exerciseId) ?: false
    }
    
    fun getExerciseCompletedSets(exerciseId: String): Int {
        return _workoutProgress.value?.exerciseCompletedSets?.get(exerciseId) ?: 0
    }
    
    fun getExerciseWeight(exerciseId: String): Float {
        return _workoutProgress.value?.exerciseWeights?.get(exerciseId) ?: 0f
    }
    
    fun updateExerciseWeight(exerciseId: String, weight: Float) {
        viewModelScope.launch {
            repository.updateExerciseWeight(exerciseId, weight)
        }
    }
    
    fun updateExercise(exerciseId: String, sets: Int, reps: String, restTime: Int) {
        viewModelScope.launch {
            val currentDays = _workoutDays.value.toMutableList()
            var updated = false
            
            for (i in currentDays.indices) {
                val day = currentDays[i]
                val exerciseIndex = day.exercises.indexOfFirst { it.id == exerciseId }
                
                if (exerciseIndex != -1) {
                    val updatedExercises = day.exercises.toMutableList()
                    val exercise = updatedExercises[exerciseIndex]
                    
                    updatedExercises[exerciseIndex] = exercise.copy(
                        sets = sets,
                        reps = reps,
                        restTime = restTime
                    )
                    
                    currentDays[i] = day.copy(exercises = updatedExercises)
                    updated = true
                    break
                }
            }
            
            if (updated) {
                _workoutDays.value = currentDays
                repository.saveWorkoutStructure(currentDays)
            }
        }
    }
    
    fun toggleExerciseCompletion(exerciseId: String) {
        viewModelScope.launch {
            // Find exercise in current workout structure
            val exercise = _workoutDays.value
                .flatMap { it.exercises }
                .find { it.id == exerciseId }
            
            if (exercise != null) {
                val progress = repository.getWorkoutProgress()
                val currentlyCompleted = progress.completedExercises[exerciseId] ?: false
                
                if (currentlyCompleted) {
                    // Currently completed -> Unmark and reset sets
                    // Update in single transaction
                    repository.saveWorkoutProgress(
                        progress.copy(
                            completedExercises = progress.completedExercises.toMutableMap().apply {
                                this[exerciseId] = false
                            },
                            exerciseCompletedSets = progress.exerciseCompletedSets.toMutableMap().apply {
                                this[exerciseId] = 0
                            },
                            lastUpdateTime = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Not completed -> Mark as complete and fill all sets
                    // Update in single transaction
                    repository.saveWorkoutProgress(
                        progress.copy(
                            completedExercises = progress.completedExercises.toMutableMap().apply {
                                this[exerciseId] = true
                            },
                            exerciseCompletedSets = progress.exerciseCompletedSets.toMutableMap().apply {
                                this[exerciseId] = exercise.sets
                            },
                            lastUpdateTime = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
    
    fun startTimer(exercise: Exercise, context: Context) {
        viewModelScope.launch {
            val completedSets = getExerciseCompletedSets(exercise.id)
            val currentSet = completedSets + 1
            
            val exerciseName = context.getString(exercise.nameResId)
            
            // Start the foreground service
            com.hananel.workouttracker2.service.TimerService.startTimer(
                context = context,
                exerciseName = exerciseName,
                exerciseId = exercise.id,
                restTime = exercise.restTime,
                currentSet = currentSet,
                totalSets = exercise.sets,
                completedSets = completedSets
            )
        }
    }
    
    fun updateTimerTick(timeLeft: Int) {
        viewModelScope.launch {
            _timerState.value?.let { currentState ->
                val updatedState = currentState.copy(timeLeft = timeLeft)
                repository.saveTimerState(updatedState)
            }
        }
    }
    
    fun completeSet(context: Context, onAllSetsComplete: () -> Unit) {
        viewModelScope.launch {
            // The service handles the next set logic
            com.hananel.workouttracker2.service.TimerService.nextSet(context)
            
            // Check if all sets are now complete
            _timerState.value?.let { timerState ->
                val newCompletedSets = timerState.completedSets + 1
                if (newCompletedSets >= timerState.totalSets) {
                    onAllSetsComplete()
                }
            }
        }
    }
    
    fun stopTimer(context: Context) {
        viewModelScope.launch {
            // Stop the foreground service
            com.hananel.workouttracker2.service.TimerService.stopTimer(context)
        }
    }
    
    fun resetWeeklyProgress() {
        viewModelScope.launch {
            repository.resetWeeklyProgress()
        }
    }
    
    fun addExerciseFromBank(dayId: String, bankItem: com.hananel.workouttracker2.data.models.ExerciseBankItem) {
        viewModelScope.launch {
            val currentDays = _workoutDays.value.toMutableList()
            val dayIndex = currentDays.indexOfFirst { it.id == dayId }
            
            if (dayIndex != -1) {
                val day = currentDays[dayIndex]
                
                // Generate unique ID for this exercise instance
                val newExerciseId = "${bankItem.id}_${System.currentTimeMillis()}"
                
                // Create Exercise from BankItem
                val newExercise = Exercise(
                    id = newExerciseId,
                    nameResId = bankItem.nameResId,
                    sets = bankItem.defaultSets,
                    reps = bankItem.defaultReps,
                    restTime = bankItem.defaultRestTime,
                    hasWeight = bankItem.hasWeight,
                    muscleGroups = bankItem.muscleGroups,
                    equipment = bankItem.equipment
                )
                
                // Add exercise to day (append to end, preserving existing exercises)
                val updatedExercises = day.exercises.toMutableList().apply {
                    add(newExercise)
                }
                
                currentDays[dayIndex] = day.copy(exercises = updatedExercises)
                
                // Save updated structure
                _workoutDays.value = currentDays
                repository.saveWorkoutStructure(currentDays)
            }
        }
    }
    
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
    
    fun deleteExercise(dayId: String, exerciseId: String) {
        viewModelScope.launch {
            val currentDays = _workoutDays.value.toMutableList()
            val dayIndex = currentDays.indexOfFirst { it.id == dayId }
            
            if (dayIndex != -1) {
                val day = currentDays[dayIndex]
                val updatedExercises = day.exercises.filter { it.id != exerciseId }
                currentDays[dayIndex] = day.copy(exercises = updatedExercises)
                
                _workoutDays.value = currentDays
                repository.saveWorkoutStructure(currentDays)
                
                // Also remove from progress
                repository.removeExerciseProgress(exerciseId)
            }
        }
    }
}

// Factory for creating ViewModel with repository
class WorkoutViewModelFactory(private val context: Context) {
    fun create(): WorkoutViewModel {
        val repository = WorkoutRepository(context)
        return WorkoutViewModel(repository, context.resources)
    }
}

