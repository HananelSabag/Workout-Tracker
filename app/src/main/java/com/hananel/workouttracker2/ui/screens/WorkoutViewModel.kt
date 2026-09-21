package com.hananel.workouttracker2.ui.screens

import android.content.Context
import android.content.res.Resources
import com.hananel.workouttracker2.data.DisplayNames
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hananel.workouttracker2.data.models.Exercise
import com.hananel.workouttracker2.data.models.TimerState
import com.hananel.workouttracker2.data.models.WorkoutDay
import com.hananel.workouttracker2.data.models.WorkoutProgress
import com.hananel.workouttracker2.data.models.SessionExerciseLog
import com.hananel.workouttracker2.data.models.WorkoutSession
import com.hananel.workouttracker2.data.repository.SessionContext
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.service.TimerService
import com.hananel.workouttracker2.utils.DateUtils
import com.hananel.workouttracker2.utils.RepsUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession: StateFlow<WorkoutSession?> = _activeSession.asStateFlow()

    private val _sessionHistory = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val sessionHistory: StateFlow<List<WorkoutSession>> = _sessionHistory.asStateFlow()

    /** Ticks once a second so the live session clock counts up without every screen owning a timer. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()
    private val errorHandler = CoroutineExceptionHandler { _, failure ->
        android.util.Log.e("WorkoutViewModel", "Workout operation failed", failure)
        _error.value = true
    }

    init { initialize() }

    fun retry() {
        viewModelScope.coroutineContext.cancelChildren()
        _error.value = false
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch(errorHandler) {
            // Validate all persisted sections before any startup operation can change them.
            repository.getWorkoutStructure()
            repository.getWorkoutProgress()
            repository.getTimerState()
            repository.getActiveSession()
            repository.getSessionHistory()
            val prefs = repository.getUserPreferences()
            _selectedDayIndex.value = (if (prefs.autoOpenCurrentDay && !prefs.hasViewedAnyDay)
                DateUtils.getCurrentDayIndex() else prefs.lastViewedDayIndex).coerceIn(0, 6)
            repository.finalizeStaleSession()
            if (prefs.autoResetWeekly && repository.getWorkoutProgress().weekStartDate != DateUtils.getCurrentWeekStart()) {
                repository.resetWeeklyProgress()
            }
            loadData()
            startSessionWatchdog()
        }
    }

    /**
     * Drives the live clock and closes an abandoned session.
     *
     * The workout clock starts at the first logged set and stops after ten minutes of silence,
     * backdated to the last action — so leaving the phone in the locker doesn't inflate the
     * session, and neither does forgetting to press anything on the way out.
     */
    private fun startSessionWatchdog() {
        viewModelScope.launch(errorHandler) {
            // Anything left open from a previous process is settled before the UI reads it.
            repository.finalizeStaleSession()

            // collectLatest restarts the ticker the moment a session opens and cancels it the
            // moment one closes, so there's no per-second wake-up when nothing is being timed.
            _activeSession.collectLatest { session ->
                if (session == null) return@collectLatest
                while (true) {
                    _now.value = System.currentTimeMillis()
                    if (session.isStaleAt(_now.value)) {
                        repository.finalizeStaleSession(_now.value)
                        return@collectLatest
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch(errorHandler) {
            repository.getWorkoutStructureFlow().distinctUntilChanged().collect { savedStructure ->
                _workoutDays.value = savedStructure
                startFreshDayIfNeeded(_selectedDayIndex.value)
            }
        }

        viewModelScope.launch(errorHandler) {
            repository.getWorkoutProgressFlow().collect { _workoutProgress.value = it }
        }

        viewModelScope.launch(errorHandler) {
            repository.getTimerStateFlow().distinctUntilChanged().collectLatest { state ->
                _timerState.value = state
                if (state != null && state.isRunning && !state.isPaused && !state.isRestComplete) {
                    while (true) {
                        val remaining = state.remainingSeconds()
                        _timerState.value = state.copy(timeLeft = remaining, isRestComplete = remaining == 0, isRunning = remaining > 0)
                        if (remaining == 0) break
                        delay(250L)
                    }
                }
            }
        }

        viewModelScope.launch(errorHandler) {
            repository.getActiveSessionFlow().collect { _activeSession.value = it }
        }

        viewModelScope.launch(errorHandler) {
            repository.getSessionHistoryFlow().collect { _sessionHistory.value = it }
        }
    }

    fun selectDay(dayIndex: Int) {
        _selectedDayIndex.value = dayIndex
        viewModelScope.launch(errorHandler) {
            repository.updateLastViewedDay(dayIndex)
            startFreshDayIfNeeded(dayIndex)
        }
    }

    /**
     * Clears a day's checkmarks when they were earned on an earlier date.
     *
     * Progress used to live for a whole week, so coming back to Sunday next Sunday showed an
     * exercise already sitting at 3/5. Every visit to a day on a new date now starts from zero —
     * except while that day's workout is actually running, so a session crossing midnight survives.
     */
    private suspend fun startFreshDayIfNeeded(dayIndex: Int) {
        val day = _workoutDays.value.find { it.dayIndex == dayIndex } ?: return
        val cleared = repository.startFreshDayIfNeeded(day.id, day.exercises.map { it.id })
        // Only announce it when marks actually disappeared — otherwise the user sees an
        // unexplained change and assumes data was lost.
        if (cleared) _dayResetNotice.emit(Unit)
    }

    private val _dayResetNotice = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val dayResetNotice: SharedFlow<Unit> = _dayResetNotice.asSharedFlow()

    fun getCurrentDayWorkout(): WorkoutDay? =
        _workoutDays.value.find { it.dayIndex == _selectedDayIndex.value }

    fun isExerciseCompleted(exerciseId: String): Boolean =
        _workoutProgress.value?.completedExercises?.get(exerciseId) ?: false

    fun getExerciseCompletedSets(exerciseId: String): Int =
        _workoutProgress.value?.exerciseCompletedSets?.get(exerciseId) ?: 0

    fun getExerciseWeight(exerciseId: String): Float =
        _workoutProgress.value?.exerciseWeights?.get(exerciseId) ?: 0f

    fun updateExerciseWeight(exerciseId: String, weight: Float) {
        viewModelScope.launch(errorHandler) { repository.updateExerciseWeight(exerciseId, weight) }
    }

    // ── Set tracking ─────────────────────────────────────────────────────────

    /**
     * Attributes an action to the day that actually owns [exercise], not to whatever tab happens to
     * be selected — otherwise finishing a set from the timer sheet after swiping to another day
     * would file it under the wrong workout.
     */
    private fun sessionContextFor(exercise: Exercise?): SessionContext {
        val day = exercise?.let { ex -> _workoutDays.value.find { d -> d.exercises.any { it.id == ex.id } } }
            ?: getCurrentDayWorkout()
        val dayName = day?.let { d ->
            DisplayNames.dayName(d, resources)
        }.orEmpty()
        val exerciseName = exercise?.let { ex ->
            DisplayNames.exerciseName(ex, resources)
        }.orEmpty()

        return SessionContext(
            dayIndex = day?.dayIndex ?: _selectedDayIndex.value,
            dayId = day?.id.orEmpty(),
            dayName = dayName,
            plannedExercises = day?.exercises?.size ?: 0,
            exerciseName = exerciseName,
            reps = exercise?.reps.orEmpty()
        )
    }

    /**
     * The single entry point for "I finished a set".
     *
     * Records the set, then starts the rest only when there's another set to rest *for*. Finishing
     * the final set marks the exercise complete in the same write and kills any running countdown —
     * previously this depended on the rest timer running to completion, which is why the last set
     * so often left the card un-ticked.
     */
    fun completeSet(exercise: Exercise, context: Context) {
        viewModelScope.launch(errorHandler) {
            val ctx = sessionContextFor(exercise)
            val result = repository.completeSet(
                exerciseId = exercise.id,
                totalSets = exercise.sets,
                ctx = ctx,
                // Pre-filled from the plan so logging a set stays a single tap; the rest timer
                // offers a stepper for the times you managed more or fewer.
                reps = RepsUtils.targetReps(exercise.reps)
            )

            // No rest after the final set — the exercise is already done, a countdown would just
            // leave a notification hanging around.
            if (result.exerciseCompleted || exercise.restTime <= 0) {
                TimerService.stopTimer(context)
            } else {
                startRest(context, exercise, ctx, result.completedSets)
            }
        }
    }

    private fun startRest(context: Context, exercise: Exercise, ctx: SessionContext, completedSets: Int) {
        TimerService.startRest(
            context = context,
            exerciseName = DisplayNames.exerciseName(exercise, context.resources),
            exerciseId = exercise.id,
            restTime = exercise.restTime,
            completedSets = completedSets,
            totalSets = exercise.sets,
            dayIndex = ctx.dayIndex,
            dayId = ctx.dayId,
            dayName = ctx.dayName,
            plannedExercises = ctx.plannedExercises,
            reps = exercise.reps
        )
    }

    fun undoSet(exercise: Exercise, context: Context) {
        viewModelScope.launch(errorHandler) {
            repository.undoSet(exercise.id, exercise.sets)
            val timer = _timerState.value
            if (timer?.exerciseId == exercise.id) {
                TimerService.stopTimer(context)
            }
        }
    }

    fun toggleExerciseCompletion(exerciseId: String) {
        viewModelScope.launch(errorHandler) {
            val exercise = _workoutDays.value.flatMap { it.exercises }.find { it.id == exerciseId } ?: return@launch
            val currentlyCompleted = repository.getWorkoutProgress().completedExercises[exerciseId] ?: false
            repository.setExerciseCompletion(
                exerciseId = exerciseId,
                completed = !currentlyCompleted,
                totalSets = exercise.sets,
                ctx = sessionContextFor(exercise)
            )
        }
    }

    // ── Rest timer controls ──────────────────────────────────────────────────

    fun startRestManually(exercise: Exercise, context: Context) {
        viewModelScope.launch(errorHandler) { startRest(context, exercise, sessionContextFor(exercise), getExerciseCompletedSets(exercise.id)) }
    }

    fun skipRest(context: Context) { viewModelScope.launch(errorHandler) { TimerService.skipRest(context) } }

    /**
     * Repairs a countdown whose process was killed mid-rest.
     *
     * A CountDownTimer doesn't survive the OS reclaiming the app, so the persisted state comes back
     * frozen at whatever second it stopped on and would never finish. The stored deadline says what
     * really happened: either the rest is already over, or there's time left worth resuming.
     */
    fun recoverTimerIfInterrupted(context: Context) {
        viewModelScope.launch(errorHandler) {
            if (repository.getTimerState() != null && !TimerService.isRunning) TimerService.recover(context)
        }
    }

    /** Adjusts the reps recorded for the set that just finished. */
    fun setLastSetReps(exerciseId: String, reps: Int) {
        viewModelScope.launch(errorHandler) { repository.updateLastSetReps(exerciseId, reps) }
    }

    /** Reps logged for the set the current rest belongs to, for the timer sheet's stepper. */
    fun lastLoggedReps(exerciseId: String): Int =
        _activeSession.value?.exercises
            ?.firstOrNull { it.exerciseId == exerciseId }
            ?.repsPerSet?.lastOrNull() ?: 0

    fun stopTimer(context: Context) { viewModelScope.launch(errorHandler) { TimerService.stopTimer(context) } }

    fun pauseResumeTimer(context: Context) { viewModelScope.launch(errorHandler) { TimerService.pauseResume(context) } }

    /** Completes the set the finished rest was leading up to, straight from the timer sheet. */
    fun completeSetFromTimer(context: Context) {
        val state = _timerState.value ?: return
        val exercise = _workoutDays.value.flatMap { it.exercises }.find { it.id == state.exerciseId } ?: return
        completeSet(exercise, context)
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    fun endSession(context: Context, onSaved: () -> Unit = {}) {
        viewModelScope.launch(errorHandler) {
            repository.endActiveSession()
            TimerService.stopTimer(context)
            onSaved()
        }
    }

    fun discardSession() {
        viewModelScope.launch(errorHandler) { repository.discardActiveSession() }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(errorHandler) { repository.deleteSessionFromHistory(sessionId) }
    }

    /** Most recent finished session for a given weekday — powers the "last time" comparison. */
    fun lastSessionForDay(dayIndex: Int): WorkoutSession? =
        _sessionHistory.value.firstOrNull { it.dayIndex == dayIndex }

    /**
     * What this exercise looked like the last time it was trained. Shown on the card so the answer
     * to "what did I lift last week" is already on screen instead of a trip through history.
     */
    fun previousLogFor(exerciseId: String): SessionExerciseLog? =
        _sessionHistory.value
            .firstNotNullOfOrNull { session ->
                session.exercises.firstOrNull { it.exerciseId == exerciseId && it.completedSets > 0 }
            }

    fun resetWeeklyProgress() {
        viewModelScope.launch(errorHandler) { repository.resetWeeklyProgress() }
    }

    // ── Structure editing ────────────────────────────────────────────────────

    fun updateExercise(exerciseId: String, sets: Int, reps: String, restTime: Int) {
        viewModelScope.launch(errorHandler) {
            repository.updateWorkoutStructure { days -> days.map { day ->
                day.copy(exercises = day.exercises.map { ex ->
                    if (ex.id == exerciseId) ex.copy(sets = sets.coerceIn(1, 100), reps = reps, restTime = restTime.coerceIn(0, 3600)) else ex
                })
            } }
        }
    }

    fun addExerciseFromBank(dayId: String, bankItem: com.hananel.workouttracker2.data.models.ExerciseBankItem) {
        val exercise = Exercise(id = java.util.UUID.randomUUID().toString(), nameResId = bankItem.nameResId,
            sets = bankItem.defaultSets, reps = bankItem.defaultReps, restTime = bankItem.defaultRestTime,
            hasWeight = bankItem.hasWeight, muscleGroups = bankItem.muscleGroups, equipment = bankItem.equipment)
        viewModelScope.launch(errorHandler) {
            repository.updateWorkoutStructure { days -> days.map { if (it.id == dayId) it.copy(exercises = it.exercises + exercise) else it } }
        }
    }

    fun addCustomExercise(dayId: String, name: String, sets: Int, reps: String, restTime: Int, hasWeight: Boolean, weight: Float, onSaved: () -> Unit = {}) {
        if (name.isBlank()) return
        val exercise = Exercise(id = java.util.UUID.randomUUID().toString(), nameResId = 0, customName = name.trim(), isCustom = true,
            sets = sets.coerceIn(1, 100), reps = reps, restTime = restTime.coerceIn(0, 3600), hasWeight = hasWeight)
        viewModelScope.launch(errorHandler) {
            repository.updateWorkoutStructure { days ->
                require(days.any { it.id == dayId }) { "Workout day no longer exists" }
                days.map { if (it.id == dayId) it.copy(exercises = it.exercises + exercise) else it }
            }
            if (hasWeight) repository.updateExerciseWeight(exercise.id, weight)
            onSaved()
        }
    }

    fun renameWorkoutDay(dayId: String, newName: String) {
        viewModelScope.launch(errorHandler) {
            repository.updateWorkoutStructure { days -> days.map { if (it.id == dayId) it.copy(customName = newName.trim()) else it } }
        }
    }

    fun deleteExercise(dayId: String, exerciseId: String) {
        viewModelScope.launch(errorHandler) {
            repository.updateWorkoutStructure { days -> days.map { if (it.id == dayId) it.copy(exercises = it.exercises.filterNot { ex -> ex.id == exerciseId }) else it } }
            repository.removeExerciseProgress(exerciseId)
        }
    }
}

class WorkoutViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext
    private val resources = context.resources
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WorkoutViewModel::class.java))
        return WorkoutViewModel(WorkoutRepository(appContext), resources) as T
    }
}
