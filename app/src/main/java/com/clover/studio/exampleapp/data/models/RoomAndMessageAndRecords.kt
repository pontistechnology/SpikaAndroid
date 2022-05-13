package com.clover.studio.exampleapp.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class RoomAndMessageAndRecords(
    @Embedded val room: ChatRoom,
    @Relation(entity = Message::class, parentColumn = "room_id", entityColumn = "room_id")
    val message: List<MessageAndRecords>?,
)