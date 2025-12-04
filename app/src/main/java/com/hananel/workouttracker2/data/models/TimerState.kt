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
    val completedSets: Int
)

