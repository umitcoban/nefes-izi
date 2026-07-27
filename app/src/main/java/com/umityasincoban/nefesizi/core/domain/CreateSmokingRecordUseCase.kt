package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.common.IdGenerator
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.Clock
import javax.inject.Inject

class CreateSmokingRecordUseCase @Inject constructor(
    private val dao: NefesIziDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(
        product: CigaretteProductEntity,
        smokedAtEpochMillis: Long = clock.millis(),
    ): SmokingRecordEntity {
        val revision = dao.getProductRevisionAt(product.id, smokedAtEpochMillis)
        val now = clock.millis()
        val record = SmokingRecordEntity(
            id = idGenerator.newId(),
            smokedAtEpochMillis = smokedAtEpochMillis,
            zoneIdSnapshot = clock.zone.id,
            quantity = 1,
            consumedQuarter = 4,
            productId = product.id,
            productRevisionIdSnapshot = revision?.id,
            productNameSnapshot = product.name,
            nicotineMicrogramsPerCigaretteSnapshot = revision?.nicotineMicrogramsPerCigarette,
            tarMicrogramsPerCigaretteSnapshot = revision?.tarMicrogramsPerCigarette,
            carbonMonoxideMicrogramsPerCigaretteSnapshot = revision?.carbonMonoxideMicrogramsPerCigarette,
            priceMicrosPerCigaretteSnapshot = revision?.priceMicrosPerCigarette,
            currencyCodeSnapshot = revision?.currencyCode ?: product.currencyCode,
            valueSourceSnapshot = revision?.valueSource,
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
}
