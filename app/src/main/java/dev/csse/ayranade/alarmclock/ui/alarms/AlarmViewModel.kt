package dev.csse.ayranade.alarmclock.ui.alarms

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private var nextId = 1
private fun getNextId() = nextId++

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
    val alarms: Map<Int, Alarm> = HashMap<Int, Alarm>()
)
class AlarmViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmUiState())
    val alarmUiState : StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        loadDefaultAlarms()
    }

    private fun loadDefaultAlarms() {
        val defaultAlarms = listOf(
            Alarm(
                alarmId = getNextId(),
                hour = 6,
                minute = 30,
                label = "Weekday Run",
                alarmSoundId = 1,
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                am = true
            ),
            Alarm(
                alarmId = getNextId(),
                hour = 8,
                minute = 0,
                label = "Standup",
                alarmSoundId = 2,
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                am = true,
            ),
            Alarm(
                alarmId = getNextId(),
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
    }

    fun addAlarm(hour: Int, minute: Int, label: String, alarmSoundId: Int, daysOfWeek: List<Int>, am: Boolean) {
        val alarmId = getNextId()
        val newAlarm = Alarm(
            alarmId = alarmId,
            hour = hour,
            minute = minute,
            label = label,
            alarmSoundId = alarmSoundId,
            daysOfWeek = daysOfWeek,
            am = am
        )
        _uiState.update {it.copy(alarms = (it.alarms + (alarmId to newAlarm)))}
    }

    fun setAlarmEnabled(alarmId: Int, isEnabled: Boolean) {
        _uiState.update { state ->
            val updatedAlarm = state.alarms[alarmId]?.copy(isEnabled = isEnabled) ?: return@update state
            state.copy(alarms = state.alarms + (alarmId to updatedAlarm))
        }
    }

    fun disableAlarm(alarmId: Int) {
        setAlarmEnabled(alarmId = alarmId, isEnabled = false)
    }
}
