package com.umityasincoban.nefesizi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CigaretteProductEntity::class,
        CigaretteProductRevisionEntity::class,
        SmokingRecordEntity::class,
        DailyHealthEntryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class NefesIziDatabase : RoomDatabase() {
    abstract fun dao(): NefesIziDao
}
