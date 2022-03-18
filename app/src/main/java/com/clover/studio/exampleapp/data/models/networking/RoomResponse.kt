package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.User

data class RoomResponse(
    val status: String?,
    val data: RoomData?
)

data class RoomData(
    val room: Room?,
)

data class Room(
    val id: Int?,
    val name: String?,
    val users: List<RoomUsers>?,
    val avatarUrl: String?,
    val type: String?,
    val createdAt: Long?
)

data class RoomUsers(
    val userId: Int?,
    val isAdmin: Boolean?,
    val user: User?
)