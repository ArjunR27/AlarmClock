package dev.csse.ayranade.alarmclock.ui.audios

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmSoundEntity::class], version=1)
abstract class AudioDatabase: RoomDatabase() {
    abstract fun audioDao(): AudioDao

    companion object {
        @Volatile
        private var INSTANCE: AudioDatabase? = null

            fun getDatabase(context: Context): AudioDatabase {
                return INSTANCE ?: synchronized(this) {
                    Room.databaseBuilder(context, AudioDatabase::class.java, "sounds_database")
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }
    }
