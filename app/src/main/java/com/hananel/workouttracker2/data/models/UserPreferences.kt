package com.hananel.workouttracker2.data.models

import kotlinx.serialization.Serializable

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Serializable
data class UserPreferences(
    val language: String = "system", // "he", "en", "system"
    val theme: String = "SYSTEM", // Store as string for DataStore
    val notificationsEnabled: Boolean = true,
    val workoutAlarms: List<WorkoutAlarm> = emptyList(),
    val autoResetWeekly: Boolean = false, // Default: manual only
    val enabledDays: List<Int> = listOf(0, 1, 2, 3, 4) // Sun-Thu default
) {
    fun getThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(theme)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }
}

