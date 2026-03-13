package dev.csse.ayranade.alarmclock.ui.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1) {
            return
        }

        val serviceIntent = Intent(context, AlarmPlaybackService::class.java)
            .setAction(ACTION_START_RINGING)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_IS_SNOOZE, intent.getBooleanExtra(EXTRA_IS_SNOOZE, false))

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
