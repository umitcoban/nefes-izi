package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity

data class CostTotal(
    val micros: Long?,
    val currencyCode: String?,
    val knownCount: Int,
    val unknownCount: Int,
)

data class TodaySummary(
    val totalCount: Int,
    val firstRecordAtEpochMillis: Long?,
    val averageIntervalMillis: Long?,
    val cost: CostTotal,
)

fun calculateTodaySummary(records: List<SmokingRecordEntity>): TodaySummary {
    val chronological = records.sortedBy(SmokingRecordEntity::smokedAtEpochMillis)
    val intervals = chronological.zipWithNext { first, second ->
        (second.smokedAtEpochMillis - first.smokedAtEpochMillis).coerceAtLeast(0L)
    }
    var costMicros = 0L
    var knownCostCount = 0
    var unknownCostCount = 0
    val currencies = mutableSetOf<String>()
    records.forEach { record ->
        val price = record.priceMicrosPerCigaretteSnapshot
        if (price == null) {
            unknownCostCount += record.quantity
        } else {
            costMicros += price * record.quantity * record.consumedQuarter / 4
            knownCostCount += record.quantity
            currencies += record.currencyCodeSnapshot
        }
    }
    return TodaySummary(
        totalCount = records.sumOf(SmokingRecordEntity::quantity),
        firstRecordAtEpochMillis = chronological.firstOrNull()?.smokedAtEpochMillis,
        averageIntervalMillis = intervals.takeIf(List<Long>::isNotEmpty)?.average()?.toLong(),
        cost = CostTotal(
            micros = costMicros.takeIf { knownCostCount > 0 },
            currencyCode = currencies.singleOrNull(),
            knownCount = knownCostCount,
            unknownCount = unknownCostCount,
        ),
    )
}
