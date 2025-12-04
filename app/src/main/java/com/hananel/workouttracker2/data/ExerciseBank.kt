package com.hananel.workouttracker2.data

import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.ExerciseBankItem
import com.hananel.workouttracker2.data.models.ExerciseCategory

object ExerciseBank {
    
    fun getAllExercises(): List<ExerciseBankItem> {
        return listOf(
            // ========== CHEST EXERCISES ==========
            ExerciseBankItem(
                id = "bank_chest_dumbbell_press",
                nameResId = R.string.exercise_chest_dumbbell_press,
                muscleGroups = listOf("Chest"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_chest_barbell_press",
                nameResId = R.string.exercise_chest_barbell_press,
                muscleGroups = listOf("Chest"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "8-10",
                defaultRestTime = 90
            ),
            ExerciseBankItem(
                id = "bank_chest_cable_press",
                nameResId = R.string.exercise_chest_cable_press,
                muscleGroups = listOf("Chest"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_chest_flyes",
                nameResId = R.string.exercise_chest_flyes,
                muscleGroups = listOf("Chest"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_chest_dumbbell_flyes",
                nameResId = R.string.exercise_chest_dumbbell_flyes,
                muscleGroups = listOf("Chest"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_chest_pushups",
                nameResId = R.string.exercise_chest_pushups,
                muscleGroups = listOf("Chest"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45,
                hasWeight = false
            ),
            
            // ========== BACK EXERCISES ==========
            ExerciseBankItem(
                id = "bank_back_lat_pulldown",
                nameResId = R.string.exercise_back_lat_pulldown,
                muscleGroups = listOf("Back"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_back_cable_row",
                nameResId = R.string.exercise_back_cable_row,
                muscleGroups = listOf("Back"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_back_cable_pullover",
                nameResId = R.string.exercise_back_cable_pullover,
                muscleGroups = listOf("Back"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_back_deadlift",
                nameResId = R.string.exercise_back_deadlift,
                muscleGroups = listOf("Back", "Legs"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "6-8",
                defaultRestTime = 120
            ),
            ExerciseBankItem(
                id = "bank_back_pullups",
                nameResId = R.string.exercise_back_pullups,
                muscleGroups = listOf("Back"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "8-12",
                defaultRestTime = 60,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_back_dumbbell_row",
                nameResId = R.string.exercise_back_dumbbell_row,
                muscleGroups = listOf("Back"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            
            // ========== SHOULDERS EXERCISES ==========
            ExerciseBankItem(
                id = "bank_shoulders_dumbbell_press",
                nameResId = R.string.exercise_shoulders_dumbbell_press,
                muscleGroups = listOf("Shoulders"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "10-12",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_shoulders_barbell_press",
                nameResId = R.string.exercise_shoulders_barbell_press,
                muscleGroups = listOf("Shoulders"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "8-10",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_shoulders_lateral_raise",
                nameResId = R.string.exercise_shoulders_lateral_raise,
                muscleGroups = listOf("Shoulders"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_shoulders_front_raise",
                nameResId = R.string.exercise_shoulders_front_raise,
                muscleGroups = listOf("Shoulders"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_shoulders_rear_delt_flyes",
                nameResId = R.string.exercise_shoulders_rear_delt_flyes,
                muscleGroups = listOf("Shoulders"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            
            // ========== ARMS EXERCISES ==========
            ExerciseBankItem(
                id = "bank_arms_dumbbell_curl",
                nameResId = R.string.exercise_arms_dumbbell_curl,
                muscleGroups = listOf("Biceps"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_arms_barbell_curl",
                nameResId = R.string.exercise_arms_barbell_curl,
                muscleGroups = listOf("Biceps"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "10-12",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_arms_hammer_curl",
                nameResId = R.string.exercise_arms_hammer_curl,
                muscleGroups = listOf("Biceps"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_arms_cable_tricep_extension",
                nameResId = R.string.exercise_arms_cable_tricep_extension,
                muscleGroups = listOf("Triceps"),
                equipment = "Cable",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_arms_dumbbell_tricep_extension",
                nameResId = R.string.exercise_arms_dumbbell_tricep_extension,
                muscleGroups = listOf("Triceps"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_arms_dips",
                nameResId = R.string.exercise_arms_dips,
                muscleGroups = listOf("Triceps", "Chest"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "10-15",
                defaultRestTime = 60,
                hasWeight = false
            ),
            
            // ========== LEGS EXERCISES ==========
            ExerciseBankItem(
                id = "bank_legs_squats",
                nameResId = R.string.exercise_legs_squats,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 5,
                defaultReps = "10-12",
                defaultRestTime = 90
            ),
            ExerciseBankItem(
                id = "bank_legs_front_squats",
                nameResId = R.string.exercise_legs_front_squats,
                muscleGroups = listOf("Quads"),
                equipment = "Barbell",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "10-12",
                defaultRestTime = 90
            ),
            ExerciseBankItem(
                id = "bank_legs_leg_press",
                nameResId = R.string.exercise_legs_leg_press,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12-15",
                defaultRestTime = 60
            ),
            ExerciseBankItem(
                id = "bank_legs_leg_curl",
                nameResId = R.string.exercise_legs_leg_curl,
                muscleGroups = listOf("Hamstrings"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_legs_leg_extension",
                nameResId = R.string.exercise_legs_leg_extension,
                muscleGroups = listOf("Quads"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_legs_lunges",
                nameResId = R.string.exercise_legs_lunges,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Dumbbells",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "12 each leg",
                defaultRestTime = 45
            ),
            ExerciseBankItem(
                id = "bank_legs_cossack_squat",
                nameResId = R.string.exercise_legs_cossack_squat,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "10 each leg",
                defaultRestTime = 45,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_legs_step_ups",
                nameResId = R.string.exercise_legs_step_ups,
                muscleGroups = listOf("Quads", "Glutes"),
                equipment = "Box/Bench",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15 each leg",
                defaultRestTime = 45,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_legs_calf_raise",
                nameResId = R.string.exercise_legs_calf_raise,
                muscleGroups = listOf("Calves"),
                equipment = "Machine",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "20-25",
                defaultRestTime = 30
            ),
            
            // ========== CORE EXERCISES ==========
            ExerciseBankItem(
                id = "bank_core_plank",
                nameResId = R.string.exercise_core_plank,
                muscleGroups = listOf("Core"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "60 sec",
                defaultRestTime = 30,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_core_crunches",
                nameResId = R.string.exercise_core_crunches,
                muscleGroups = listOf("Core"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "20-25",
                defaultRestTime = 30,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_core_russian_twist",
                nameResId = R.string.exercise_core_russian_twist,
                muscleGroups = listOf("Core"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "20 each side",
                defaultRestTime = 30,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_core_leg_raises",
                nameResId = R.string.exercise_core_leg_raises,
                muscleGroups = listOf("Core"),
                equipment = "Bodyweight",
                category = ExerciseCategory.ANAEROBIC,
                defaultSets = 4,
                defaultReps = "15-20",
                defaultRestTime = 30,
                hasWeight = false
            ),
            
            // ========== CARDIO EXERCISES ==========
            ExerciseBankItem(
                id = "bank_cardio_hiit",
                nameResId = R.string.exercise_cardio_hiit,
                muscleGroups = listOf("Full Body"),
                equipment = "Treadmill/Bike",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "30 min",
                defaultRestTime = 0,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_cardio_running",
                nameResId = R.string.exercise_cardio_running,
                muscleGroups = listOf("Full Body"),
                equipment = "Treadmill",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "30-40 min",
                defaultRestTime = 0,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_cardio_cycling",
                nameResId = R.string.exercise_cardio_cycling,
                muscleGroups = listOf("Full Body"),
                equipment = "Bike",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "30-45 min",
                defaultRestTime = 0,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_cardio_rowing",
                nameResId = R.string.exercise_cardio_rowing,
                muscleGroups = listOf("Full Body"),
                equipment = "Rowing Machine",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "20-30 min",
                defaultRestTime = 0,
                hasWeight = false
            ),
            ExerciseBankItem(
                id = "bank_cardio_elliptical",
                nameResId = R.string.exercise_cardio_elliptical,
                muscleGroups = listOf("Full Body"),
                equipment = "Elliptical",
                category = ExerciseCategory.AEROBIC,
                defaultSets = 1,
                defaultReps = "30-40 min",
                defaultRestTime = 0,
                hasWeight = false
            )
        )
    }
    
    fun getExercisesByMuscleGroup(group: String): List<ExerciseBankItem> {
        return getAllExercises().filter { exercise ->
            exercise.muscleGroups.any { it.equals(group, ignoreCase = true) }
        }
    }
    
    fun getExercisesByCategory(category: ExerciseCategory): List<ExerciseBankItem> {
        return getAllExercises().filter { it.category == category }
    }
    
    fun getMuscleGroupsForFiltering(): List<String> {
        return listOf("Chest", "Back", "Shoulders", "Arms", "Legs", "Core", "Cardio")
    }
}

