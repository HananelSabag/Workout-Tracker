package com.hananel.workouttracker2

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.hananel.workouttracker2.data.DefaultData
import com.hananel.workouttracker2.data.models.UserPreferences
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.service.TimerService
import com.hananel.workouttracker2.ui.StorageFailure
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Onboarding as a returning user reaches it: Settings → Help reopens it over a real plan. Paging it
 * must work, and picking the recommended program must not cost the user their working weights.
 */
class OnboardingFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = WorkoutRepository(context)
    private var scenario: ActivityScenario<MainActivity>? = null
    private fun text(id: Int): String { var result = ""; scenario!!.onActivity { result = it.getString(id) }; return result }
    private fun shown(label: String) = compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()

    @Before fun setUp() = runBlocking {
        context.stopService(Intent(context, TimerService::class.java))
        StorageFailure.failed.value = false
        repository.clearAllProgressAndTimerState()
        repository.getSessionHistory().forEach { repository.deleteSessionFromHistory(it.id) }
        repository.saveWorkoutStructure(DefaultData.getDefaultWorkoutStructure())
        repository.updateExerciseWeight("1", 42.5f)
        repository.saveUserPreferences(UserPreferences(hasCompletedOnboarding = false, language = "system"))
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun tearDown() {
        scenario?.close()
        StorageFailure.failed.value = false
        runBlocking { repository.saveUserPreferences(UserPreferences(hasCompletedOnboarding = true)) }
    }

    @Test fun nextPagesTheOnboardingAndTheDefaultProgramKeepsWeights() {
        compose.waitUntil(10_000) { shown(text(R.string.next)) }
        compose.onNodeWithText(text(R.string.next)).performClick()

        // The pager animation used to throw here and swap the whole app for the error screen.
        compose.waitUntil(10_000) { shown(text(R.string.use_default_program)) || StorageFailure.failed.value }
        assertFalse(StorageFailure.failed.value)

        compose.onNodeWithText(text(R.string.use_default_program)).performClick()
        compose.waitUntil(10_000) { runBlocking { repository.getUserPreferences().hasCompletedOnboarding } }

        assertEquals(42.5f, runBlocking { repository.getWorkoutProgress().exerciseWeights["1"] } ?: 0f, 0.01f)
        assertFalse(StorageFailure.failed.value)
    }
}
