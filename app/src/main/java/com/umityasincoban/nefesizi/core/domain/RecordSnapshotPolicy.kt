package com.umityasincoban.nefesizi.core.domain

import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity

data class RecordProductSnapshot(
    val productId: String,
    val revisionId: String?,
    val productName: String,
    val nicotineMicrograms: Long?,
    val tarMicrograms: Long?,
    val carbonMonoxideMicrograms: Long?,
    val priceMicros: Long?,
    val currencyCode: String,
    val valueSource: String?,
)

fun resolveRecordSnapshot(
    existing: SmokingRecordEntity?,
    product: CigaretteProductEntity,
    revision: CigaretteProductRevisionEntity?,
): RecordProductSnapshot {
    val preserved = existing?.takeIf {
        it.productId == product.id && it.productRevisionIdSnapshot == revision?.id
    }
    return if (preserved != null) {
        RecordProductSnapshot(
            productId = product.id,
            revisionId = preserved.productRevisionIdSnapshot,
            productName = preserved.productNameSnapshot,
            nicotineMicrograms = preserved.nicotineMicrogramsPerCigaretteSnapshot
                ?: product.nicotineMicrogramsPerCigarette.takeIf {
                    preserved.productRevisionIdSnapshot == null
                },
            tarMicrograms = preserved.tarMicrogramsPerCigaretteSnapshot
                ?: product.tarMicrogramsPerCigarette.takeIf {
                    preserved.productRevisionIdSnapshot == null
                },
            carbonMonoxideMicrograms = preserved.carbonMonoxideMicrogramsPerCigaretteSnapshot
                ?: product.carbonMonoxideMicrogramsPerCigarette.takeIf {
                    preserved.productRevisionIdSnapshot == null
                },
            priceMicros = preserved.priceMicrosPerCigaretteSnapshot,
            currencyCode = preserved.currencyCodeSnapshot,
            valueSource = preserved.valueSourceSnapshot
                ?: product.valueSource.takeIf { preserved.productRevisionIdSnapshot == null },
        )
    } else {
        RecordProductSnapshot(
            productId = product.id,
            revisionId = revision?.id,
            productName = product.name,
            nicotineMicrograms = revision?.nicotineMicrogramsPerCigarette
                ?: product.nicotineMicrogramsPerCigarette,
            tarMicrograms = revision?.tarMicrogramsPerCigarette
                ?: product.tarMicrogramsPerCigarette,
            carbonMonoxideMicrograms = revision?.carbonMonoxideMicrogramsPerCigarette
                ?: product.carbonMonoxideMicrogramsPerCigarette,
            priceMicros = revision?.priceMicrosPerCigarette,
            currencyCode = revision?.currencyCode ?: product.currencyCode,
            valueSource = revision?.valueSource ?: product.valueSource,
        )
    }
}
