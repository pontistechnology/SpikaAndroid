package com.clover.studio.exampleapp.di

import com.clover.studio.exampleapp.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Singleton
    @Provides
    fun provideChatDao(database: AppDatabase) = database.chatDao()

    @Singleton
    @Provides
    fun provideMessageDao(database: AppDatabase) = database.messageDao()

    @Singleton
    @Provides
    fun provideReactionDao(database: AppDatabase) = database.reactionDao()

    @Singleton
    @Provides
    fun provideUserDao(database: AppDatabase) = database.userDao()

    @Singleton
    @Provides
    fun provideChatUserDao(database: AppDatabase) = database.chatUserDao()

    @Singleton
    @Provides
    fun providePhoneUserDao(database: AppDatabase) = database.phoneUserDao()
}