package com.clover.studio.spikamessenger.di

import android.content.Context
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.services.ChatService
import com.clover.studio.spikamessenger.data.services.OnboardingService
import com.clover.studio.spikamessenger.data.services.RetrofitService
import com.clover.studio.spikamessenger.data.services.SSEService
import com.clover.studio.spikamessenger.utils.helpers.GsonProvider
import com.google.gson.Gson
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
        return GsonProvider.gson
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
    fun provideOnboardingService(retrofit: Retrofit.Builder): OnboardingService =
        retrofit.build().create(OnboardingService::class.java)

    @Singleton
    @Provides
    fun provideChatService(retrofit: Retrofit.Builder): ChatService =
        retrofit.build().create(ChatService::class.java)

    @Singleton
    @Provides
    fun provideSSEService(retrofit: Retrofit.Builder): SSEService =
        retrofit.build().create(SSEService::class.java)

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context) =
        AppDatabase.getDatabase(appContext)
}