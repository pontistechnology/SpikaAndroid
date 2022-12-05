package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.entity.Message

data class MessageResponse(
    val status: String?,
    val data: MessageData?
)

data class MessageData(
    val messages: List<Message>?,
    val count: Int?,
    val limit: Int?,
    val message: Message?
)