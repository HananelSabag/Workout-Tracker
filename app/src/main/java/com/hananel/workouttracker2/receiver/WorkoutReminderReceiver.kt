package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.*

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("alarm_id") ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = WorkoutRepository(context).getUserPreferences()
                val alarm = prefs.workoutAlarms.find { it.id == id && it.isEnabled }
                val helper = NotificationHelper(context)
                if (alarm != null && prefs.notificationsEnabled) {
                    helper.showWorkoutReminder(alarm.customMessage.ifBlank { null })
                    helper.scheduleWorkoutReminder(alarm)
                } else helper.cancelWorkoutReminder(id)
            } catch (e: Exception) { android.util.Log.e("WorkoutReminder", "Could not process reminder", e) }
            finally { pending.finish() }
        }
    }
}
