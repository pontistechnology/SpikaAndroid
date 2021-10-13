package com.clover.studio.exampleapp.di

import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.repositories.ChatRepository
import com.clover.studio.exampleapp.data.repositories.MessageRepository
import com.clover.studio.exampleapp.data.repositories.ReactionRepository
import com.clover.studio.exampleapp.data.repositories.UserRepository
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
        ChatRepository(retrofitService, chatDao, chatUserDao)

    @Singleton
    @Provides
    fun provideMessageRepository(retrofitService: RetrofitService, messageDao: MessageDao) =
        MessageRepository(retrofitService, messageDao)

    @Singleton
    @Provides
    fun provideReactionRepository(retrofitService: RetrofitService, reactionDao: ReactionDao) =
        ReactionRepository(retrofitService, reactionDao)

    @Singleton
    @Provides
    fun provideUserRepository(retrofitService: RetrofitService, userDao: UserDao) =
        UserRepository(retrofitService, userDao)
}