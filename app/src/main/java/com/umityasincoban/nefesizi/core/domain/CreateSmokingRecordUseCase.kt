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
    ): SmokingRecordEntity = create(
        product = product,
        draft = SmokingRecordDraft(
            productId = product.id,
            smokedAtEpochMillis = smokedAtEpochMillis,
        ),
    )

    suspend fun create(
        product: CigaretteProductEntity,
        draft: SmokingRecordDraft,
    ): SmokingRecordEntity {
        require(draft.productId == product.id)
        require(draft.quantity > 0)
        require(draft.consumedQuarter in 1..4)
        require(draft.cravingLevel == null || draft.cravingLevel in 1..5)
        val revision = dao.getProductRevisionAt(product.id, draft.smokedAtEpochMillis)
        val now = clock.millis()
        val record = SmokingRecordEntity(
            id = idGenerator.newId(),
            smokedAtEpochMillis = draft.smokedAtEpochMillis,
            zoneIdSnapshot = clock.zone.id,
            quantity = draft.quantity,
            consumedQuarter = draft.consumedQuarter,
            productId = product.id,
            productRevisionIdSnapshot = revision?.id,
            productNameSnapshot = product.name,
            nicotineMicrogramsPerCigaretteSnapshot = revision?.nicotineMicrogramsPerCigarette,
            tarMicrogramsPerCigaretteSnapshot = revision?.tarMicrogramsPerCigarette,
            carbonMonoxideMicrogramsPerCigaretteSnapshot = revision?.carbonMonoxideMicrogramsPerCigarette,
            priceMicrosPerCigaretteSnapshot = revision?.priceMicrosPerCigarette,
            currencyCodeSnapshot = revision?.currencyCode ?: product.currencyCode,
            valueSourceSnapshot = revision?.valueSource,
            cravingLevel = draft.cravingLevel,
            trigger = draft.trigger?.trim()?.takeIf(String::isNotEmpty),
            mood = draft.mood?.trim()?.takeIf(String::isNotEmpty),
            locationType = draft.locationType?.trim()?.takeIf(String::isNotEmpty),
            note = draft.note?.trim()?.takeIf(String::isNotEmpty),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        dao.insertRecord(record)
        return record
    }
}
