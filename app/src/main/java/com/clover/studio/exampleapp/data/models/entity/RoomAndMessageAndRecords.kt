package com.clover.studio.exampleapp.data.models.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers

data class RoomAndMessageAndRecords(
    @Embedded val roomWithUsers: RoomWithUsers,
    @Relation(entity = Message::class, parentColumn = "room_id", entityColumn = "room_id")
    val message: List<MessageAndRecords>?,
)