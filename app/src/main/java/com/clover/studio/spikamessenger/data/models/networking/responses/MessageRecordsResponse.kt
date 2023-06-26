package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.MessageRecords

data class MessageRecordsResponse(
    val status: String,
    val data: Records
)

data class Records(
    val messageRecords: List<MessageRecords>
)