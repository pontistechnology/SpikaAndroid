package com.clover.studio.exampleapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import com.clover.studio.exampleapp.utils.CHANNEL_ID
import com.clover.studio.exampleapp.utils.helpers.AppLifecycleManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MainApplication : Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "dev" || BuildConfig.BUILD_TYPE == "releaseDebug") {
            Timber.plant(Timber.DebugTree())
        }

        appContext = this
        createNotificationChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleManager)
        // TODO release tree implementation

        // Emoji:
        EmojiManager.install(GoogleEmojiProvider())
    }

    fun getContext(): Context {
        return appContext
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_description)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}