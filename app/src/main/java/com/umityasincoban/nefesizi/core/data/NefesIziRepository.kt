package com.umityasincoban.nefesizi.core.data

import com.umityasincoban.nefesizi.core.common.IdGenerator
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import com.umityasincoban.nefesizi.core.domain.CreateSmokingRecordUseCase
import com.umityasincoban.nefesizi.core.domain.SmokingRecordDraft
import com.umityasincoban.nefesizi.core.domain.UpdateSmokingRecordUseCase
import java.time.Clock
import java.time.Instant
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class NefesIziRepository @Inject constructor(
    private val dao: NefesIziDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val createSmokingRecord: CreateSmokingRecordUseCase,
    private val updateSmokingRecord: UpdateSmokingRecordUseCase,
) {
    fun observeDefaultProduct(): Flow<CigaretteProductEntity?> = dao.observeDefaultProduct()

    fun observeProducts(): Flow<List<CigaretteProductEntity>> = dao.observeProducts()

    fun observeAllProducts(): Flow<List<CigaretteProductEntity>> = dao.observeAllProducts()

    fun observeProductRevisions(productId: String): Flow<List<CigaretteProductRevisionEntity>> =
        dao.observeProductRevisions(productId)

    fun observeAllRecords(): Flow<List<SmokingRecordEntity>> = dao.observeAllRecords()

    fun observeRecords(startInclusive: Instant, endExclusive: Instant): Flow<List<SmokingRecordEntity>> =
        dao.observeRecords(startInclusive.toEpochMilli(), endExclusive.toEpochMilli())

    suspend fun createDefaultProduct(
        name: String,
        nicotineMicrograms: Long?,
        tarMicrograms: Long?,
        carbonMonoxideMicrograms: Long?,
    ) {
        val now = clock.millis()
        val productId = idGenerator.newId()
        val currencyCode = defaultCurrencyCode()
        val product = CigaretteProductEntity(
            id = productId,
            name = name.trim(),
            brand = null,
            variant = null,
            nicotineMicrogramsPerCigarette = nicotineMicrograms,
            tarMicrogramsPerCigarette = tarMicrograms,
            carbonMonoxideMicrogramsPerCigarette = carbonMonoxideMicrograms,
            priceMicrosPerCigarette = null,
            currencyCode = currencyCode,
            valueSource = "USER_ENTERED",
            isDefault = true,
            isArchived = false,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        val revision = CigaretteProductRevisionEntity(
            id = idGenerator.newId(),
            productId = productId,
            effectiveFromEpochMillis = now,
            nicotineMicrogramsPerCigarette = nicotineMicrograms,
            tarMicrogramsPerCigarette = tarMicrograms,
            carbonMonoxideMicrogramsPerCigarette = carbonMonoxideMicrograms,
            packPriceMicros = null,
            cigarettesPerPack = null,
            priceMicrosPerCigarette = null,
            currencyCode = currencyCode,
            valueSource = "USER_ENTERED",
            createdAtEpochMillis = now,
        )
        dao.createProductWithRevision(product, revision)
    }

    suspend fun setDefaultProduct(product: CigaretteProductEntity) {
        if (product.isArchived) return
        dao.replaceDefaultProduct(product.copy(updatedAtEpochMillis = clock.millis()))
    }

    suspend fun createProduct(
        identity: ProductIdentityDraft,
        revisionDraft: ProductRevisionDraft,
    ): CigaretteProductEntity {
        val now = clock.millis()
        val productId = idGenerator.newId()
        val shouldBeDefault = dao.getActiveProductCount() == 0
        val product = CigaretteProductEntity(
            id = productId,
            name = identity.name.trim(),
            brand = identity.brand?.trim()?.takeIf(String::isNotEmpty),
            variant = identity.variant?.trim()?.takeIf(String::isNotEmpty),
            nicotineMicrogramsPerCigarette = revisionDraft.nicotineMicrogramsPerCigarette,
            tarMicrogramsPerCigarette = revisionDraft.tarMicrogramsPerCigarette,
            carbonMonoxideMicrogramsPerCigarette = revisionDraft.carbonMonoxideMicrogramsPerCigarette,
            priceMicrosPerCigarette = revisionDraft.priceMicrosPerCigarette,
            currencyCode = revisionDraft.currencyCode,
            valueSource = revisionDraft.valueSource,
            isDefault = shouldBeDefault,
            isArchived = false,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        dao.createProductWithRevision(product, revisionDraft.toEntity(productId, now))
        return product
    }

    suspend fun updateProduct(
        product: CigaretteProductEntity,
        identity: ProductIdentityDraft,
        revisionDraft: ProductRevisionDraft?,
    ) {
        val now = clock.millis()
        val updatedProduct = product.copy(
            name = identity.name.trim(),
            brand = identity.brand?.trim()?.takeIf(String::isNotEmpty),
            variant = identity.variant?.trim()?.takeIf(String::isNotEmpty),
            updatedAtEpochMillis = now,
        )
        dao.updateProductWithRevision(
            updatedProduct,
            revisionDraft?.toEntity(product.id, now),
            now,
        )
    }

    suspend fun duplicateProduct(
        source: CigaretteProductEntity,
        revision: CigaretteProductRevisionEntity?,
    ): CigaretteProductEntity {
        val now = clock.millis()
        return createProduct(
            identity = ProductIdentityDraft(
                name = "${source.name} kopyası",
                brand = source.brand,
                variant = source.variant,
            ),
            revisionDraft = ProductRevisionDraft(
                effectiveFromEpochMillis = now,
                nicotineMicrogramsPerCigarette =
                    revision?.nicotineMicrogramsPerCigarette
                        ?: source.nicotineMicrogramsPerCigarette,
                tarMicrogramsPerCigarette =
                    revision?.tarMicrogramsPerCigarette
                        ?: source.tarMicrogramsPerCigarette,
                carbonMonoxideMicrogramsPerCigarette =
                    revision?.carbonMonoxideMicrogramsPerCigarette
                        ?: source.carbonMonoxideMicrogramsPerCigarette,
                packPriceMicros = revision?.packPriceMicros,
                cigarettesPerPack = revision?.cigarettesPerPack,
                priceMicrosPerCigarette =
                    revision?.priceMicrosPerCigarette ?: source.priceMicrosPerCigarette,
                currencyCode = revision?.currencyCode ?: source.currencyCode,
                valueSource = revision?.valueSource ?: source.valueSource,
            ),
        )
    }

    suspend fun setProductArchived(
        product: CigaretteProductEntity,
        archived: Boolean,
    ): Boolean = dao.setProductArchived(product, archived, clock.millis())

    suspend fun logWithDefaultProduct(): SmokingRecordEntity? {
        val product = dao.getDefaultProduct() ?: return null
        return createSmokingRecord(product)
    }

    suspend fun logWithProduct(productId: String): SmokingRecordEntity? {
        val product = dao.getProduct(productId)?.takeUnless { it.isArchived } ?: return null
        return createSmokingRecord(product)
    }

    suspend fun createRecord(draft: SmokingRecordDraft): SmokingRecordEntity? {
        val product = dao.getProduct(draft.productId) ?: return null
        return createSmokingRecord.create(product, draft)
    }

    suspend fun updateRecord(
        existing: SmokingRecordEntity,
        draft: SmokingRecordDraft,
    ): SmokingRecordEntity? {
        val product = dao.getProduct(draft.productId) ?: return null
        return updateSmokingRecord(existing, product, draft)
    }

    suspend fun undoRecord(id: String) = dao.deleteRecord(id)

    suspend fun restoreRecord(record: SmokingRecordEntity) = dao.restoreRecord(record)

    fun observeHealthEntry(date: String): Flow<DailyHealthEntryEntity?> =
        dao.observeHealthEntry(date)

    fun observeHealthEntries(startDate: String, endDate: String): Flow<List<DailyHealthEntryEntity>> =
        dao.observeHealthEntries(startDate, endDate)

    suspend fun saveHealthEntry(entry: DailyHealthEntryEntity) = dao.upsertHealthEntry(entry)

    private fun defaultCurrencyCode(): String = runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("TRY")

    private fun ProductRevisionDraft.toEntity(
        productId: String,
        createdAt: Long,
    ) = CigaretteProductRevisionEntity(
        id = idGenerator.newId(),
        productId = productId,
        effectiveFromEpochMillis = effectiveFromEpochMillis,
        nicotineMicrogramsPerCigarette = nicotineMicrogramsPerCigarette,
        tarMicrogramsPerCigarette = tarMicrogramsPerCigarette,
        carbonMonoxideMicrogramsPerCigarette = carbonMonoxideMicrogramsPerCigarette,
        packPriceMicros = packPriceMicros,
        cigarettesPerPack = cigarettesPerPack,
        priceMicrosPerCigarette = priceMicrosPerCigarette,
        currencyCode = currencyCode,
        valueSource = valueSource,
        createdAtEpochMillis = createdAt,
    )
}
