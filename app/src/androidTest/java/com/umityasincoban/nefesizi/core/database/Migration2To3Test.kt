package com.umityasincoban.nefesizi.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {
    private val databaseName = "migration-2-to-3-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NefesIziDatabase::class.java,
    )

    @Test
    fun migrationPreservesRecordSnapshotsAndSeedsProductRevision() {
        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                """
                INSERT INTO cigarette_products (
                    id, name, brand, variant,
                    nicotineMicrogramsPerCigarette,
                    tarMicrogramsPerCigarette,
                    carbonMonoxideMicrogramsPerCigarette,
                    priceMicrosPerCigarette,
                    currencyCode, valueSource,
                    isDefault, isArchived,
                    createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "product-1",
                    "Test Ürünü",
                    null,
                    null,
                    700L,
                    8_000L,
                    9_000L,
                    6_000_000L,
                    "TRY",
                    "USER_ENTERED",
                    1,
                    0,
                    1_000L,
                    2_000L,
                ),
            )
            execSQL(
                """
                INSERT INTO smoking_records (
                    id, smokedAtEpochMillis, zoneIdSnapshot,
                    quantity, consumedQuarter, productId,
                    productNameSnapshot,
                    nicotineMicrogramsPerCigaretteSnapshot,
                    tarMicrogramsPerCigaretteSnapshot,
                    carbonMonoxideMicrogramsPerCigaretteSnapshot,
                    priceMicrosPerCigaretteSnapshot,
                    currencyCodeSnapshot,
                    cravingLevel, trigger, mood, locationType, note,
                    createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "record-1",
                    1_500L,
                    "Europe/Istanbul",
                    1,
                    4,
                    "product-1",
                    "Eski Ürün Adı",
                    600L,
                    7_000L,
                    8_000L,
                    5_000_000L,
                    "TRY",
                    null,
                    null,
                    null,
                    null,
                    null,
                    1_500L,
                    1_500L,
                ),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            3,
            true,
            DatabaseMigrations.MIGRATION_2_3,
        )

        migrated.query(
            """
            SELECT productNameSnapshot,
                   nicotineMicrogramsPerCigaretteSnapshot,
                   priceMicrosPerCigaretteSnapshot,
                   productRevisionIdSnapshot,
                   valueSourceSnapshot
            FROM smoking_records
            WHERE id = 'record-1'
            """.trimIndent(),
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("Eski Ürün Adı", cursor.getString(0))
            assertEquals(600L, cursor.getLong(1))
            assertEquals(5_000_000L, cursor.getLong(2))
            assertEquals("legacy-product-1", cursor.getString(3))
            assertEquals("USER_ENTERED", cursor.getString(4))
        }

        migrated.query(
            """
            SELECT productId,
                   effectiveFromEpochMillis,
                   priceMicrosPerCigarette,
                   packPriceMicros,
                   cigarettesPerPack
            FROM cigarette_product_revisions
            WHERE id = 'legacy-product-1'
            """.trimIndent(),
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("product-1", cursor.getString(0))
            assertEquals(1_000L, cursor.getLong(1))
            assertEquals(6_000_000L, cursor.getLong(2))
            assertNull(cursor.getString(3))
            assertNull(cursor.getString(4))
        }
    }
}
