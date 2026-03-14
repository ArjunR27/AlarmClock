package dev.csse.ayranade.alarmclock.ui.alarms

internal const val DEFAULT_SNOOZE_MINUTES = 5
internal val SNOOZE_MINUTE_OPTIONS = listOf(5, 10, 15, 20, 30)

internal fun normalizeSnoozeMinutes(snoozeMinutes: Int): Int =
    if (snoozeMinutes in SNOOZE_MINUTE_OPTIONS) {
        snoozeMinutes
    } else {
        DEFAULT_SNOOZE_MINUTES
    }

internal fun formatSnoozeMinutes(snoozeMinutes: Int): String =
    "${normalizeSnoozeMinutes(snoozeMinutes)} min"
