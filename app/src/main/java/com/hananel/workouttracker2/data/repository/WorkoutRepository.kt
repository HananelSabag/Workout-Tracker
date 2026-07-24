package com.hananel.workouttracker2.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import java.util.UUID

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
        private val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session")
        private val SESSION_HISTORY_KEY = stringPreferencesKey("session_history")
    }

    // ── Workout Progress ─────────────────────────────────────────────────────

    suspend fun saveWorkoutProgress(progress: WorkoutProgress) {
        context.dataStore.edit { preferences ->
            preferences[WORKOUT_PROGRESS_KEY] = json.encodeToString(progress)
        }
    }

    fun getWorkoutProgressFlow(): Flow<WorkoutProgress> = context.dataStore.data.map { preferences ->
        preferences.readProgress()
    }

    suspend fun getWorkoutProgress(): WorkoutProgress = getWorkoutProgressFlow().first()

    private fun Preferences.readProgress(): WorkoutProgress =
        this[WORKOUT_PROGRESS_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<WorkoutProgress>(jsonString)
            } catch (e: Exception) {
                createDefaultProgress()
            }
        } ?: createDefaultProgress()

    private fun createDefaultProgress(): WorkoutProgress = WorkoutProgress(
        weekStartDate = DateUtils.getCurrentWeekStart(),
        lastUpdateTime = System.currentTimeMillis()
    )

    // ── User Preferences ─────────────────────────────────────────────────────

    suspend fun saveUserPreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[USER_PREFERENCES_KEY] = json.encodeToString(preferences)
        }
    }

    suspend fun updateUserPreferences(preferences: UserPreferences) = saveUserPreferences(preferences)

    fun getUserPreferencesFlow(): Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        preferences[USER_PREFERENCES_KEY]?.let { jsonString ->
            try {
                json.decodeFromString<UserPreferences>(jsonString)
            } catch (e: Exception) {
                UserPreferences()
            }
        } ?: UserPreferences()
    }

    suspend fun getUserPreferences(): UserPreferences = getUserPreferencesFlow().first()

    suspend fun updateAutoOpenCurrentDay(enabled: Boolean) {
        saveUserPreferences(getUserPreferences().copy(autoOpenCurrentDay = enabled))
    }

    suspend fun updateLastViewedDay(dayIndex: Int) {
        saveUserPreferences(
            getUserPreferences().copy(lastViewedDayIndex = dayIndex, hasViewedAnyDay = true)
        )
    }

    // ── Workout Structure ────────────────────────────────────────────────────

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

    suspend fun getWorkoutStructure(): List<WorkoutDay> = getWorkoutStructureFlow().first()

    // ── Timer State ──────────────────────────────────────────────────────────

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

    suspend fun getTimerState(): TimerState? = getTimerStateFlow().first()

    // ── Sessions: live + history ─────────────────────────────────────────────

    fun getActiveSessionFlow(): Flow<WorkoutSession?> = context.dataStore.data.map { it.readActiveSession() }

    suspend fun getActiveSession(): WorkoutSession? = getActiveSessionFlow().first()

    fun getSessionHistoryFlow(): Flow<List<WorkoutSession>> =
        context.dataStore.data.map { it.readHistory() }

    suspend fun getSessionHistory(): List<WorkoutSession> = getSessionHistoryFlow().first()

    private fun Preferences.readActiveSession(): WorkoutSession? =
        this[ACTIVE_SESSION_KEY]?.let {
            try {
                json.decodeFromString<WorkoutSession>(it)
            } catch (e: Exception) {
                null
            }
        }

    private fun Preferences.readHistory(): List<WorkoutSession> =
        this[SESSION_HISTORY_KEY]?.let {
            try {
                json.decodeFromString<List<WorkoutSession>>(it)
            } catch (e: Exception) {
                emptyList()
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
        context.dataStore.edit { prefs ->
            val active = prefs.readActiveSession() ?: return@edit
            if (active.isStaleAt(now)) {
                prefs.archive(active, active.lastActivityTime)
                prefs.writeActiveSession(null)
                closed = true
            }
        }
        return closed
    }

    /** Explicit "I'm done" — ends the session at [now]. */
    suspend fun endActiveSession(now: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            val active = prefs.readActiveSession() ?: return@edit
            prefs.archive(active, now)
            prefs.writeActiveSession(null)
        }
    }

    /** Throws the live session away without recording it. */
    suspend fun discardActiveSession() {
        context.dataStore.edit { prefs -> prefs.writeActiveSession(null) }
    }

    suspend fun deleteSessionFromHistory(sessionId: String) {
        context.dataStore.edit { prefs ->
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
        now: Long = System.currentTimeMillis()
    ): SetResult {
        var result = SetResult(0, false)
        context.dataStore.edit { prefs ->
            val progress = prefs.readProgress()
            val previous = progress.exerciseCompletedSets[exerciseId] ?: 0
            val next = (previous + 1).coerceAtMost(totalSets.coerceAtLeast(1))
            val isComplete = next >= totalSets
            val weight = progress.exerciseWeights[exerciseId] ?: 0f

            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets + (exerciseId to next),
                    completedExercises = progress.completedExercises + (exerciseId to isComplete),
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
            result = SetResult(next, isComplete)
        }
        return result
    }

    /** Steps one set back. Used by the card's undo affordance. */
    suspend fun undoSet(
        exerciseId: String,
        totalSets: Int,
        now: Long = System.currentTimeMillis()
    ): SetResult {
        var result = SetResult(0, false)
        context.dataStore.edit { prefs ->
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
                                it.copy(completedSets = next, completed = isComplete)
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
        context.dataStore.edit { prefs ->
            val progress = prefs.readProgress()
            val sets = if (completed) totalSets else 0
            val weight = progress.exerciseWeights[exerciseId] ?: 0f

            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseCompletedSets = progress.exerciseCompletedSets + (exerciseId to sets),
                    completedExercises = progress.completedExercises + (exerciseId to completed),
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
                                    it.copy(completedSets = 0, completed = false)
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
        context.dataStore.edit { prefs ->
            val progress = prefs.readProgress()
            prefs[WORKOUT_PROGRESS_KEY] = json.encodeToString(
                progress.copy(
                    exerciseWeights = progress.exerciseWeights + (exerciseId to weight),
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
                            if (it.exerciseId == exerciseId) it.copy(weight = weight) else it
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
        context.dataStore.edit { prefs ->
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
        context.dataStore.edit { prefs ->
            val current = prefs.readProgress()
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
        val progress = getWorkoutProgress()
        saveWorkoutProgress(
            progress.copy(
                completedExercises = progress.completedExercises - exerciseId,
                exerciseWeights = progress.exerciseWeights - exerciseId,
                exerciseNotes = progress.exerciseNotes - exerciseId,
                exerciseCompletedSets = progress.exerciseCompletedSets - exerciseId,
                lastUpdateTime = System.currentTimeMillis()
            )
        )
    }

    /**
     * Clears ALL progress and timer state. Used when installing a new workout program, and when
     * corrupt data is detected after a cloud-backup restore. History is deliberately preserved —
     * past workouts stay valid even when the current program is rebuilt.
     */
    suspend fun clearAllProgressAndTimerState() {
        context.dataStore.edit { preferences ->
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
        context.dataStore.edit { preferences -> preferences.remove(TIMER_STATE_KEY) }
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
