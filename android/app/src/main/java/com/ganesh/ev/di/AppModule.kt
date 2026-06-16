package com.ganesh.ev.di

import android.content.Context
import com.ganesh.ev.data.local.AppDatabase
import com.ganesh.ev.data.local.StationDao
import com.ganesh.ev.data.network.ApiService
import com.ganesh.ev.data.network.RetrofitClient
import com.ganesh.ev.data.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level DI graph (CV-9). Exposes the existing singletons so new code can
 * inject them rather than reaching for global objects. The configured Retrofit
 * client is reused as-is (keeps its auth interceptor/authenticator).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService = RetrofitClient.apiService

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
            @ApplicationContext context: Context
    ): UserPreferencesRepository = UserPreferencesRepository(context)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            AppDatabase.getInstance(context)

    @Provides
    fun provideStationDao(db: AppDatabase): StationDao = db.stationDao()
}
