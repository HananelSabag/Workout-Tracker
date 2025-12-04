package com.hananel.workouttracker2.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananel.workouttracker2.data.DefaultData
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

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {
    
    private val _workoutDays = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val workoutDays: StateFlow<List<WorkoutDay>> = _workoutDays.asStateFlow()
    
    private val _workoutProgress = MutableStateFlow<WorkoutProgress?>(null)
    val workoutProgress: StateFlow<WorkoutProgress?> = _workoutProgress.asStateFlow()
    
    private val _timerState = MutableStateFlow<TimerState?>(null)
    val timerState: StateFlow<TimerState?> = _timerState.asStateFlow()
    
    private val _selectedDayIndex = MutableStateFlow(DateUtils.getCurrentDayIndex())
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Try to load saved workout structure first, fallback to default
            repository.getWorkoutStructureFlow().collect { savedStructure ->
                _workoutDays.value = if (savedStructure.isNotEmpty()) {
                    savedStructure
                } else {
                    // First time - load default and save it
                    val defaultStructure = DefaultData.getDefaultWorkoutStructure()
                    repository.saveWorkoutStructure(defaultStructure)
                    defaultStructure
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
}

// Factory for creating ViewModel with repository
class WorkoutViewModelFactory(private val context: Context) {
    fun create(): WorkoutViewModel {
        val repository = WorkoutRepository(context)
        return WorkoutViewModel(repository)
    }
}

