package dev.csse.ayranade.alarmclock.ui.alarms

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_STABLE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "alarms")

object AlarmStorage {
    private val ALARMS_KEY = stringPreferencesKey("alarms")
    private val gson = Gson()

    suspend fun saveAlarms(context: Context, alarms: Map<Int, Alarm>) {
        context.dataStore.edit { prefs ->
            prefs[ALARMS_KEY] = gson.toJson(alarms.values.toList())
        }
    }

    fun loadAlarms(context: Context): Flow<List<Alarm>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[ALARMS_KEY] ?: return@map emptyList()
            gson.fromJson(json, Array<Alarm>::class.java).toList()
        }
    }

    suspend fun loadAlarmsOnce(context: Context): List<Alarm> = loadAlarms(context).first()

    suspend fun loadAlarmMap(context: Context): Map<Int, Alarm> =
        loadAlarmsOnce(context)
            .map { it.normalized() }
            .associateBy { it.alarmId }
}

internal fun Alarm.normalized(): Alarm {
    val normalizedSoundId = soundId ?: DEFAULT_ALARM_STABLE_ID
    val normalizedSnoozeMinutes = normalizeSnoozeMinutes(snoozeMinutes)
    return if (
        normalizedSoundId == soundId &&
        alarmSoundId == null &&
        normalizedSnoozeMinutes == snoozeMinutes
    ) {
        this
    } else {
        copy(
            soundId = normalizedSoundId,
            alarmSoundId = null,
            snoozeMinutes = normalizedSnoozeMinutes
        )
    }
}
