package com.umityasincoban.nefesizi.core.data

import com.umityasincoban.nefesizi.core.common.IdGenerator
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.database.DailyHealthEntryEntity
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import com.umityasincoban.nefesizi.core.domain.CreateSmokingRecordUseCase
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
) {
    fun observeDefaultProduct(): Flow<CigaretteProductEntity?> = dao.observeDefaultProduct()

    fun observeProducts(): Flow<List<CigaretteProductEntity>> = dao.observeProducts()

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
        dao.replaceDefaultProduct(product.copy(updatedAtEpochMillis = clock.millis()))
    }

    suspend fun logWithDefaultProduct(): SmokingRecordEntity? {
        val product = dao.getDefaultProduct() ?: return null
        return createSmokingRecord(product)
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
}
