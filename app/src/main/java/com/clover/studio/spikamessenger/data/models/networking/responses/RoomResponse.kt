package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.User

data class RoomResponse(
    val status: String?,
    val data: RoomData?
)

data class RoomData(
    val list: List<ChatRoom>?,
    val rooms: List<ChatRoom>?,
    val room: ChatRoom?,
    val count: Long?,
    val limit: Long?,
    val hasNext: Boolean?
)

data class RoomUsers(
    val userId: Int,
    val isAdmin: Boolean?,
    val user: User?
)

data class UpdatedRoom (
    val roomId: Int,
    val groupName : String,
    val avatarId: Long,
    val userNumber: Int,
)
