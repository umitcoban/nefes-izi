package com.umityasincoban.nefesizi.feature.records

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun filterRecords(
    records: List<SmokingRecordEntity>,
    search: String,
    filters: RecordFilters,
    today: LocalDate,
    zoneId: ZoneId,
): List<SmokingRecordEntity> = records.filter { record ->
    record.matchesSearch(search) && record.matchesFilters(filters, today, zoneId)
}

private fun SmokingRecordEntity.matchesSearch(search: String): Boolean =
    search.isBlank() ||
        productNameSnapshot.contains(search, ignoreCase = true) ||
        note.orEmpty().contains(search, ignoreCase = true) ||
        trigger.orEmpty().contains(search, ignoreCase = true) ||
        mood.orEmpty().contains(search, ignoreCase = true)

private fun SmokingRecordEntity.matchesFilters(
    filters: RecordFilters,
    today: LocalDate,
    zoneId: ZoneId,
): Boolean {
    val startDate = when (filters.period) {
        RecordPeriod.ALL -> null
        RecordPeriod.LAST_7_DAYS -> today.minusDays(6)
        RecordPeriod.LAST_30_DAYS -> today.minusDays(29)
        RecordPeriod.CUSTOM -> filters.startDate.toLocalDateOrNull()
    }
    val endDate = if (filters.period == RecordPeriod.CUSTOM) {
        filters.endDate.toLocalDateOrNull()
    } else {
        null
    }
    val recordDate = Instant.ofEpochMilli(smokedAtEpochMillis).atZone(zoneId).toLocalDate()
    return (startDate == null || !recordDate.isBefore(startDate)) &&
        (endDate == null || !recordDate.isAfter(endDate)) &&
        (filters.productId == null || productId == filters.productId) &&
        (filters.trigger == null || trigger == filters.trigger) &&
        (filters.mood == null || mood == filters.mood) &&
        (!filters.notesOnly || !note.isNullOrBlank()) &&
        (!filters.unknownOnly ||
            productId == null ||
            priceMicrosPerCigaretteSnapshot == null ||
            nicotineMicrogramsPerCigaretteSnapshot == null ||
            tarMicrogramsPerCigaretteSnapshot == null ||
            carbonMonoxideMicrogramsPerCigaretteSnapshot == null)
}

private fun String.toLocalDateOrNull(): LocalDate? =
    takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
