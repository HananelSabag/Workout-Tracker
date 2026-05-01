package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

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
    val isRestComplete: Boolean = false
)

