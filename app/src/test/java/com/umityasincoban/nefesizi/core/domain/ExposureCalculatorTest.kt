package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExposureCalculatorTest {
    @Test
    fun `known values use quantity and consumed ratio`() {
        val records = listOf(
            record(quantity = 2, consumedQuarter = 4, nicotineMicrograms = 800),
            record(quantity = 1, consumedQuarter = 2, nicotineMicrograms = 1_000),
        )

        val result = calculateExposure(records) { it.nicotineMicrogramsPerCigaretteSnapshot }

        assertEquals(2_100L, result.micrograms)
        assertEquals(3, result.knownCount)
        assertEquals(0, result.unknownCount)
    }

    @Test
    fun `unknown values are counted instead of silently treated as zero`() {
        val records = listOf(
            record(quantity = 2, nicotineMicrograms = null),
            record(quantity = 1, nicotineMicrograms = 700),
        )

        val result = calculateExposure(records) { it.nicotineMicrogramsPerCigaretteSnapshot }

        assertEquals(700L, result.micrograms)
        assertEquals(1, result.knownCount)
        assertEquals(2, result.unknownCount)
    }

    @Test
    fun `all unknown values produce no total`() {
        val result = calculateExposure(
            listOf(record(quantity = 3, nicotineMicrograms = null)),
        ) { it.nicotineMicrogramsPerCigaretteSnapshot }

        assertNull(result.micrograms)
        assertEquals(0, result.knownCount)
        assertEquals(3, result.unknownCount)
    }

    private fun record(
        quantity: Int,
        consumedQuarter: Int = 4,
        nicotineMicrograms: Long?,
    ) = SmokingRecordEntity(
        id = "record-$quantity-$consumedQuarter-$nicotineMicrograms",
        smokedAtEpochMillis = 0,
        zoneIdSnapshot = "UTC",
        quantity = quantity,
        consumedQuarter = consumedQuarter,
        productId = null,
        productNameSnapshot = "Test",
        nicotineMicrogramsPerCigaretteSnapshot = nicotineMicrograms,
        tarMicrogramsPerCigaretteSnapshot = null,
        carbonMonoxideMicrogramsPerCigaretteSnapshot = null,
        priceMicrosPerCigaretteSnapshot = null,
        currencyCodeSnapshot = "TRY",
        cravingLevel = null,
        trigger = null,
        mood = null,
        locationType = null,
        note = null,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
