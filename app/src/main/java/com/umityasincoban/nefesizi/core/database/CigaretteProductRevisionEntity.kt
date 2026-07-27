package com.umityasincoban.nefesizi.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cigarette_product_revisions",
    foreignKeys = [
        ForeignKey(
            entity = CigaretteProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(
            value = ["productId", "effectiveFromEpochMillis"],
            unique = true,
        ),
    ],
)
data class CigaretteProductRevisionEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val effectiveFromEpochMillis: Long,
    val nicotineMicrogramsPerCigarette: Long?,
    val tarMicrogramsPerCigarette: Long?,
    val carbonMonoxideMicrogramsPerCigarette: Long?,
    val packPriceMicros: Long?,
    val cigarettesPerPack: Int?,
    val priceMicrosPerCigarette: Long?,
    val currencyCode: String,
    val valueSource: String,
    val createdAtEpochMillis: Long,
)
