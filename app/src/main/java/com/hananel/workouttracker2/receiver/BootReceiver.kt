package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all enabled alarms after device boot
            val repository = WorkoutRepository(context)
            val notificationHelper = NotificationHelper(context)
            
            // Use coroutine to load alarms
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val preferences = repository.getUserPreferences()
                    preferences.workoutAlarms
                        .filter { it.isEnabled }
                        .forEach { alarm ->
                            notificationHelper.scheduleWorkoutReminder(alarm)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

