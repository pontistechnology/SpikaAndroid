package com.clover.studio.spikamessenger.utils

import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.clover.studio.spikamessenger.MainApplication

object VideoCompression {
    fun compressVideoFile(
        uris: List<Uri>,
        muteAudio: Boolean,
        videoQuality: VideoQuality,
//        metadata: FileMetadata,
        listener: CompressionListener
    ) {
        VideoCompressor.start(
            context = MainApplication.appContext,
            uris = uris,
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
                quality = videoQuality,
                isMinBitrateCheckEnabled = true,
                videoBitrateInMbps = 5, /*Int, ignore, or null*/
                disableAudio = muteAudio,
                keepOriginalResolution = false, /*Boolean, or ignore*/
                videoWidth = null,
                videoHeight = null,
            ),
            listener = listener
        )
    }
}
