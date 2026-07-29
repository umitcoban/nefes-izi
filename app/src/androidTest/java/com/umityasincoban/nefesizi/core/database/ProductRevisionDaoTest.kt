package com.umityasincoban.nefesizi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.umityasincoban.nefesizi.core.common.IdGenerator
import com.umityasincoban.nefesizi.core.domain.CreateSmokingRecordUseCase
import com.umityasincoban.nefesizi.core.domain.SmokingRecordDraft
import com.umityasincoban.nefesizi.core.domain.UpdateSmokingRecordUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductRevisionDaoTest {
    private lateinit var database: NefesIziDatabase
    private lateinit var dao: NefesIziDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NefesIziDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.dao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun revisionResolutionUsesLatestValueEffectiveAtEventTime() = runBlocking {
        val product = product()
        dao.createProductWithRevision(
            product,
            revision(
                id = "revision-old",
                effectiveFrom = 1_000L,
                priceMicros = 5_000_000L,
            ),
        )
        dao.insertProductRevision(
            revision(
                id = "revision-new",
                effectiveFrom = 2_000L,
                priceMicros = 6_000_000L,
            ),
        )

        assertNull(dao.getProductRevisionAt(product.id, 999L))
        assertEquals(
            5_000_000L,
            dao.getProductRevisionAt(product.id, 1_999L)?.priceMicrosPerCigarette,
        )
        assertEquals(
            6_000_000L,
            dao.getProductRevisionAt(product.id, 2_000L)?.priceMicrosPerCigarette,
        )
    }

    @Test
    fun smokingRecordsKeepPriceSnapshotAcrossPriceIncrease() = runBlocking {
        val product = product()
        dao.createProductWithRevision(
            product,
            revision(
                id = "revision-old",
                effectiveFrom = 1_000L,
                priceMicros = 5_000_000L,
            ),
        )
        dao.insertProductRevision(
            revision(
                id = "revision-new",
                effectiveFrom = 2_000L,
                priceMicros = 6_000_000L,
            ),
        )
        val ids = ArrayDeque(listOf("record-old", "record-new"))
        val useCase = CreateSmokingRecordUseCase(
            dao = dao,
            clock = Clock.fixed(Instant.ofEpochMilli(2_500L), ZoneOffset.UTC),
            idGenerator = IdGenerator { ids.removeFirst() },
        )

        val oldRecord = useCase(product, smokedAtEpochMillis = 1_500L)
        val newRecord = useCase(product, smokedAtEpochMillis = 2_500L)

        assertEquals("revision-old", oldRecord.productRevisionIdSnapshot)
        assertEquals(5_000_000L, oldRecord.priceMicrosPerCigaretteSnapshot)
        assertEquals("revision-new", newRecord.productRevisionIdSnapshot)
        assertEquals(6_000_000L, newRecord.priceMicrosPerCigaretteSnapshot)
        assertEquals(
            5_000_000L,
            dao.observeAllRecords()
                .first()
                .first { it.id == oldRecord.id }
                .priceMicrosPerCigaretteSnapshot,
        )
    }

    @Test
    fun recordBeforeFirstRevisionUsesProductChemicalsButKeepsPriceUnknown() = runBlocking {
        val product = product()
        dao.createProductWithRevision(
            product,
            revision(
                id = "revision-first",
                effectiveFrom = 2_000L,
                priceMicros = 6_000_000L,
            ),
        )
        val useCase = CreateSmokingRecordUseCase(
            dao = dao,
            clock = Clock.fixed(Instant.ofEpochMilli(2_500L), ZoneOffset.UTC),
            idGenerator = IdGenerator { "record-before-history" },
        )

        val record = useCase(product, smokedAtEpochMillis = 1_000L)

        assertNull(record.productRevisionIdSnapshot)
        assertEquals(700L, record.nicotineMicrogramsPerCigaretteSnapshot)
        assertEquals(8_000L, record.tarMicrogramsPerCigaretteSnapshot)
        assertEquals(9_000L, record.carbonMonoxideMicrogramsPerCigaretteSnapshot)
        assertNull(record.priceMicrosPerCigaretteSnapshot)
    }

    @Test
    fun detailedRecordPersistsContextAndHistoricalSnapshot() = runBlocking {
        val product = product()
        dao.createProductWithRevision(
            product,
            revision("revision-old", 1_000L, 5_000_000L),
        )
        val useCase = CreateSmokingRecordUseCase(
            dao = dao,
            clock = Clock.fixed(Instant.ofEpochMilli(3_000L), ZoneOffset.UTC),
            idGenerator = IdGenerator { "record-detailed" },
        )

        val record = useCase.create(
            product = product,
            draft = SmokingRecordDraft(
                productId = product.id,
                smokedAtEpochMillis = 1_500L,
                quantity = 2,
                consumedQuarter = 3,
                cravingLevel = 4,
                trigger = "Kahve",
                mood = "Sakin",
                locationType = "Ev",
                note = "Sabah kaydı",
            ),
        )

        assertEquals(2, record.quantity)
        assertEquals(3, record.consumedQuarter)
        assertEquals(4, record.cravingLevel)
        assertEquals("Kahve", record.trigger)
        assertEquals("Sakin", record.mood)
        assertEquals("Ev", record.locationType)
        assertEquals("Sabah kaydı", record.note)
        assertEquals("revision-old", record.productRevisionIdSnapshot)
        assertEquals(5_000_000L, record.priceMicrosPerCigaretteSnapshot)
    }

    @Test
    fun editingMetadataPreservesSnapshotButCrossingRevisionResnapshots() = runBlocking {
        val product = product()
        dao.createProductWithRevision(
            product,
            revision("revision-old", 1_000L, 5_000_000L),
        )
        dao.insertProductRevision(
            revision("revision-new", 2_000L, 6_000_000L),
        )
        val clock = Clock.fixed(Instant.ofEpochMilli(4_000L), ZoneOffset.UTC)
        val created = CreateSmokingRecordUseCase(
            dao = dao,
            clock = clock,
            idGenerator = IdGenerator { "record-edit" },
        )(product, 1_500L)
        val update = UpdateSmokingRecordUseCase(dao, clock)
        val renamedProduct = product.copy(name = "Yeni ürün adı")

        val metadataEdit = update(
            existing = created,
            product = renamedProduct,
            draft = SmokingRecordDraft(
                productId = product.id,
                smokedAtEpochMillis = 1_700L,
                quantity = 2,
                consumedQuarter = 2,
                note = "Düzenlendi",
            ),
        )

        assertEquals("revision-old", metadataEdit.productRevisionIdSnapshot)
        assertEquals("Test", metadataEdit.productNameSnapshot)
        assertEquals(5_000_000L, metadataEdit.priceMicrosPerCigaretteSnapshot)
        assertEquals("Düzenlendi", metadataEdit.note)

        val crossedRevision = update(
            existing = metadataEdit,
            product = renamedProduct,
            draft = SmokingRecordDraft(
                productId = product.id,
                smokedAtEpochMillis = 2_500L,
                quantity = 2,
                consumedQuarter = 2,
            ),
        )

        assertEquals("revision-new", crossedRevision.productRevisionIdSnapshot)
        assertEquals("Yeni ürün adı", crossedRevision.productNameSnapshot)
        assertEquals(6_000_000L, crossedRevision.priceMicrosPerCigaretteSnapshot)
        assertEquals(
            6_000_000L,
            dao.observeAllRecords().first().single().priceMicrosPerCigaretteSnapshot,
        )
    }

    @Test
    fun backdatedRevisionDoesNotReplaceNewerCurrentMirrorValues() = runBlocking {
        val product = product()
        dao.createProductWithRevision(
            product,
            revision("revision-old", 1_000L, 5_000_000L),
        )
        dao.updateProductWithRevision(
            product = product.copy(updatedAtEpochMillis = 3_000L),
            revision = revision("revision-current", 3_000L, 7_000_000L),
            nowEpochMillis = 4_000L,
        )
        dao.updateProductWithRevision(
            product = product.copy(updatedAtEpochMillis = 4_000L),
            revision = revision("revision-backdated", 2_000L, 6_000_000L),
            nowEpochMillis = 4_000L,
        )

        val mirrored = dao.observeAllProducts().first().single()

        assertEquals(7_000_000L, mirrored.priceMicrosPerCigarette)
        assertEquals(
            "revision-current",
            dao.getProductRevisionAt(product.id, 4_000L)?.id,
        )
    }

    @Test
    fun archivingDefaultTransfersDefaultAndLastActiveCannotBeArchived() = runBlocking {
        val first = product(id = "first", isDefault = true)
        val second = product(id = "second", isDefault = false)
        dao.createProductWithRevision(
            first,
            revision("first-revision", 1_000L, 5_000_000L, productId = first.id),
        )
        dao.createProductWithRevision(
            second,
            revision("second-revision", 1_000L, 6_000_000L, productId = second.id),
        )

        assertEquals(true, dao.setProductArchived(first, true, 2_000L))
        val afterFirstArchive = dao.observeAllProducts().first()
        assertEquals(true, afterFirstArchive.first { it.id == first.id }.isArchived)
        assertEquals(true, afterFirstArchive.first { it.id == second.id }.isDefault)
        assertEquals(
            false,
            dao.setProductArchived(
                afterFirstArchive.first { it.id == second.id },
                true,
                3_000L,
            ),
        )
    }

    private fun product(
        id: String = "product",
        isDefault: Boolean = true,
    ) = CigaretteProductEntity(
        id = id,
        name = "Test",
        brand = null,
        variant = null,
        nicotineMicrogramsPerCigarette = 700,
        tarMicrogramsPerCigarette = 8_000,
        carbonMonoxideMicrogramsPerCigarette = 9_000,
        priceMicrosPerCigarette = 5_000_000,
        currencyCode = "TRY",
        valueSource = "USER_ENTERED",
        isDefault = isDefault,
        isArchived = false,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 1_000,
    )

    private fun revision(
        id: String,
        effectiveFrom: Long,
        priceMicros: Long,
        productId: String = "product",
    ) = CigaretteProductRevisionEntity(
        id = id,
        productId = productId,
        effectiveFromEpochMillis = effectiveFrom,
        nicotineMicrogramsPerCigarette = 700,
        tarMicrogramsPerCigarette = 8_000,
        carbonMonoxideMicrogramsPerCigarette = 9_000,
        packPriceMicros = priceMicros * 20,
        cigarettesPerPack = 20,
        priceMicrosPerCigarette = priceMicros,
        currencyCode = "TRY",
        valueSource = "USER_ENTERED",
        createdAtEpochMillis = effectiveFrom,
    )
}
