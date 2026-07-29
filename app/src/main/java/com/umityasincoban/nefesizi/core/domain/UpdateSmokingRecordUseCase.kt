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
        require(draft.quantity > 0)
        require(draft.consumedQuarter in 1..4)
        require(draft.cravingLevel == null || draft.cravingLevel in 1..5)

        val revision = dao.getProductRevisionAt(product.id, draft.smokedAtEpochMillis)
        val preserveSnapshot = existing.productId == product.id &&
            existing.productRevisionIdSnapshot == revision?.id
        val updated = existing.copy(
            smokedAtEpochMillis = draft.smokedAtEpochMillis,
            zoneIdSnapshot = clock.zone.id,
            quantity = draft.quantity,
            consumedQuarter = draft.consumedQuarter,
            productId = product.id,
            productRevisionIdSnapshot =
                if (preserveSnapshot) existing.productRevisionIdSnapshot else revision?.id,
            productNameSnapshot =
                if (preserveSnapshot) existing.productNameSnapshot else product.name,
            nicotineMicrogramsPerCigaretteSnapshot =
                if (preserveSnapshot) {
                    existing.nicotineMicrogramsPerCigaretteSnapshot
                } else {
                    revision?.nicotineMicrogramsPerCigarette
                },
            tarMicrogramsPerCigaretteSnapshot =
                if (preserveSnapshot) {
                    existing.tarMicrogramsPerCigaretteSnapshot
                } else {
                    revision?.tarMicrogramsPerCigarette
                },
            carbonMonoxideMicrogramsPerCigaretteSnapshot =
                if (preserveSnapshot) {
                    existing.carbonMonoxideMicrogramsPerCigaretteSnapshot
                } else {
                    revision?.carbonMonoxideMicrogramsPerCigarette
                },
            priceMicrosPerCigaretteSnapshot =
                if (preserveSnapshot) {
                    existing.priceMicrosPerCigaretteSnapshot
                } else {
                    revision?.priceMicrosPerCigarette
                },
            currencyCodeSnapshot =
                if (preserveSnapshot) {
                    existing.currencyCodeSnapshot
                } else {
                    revision?.currencyCode ?: product.currencyCode
                },
            valueSourceSnapshot =
                if (preserveSnapshot) existing.valueSourceSnapshot else revision?.valueSource,
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
