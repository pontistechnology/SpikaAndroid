package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.entity.ChatRoom

data class ChatRoomUpdate(
    val oldRoom: ChatRoom?,
    val newRoom: ChatRoom
)