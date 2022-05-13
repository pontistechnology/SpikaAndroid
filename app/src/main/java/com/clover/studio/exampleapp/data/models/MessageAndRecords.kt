package com.clover.studio.exampleapp.data.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class MessageAndRecords(
    @Embedded val message: Message,
    @Relation(
        parentColumn = "id",
        entityColumn = "message_id",
        associateBy = Junction(MessageAndRecordsCrossRef::class)
    )
    val records: List<MessageRecords>?
)
