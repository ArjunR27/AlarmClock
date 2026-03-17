package dev.csse.ayranade.alarmclock.ui.alarms

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSnoozeTest {
    @Test
    fun normalize_snooze_minutes_accepts_custom_values_within_range() {
        assertEquals(47, normalizeSnoozeMinutes(47))
        assertEquals(MIN_SNOOZE_MINUTES, normalizeSnoozeMinutes(MIN_SNOOZE_MINUTES))
        assertEquals(MAX_SNOOZE_MINUTES, normalizeSnoozeMinutes(MAX_SNOOZE_MINUTES))
    }

    @Test
    fun normalize_snooze_minutes_defaults_invalid_values() {
        assertEquals(DEFAULT_SNOOZE_MINUTES, normalizeSnoozeMinutes(0))
        assertEquals(DEFAULT_SNOOZE_MINUTES, normalizeSnoozeMinutes(MAX_SNOOZE_MINUTES + 1))
    }
}
