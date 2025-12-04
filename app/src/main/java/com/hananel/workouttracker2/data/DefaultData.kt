package com.hananel.workouttracker2.data

import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.Exercise
import com.hananel.workouttracker2.data.models.WorkoutDay

object DefaultData {
    
    fun getDefaultWorkoutStructure(): List<WorkoutDay> {
        return listOf(
            // Day 0 - Sunday: Chest & Shoulders
            WorkoutDay(
                id = "day_0",
                nameResId = R.string.workout_chest_shoulders,
                dayIndex = 0,
                exercises = listOf(
                    Exercise(
                        id = "1",
                        nameResId = R.string.exercise1,
                        sets = 5,
                        reps = "10-12",
                        restTime = 60,
                        hasWeight = true,
                        muscleGroups = listOf("Chest"),
                        equipment = "Dumbbells"
                    ),
                    Exercise(
                        id = "2",
                        nameResId = R.string.exercise2,
                        sets = 4,
                        reps = "12-15",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Chest"),
                        equipment = "Cable"
                    ),
                    Exercise(
                        id = "3",
                        nameResId = R.string.exercise3,
                        sets = 4,
                        reps = "15-20",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Chest"),
                        equipment = "Machine"
                    ),
                    Exercise(
                        id = "4",
                        nameResId = R.string.exercise4,
                        sets = 4,
                        reps = "10-12",
                        restTime = 60,
                        hasWeight = true,
                        muscleGroups = listOf("Shoulders"),
                        equipment = "Dumbbells"
                    ),
                    Exercise(
                        id = "5",
                        nameResId = R.string.exercise5,
                        sets = 4,
                        reps = "15-20",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Shoulders"),
                        equipment = "Dumbbells"
                    )
                ),
                targetMuscleGroups = listOf("Chest", "Shoulders")
            ),
            
            // Day 1 - Monday: Cardio
            WorkoutDay(
                id = "day_1",
                nameResId = R.string.workout_cardio,
                dayIndex = 1,
                exercises = listOf(
                    Exercise(
                        id = "6",
                        nameResId = R.string.exercise6,
                        sets = 1,
                        reps = "30 min",
                        restTime = 0,
                        hasWeight = false,
                        muscleGroups = listOf("Full Body"),
                        equipment = "Treadmill/Bike"
                    )
                ),
                targetMuscleGroups = listOf("Cardio")
            ),
            
            // Day 2 - Tuesday: Legs
            WorkoutDay(
                id = "day_2",
                nameResId = R.string.workout_legs,
                dayIndex = 2,
                exercises = listOf(
                    Exercise(
                        id = "7",
                        nameResId = R.string.exercise7,
                        sets = 5,
                        reps = "10-12",
                        restTime = 60,
                        hasWeight = true,
                        muscleGroups = listOf("Quads", "Glutes"),
                        equipment = "Barbell"
                    ),
                    Exercise(
                        id = "8",
                        nameResId = R.string.exercise8,
                        sets = 4,
                        reps = "10 each leg",
                        restTime = 45,
                        hasWeight = false,
                        muscleGroups = listOf("Quads", "Glutes"),
                        equipment = "Bodyweight"
                    ),
                    Exercise(
                        id = "9",
                        nameResId = R.string.exercise9,
                        sets = 4,
                        reps = "12 steps each",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Quads", "Glutes"),
                        equipment = "Dumbbells"
                    ),
                    Exercise(
                        id = "10",
                        nameResId = R.string.exercise10,
                        sets = 4,
                        reps = "15-20",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Hamstrings"),
                        equipment = "Machine"
                    ),
                    Exercise(
                        id = "11",
                        nameResId = R.string.exercise11,
                        sets = 4,
                        reps = "15 each leg",
                        restTime = 45,
                        hasWeight = false,
                        muscleGroups = listOf("Quads", "Glutes"),
                        equipment = "Box/Bench"
                    )
                ),
                targetMuscleGroups = listOf("Legs")
            ),
            
            // Day 3 - Wednesday: Cardio
            WorkoutDay(
                id = "day_3",
                nameResId = R.string.workout_cardio,
                dayIndex = 3,
                exercises = listOf(
                    Exercise(
                        id = "12",
                        nameResId = R.string.exercise12,
                        sets = 1,
                        reps = "30-40 min",
                        restTime = 0,
                        hasWeight = false,
                        muscleGroups = listOf("Full Body"),
                        equipment = "Treadmill"
                    )
                ),
                targetMuscleGroups = listOf("Cardio")
            ),
            
            // Day 4 - Thursday: Back & Arms
            WorkoutDay(
                id = "day_4",
                nameResId = R.string.workout_back_arms,
                dayIndex = 4,
                exercises = listOf(
                    Exercise(
                        id = "13",
                        nameResId = R.string.exercise13,
                        sets = 5,
                        reps = "10-12",
                        restTime = 60,
                        hasWeight = true,
                        muscleGroups = listOf("Back"),
                        equipment = "Cable"
                    ),
                    Exercise(
                        id = "14",
                        nameResId = R.string.exercise14,
                        sets = 4,
                        reps = "12-15",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Back"),
                        equipment = "Cable"
                    ),
                    Exercise(
                        id = "15",
                        nameResId = R.string.exercise15,
                        sets = 4,
                        reps = "15-20",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Back"),
                        equipment = "Cable"
                    ),
                    Exercise(
                        id = "16",
                        nameResId = R.string.exercise16,
                        sets = 4,
                        reps = "12-15",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Biceps"),
                        equipment = "Dumbbells"
                    ),
                    Exercise(
                        id = "17",
                        nameResId = R.string.exercise17,
                        sets = 4,
                        reps = "15-20",
                        restTime = 45,
                        hasWeight = true,
                        muscleGroups = listOf("Triceps"),
                        equipment = "Cable"
                    )
                ),
                targetMuscleGroups = listOf("Back", "Arms")
            )
        )
    }
}

