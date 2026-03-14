package dev.csse.ayranade.alarmclock.ui.alarms

import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_STABLE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmStorageTest {
    @Test
    fun normalized_defaults_legacy_snooze_minutes() {
        val alarm = Alarm(
            alarmId = 1,
            hour = 7,
            minute = 30,
            soundId = null,
            alarmSoundId = 42,
            snoozeMinutes = 0
        )

        val normalized = alarm.normalized()

        assertEquals(DEFAULT_ALARM_STABLE_ID, normalized.soundId)
        assertNull(normalized.alarmSoundId)
        assertEquals(DEFAULT_SNOOZE_MINUTES, normalized.snoozeMinutes)
    }

    @Test
    fun normalized_preserves_valid_snooze_minutes() {
        val alarm = Alarm(
            alarmId = 2,
            hour = 6,
            minute = 45,
            soundId = DEFAULT_ALARM_STABLE_ID,
            snoozeMinutes = 15
        )

        val normalized = alarm.normalized()

        assertEquals(15, normalized.snoozeMinutes)
    }
}
