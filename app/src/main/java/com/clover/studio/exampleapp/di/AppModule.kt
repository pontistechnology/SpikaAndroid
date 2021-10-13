package com.clover.studio.exampleapp.di

import android.content.Context
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.repositories.ChatRepository
import com.clover.studio.exampleapp.data.repositories.MessageRepository
import com.clover.studio.exampleapp.data.repositories.ReactionRepository
import com.clover.studio.exampleapp.data.repositories.UserRepository
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGsonBuilder(): Gson {
        return GsonBuilder()
            .create()
    }

    @Singleton
    @Provides
    fun provideInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Singleton
    @Provides
    fun provideRetrofitClient(interceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson, client: OkHttpClient): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
    }

    @Singleton
    @Provides
    fun provideRetrofitService(retrofit: Retrofit.Builder): RetrofitService =
        retrofit.build().create(RetrofitService::class.java)

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context) =
        AppDatabase.getDatabase(appContext)

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
    fun provideUserService(retrofitService: RetrofitService, userDao: UserDao) =
        UserRepository(retrofitService, userDao)
}