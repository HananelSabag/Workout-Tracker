package com.hananel.workouttracker2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
 * The composition's scope — its frame clock, cancelled when the screen leaves — run on the main
 * thread under a [SupervisorJob].
 *
 * A plain child job passes a failure upwards, and the composition scope's job is itself a child of
 * the recomposer's, so one failed write cancelled the recomposer: the flag below flipped, but the
 * screen froze on its last frame and never drew the error. A supervisor keeps the failure local.
 * The frame clock must stay: animations launched here (onboarding pages its pager) throw without
 * the composition's MonotonicFrameClock. The dispatcher is pinned to main: work here resumes after
 * DataStore writes and goes on to navigate, which must happen on the main thread whatever
 * dispatcher the composition supplies (under test it supplies one that resumes on the caller).
 */
@Composable
fun rememberStorageScope(): CoroutineScope {
    val composition = rememberCoroutineScope()
    return remember(composition) {
        CoroutineScope(
            composition.coroutineContext +
                Dispatchers.Main.immediate +
                SupervisorJob(composition.coroutineContext[Job]) +
                StorageFailure.handler
        )
    }
}
