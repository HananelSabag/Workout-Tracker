package com.hananel.workouttracker2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hananel.workouttracker2.ui.StorageFailure
import com.hananel.workouttracker2.ui.rememberStorageScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A failed storage write must not take the recomposer down with it.
 *
 * While the screen's scope was a plain child job of the recomposer, one thrown exception cancelled
 * the recomposer on its way up: the error flag flipped, but the screen froze on its last frame and
 * could never render the error it had just recorded.
 */
class StorageFailureScopeTest {
    @get:Rule val compose = createComposeRule()

    // The flag is a process-wide singleton and AppNavigation swaps the whole app for the error
    // screen while it is set — leaving it on here would fail every screen test that follows.
    @Before fun clearFlag() { StorageFailure.failed.value = false }
    @After fun clearFlagAgain() { StorageFailure.failed.value = false }

    @Test fun animationsLaunchedInTheScopeStillRun() {
        // Onboarding's "next" pages the pager from this scope; an animation needs the composition's
        // frame clock, which a scope built from scratch doesn't carry.
        lateinit var scope: CoroutineScope
        compose.setContent {
            scope = rememberStorageScope()
            Text("page")
        }
        var finished = false
        scope.launch {
            Animatable(0f).animateTo(1f, tween(100))
            finished = true
        }
        compose.waitUntil(5_000) { finished || StorageFailure.failed.value }
        assertFalse(StorageFailure.failed.value)
        assertTrue(finished)
    }

    @Test fun failedWriteSetsTheFlagAndLeavesTheScreenRecomposing() {
        lateinit var scope: CoroutineScope
        var label by mutableStateOf("before")

        compose.setContent {
            scope = rememberStorageScope()
            Text(label)
        }
        compose.onNodeWithText("before").assertIsDisplayed()

        scope.launch { throw IllegalStateException("storage unavailable") }
        compose.waitUntil(5_000) { StorageFailure.failed.value }

        // State written after the failure still reaches the screen — recomposition survived.
        label = "after"
        compose.waitUntil(5_000) { compose.onAllNodesWithText("after").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(StorageFailure.failed.value)
    }
}
