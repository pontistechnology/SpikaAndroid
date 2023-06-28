package com.clover.studio.spikamessenger.di

import android.content.Context
import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.daos.*
import com.clover.studio.spikamessenger.data.repositories.*
import com.clover.studio.spikamessenger.data.repositories.data_sources.ChatRemoteDataSource
import com.clover.studio.spikamessenger.data.repositories.data_sources.MainRemoteDataSource
import com.clover.studio.spikamessenger.data.repositories.data_sources.OnboardingRemoteDataSource
import com.clover.studio.spikamessenger.data.repositories.data_sources.SSERemoteDataSource
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
object RepositoryModule {

    @Singleton
    @Provides
    fun provideSharedPreferencesRepository(
        @ApplicationContext context: Context
    ): SharedPreferencesRepository = SharedPreferencesRepositoryImpl(context)

    @Singleton
    @Provides
    fun provideChatRepository(
        chatRemoteDataSource: ChatRemoteDataSource,
        roomDao: ChatRoomDao,
        messageDao: MessageDao,
        userDao: UserDao,
        roomUserDao: RoomUserDao,
        notesDao: NotesDao,
        appDatabase: AppDatabase,
    ) =
        ChatRepositoryImpl(
            chatRemoteDataSource,
            roomDao,
            messageDao,
            userDao,
            roomUserDao,
            notesDao,
            appDatabase,
        )

    @Singleton
    @Provides
    fun provideMainRepository(
        mainRemoteDataSource: MainRemoteDataSource,
        userDao: UserDao,
        chatRoomDao: ChatRoomDao,
        roomUserDao: RoomUserDao,
        appDatabase: AppDatabase,
        sharedPrefs: SharedPreferencesRepository
    ) =
        MainRepositoryImpl(
            mainRemoteDataSource,
            userDao,
            chatRoomDao,
            roomUserDao,
            appDatabase,
            sharedPrefs
        )


    @Singleton
    @Provides
    fun provideOnboardingRepository(
        onboardingRemoteDataSource: OnboardingRemoteDataSource,
        userDao: UserDao,
        phoneUserDao: PhoneUserDao,
        sharedPrefs: SharedPreferencesRepository
    ) =
        OnboardingRepositoryImpl(onboardingRemoteDataSource, userDao, phoneUserDao, sharedPrefs)

    @Singleton
    @Provides
    fun provideSSERepository(
        sseRemoteDataSource: SSERemoteDataSource,
        sharedPrefs: SharedPreferencesRepository,
        messageDao: MessageDao,
        messageRecordsDao: MessageRecordsDao,
        chatRoomDao: ChatRoomDao,
        roomUserDao: RoomUserDao,
        appDatabase: AppDatabase,
        userDao: UserDao
    ) = SSERepositoryImpl(
        sseRemoteDataSource,
        sharedPrefs,
        messageDao,
        messageRecordsDao,
        chatRoomDao,
        roomUserDao,
        appDatabase,
        userDao
    )

    @Singleton
    @Provides
    fun provideSSEManager(
        sseRepo: SSERepositoryImpl,
        sharedPrefs: SharedPreferencesRepository
    ) =
        SSEManager(sseRepo, sharedPrefs)

    @Singleton
    @Provides
    fun provideUploadDownloadManager(
        repository: MainRepositoryImpl
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