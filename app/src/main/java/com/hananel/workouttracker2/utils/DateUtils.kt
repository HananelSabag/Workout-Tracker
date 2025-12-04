package com.hananel.workouttracker2.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
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
}

