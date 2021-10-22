package com.clover.studio.exampleapp.di

import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.repositories.ChatRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.MessageRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.ReactionRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.UserRepositoryImpl
import com.clover.studio.exampleapp.data.services.RetrofitService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideChatRepository(
        retrofitService: RetrofitService,
        chatDao: ChatDao,
        chatUserDao: ChatUserDao
    ) =
        ChatRepositoryImpl(retrofitService, chatDao, chatUserDao)

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
    fun provideUserRepository(retrofitService: RetrofitService, userDao: UserDao) =
        UserRepositoryImpl(retrofitService, userDao)
}