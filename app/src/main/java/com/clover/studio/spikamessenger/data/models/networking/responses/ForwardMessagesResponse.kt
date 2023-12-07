package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.User

data class ForwardMessagesResponse(
    val status: String,
    val data: ForwardMessages,
)

data class ForwardMessages(
    val messages: List<Message>,
    val newRooms: List<NewRooms>
)

data class NewRooms(
    val id: Long,
    val name: String,
    val users: List<ForwardUser>,
    val avatarFileId: Int,
    val muted: Boolean,
    val pinned: Boolean,
    val deleted: Boolean,
    val type: String,
    val unreadCount: Int,
    val createdAt: Int,
    val modifiedAt: Int
)

data class ForwardUser(
    val userId: Int,
    val roomId: Int,
    val isAdmin: Boolean,
    val user: User,
)
