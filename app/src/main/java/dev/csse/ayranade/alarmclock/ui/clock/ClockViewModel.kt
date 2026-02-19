package dev.csse.ayranade.alarmclock.ui.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ClockUiState(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
)

class ClockViewModel() : ViewModel() {
    // StateFlow
        // Observable state holder (read-only)
        // When value changes it emits the new value to all collectors

    // MutableStateFlow
        // Mutable version used inside the class
        // read/write only used inside ViewModel itself
    private val _uiState = MutableStateFlow(ClockUiState())
    val clockUiState : StateFlow<ClockUiState> = _uiState.asStateFlow()

    init {
        startClock()
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                val calendar = Calendar.getInstance()
                _uiState.value = ClockUiState(
                    hour = calendar.get(Calendar.HOUR),
                    minute = calendar.get(Calendar.MINUTE),
                    second = calendar.get(Calendar.SECOND)
                )
                // update, wait 1 second, rerun
                delay(1000)
            }
        }
    }
}

