package com.hananel.workouttracker2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hananel.workouttracker2.MainActivity
import com.hananel.workouttracker2.R
import com.hananel.workouttracker2.data.models.TimerState
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.receiver.TimerActionReceiver
import com.hananel.workouttracker2.utils.NotificationHelper
import kotlinx.coroutines.*

class TimerService : Service() {
    
    private var countDownTimer: CountDownTimer? = null
    private var repository: WorkoutRepository? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentTimerState: TimerState? = null
    
    companion object {
        const val ACTION_START_TIMER = "com.hananel.workouttracker2.START_TIMER"
        const val ACTION_STOP_TIMER = "com.hananel.workouttracker2.STOP_TIMER"
        const val ACTION_NEXT_SET = "com.hananel.workouttracker2.NEXT_SET"
        const val ACTION_SKIP_REST = "com.hananel.workouttracker2.SKIP_REST"
        
        const val NOTIFICATION_ID = 1003
        const val CHANNEL_ID = "timer_service"
        
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_EXERCISE_ID = "exercise_id"
        const val EXTRA_REST_TIME = "rest_time"
        const val EXTRA_CURRENT_SET = "current_set"
        const val EXTRA_TOTAL_SETS = "total_sets"
        const val EXTRA_COMPLETED_SETS = "completed_sets"
        
        fun startTimer(
            context: Context,
            exerciseName: String,
            exerciseId: String,
            restTime: Int,
            currentSet: Int,
            totalSets: Int,
            completedSets: Int
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
                putExtra(EXTRA_EXERCISE_ID, exerciseId)
                putExtra(EXTRA_REST_TIME, restTime)
                putExtra(EXTRA_CURRENT_SET, currentSet)
                putExtra(EXTRA_TOTAL_SETS, totalSets)
                putExtra(EXTRA_COMPLETED_SETS, completedSets)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            context.startService(intent)
        }
        
        fun nextSet(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_NEXT_SET
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        repository = WorkoutRepository(applicationContext)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: ""
                val exerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID) ?: ""
                val restTime = intent.getIntExtra(EXTRA_REST_TIME, 60)
                val currentSet = intent.getIntExtra(EXTRA_CURRENT_SET, 1)
                val totalSets = intent.getIntExtra(EXTRA_TOTAL_SETS, 4)
                val completedSets = intent.getIntExtra(EXTRA_COMPLETED_SETS, 0)
                
                startTimerCountdown(
                    exerciseName = exerciseName,
                    exerciseId = exerciseId,
                    restTime = restTime,
                    currentSet = currentSet,
                    totalSets = totalSets,
                    completedSets = completedSets
                )
            }
            ACTION_STOP_TIMER -> {
                stopTimerAndCleanup()
            }
            ACTION_NEXT_SET -> {
                handleNextSet()
            }
            ACTION_SKIP_REST -> {
                handleNextSet()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startTimerCountdown(
        exerciseName: String,
        exerciseId: String,
        restTime: Int,
        currentSet: Int,
        totalSets: Int,
        completedSets: Int
    ) {
        // Cancel any existing timer
        countDownTimer?.cancel()
        
        // Create timer state
        currentTimerState = TimerState(
            timeLeft = restTime,
            totalTime = restTime,
            isRunning = true,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            currentSet = currentSet,
            totalSets = totalSets,
            completedSets = completedSets
        )
        
        // Save initial state
        serviceScope.launch {
            repository?.saveTimerState(currentTimerState)
        }
        
        // Show foreground notification
        startForeground(NOTIFICATION_ID, createTimerNotification(restTime, exerciseName, currentSet, totalSets))
        
        // Start countdown
        countDownTimer = object : CountDownTimer((restTime * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                
                // Update state
                currentTimerState = currentTimerState?.copy(timeLeft = secondsLeft)
                
                // Save to DataStore
                serviceScope.launch {
                    repository?.saveTimerState(currentTimerState)
                }
                
                // Update notification
                updateNotification(secondsLeft, exerciseName, currentSet, totalSets)
            }
            
            override fun onFinish() {
                onTimerComplete()
            }
        }.start()
    }
    
    private fun onTimerComplete() {
        // Show completion notification
        showCompletionNotification()
        
        // Keep state but mark as not running
        currentTimerState = currentTimerState?.copy(timeLeft = 0, isRunning = false)
        serviceScope.launch {
            repository?.saveTimerState(currentTimerState)
        }
        
        // Update notification to show completion
        updateNotification(0, currentTimerState?.exerciseName ?: "", 
            currentTimerState?.currentSet ?: 1, currentTimerState?.totalSets ?: 4)
    }
    
    private fun handleNextSet() {
        currentTimerState?.let { state ->
            val newCompletedSets = state.completedSets + 1
            
            serviceScope.launch {
                // Update completed sets in progress
                repository?.updateExerciseCompletedSets(
                    state.exerciseId,
                    newCompletedSets,
                    state.totalSets
                )
                
                // Check if all sets are complete
                if (newCompletedSets >= state.totalSets) {
                    // All sets complete!
                    showAllSetsCompleteNotification(state.exerciseName)
                    stopTimerAndCleanup()
                } else {
                    // Start next set timer
                    val nextSet = newCompletedSets + 1
                    startTimerCountdown(
                        exerciseName = state.exerciseName,
                        exerciseId = state.exerciseId,
                        restTime = state.totalTime,
                        currentSet = nextSet,
                        totalSets = state.totalSets,
                        completedSets = newCompletedSets
                    )
                }
            }
        }
    }
    
    private fun stopTimerAndCleanup() {
        countDownTimer?.cancel()
        countDownTimer = null
        
        serviceScope.launch {
            repository?.saveTimerState(null)
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_timer),
                NotificationManager.IMPORTANCE_LOW // LOW so it doesn't make sound during countdown
            ).apply {
                description = "Rest timer notifications"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createTimerNotification(timeLeft: Int, exerciseName: String, currentSet: Int, totalSets: Int): Notification {
        // Intent to open app
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Skip rest action
        val skipIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_SKIP_REST
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            this, 1, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Stop timer action
        val stopIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.rest_timer))
            .setContentText("$exerciseName - ${getString(R.string.set_of, currentSet, totalSets)}")
            .setSubText(formatTime(timeLeft))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.next_set),
                skipPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.stop),
                stopPendingIntent
            )
            .setProgress(currentTimerState?.totalTime ?: 60, timeLeft, false)
            .build()
    }
    
    private fun updateNotification(timeLeft: Int, exerciseName: String, currentSet: Int, totalSets: Int) {
        val notification = createTimerNotification(timeLeft, exerciseName, currentSet, totalSets)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompletionNotification() {
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showTimerComplete(
            currentTimerState?.exerciseName ?: "",
            currentTimerState?.currentSet ?: 1,
            currentTimerState?.totalSets ?: 4
        )
    }
    
    private fun showAllSetsCompleteNotification(exerciseName: String) {
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showExerciseComplete(exerciseName)
    }
    
    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        serviceScope.cancel()
    }
}

