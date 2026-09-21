package com.hananel.workouttracker2.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    // SimpleDateFormat is mutable: repository workers and the UI must not share an instance.
    private val dateFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    fun getCurrentWeekStart(): String {
        val calendar = Calendar.getInstance()
        // Set to Sunday (first day of week)
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return dateFormat.format(calendar.time)
    }
    
    fun getCurrentDayIndex(): Int {
        val calendar = Calendar.getInstance()
        // Sunday = 0, Monday = 1, ..., Saturday = 6
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }
    }
    
    fun getDayName(dayIndex: Int, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + dayIndex)
        val dayFormat = SimpleDateFormat("EEEE", locale)
        return dayFormat.format(calendar.time)
    }
    
    fun getShortDayName(dayIndex: Int, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + dayIndex)
        val dayFormat = SimpleDateFormat("EEE", locale)
        return dayFormat.format(calendar.time)
    }

    /** Calendar day key used to bucket sessions — a workout never spans two dates. */
    fun getTodayIso(): String = dateFormat.format(Date())

    fun toIso(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /** "19:42" — session start/end stamps. Always 24h; the app is Hebrew-first. */
    fun formatClock(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.US).format(Date(timestamp))

    /** "24.7" — compact date used on history rows. */
    fun formatShortDate(timestamp: Long): String =
        SimpleDateFormat("d.M", Locale.US).format(Date(timestamp))

    fun formatFullDate(timestamp: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("d MMMM yyyy", locale).format(Date(timestamp))

    /** "1:04:20" past an hour, otherwise "24:07". Used for the live session clock. */
    fun formatDuration(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }

    fun durationMinutes(millis: Long): Int =
        TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0)).toInt()

    /**
     * Calendar days between [timestamp] and now, for "לפני 3 ימים" labels.
     *
     * Counts dates, not 24-hour blocks: the day the clocks go forward is 23 hours long, so dividing
     * the gap between two midnights by a day's length read "yesterday" as "today" and left every
     * older session one day short until the clocks went back.
     */
    fun daysAgo(timestamp: Long, now: Long = System.currentTimeMillis()): Int {
        val zone = java.time.ZoneId.systemDefault()
        val then = java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(then, today).toInt()
    }
}

