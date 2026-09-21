package com.hananel.workouttracker2.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hananel.workouttracker2.data.models.*
import com.hananel.workouttracker2.data.DisplayNames
import com.hananel.workouttracker2.utils.DateUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_tracker")

class WorkoutRepository(context: Context, private val store: DataStore<Preferences> = context.applicationContext.dataStore) {
    private val context = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private val WORKOUT_PROGRESS_KEY = stringPreferencesKey("workout_progress")
        private val USER_PREFERENCES_KEY = stringPreferencesKey("user_preferences")
        private val WORKOUT_STRUCTURE_KEY = stringPreferencesKey("workout_structure")
        private val TIMER_STATE_KEY = stringPreferencesKey("timer_state")
        private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session")
        private val SESSION_HISTORY_KEY = stringPreferencesKey("session_history")
    }

    // ── Workout Progress ─────────────────────────────────────────────────────

    suspend fun saveWorkoutProgress(progress: WorkoutProgress) {
        store.edit { preferences ->
            preferences[WORKOUT_PROGRESS_KEY] = json.encodeToString(progress)
        }
    }

    fun getWorkoutProgressFlow(): Flow<WorkoutProgress> = store.data.map { preferences ->
        preferences.readProgress()
    }

    suspend fun getWorkoutProgress(): WorkoutProgress = getWorkoutProgressFlow().first()

    private fun Preferences.readProgress(): WorkoutProgress =
        this[WORKOUT_PROGRESS_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<WorkoutProgress>(jsonString)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw WorkoutDataException(e)
            }
        } ?: createDefaultProgress()

    private fun createDefaultProgress(): WorkoutProgress = WorkoutProgress(
        weekStartDate = DateUtils.getCurrentWeekStart(),
        lastUpdateTime = System.currentTimeMillis()
    )

    // ── User Preferences ─────────────────────────────────────────────────────

    suspend fun saveUserPreferences(preferences: UserPreferences) {
        store.edit { prefs ->
            prefs[USER_PREFERENCES_KEY] = json.encodeToString(preferences)
        }
    }

    suspend fun updateUserPreferences(preferences: UserPreferences) = saveUserPreferences(preferences)

    fun getUserPreferencesFlow(): Flow<UserPreferences> = store.data.map { preferences ->
        preferences[USER_PREFERENCES_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<UserPreferences>(jsonString)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw WorkoutDataException(e)
            }
        } ?: UserPreferences()
    }

    suspend fun getUserPreferences(): UserPreferences = getUserPreferencesFlow().first()

    suspend fun updateUserPreferences(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs ->
            val latest = prefs[USER_PREFERENCES_KEY]?.let { json.decodeFromString<UserPreferences>(it) } ?: UserPreferences()
            prefs[USER_PREFERENCES_KEY] = json.encodeToString(transform(latest))
        }
    }

    suspend fun updateAutoOpenCurrentDay(enabled: Boolean) =
        updateUserPreferences { it.copy(autoOpenCurrentDay = enabled) }

    suspend fun updateLastViewedDay(dayIndex: Int) =
        updateUserPreferences { it.copy(lastViewedDayIndex = dayIndex.coerceIn(0, 6), hasViewedAnyDay = true) }

    // ── Workout Structure ────────────────────────────────────────────────────

    suspend fun saveWorkoutStructure(workoutDays: List<WorkoutDay>) {
        store.edit { preferences ->
            preferences[WORKOUT_STRUCTURE_KEY] = json.encodeToString(DisplayNames.prepare(workoutDays, context.resources, legacy = false))
        }
    }

    suspend fun updateWorkoutStructure(transform: (List<WorkoutDay>) -> List<WorkoutDay>) {
        store.edit { prefs ->
            val latest = prefs[WORKOUT_STRUCTURE_KEY]?.let {
                DisplayNames.prepare(json.decodeFromString<List<WorkoutDay>>(it), context.resources, legacy = true)
            }.orEmpty()
            prefs[WORKOUT_STRUCTURE_KEY] = json.encodeToString(DisplayNames.prepare(transform(latest), context.resources, legacy = false))
        }
    }

    fun getWorkoutStructureFlow(): Flow<List<WorkoutDay>> = store.data.map { preferences ->
        preferences[WORKOUT_STRUCTURE_KEY]?.let { jsonString ->
            try {
                DisplayNames.prepare(json.decodeFromString<List<WorkoutDay>>(jsonString), context.resources, legacy = true)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw WorkoutDataException(e)
            }
        } ?: emptyList()
    }

    suspend fun getWorkoutStructure(): List<WorkoutDay> = getWorkoutStructureFlow().first()

    // ── Timer State ──────────────────────────────────────────────────────────

    suspend fun saveTimerState(timerState: TimerState?) {
        store.edit { preferences ->
            if (timerState != null) {
                preferences[TIMER_STATE_KEY] = json.encodeToString(timerState)
            } else {
                preferences.remove(TIMER_STATE_KEY)
            }
        }
    }

    fun getTimerStateFlow(): Flow<TimerState?> = store.data.map { preferences ->
        preferences[TIMER_STATE_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<TimerState>(jsonString)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw WorkoutDataException(e)
            }
        }
    }

    suspend fun getTimerState(): TimerState? = getTimerStateFlow().first()

    // ── Sessions: live + history ─────────────────────────────────────────────

    fun getActiveSessionFlow(): Flow<WorkoutSession?> = store.data.map { it.readActiveSession() }

    suspend fun getActiveSession(): WorkoutSession? = getActiveSessionFlow().first()

    fun getSessionHistoryFlow(): Flow<List<WorkoutSession>> =
        store.data.map { it.readHistory() }

    suspend fun getSessionHistory(): List<WorkoutSession> = getSessionHistoryFlow().first()

    private fun Preferences.readActiveSession(): WorkoutSession? =
        this[ACTIVE_SESSION_KEY]?.let {
            try {
                json.decodeFromString<WorkoutSession>(it)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw WorkoutDataException(e)
            }
        }

    private fun Preferences.readHistory(): List<WorkoutSession> =
        this[SESSION_HISTORY_KEY]?.let {
            try {
                json.decodeFromString<List<WorkoutSession>>(it)
            } catch (e: kotlinx.serialization.SerializationException) {
                throw WorkoutDataException(e)
            }
        } ?: emptyList()

    private fun MutablePreferences.writeActiveSession(session: WorkoutSession?) {
        if (session == null) remove(ACTIVE_SESSION_KEY)
        else this[ACTIVE_SESSION_KEY] = json.encodeToString(session)
    }

    private fun MutablePreferences.archive(session: WorkoutSession, endTime: Long) {
        // Sessions where nothing actually happened are noise — drop them instead of polluting history.
        if (session.exercises.none { it.completedSets > 0 }) return
        val finished = session.copy(endTime = endTime.coerceAtLeast(session.startTime))
        val updated = (listOf(finished) + readHistory()).take(WorkoutSession.MAX_HISTORY)
        this[SESSION_HISTORY_KEY] = json.encodeToString(updated)
    }

    /**
     * Closes the live session if it has been idle past the timeout, backdating the end to the last
     * recorded action. Safe to call on every app start / resume — it's a no-op otherwise.
     * Returns true when a session was actually closed.
     */
    suspend fun finalizeStaleSession(now: Long = System.currentTimeMillis()): Boolean {
        var closed = false
        store.edit { prefs ->
            val active = prefs.readActiveSession() ?: return@edit
            if (active.isStaleAt(now)) {
                prefs.archive(active, active.lastActivityTime)
                prefs.writeActiveSession(null)
                prefs.remove(TIMER_STATE_KEY)
                closed = true
            }
        }
        return closed
    }

    /** Explicit "I'm done" — ends the session at [now]. */
    suspend fun endActiveSession(now: Long = System.currentTimeMillis()) {
        store.edit { prefs ->
            val active = prefs.readActiveSession() ?: return@edit
            prefs.archive(active, now)
            prefs.writeActiveSession(null)
            prefs.remove(TIMER_STATE_KEY)
        }
    }

    /** Throws the live session away without recording it. */
    suspend fun discardActiveSession() {
        store.edit { prefs -> prefs.writeActiveSession(null) }
    }

    suspend fun deleteSessionFromHistory(sessionId: String) {
        store.edit { prefs ->
            val updated = prefs.readHistory().filterNot { it.id == sessionId }
            prefs[SESSION_HISTORY_KEY] = json.encodeToString(updated)
        }
    }

    /**
     * Returns the session that should receive [ctx]'s activity, rotating to a fresh one when the
     * current one went stale or when the user moved to a different workout day.
     *
     * Deliberately does NOT rotate on a calendar-date change: a late workout that crosses midnight
     * is one session, and a session left open from a previous day is already stale by definition.
     */
    private fun MutablePreferences.sessionFor(ctx: SessionContext, now: Long): WorkoutSession {
        val active = readActiveSession()
        if (active != null) {
            val sameWorkout = active.dayIndex == ctx.dayIndex &&
                    (ctx.dayId.isBlank() || active.dayId.isBlank() || active.dayId == ctx.dayId)
            if (!active.isStaleAt(now) && sameWorkout) return active
            archive(active, active.lastActivityTime)
        }
        return WorkoutSession(
            id = UUID.randomUUID().toString(),
            dayIndex = ctx.dayIndex,
            dayId = ctx.dayId,
            dayName = ctx.dayName,
            dateIso = DateUtils.toIso(now),
            startTime = now,
            lastActivityTime = now,
            plannedExercises = ctx.plannedExercises
        )
    }

    private fun WorkoutSession.upsert(
        exerciseId: String,
        now: Long,
        transform: (SessionExerciseLog) -> SessionExerciseLog,
        seed: () -> SessionExerciseLog
    ): WorkoutSession {
        val existing = exercises.firstOrNull { it.exerciseId == exerciseId }
        val updatedEntry = transform(existing ?: seed())
        val updatedList =
            if (existing == null) exercises + updatedEntry
            else exercises.map { if (it.exerciseId == exerciseId) updatedEntry else it }
        return copy(exercises = updatedList, lastActivityTime = now)
    }

    // ── Set tracking (single source of truth) ────────────────────────────────

    /**
     * Advances one set and records it in the live session, atomically.
     *
     * This is the *only* place sets move forward. The previous design split the count between the
     * timer service and the UI, which is why finishing the last set sometimes left the exercise
     * un-completed: the rest countdown owned the counter and its "last set" branch could be skipped
     * (user stopped the timer, service was killed, rest was zero). Here the exercise is marked
     * complete in the same write that reaches the final set, so the two can never disagree.
     */
    suspend fun completeSet(
        exerciseId: String,
        totalSets: Int,
        ctx: SessionContext,
        reps: Int = 0,
        now: Long = System.currentTimeMillis()
    ): SetResult {
        var result = SetResult(0, false)
        store.edit { prefs ->
            val progress = prefs.readProgress()
            val previous = progress.exerciseCompletedSets[exerciseId] ?: 0
            if (previous >= totalSets.coerceAtLeast(1)) {
                // Nothing left to log: a repeated tap after the final set, or the plan was edited
                // down to (or below) the sets already done. Never count another set — but in the
                // second case the exercise was never flagged, so flag it now or the button is dead.
                if (progress.completedExercises[exerciseId] != true) {
                    prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                        progress.copy(
                            completedExercises = progress.completedExercises + (exerciseId to true),
                            lastUpdateTime = now
                        )
                    )
                    // Same rule as a weight edit: never revive a session that already timed out.
                    prefs.readActiveSession()?.takeIf { !it.isStaleAt(now) }?.let { active ->
                        prefs.writeActiveSession(
                            active.copy(
                                exercises = active.exercises.map {
                                    if (it.exerciseId == exerciseId) it.copy(completed = true, plannedSets = totalSets) else it
                                },
                                lastActivityTime = now
                            )
                        )
                    }
                }
                result = SetResult(previous, true)
                return@edit
            }
            val next = (previous + 1).coerceAtMost(totalSets.coerceAtLeast(1))
            val isComplete = next >= totalSets
            val weight = progress.exerciseWeights[exerciseId] ?: 0f

            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets + (exerciseId to next),
                    completedExercises = progress.completedExercises + (exerciseId to isComplete),
                    // Claim the day for today so the fresh-day reset leaves this workout alone.
                    dayTrainingDates = if (ctx.dayId.isBlank()) progress.dayTrainingDates
                    else progress.dayTrainingDates + (ctx.dayId to DateUtils.toIso(now)),
                    lastUpdateTime = now
                )
            )

            val session = prefs.sessionFor(ctx, now).upsert(
                exerciseId = exerciseId,
                now = now,
                transform = { entry ->
                    entry.copy(
                        name = ctx.exerciseName.ifBlank { entry.name },
                        plannedSets = totalSets,
                        reps = ctx.reps.ifBlank { entry.reps },
                        completedSets = next,
                        weight = weight,
                        completed = isComplete,
                        firstSetTime = if (entry.firstSetTime == 0L) now else entry.firstSetTime,
                        lastSetTime = now,
                        repsPerSet = (entry.repsPerSet + reps.coerceIn(0, 999)).takeLast(next)
                    )
                },
                seed = {
                    SessionExerciseLog(
                        exerciseId = exerciseId,
                        name = ctx.exerciseName,
                        plannedSets = totalSets,
                        reps = ctx.reps
                    )
                }
            )
            prefs.writeActiveSession(session)
            result = SetResult(next, isComplete)
        }
        return result
    }

    /** Corrects the rep count of the set just logged, from the rest timer. */
    suspend fun updateLastSetReps(exerciseId: String, reps: Int) {
        store.edit { prefs ->
            val active = prefs.readActiveSession() ?: return@edit
            prefs.writeActiveSession(
                active.copy(
                    exercises = active.exercises.map { entry ->
                        if (entry.exerciseId != exerciseId || entry.repsPerSet.isEmpty()) entry
                        else entry.copy(
                            repsPerSet = entry.repsPerSet.dropLast(1) + reps.coerceIn(0, 999)
                        )
                    }
                )
            )
        }
    }

    /**
     * Wipes a workout day's checkmarks when they belong to an earlier date, so every session starts
     * from zero. Skipped while that day has a live session — a workout running past midnight must
     * not reset itself out from under the user.
     *
     * Returns true if anything was cleared.
     */
    suspend fun startFreshDayIfNeeded(
        dayId: String,
        exerciseIds: List<String>,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (dayId.isBlank()) return false
        var cleared = false
        store.edit { prefs ->
            val today = DateUtils.toIso(now)
            val progress = prefs.readProgress()
            if (progress.dayTrainingDates[dayId] == today) return@edit

            val active = prefs.readActiveSession()
            if (active != null && active.dayId == dayId && !active.isStaleAt(now)) return@edit

            val hasProgress = exerciseIds.any {
                (progress.exerciseCompletedSets[it] ?: 0) > 0 || progress.completedExercises[it] == true
            }

            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets - exerciseIds.toSet(),
                    completedExercises = progress.completedExercises - exerciseIds.toSet(),
                    dayTrainingDates = progress.dayTrainingDates + (dayId to today),
                    lastUpdateTime = now
                )
            )
            cleared = hasProgress
        }
        return cleared
    }

    /** Steps one set back. Used by the card's undo affordance. */
    suspend fun undoSet(
        exerciseId: String,
        totalSets: Int,
        now: Long = System.currentTimeMillis()
    ): SetResult {
        var result = SetResult(0, false)
        store.edit { prefs ->
            val progress = prefs.readProgress()
            val previous = progress.exerciseCompletedSets[exerciseId] ?: 0
            val next = (previous - 1).coerceAtLeast(0)
            val isComplete = totalSets > 0 && next >= totalSets

            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets + (exerciseId to next),
                    completedExercises = progress.completedExercises + (exerciseId to isComplete),
                    lastUpdateTime = now
                )
            )

            val active = prefs.readActiveSession()
            if (active != null) {
                prefs.writeActiveSession(
                    active.copy(
                        exercises = active.exercises.map {
                            if (it.exerciseId == exerciseId)
                                it.copy(
                                    completedSets = next,
                                    completed = isComplete,
                                    repsPerSet = it.repsPerSet.dropLast(1)
                                )
                            else it
                        },
                        lastActivityTime = now
                    )
                )
            }
            result = SetResult(next, isComplete)
        }
        return result
    }

    /**
     * Marks an exercise done (or undone) in one shot and mirrors it into the session log, so a
     * manually-ticked exercise still shows up in history.
     */
    suspend fun setExerciseCompletion(
        exerciseId: String,
        completed: Boolean,
        totalSets: Int,
        ctx: SessionContext,
        now: Long = System.currentTimeMillis()
    ) {
        store.edit { prefs ->
            val progress = prefs.readProgress()
            val sets = if (completed) totalSets else 0
            val weight = progress.exerciseWeights[exerciseId] ?: 0f

            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets + (exerciseId to sets),
                    completedExercises = progress.completedExercises + (exerciseId to completed),
                    dayTrainingDates = if (!completed || ctx.dayId.isBlank()) progress.dayTrainingDates
                    else progress.dayTrainingDates + (ctx.dayId to DateUtils.toIso(now)),
                    lastUpdateTime = now
                )
            )

            if (completed) {
                val session = prefs.sessionFor(ctx, now).upsert(
                    exerciseId = exerciseId,
                    now = now,
                    transform = {
                        it.copy(
                            name = ctx.exerciseName.ifBlank { it.name },
                            plannedSets = totalSets,
                            reps = ctx.reps.ifBlank { it.reps },
                            completedSets = totalSets,
                            weight = weight,
                            completed = true,
                            firstSetTime = if (it.firstSetTime == 0L) now else it.firstSetTime,
                            lastSetTime = now
                        )
                    },
                    seed = {
                        SessionExerciseLog(
                            exerciseId = exerciseId,
                            name = ctx.exerciseName,
                            plannedSets = totalSets,
                            reps = ctx.reps
                        )
                    }
                )
                prefs.writeActiveSession(session)
            } else {
                val active = prefs.readActiveSession()
                if (active != null) {
                    prefs.writeActiveSession(
                        active.copy(
                            exercises = active.exercises.map {
                                if (it.exerciseId == exerciseId)
                                    it.copy(completedSets = 0, completed = false, repsPerSet = emptyList())
                                else it
                            },
                            lastActivityTime = now
                        )
                    )
                }
            }
        }
    }

    suspend fun updateExerciseWeight(
        exerciseId: String,
        weight: Float,
        now: Long = System.currentTimeMillis()
    ) {
        val safeWeight = weight.takeIf { it.isFinite() }?.coerceIn(0f, 10000f) ?: 0f
        store.edit { prefs ->
            val progress = prefs.readProgress()
            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseWeights = progress.exerciseWeights + (exerciseId to safeWeight),
                    lastUpdateTime = now
                )
            )
            // Keep the live session's logged weight in sync, but never *open* a session just
            // because someone typed a number — a session starts when a set is actually done.
            val active = prefs.readActiveSession()
            if (active != null && !active.isStaleAt(now)) {
                prefs.writeActiveSession(
                    active.copy(
                        exercises = active.exercises.map {
                            if (it.exerciseId == exerciseId) it.copy(weight = safeWeight) else it
                        },
                        lastActivityTime = now
                    )
                )
            }
        }
    }

    /**
     * Legacy entry point still used by the timer service. Kept so an in-flight rest countdown from
     * an older install can't crash, and so notification actions converge on the same rules.
     */
    suspend fun updateExerciseCompletedSets(exerciseId: String, completedSets: Int, totalSets: Int) {
        val now = System.currentTimeMillis()
        store.edit { prefs ->
            val progress = prefs.readProgress()
            val isComplete = totalSets > 0 && completedSets >= totalSets
            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets + (exerciseId to completedSets),
                    completedExercises = progress.completedExercises + (exerciseId to isComplete),
                    lastUpdateTime = now
                )
            )
            val active = prefs.readActiveSession()
            if (active != null) {
                prefs.writeActiveSession(
                    active.copy(
                        exercises = active.exercises.map {
                            if (it.exerciseId == exerciseId)
                                it.copy(completedSets = completedSets, completed = isComplete, lastSetTime = now)
                            else it
                        },
                        lastActivityTime = now
                    )
                )
            }
        }
    }

    suspend fun toggleExerciseCompletion(exerciseId: String) {
        val progress = getWorkoutProgress()
        val currentlyCompleted = progress.completedExercises[exerciseId] ?: false
        saveWorkoutProgress(
            progress.copy(
                completedExercises = progress.completedExercises + (exerciseId to !currentlyCompleted),
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetWeeklyProgress() {
        store.edit { prefs ->
            val current = prefs.readProgress()
            prefs.remove(TIMER_STATE_KEY)
            // A running session belongs to the week being wiped — close it out first so its work
            // still lands in history instead of vanishing.
            prefs.readActiveSession()?.let { active ->
                prefs.archive(active, active.lastActivityTime)
                prefs.writeActiveSession(null)
            }
            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                WorkoutProgress(
                    completedExercises = emptyMap(),
                    exerciseWeights = current.exerciseWeights, // Keep weights
                    exerciseNotes = current.exerciseNotes,     // Keep notes
                    exerciseCompletedSets = emptyMap(),
                    weekStartDate = DateUtils.getCurrentWeekStart(),
                    lastUpdateTime = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeExerciseProgress(exerciseId: String) {
        store.edit { prefs ->
            val progress = prefs.readProgress()
            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(progress.copy(
                completedExercises = progress.completedExercises - exerciseId,
                exerciseWeights = progress.exerciseWeights - exerciseId,
                exerciseNotes = progress.exerciseNotes - exerciseId,
                exerciseCompletedSets = progress.exerciseCompletedSets - exerciseId,
                lastUpdateTime = System.currentTimeMillis()
            ))
            val timer = prefs[TIMER_STATE_KEY]?.let { json.decodeFromString<TimerState>(it) }
            if (timer?.exerciseId == exerciseId) prefs.remove(TIMER_STATE_KEY)
        }
    }

    /**
     * Clears ALL progress — weights and notes included — plus timer and live-session state, without
     * reading what is stored. History is preserved. Test setup only: installing a program goes
     * through [resetWeeklyProgress], which keeps weights and archives a running session instead of
     * dropping it.
     */
    suspend fun clearAllProgressAndTimerState() {
        store.edit { preferences ->
            preferences.remove(TIMER_STATE_KEY)
            preferences.remove(ACTIVE_SESSION_KEY)
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

    suspend fun clearTimerState() {
        store.edit { preferences -> preferences.remove(TIMER_STATE_KEY) }
    }
}

/** Everything the repository needs to attribute an action to the right session entry. */
data class SessionContext(
    val dayIndex: Int,
    val dayId: String = "",
    val dayName: String = "",
    val plannedExercises: Int = 0,
    val exerciseName: String = "",
    val reps: String = ""
)

data class SetResult(val completedSets: Int, val exerciseCompleted: Boolean)

class WorkoutDataException(cause: Throwable) : IllegalStateException("Saved workout data could not be read; the original data was retained.", cause)
