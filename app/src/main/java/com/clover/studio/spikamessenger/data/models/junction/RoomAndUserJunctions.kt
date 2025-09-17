package com.clover.studio.spikamessenger.data.models.junction

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserWithRooms(
    @Embedded val user: User,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "room_id",
        associateBy = Junction(RoomUser::class)
    )
    val chatRooms: List<ChatRoom>
) : Parcelable

@Parcelize
data class RoomWithUsers(
    @Embedded val room: ChatRoom,
    @Relation(
        parentColumn = "room_id",
        entityColumn = "user_id",
        associateBy = Junction(RoomUser::class)
    )
    var users: List<User>
) : Parcelable

@Parcelize
data class RoomWhitUserAndPhoneUser(
    @Embedded val room: ChatRoom,
    @Relation(
        parentColumn = "room_id",
        entityColumn = "user_id",
        associateBy = Junction(RoomUser::class)
    )
    val users: List<UserAndPhoneUser>
) : Parcelable
