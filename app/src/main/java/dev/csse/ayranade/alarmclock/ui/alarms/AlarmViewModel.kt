package dev.csse.ayranade.alarmclock.ui.alarms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_STABLE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Alarm(
    val alarmId: Int,
    var hour: Int,
    var minute: Int,
    var second: Int = 0,
    val label: String = "",
    var isEnabled: Boolean = true,
    var soundId: String? = null,
    // Retained to migrate older saved alarms that used an ambiguous numeric ID.
    var alarmSoundId: Int? = null,
    var daysOfWeek: List<Int> = emptyList(),
    var am : Boolean = true,
) {
    val resolvedSoundId: String
        get() = soundId ?: DEFAULT_ALARM_STABLE_ID
}

data class AlarmUiState(
    val alarms: Map<Int, Alarm> = emptyMap()
)

class AlarmViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AlarmViewModel(context.applicationContext) as T
    }
}


class AlarmViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmUiState())
    val alarmUiState : StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AlarmStorage.loadAlarms(context).collect { alarms ->
                val normalizedAlarms = alarms.map { it.normalized() }

                if (normalizedAlarms.isEmpty()) {
                    loadDefaultAlarms()
                } else {
                    val alarmMap = normalizedAlarms.associateBy { alarm -> alarm.alarmId }
                    _uiState.update { it.copy(alarms = alarmMap) }

                    if (normalizedAlarms != alarms) {
                        AlarmStorage.saveAlarms(context, alarmMap)
                    }

                    AlarmScheduler.reconcileEnabledAlarms(context, alarmMap.values)
                }
            }
        }
    }

    private fun nextAlarmId(state: AlarmUiState): Int {
        return (state.alarms.keys.maxOrNull() ?: 0) + 1
    }

    private fun loadDefaultAlarms() {
        var nextAlarmId = 1
        val defaultAlarms = listOf(
            Alarm(
                alarmId = nextAlarmId++,
                hour = 6,
                minute = 30,
                label = "Weekday Run",
                soundId = DEFAULT_ALARM_STABLE_ID,
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                am = true
            ),
            Alarm(
                alarmId = nextAlarmId++,
                hour = 8,
                minute = 0,
                label = "Standup",
                soundId = DEFAULT_ALARM_STABLE_ID,
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                am = true,
            ),
            Alarm(
                alarmId = nextAlarmId++,
                hour = 10,
                minute = 15,
                label = "Weekend Chores",
                isEnabled = false,
                soundId = DEFAULT_ALARM_STABLE_ID,
                daysOfWeek = listOf(6),
                am = false
            )
        )

        val alarmMap = defaultAlarms.associateBy { alarm -> alarm.alarmId }
        _uiState.update { it.copy(alarms = alarmMap) }

        viewModelScope.launch {
            AlarmStorage.saveAlarms(context, alarmMap)
            AlarmScheduler.reconcileEnabledAlarms(context, alarmMap.values)
        }
    }

    fun addAlarm(
        hour: Int,
        minute: Int,
        label: String,
        soundId: String,
        daysOfWeek: List<Int>,
        am: Boolean,
        isEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val alarmId = nextAlarmId(state)
            val newAlarm = Alarm(
                alarmId = alarmId,
                hour = hour,
                minute = minute,
                label = label,
                soundId = soundId,
                daysOfWeek = daysOfWeek,
                am = am,
                isEnabled = isEnabled
            )
            val updatedAlarms = state.alarms + (alarmId to newAlarm)
            _uiState.value = state.copy(alarms = updatedAlarms)
            AlarmStorage.saveAlarms(context, updatedAlarms)

            if (newAlarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(context, newAlarm)
            } else {
                AlarmScheduler.cancelAlarm(context, newAlarm.alarmId)
            }
        }
    }

    fun setAlarmEnabled(alarmId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val updated = state.alarms[alarmId]?.copy(isEnabled = isEnabled) ?: return@launch
            val updatedAlarms = state.alarms + (alarmId to updated)
            _uiState.value = state.copy(alarms = updatedAlarms)
            AlarmStorage.saveAlarms(context, updatedAlarms)

            if (isEnabled) {
                AlarmScheduler.scheduleAlarm(context, updated)
            } else {
                AlarmScheduler.cancelAlarm(context, alarmId)
            }
        }
    }

    fun disableAlarm(alarmId: Int) {
        setAlarmEnabled(alarmId = alarmId, isEnabled = false)
    }

    fun deleteAlarms(alarmIds: Set<Int>) {
        if (alarmIds.isEmpty()) return

        viewModelScope.launch {
            val state = _uiState.value
            val updatedAlarms = state.alarms.filterKeys { it !in alarmIds }
            _uiState.value = state.copy(alarms = updatedAlarms)
            AlarmStorage.saveAlarms(context, updatedAlarms)
            alarmIds.forEach { alarmId ->
                AlarmScheduler.cancelAlarm(context, alarmId)
            }
        }
    }
}
