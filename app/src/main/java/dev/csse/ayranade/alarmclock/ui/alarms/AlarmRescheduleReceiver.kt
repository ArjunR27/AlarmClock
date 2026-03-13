package dev.csse.ayranade.alarmclock.ui.alarms

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED &&
                    !AlarmScheduler.canScheduleExactAlarms(context)
                ) {
                    return@launch
                }

                val alarms = AlarmStorage.loadAlarmMap(context).values
                AlarmScheduler.reconcileEnabledAlarms(context, alarms)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
