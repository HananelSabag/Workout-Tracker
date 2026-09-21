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
    private lateinit var repository: WorkoutRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val commands = kotlinx.coroutines.channels.Channel<suspend () -> Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private var currentTimerState: TimerState? = null
    private val isPaused get() = currentTimerState?.isPaused == true
    private var lastStartId = 0

    companion object {
        const val ACTION_RECOVER = "com.hananel.workouttracker2.RECOVER"
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

            dispatch(context, intent)
        }

        private fun dispatch(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }
        fun stopTimer(context: Context) = dispatch(context, Intent(context, TimerService::class.java).setAction(ACTION_STOP_TIMER))
        fun nextSet(context: Context) = dispatch(context, Intent(context, TimerService::class.java).setAction(ACTION_NEXT_SET))
        fun skipRest(context: Context) = dispatch(context, Intent(context, TimerService::class.java).setAction(ACTION_SKIP_REST))
        fun pauseResume(context: Context) = dispatch(context, Intent(context, TimerService::class.java).setAction(ACTION_PAUSE_RESUME))
        fun recover(context: Context) = dispatch(context, Intent(context, TimerService::class.java).setAction(ACTION_RECOVER))

    }


    override fun onCreate() {
        super.onCreate()
        repository = WorkoutRepository(applicationContext)
        createNotificationChannel()
        NotificationHelper(this).createNotificationChannels()
        serviceScope.launch {
            for (command in commands) {
                try { command() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    android.util.Log.e("TimerService", "Timer operation failed; saved state retained", e)
                    shutdown(lastStartId)
                }
            }
        }
        serviceScope.launch {
            repository.getTimerStateFlow().collect { saved ->
                if (saved == null && currentTimerState != null) commands.send {
                    if (repository.getTimerState() == null) shutdown(lastStartId)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Android's foreground deadline is satisfied before any disk read or suspension.
        try { startForeground(NOTIFICATION_ID, createTimerNotification(currentTimerState?.timeLeft ?: 0)) }
        catch (e: RuntimeException) {
            android.util.Log.e("TimerService", "Foreground service unavailable", e)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        isRunning = true
        commands.trySend { handleCommand(intent, startId) }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun handleCommand(intent: Intent?, startId: Int) {
        currentTimerState = repository.getTimerState()
        val state = currentTimerState
        val expected = intent?.getStringExtra("expected_exercise")
        if (expected != null && (state?.exerciseId != expected || state.completedSets != intent.getIntExtra("expected_sets", -1))) {
            if (state == null) shutdown(startId)
            return
        }
        when (intent?.action) {
            ACTION_STOP_TIMER, ACTION_SKIP_REST -> {
                repository.clearTimerState()
                shutdown(startId)
            }
            ACTION_START_TIMER -> {
                val seconds = intent.getIntExtra(EXTRA_REST_TIME, 60).coerceIn(0, 3600)
                val total = intent.getIntExtra(EXTRA_TOTAL_SETS, 1).coerceIn(1, 100)
                val completed = intent.getIntExtra(EXTRA_COMPLETED_SETS, 0).coerceIn(0, total)
                if (seconds == 0 || completed >= total) {
                    repository.clearTimerState(); shutdown(startId); return
                }
                val id = intent.getStringExtra(EXTRA_EXERCISE_ID).orEmpty()
                if (repository.getWorkoutStructure().none { day -> day.exercises.any { it.id == id } }) {
                    repository.clearTimerState(); shutdown(startId); return
                }
                publish(TimerState(seconds, intent.getIntExtra(EXTRA_TOTAL_TIME, seconds).coerceIn(seconds, 3600), true,
                    intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty(), id, completed, total, completed,
                    endsAtEpochMs = System.currentTimeMillis() + seconds * 1000L,
                    dayIndex = intent.getIntExtra(EXTRA_DAY_INDEX, -1), dayId = intent.getStringExtra(EXTRA_DAY_ID).orEmpty(),
                    dayName = intent.getStringExtra(EXTRA_DAY_NAME).orEmpty(), plannedExercises = intent.getIntExtra(EXTRA_PLANNED_EXERCISES, 0),
                    reps = intent.getStringExtra(EXTRA_REPS).orEmpty()))
            }
            ACTION_NEXT_SET -> {
                if (state == null) { shutdown(startId); return }
                val day = repository.getWorkoutStructure().find { it.exercises.any { ex -> ex.id == state.exerciseId } }
                val ex = day?.exercises?.find { it.id == state.exerciseId }
                if (ex == null) { repository.clearTimerState(); shutdown(startId); return }
                // A repeated/stale notification cannot advance the following set as well.
                val count = repository.getWorkoutProgress().exerciseCompletedSets[state.exerciseId] ?: 0
                if (count != state.completedSets) return
                val result = repository.completeSet(ex.id, ex.sets, SessionContext(day.dayIndex, day.id,
                    com.hananel.workouttracker2.data.DisplayNames.dayName(day, resources), day.exercises.size,
                    com.hananel.workouttracker2.data.DisplayNames.exerciseName(ex, resources), ex.reps), RepsUtils.targetReps(ex.reps))
                if (result.exerciseCompleted || ex.restTime <= 0) {
                    repository.clearTimerState(); shutdown(startId)
                } else publish(state.copy(completedSets = result.completedSets, currentSet = result.completedSets,
                    totalSets = ex.sets, timeLeft = ex.restTime, totalTime = ex.restTime, isRunning = true, isPaused = false,
                    isRestComplete = false, endsAtEpochMs = System.currentTimeMillis() + ex.restTime * 1000L))
            }
            ACTION_PAUSE_RESUME -> {
                if (state == null) { shutdown(startId); return }
                if (state.isRestComplete) { render(state); return }
                val remaining = state.remainingSeconds()
                if (remaining <= 0) finishRest(state)
                else if (state.isPaused) publish(state.copy(isPaused = false, isRunning = true,
                    endsAtEpochMs = System.currentTimeMillis() + remaining * 1000L))
                else publish(state.copy(isPaused = true, isRunning = false, timeLeft = remaining, endsAtEpochMs = 0L))
            }
            else -> {
                if (state == null) { shutdown(startId); return }
                if (repository.getWorkoutStructure().none { day -> day.exercises.any { it.id == state.exerciseId } }) {
                    repository.clearTimerState(); shutdown(startId); return
                }
                if (!state.isPaused && !state.isRestComplete && state.remainingSeconds() <= 0) finishRest(state)
                else render(state)
            }
        }
    }

    private suspend fun publish(state: TimerState) {
        repository.saveTimerState(state)
        render(state)
    }

    private fun render(state: TimerState) {
        countDownTimer?.cancel()
        currentTimerState = state
        isRunning = true
        if (state.isRestComplete) { showRestCompleteNotification(state.exerciseName, state.nextSet, state.totalSets); return }
        val seconds = state.remainingSeconds()
        updateNotification(seconds)
        if (state.isPaused) return
        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val left = state.remainingSeconds()
                currentTimerState = state.copy(timeLeft = left)
                updateNotification(left)
            }
            override fun onFinish() {
                commands.trySend {
                    // Ignore completion from a replaced/stopped timer.
                    if (repository.getTimerState() == state) finishRest(state)
                }
            }
        }.start()
    }

    private suspend fun finishRest(state: TimerState) {
        publish(state.copy(timeLeft = 0, isRunning = false, isRestComplete = true, isPaused = false, endsAtEpochMs = 0L))
        if (repository.getUserPreferences().notificationsEnabled) NotificationHelper(this).showTimerComplete(state.exerciseName, state.nextSet, state.totalSets)
    }

    private fun shutdown(startId: Int) {
        countDownTimer?.cancel()
        countDownTimer = null
        currentTimerState = null
        isRunning = false
        if (startId == lastStartId) {
            getSystemService(NotificationManager::class.java).cancel(NotificationHelper.TIMER_COMPLETE_ID)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(startId)
        }
    }

    private fun postNotification(notification: Notification) {
        try {
            if (getSystemService(NotificationManager::class.java).areNotificationsEnabled())
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) { android.util.Log.w("TimerService", "Notifications disabled", e) }
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
        val intent = Intent(this, TimerService::class.java).apply {
            action = actionName
            putExtra("expected_exercise", currentTimerState?.exerciseId)
            putExtra("expected_sets", currentTimerState?.completedSets ?: -1)
            data = android.net.Uri.parse("workout://timer/$actionName/${currentTimerState?.exerciseId}/${currentTimerState?.completedSets}")
        }
        return PendingIntent.getForegroundService(
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
        postNotification(createTimerNotification(timeLeft))
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

        postNotification(notification)
    }

    private fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)


    override fun onDestroy() {
        countDownTimer?.cancel()
        commands.close()
        serviceScope.cancel()
        isRunning = false
        // An OS stop keeps the persisted deadline/paused state for recovery.
        super.onDestroy()
    }
}
