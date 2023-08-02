package com.clover.studio.spikamessenger.data.models.networking

import com.clover.studio.spikamessenger.data.models.entity.ChatRoom

data class ChatRoomUpdate(
    val oldRoom: ChatRoom?,
    val newRoom: ChatRoom
)