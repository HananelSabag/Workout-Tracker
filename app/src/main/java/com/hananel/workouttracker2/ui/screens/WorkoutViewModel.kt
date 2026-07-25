package com.hananel.workouttracker2.ui.screens

import android.content.Context
import android.content.res.Resources
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

    init {
        validateAndClearStaleTimerState()
        runAutoResetIfNeeded()
        loadData()
        setInitialDay()
        startSessionWatchdog()
    }

    /**
     * Drives the live clock and closes an abandoned session.
     *
     * The workout clock starts at the first logged set and stops after ten minutes of silence,
     * backdated to the last action — so leaving the phone in the locker doesn't inflate the
     * session, and neither does forgetting to press anything on the way out.
     */
    private fun startSessionWatchdog() {
        viewModelScope.launch {
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

    private fun runAutoResetIfNeeded() {
        viewModelScope.launch {
            val prefs = repository.getUserPreferences()
            if (!prefs.autoResetWeekly) return@launch

            val progress = repository.getWorkoutProgress()
            val currentWeekStart = DateUtils.getCurrentWeekStart()
            if (progress.weekStartDate != currentWeekStart) {
                repository.resetWeeklyProgress()
            }
        }
    }

    /**
     * Clears timer state that survived a cloud-backup restore and no longer points at a real
     * exercise — otherwise the UI shows a countdown for something that doesn't exist.
     */
    private fun validateAndClearStaleTimerState() {
        viewModelScope.launch {
            val timerState = repository.getTimerState() ?: return@launch
            val workoutStructure = repository.getWorkoutStructure()
            val exerciseExists = workoutStructure.flatMap { it.exercises }.any { it.id == timerState.exerciseId }
            if (workoutStructure.isEmpty() || !exerciseExists) {
                repository.clearTimerState()
            }
        }
    }

    /**
     * Returns true if data is valid, false if corrupted (should be cleared). Exercise names that
     * come back with format placeholders mean the R ids no longer line up with the strings.
     */
    private fun isWorkoutDataValid(workoutDays: List<WorkoutDay>): Boolean {
        if (workoutDays.isEmpty()) return true
        val res = resources ?: return true
        val firstExercise = workoutDays.firstOrNull()?.exercises?.firstOrNull() ?: return true

        return try {
            val name = res.getString(firstExercise.nameResId)
            !name.contains("%") && name.length >= 3 && name.length < 100
        } catch (e: Resources.NotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun setInitialDay() {
        viewModelScope.launch {
            val prefs = repository.getUserPreferences()
            // Auto-open only applies before the user has picked a day themselves; afterwards we
            // always restore their last choice, so minimizing the app — or a theme change that
            // recreates the Activity — never jumps them to a different day mid-workout.
            _selectedDayIndex.value = if (prefs.autoOpenCurrentDay && !prefs.hasViewedAnyDay) {
                DateUtils.getCurrentDayIndex()
            } else {
                prefs.lastViewedDayIndex
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getWorkoutStructureFlow().collect { savedStructure ->
                if (isWorkoutDataValid(savedStructure)) {
                    _workoutDays.value = savedStructure
                    // Covers app launch, where the structure arrives after the initial day is set.
                    startFreshDayIfNeeded(_selectedDayIndex.value)
                } else {
                    // Corrupted — clear progress and timer state too, they're just as stale.
                    repository.clearAllProgressAndTimerState()
                    repository.saveWorkoutStructure(emptyList())
                    _workoutDays.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            repository.getWorkoutProgressFlow().collect { _workoutProgress.value = it }
        }

        viewModelScope.launch {
            repository.getTimerStateFlow().collect { _timerState.value = it }
        }

        viewModelScope.launch {
            repository.getActiveSessionFlow().collect { _activeSession.value = it }
        }

        viewModelScope.launch {
            repository.getSessionHistoryFlow().collect { _sessionHistory.value = it }
        }
    }

    fun selectDay(dayIndex: Int) {
        _selectedDayIndex.value = dayIndex
        viewModelScope.launch {
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
        viewModelScope.launch { repository.updateExerciseWeight(exerciseId, weight) }
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
            d.customName.ifBlank { resources?.runCatching { getString(d.nameResId) }?.getOrNull().orEmpty() }
        }.orEmpty()
        val exerciseName = exercise?.let { ex ->
            ex.customName.ifBlank { resources?.runCatching { getString(ex.nameResId) }?.getOrNull().orEmpty() }
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
        viewModelScope.launch {
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
            exerciseName = ctx.exerciseName.ifBlank { context.getString(exercise.nameResId) },
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
        viewModelScope.launch {
            repository.undoSet(exercise.id, exercise.sets)
            val timer = _timerState.value
            if (timer?.exerciseId == exercise.id) {
                TimerService.stopTimer(context)
            }
        }
    }

    fun toggleExerciseCompletion(exerciseId: String) {
        viewModelScope.launch {
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
        startRest(context, exercise, sessionContextFor(exercise), getExerciseCompletedSets(exercise.id))
    }

    fun skipRest(context: Context) = TimerService.skipRest(context)

    /**
     * Repairs a countdown whose process was killed mid-rest.
     *
     * A CountDownTimer doesn't survive the OS reclaiming the app, so the persisted state comes back
     * frozen at whatever second it stopped on and would never finish. The stored deadline says what
     * really happened: either the rest is already over, or there's time left worth resuming.
     */
    fun recoverTimerIfInterrupted(context: Context) {
        viewModelScope.launch {
            val state = repository.getTimerState() ?: return@launch
            if (!state.isRunning || state.isPaused || state.isRestComplete) return@launch
            if (TimerService.isRunning) return@launch // Countdown is alive; nothing to repair.

            // A state written before deadlines existed (upgrade from an older build) has no way to
            // resume, and leaving it would strand a countdown that can never reach zero.
            if (state.endsAtEpochMs <= 0L) {
                repository.saveTimerState(
                    state.copy(timeLeft = 0, isRunning = false, isRestComplete = true)
                )
                return@launch
            }

            val remaining = ((state.endsAtEpochMs - System.currentTimeMillis()) / 1000L).toInt()
            if (remaining > 0) {
                TimerService.startRest(
                    context = context,
                    exerciseName = state.exerciseName,
                    exerciseId = state.exerciseId,
                    restTime = remaining,
                    completedSets = state.completedSets,
                    totalSets = state.totalSets,
                    dayIndex = state.dayIndex,
                    dayId = state.dayId,
                    dayName = state.dayName,
                    plannedExercises = state.plannedExercises,
                    reps = state.reps,
                    totalTime = state.totalTime
                )
            } else {
                repository.saveTimerState(
                    state.copy(timeLeft = 0, isRunning = false, isRestComplete = true)
                )
            }
        }
    }

    /** Adjusts the reps recorded for the set that just finished. */
    fun setLastSetReps(exerciseId: String, reps: Int) {
        viewModelScope.launch { repository.updateLastSetReps(exerciseId, reps) }
    }

    /** Reps logged for the set the current rest belongs to, for the timer sheet's stepper. */
    fun lastLoggedReps(exerciseId: String): Int =
        _activeSession.value?.exercises
            ?.firstOrNull { it.exerciseId == exerciseId }
            ?.repsPerSet?.lastOrNull() ?: 0

    fun stopTimer(context: Context) = TimerService.stopTimer(context)

    fun pauseResumeTimer(context: Context) {
        context.startService(
            android.content.Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_PAUSE_RESUME
            }
        )
    }

    /** Completes the set the finished rest was leading up to, straight from the timer sheet. */
    fun completeSetFromTimer(context: Context) {
        val state = _timerState.value ?: return
        val exercise = _workoutDays.value.flatMap { it.exercises }.find { it.id == state.exerciseId } ?: return
        completeSet(exercise, context)
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    fun endSession(context: Context) {
        viewModelScope.launch {
            repository.endActiveSession()
            TimerService.stopTimer(context)
        }
    }

    fun discardSession() {
        viewModelScope.launch { repository.discardActiveSession() }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { repository.deleteSessionFromHistory(sessionId) }
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
        viewModelScope.launch { repository.resetWeeklyProgress() }
    }

    // ── Structure editing ────────────────────────────────────────────────────

    fun updateExercise(exerciseId: String, sets: Int, reps: String, restTime: Int) {
        viewModelScope.launch {
            val currentDays = _workoutDays.value.toMutableList()
            var updated = false

            for (i in currentDays.indices) {
                val day = currentDays[i]
                val exerciseIndex = day.exercises.indexOfFirst { it.id == exerciseId }
                if (exerciseIndex != -1) {
                    val updatedExercises = day.exercises.toMutableList()
                    updatedExercises[exerciseIndex] = updatedExercises[exerciseIndex].copy(
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

    fun addExerciseFromBank(dayId: String, bankItem: com.hananel.workouttracker2.data.models.ExerciseBankItem) {
        viewModelScope.launch {
            val currentDays = _workoutDays.value.toMutableList()
            val dayIndex = currentDays.indexOfFirst { it.id == dayId }
            if (dayIndex == -1) return@launch

            val day = currentDays[dayIndex]
            val newExercise = Exercise(
                // Timestamp suffix keeps repeated additions of the same catalog item distinct.
                id = "${bankItem.id}_${System.currentTimeMillis()}",
                nameResId = bankItem.nameResId,
                sets = bankItem.defaultSets,
                reps = bankItem.defaultReps,
                restTime = bankItem.defaultRestTime,
                hasWeight = bankItem.hasWeight,
                muscleGroups = bankItem.muscleGroups,
                equipment = bankItem.equipment
            )

            currentDays[dayIndex] = day.copy(exercises = day.exercises + newExercise)
            _workoutDays.value = currentDays
            repository.saveWorkoutStructure(currentDays)
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
            if (dayIndex == -1) return@launch

            val day = currentDays[dayIndex]
            currentDays[dayIndex] = day.copy(exercises = day.exercises.filter { it.id != exerciseId })
            _workoutDays.value = currentDays
            repository.saveWorkoutStructure(currentDays)
            repository.removeExerciseProgress(exerciseId)
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
