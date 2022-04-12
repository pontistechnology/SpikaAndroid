package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.Message

data class StreamingResponse(
    val data: DataResponse?
)

data class DataResponse(
    val type: String?,
    val message: Message?
)