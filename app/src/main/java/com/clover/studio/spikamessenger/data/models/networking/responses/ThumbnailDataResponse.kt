package com.clover.studio.spikamessenger.data.models.networking.responses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ThumbnailDataResponse(
    val status: String,
    val data: ThumbnailData?
)

@Parcelize
data class ThumbnailData(
    val title: String?,
    val image: String?,
    val icon: String?,
    val description: String?,
    val url: String
): Parcelable
