package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.entity.User

data class StreamingResponse(
    val data: DataResponse?
)

data class DataResponse(
    val type: String?,
    val message: Message?,
    val messageRecord: MessageRecords?,
    val user: User?,
    val room: ChatRoom?,
    val roomId: Int?,
    val deliveredCount: Int?,
    val seenCount: Int?,
    val totalUserCount: Int?
)
