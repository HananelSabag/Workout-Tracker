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
import com.hananel.workouttracker2.data.repository.SessionContext
import com.hananel.workouttracker2.data.repository.WorkoutRepository
import com.hananel.workouttracker2.receiver.TimerActionReceiver
import com.hananel.workouttracker2.utils.NotificationHelper
import com.hananel.workouttracker2.utils.RepsUtils
import kotlinx.coroutines.*

/**
 * Rest countdown, and nothing else.
 *
 * The service used to own the set counter, which is what made "finish the last set" unreliable —
 * the only code path that marked an exercise complete lived inside the countdown, so stopping the
 * timer, killing the process, or a zero-second rest all skipped it. Sets now advance exclusively
 * through [WorkoutRepository.completeSet]; this service just counts down the rest that follows a
 * set that has *already* been recorded, and its notification actions call that same method.
 */
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

        /**
         * Whether a countdown is live in this process. Used by the app to tell "the service is
         * still ticking" apart from "the persisted state outlived its process".
         */
        @Volatile
        var isRunning: Boolean = false
            private set

        const val NOTIFICATION_ID = 1003
        const val CHANNEL_ID = "timer_service"

        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_EXERCISE_ID = "exercise_id"
        const val EXTRA_REST_TIME = "rest_time"
        const val EXTRA_TOTAL_TIME = "total_time"
        const val EXTRA_CURRENT_SET = "current_set"
        const val EXTRA_TOTAL_SETS = "total_sets"
        const val EXTRA_COMPLETED_SETS = "completed_sets"
        const val EXTRA_DAY_INDEX = "day_index"
        const val EXTRA_DAY_ID = "day_id"
        const val EXTRA_DAY_NAME = "day_name"
        const val EXTRA_PLANNED_EXERCISES = "planned_exercises"
        const val EXTRA_REPS = "reps"

        /**
         * Starts the rest that follows the set the user just logged.
         * [completedSets] is the count *after* that set — the countdown never changes it.
         */
        fun startRest(
            context: Context,
            exerciseName: String,
            exerciseId: String,
            restTime: Int,
            completedSets: Int,
            totalSets: Int,
            dayIndex: Int = -1,
            dayId: String = "",
            dayName: String = "",
            plannedExercises: Int = 0,
            reps: String = "",
            /** Full rest length, when [restTime] is only the remainder being resumed. */
            totalTime: Int = restTime
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
                putExtra(EXTRA_EXERCISE_ID, exerciseId)
                putExtra(EXTRA_REST_TIME, restTime)
                putExtra(EXTRA_TOTAL_TIME, totalTime)
                putExtra(EXTRA_CURRENT_SET, completedSets)
                putExtra(EXTRA_TOTAL_SETS, totalSets)
                putExtra(EXTRA_COMPLETED_SETS, completedSets)
                putExtra(EXTRA_DAY_INDEX, dayIndex)
                putExtra(EXTRA_DAY_ID, dayId)
                putExtra(EXTRA_DAY_NAME, dayName)
                putExtra(EXTRA_PLANNED_EXERCISES, plannedExercises)
                putExtra(EXTRA_REPS, reps)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTimer(context: Context) {
            context.startService(Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
            })
        }

        /** Logs the next set from outside the app (notification action). */
        fun nextSet(context: Context) {
            context.startService(Intent(context, TimerService::class.java).apply {
                action = ACTION_NEXT_SET
            })
        }

        /** Ends the rest early without touching the set count. */
        fun skipRest(context: Context) {
            context.startService(Intent(context, TimerService::class.java).apply {
                action = ACTION_SKIP_REST
            })
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

        if (isServiceStopping && intent?.action != ACTION_START_TIMER) {
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START_TIMER -> {
                val restTime = intent.getIntExtra(EXTRA_REST_TIME, 60)
                val completedSets = intent.getIntExtra(EXTRA_COMPLETED_SETS, 0)
                val totalSets = intent.getIntExtra(EXTRA_TOTAL_SETS, 4)

                // A zero-length rest is a valid configuration ("no rest") — starting a countdown
                // for it would leave a 0:00 notification hanging around forever.
                if (restTime <= 0 || completedSets >= totalSets) {
                    stopTimerAndCleanup()
                    return START_NOT_STICKY
                }

                startRestCountdown(
                    exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: "",
                    exerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID) ?: "",
                    restTime = restTime,
                    totalTime = intent.getIntExtra(EXTRA_TOTAL_TIME, restTime).coerceAtLeast(restTime),
                    completedSets = completedSets,
                    totalSets = totalSets,
                    dayIndex = intent.getIntExtra(EXTRA_DAY_INDEX, -1),
                    dayId = intent.getStringExtra(EXTRA_DAY_ID) ?: "",
                    dayName = intent.getStringExtra(EXTRA_DAY_NAME) ?: "",
                    plannedExercises = intent.getIntExtra(EXTRA_PLANNED_EXERCISES, 0),
                    reps = intent.getStringExtra(EXTRA_REPS) ?: ""
                )
            }
            ACTION_NEXT_SET -> {
                ensureStateRestored()
                logNextSet()
            }
            ACTION_SKIP_REST -> {
                stopTimerAndCleanup()
            }
            ACTION_PAUSE_RESUME -> {
                ensureStateRestored()
                handlePauseResume()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * If the service was killed and just relaunched (e.g. the user tapped a notification action
     * while the process was dead), `currentTimerState` is null in memory but DataStore still has
     * the latest persisted state.
     */
    private fun ensureStateRestored() {
        if (currentTimerState != null) return
        currentTimerState = runBlocking { repository?.getTimerState() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Countdown ────────────────────────────────────────────────────────────

    private fun startRestCountdown(
        exerciseName: String,
        exerciseId: String,
        restTime: Int,
        totalTime: Int = restTime,
        completedSets: Int,
        totalSets: Int,
        dayIndex: Int,
        dayId: String,
        dayName: String,
        plannedExercises: Int,
        reps: String
    ) {
        isServiceStopping = false
        isPaused = false
        isRunning = true
        countDownTimer?.cancel()

        currentTimerState = TimerState(
            timeLeft = restTime,
            totalTime = totalTime,
            isRunning = true,
            exerciseName = exerciseName,
            exerciseId = exerciseId,
            currentSet = completedSets,
            totalSets = totalSets,
            completedSets = completedSets,
            isRestComplete = false,
            isPaused = false,
            endsAtEpochMs = System.currentTimeMillis() + restTime * 1000L,
            dayIndex = dayIndex,
            dayId = dayId,
            dayName = dayName,
            plannedExercises = plannedExercises,
            reps = reps
        )

        // Persist synchronously so the UI can never read a stale "rest complete" flag from the
        // previous set while a new countdown is starting.
        runBlocking { repository?.saveTimerState(currentTimerState) }

        startForeground(NOTIFICATION_ID, createTimerNotification(restTime))
        tick(restTime)
    }

    private fun tick(seconds: Int) {
        countDownTimer = object : CountDownTimer((seconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                currentTimerState = currentTimerState?.copy(timeLeft = secondsLeft)
                // Saved every tick so the in-app countdown updates once per second.
                serviceScope.launch { repository?.saveTimerState(currentTimerState) }
                updateNotification(secondsLeft)
            }

            override fun onFinish() {
                onRestComplete()
            }
        }.start()
    }

    private fun handlePauseResume() {
        val state = currentTimerState ?: return
        // Nothing to pause once the countdown has already finished — a stale notification action
        // arriving here would otherwise park the timer in a "paused at 0:00" state it can't leave.
        if (state.isRestComplete) return
        if (isPaused) {
            isPaused = false
            currentTimerState = state.copy(
                isPaused = false,
                isRunning = true,
                endsAtEpochMs = System.currentTimeMillis() + pausedTimeLeft * 1000L
            )
            runBlocking { repository?.saveTimerState(currentTimerState) }
            startForeground(NOTIFICATION_ID, createTimerNotification(pausedTimeLeft))
            tick(pausedTimeLeft)
        } else {
            countDownTimer?.cancel()
            countDownTimer = null
            isPaused = true
            pausedTimeLeft = state.timeLeft
            currentTimerState = state.copy(isPaused = true, isRunning = false)
            runBlocking { repository?.saveTimerState(currentTimerState) }
            startForeground(NOTIFICATION_ID, createTimerNotification(pausedTimeLeft))
        }
    }

    /**
     * Rest is over. The exercise's set count is untouched here — the user still has to actually do
     * the next set and log it.
     */
    private fun onRestComplete() {
        if (isServiceStopping) return
        val state = currentTimerState ?: run { stopTimerAndCleanup(); return }

        currentTimerState = state.copy(timeLeft = 0, isRunning = false, isRestComplete = true, isPaused = false)
        // Synchronous: if the OS kills the service right now, the next launch must still read
        // isRestComplete=true rather than a stale running countdown.
        runBlocking { repository?.saveTimerState(currentTimerState) }

        NotificationHelper(applicationContext).showTimerComplete(
            state.exerciseName,
            state.nextSet,
            state.totalSets
        )
        showRestCompleteNotification(state.exerciseName, state.nextSet, state.totalSets)
    }

    /**
     * Notification action: the user did the next set away from the app. Route it through the same
     * repository call the in-app button uses, then either rest again or finish the exercise.
     */
    private fun logNextSet() {
        val state = currentTimerState ?: run { stopTimerAndCleanup(); return }
        countDownTimer?.cancel()
        countDownTimer = null

        val result = runBlocking {
            repository?.completeSet(
                exerciseId = state.exerciseId,
                totalSets = state.totalSets,
                ctx = SessionContext(
                    dayIndex = state.dayIndex,
                    dayId = state.dayId,
                    dayName = state.dayName,
                    plannedExercises = state.plannedExercises,
                    exerciseName = state.exerciseName,
                    reps = state.reps
                ),
                reps = RepsUtils.targetReps(state.reps)
            )
        }

        if (result == null || result.exerciseCompleted) {
            NotificationHelper(applicationContext).showExerciseComplete(state.exerciseName)
            stopTimerAndCleanup()
        } else {
            startRestCountdown(
                exerciseName = state.exerciseName,
                exerciseId = state.exerciseId,
                restTime = state.totalTime,
                completedSets = result.completedSets,
                totalSets = state.totalSets,
                dayIndex = state.dayIndex,
                dayId = state.dayId,
                dayName = state.dayName,
                plannedExercises = state.plannedExercises,
                reps = state.reps
            )
        }
    }

    private fun stopTimerAndCleanup() {
        isServiceStopping = true
        isRunning = false
        countDownTimer?.cancel()
        countDownTimer = null
        currentTimerState = null
        isPaused = false
        pausedTimeLeft = 0

        runBlocking {
            try {
                repository?.saveTimerState(null)
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }

        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.cancel(NOTIFICATION_ID)
            // The alerting "rest is over" notification is a separate id posted by NotificationHelper;
            // without this it survives the timer and sits in the shade until tapped.
            manager?.cancel(NotificationHelper.TIMER_COMPLETE_ID)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // Ignore - service might already be stopping
        }

        stopSelf()
    }

    // ── Notifications ────────────────────────────────────────────────────────

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
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            // SINGLE_TOP reuses the existing Activity so Compose `remember` state survives.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TIMER_DIALOG", true)
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun action(requestCode: Int, actionName: String): PendingIntent {
        val intent = Intent(this, TimerActionReceiver::class.java).apply { action = actionName }
        return PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createTimerNotification(timeLeft: Int): Notification {
        val state = currentTimerState
        val nextSet = state?.nextSet ?: 1
        val totalSets = state?.totalSets ?: 1
        val pauseResumeText = if (isPaused) getString(R.string.resume) else getString(R.string.pause)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${state?.exerciseName.orEmpty()} • ${getString(R.string.rest_before_set, nextSet, totalSets)}")
            .setContentText(
                "${if (isPaused) getString(R.string.paused) + " • " else ""}" +
                        "${formatTime(timeLeft)} ${getString(R.string.remaining)}"
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_launcher_foreground, pauseResumeText, action(1, ACTION_PAUSE_RESUME))
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.skip_rest), action(2, ACTION_SKIP_REST))
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.stop), action(3, ACTION_STOP_TIMER))
            .setProgress(state?.totalTime ?: 60, timeLeft, false)
            .build()
    }

    private fun updateNotification(timeLeft: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, createTimerNotification(timeLeft))
    }

    private fun showRestCompleteNotification(exerciseName: String, nextSet: Int, totalSets: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${getString(R.string.rest_complete)} — $exerciseName")
            .setContentText(getString(R.string.time_for_set, nextSet, totalSets))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.set_done), action(4, ACTION_NEXT_SET))
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.stop), action(3, ACTION_STOP_TIMER))
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    override fun onDestroy() {
        super.onDestroy()
        isServiceStopping = true
        isRunning = false
        countDownTimer?.cancel()
        countDownTimer = null
        currentTimerState = null

        // Do NOT clear the persisted timer state here. onDestroy fires for both intentional stops
        // (stopTimerAndCleanup already cleared DataStore synchronously) and for OS process-kills
        // mid-workout. In the second case wiping DataStore would lose the rest-complete recovery
        // state — when the user re-opens the app they'd see no timer at all.

        serviceScope.cancel()
    }
}
