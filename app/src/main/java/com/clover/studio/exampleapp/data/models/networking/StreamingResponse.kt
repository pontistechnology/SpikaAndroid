package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageRecords

data class StreamingResponse(
    val data: DataResponse?
)

data class DataResponse(
    val type: String?,
    val message: Message?,
    val messageRecord: MessageRecords?
)