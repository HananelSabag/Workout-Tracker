package com.hananel.workouttracker2.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class RepsUtilsTest {

    @Test
    fun `a range targets its upper bound`() {
        // "10-12" means "aim for 12" — pre-filling the low end would under-report every set.
        assertEquals(12, RepsUtils.targetReps("10-12"))
        assertEquals(10, RepsUtils.targetReps("8-10"))
        assertEquals(15, RepsUtils.targetReps("12-15"))
    }

    @Test
    fun `a plain number is used as is`() {
        assertEquals(8, RepsUtils.targetReps("8"))
        assertEquals(20, RepsUtils.targetReps("20"))
    }

    @Test
    fun `numbers survive surrounding words in either language`() {
        assertEquals(12, RepsUtils.targetReps("12 לכל צד"))
        assertEquals(30, RepsUtils.targetReps("30 seconds"))
    }

    @Test
    fun `plans with no number are untracked rather than zero reps`() {
        assertEquals(0, RepsUtils.targetReps("מקסימום"))
        assertEquals(0, RepsUtils.targetReps("AMRAP"))
        assertEquals(0, RepsUtils.targetReps(""))
    }

    @Test
    fun `absurd numbers are clamped`() {
        assertEquals(999, RepsUtils.targetReps("100000"))
    }

    @Test
    fun `set breakdown hides untracked sets`() {
        assertEquals("12 · 12 · 10", RepsUtils.formatSetReps(listOf(12, 12, 10)))
        assertEquals("12 · 10", RepsUtils.formatSetReps(listOf(12, 0, 10)))
        assertEquals("", RepsUtils.formatSetReps(listOf(0, 0)))
        assertEquals("", RepsUtils.formatSetReps(emptyList()))
    }
}
