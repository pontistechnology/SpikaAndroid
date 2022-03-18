package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.Message

data class MessageResponse(
    val status: String?,
    val data: MessageData?
)

data class MessageData(
    val list: List<Message>?,
    val count: Int?,
    val limit: Int?
)