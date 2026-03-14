package dev.csse.ayranade.alarmclock.ui.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.ZoneId
import java.time.ZonedDateTime

internal const val ALARM_CHANNEL_ID = "alarm_ringing"
internal const val ALARM_NOTIFICATION_ID = 4001
internal const val EXTRA_ALARM_ID = "extra_alarm_id"
internal const val EXTRA_IS_SNOOZE = "extra_is_snooze"
internal const val ACTION_TRIGGER_ALARM = "dev.csse.ayranade.alarmclock.action.TRIGGER_ALARM"
internal const val ACTION_START_RINGING = "dev.csse.ayranade.alarmclock.action.START_RINGING"
internal const val ACTION_DISMISS_ALARM = "dev.csse.ayranade.alarmclock.action.DISMISS_ALARM"
internal const val ACTION_SNOOZE_ALARM = "dev.csse.ayranade.alarmclock.action.SNOOZE_ALARM"
internal const val ACTION_ALARM_STOPPED = "dev.csse.ayranade.alarmclock.action.ALARM_STOPPED"

private const val SNOOZE_REQUEST_CODE_OFFSET = 100_000

object AlarmScheduler {
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun scheduleAlarm(context: Context, alarm: Alarm): Boolean {
        if (!alarm.isEnabled || !canScheduleExactAlarms(context)) {
            return false
        }

        val triggerAtMillis = computeNextTriggerAtMillis(alarm) ?: return false
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val showIntent = buildAlarmContentPendingIntent(context, alarm.alarmId)
        val fireIntent = buildAlarmPendingIntent(context, alarm.alarmId, isSnooze = false)

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            fireIntent
        )
        return true
    }

    fun scheduleSnooze(context: Context, alarmId: Int, snoozeMinutes: Int): Boolean {
        if (!canScheduleExactAlarms(context)) {
            return false
        }

        val triggerAtMillis =
            System.currentTimeMillis() + normalizeSnoozeMinutes(snoozeMinutes) * 60_000L
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val showIntent = buildAlarmContentPendingIntent(context, alarmId)
        val fireIntent = buildAlarmPendingIntent(context, alarmId, isSnooze = true)

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            fireIntent
        )
        return true
    }

    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(buildAlarmPendingIntent(context, alarmId, isSnooze = false))
        alarmManager.cancel(buildAlarmPendingIntent(context, alarmId, isSnooze = true))
    }

    fun reconcileEnabledAlarms(context: Context, alarms: Collection<Alarm>) {
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleAlarm(context, alarm)
            } else {
                cancelAlarm(context, alarm.alarmId)
            }
        }
    }

    fun buildAlarmPendingIntent(
        context: Context,
        alarmId: Int,
        isSnooze: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_TRIGGER_ALARM)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_IS_SNOOZE, isSnooze)

        return PendingIntent.getBroadcast(
            context,
            pendingIntentRequestCode(alarmId, isSnooze),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildAlarmContentPendingIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmRingingActivity::class.java)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

        return PendingIntent.getActivity(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildServiceActionPendingIntent(
        context: Context,
        action: String,
        alarmId: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmPlaybackService::class.java)
            .setAction(action)
            .putExtra(EXTRA_ALARM_ID, alarmId)

        return PendingIntent.getService(
            context,
            pendingIntentRequestCode(alarmId, isSnooze = false) + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntentRequestCode(alarmId: Int, isSnooze: Boolean): Int =
        if (isSnooze) {
            alarmId + SNOOZE_REQUEST_CODE_OFFSET
        } else {
            alarmId
        }

    private fun computeNextTriggerAtMillis(alarm: Alarm): Long? {
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val targetHour = to24Hour(alarm.hour, alarm.am)

        if (alarm.daysOfWeek.isEmpty()) {
            var candidate = now
                .withHour(targetHour)
                .withMinute(alarm.minute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)

            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1)
            }

            return candidate.toInstant().toEpochMilli()
        }

        val selectedDays = alarm.daysOfWeek.toSet()
        for (dayOffset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
            if (candidateDate.dayOfWeek.value !in selectedDays) {
                continue
            }

            val candidate = candidateDate
                .atTime(targetHour, alarm.minute.coerceIn(0, 59))
                .atZone(zoneId)

            if (candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }

        return null
    }

    private fun to24Hour(hour: Int, am: Boolean): Int {
        val normalizedHour = when {
            hour <= 0 -> 12
            hour > 12 -> hour % 12
            else -> hour
        }

        return when {
            am && normalizedHour == 12 -> 0
            !am && normalizedHour == 12 -> 12
            am -> normalizedHour
            else -> normalizedHour + 12
        }
    }
}
