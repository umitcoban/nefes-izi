package com.umityasincoban.nefesizi.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cigarette_products",
    indices = [Index("isDefault"), Index("isArchived")],
)
data class CigaretteProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val variant: String?,
    val nicotineMicrogramsPerCigarette: Long?,
    val tarMicrogramsPerCigarette: Long?,
    val carbonMonoxideMicrogramsPerCigarette: Long?,
    val priceMicrosPerCigarette: Long?,
    val currencyCode: String,
    val valueSource: String,
    val isDefault: Boolean,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
