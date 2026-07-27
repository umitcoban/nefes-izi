package com.umityasincoban.nefesizi.core.domain

import java.math.BigDecimal
import java.math.RoundingMode

fun calculatePricePerCigaretteMicros(
    packPriceMicros: Long?,
    cigarettesPerPack: Int?,
): Long? {
    if (packPriceMicros == null || cigarettesPerPack == null) return null
    require(packPriceMicros >= 0) { "Pack price cannot be negative." }
    require(cigarettesPerPack > 0) { "Cigarettes per pack must be positive." }
    return BigDecimal.valueOf(packPriceMicros)
        .divide(BigDecimal.valueOf(cigarettesPerPack.toLong()), 0, RoundingMode.HALF_UP)
        .longValueExact()
}
