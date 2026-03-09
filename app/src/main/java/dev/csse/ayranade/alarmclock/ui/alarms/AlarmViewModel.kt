package dev.csse.ayranade.alarmclock.ui.alarms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    var alarmSoundId: Int? = null,
    var daysOfWeek: List<Int> = emptyList(),
    var am : Boolean = true,
)

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
                if (alarms.isEmpty()) loadDefaultAlarms()
                else _uiState.update { it.copy(alarms = alarms.associateBy { a -> a.alarmId })}
            }
        }
    }

    private fun save() {
        viewModelScope.launch { AlarmStorage.saveAlarms(context, _uiState.value.alarms) }
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
                alarmSoundId = 1,
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                am = true
            ),
            Alarm(
                alarmId = nextAlarmId++,
                hour = 8,
                minute = 0,
                label = "Standup",
                alarmSoundId = 2,
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                am = true,
            ),
            Alarm(
                alarmId = nextAlarmId++,
                hour = 10,
                minute = 15,
                label = "Weekend Chores",
                isEnabled = false,
                alarmSoundId = 3,
                daysOfWeek = listOf(6),
                am = false
            )
        )

        _uiState.update { it.copy(alarms = defaultAlarms.associateBy { alarm -> alarm.alarmId }) }
        save()
    }

    fun addAlarm(hour: Int, minute: Int, label: String, alarmSoundId: Int, daysOfWeek: List<Int>, am: Boolean) {
        _uiState.update { state ->
            val alarmId = nextAlarmId(state)
            state.copy(
                alarms = state.alarms + (
                    alarmId to Alarm(
                        alarmId = alarmId,
                        hour = hour,
                        minute = minute,
                        label = label,
                        alarmSoundId = alarmSoundId,
                        daysOfWeek = daysOfWeek,
                        am = am
                    )
                )
            )
        }
        save()
    }

    fun setAlarmEnabled(alarmId: Int, isEnabled: Boolean) {
        _uiState.update { state ->
            val updated = state.alarms[alarmId]?.copy(isEnabled = isEnabled) ?: return@update state
            state.copy(alarms = state.alarms + (alarmId to updated))
        }
        save()
    }

    fun disableAlarm(alarmId: Int) {
        setAlarmEnabled(alarmId = alarmId, isEnabled = false)
    }
}
