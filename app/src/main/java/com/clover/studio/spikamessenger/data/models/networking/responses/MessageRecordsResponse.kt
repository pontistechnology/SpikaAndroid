package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.MessageRecords

data class MessageRecordsResponse(
    val status: String,
    val data: Records
)

data class Records(
    val list: List<MessageRecords>,
    val limit: Int?,
    val count: Int?,
    val hasNext: Boolean?
)