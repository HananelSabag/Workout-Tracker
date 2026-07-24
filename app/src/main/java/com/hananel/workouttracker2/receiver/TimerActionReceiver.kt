package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.service.TimerService

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // "דלג" — end the rest early. Deliberately does NOT advance the set count; skipping a
            // rest is not the same as having done the next set.
            TimerService.ACTION_SKIP_REST -> TimerService.skipRest(context)
            TimerService.ACTION_STOP_TIMER -> TimerService.stopTimer(context)
            TimerService.ACTION_NEXT_SET -> TimerService.nextSet(context)
            TimerService.ACTION_PAUSE_RESUME -> {
                context.startService(Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_PAUSE_RESUME
                })
            }
        }
    }
}
