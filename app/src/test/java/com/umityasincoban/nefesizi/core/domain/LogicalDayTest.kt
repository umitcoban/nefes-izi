package com.umityasincoban.nefesizi.core.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LogicalDayTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")

    @Test
    fun `time before configured start belongs to previous logical day`() {
        val now = Instant.parse("2026-07-29T02:00:00Z") // İstanbul 05:00

        val range = logicalDayRange(now, istanbul, 6)

        assertEquals(LocalDate.of(2026, 7, 28), range.date)
        assertEquals(Instant.parse("2026-07-28T03:00:00Z"), range.start)
        assertEquals(Instant.parse("2026-07-29T03:00:00Z"), range.endExclusive)
    }

    @Test
    fun `midnight start uses local calendar day`() {
        val now = Instant.parse("2026-07-28T22:00:00Z") // İstanbul 01:00

        val range = logicalDayRange(now, istanbul, 0)

        assertEquals(LocalDate.of(2026, 7, 29), range.date)
    }

    @Test
    fun `DST day uses local next boundary instead of fixed twenty four hours`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val now = Instant.parse("2026-03-29T10:00:00Z")

        val range = logicalDayRange(now, berlin, 0)

        assertEquals(23, Duration.between(range.start, range.endExclusive).toHours())
    }
}
