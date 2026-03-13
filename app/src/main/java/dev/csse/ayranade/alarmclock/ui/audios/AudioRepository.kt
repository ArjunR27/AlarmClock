package dev.csse.ayranade.alarmclock.ui.audios

import kotlinx.coroutines.flow.Flow

class AudioRepository(private val dao: AudioDao) {
    fun getAllSounds(): Flow<List<AlarmSoundEntity>> = dao.getAllSounds()
    suspend fun getAllSoundsOnce(): List<AlarmSoundEntity> = dao.getAllSoundsOnce()
    suspend fun insert(sound: AlarmSoundEntity) = dao.insert(sound)
    suspend fun delete(sound: AlarmSoundEntity) = dao.delete(sound)
    suspend fun deleteByIds(soundIds: List<Int>) = dao.deleteByIds(soundIds)
}
