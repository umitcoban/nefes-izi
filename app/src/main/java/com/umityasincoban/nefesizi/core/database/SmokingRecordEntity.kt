package com.umityasincoban.nefesizi.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "smoking_records",
    indices = [Index("smokedAtEpochMillis"), Index("productId")],
)
data class SmokingRecordEntity(
    @PrimaryKey val id: String,
    val smokedAtEpochMillis: Long,
    val zoneIdSnapshot: String,
    val quantity: Int,
    val consumedQuarter: Int,
    val productId: String?,
    val productNameSnapshot: String,
    val nicotineMicrogramsPerCigaretteSnapshot: Long?,
    val tarMicrogramsPerCigaretteSnapshot: Long?,
    val carbonMonoxideMicrogramsPerCigaretteSnapshot: Long?,
    val priceMicrosPerCigaretteSnapshot: Long?,
    val currencyCodeSnapshot: String,
    val cravingLevel: Int?,
    val trigger: String?,
    val mood: String?,
    val locationType: String?,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
