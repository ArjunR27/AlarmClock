package dev.csse.ayranade.alarmclock.ui.alarms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeUtilsTest {

    private val zoneId: ZoneId = ZoneId.of("America/Los_Angeles")

    @Test
    fun oneTimeAlarmUsesLaterTodayWhenStillAhead() {
        val now = ZonedDateTime.of(2026, 3, 16, 8, 15, 0, 0, zoneId)
        val alarm = Alarm(
            alarmId = 1,
            hour = 9,
            minute = 45,
            am = true
        )

        val trigger = alarm.nextTriggerAt(now)

        assertNotNull(trigger)
        assertEquals(ZonedDateTime.of(2026, 3, 16, 9, 45, 0, 0, zoneId), trigger)
    }

    @Test
    fun oneTimeAlarmRollsToTomorrowWhenTimePassed() {
        val now = ZonedDateTime.of(2026, 3, 16, 22, 15, 0, 0, zoneId)
        val alarm = Alarm(
            alarmId = 1,
            hour = 9,
            minute = 45,
            am = true
        )

        val trigger = alarm.nextTriggerAt(now)

        assertNotNull(trigger)
        assertEquals(ZonedDateTime.of(2026, 3, 17, 9, 45, 0, 0, zoneId), trigger)
    }

    @Test
    fun repeatingAlarmSkipsPastEarlierToday() {
        val now = ZonedDateTime.of(2026, 3, 16, 10, 5, 0, 0, zoneId)
        val alarm = Alarm(
            alarmId = 2,
            hour = 9,
            minute = 0,
            daysOfWeek = listOf(1, 3, 5),
            am = true
        )

        val trigger = alarm.nextTriggerAt(now)

        assertNotNull(trigger)
        assertEquals(ZonedDateTime.of(2026, 3, 18, 9, 0, 0, 0, zoneId), trigger)
    }

    @Test
    fun nextEnabledAlarmReturnsNullWhenNothingEnabled() {
        val now = ZonedDateTime.of(2026, 3, 16, 8, 0, 0, 0, zoneId)
        val alarms = listOf(
            Alarm(alarmId = 1, hour = 7, minute = 30, isEnabled = false, am = true),
            Alarm(alarmId = 2, hour = 8, minute = 45, isEnabled = false, am = true)
        )

        val upcomingAlarm = nextEnabledAlarm(alarms, now)

        assertNull(upcomingAlarm)
    }

    @Test
    fun countdownRoundsUpPartialMinutes() {
        val now = ZonedDateTime.of(2026, 3, 16, 8, 0, 30, 0, zoneId)
        val trigger = ZonedDateTime.of(2026, 3, 16, 9, 1, 0, 0, zoneId)

        assertEquals("Next alarm in 1h 1m", formatNextAlarmCountdown(trigger, now))
    }
}
