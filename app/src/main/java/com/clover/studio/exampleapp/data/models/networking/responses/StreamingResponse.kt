package com.clover.studio.exampleapp.data.models.networking.responses

import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.entity.User

data class StreamingResponse(
    val data: DataResponse?
)

data class DataResponse(
    val type: String?,
    val message: Message?,
    val messageRecord: MessageRecords?,
    val user: User?,
    val room: ChatRoom?
)