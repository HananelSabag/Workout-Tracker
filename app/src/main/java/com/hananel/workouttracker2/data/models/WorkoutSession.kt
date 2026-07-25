package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

/**
 * A single training session — one visit to the gym.
 *
 * A session opens implicitly on the first real action of a workout day (completing a set, logging a
 * weight, marking an exercise done) and closes either explicitly ("סיים אימון") or automatically
 * after [IDLE_TIMEOUT_MS] with no activity. On an idle close the end time is rolled back to
 * [lastActivityTime], so a session the user walked away from is recorded as ending when they
 * actually stopped training — not ten minutes later, and not whenever the app happened to reopen.
 */
@Serializable
data class WorkoutSession(
    val id: String,
    val dayIndex: Int,
    val dayId: String = "",
    val dayName: String = "",
    val dateIso: String,
    val startTime: Long,
    val lastActivityTime: Long,
    val endTime: Long? = null,
    val plannedExercises: Int = 0,
    val exercises: List<SessionExerciseLog> = emptyList()
) {
    val isActive: Boolean get() = endTime == null

    /** Elapsed training time. For a live session this grows with every recorded action. */
    val durationMs: Long get() = ((endTime ?: lastActivityTime) - startTime).coerceAtLeast(0L)

    val totalSets: Int get() = exercises.sumOf { it.completedSets }

    val completedExerciseCount: Int get() = exercises.count { it.completed }

    val touchedExerciseCount: Int get() = exercises.count { it.completedSets > 0 }

    /** kg × sets, summed. A rough but useful "how much did I move today" number. */
    val totalVolume: Float
        get() = exercises.sumOf { (it.completedSets * it.weight).toDouble() }.toFloat()

    fun isStaleAt(now: Long): Boolean = isActive && now - lastActivityTime >= IDLE_TIMEOUT_MS

    companion object {
        /** No set, no weight, nothing for 10 minutes → the workout is over. */
        const val IDLE_TIMEOUT_MS = 10 * 60 * 1000L
        const val MAX_HISTORY = 120
    }
}

/**
 * One exercise inside a session. [name] is the *resolved* display name captured when the entry was
 * created — resource IDs shift between builds and a restored cloud backup can point them at
 * garbage, so history keeps plain text rather than an R id.
 */
@Serializable
data class SessionExerciseLog(
    val exerciseId: String,
    val name: String,
    val plannedSets: Int,
    val reps: String = "",
    val completedSets: Int = 0,
    val weight: Float = 0f,
    val completed: Boolean = false,
    val firstSetTime: Long = 0L,
    val lastSetTime: Long = 0L,
    /**
     * Reps actually performed, one entry per completed set. Pre-filled from the plan when a set is
     * logged and adjustable from the rest timer, so "what did I actually do last time" is answerable
     * without the user having to type anything during a workout.
     */
    val repsPerSet: List<Int> = emptyList()
) {
    val totalReps: Int get() = repsPerSet.sum()
}
