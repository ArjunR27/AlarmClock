package dev.csse.ayranade.alarmclock.ui.alarms

internal const val DEFAULT_SNOOZE_MINUTES = 5
internal const val MIN_SNOOZE_MINUTES = 1
internal const val MAX_SNOOZE_MINUTES = 120

internal fun normalizeSnoozeMinutes(snoozeMinutes: Int): Int =
    if (snoozeMinutes in MIN_SNOOZE_MINUTES..MAX_SNOOZE_MINUTES) {
        snoozeMinutes
    } else {
        DEFAULT_SNOOZE_MINUTES
    }

internal fun formatSnoozeMinutes(snoozeMinutes: Int): String =
    "${normalizeSnoozeMinutes(snoozeMinutes)} min"
