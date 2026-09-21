package com.hananel.workouttracker2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared error surface for edits made outside the workout ViewModel (settings, builder, alarms). */
object StorageFailure {
    val failed = MutableStateFlow(false)
    val handler = CoroutineExceptionHandler { _, error ->
        android.util.Log.e("WorkoutStorage", "Operation failed; existing data retained", error)
        failed.value = true
    }
}

/**
 * Scope for storage writes launched from a screen, cancelled when that screen leaves composition.
 *
 * Deliberately NOT [androidx.compose.runtime.rememberCoroutineScope]: the job it hands out is a
 * child of the recomposer's job, and a plain job passes a child's failure upwards. A failed write
 * would therefore cancel the recomposer itself — recomposition stops, and the screen freezes on its
 * last frame instead of showing the error state this handler sets. A SupervisorJob keeps the
 * failure local, so the handler runs while the UI is still alive to render it.
 */
@Composable
fun rememberStorageScope(): CoroutineScope {
    val scope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + StorageFailure.handler)
    }
    DisposableEffect(scope) {
        onDispose { scope.cancel() }
    }
    return scope
}
