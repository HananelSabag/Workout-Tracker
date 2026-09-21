package com.hananel.workouttracker2.data

import android.content.res.Resources
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.*

/** Resolves only known name resources. A missing label never deletes a plan. */
object DisplayNames {
    private val resourceIds by lazy { R.string::class.java.fields.associate { it.name to it.getInt(null) } }
    private val defaultDays by lazy { DefaultData.getDefaultWorkoutStructure().associateBy { it.id } }
    private val defaultExercises by lazy { defaultDays.values.flatMap { it.exercises }.associateBy { it.id } }
    private val catalog by lazy { ExerciseBank.getAllExercises() }
    private fun key(id: Int, resources: Resources): String =
        runCatching { resources.getResourceEntryName(id) }.getOrDefault("")
    private fun exerciseKey(key: String) = key.matches(Regex("exercise[0-9]+|exercise_(chest|back|shoulders|arms|legs|core|cardio)_.+"))
    private fun dayKey(key: String) = key.startsWith("workout_") || key.startsWith("day_")

    fun prepare(days: List<WorkoutDay>, resources: Resources, legacy: Boolean): List<WorkoutDay> = days.map { day ->
        // Numeric exercise IDs are the original default program's stable identity. They also
        // recover data from pre-2.2 builds whose R integers differ from the shipped 2.2 table.
        val originalDay = defaultDays[day.id]?.takeIf { original ->
            day.exercises.any { ex -> original.exercises.any { it.id == ex.id } }
        }
        val dayKey = day.nameKey.ifBlank {
            if (legacy && originalDay != null) key(originalDay.nameResId, resources)
            else (if (legacy) legacyResourceNames[day.nameResId] else null) ?: key(day.nameResId, resources)
        }
        day.copy(nameKey = dayKey, nameResId = resourceIds[dayKey] ?: 0, exercises = day.exercises.map { ex ->
            val originalId = defaultExercises[ex.id]?.nameResId
                ?: catalog.firstOrNull { ex.id == it.id || ex.id.startsWith("${it.id}_") }?.nameResId
            val name = ex.nameKey.ifBlank {
                if (legacy && originalId != null) key(originalId, resources)
                else (if (legacy) legacyResourceNames[ex.nameResId] else null) ?: key(ex.nameResId, resources)
            }
            ex.copy(nameKey = name, nameResId = resourceIds[name] ?: 0, sets = ex.sets.coerceIn(1, 100), restTime = ex.restTime.coerceIn(0, 3600))
        })
    }

    fun exerciseName(ex: Exercise, resources: Resources?): String {
        if (ex.customName.isNotBlank()) return ex.customName
        if (resources == null) return ex.id
        val name = ex.nameKey.ifBlank { key(ex.nameResId, resources) }
        val id = resourceIds[name]?.takeIf { exerciseKey(name) } ?: R.string.unnamed_exercise
        return resources.getString(id)
    }

    fun dayName(day: WorkoutDay, resources: Resources?): String {
        if (day.customName.isNotBlank()) return day.customName
        if (resources == null) return day.id
        val name = day.nameKey.ifBlank { key(day.nameResId, resources) }
        val id = resourceIds[name]?.takeIf { dayKey(name) } ?: R.string.workout
        return resources.getString(id)
    }
}
