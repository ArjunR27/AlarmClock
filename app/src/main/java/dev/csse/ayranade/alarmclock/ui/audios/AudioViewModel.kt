package dev.csse.ayranade.alarmclock.ui.audios

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AlarmSound(
    val stableId: String,
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

@Entity(tableName = "sounds")
data class AlarmSoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val fileUri: String,
    val isCustom: Boolean
)

@Dao
interface AudioDao {
    @Query("SELECT * from sounds")
    fun getAllSounds(): Flow<List<AlarmSoundEntity>>

    @Insert
    suspend fun insert(sound: AlarmSoundEntity)

    @Delete
    suspend fun delete(sound: AlarmSoundEntity)

    @Query("DELETE FROM sounds WHERE id IN (:soundIds)")
    suspend fun deleteByIds(soundIds: List<Int>)
}

private const val DEFAULT_NO_POLE_SOUND_ID = 1
private const val DEFAULT_NOTIFICATION_SOUND_ID = 2
private const val DEFAULT_ALARM_SOUND_ID = 3

private const val DEFAULT_NO_POLE_STABLE_ID = "default:no_pole"
private const val DEFAULT_NOTIFICATION_STABLE_ID = "default:notification"
const val DEFAULT_ALARM_STABLE_ID = "default:alarm"
const val DEFAULT_ALARM_SOUND_NAME = "Default Alarm Sound"

private fun customStableSoundId(id: Int) = "custom:$id"

private fun AlarmSoundEntity.toAlarmSound(): AlarmSound =
    AlarmSound(
        stableId = customStableSoundId(id),
        alarmSoundId = id,
        name = name,
        fileUri = fileUri,
        isCustom = isCustom
    )

class AudioViewModel(private val repository: AudioRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SoundsUiState())
    val soundUiState : StateFlow<SoundsUiState> = _uiState.asStateFlow()

    val soundPath = "android.resource://dev.csse.ayranade.alarmclock/raw/"

    init {
        loadDefaultSounds()
        observeCustomSounds()
    }

    private fun loadDefaultSounds() {
        val defaults = listOf<AlarmSound>(
            AlarmSound(
                stableId = DEFAULT_NO_POLE_STABLE_ID,
                alarmSoundId = DEFAULT_NO_POLE_SOUND_ID,
                name = "No Pole - Don Toliver",
                fileUri = soundPath + "no_pole"
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
        _uiState.update {it.copy(defaultSounds = defaults)}
    }

    private fun observeCustomSounds() {
        viewModelScope.launch {
            repository.getAllSounds().collect { sounds ->
                _uiState.update { currentState ->
                    currentState.copy(customSounds = sounds.map { it.toAlarmSound() })
                }
            }
        }
    }

    fun addCustomSound(name: String, uri: String) {
        viewModelScope.launch {
            repository.insert(AlarmSoundEntity(name=name, fileUri = uri, isCustom = true))
        }
    }

    fun deleteCustomSound(sound: AlarmSoundEntity) {
        viewModelScope.launch {
            repository.delete(sound)
        }
    }

    fun deleteCustomSounds(soundIds: Set<Int>) {
        if (soundIds.isEmpty()) return

        viewModelScope.launch {
            repository.deleteByIds(soundIds.toList())
        }
    }

    // Users can select a sound for an alarm
    fun selectSound(id: Int) {
        // Not implemented
    }
}

class AudioViewModelFactory(private val repository: AudioRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
