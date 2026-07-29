package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthJournalTest {
    private val today = LocalDate.of(2026, 7, 29)

    @Test
    fun `zero and structurally invalid physiological values are blocked`() {
        val result = validateHealthMeasurements(
            HealthMeasurementDraft(
                restingHeartRate = 0,
                exerciseMinutes = -1,
                systolicBloodPressure = 70,
                diastolicBloodPressure = 80,
                weightGrams = 0,
            ),
        )

        assertFalse(result.canSave)
        assertEquals(4, result.errors.size)
    }

    @Test
    fun `partial blood pressure pair is blocked`() {
        val result = validateHealthMeasurements(
            HealthMeasurementDraft(70, 20, 120, null, 75_000),
        )

        assertFalse(result.canSave)
        assertTrue(result.errors.any { it.contains("birlikte") })
    }

    @Test
    fun `unusual but positive values require confirmation instead of diagnosis`() {
        val result = validateHealthMeasurements(
            HealthMeasurementDraft(250, 700, 260, 160, 600_000),
        )

        assertTrue(result.canSave)
        assertTrue(result.warnings.size >= 4)
        assertFalse(result.warnings.any { it.contains("hipertans", ignoreCase = true) })
    }

    @Test
    fun `nullable measurements and symptoms remain valid`() {
        val result = validateHealthMeasurements(
            HealthMeasurementDraft(null, null, null, null, null),
        )

        assertTrue(result.canSave)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `association is withheld before fourteen common days`() {
        val entries = (0L until 13L).map { offset ->
            entry(today.minusDays(offset), headache = offset < 5)
        }
        val records = entries.map { record(LocalDate.parse(it.entryDate), 2) }

        val result = calculateHealthJournalSummary(entries, records, today)

        assertTrue(result.associations.isEmpty())
    }

    @Test
    fun `association requires at least five days in both compared groups`() {
        val entries = (0L until 14L).map { offset ->
            entry(today.minusDays(offset), headache = offset < 4)
        }
        val records = entries.map { record(LocalDate.parse(it.entryDate), 2) }

        val result = calculateHealthJournalSummary(entries, records, today)

        assertTrue(result.associations.none { it.label == "Baş ağrısı" })
    }

    @Test
    fun `qualified association reports samples and avoids causal claim`() {
        val entries = (0L until 14L).map { offset ->
            entry(today.minusDays(offset), headache = offset < 5)
        }
        val records = entries.map { entry ->
            val date = LocalDate.parse(entry.entryDate)
            record(date, if (entry.headache == true) 4 else 2)
        }

        val result = calculateHealthJournalSummary(entries, records, today)
        val association = result.associations.first { it.label == "Baş ağrısı" }

        assertEquals(5, association.yesDays)
        assertEquals(9, association.noDays)
        assertEquals(4.0, association.yesAverageSmoking, 0.0)
        assertEquals(2.0, association.noAverageSmoking, 0.0)
        assertTrue(association.text.contains("birlikte görülüyor"))
        assertFalse(association.text.contains("sebep", ignoreCase = true))
        assertFalse(association.text.contains("kaynaklan", ignoreCase = true))
    }

    @Test
    fun `qualified scale groups compare high and low days without causal claim`() {
        val entries = (0L until 14L).map { offset ->
            entry(
                date = today.minusDays(offset),
                headache = null,
                energy = if (offset < 7) 5 else 2,
            )
        }
        val records = entries.map { entry ->
            record(LocalDate.parse(entry.entryDate), if (entry.energyLevel == 5) 1 else 4)
        }

        val association = calculateHealthJournalSummary(entries, records, today)
            .associations.first { it.label == "Enerji" }

        assertEquals(7, association.yesDays)
        assertEquals(7, association.noDays)
        assertTrue(association.text.contains("4–5"))
        assertTrue(association.text.contains("birlikte görülüyor"))
        assertFalse(association.text.contains("neden", ignoreCase = true))
    }

    private fun entry(
        date: LocalDate,
        headache: Boolean?,
        energy: Int? = null,
    ) = DailyHealthEntryEntity(
        entryDate = date.toString(),
        zoneId = "UTC",
        energyLevel = energy,
        stressLevel = null,
        sleepQuality = null,
        morningCough = null,
        headache = headache,
        shortnessOfBreath = null,
        chestDiscomfort = null,
        restingHeartRate = null,
        exerciseMinutes = null,
        note = null,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )

    private fun record(date: LocalDate, quantity: Int) = SmokingRecordEntity(
        id = "record-$date",
        smokedAtEpochMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
        zoneIdSnapshot = "UTC",
        quantity = quantity,
        consumedQuarter = 4,
        productId = "product",
        productRevisionIdSnapshot = "revision",
        productNameSnapshot = "Test",
        nicotineMicrogramsPerCigaretteSnapshot = null,
        tarMicrogramsPerCigaretteSnapshot = null,
        carbonMonoxideMicrogramsPerCigaretteSnapshot = null,
        priceMicrosPerCigaretteSnapshot = null,
        currencyCodeSnapshot = "TRY",
        valueSourceSnapshot = null,
        cravingLevel = null,
        trigger = null,
        mood = null,
        locationType = null,
        note = null,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )
}
