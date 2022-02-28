package com.clover.studio.exampleapp.di

import android.content.Context
import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.repositories.*
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.data.services.OnboardingService
import com.clover.studio.exampleapp.data.services.RetrofitService
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
        chatDao: ChatDao,
        chatUserDao: ChatUserDao,
        sharedPrefs: SharedPreferencesRepository
    ) =
        ChatRepositoryImpl(chatService, chatDao, chatUserDao, sharedPrefs)

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
        sharedPrefs: SharedPreferencesRepository
    ) =
        UserRepositoryImpl(retrofitService, userDao, sharedPrefs)

    @Singleton
    @Provides
    fun provideOnboardingRepository(
        retrofitService: OnboardingService,
        userDao: UserDao,
        sharedPrefs: SharedPreferencesRepository
    ) =
        OnboardingRepositoryImpl(retrofitService, userDao, sharedPrefs)
}