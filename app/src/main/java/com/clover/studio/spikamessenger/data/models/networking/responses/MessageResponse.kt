package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.Message

data class MessageResponse(
    val status: String?,
    val data: MessageData?
)

data class MessageData(
    val messages: List<Message>?,
    val count: Int?,
    val limit: Int?,
    val message: Message?,
    val list: List<Message>?,
    val hasNext: Boolean?
)