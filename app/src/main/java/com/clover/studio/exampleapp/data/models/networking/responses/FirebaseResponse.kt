package com.clover.studio.exampleapp.data.models.networking.responses

import com.clover.studio.exampleapp.data.models.entity.Message

data class FirebaseResponse(
    val groupName: String,
    val unreadCount: Int,
    val muted: Boolean,
    val message: Message,
    val fromUserName: String
)