package com.umityasincoban.nefesizi.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `daily_health_entries` (
                    `entryDate` TEXT NOT NULL,
                    `zoneId` TEXT NOT NULL,
                    `energyLevel` INTEGER,
                    `stressLevel` INTEGER,
                    `sleepQuality` INTEGER,
                    `morningCough` INTEGER,
                    `headache` INTEGER,
                    `shortnessOfBreath` INTEGER,
                    `chestDiscomfort` INTEGER,
                    `restingHeartRate` INTEGER,
                    `exerciseMinutes` INTEGER,
                    `note` TEXT,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`entryDate`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cigarette_product_revisions` (
                    `id` TEXT NOT NULL,
                    `productId` TEXT NOT NULL,
                    `effectiveFromEpochMillis` INTEGER NOT NULL,
                    `nicotineMicrogramsPerCigarette` INTEGER,
                    `tarMicrogramsPerCigarette` INTEGER,
                    `carbonMonoxideMicrogramsPerCigarette` INTEGER,
                    `packPriceMicros` INTEGER,
                    `cigarettesPerPack` INTEGER,
                    `priceMicrosPerCigarette` INTEGER,
                    `currencyCode` TEXT NOT NULL,
                    `valueSource` TEXT NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`productId`) REFERENCES `cigarette_products`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_cigarette_product_revisions_productId_effectiveFromEpochMillis`
                ON `cigarette_product_revisions` (`productId`, `effectiveFromEpochMillis`)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `cigarette_product_revisions` (
                    `id`,
                    `productId`,
                    `effectiveFromEpochMillis`,
                    `nicotineMicrogramsPerCigarette`,
                    `tarMicrogramsPerCigarette`,
                    `carbonMonoxideMicrogramsPerCigarette`,
                    `packPriceMicros`,
                    `cigarettesPerPack`,
                    `priceMicrosPerCigarette`,
                    `currencyCode`,
                    `valueSource`,
                    `createdAtEpochMillis`
                )
                SELECT
                    'legacy-' || `id`,
                    `id`,
                    `createdAtEpochMillis`,
                    `nicotineMicrogramsPerCigarette`,
                    `tarMicrogramsPerCigarette`,
                    `carbonMonoxideMicrogramsPerCigarette`,
                    NULL,
                    NULL,
                    `priceMicrosPerCigarette`,
                    `currencyCode`,
                    `valueSource`,
                    `createdAtEpochMillis`
                FROM `cigarette_products`
                """.trimIndent(),
            )
            db.execSQL(
                "ALTER TABLE `smoking_records` ADD COLUMN `productRevisionIdSnapshot` TEXT",
            )
            db.execSQL(
                "ALTER TABLE `smoking_records` ADD COLUMN `valueSourceSnapshot` TEXT",
            )
            db.execSQL(
                """
                UPDATE `smoking_records`
                SET
                    `productRevisionIdSnapshot` = 'legacy-' || `productId`,
                    `valueSourceSnapshot` = (
                        SELECT `valueSource`
                        FROM `cigarette_products`
                        WHERE `cigarette_products`.`id` = `smoking_records`.`productId`
                    )
                WHERE `productId` IS NOT NULL
                  AND EXISTS (
                      SELECT 1
                      FROM `cigarette_products`
                      WHERE `cigarette_products`.`id` = `smoking_records`.`productId`
                  )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_smoking_records_trigger` ON `smoking_records` (`trigger`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_smoking_records_mood` ON `smoking_records` (`mood`)",
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `daily_health_entries` ADD COLUMN `systolicBloodPressure` INTEGER",
            )
            db.execSQL(
                "ALTER TABLE `daily_health_entries` ADD COLUMN `diastolicBloodPressure` INTEGER",
            )
            db.execSQL(
                "ALTER TABLE `daily_health_entries` ADD COLUMN `weightGrams` INTEGER",
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
