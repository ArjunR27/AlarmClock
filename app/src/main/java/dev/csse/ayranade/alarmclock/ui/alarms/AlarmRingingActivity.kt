package dev.csse.ayranade.alarmclock.ui.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.csse.ayranade.alarmclock.MainActivity
import dev.csse.ayranade.alarmclock.ui.theme.AppTheme

class AlarmRingingActivity : ComponentActivity() {
    private var alarmId: Int = -1
    private var receiverRegistered = false

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stoppedAlarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
            if (alarmId == -1 || stoppedAlarmId == -1 || stoppedAlarmId == alarmId) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (alarmId == -1) {
            finish()
            return
        }

        setContent {
            AppTheme(dynamicColor = false) {
                AlarmRingingScreen(
                    alarmId = alarmId,
                    onDismiss = {
                        startService(
                            Intent(this, AlarmPlaybackService::class.java)
                                .setAction(ACTION_DISMISS_ALARM)
                                .putExtra(EXTRA_ALARM_ID, alarmId)
                        )
                        returnToApp()
                    },
                    onSnooze = {
                        startService(
                            Intent(this, AlarmPlaybackService::class.java)
                                .setAction(ACTION_SNOOZE_ALARM)
                                .putExtra(EXTRA_ALARM_ID, alarmId)
                        )
                        returnToApp()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                stopReceiver,
                IntentFilter(ACTION_ALARM_STOPPED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(stopReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    private fun returnToApp() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
        finish()
    }
}

@Composable
private fun AlarmRingingScreen(
    alarmId: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    var alarm by remember { mutableStateOf<Alarm?>(null) }

    LaunchedEffect(alarmId) {
        alarm = AlarmStorage.loadAlarmMap(context)[alarmId]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = alarm?.label?.ifBlank { "Alarm" } ?: "Alarm",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = alarm?.let { String.format("%02d:%02d %s", it.hour, it.minute, if (it.am) "AM" else "PM") }
                ?: "Ringing now",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Dismiss")
        }
        Button(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Snooze ${formatSnoozeMinutes(alarm?.snoozeMinutes ?: DEFAULT_SNOOZE_MINUTES)}")
        }
    }
}
