package com.clover.studio.exampleapp.data.models.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageAndRecords(
    @Embedded val message: Message,
    @Relation(
        parentColumn = "id",
        entityColumn = "message_id",
    )
    val records: List<MessageRecords>?
) : Parcelable
