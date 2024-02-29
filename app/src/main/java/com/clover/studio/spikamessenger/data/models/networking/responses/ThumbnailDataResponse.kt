package com.clover.studio.spikamessenger.data.models.networking.responses

data class ThumbnailDataResponse(
    val status: String,
    val data: ThumbnailData?
)

data class ThumbnailData(
    val title: String?,
    val image: String?,
    val icon: String?,
    val description: String?,
    val url: String
)
