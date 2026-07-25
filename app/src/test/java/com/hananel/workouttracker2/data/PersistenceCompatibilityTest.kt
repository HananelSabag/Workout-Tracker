package com.hananel.workouttracker2.data

import com.hananel.workouttracker2.data.models.Exercise
import com.hananel.workouttracker2.data.models.TimerState
import com.hananel.workouttracker2.data.models.UserPreferences
import com.hananel.workouttracker2.data.models.WorkoutDay
import com.hananel.workouttracker2.data.models.WorkoutProgress
import com.hananel.workouttracker2.data.models.WorkoutSession
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Upgrade safety.
 *
 * Everything is stored as JSON blobs in DataStore, and users update the app rather than reinstall
 * it — so yesterday's payload has to decode into today's classes without throwing. A decode failure
 * here is not a crash, it's worse: the repository falls back to an empty value and the user's
 * workout plan silently disappears. Every field added to a persisted model must be optional, and
 * these tests hold that line by decoding literal older payloads.
 */
class PersistenceCompatibilityTest {

    // Must mirror WorkoutRepository's configuration exactly.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `v1 workout progress decodes without the day-reset and reps fields`() {
        val v1 = """
            {"completedExercises":{"ex1":true},"exerciseWeights":{"ex1":22.5},
             "exerciseNotes":{},"exerciseCompletedSets":{"ex1":4},
             "weekStartDate":"2026-07-19","lastUpdateTime":1700000000000}
        """.trimIndent()

        val progress = json.decodeFromString<WorkoutProgress>(v1)

        assertEquals(4, progress.exerciseCompletedSets["ex1"])
        assertEquals(22.5f, progress.exerciseWeights["ex1"]!!, 0.01f)
        // New in v2.1 — absent payloads must land on an empty map, not blow up.
        assertTrue(progress.dayTrainingDates.isEmpty())
    }

    @Test
    fun `v1 timer state decodes without the deadline and day fields`() {
        val v1 = """
            {"timeLeft":45,"totalTime":60,"isRunning":true,"exerciseName":"Bench",
             "exerciseId":"ex1","currentSet":2,"totalSets":4,"completedSets":1}
        """.trimIndent()

        val state = json.decodeFromString<TimerState>(v1)

        assertEquals(45, state.timeLeft)
        assertTrue(state.isRunning)
        assertFalse(state.isRestComplete)
        assertFalse(state.isPaused)
        // 0 is the signal that this state predates deadlines; recovery closes it out rather than
        // leaving a countdown that can never finish.
        assertEquals(0L, state.endsAtEpochMs)
        assertEquals(-1, state.dayIndex)
        assertEquals("", state.reps)
    }

    @Test
    fun `v1 preferences decode without the day-picked flag`() {
        val v1 = """
            {"language":"he","theme":"DARK","notificationsEnabled":true,"workoutAlarms":[],
             "autoResetWeekly":false,"enabledDays":[0,2,4],"autoOpenCurrentDay":true,
             "lastViewedDayIndex":2,"hasCompletedOnboarding":true}
        """.trimIndent()

        val prefs = json.decodeFromString<UserPreferences>(v1)

        assertEquals("he", prefs.language)
        assertEquals(listOf(0, 2, 4), prefs.enabledDays)
        assertEquals(2, prefs.lastViewedDayIndex)
        assertFalse(prefs.hasViewedAnyDay)
    }

    @Test
    fun `a workout plan saved by the old build still loads`() {
        // The one that actually matters: losing this means losing the user's whole program.
        val v1 = """
            [{"id":"day-sun","nameResId":2131034112,"customName":"Chest","dayIndex":0,
              "exercises":[{"id":"ex1","nameResId":2131034113,"customName":"","isCustom":false,
                            "sets":4,"reps":"10-12","restTime":60,"hasWeight":true,
                            "muscleGroups":["Chest"],"equipment":"Dumbbells"}],
              "targetMuscleGroups":["Chest"]}]
        """.trimIndent()

        val days = json.decodeFromString<List<WorkoutDay>>(v1)

        assertEquals(1, days.size)
        assertEquals("Chest", days[0].customName)
        assertEquals(1, days[0].exercises.size)
        assertEquals(4, days[0].exercises[0].sets)
        assertEquals("10-12", days[0].exercises[0].reps)
    }

    @Test
    fun `v2 session history decodes without per-set reps`() {
        val v2 = """
            [{"id":"s1","dayIndex":0,"dayId":"day-sun","dayName":"Chest","dateIso":"2026-07-20",
              "startTime":1700000000000,"lastActivityTime":1700003000000,"endTime":1700003000000,
              "plannedExercises":3,
              "exercises":[{"exerciseId":"ex1","name":"Bench","plannedSets":4,"reps":"10-12",
                            "completedSets":4,"weight":40.0,"completed":true,
                            "firstSetTime":1700000000000,"lastSetTime":1700003000000}]}]
        """.trimIndent()

        val history = json.decodeFromString<List<WorkoutSession>>(v2)

        assertEquals(1, history.size)
        assertEquals(4, history[0].totalSets)
        // No reps were recorded back then — the log renders sets-only rather than fake numbers.
        assertTrue(history[0].exercises[0].repsPerSet.isEmpty())
        assertEquals(0, history[0].exercises[0].totalReps)
    }

    @Test
    fun `unknown fields from a newer build are ignored rather than fatal`() {
        // Protects the downgrade / sideload direction too.
        val future = """
            {"timeLeft":10,"totalTime":30,"isRunning":false,"exerciseName":"Squat",
             "exerciseId":"ex9","currentSet":1,"totalSets":3,"completedSets":0,
             "someFieldFromTheFuture":"whatever"}
        """.trimIndent()

        val state = json.decodeFromString<TimerState>(future)

        assertEquals("Squat", state.exerciseName)
    }

    @Test
    fun `round-tripping today's models keeps every value`() {
        val exercise = Exercise(
            id = "ex1", nameResId = 1, sets = 4, reps = "10-12", restTime = 60,
            muscleGroups = listOf("Chest"), equipment = "Dumbbells"
        )
        val day = WorkoutDay(id = "d1", nameResId = 1, dayIndex = 0, exercises = listOf(exercise))

        val decoded = json.decodeFromString<WorkoutDay>(json.encodeToString(WorkoutDay.serializer(), day))

        assertEquals(day, decoded)
    }
}
