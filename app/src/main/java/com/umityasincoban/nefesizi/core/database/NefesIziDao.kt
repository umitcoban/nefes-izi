package com.umityasincoban.nefesizi.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NefesIziDao {
    @Query("SELECT * FROM cigarette_products WHERE isArchived = 0 ORDER BY isDefault DESC, updatedAtEpochMillis DESC")
    fun observeProducts(): Flow<List<CigaretteProductEntity>>

    @Query("SELECT * FROM cigarette_products WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    fun observeDefaultProduct(): Flow<CigaretteProductEntity?>

    @Query("SELECT * FROM cigarette_products WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    suspend fun getDefaultProduct(): CigaretteProductEntity?

    @Query("UPDATE cigarette_products SET isDefault = 0, updatedAtEpochMillis = :updatedAt")
    suspend fun clearDefaultProduct(updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: CigaretteProductEntity)

    @Transaction
    suspend fun replaceDefaultProduct(product: CigaretteProductEntity) {
        clearDefaultProduct(product.updatedAtEpochMillis)
        insertProduct(product.copy(isDefault = true))
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: SmokingRecordEntity)

    @Query("DELETE FROM smoking_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query(
        """
        SELECT * FROM smoking_records
        WHERE smokedAtEpochMillis >= :startInclusive AND smokedAtEpochMillis < :endExclusive
        ORDER BY smokedAtEpochMillis DESC
        """,
    )
    fun observeRecords(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<SmokingRecordEntity>>

    @Query("SELECT * FROM smoking_records ORDER BY smokedAtEpochMillis DESC")
    fun observeAllRecords(): Flow<List<SmokingRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreRecord(record: SmokingRecordEntity)

    @Query("SELECT * FROM daily_health_entries WHERE entryDate = :date LIMIT 1")
    fun observeHealthEntry(date: String): Flow<DailyHealthEntryEntity?>

    @Query(
        """
        SELECT * FROM daily_health_entries
        WHERE entryDate >= :startDate AND entryDate <= :endDate
        ORDER BY entryDate DESC
        """,
    )
    fun observeHealthEntries(startDate: String, endDate: String): Flow<List<DailyHealthEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthEntry(entry: DailyHealthEntryEntity)
}
