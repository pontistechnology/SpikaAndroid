package com.clover.studio.exampleapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "dev" || BuildConfig.BUILD_TYPE == "releaseDebug") {
            Timber.plant(Timber.DebugTree())
        }

        // TODO release tree implementation
    }
}