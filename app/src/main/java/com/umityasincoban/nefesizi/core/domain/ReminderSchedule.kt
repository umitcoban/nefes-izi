package com.umityasincoban.nefesizi.core.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

fun nextDailyReminderDelay(now: LocalDateTime, time: LocalTime): Duration {
    var next = now.toLocalDate().atTime(time)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next)
}

fun nextWeeklyReminderDelay(
    now: LocalDateTime,
    dayOfWeek: java.time.DayOfWeek,
    time: LocalTime,
): Duration {
    var next = now.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        .withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
    if (!next.isAfter(now)) next = next.plusWeeks(1)
    return Duration.between(now, next)
}
