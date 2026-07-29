package com.umityasincoban.nefesizi.core.domain

import java.time.Instant
import java.time.ZoneId

data class LogicalDayRange(
    val date: java.time.LocalDate,
    val start: Instant,
    val endExclusive: Instant,
)

fun logicalDayRange(
    now: Instant,
    zoneId: ZoneId,
    dayStartHour: Int,
): LogicalDayRange {
    require(dayStartHour in 0..23)
    val zonedNow = now.atZone(zoneId)
    val logicalDate = zonedNow.minusHours(dayStartHour.toLong()).toLocalDate()
    return LogicalDayRange(
        date = logicalDate,
        start = logicalDate.atTime(dayStartHour, 0).atZone(zoneId).toInstant(),
        endExclusive = logicalDate.plusDays(1).atTime(dayStartHour, 0).atZone(zoneId).toInstant(),
    )
}
