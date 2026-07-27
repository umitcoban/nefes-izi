package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity

data class ExposureTotal(
    val micrograms: Long?,
    val knownCount: Int,
    val unknownCount: Int,
)

fun calculateExposure(
    records: List<SmokingRecordEntity>,
    valuePerCigarette: (SmokingRecordEntity) -> Long?,
): ExposureTotal {
    var total = 0L
    var known = 0
    var unknown = 0
    records.forEach { record ->
        val value = valuePerCigarette(record)
        if (value == null) {
            unknown += record.quantity
        } else {
            total += value * record.quantity * record.consumedQuarter / 4
            known += record.quantity
        }
    }
    return ExposureTotal(
        micrograms = total.takeIf { known > 0 },
        knownCount = known,
        unknownCount = unknown,
    )
}
