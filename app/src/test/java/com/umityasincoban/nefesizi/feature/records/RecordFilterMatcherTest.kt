package com.umityasincoban.nefesizi.feature.records

import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordFilterMatcherTest {
    private val today = LocalDate.of(2026, 7, 29)

    @Test
    fun `search checks product note trigger and mood case insensitively`() {
        val records = listOf(
            record("product", productName = "Gece Ürünü"),
            record("note", note = "Sabah balkonda"),
            record("trigger", trigger = "Kahve"),
            record("mood", mood = "Gergin"),
        )

        assertIds(records, "GECE", "product")
        assertIds(records, "balkon", "note")
        assertIds(records, "kahve", "trigger")
        assertIds(records, "gergin", "mood")
    }

    @Test
    fun `last seven days includes today and sixth previous day`() {
        val records = listOf(
            record("today", date = today),
            record("boundary", date = today.minusDays(6)),
            record("old", date = today.minusDays(7)),
        )

        assertEquals(
            listOf("today", "boundary"),
            filtered(records, RecordFilters(period = RecordPeriod.LAST_7_DAYS)).map { it.id },
        )
    }

    @Test
    fun `custom range includes both boundaries`() {
        val records = listOf(
            record("before", date = LocalDate.of(2026, 7, 9)),
            record("start", date = LocalDate.of(2026, 7, 10)),
            record("end", date = LocalDate.of(2026, 7, 20)),
            record("after", date = LocalDate.of(2026, 7, 21)),
        )

        assertEquals(
            listOf("start", "end"),
            filtered(
                records,
                RecordFilters(
                    period = RecordPeriod.CUSTOM,
                    startDate = "2026-07-10",
                    endDate = "2026-07-20",
                ),
            ).map { it.id },
        )
    }

    @Test
    fun `product trigger mood and notes filters combine with AND`() {
        val matching = record(
            id = "matching",
            productId = "p1",
            trigger = "Kahve",
            mood = "Sakin",
            note = "Not",
        )
        val records = listOf(
            matching,
            matching.copy(id = "wrong-product", productId = "p2"),
            matching.copy(id = "wrong-trigger", trigger = "Stres"),
            matching.copy(id = "wrong-mood", mood = "Yorgun"),
            matching.copy(id = "no-note", note = null),
        )

        val result = filtered(
            records,
            RecordFilters(
                productId = "p1",
                trigger = "Kahve",
                mood = "Sakin",
                notesOnly = true,
            ),
        )

        assertEquals(listOf("matching"), result.map { it.id })
    }

    @Test
    fun `unknown filter detects each missing snapshot category`() {
        val known = record("known")
        val records = listOf(
            known,
            known.copy(id = "product", productId = null),
            known.copy(id = "price", priceMicrosPerCigaretteSnapshot = null),
            known.copy(id = "nicotine", nicotineMicrogramsPerCigaretteSnapshot = null),
            known.copy(id = "tar", tarMicrogramsPerCigaretteSnapshot = null),
            known.copy(id = "co", carbonMonoxideMicrogramsPerCigaretteSnapshot = null),
        )

        assertEquals(
            listOf("product", "price", "nicotine", "tar", "co"),
            filtered(records, RecordFilters(unknownOnly = true)).map { it.id },
        )
    }

    @Test
    fun `blank search and default filters preserve source order`() {
        val records = listOf(record("one"), record("two"), record("three"))

        assertEquals(records, filtered(records, RecordFilters()))
    }

    private fun assertIds(records: List<SmokingRecordEntity>, query: String, vararg ids: String) {
        assertEquals(
            ids.toList(),
            filterRecords(records, query, RecordFilters(), today, ZoneOffset.UTC).map { it.id },
        )
    }

    private fun filtered(
        records: List<SmokingRecordEntity>,
        filters: RecordFilters,
    ) = filterRecords(records, "", filters, today, ZoneOffset.UTC)

    private fun record(
        id: String,
        date: LocalDate = today,
        productId: String? = "p1",
        productName: String = "Test",
        trigger: String? = null,
        mood: String? = null,
        note: String? = null,
    ) = SmokingRecordEntity(
        id = id,
        smokedAtEpochMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        zoneIdSnapshot = "UTC",
        quantity = 1,
        consumedQuarter = 4,
        productId = productId,
        productRevisionIdSnapshot = "revision",
        productNameSnapshot = productName,
        nicotineMicrogramsPerCigaretteSnapshot = 700L,
        tarMicrogramsPerCigaretteSnapshot = 8_000L,
        carbonMonoxideMicrogramsPerCigaretteSnapshot = 9_000L,
        priceMicrosPerCigaretteSnapshot = 5_000_000L,
        currencyCodeSnapshot = "TRY",
        valueSourceSnapshot = "USER_ENTERED",
        cravingLevel = null,
        trigger = trigger,
        mood = mood,
        locationType = null,
        note = note,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )
}
