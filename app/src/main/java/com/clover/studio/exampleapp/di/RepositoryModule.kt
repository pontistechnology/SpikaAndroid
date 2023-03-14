package com.clover.studio.exampleapp.di

import android.content.Context
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.repositories.*
import com.clover.studio.exampleapp.data.repositories.data_sources.MainRemoteDataSource
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.data.services.OnboardingService
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.data.services.SSEService
import com.clover.studio.exampleapp.utils.SSEManager
import com.clover.studio.exampleapp.utils.UploadDownloadManager
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
        chatService: ChatService,
        roomDao: ChatRoomDao,
        messageDao: MessageDao,
        userDao: UserDao,
        roomUserDao: RoomUserDao,
        notesDao: NotesDao,
        appDatabase: AppDatabase,
        sharedPrefs: SharedPreferencesRepository
    ) =
        ChatRepositoryImpl(
            chatService,
            roomDao,
            messageDao,
            userDao,
            roomUserDao,
            notesDao,
            appDatabase,
            sharedPrefs
        )

    @Singleton
    @Provides
    fun provideMainRepository(
        mainRemoteDataSource: MainRemoteDataSource,
        retrofitService: RetrofitService,
        userDao: UserDao,
        chatRoomDao: ChatRoomDao,
        roomUserDao: RoomUserDao,
        appDatabase: AppDatabase,
        sharedPrefs: SharedPreferencesRepository
    ) =
        MainRepositoryImpl(
            mainRemoteDataSource,
            retrofitService,
            userDao,
            chatRoomDao,
            roomUserDao,
            appDatabase,
            sharedPrefs
        )

    @Singleton
    @Provides
    fun provideOnboardingRepository(
        retrofitService: OnboardingService,
        userDao: UserDao,
        phoneUserDao: PhoneUserDao,
        sharedPrefs: SharedPreferencesRepository
    ) =
        OnboardingRepositoryImpl(retrofitService, userDao, phoneUserDao, sharedPrefs)

    @Singleton
    @Provides
    fun provideSSERepository(
        sharedPrefs: SharedPreferencesRepository,
        sseService: SSEService,
        messageDao: MessageDao,
        messageRecordsDao: MessageRecordsDao,
        chatRoomDao: ChatRoomDao,
        roomUserDao: RoomUserDao,
        appDatabase: AppDatabase,
        userDao: UserDao
    ) = SSERepositoryImpl(
        sharedPrefs,
        sseService,
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
}