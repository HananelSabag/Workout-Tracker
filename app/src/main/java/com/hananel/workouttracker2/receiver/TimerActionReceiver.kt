package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.service.TimerService

/** Compatibility with notifications posted before upgrading. New actions target the service. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                TimerService.ACTION_SKIP_REST -> TimerService.skipRest(context)
                TimerService.ACTION_STOP_TIMER -> TimerService.stopTimer(context)
                TimerService.ACTION_NEXT_SET -> TimerService.nextSet(context)
                TimerService.ACTION_PAUSE_RESUME -> TimerService.pauseResume(context)
            }
        } catch (e: RuntimeException) { android.util.Log.w("TimerActionReceiver", "Timer action unavailable", e) }
    }
}
