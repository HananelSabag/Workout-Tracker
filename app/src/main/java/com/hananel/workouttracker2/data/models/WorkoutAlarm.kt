package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutAlarm(
    val id: String,
    val time: String, // HH:mm format
    val daysOfWeek: Set<Int>, // 0=Sunday, 6=Saturday
    val isEnabled: Boolean = true,
    val customMessage: String = ""
)

