package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodaySummaryCalculatorTest {
    @Test
    fun `empty day has no time or cost metrics`() {
        val result = calculateTodaySummary(emptyList())

        assertEquals(0, result.totalCount)
        assertNull(result.firstRecordAtEpochMillis)
        assertNull(result.averageIntervalMillis)
        assertNull(result.cost.micros)
        assertEquals(0, result.cost.knownCount)
        assertEquals(0, result.cost.unknownCount)
    }

    @Test
    fun `records are sorted before first time and average interval calculation`() {
        val result = calculateTodaySummary(
            listOf(
                record(id = "third", at = 10_000L),
                record(id = "first", at = 1_000L),
                record(id = "second", at = 4_000L),
            ),
        )

        assertEquals(1_000L, result.firstRecordAtEpochMillis)
        assertEquals(4_500L, result.averageIntervalMillis)
    }

    @Test
    fun `single record has first time but no average interval`() {
        val result = calculateTodaySummary(listOf(record(at = 2_000L)))

        assertEquals(2_000L, result.firstRecordAtEpochMillis)
        assertNull(result.averageIntervalMillis)
    }

    @Test
    fun `cost uses quantity and consumed quarter`() {
        val result = calculateTodaySummary(
            listOf(
                record(id = "full", priceMicros = 5_000_000L, quantity = 2),
                record(
                    id = "half",
                    priceMicros = 4_000_000L,
                    quantity = 1,
                    consumedQuarter = 2,
                ),
            ),
        )

        assertEquals(12_000_000L, result.cost.micros)
        assertEquals("TRY", result.cost.currencyCode)
        assertEquals(3, result.cost.knownCount)
        assertEquals(0, result.cost.unknownCount)
    }

    @Test
    fun `unknown prices remain visible in cost coverage`() {
        val result = calculateTodaySummary(
            listOf(
                record(id = "known", priceMicros = 5_000_000L, quantity = 1),
                record(id = "unknown", priceMicros = null, quantity = 3),
            ),
        )

        assertEquals(5_000_000L, result.cost.micros)
        assertEquals(1, result.cost.knownCount)
        assertEquals(3, result.cost.unknownCount)
    }

    @Test
    fun `all unknown prices do not silently produce zero cost`() {
        val result = calculateTodaySummary(
            listOf(record(priceMicros = null, quantity = 2)),
        )

        assertNull(result.cost.micros)
        assertNull(result.cost.currencyCode)
        assertEquals(0, result.cost.knownCount)
        assertEquals(2, result.cost.unknownCount)
    }

    @Test
    fun `mixed currencies are not represented as one currency`() {
        val result = calculateTodaySummary(
            listOf(
                record(id = "try", priceMicros = 5_000_000L, currency = "TRY"),
                record(id = "eur", priceMicros = 2_000_000L, currency = "EUR"),
            ),
        )

        assertEquals(7_000_000L, result.cost.micros)
        assertNull(result.cost.currencyCode)
    }

    private fun record(
        id: String = "record",
        at: Long = 0L,
        quantity: Int = 1,
        consumedQuarter: Int = 4,
        priceMicros: Long? = null,
        currency: String = "TRY",
    ) = SmokingRecordEntity(
        id = id,
        smokedAtEpochMillis = at,
        zoneIdSnapshot = "UTC",
        quantity = quantity,
        consumedQuarter = consumedQuarter,
        productId = "product",
        productRevisionIdSnapshot = "revision",
        productNameSnapshot = "Test",
        nicotineMicrogramsPerCigaretteSnapshot = 700L,
        tarMicrogramsPerCigaretteSnapshot = 8_000L,
        carbonMonoxideMicrogramsPerCigaretteSnapshot = 9_000L,
        priceMicrosPerCigaretteSnapshot = priceMicros,
        currencyCodeSnapshot = currency,
        valueSourceSnapshot = "USER_ENTERED",
        cravingLevel = null,
        trigger = null,
        mood = null,
        locationType = null,
        note = null,
        createdAtEpochMillis = at,
        updatedAtEpochMillis = at,
    )
}
