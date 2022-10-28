package com.clover.studio.exampleapp.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class MessageAndRecords(
    @Embedded val message: Message,
    @Relation(
        parentColumn = "id",
        entityColumn = "message_id",
    )
    val records: List<MessageRecords>?
)
