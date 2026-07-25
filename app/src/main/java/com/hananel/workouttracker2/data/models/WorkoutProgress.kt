package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutProgress(
    val completedExercises: Map<String, Boolean> = emptyMap(),
    val exerciseWeights: Map<String, Float> = emptyMap(),
    val exerciseNotes: Map<String, String> = emptyMap(),
    val exerciseCompletedSets: Map<String, Int> = emptyMap(), // ⭐ KEY FEATURE
    /**
     * dayId → the ISO date that day's checkmarks belong to. Opening a workout day on a later date
     * wipes its sets, so you never walk into the gym and find an exercise already sitting at 3/5
     * from last week. Weights are deliberately not part of this — those carry over.
     */
    val dayTrainingDates: Map<String, String> = emptyMap(),
    val weekStartDate: String,
    val lastUpdateTime: Long
)

