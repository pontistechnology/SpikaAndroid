package com.clover.studio.exampleapp.data.models.networking

import com.clover.studio.exampleapp.data.models.MessageRecords

data class MessageRecordsResponse(
    val status: String,
    val data: Records
)

data class Records(
    val messageRecords: List<MessageRecords>
)