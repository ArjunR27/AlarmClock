package dev.csse.ayranade.alarmclock.ui.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.csse.ayranade.alarmclock.R
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_STABLE_ID
import dev.csse.ayranade.alarmclock.ui.audios.resolveAlarmSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmPlaybackService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var currentPlayer: MediaPlayer? = null
    private var currentAlarmId: Int? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RINGING -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                if (alarmId == -1) {
                    stopSelfResult(startId)
                    return START_NOT_STICKY
                }

                startForeground(
                    ALARM_NOTIFICATION_ID,
                    buildNotification(
                        alarmId = alarmId,
                        title = "Alarm ringing",
                        snoozeMinutes = DEFAULT_SNOOZE_MINUTES
                    )
                )

                serviceScope.launch {
                    handleTriggeredAlarm(
                        alarmId = alarmId,
                        isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
                    )
                }
            }

            ACTION_DISMISS_ALARM -> {
                stopRinging()
            }

            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, currentAlarmId ?: -1)
                if (alarmId != -1) {
                    serviceScope.launch {
                        val alarm = AlarmStorage.loadAlarmMap(applicationContext)[alarmId]
                        AlarmScheduler.scheduleSnooze(
                            applicationContext,
                            alarmId,
                            alarm?.snoozeMinutes ?: DEFAULT_SNOOZE_MINUTES
                        )
                        stopRinging()
                    }
                } else {
                    stopRinging()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayer()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleTriggeredAlarm(alarmId: Int, isSnooze: Boolean) {
        val alarms = AlarmStorage.loadAlarmMap(applicationContext).toMutableMap()
        val alarm = alarms[alarmId]

        if (alarm == null || (!alarm.isEnabled && !isSnooze)) {
            stopRinging()
            return
        }

        currentAlarmId = alarmId

        if (!isSnooze) {
            if (alarm.daysOfWeek.isEmpty()) {
                alarms[alarmId] = alarm.copy(isEnabled = false)
                AlarmStorage.saveAlarms(applicationContext, alarms)
                AlarmScheduler.cancelAlarm(applicationContext, alarmId)
            } else {
                AlarmScheduler.scheduleAlarm(applicationContext, alarm)
            }
        }

        val sound = resolveAlarmSound(applicationContext, alarm.resolvedSoundId)
            ?: resolveAlarmSound(applicationContext, DEFAULT_ALARM_STABLE_ID)
        val title = alarm.label.ifBlank { "Alarm" }

        val notification = buildNotification(
            alarmId = alarmId,
            title = title,
            snoozeMinutes = alarm.snoozeMinutes
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)

        val soundUri = sound?.fileUri ?: return stopRinging()
        if (!playAlarm(soundUri)) {
            val fallbackSound = resolveAlarmSound(applicationContext, DEFAULT_ALARM_STABLE_ID)
            if (fallbackSound == null || !playAlarm(fallbackSound.fileUri)) {
                stopRinging()
            }
        }
    }

    private suspend fun playAlarm(soundUri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            stopPlayer()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setDataSource(applicationContext, Uri.parse(soundUri))
                prepare()
                start()
            }
            currentPlayer = player
        }.isSuccess
    }

    private fun stopRinging() {
        val alarmId = currentAlarmId
        stopPlayer()
        currentAlarmId = null
        sendBroadcast(
            Intent(ACTION_ALARM_STOPPED)
                .setPackage(packageName)
                .putExtra(EXTRA_ALARM_ID, alarmId ?: -1)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun stopPlayer() {
        currentPlayer?.release()
        currentPlayer = null
    }

    private fun buildNotification(
        alarmId: Int,
        title: String,
        snoozeMinutes: Int
    ) =
        NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Tap to manage this alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(
                AlarmScheduler.buildAlarmContentPendingIntent(this, alarmId),
                true
            )
            .setContentIntent(AlarmScheduler.buildAlarmContentPendingIntent(this, alarmId))
            .addAction(
                0,
                "Dismiss",
                AlarmScheduler.buildServiceActionPendingIntent(this, ACTION_DISMISS_ALARM, alarmId)
            )
            .addAction(
                0,
                "Snooze ${formatSnoozeMinutes(snoozeMinutes)}",
                AlarmScheduler.buildServiceActionPendingIntent(this, ACTION_SNOOZE_ALARM, alarmId)
            )
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Alarm ringing",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Foreground notifications for active alarms"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
