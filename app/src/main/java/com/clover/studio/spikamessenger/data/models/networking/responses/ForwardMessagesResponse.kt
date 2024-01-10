package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.Message

data class ForwardMessagesResponse(
    val status: String,
    val data: ForwardMessages,
)

data class ForwardMessages(
    val messages: List<Message>,
    val newRooms: List<ChatRoom>
)
