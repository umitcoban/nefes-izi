package com.umityasincoban.nefesizi.core.di

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(context, NefesIziDatabase::class.java, "nefes_izi.db").build()

    @Provides
    fun provideDao(database: NefesIziDatabase): NefesIziDao = database.dao()

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
