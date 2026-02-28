package dev.csse.ayranade.alarmclock

import android.app.Application
import dev.csse.ayranade.alarmclock.ui.audios.AudioDatabase
import dev.csse.ayranade.alarmclock.ui.audios.AudioRepository

class AlarmClockApplication : Application() {
    val database by lazy { AudioDatabase.getDatabase(this) }
    val audioRepository by lazy { AudioRepository(database.audioDao()) }
}