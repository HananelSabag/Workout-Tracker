package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val nameResId: Int,
    val customName: String = "",
    val isCustom: Boolean = false,
    val sets: Int,
    val reps: String,
    val restTime: Int, // seconds
    val hasWeight: Boolean = true,
    val muscleGroups: List<String> = emptyList(),
    val equipment: String = ""
)

