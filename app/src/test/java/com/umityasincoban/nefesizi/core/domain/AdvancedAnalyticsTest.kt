package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedAnalyticsTest {
    private val end = LocalDate.of(2026, 7, 29)

    @Test
    fun `empty fixed period contains zero days without fabricated totals`() {
        val result = calculateAdvancedAnalytics(
            allRecords = emptyList(),
            requestedStart = end.minusDays(6),
            requestedEnd = end,
        )

        assertEquals(7, result.dayCount)
        assertEquals(7, result.daily.size)
        assertEquals(0, result.totalCount)
        assertEquals(0.0, result.dailyAverage, 0.0)
        assertNull(result.averageIntervalMinutes)
        assertNull(result.longestGapMinutes)
        assertTrue(result.costs.isEmpty())
        assertNull(result.nicotine.micrograms)
    }

    @Test
    fun `daily totals use quantity and event timezone snapshot`() {
        val istanbul = ZoneId.of("Europe/Istanbul")
        val instant = LocalDateTime.of(2026, 7, 28, 22, 30)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val record = record(
            id = "travel",
            at = instant,
            zoneId = istanbul.id,
            quantity = 3,
        )

        val result = calculateAdvancedAnalytics(listOf(record), end, end)

        assertEquals(3, result.totalCount)
        assertEquals(3, result.daily.single().count)
        assertEquals(3, result.hourlyCounts[1])
        assertEquals(3, result.weekdayCounts[end.dayOfWeek])
    }

    @Test
    fun `highest lowest average interval and longest gap are deterministic`() {
        val records = listOf(
            record("one", date = end.minusDays(2), hour = 8),
            record("two", date = end.minusDays(1), hour = 8, quantity = 3),
            record("three", date = end, hour = 8, quantity = 2),
            record("four", date = end, hour = 10, quantity = 2),
        )

        val result = calculateAdvancedAnalytics(records, end.minusDays(2), end)

        assertEquals(end, result.highestDay?.date)
        assertEquals(4, result.highestDay?.count)
        assertEquals(end.minusDays(2), result.lowestDay?.date)
        assertEquals(1, result.lowestDay?.count)
        assertEquals(1_440L, result.longestGapMinutes)
        assertEquals(1_000L, result.averageIntervalMinutes)
    }

    @Test
    fun `cost groups currencies applies consumed ratio and reports unknown coverage`() {
        val records = listOf(
            record("try", price = 5_000_000L, quantity = 2),
            record("eur", price = 4_000_000L, currency = "EUR", quarter = 2),
            record("unknown", price = null, quantity = 3),
        )

        val result = calculateAdvancedAnalytics(records, end.minusDays(6), end)

        assertEquals(
            listOf(
                CurrencyAmount("EUR", 2_000_000L, 1),
                CurrencyAmount("TRY", 10_000_000L, 2),
            ),
            result.costs,
        )
        assertEquals(3, result.unknownCostCount)
    }

    @Test
    fun `previous comparison uses immediately preceding equal length period`() {
        val records = buildList {
            repeat(7) { offset ->
                add(record("previous-$offset", date = end.minusDays(13L - offset)))
                add(record("current-$offset", date = end.minusDays(6L - offset), quantity = 2))
            }
        }

        val result = calculateAdvancedAnalytics(records, end.minusDays(6), end)

        assertEquals(14, result.comparison?.currentCount)
        assertEquals(7, result.comparison?.previousCount)
        assertEquals(100.0, result.comparison?.changePercent ?: 0.0, 0.001)
        assertTrue(result.insights.any { it.type == AnalyticsInsightType.PERIOD_CHANGE })
    }

    @Test
    fun `ranked context uses cigarette quantity and exposes missing denominator`() {
        val records = listOf(
            record("coffee-one", trigger = "Kahve", mood = "Sakin", quantity = 3),
            record("coffee-two", trigger = "Kahve", mood = "Yorgun", quantity = 2),
            record("stress", trigger = "Stres", mood = "Sakin"),
            record("missing", trigger = null, mood = null, quantity = 4),
        )

        val result = calculateAdvancedAnalytics(records, end.minusDays(6), end)

        assertEquals("Kahve", result.mostCommonTrigger?.label)
        assertEquals(5, result.mostCommonTrigger?.count)
        assertEquals(6, result.mostCommonTrigger?.knownCount)
        assertEquals(4, result.mostCommonTrigger?.unknownCount)
        val insight = result.insights.first { it.type == AnalyticsInsightType.COMMON_TRIGGER }
        assertEquals(5, insight.numerator)
        assertEquals(6, insight.denominator)
    }

    @Test
    fun `exposure coverage does not treat missing values as zero`() {
        val records = listOf(
            record("known", nicotine = 800L, quantity = 2),
            record("missing", nicotine = null, quantity = 3),
        )

        val result = calculateAdvancedAnalytics(records, end.minusDays(6), end)

        assertEquals(1_600L, result.nicotine.micrograms)
        assertEquals(2, result.nicotine.knownCount)
        assertEquals(3, result.nicotine.unknownCount)
        assertTrue(result.insights.any { it.type == AnalyticsInsightType.DATA_COVERAGE })
    }

    @Test
    fun `annual projection requires seven distinct recent recorded days`() {
        val sixDays = (0L..5L).map { offset ->
            record("six-$offset", date = end.minusDays(offset), price = 1_000_000L)
        }
        val sevenDays = sixDays + record("seventh", date = end.minusDays(6), price = 1_000_000L)

        val ineligible = calculateAdvancedAnalytics(sixDays, end.minusDays(29), end)
        val eligible = calculateAdvancedAnalytics(sevenDays, end.minusDays(29), end)

        assertFalse(ineligible.annualProjectionEligible)
        assertTrue(ineligible.annualCostProjection.isEmpty())
        assertTrue(eligible.annualProjectionEligible)
        assertEquals(85_166_666L, eligible.annualCostProjection.single().micros)
    }

    @Test
    fun `trend insight is withheld for periods shorter than seven days`() {
        val records = listOf(
            record("previous", date = end.minusDays(2)),
            record("current", date = end, quantity = 2),
        )

        val result = calculateAdvancedAnalytics(records, end.minusDays(1), end)

        assertFalse(result.insights.any { it.type == AnalyticsInsightType.PERIOD_CHANGE })
    }

    @Test
    fun `optional wake time calculates average delay to first record and ignores pre-wake entries`() {
        val records = listOf(
            record("day-one", date = end.minusDays(2), hour = 8),
            record("day-one-later", date = end.minusDays(2), hour = 10),
            record("day-two", date = end.minusDays(1), hour = 9).copy(
                smokedAtEpochMillis = end.minusDays(1).atTime(9, 30)
                    .atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
            ),
            record("before-wake", date = end, hour = 6),
        )

        val result = calculateAdvancedAnalytics(
            allRecords = records,
            requestedStart = end.minusDays(2),
            requestedEnd = end,
            wakeTime = LocalTime.of(7, 0),
        )

        assertEquals(105L, result.averageFirstRecordAfterWakeMinutes)
    }

    private fun record(
        id: String,
        date: LocalDate = end,
        hour: Int = 12,
        at: Long = date.atTime(hour, 0).atZone(ZoneOffset.UTC).toInstant().toEpochMilli(),
        zoneId: String = "UTC",
        quantity: Int = 1,
        quarter: Int = 4,
        price: Long? = 5_000_000L,
        currency: String = "TRY",
        nicotine: Long? = 700L,
        trigger: String? = "Kahve",
        mood: String? = "Sakin",
    ) = SmokingRecordEntity(
        id = id,
        smokedAtEpochMillis = at,
        zoneIdSnapshot = zoneId,
        quantity = quantity,
        consumedQuarter = quarter,
        productId = "product",
        productRevisionIdSnapshot = "revision",
        productNameSnapshot = "Test",
        nicotineMicrogramsPerCigaretteSnapshot = nicotine,
        tarMicrogramsPerCigaretteSnapshot = 8_000L,
        carbonMonoxideMicrogramsPerCigaretteSnapshot = 9_000L,
        priceMicrosPerCigaretteSnapshot = price,
        currencyCodeSnapshot = currency,
        valueSourceSnapshot = "USER_ENTERED",
        cravingLevel = null,
        trigger = trigger,
        mood = mood,
        locationType = null,
        note = null,
        createdAtEpochMillis = at,
        updatedAtEpochMillis = at,
    )
}
