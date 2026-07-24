package com.hananel.workouttracker2.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hananel.workouttracker2.data.models.UserPreferences
import com.hananel.workouttracker2.data.models.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * Exercises the real DataStore. These cover the rule the whole app hangs on: set counting and
 * exercise completion are decided in a single write, so they can never disagree.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {

    private lateinit var repository: WorkoutRepository

    private val ctx = SessionContext(
        dayIndex = 0,
        dayId = "day-sun",
        dayName = "Chest",
        plannedExercises = 2,
        exerciseName = "Dumbbell Chest Press",
        reps = "10-12"
    )

    private val start = 1_700_000_000_000L

    @Before
    fun setUp() = runBlocking {
        // These share the app's real DataStore, so every test starts from a wiped store.
        repository = WorkoutRepository(ApplicationProvider.getApplicationContext())
        repository.clearAllProgressAndTimerState()
        repository.discardActiveSession()
        repository.saveUserPreferences(UserPreferences())
        repository.getSessionHistory().forEach { repository.deleteSessionFromHistory(it.id) }
    }

    // ── Set counting ─────────────────────────────────────────────────────────

    @Test
    fun completingTheFinalSetMarksTheExerciseComplete() = runBlocking {
        // The regression this whole rewrite exists for: the last set must complete the exercise
        // in the same write, with no timer involvement.
        repeat(3) { repository.completeSet("ex1", totalSets = 4, ctx = ctx) }
        val beforeLast = repository.getWorkoutProgress()
        assertEquals(3, beforeLast.exerciseCompletedSets["ex1"])
        assertFalse(beforeLast.completedExercises["ex1"] ?: false)

        val result = repository.completeSet("ex1", totalSets = 4, ctx = ctx)

        assertTrue(result.exerciseCompleted)
        assertEquals(4, result.completedSets)
        val progress = repository.getWorkoutProgress()
        assertEquals(4, progress.exerciseCompletedSets["ex1"])
        assertTrue(progress.completedExercises["ex1"] ?: false)
    }

    @Test
    fun aSingleSetExerciseCompletesOnTheVeryFirstTap() = runBlocking {
        val result = repository.completeSet("ex1", totalSets = 1, ctx = ctx)
        assertTrue(result.exerciseCompleted)
        assertTrue(repository.getWorkoutProgress().completedExercises["ex1"] ?: false)
    }

    @Test
    fun setCountNeverExceedsThePlannedTotal() = runBlocking {
        repeat(6) { repository.completeSet("ex1", totalSets = 3, ctx = ctx) }
        assertEquals(3, repository.getWorkoutProgress().exerciseCompletedSets["ex1"])
    }

    @Test
    fun undoingASetReopensACompletedExercise() = runBlocking {
        repeat(3) { repository.completeSet("ex1", totalSets = 3, ctx = ctx) }
        assertTrue(repository.getWorkoutProgress().completedExercises["ex1"] ?: false)

        val result = repository.undoSet("ex1", totalSets = 3)

        assertEquals(2, result.completedSets)
        assertFalse(result.exerciseCompleted)
        val progress = repository.getWorkoutProgress()
        assertEquals(2, progress.exerciseCompletedSets["ex1"])
        assertFalse(progress.completedExercises["ex1"] ?: true)
    }

    @Test
    fun undoingBelowZeroIsANoOp() = runBlocking {
        val result = repository.undoSet("ex1", totalSets = 3)
        assertEquals(0, result.completedSets)
    }

    @Test
    fun manuallyMarkingCompleteFillsEveryySet() = runBlocking {
        repository.setExerciseCompletion("ex1", completed = true, totalSets = 4, ctx = ctx)
        val progress = repository.getWorkoutProgress()
        assertEquals(4, progress.exerciseCompletedSets["ex1"])
        assertTrue(progress.completedExercises["ex1"] ?: false)

        repository.setExerciseCompletion("ex1", completed = false, totalSets = 4, ctx = ctx)
        val cleared = repository.getWorkoutProgress()
        assertEquals(0, cleared.exerciseCompletedSets["ex1"])
        assertFalse(cleared.completedExercises["ex1"] ?: true)
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    @Test
    fun theFirstSetOpensASessionAndLaterSetsAccumulateIntoIt() = runBlocking {
        assertNull(repository.getActiveSession())

        repository.completeSet("ex1", totalSets = 3, ctx = ctx, now = start)
        val opened = repository.getActiveSession()
        assertNotNull(opened)
        assertEquals(start, opened!!.startTime)
        assertEquals(1, opened.totalSets)

        repository.completeSet("ex2", totalSets = 2, ctx = ctx.copy(exerciseName = "Flyes"), now = start + 60_000)
        val grown = repository.getActiveSession()!!
        // Same session — start time is pinned to the first set, not the latest one.
        assertEquals(opened.id, grown.id)
        assertEquals(start, grown.startTime)
        assertEquals(start + 60_000, grown.lastActivityTime)
        assertEquals(2, grown.exercises.size)
        assertEquals(2, grown.totalSets)
    }

    @Test
    fun weightIsCapturedOnTheSessionLog() = runBlocking {
        repository.updateExerciseWeight("ex1", 22.5f)
        repository.completeSet("ex1", totalSets = 2, ctx = ctx)
        val entry = repository.getActiveSession()!!.exercises.first()
        assertEquals(22.5f, entry.weight, 0.01f)
        assertEquals("Dumbbell Chest Press", entry.name)
    }

    @Test
    fun anIdleSessionIsClosedAndBackdatedToTheLastAction() = runBlocking {
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = start)
        val lastAction = start + 30_000
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = lastAction)

        // Come back an hour later: the workout must be recorded as ending when it actually stopped.
        val closed = repository.finalizeStaleSession(lastAction + WorkoutSession.IDLE_TIMEOUT_MS + 60_000)

        assertTrue(closed)
        assertNull(repository.getActiveSession())
        val archived = repository.getSessionHistory().single()
        assertEquals(lastAction, archived.endTime)
        assertEquals(30_000L, archived.durationMs)
    }

    @Test
    fun aSessionShorterThanTheTimeoutIsLeftAlone() = runBlocking {
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = start)
        val closed = repository.finalizeStaleSession(start + WorkoutSession.IDLE_TIMEOUT_MS - 1)
        assertFalse(closed)
        assertNotNull(repository.getActiveSession())
        assertTrue(repository.getSessionHistory().isEmpty())
    }

    @Test
    fun trainingADifferentDayRotatesToAFreshSession() = runBlocking {
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = start)
        val monday = ctx.copy(dayIndex = 1, dayId = "day-mon", dayName = "Back")

        repository.completeSet("ex9", totalSets = 2, ctx = monday, now = start + 5_000)

        val active = repository.getActiveSession()!!
        assertEquals(1, active.dayIndex)
        assertEquals(1, active.totalSets)
        assertEquals(0, repository.getSessionHistory().single().dayIndex)
    }

    @Test
    fun crossingMidnightKeepsOneSession() = runBlocking {
        // 23:57 then 00:03 — six minutes apart, but a different calendar date. A late workout is
        // still one workout.
        val beforeMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 57)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val afterMidnight = beforeMidnight + 360_000

        repository.completeSet("ex1", totalSets = 3, ctx = ctx, now = beforeMidnight)
        val id = repository.getActiveSession()!!.id
        repository.completeSet("ex1", totalSets = 3, ctx = ctx, now = afterMidnight)

        assertEquals(id, repository.getActiveSession()!!.id)
        assertTrue(repository.getSessionHistory().isEmpty())
    }

    @Test
    fun endingTheWorkoutArchivesItAndDiscardingDoesNot() = runBlocking {
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = start)
        repository.endActiveSession(start + 45_000)

        assertNull(repository.getActiveSession())
        assertEquals(1, repository.getSessionHistory().size)
        assertEquals(45_000L, repository.getSessionHistory().single().durationMs)

        repository.completeSet("ex2", totalSets = 2, ctx = ctx, now = start + 100_000)
        repository.discardActiveSession()
        assertNull(repository.getActiveSession())
        assertEquals(1, repository.getSessionHistory().size)
    }

    @Test
    fun aSessionWithNoRecordedSetsIsNotWorthKeeping() = runBlocking {
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = start)
        repository.undoSet("ex1", totalSets = 2)

        repository.endActiveSession(start + 10_000)

        assertTrue(repository.getSessionHistory().isEmpty())
    }

    @Test
    fun resettingTheWeekKeepsWeightsAndArchivesTheLiveSession() = runBlocking {
        repository.updateExerciseWeight("ex1", 40f)
        repository.completeSet("ex1", totalSets = 2, ctx = ctx, now = start)

        repository.resetWeeklyProgress()

        val progress = repository.getWorkoutProgress()
        assertTrue(progress.exerciseCompletedSets.isEmpty())
        assertEquals(40f, progress.exerciseWeights["ex1"]!!, 0.01f)
        assertNull(repository.getActiveSession())
        assertEquals(1, repository.getSessionHistory().size)
    }

    @Test
    fun deletingAnExerciseRemovesItsProgress() = runBlocking {
        repository.completeSet("ex1", totalSets = 2, ctx = ctx)
        repository.removeExerciseProgress("ex1")
        val progress = repository.getWorkoutProgress()
        assertNull(progress.exerciseCompletedSets["ex1"])
        assertNull(progress.completedExercises["ex1"])
    }

    @Test
    fun pickingADayIsRememberedEvenWhenItIsSunday() = runBlocking {
        // Sunday is index 0, which used to be indistinguishable from "never picked a day".
        assertFalse(repository.getUserPreferences().hasViewedAnyDay)
        repository.updateLastViewedDay(0)
        val prefs = repository.getUserPreferences()
        assertEquals(0, prefs.lastViewedDayIndex)
        assertTrue(prefs.hasViewedAnyDay)
    }
}
