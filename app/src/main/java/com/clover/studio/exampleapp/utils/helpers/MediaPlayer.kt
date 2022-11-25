package com.clover.studio.exampleapp.utils.helpers

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

object MediaPlayer {
    private var instance: ExoPlayer? = null

    fun getInstance(context: Context): ExoPlayer {
        return if (instance != null) instance as ExoPlayer
        else {
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(buildUponParameters().setMaxVideoSizeSd())
            }

            instance = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()

            instance as ExoPlayer
        }
    }

    fun resetPlayer() {
        instance = null
    }
}