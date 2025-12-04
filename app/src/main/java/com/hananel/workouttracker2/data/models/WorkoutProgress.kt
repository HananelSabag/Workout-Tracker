package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutProgress(
    val completedExercises: Map<String, Boolean> = emptyMap(),
    val exerciseWeights: Map<String, Float> = emptyMap(),
    val exerciseNotes: Map<String, String> = emptyMap(),
    val exerciseCompletedSets: Map<String, Int> = emptyMap(), // ⭐ KEY FEATURE
    val weekStartDate: String,
    val lastUpdateTime: Long
)

