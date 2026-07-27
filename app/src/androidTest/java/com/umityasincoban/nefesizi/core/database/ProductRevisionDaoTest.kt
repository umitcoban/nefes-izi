package com.umityasincoban.nefesizi.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.umityasincoban.nefesizi.core.common.IdGenerator
import com.umityasincoban.nefesizi.core.domain.CreateSmokingRecordUseCase
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
    fun recordBeforeFirstRevisionKeepsProductValuesUnknown() = runBlocking {
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
        assertNull(record.nicotineMicrogramsPerCigaretteSnapshot)
        assertNull(record.priceMicrosPerCigaretteSnapshot)
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
