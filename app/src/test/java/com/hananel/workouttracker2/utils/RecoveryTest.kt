package com.hananel.workouttracker2.utils

import com.hananel.workouttracker2.data.models.TimerState
import org.junit.Assert.*
import org.junit.Test

class RecoveryTest {
    private val running = TimerState(60, 60, true, "Press", "ex", 1, 4, 1, endsAtEpochMs = 160_000)

    @Test fun timerUsesDeadlineInsteadOfFrozenSavedSeconds() {
        assertEquals(31, running.remainingSeconds(129_001))
        assertEquals(0, running.remainingSeconds(160_000))
        assertEquals(0, running.remainingSeconds(900_000))
    }

    @Test fun aPausedTimerRetainsItsRemainderAcrossProcessDeath() {
        val paused = running.copy(isRunning = false, isPaused = true, timeLeft = 37, endsAtEpochMs = 0)
        assertEquals(37, paused.remainingSeconds(900_000))
        assertEquals(0, paused.copy(isRestComplete = true).remainingSeconds(900_000))
    }

    @Test fun legacyTimerWithoutDeadlineBecomesComplete() {
        assertEquals(0, running.copy(endsAtEpochMs = 0).remainingSeconds(100_000))
    }

    @Test fun invalidAlarmTimesCannotCrashTheReceiverOrEditor() {
        listOf("", "tomorrow", "12", "12:xx", "25:00", "23:60", "-1:15", "12:30:00").forEach {
            assertNull(parseReminderTime(it))
        }
        assertEquals(0 to 0, parseReminderTime("00:00"))
        assertEquals(23 to 59, parseReminderTime("23:59"))
    }
}
