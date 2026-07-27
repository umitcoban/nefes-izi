package com.umityasincoban.nefesizi.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CigaretteProductEntity::class,
        SmokingRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NefesIziDatabase : RoomDatabase() {
    abstract fun dao(): NefesIziDao
}
