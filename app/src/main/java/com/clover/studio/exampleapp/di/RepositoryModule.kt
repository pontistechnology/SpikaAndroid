package com.clover.studio.exampleapp.di

import android.content.Context
import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.repositories.*
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.data.services.OnboardingService
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.SSEManager
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
        messageDao: MessageDao,
        sharedPrefs: SharedPreferencesRepository
    ) =
        ChatRepositoryImpl(chatService, messageDao, sharedPrefs)

    @Singleton
    @Provides
    fun provideMessageRepository(retrofitService: RetrofitService, messageDao: MessageDao) =
        MessageRepositoryImpl(retrofitService, messageDao)

    @Singleton
    @Provides
    fun provideReactionRepository(retrofitService: RetrofitService, reactionDao: ReactionDao) =
        ReactionRepositoryImpl(retrofitService, reactionDao)

    @Singleton
    @Provides
    fun provideUserRepository(
        retrofitService: RetrofitService,
        userDao: UserDao,
        messageDao: MessageDao,
        chatRoomDao: ChatRoomDao,
        sharedPrefs: SharedPreferencesRepository
    ) =
        MainRepositoryImpl(retrofitService, userDao, messageDao, chatRoomDao, sharedPrefs)

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
    fun provideSSEManager(
        sharedPrefs: SharedPreferencesRepository,
        messageDao: MessageDao
    ) =
        SSEManager(sharedPrefs, messageDao)
}