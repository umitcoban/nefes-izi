package com.umityasincoban.nefesizi.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NefesIziDao {
    @Query("SELECT * FROM cigarette_products WHERE isArchived = 0 ORDER BY isDefault DESC, updatedAtEpochMillis DESC")
    fun observeProducts(): Flow<List<CigaretteProductEntity>>

    @Query("SELECT * FROM cigarette_products ORDER BY isArchived ASC, isDefault DESC, updatedAtEpochMillis DESC")
    fun observeAllProducts(): Flow<List<CigaretteProductEntity>>

    @Query("SELECT COUNT(*) FROM cigarette_products WHERE isArchived = 0")
    suspend fun getActiveProductCount(): Int

    @Query("SELECT * FROM cigarette_products WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    fun observeDefaultProduct(): Flow<CigaretteProductEntity?>

    @Query("SELECT * FROM cigarette_products WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    suspend fun getDefaultProduct(): CigaretteProductEntity?

    @Query("SELECT * FROM cigarette_products WHERE id = :id LIMIT 1")
    suspend fun getProduct(id: String): CigaretteProductEntity?

    @Query("UPDATE cigarette_products SET isDefault = 0, updatedAtEpochMillis = :updatedAt")
    suspend fun clearDefaultProduct(updatedAt: Long)

    @Upsert
    suspend fun upsertProduct(product: CigaretteProductEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProductRevision(revision: CigaretteProductRevisionEntity)

    @Query(
        """
        SELECT * FROM cigarette_product_revisions
        WHERE productId = :productId AND effectiveFromEpochMillis <= :atEpochMillis
        ORDER BY effectiveFromEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getProductRevisionAt(
        productId: String,
        atEpochMillis: Long,
    ): CigaretteProductRevisionEntity?

    @Query(
        """
        SELECT * FROM cigarette_product_revisions
        WHERE productId = :productId
        ORDER BY effectiveFromEpochMillis DESC
        """,
    )
    fun observeProductRevisions(productId: String): Flow<List<CigaretteProductRevisionEntity>>

    @Transaction
    suspend fun createProductWithRevision(
        product: CigaretteProductEntity,
        revision: CigaretteProductRevisionEntity,
    ) {
        if (product.isDefault) clearDefaultProduct(product.updatedAtEpochMillis)
        upsertProduct(product)
        insertProductRevision(revision)
    }

    @Transaction
    suspend fun updateProductWithRevision(
        product: CigaretteProductEntity,
        revision: CigaretteProductRevisionEntity?,
        nowEpochMillis: Long,
    ) {
        if (revision != null) insertProductRevision(revision)
        val current = getProductRevisionAt(product.id, nowEpochMillis)
        upsertProduct(
            if (current == null) {
                product
            } else {
                product.copy(
                    nicotineMicrogramsPerCigarette = current.nicotineMicrogramsPerCigarette,
                    tarMicrogramsPerCigarette = current.tarMicrogramsPerCigarette,
                    carbonMonoxideMicrogramsPerCigarette =
                        current.carbonMonoxideMicrogramsPerCigarette,
                    priceMicrosPerCigarette = current.priceMicrosPerCigarette,
                    currencyCode = current.currencyCode,
                    valueSource = current.valueSource,
                )
            },
        )
    }

    @Query(
        """
        SELECT * FROM cigarette_products
        WHERE isArchived = 0 AND id != :excludedId
        ORDER BY updatedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getDefaultReplacement(excludedId: String): CigaretteProductEntity?

    @Transaction
    suspend fun setProductArchived(
        product: CigaretteProductEntity,
        archived: Boolean,
        updatedAt: Long,
    ): Boolean {
        if (archived && !product.isArchived && getActiveProductCount() <= 1) return false
        upsertProduct(
            product.copy(
                isArchived = archived,
                isDefault = if (archived) false else product.isDefault,
                updatedAtEpochMillis = updatedAt,
            ),
        )
        if (archived && product.isDefault) {
            getDefaultReplacement(product.id)?.let {
                replaceDefaultProduct(it.copy(updatedAtEpochMillis = updatedAt))
            }
        }
        return true
    }

    @Transaction
    suspend fun replaceDefaultProduct(product: CigaretteProductEntity) {
        clearDefaultProduct(product.updatedAtEpochMillis)
        upsertProduct(product.copy(isDefault = true))
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecord(record: SmokingRecordEntity)

    @Update
    suspend fun updateRecord(record: SmokingRecordEntity)

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
