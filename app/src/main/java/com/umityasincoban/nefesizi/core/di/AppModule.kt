package com.umityasincoban.nefesizi.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.umityasincoban.nefesizi.core.database.NefesIziDao
import com.umityasincoban.nefesizi.core.database.NefesIziDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NefesIziDatabase =
        Room.databaseBuilder(context, NefesIziDatabase::class.java, "nefes_izi.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideDao(database: NefesIziDatabase): NefesIziDao = database.dao()

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
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
}
