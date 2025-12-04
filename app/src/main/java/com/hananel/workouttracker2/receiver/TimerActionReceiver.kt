package com.hananel.workouttracker2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hananel.workouttracker2.service.TimerService

class TimerActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerService.ACTION_SKIP_REST -> {
                // Handle skip rest / next set
                TimerService.nextSet(context)
            }
            TimerService.ACTION_STOP_TIMER -> {
                // Handle stop timer
                TimerService.stopTimer(context)
            }
            TimerService.ACTION_NEXT_SET -> {
                // Handle next set
                TimerService.nextSet(context)
            }
        }
    }
}

