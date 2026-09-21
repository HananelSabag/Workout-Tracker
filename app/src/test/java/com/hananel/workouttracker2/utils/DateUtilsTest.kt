package com.hananel.workouttracker2.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DateUtilsTest {

    @Test
    fun `duration is mm ss below an hour`() {
        assertEquals("0:00", DateUtils.formatDuration(0))
        assertEquals("0:07", DateUtils.formatDuration(7_000))
        assertEquals("24:07", DateUtils.formatDuration(TimeUnit.MINUTES.toMillis(24) + 7_000))
        assertEquals("59:59", DateUtils.formatDuration(TimeUnit.MINUTES.toMillis(59) + 59_000))
    }

    @Test
    fun `duration rolls over to h mm ss`() {
        assertEquals("1:00:00", DateUtils.formatDuration(TimeUnit.HOURS.toMillis(1)))
        assertEquals(
            "1:04:20",
            DateUtils.formatDuration(
                TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(4) + 20_000
            )
        )
    }

    @Test
    fun `negative durations clamp to zero rather than printing a minus`() {
        assertEquals("0:00", DateUtils.formatDuration(-5_000))
    }

    @Test
    fun `durationMinutes truncates instead of rounding`() {
        assertEquals(0, DateUtils.durationMinutes(59_000))
        assertEquals(1, DateUtils.durationMinutes(60_000))
        assertEquals(1, DateUtils.durationMinutes(119_000))
    }

    @Test
    fun `daysAgo compares calendar days, not elapsed hours`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        // 23:00 "yesterday" is two hours earlier but must still read as 1 day ago.
        val lateYesterday = now - TimeUnit.HOURS.toMillis(2)
        assertEquals(1, DateUtils.daysAgo(lateYesterday, now))

        assertEquals(0, DateUtils.daysAgo(now, now))
        assertEquals(3, DateUtils.daysAgo(now - TimeUnit.DAYS.toMillis(3), now))
    }

    @Test
    fun `daysAgo stays exact across Israeli clock changes`() {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jerusalem"))
        try {
            val zone = ZoneId.of("Asia/Jerusalem")
            fun at(month: Int, day: Int) = ZonedDateTime.of(2026, month, day, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
            // Clocks went forward at 02:00 on 27 March 2026, so that day was only 23 hours long.
            assertEquals(1, DateUtils.daysAgo(at(3, 27), at(3, 28)))
            assertEquals(2, DateUtils.daysAgo(at(3, 26), at(3, 28)))
            assertEquals(12, DateUtils.daysAgo(at(3, 20), at(4, 1)))
            // And back at 02:00 on 25 October — a 25-hour day.
            assertEquals(1, DateUtils.daysAgo(at(10, 25), at(10, 26)))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `iso conversion is stable for the same instant`() {
        val instant = System.currentTimeMillis()
        assertEquals(DateUtils.toIso(instant), DateUtils.toIso(instant))
        assertEquals(DateUtils.getTodayIso(), DateUtils.toIso(System.currentTimeMillis()))
    }
}
