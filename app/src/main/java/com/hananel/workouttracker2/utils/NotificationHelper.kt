package com.hananel.workouttracker2.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hananel.workouttracker2.MainActivity
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.WorkoutAlarm
import com.hananel.workouttracker2.receiver.WorkoutReminderReceiver
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_WORKOUT_REMINDERS = "workout_reminders"
        private const val CHANNEL_TIMER = "timer"
        private const val CHANNEL_WEEKLY = "weekly_updates"

        /** Alerting "rest is over" notification — TimerService clears it when the timer stops. */
        const val TIMER_COMPLETE_ID = 1002
    }
    
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Workout Reminders Channel
            val workoutChannel = NotificationChannel(
                CHANNEL_WORKOUT_REMINDERS,
                context.getString(R.string.notification_channel_workout_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled workouts"
                enableVibration(true)
            }
            
            // Timer Channel
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                context.getString(R.string.notification_channel_timer),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rest timer notifications"
                enableVibration(true)
            }
            
            // Weekly Updates Channel
            val weeklyChannel = NotificationChannel(
                CHANNEL_WEEKLY,
                context.getString(R.string.notification_channel_weekly),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly progress updates"
            }
            
            notificationManager.createNotificationChannels(
                listOf(workoutChannel, timerChannel, weeklyChannel)
            )
        }
    }
    
    fun showWorkoutReminder(message: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_workout_reminder_title))
            .setContentText(message ?: context.getString(R.string.notification_workout_reminder_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1001, notification)
    }
    
    fun showTimerComplete(exerciseName: String, currentSet: Int, totalSets: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TIMER_DIALOG", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_timer_complete_title))
            .setContentText("$exerciseName - ${context.getString(R.string.rest_complete_start_set, currentSet)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()
        
        notificationManager.notify(TIMER_COMPLETE_ID, notification)
    }
    
    fun showExerciseComplete(exerciseName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.exercise_completed))
            .setContentText("$exerciseName ${context.getString(R.string.completed_all_sets)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 100, 300, 100, 300))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()
        
        notificationManager.notify(1004, notification)
    }
    
    fun showWeeklyReset() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_WEEKLY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_weekly_reset_title))
            .setContentText(context.getString(R.string.notification_weekly_reset_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1005, notification) // 1005 — avoids collision with TimerService.NOTIFICATION_ID (1003)
    }

    fun scheduleWorkoutReminder(alarm: WorkoutAlarm) {
        val timeParts = alarm.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val alarmId = alarm.id.hashCode()
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        alarm.daysOfWeek.forEach { dayOfWeek ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If time has passed today, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            
            val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
                putExtra("message", alarm.customMessage.ifEmpty { null })
                putExtra("alarm_id", alarm.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId * 10 + dayOfWeek, // Unique ID for each day
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use setExactAndAllowWhileIdle for better reliability
            // This works even in Doze mode and when app is closed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
    
    fun cancelWorkoutReminder(alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val id = alarmId.hashCode()
        
        // Cancel for all days (0-6)
        (0..6).forEach { dayOfWeek ->
            val intent = Intent(context, WorkoutReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id * 10 + dayOfWeek,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

