package com.umityasincoban.nefesizi.core.backup

import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun `json round trip preserves entities snapshots and preferences`() {
        val original = backup()

        val decoded = BackupCodec.decode(BackupCodec.encode(original)).getOrThrow()

        assertEquals(original, decoded)
        assertTrue(BackupCodec.validate(decoded).isValid)
    }

    @Test
    fun `malformed json is rejected before import`() {
        assertTrue(BackupCodec.decode("{not-json").isFailure)
    }

    @Test
    fun `unsupported schema version is rejected`() {
        val encoded = BackupCodec.encode(backup()).replace(
            "\"schemaVersion\": 1",
            "\"schemaVersion\": 99",
        )

        assertTrue(BackupCodec.decode(encoded).isFailure)
    }

    @Test
    fun `missing product reference is reported`() {
        val invalid = backup().copy(products = emptyList())

        val validation = BackupCodec.validate(invalid)

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("ürünü bulunmuyor") })
    }

    @Test
    fun `duplicate ids and invalid record bounds are rejected`() {
        val source = backup()
        val invalid = source.copy(
            products = source.products + source.products.single(),
            records = listOf(source.records.single().copy(quantity = 0)),
        )

        val validation = BackupCodec.validate(invalid)

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("yinelenen") })
        assertTrue(validation.errors.any { it.contains("miktar") })
    }

    @Test
    fun `missing or mismatched revision snapshot is rejected`() {
        val source = backup()
        val missing = source.copy(
            records = listOf(source.records.single().copy(productRevisionIdSnapshot = "missing")),
        )
        val otherProduct = source.products.single().copy(id = "other", isDefault = false)
        val mismatch = source.copy(
            products = source.products + otherProduct,
            revisions = listOf(source.revisions.single().copy(productId = otherProduct.id)),
        )

        assertTrue(
            BackupCodec.validate(missing).errors.any { it.contains("revizyonu bulunmuyor") },
        )
        assertTrue(
            BackupCodec.validate(mismatch).errors.any { it.contains("ilişkisi geçersiz") },
        )
    }

    @Test
    fun `invalid health measurements are rejected`() {
        val source = backup()
        val invalid = source.copy(
            healthEntries = listOf(
                source.healthEntries.single().copy(
                    energyLevel = 8,
                    restingHeartRate = 0,
                    systolicBloodPressure = 70,
                    diastolicBloodPressure = 90,
                ),
            ),
        )

        val errors = BackupCodec.validate(invalid).errors

        assertTrue(errors.any { it.contains("ölçek") })
        assertTrue(errors.any { it.contains("ölçüm") })
        assertTrue(errors.any { it.contains("tansiyon") })
    }

    @Test
    fun `invalid portable preferences are rejected`() {
        val invalid = backup().copy(
            preferences = mapOf(
                "preferredCurrency" to "NOT_A_CURRENCY",
                "dayStartHour" to "24",
                "firstDayOfWeek" to "FRIDAY",
            ),
        )

        val errors = BackupCodec.validate(invalid).errors

        assertTrue(errors.any { it.contains("para birimi") })
        assertTrue(errors.any { it.contains("başlangıç") })
        assertTrue(errors.any { it.contains("ilk günü") })
    }

    @Test
    fun `invalid product defaults and duplicate revision effective time are rejected`() {
        val source = backup()
        val other = source.products.single().copy(id = "other", isDefault = true)
        val invalid = source.copy(
            products = source.products + other,
            revisions = source.revisions + source.revisions.single().copy(id = "revision-2"),
        )

        val errors = BackupCodec.validate(invalid).errors

        assertTrue(errors.any { it.contains("Birden fazla varsayılan") })
        assertTrue(errors.any { it.contains("yürürlük") })
    }

    @Test
    fun `backup comparison distinguishes duplicates from conflicts`() {
        val incoming = backup()
        val existing = backup().copy(
            records = listOf(incoming.records.single().copy(note = "Farklı")),
        )

        val result = compareBackups(incoming, existing)

        assertEquals(3, result.duplicateCount)
        assertEquals(1, result.conflictCount)
    }

    @Test
    fun `csv escapes quotes newlines and spreadsheet formula prefixes`() {
        assertEquals("\"'=@SUM(A1:A2)\"", csvEscape("=@SUM(A1:A2)"))
        assertEquals("\"a\"\"b\"", csvEscape("a\"b"))
        assertEquals("\"satır\niki\"", csvEscape("satır\niki"))
        assertEquals("", csvEscape(null))
    }

    @Test
    fun `csv export includes complete snapshot and health columns`() {
        val files = backup().toCsvFiles()

        assertTrue(files.getValue("products.csv").contains("updatedAtEpochMillis"))
        assertTrue(files.getValue("product_revisions.csv").contains("packPriceMicros"))
        assertTrue(files.getValue("smoking_records.csv").contains("productRevisionIdSnapshot"))
        assertTrue(files.getValue("smoking_records.csv").contains("createdAtEpochMillis"))
        assertTrue(files.getValue("health_entries.csv").contains("shortnessOfBreath"))
        assertTrue(files.getValue("health_entries.csv").contains("updatedAtEpochMillis"))
    }

    private fun backup(): BackupData {
        val product = CigaretteProductEntity(
            id = "product",
            name = "Test",
            brand = "Marka",
            variant = null,
            nicotineMicrogramsPerCigarette = 700,
            tarMicrogramsPerCigarette = 8_000,
            carbonMonoxideMicrogramsPerCigarette = 9_000,
            priceMicrosPerCigarette = 5_000_000,
            currencyCode = "TRY",
            valueSource = "USER_ENTERED",
            isDefault = true,
            isArchived = false,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 2,
        )
        val revision = CigaretteProductRevisionEntity(
            id = "revision",
            productId = product.id,
            effectiveFromEpochMillis = 1,
            nicotineMicrogramsPerCigarette = 700,
            tarMicrogramsPerCigarette = 8_000,
            carbonMonoxideMicrogramsPerCigarette = 9_000,
            packPriceMicros = 100_000_000,
            cigarettesPerPack = 20,
            priceMicrosPerCigarette = 5_000_000,
            currencyCode = "TRY",
            valueSource = "USER_ENTERED",
            createdAtEpochMillis = 1,
        )
        val record = SmokingRecordEntity(
            id = "record",
            smokedAtEpochMillis = 3,
            zoneIdSnapshot = "Europe/Istanbul",
            quantity = 2,
            consumedQuarter = 3,
            productId = product.id,
            productRevisionIdSnapshot = revision.id,
            productNameSnapshot = product.name,
            nicotineMicrogramsPerCigaretteSnapshot = 700,
            tarMicrogramsPerCigaretteSnapshot = 8_000,
            carbonMonoxideMicrogramsPerCigaretteSnapshot = 9_000,
            priceMicrosPerCigaretteSnapshot = 5_000_000,
            currencyCodeSnapshot = "TRY",
            valueSourceSnapshot = "USER_ENTERED",
            cravingLevel = 4,
            trigger = "Kahve",
            mood = "Sakin",
            locationType = "Ev",
            note = "Not",
            createdAtEpochMillis = 3,
            updatedAtEpochMillis = 4,
        )
        val health = DailyHealthEntryEntity(
            entryDate = "2026-07-29",
            zoneId = "Europe/Istanbul",
            energyLevel = 4,
            stressLevel = 2,
            sleepQuality = 3,
            morningCough = false,
            headache = null,
            shortnessOfBreath = false,
            chestDiscomfort = null,
            restingHeartRate = 70,
            exerciseMinutes = 30,
            systolicBloodPressure = 120,
            diastolicBloodPressure = 80,
            weightGrams = 75_500,
            note = "İyi",
            createdAtEpochMillis = 3,
            updatedAtEpochMillis = 4,
        )
        return BackupData(
            products = listOf(product),
            revisions = listOf(revision),
            records = listOf(record),
            healthEntries = listOf(health),
            preferences = mapOf("themeMode" to "DARK"),
            exportedAtEpochMillis = 5,
            appVersion = "1.0",
        )
    }
}
