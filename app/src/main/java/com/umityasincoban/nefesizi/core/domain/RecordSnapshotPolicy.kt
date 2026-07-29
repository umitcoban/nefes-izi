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
            nicotineMicrograms = preserved.nicotineMicrogramsPerCigaretteSnapshot,
            tarMicrograms = preserved.tarMicrogramsPerCigaretteSnapshot,
            carbonMonoxideMicrograms = preserved.carbonMonoxideMicrogramsPerCigaretteSnapshot,
            priceMicros = preserved.priceMicrosPerCigaretteSnapshot,
            currencyCode = preserved.currencyCodeSnapshot,
            valueSource = preserved.valueSourceSnapshot,
        )
    } else {
        RecordProductSnapshot(
            productId = product.id,
            revisionId = revision?.id,
            productName = product.name,
            nicotineMicrograms = revision?.nicotineMicrogramsPerCigarette,
            tarMicrograms = revision?.tarMicrogramsPerCigarette,
            carbonMonoxideMicrograms = revision?.carbonMonoxideMicrogramsPerCigarette,
            priceMicros = revision?.priceMicrosPerCigarette,
            currencyCode = revision?.currencyCode ?: product.currencyCode,
            valueSource = revision?.valueSource,
        )
    }
}
