package com.clover.studio.spikamessenger.utils

import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.clover.studio.spikamessenger.MainApplication

object VideoCompression {
    fun compressVideoFile(uris: List<Uri>, listener: CompressionListener) {
        VideoCompressor.start(
            context = MainApplication.appContext, // => This is required
            uris = uris, // => Source can be provided as content uris
            isStreamable = false,
            // THIS STORAGE
            sharedStorageConfiguration = SharedStorageConfiguration(
                saveAt = null
            ),
//            // OR AND NOT BOTH
//            appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
//
//            ),
            configureWith = Configuration(
                videoNames = listOf("videoname"),
                quality = VideoQuality.MEDIUM,
                isMinBitrateCheckEnabled = true,
                videoBitrateInMbps = 5, /*Int, ignore, or null*/
                disableAudio = false, /*Boolean, or ignore*/
                keepOriginalResolution = false, /*Boolean, or ignore*/
                videoWidth = 360.0, /*Double, ignore, or null*/
                videoHeight = 480.0, /*Double, ignore, or null*/
            ),
            listener = listener
        )
    }
}
