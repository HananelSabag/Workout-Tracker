package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = WorkoutRepository(context).getUserPreferences()
                val helper = NotificationHelper(context)
                prefs.workoutAlarms.forEach { alarm ->
                    if (prefs.notificationsEnabled && alarm.isEnabled) helper.scheduleWorkoutReminder(alarm)
                    else helper.cancelWorkoutReminder(alarm.id)
                }
            } catch (e: Exception) { android.util.Log.e("BootReceiver", "Could not restore reminders", e) }
            finally { pending.finish() }
        }
    }
}
