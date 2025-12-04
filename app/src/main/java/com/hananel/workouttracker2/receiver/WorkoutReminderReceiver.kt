package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Get custom message and alarm ID
        val message = intent.getStringExtra("message")
        val alarmId = intent.getStringExtra("alarm_id")
        
        // Show workout reminder notification
        NotificationHelper(context).showWorkoutReminder(message)
        
        // Reschedule the alarm for next week (since setExact is one-time)
        if (alarmId != null) {
            val repository = WorkoutRepository(context)
            val notificationHelper = NotificationHelper(context)
            
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val preferences = repository.getUserPreferences()
                    val alarm = preferences.workoutAlarms.find { it.id == alarmId }
                    
                    if (alarm != null && alarm.isEnabled) {
                        // Reschedule for next occurrence
                        notificationHelper.scheduleWorkoutReminder(alarm)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

