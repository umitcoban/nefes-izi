package com.umityasincoban.nefesizi.core.data

import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class NefesIziRepository @Inject constructor(
    private val dao: NefesIziDao,
    private val clock: Clock,
) {
    fun observeDefaultProduct(): Flow<CigaretteProductEntity?> = dao.observeDefaultProduct()

    fun observeProducts(): Flow<List<CigaretteProductEntity>> = dao.observeProducts()

    fun observeRecords(startInclusive: Instant, endExclusive: Instant): Flow<List<SmokingRecordEntity>> =
        dao.observeRecords(startInclusive.toEpochMilli(), endExclusive.toEpochMilli())

    suspend fun createDefaultProduct(
        name: String,
        nicotineMicrograms: Long?,
        tarMicrograms: Long?,
        carbonMonoxideMicrograms: Long?,
    ) {
        val now = clock.millis()
        dao.replaceDefaultProduct(
            CigaretteProductEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                brand = null,
                variant = null,
                nicotineMicrogramsPerCigarette = nicotineMicrograms,
                tarMicrogramsPerCigarette = tarMicrograms,
                carbonMonoxideMicrogramsPerCigarette = carbonMonoxideMicrograms,
                priceMicrosPerCigarette = null,
                currencyCode = defaultCurrencyCode(),
                valueSource = "USER_ENTERED",
                isDefault = true,
                isArchived = false,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
    }

    suspend fun logWithDefaultProduct(): SmokingRecordEntity? {
        val product = dao.getDefaultProduct() ?: return null
        val now = clock.millis()
        val record = SmokingRecordEntity(
            id = UUID.randomUUID().toString(),
            smokedAtEpochMillis = now,
            zoneIdSnapshot = ZoneId.systemDefault().id,
            quantity = 1,
            consumedQuarter = 4,
            productId = product.id,
            productNameSnapshot = product.name,
            nicotineMicrogramsPerCigaretteSnapshot = product.nicotineMicrogramsPerCigarette,
            tarMicrogramsPerCigaretteSnapshot = product.tarMicrogramsPerCigarette,
            carbonMonoxideMicrogramsPerCigaretteSnapshot = product.carbonMonoxideMicrogramsPerCigarette,
            priceMicrosPerCigaretteSnapshot = product.priceMicrosPerCigarette,
            currencyCodeSnapshot = product.currencyCode,
            cravingLevel = null,
            trigger = null,
            mood = null,
            locationType = null,
            note = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        dao.insertRecord(record)
        return record
    }

    suspend fun undoRecord(id: String) = dao.deleteRecord(id)

    private fun defaultCurrencyCode(): String = runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("TRY")
}
