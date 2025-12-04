package com.hananel.workouttracker2.data.models

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDay(
    val id: String,
    val nameResId: Int,
    val customName: String = "",
    val dayIndex: Int, // 0=Sunday, 1=Monday...
    val exercises: List<Exercise>,
    val targetMuscleGroups: List<String> = emptyList()
)

fun WorkoutDay.getDisplayName(context: Context): String {
    return if (customName.isNotBlank()) {
        customName
    } else {
        context.getString(nameResId)
    }
}

