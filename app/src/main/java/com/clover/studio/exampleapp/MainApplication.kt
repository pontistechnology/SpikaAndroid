package com.clover.studio.exampleapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.CHANNEL_ID
import com.clover.studio.exampleapp.utils.SSEManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), LifecycleEventObserver {
    @Inject
    lateinit var sseManager: SSEManager

    @Inject
    lateinit var sharedPrefs: SharedPreferencesRepository

    companion object {
        lateinit var appContext: Context
        var isInForeground = false
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "dev" || BuildConfig.BUILD_TYPE == "releaseDebug") {
            Timber.plant(Timber.DebugTree())
        }
        // TODO release tree implementation

        appContext = this
        createNotificationChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

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

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                CoroutineScope(Dispatchers.IO).launch {
                    if (sharedPrefs.readToken()?.isNotEmpty() == true) {
                        sseManager.startSSEStream()
                    }
                }
                isInForeground = true
            }
            Lifecycle.Event.ON_CREATE -> {
                // ignore
            }
            Lifecycle.Event.ON_RESUME -> {
                // ignore
            }
            Lifecycle.Event.ON_PAUSE -> {
                // ignore
            }
            Lifecycle.Event.ON_STOP -> {
                isInForeground = false
            }
            Lifecycle.Event.ON_DESTROY -> {
                isInForeground = false
            }
            Lifecycle.Event.ON_ANY -> {
                // ignore
            }
        }
    }
}