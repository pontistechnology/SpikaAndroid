package com.clover.studio.exampleapp.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class ChatRoomAndMessage(
    @Embedded val chatRoom: ChatRoom,
    @Relation(parentColumn = "room_id", entityColumn = "room_id")
    val message: List<Message>?
)