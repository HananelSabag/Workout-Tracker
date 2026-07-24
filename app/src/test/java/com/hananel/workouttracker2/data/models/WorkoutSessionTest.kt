package com.hananel.workouttracker2.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTest {

    private val start = 1_700_000_000_000L

    private fun session(
        lastActivity: Long = start,
        endTime: Long? = null,
        exercises: List<SessionExerciseLog> = emptyList()
    ) = WorkoutSession(
        id = "s1",
        dayIndex = 0,
        dateIso = "2026-07-24",
        startTime = start,
        lastActivityTime = lastActivity,
        endTime = endTime,
        plannedExercises = 3,
        exercises = exercises
    )

    private fun log(
        id: String,
        planned: Int,
        done: Int,
        weight: Float = 0f,
        completed: Boolean = done >= planned
    ) = SessionExerciseLog(
        exerciseId = id,
        name = id,
        plannedSets = planned,
        completedSets = done,
        weight = weight,
        completed = completed
    )

    @Test
    fun `live session duration grows with the last recorded action`() {
        val live = session(lastActivity = start + 90_000)
        assertTrue(live.isActive)
        assertEquals(90_000L, live.durationMs)
    }

    @Test
    fun `finished session duration is frozen at the end time`() {
        // The idle close backdates endTime to lastActivityTime, so a session abandoned for an hour
        // must still report only the time actually trained.
        val finished = session(lastActivity = start + 120_000, endTime = start + 120_000)
        assertFalse(finished.isActive)
        assertEquals(120_000L, finished.durationMs)
    }

    @Test
    fun `duration never goes negative when clocks disagree`() {
        val skewed = session(lastActivity = start - 5_000)
        assertEquals(0L, skewed.durationMs)
    }

    @Test
    fun `session goes stale exactly at the idle timeout`() {
        val now = start + WorkoutSession.IDLE_TIMEOUT_MS
        assertTrue(session(lastActivity = start).isStaleAt(now))
        assertFalse(session(lastActivity = start).isStaleAt(now - 1))
    }

    @Test
    fun `a closed session is never stale`() {
        val closed = session(lastActivity = start, endTime = start)
        assertFalse(closed.isStaleAt(start + WorkoutSession.IDLE_TIMEOUT_MS * 10))
    }

    @Test
    fun `totals count sets, finished exercises and volume`() {
        val s = session(
            exercises = listOf(
                log("a", planned = 4, done = 4, weight = 20f),
                log("b", planned = 3, done = 1, weight = 10f),
                log("c", planned = 3, done = 0)
            )
        )
        assertEquals(5, s.totalSets)
        assertEquals(1, s.completedExerciseCount)
        assertEquals(2, s.touchedExerciseCount)
        // 4 sets x 20kg + 1 set x 10kg
        assertEquals(90f, s.totalVolume, 0.01f)
    }

    @Test
    fun `bodyweight exercises contribute no volume but still count as sets`() {
        val s = session(exercises = listOf(log("pushups", planned = 3, done = 3)))
        assertEquals(3, s.totalSets)
        assertEquals(0f, s.totalVolume, 0.01f)
    }
}
