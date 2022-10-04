package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.Message
import com.google.gson.annotations.SerializedName

data class FirebaseResponse(
    val message: Message,
    @SerializedName("fromUserName")
    val userName: String,
    // TODO name of room
    // @SerializedName("groupName")
    val groupName: String
)