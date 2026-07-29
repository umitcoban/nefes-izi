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
class Migration3To4Test {
    private val databaseName = "migration-3-to-4-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NefesIziDatabase::class.java,
    )

    @Test
    fun migrationPreservesHealthEntryAndAddsNullableMeasurements() {
        helper.createDatabase(databaseName, 3).apply {
            execSQL(
                """
                INSERT INTO daily_health_entries (
                    entryDate, zoneId, energyLevel, stressLevel, sleepQuality,
                    morningCough, headache, shortnessOfBreath, chestDiscomfort,
                    restingHeartRate, exerciseMinutes, note,
                    createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "2026-07-29",
                    "Europe/Istanbul",
                    4,
                    2,
                    3,
                    0,
                    null,
                    0,
                    null,
                    70,
                    30,
                    "Korunan not",
                    1_000L,
                    2_000L,
                ),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            4,
            true,
            DatabaseMigrations.MIGRATION_3_4,
        )

        migrated.query(
            """
            SELECT energyLevel, note, systolicBloodPressure,
                   diastolicBloodPressure, weightGrams
            FROM daily_health_entries
            WHERE entryDate = '2026-07-29'
            """.trimIndent(),
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(4, cursor.getInt(0))
            assertEquals("Korunan not", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertNull(cursor.getString(3))
            assertNull(cursor.getString(4))
        }
    }
}
