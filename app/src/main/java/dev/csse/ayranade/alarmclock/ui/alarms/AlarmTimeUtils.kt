package dev.csse.ayranade.alarmclock.ui.alarms

import java.time.ZonedDateTime
import kotlin.math.ceil

internal data class UpcomingAlarm(
    val alarm: Alarm,
    val triggerAt: ZonedDateTime
)

internal fun Alarm.nextTriggerAt(now: ZonedDateTime): ZonedDateTime? {
    val targetHour = to24HourValue()

    if (daysOfWeek.isEmpty()) {
        var candidate = now
            .withHour(targetHour)
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }

        return candidate
    }

    val selectedDays = daysOfWeek.toSet()
    for (dayOffset in 0..7) {
        val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
        if (candidateDate.dayOfWeek.value !in selectedDays) {
            continue
        }

        val candidate = candidateDate
            .atTime(targetHour, minute.coerceIn(0, 59))
            .atZone(now.zone)

        if (candidate.isAfter(now)) {
            return candidate
        }
    }

    return null
}

internal fun Alarm.nextTriggerAtMillis(now: ZonedDateTime): Long? =
    nextTriggerAt(now)?.toInstant()?.toEpochMilli()

internal fun nextEnabledAlarm(
    alarms: Collection<Alarm>,
    now: ZonedDateTime
): UpcomingAlarm? =
    alarms
        .asSequence()
        .filter { it.isEnabled }
        .mapNotNull { alarm ->
            alarm.nextTriggerAt(now)?.let { triggerAt ->
                UpcomingAlarm(alarm = alarm, triggerAt = triggerAt)
            }
        }
        .minByOrNull { it.triggerAt.toInstant().toEpochMilli() }

internal fun formatNextAlarmCountdown(
    triggerAt: ZonedDateTime,
    now: ZonedDateTime
): String {
    val remainingMillis =
        (triggerAt.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()).coerceAtLeast(0L)
    val totalMinutes =
        if (remainingMillis == 0L) 0L else ceil(remainingMillis / 60_000.0).toLong()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "Next alarm in ${hours}h ${minutes}m"
}

internal fun Alarm.to24HourValue(): Int {
    val normalizedHour = normalizedDisplayHour(hour)
    return when {
        am && normalizedHour == 12 -> 0
        !am && normalizedHour == 12 -> 12
        am -> normalizedHour
        else -> normalizedHour + 12
    }
}

internal fun from24HourValue(hour24: Int): Pair<Int, Boolean> {
    val normalizedHour = ((hour24 % 24) + 24) % 24
    val isAm = normalizedHour < 12
    val displayHour = when (val value = normalizedHour % 12) {
        0 -> 12
        else -> value
    }
    return displayHour to isAm
}

internal fun normalizedDisplayHour(hour: Int): Int =
    when {
        hour <= 0 -> 12
        hour > 12 && hour % 12 == 0 -> 12
        hour > 12 -> hour % 12
        else -> hour
    }
