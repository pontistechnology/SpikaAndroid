package com.clover.studio.spikamessenger.di

import android.content.Context
import com.clover.studio.spikamessenger.data.repositories.MainRepository
import com.clover.studio.spikamessenger.data.repositories.SSERepository
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.data_sources.ChatRemoteDataSource
import com.clover.studio.spikamessenger.data.repositories.data_sources.MainRemoteDataSource
import com.clover.studio.spikamessenger.data.services.ChatService
import com.clover.studio.spikamessenger.data.services.RetrofitService
import com.clover.studio.spikamessenger.utils.SSEManager
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HelperModule {

    @Singleton
    @Provides
    fun provideSharedPreferencesRepository(
        @ApplicationContext context: Context
    ) = SharedPreferencesRepositoryImpl(context)

    @Singleton
    @Provides
    fun provideSSEManager(
        sseRepo: SSERepository,
        sharedPrefs: SharedPreferencesRepository
    ) =
        SSEManager(sseRepo, sharedPrefs)

    @Singleton
    @Provides
    fun provideUploadDownloadManager(
        repository: MainRepository
    ) = UploadDownloadManager(repository)

    @Singleton
    @Provides
    fun provideMainDataSource(
        retrofitService: RetrofitService,
        sharedPrefs: SharedPreferencesRepository
    ) =
        MainRemoteDataSource(retrofitService, sharedPrefs)

    @Singleton
    @Provides
    fun provideChatDataSource(
        retrofitService: ChatService,
        sharedPrefs: SharedPreferencesRepository
    ) =
        ChatRemoteDataSource(retrofitService, sharedPrefs)
}
