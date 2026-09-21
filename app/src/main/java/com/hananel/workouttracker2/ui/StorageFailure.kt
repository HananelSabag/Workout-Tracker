package com.hananel.workouttracker2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared error surface for edits made outside the workout ViewModel (settings, builder, alarms). */
object StorageFailure {
    val failed = MutableStateFlow(false)
    val handler = CoroutineExceptionHandler { _, error ->
        android.util.Log.e("WorkoutStorage", "Operation failed; existing data retained", error)
        failed.value = true
    }
}

@Composable
fun rememberStorageScope() = rememberCoroutineScope { StorageFailure.handler }
