package dev.csse.ayranade.alarmclock.ui.audios

import android.content.Context
import android.provider.Settings

internal const val DEFAULT_NO_POLE_SOUND_ID = 1
internal const val DEFAULT_NOTIFICATION_SOUND_ID = 2
internal const val DEFAULT_ALARM_SOUND_ID = 3

internal const val DEFAULT_NO_POLE_STABLE_ID = "default:no_pole"
internal const val DEFAULT_NOTIFICATION_STABLE_ID = "default:notification"
const val DEFAULT_ALARM_STABLE_ID = "default:alarm"
const val DEFAULT_ALARM_SOUND_NAME = "Default Alarm Sound"

private const val SOUND_RESOURCE_PATH = "android.resource://dev.csse.ayranade.alarmclock/raw/"

private fun customStableSoundId(id: Int) = "custom:$id"

internal fun AlarmSoundEntity.toAlarmSound(): AlarmSound =
    AlarmSound(
        stableId = customStableSoundId(id),
        alarmSoundId = id,
        name = name,
        fileUri = fileUri,
        isCustom = isCustom
    )

fun defaultAlarmSounds(): List<AlarmSound> =
    listOf(
        AlarmSound(
            stableId = DEFAULT_NO_POLE_STABLE_ID,
            alarmSoundId = DEFAULT_NO_POLE_SOUND_ID,
            name = "No Pole - Don Toliver",
            fileUri = SOUND_RESOURCE_PATH + "no_pole"
        ),
        AlarmSound(
            stableId = DEFAULT_NOTIFICATION_STABLE_ID,
            alarmSoundId = DEFAULT_NOTIFICATION_SOUND_ID,
            name = "Notification Tone",
            fileUri = Settings.System.DEFAULT_NOTIFICATION_URI.toString()
        ),
        AlarmSound(
            stableId = DEFAULT_ALARM_STABLE_ID,
            alarmSoundId = DEFAULT_ALARM_SOUND_ID,
            name = DEFAULT_ALARM_SOUND_NAME,
            fileUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
        )
    )

suspend fun loadAllAlarmSounds(context: Context): List<AlarmSound> {
    val customSounds = AudioDatabase.getDatabase(context)
        .audioDao()
        .getAllSoundsOnce()
        .map { it.toAlarmSound() }

    return defaultAlarmSounds() + customSounds
}

suspend fun resolveAlarmSound(context: Context, stableId: String): AlarmSound? =
    loadAllAlarmSounds(context).firstOrNull { it.stableId == stableId }
