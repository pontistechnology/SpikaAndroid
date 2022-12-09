package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.entity.MessageRecords

data class MessageRecordsResponse(
    val status: String,
    val data: Records
)

data class Records(
    val messageRecords: List<MessageRecords>
)