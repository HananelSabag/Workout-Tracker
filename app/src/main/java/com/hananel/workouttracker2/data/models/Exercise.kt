package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

fun Exercise.getDisplayName(context: android.content.Context): String =
    com.hananel.workouttracker2.data.DisplayNames.exerciseName(this, context.resources)

@Serializable
data class Exercise(
    val id: String,
    val nameResId: Int,
    val nameKey: String = "", // Stable across builds; legacy nameResId remains decodable.
    val customName: String = "",
    val isCustom: Boolean = false,
    val sets: Int,
    val reps: String,
    val restTime: Int, // seconds
    val hasWeight: Boolean = true,
    val muscleGroups: List<String> = emptyList(),
    val equipment: String = ""
)

