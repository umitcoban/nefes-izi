package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordSnapshotPolicyTest {
    @Test
    fun `same product and revision preserve original historical values`() {
        val existing = record()
        val renamed = product().copy(name = "Yeni ad", currencyCode = "EUR")
        val changedMirror = revision().copy(priceMicrosPerCigarette = 99_000_000L)

        val result = resolveRecordSnapshot(existing, renamed, changedMirror)

        assertEquals("Eski ad", result.productName)
        assertEquals(5_000_000L, result.priceMicros)
        assertEquals("TRY", result.currencyCode)
        assertEquals(700L, result.nicotineMicrograms)
    }

    @Test
    fun `different revision takes values effective at new event time`() {
        val newRevision = revision().copy(
            id = "revision-new",
            priceMicrosPerCigarette = 6_000_000L,
            nicotineMicrogramsPerCigarette = 800L,
        )

        val result = resolveRecordSnapshot(record(), product().copy(name = "Yeni ad"), newRevision)

        assertEquals("revision-new", result.revisionId)
        assertEquals("Yeni ad", result.productName)
        assertEquals(6_000_000L, result.priceMicros)
        assertEquals(800L, result.nicotineMicrograms)
    }

    @Test
    fun `changing product always takes selected product snapshot`() {
        val selected = product().copy(id = "product-two", name = "İkinci ürün")
        val selectedRevision = revision().copy(id = "revision", productId = selected.id)

        val result = resolveRecordSnapshot(record(), selected, selectedRevision)

        assertEquals("product-two", result.productId)
        assertEquals("İkinci ürün", result.productName)
        assertEquals(6_000_000L, result.priceMicros)
    }

    @Test
    fun `date before first revision uses known product chemicals but not an inferred price`() {
        val result = resolveRecordSnapshot(null, product(), null)

        assertNull(result.revisionId)
        assertNull(result.priceMicros)
        assertEquals(800L, result.nicotineMicrograms)
        assertEquals(9_000L, result.tarMicrograms)
        assertEquals(10_000L, result.carbonMonoxideMicrograms)
        assertEquals("TRY", result.currencyCode)
        assertEquals("USER_ENTERED", result.valueSource)
    }

    @Test
    fun `existing unknown snapshot stays historical while revision remains absent`() {
        val existing = record().copy(
            productRevisionIdSnapshot = null,
            priceMicrosPerCigaretteSnapshot = null,
            nicotineMicrogramsPerCigaretteSnapshot = 650L,
            valueSourceSnapshot = "USER_ENTERED",
        )

        val result = resolveRecordSnapshot(existing, product().copy(name = "Değişen ad"), null)

        assertEquals("Eski ad", result.productName)
        assertEquals(650L, result.nicotineMicrograms)
        assertNull(result.priceMicros)
        assertNull(result.revisionId)
    }

    @Test
    fun `legacy record without revision is enriched with product chemicals on edit`() {
        val legacy = record().copy(
            productRevisionIdSnapshot = null,
            nicotineMicrogramsPerCigaretteSnapshot = null,
            tarMicrogramsPerCigaretteSnapshot = null,
            carbonMonoxideMicrogramsPerCigaretteSnapshot = null,
            priceMicrosPerCigaretteSnapshot = null,
            valueSourceSnapshot = null,
        )

        val result = resolveRecordSnapshot(legacy, product(), null)

        assertEquals(800L, result.nicotineMicrograms)
        assertEquals(9_000L, result.tarMicrograms)
        assertEquals(10_000L, result.carbonMonoxideMicrograms)
        assertNull(result.priceMicros)
        assertEquals("USER_ENTERED", result.valueSource)
    }

    private fun product() = CigaretteProductEntity(
        id = "product",
        name = "Güncel ad",
        brand = null,
        variant = null,
        nicotineMicrogramsPerCigarette = 800L,
        tarMicrogramsPerCigarette = 9_000L,
        carbonMonoxideMicrogramsPerCigarette = 10_000L,
        priceMicrosPerCigarette = 6_000_000L,
        currencyCode = "TRY",
        valueSource = "USER_ENTERED",
        isDefault = true,
        isArchived = false,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )

    private fun revision() = CigaretteProductRevisionEntity(
        id = "revision",
        productId = "product",
        effectiveFromEpochMillis = 0L,
        nicotineMicrogramsPerCigarette = 800L,
        tarMicrogramsPerCigarette = 9_000L,
        carbonMonoxideMicrogramsPerCigarette = 10_000L,
        packPriceMicros = 120_000_000L,
        cigarettesPerPack = 20,
        priceMicrosPerCigarette = 6_000_000L,
        currencyCode = "TRY",
        valueSource = "USER_ENTERED",
        createdAtEpochMillis = 0L,
    )

    private fun record() = SmokingRecordEntity(
        id = "record",
        smokedAtEpochMillis = 0L,
        zoneIdSnapshot = "UTC",
        quantity = 1,
        consumedQuarter = 4,
        productId = "product",
        productRevisionIdSnapshot = "revision",
        productNameSnapshot = "Eski ad",
        nicotineMicrogramsPerCigaretteSnapshot = 700L,
        tarMicrogramsPerCigaretteSnapshot = 8_000L,
        carbonMonoxideMicrogramsPerCigaretteSnapshot = 9_000L,
        priceMicrosPerCigaretteSnapshot = 5_000_000L,
        currencyCodeSnapshot = "TRY",
        valueSourceSnapshot = "USER_ENTERED",
        cravingLevel = null,
        trigger = null,
        mood = null,
        locationType = null,
        note = null,
        createdAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )
}
