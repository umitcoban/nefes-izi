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
        require(validateSmokingRecordDraft(draft, clock.millis()) == null)
        val revision = dao.getProductRevisionAt(product.id, draft.smokedAtEpochMillis)
        val snapshot = resolveRecordSnapshot(null, product, revision)
        val now = clock.millis()
        val record = SmokingRecordEntity(
            id = idGenerator.newId(),
            smokedAtEpochMillis = draft.smokedAtEpochMillis,
            zoneIdSnapshot = clock.zone.id,
            quantity = draft.quantity,
            consumedQuarter = draft.consumedQuarter,
            productId = snapshot.productId,
            productRevisionIdSnapshot = snapshot.revisionId,
            productNameSnapshot = snapshot.productName,
            nicotineMicrogramsPerCigaretteSnapshot = snapshot.nicotineMicrograms,
            tarMicrogramsPerCigaretteSnapshot = snapshot.tarMicrograms,
            carbonMonoxideMicrogramsPerCigaretteSnapshot = snapshot.carbonMonoxideMicrograms,
            priceMicrosPerCigaretteSnapshot = snapshot.priceMicros,
            currencyCodeSnapshot = snapshot.currencyCode,
            valueSourceSnapshot = snapshot.valueSource,
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
