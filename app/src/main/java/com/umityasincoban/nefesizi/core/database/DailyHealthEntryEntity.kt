package com.umityasincoban.nefesizi.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_entries")
data class DailyHealthEntryEntity(
    @PrimaryKey val entryDate: String,
    val zoneId: String,
    val energyLevel: Int?,
    val stressLevel: Int?,
    val sleepQuality: Int?,
    val morningCough: Boolean?,
    val headache: Boolean?,
    val shortnessOfBreath: Boolean?,
    val chestDiscomfort: Boolean?,
    val restingHeartRate: Int?,
    val exerciseMinutes: Int?,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
