package dev.csse.ayranade.alarmclock.ui.audios

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AlarmSound(
    val alarmSoundId: Int,
    val name: String,
    val fileUri: String,
    val isCustom: Boolean = false
)
data class SoundsUiState(
    val defaultSounds: List<AlarmSound> = emptyList(),
    val customSounds: List<AlarmSound> = emptyList(),
    val selectedSoundId: Int? = null
)

private var nextId = 1
private fun getNextId() = nextId++

class SoundsViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(SoundsUiState())
    val soundUiState : StateFlow<SoundsUiState> = _uiState.asStateFlow()

    init {
        loadDefaultSounds()
    }

    private fun loadDefaultSounds() {
        val defaults = listOf<AlarmSound>(
            AlarmSound(alarmSoundId = getNextId(), name = "Basic Sound 1", fileUri="basicSound1"),
            AlarmSound(alarmSoundId  = getNextId(), name = "Basic Sound 2", fileUri="basicSound2"),
            AlarmSound(alarmSoundId = getNextId(), name = "Basic Sound 3", fileUri="basicSound3")
        )
        _uiState.update {it.copy(defaultSounds = defaults)}
    }

    fun addCustomSound(name: String, uri: String) {
        val newSound = AlarmSound(
            alarmSoundId = getNextId(),
            name = name,
            fileUri = uri,
        )

        _uiState.update {it.copy(customSounds = _uiState.value.customSounds + newSound)}
    }

    // Users can delete a sound
    fun deleteCustomSound(id: Int) {
        // Not implemented
    }

    // Users can select a sound for an alarm
    fun selectSound(id: Int) {
        // Not implemented
    }
}