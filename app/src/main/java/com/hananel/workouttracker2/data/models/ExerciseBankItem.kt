package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseBankItem(
    val id: String,
    val nameResId: Int,
    val muscleGroups: List<String>,  // e.g., "Chest", "Back", "Shoulders"
    val equipment: String,            // e.g., "Dumbbells", "Cable", "Machine"
    val category: ExerciseCategory,   // Anaerobic, Aerobic
    val defaultSets: Int = 4,
    val defaultReps: String = "10-12",
    val defaultRestTime: Int = 60,
    val hasWeight: Boolean = true
)

enum class ExerciseCategory {
    ANAEROBIC, AEROBIC
}

enum class MuscleGroup(val displayNameResId: Int) {
    CHEST(com.hananel.workouttracker2.R.string.muscle_chest),
    BACK(com.hananel.workouttracker2.R.string.muscle_back),
    SHOULDERS(com.hananel.workouttracker2.R.string.muscle_shoulders),
    ARMS(com.hananel.workouttracker2.R.string.muscle_arms),
    LEGS(com.hananel.workouttracker2.R.string.muscle_legs),
    CORE(com.hananel.workouttracker2.R.string.muscle_core),
    CARDIO(com.hananel.workouttracker2.R.string.muscle_cardio)
}




