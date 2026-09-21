package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

/**
 * The live rest countdown.
 *
 * The timer no longer owns the set counter — it only counts down the rest that follows a set the
 * repository has already recorded. [completedSets] is therefore a display snapshot, not the source
 * of truth. The day fields ride along so notification actions can log the next set without the app
 * being open.
 */
@Serializable
data class TimerState(
    val timeLeft: Int,
    val totalTime: Int,
    val isRunning: Boolean,
    val exerciseName: String,
    val exerciseId: String,
    val currentSet: Int,
    val totalSets: Int,
    val completedSets: Int,
    // True when the rest countdown finished and we're waiting for the user to start the next set.
    // Older persisted states won't have this field — kotlinx-serialization defaults to false, then we
    // can also derive it from (!isRunning && timeLeft == 0) for backwards compatibility.
    val isRestComplete: Boolean = false,
    val isPaused: Boolean = false,
    /**
     * Wall-clock instant the countdown is due to end. A CountDownTimer dies with its process, so
     * without this a rest interrupted by the OS reopens frozen at whatever second it was on and
     * never finishes. Recovery compares this against the clock instead of trusting [timeLeft].
     */
    val endsAtEpochMs: Long = 0L,
    // Session attribution — defaults keep states persisted by older versions decodable.
    val dayIndex: Int = -1,
    val dayId: String = "",
    val dayName: String = "",
    val plannedExercises: Int = 0,
    val reps: String = ""
) {
    fun remainingSeconds(now: Long = System.currentTimeMillis()): Int = when {
        isRestComplete -> 0
        isPaused -> timeLeft.coerceIn(0, 3600)
        endsAtEpochMs <= 0L -> 0
        else -> ((endsAtEpochMs - now + 999L) / 1000L).coerceIn(0L, 3600L).toInt()
    }

    /** The set the user is about to perform once this rest ends. */
    val nextSet: Int get() = (completedSets + 1).coerceAtMost(totalSets)
}
