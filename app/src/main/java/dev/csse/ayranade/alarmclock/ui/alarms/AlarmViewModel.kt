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
    var daysOfWeek: List<Int> = emptyList()
)

data class AlarmUiState(
    val alarms: Map<Int, Alarm> = HashMap<Int, Alarm>()
)
class AlarmViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmUiState())
    val alarmUiState : StateFlow<AlarmUiState> = _uiState.asStateFlow()

    fun addAlarm(hour: Int, minute: Int, label: String, alarmSoundId: Int, daysOfWeek: List<Int>) {
        val alarmId = getNextId()
        val newAlarm = Alarm(
            alarmId = alarmId,
            hour = hour,
            minute = minute,
            label = label,
            alarmSoundId = alarmSoundId,
            daysOfWeek = daysOfWeek
        )
        _uiState.update {it.copy(alarms = (it.alarms + (alarmId to newAlarm)))}
    }

    fun disableAlarm(alarmId: Int) {
        _uiState.update {state ->
            val updatedAlarm = state.alarms[alarmId]?.copy(isEnabled = false) ?: return@update state
            state.copy(alarms = state.alarms + (alarmId to updatedAlarm))
        }
    }
}
