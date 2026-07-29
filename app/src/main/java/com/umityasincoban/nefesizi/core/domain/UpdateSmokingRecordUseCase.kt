package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.Clock
import javax.inject.Inject

class UpdateSmokingRecordUseCase @Inject constructor(
    private val dao: NefesIziDao,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        existing: SmokingRecordEntity,
        product: CigaretteProductEntity,
        draft: SmokingRecordDraft,
    ): SmokingRecordEntity {
        require(draft.productId == product.id)
        require(validateSmokingRecordDraft(draft, clock.millis()) == null)

        val revision = dao.getProductRevisionAt(product.id, draft.smokedAtEpochMillis)
        val snapshot = resolveRecordSnapshot(existing, product, revision)
        val updated = existing.copy(
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
            updatedAtEpochMillis = clock.millis(),
        )
        dao.updateRecord(updated)
        return updated
    }
}
