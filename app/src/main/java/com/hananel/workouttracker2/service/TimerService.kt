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
    private var isPaused = false
    private var pausedTimeLeft = 0
    private var isServiceStopping = false // Prevent race conditions
    
    companion object {
        const val ACTION_START_TIMER = "com.hananel.workouttracker2.START_TIMER"
        const val ACTION_STOP_TIMER = "com.hananel.workouttracker2.STOP_TIMER"
        const val ACTION_NEXT_SET = "com.hananel.workouttracker2.NEXT_SET"
        const val ACTION_SKIP_REST = "com.hananel.workouttracker2.SKIP_REST"
        const val ACTION_PAUSE_RESUME = "com.hananel.workouttracker2.PAUSE_RESUME"
        
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
        // ACTION_STOP_TIMER always gets processed - no matter what state we're in
        if (intent?.action == ACTION_STOP_TIMER) {
            stopTimerAndCleanup()
            return START_NOT_STICKY
        }
        
        // Prevent other actions when service is stopping
        if (isServiceStopping) {
            return START_NOT_STICKY
        }
        
        when (intent?.action) {
            ACTION_START_TIMER -> {
                // Prevent starting new timer if one is already running
                if (currentTimerState != null && currentTimerState?.isRunning == true) {
                    // Timer already running - ignore this request
                    return START_STICKY
                }
                
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
            ACTION_NEXT_SET -> {
                ensureStateRestored()
                handleNextSet()
            }
            ACTION_SKIP_REST -> {
                ensureStateRestored()
                handleNextSet()
            }
            ACTION_PAUSE_RESUME -> {
                handlePauseResume()
            }
        }
        
        return START_STICKY
    }
    
    /**
     * If the service was killed and just relaunched (e.g. user tapped "Next Set" on the
     * rest-complete notification while the process was dead), `currentTimerState` is null in memory
     * but DataStore still has the latest persisted state. Restore it so handleNextSet() can run.
     */
    private fun ensureStateRestored() {
        if (currentTimerState != null) return
        val persisted = runBlocking { repository?.getTimerState() }
        if (persisted != null) {
            currentTimerState = persisted
        }
    }

    private fun handlePauseResume() {
        if (isPaused) {
            // Resume: start countdown with paused time
            isPaused = false
            currentTimerState?.let { state ->
                startTimerCountdown(
                    exerciseName = state.exerciseName,
                    exerciseId = state.exerciseId,
                    restTime = pausedTimeLeft,
                    currentSet = state.currentSet,
                    totalSets = state.totalSets,
                    completedSets = state.completedSets
                )
            }
        } else {
            // Pause: cancel timer and save current time
            countDownTimer?.cancel()
            isPaused = true
            currentTimerState?.let { state ->
                pausedTimeLeft = state.timeLeft
                // Update notification to show paused state
                val notification = createTimerNotification(
                    pausedTimeLeft,
                    state.exerciseName,
                    state.currentSet,
                    state.totalSets
                )
                startForeground(NOTIFICATION_ID, notification)
            }
        }
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
        // CRITICAL: Reset stopping flag when starting a new timer
        isServiceStopping = false
        isPaused = false
        
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
            completedSets = completedSets,
            isRestComplete = false
        )

        // Save initial state synchronously so the UI never reads a stale "rest complete" flag from a
        // previous set when a new countdown is starting. The single runBlocking call is fast (writes
        // a small JSON to DataStore) and prevents a race where handleNextSet → startTimerCountdown
        // returns before the OFF-flag is persisted.
        runBlocking {
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

                // Save every tick so the UI updates every second (real-time countdown)
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
        // Check if already stopping to prevent race conditions
        if (isServiceStopping) return
        
        // Mark as stopping immediately to prevent duplicate calls
        isServiceStopping = true
        
        currentTimerState?.let { state ->
            // Check if this was the last set
            val isLastSet = state.currentSet >= state.totalSets
            
            if (isLastSet) {
                // Last set complete - stop everything
                showAllSetsCompleteNotification(state.exerciseName)

                // Save progress SYNCHRONOUSLY before cleanup — async would race with stopSelf()
                runBlocking {
                    repository?.updateExerciseCompletedSets(
                        state.exerciseId,
                        state.totalSets,
                        state.totalSets
                    )
                }

                // Stop service and clear state
                stopTimerAndCleanup()
            } else {
                // Not last set - reset stopping flag since we're continuing
                isServiceStopping = false
                
                // Show completion notification with sound to alert user
                showCompletionNotification()
                
                // Update state to show timer completed but waiting for user action.
                // Persist synchronously — if the service is killed by the OS right after the rest
                // finishes, the next launch must read isRestComplete=true from DataStore so the UI
                // shows "Start Next Set" instead of stale "Resume Rest".
                currentTimerState = state.copy(
                    timeLeft = 0,
                    isRunning = false,
                    isRestComplete = true
                )
                runBlocking {
                    repository?.saveTimerState(currentTimerState)
                }
                
                // Update notification to show "rest complete - tap for next set"
                showRestCompleteNotification(state.exerciseName, state.currentSet, state.totalSets)
            }
        } ?: run {
            // No state - just stop
            stopTimerAndCleanup()
        }
    }
    
    private fun showRestCompleteNotification(exerciseName: String, currentSet: Int, totalSets: Int) {
        // Intent to open app and show timer dialog — use SINGLE_TOP to preserve Activity state
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TIMER_DIALOG", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Next Set action - proceed to next set timer
        val nextSetIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_NEXT_SET
        }
        val nextSetPendingIntent = PendingIntent.getBroadcast(
            this, 4, nextSetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Stop timer action
        val stopIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${getString(R.string.rest_complete)} - $exerciseName")
            .setContentText("${getString(R.string.set_of, currentSet, totalSets)} • ${getString(R.string.tap_for_next_set)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.next_set),
                nextSetPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.stop),
                stopPendingIntent
            )
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun handleNextSet() {
        // Double-check stopping flag
        if (isServiceStopping) return
        
        // IMPORTANT: Cancel any running countdown timer FIRST
        // This prevents race conditions when user clicks Next Set mid-countdown
        countDownTimer?.cancel()
        countDownTimer = null
        
        currentTimerState?.let { state ->
            val newCompletedSets = state.completedSets + 1
            
            // Check if all sets will be complete after this
            if (newCompletedSets >= state.totalSets) {
                // All sets complete - update progress synchronously
                runBlocking {
                    repository?.updateExerciseCompletedSets(
                        state.exerciseId,
                        newCompletedSets,
                        state.totalSets
                    )
                }
                
                showAllSetsCompleteNotification(state.exerciseName)
                stopTimerAndCleanup()
            } else {
                // Update completed sets
                serviceScope.launch {
                    repository?.updateExerciseCompletedSets(
                        state.exerciseId,
                        newCompletedSets,
                        state.totalSets
                    )
                }
                
                // Start next set timer (this also resets isServiceStopping)
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
        } ?: run {
            // No timer state - stop service
            stopTimerAndCleanup()
        }
    }
    
    private fun stopTimerAndCleanup() {
        // Mark service as stopping immediately
        isServiceStopping = true
        
        // Cancel any running timer immediately
        countDownTimer?.cancel()
        countDownTimer = null
        
        // Clear local state immediately
        currentTimerState = null
        isPaused = false
        pausedTimeLeft = 0
        
        // Clear persisted state SYNCHRONOUSLY to ensure it's done before service stops
        runBlocking {
            try {
                repository?.saveTimerState(null)
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }
        
        // Explicitly cancel the notification
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
        
        // Stop foreground and service
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // Ignore - service might already be stopping
        }
        
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
        // Intent to open app — use SINGLE_TOP so existing Activity is reused (preserves remember state)
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TIMER_DIALOG", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Pause/Resume action
        val pauseResumeIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_PAUSE_RESUME
        }
        val pauseResumePendingIntent = PendingIntent.getBroadcast(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Skip rest action
        val skipIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_SKIP_REST
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            this, 2, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Stop timer action
        val stopIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val timeDisplay = formatTime(timeLeft)
        val pauseResumeText = if (isPaused) getString(R.string.resume) else getString(R.string.pause)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$exerciseName • ${getString(R.string.set_of, currentSet, totalSets)}")
            .setContentText("${if (isPaused) getString(R.string.paused) + " • " else ""}$timeDisplay ${getString(R.string.remaining)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_launcher_foreground,
                pauseResumeText,
                pauseResumePendingIntent
            )
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
        isServiceStopping = true
        countDownTimer?.cancel()
        countDownTimer = null
        currentTimerState = null

        // Do NOT clear the persisted timer state here. onDestroy fires for both intentional stops
        // (in which case stopTimerAndCleanup already synchronously cleared DataStore) and for OS
        // process-kills mid-workout. In the second case wiping DataStore would lose the rest-
        // complete recovery state — when the user re-opens the app they'd see no timer at all.

        serviceScope.cancel()
    }
}

