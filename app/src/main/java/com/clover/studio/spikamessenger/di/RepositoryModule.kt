package com.clover.studio.spikamessenger.di

import com.clover.studio.spikamessenger.data.repositories.ChatRepository
import com.clover.studio.spikamessenger.data.repositories.ChatRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.MainRepository
import com.clover.studio.spikamessenger.data.repositories.MainRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.OnboardingRepository
import com.clover.studio.spikamessenger.data.repositories.OnboardingRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SSERepository
import com.clover.studio.spikamessenger.data.repositories.SSERepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideSharedPreferencesRepository(
        sharedPreferencesRepositoryImpl: SharedPreferencesRepositoryImpl
    ): SharedPreferencesRepository

    @Binds
    @Singleton
    abstract fun provideOnboardingRepository(
        onboardingRepositoryImpl: OnboardingRepositoryImpl
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun provideSSERepository(
        sseRepositoryImpl: SSERepositoryImpl
    ): SSERepository

    @Binds
    @Singleton
    abstract fun provideChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun provideMainRepository(
        mainRepositoryImpl: MainRepositoryImpl
    ): MainRepository
}
