package com.hananel.workouttracker2

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.hananel.workouttracker2.data.models.*
import com.hananel.workouttracker2.data.repository.SessionContext
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.service.TimerService
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real Activity + DataStore + service regressions. Only the emulator's test app is modified. */
class WorkoutStabilityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = WorkoutRepository(context)
    private var scenario: ActivityScenario<MainActivity>? = null
    private val exercise = Exercise("stability-ex", 0, customName = "Stability press", isCustom = true, sets = 3, reps = "10", restTime = 60)
    private val day = WorkoutDay("stability-day", R.string.day_sunday, customName = "אימון A", dayIndex = 0, exercises = listOf(exercise))
    private val session = SessionContext(0, day.id, day.customName, 1, exercise.customName, "10")
    private fun text(id: Int): String { var result = ""; scenario!!.onActivity { result = it.getString(id) }; return result }
    private fun awaitState(predicate: (TimerState?) -> Boolean) = compose.waitUntil(10_000) { runBlocking { predicate(repository.getTimerState()) } }

    @Before fun setUp() = runBlocking {
        context.stopService(Intent(context, TimerService::class.java))
        repository.clearAllProgressAndTimerState()
        repository.getSessionHistory().forEach { repository.deleteSessionFromHistory(it.id) }
        repository.saveWorkoutStructure(listOf(day))
        repository.saveUserPreferences(UserPreferences(hasCompletedOnboarding = true, hasViewedAnyDay = true, enabledDays = (0..6).toList(), language = "system"))
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Stability press").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun tearDown() {
        scenario?.close()
        context.stopService(Intent(context, TimerService::class.java))
        runBlocking { repository.clearTimerState() }
    }

    @Test fun customExerciseIsReallySavedAndSurvivesActivityRecreation() {
        compose.onNodeWithContentDescription(text(R.string.add_exercise)).performClick()
        compose.onNodeWithText(text(R.string.create_new_exercise)).performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("My new exercise")
        compose.onNodeWithText(text(R.string.add_exercise)).performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { repository.getWorkoutStructure().single().exercises.size == 2 } }
        scenario!!.recreate()
        compose.waitUntil(10_000) { runBlocking { repository.getWorkoutStructure().single().exercises.any { it.customName == "My new exercise" } } }
        assertEquals("Stability press", runBlocking { repository.getWorkoutStructure().single().exercises.first().customName })
        assertEquals(2, runBlocking { repository.getWorkoutStructure().single().exercises.size })
    }

    @Test fun finalSetWithZeroRestIsSavedAndCanBeUndone() {
        runBlocking { repository.updateWorkoutStructure { listOf(day.copy(exercises = listOf(exercise.copy(sets = 1, restTime = 0)))) } }
        compose.onNodeWithText(text(R.string.finish_last_set)).performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { repository.getWorkoutProgress().completedExercises[exercise.id] == true } }
        assertEquals(1, runBlocking { repository.getActiveSession()!!.totalSets })
        awaitState { it == null }
        compose.onNodeWithText(text(R.string.unmark_complete)).performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { repository.getWorkoutProgress().exerciseCompletedSets[exercise.id] == 0 } }
    }

    @Test fun pausedTimerRecoversAndResumesWithoutLosingItsRemainder() {
        runBlocking { repository.completeSet(exercise.id, 3, session, reps = 10) }
        scenario!!.onActivity { TimerService.startRest(it, exercise.customName, exercise.id, 60, 1, 3, 0, day.id, day.customName, 1, "10") }
        awaitState { it?.isRunning == true }
        scenario!!.onActivity { TimerService.pauseResume(it) }
        awaitState { it?.isPaused == true }
        val paused = runBlocking { repository.getTimerState()!! }
        assertTrue(paused.timeLeft in 50..60)
        // Destroying the service reproduces loss of its in-memory fields.
        context.stopService(Intent(context, TimerService::class.java))
        compose.waitUntil(10_000) { !TimerService.isRunning }
        scenario!!.onActivity { TimerService.recover(it) }
        compose.waitUntil(10_000) { TimerService.isRunning }
        scenario!!.onActivity { TimerService.pauseResume(it) }
        awaitState { it?.isRunning == true && !it.isPaused }
        val resumed = runBlocking { repository.getTimerState()!! }
        assertTrue(resumed.remainingSeconds() in (paused.timeLeft - 2)..paused.timeLeft)
        assertEquals(1, runBlocking { repository.getWorkoutProgress().exerciseCompletedSets[exercise.id] })
    }

    @Test fun expiredTimerRecoversAndStaleNotificationCannotLogTwice() {
        runBlocking {
            repository.completeSet(exercise.id, 3, session, reps = 10)
            repository.saveTimerState(TimerState(60, 60, true, exercise.customName, exercise.id, 1, 3, 1,
                endsAtEpochMs = System.currentTimeMillis() - 30_000, dayIndex = 0, dayId = day.id, dayName = day.customName, reps = "10"))
        }
        scenario!!.onActivity { TimerService.recover(it) }
        awaitState { it?.isRestComplete == true }
        scenario!!.onActivity { activity ->
            repeat(2) { activity.startForegroundService(Intent(activity, TimerService::class.java)
                .setAction(TimerService.ACTION_NEXT_SET).putExtra("expected_exercise", exercise.id).putExtra("expected_sets", 1)) }
        }
        awaitState { it?.completedSets == 2 && it.isRunning }
        assertEquals(2, runBlocking { repository.getWorkoutProgress().exerciseCompletedSets[exercise.id] })
        scenario!!.onActivity { TimerService.stopTimer(it) }
        awaitState { it == null }
        compose.waitUntil(10_000) { !TimerService.isRunning }
        assertNull(runBlocking { repository.getTimerState() })
    }

    @Test fun reminderWithoutExactPermissionAndMalformedReminderDoNotCrash() {
        val helper = NotificationHelper(context)
        helper.scheduleWorkoutReminder(WorkoutAlarm("bad", "not-a-time", setOf(0)))
        helper.scheduleWorkoutReminder(WorkoutAlarm("valid", "18:30", setOf(0, 2)))
        helper.scheduleWorkoutReminder(WorkoutAlarm("valid", "19:00", setOf(4)))
        helper.cancelWorkoutReminder("valid")
    }
}
