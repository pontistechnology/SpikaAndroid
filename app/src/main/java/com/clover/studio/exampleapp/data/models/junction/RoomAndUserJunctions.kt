package com.clover.studio.exampleapp.data.models.junction

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.User

data class UserWithRooms(
    @Embedded val user: User,
    @Relation(
        parentColumn = "id",
        entityColumn = "room_id",
        associateBy = Junction(RoomUser::class)
    )
    val chatRooms: List<ChatRoom>
)

data class RoomWithUsers(
    @Embedded val room: ChatRoom,
    @Relation(
        parentColumn = "room_id",
        entityColumn = "id",
        associateBy = Junction(RoomUser::class)
    )
    val users: List<User>
)