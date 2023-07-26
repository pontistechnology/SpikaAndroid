package com.clover.studio.spikamessenger.data.models.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageWithUser(
    @Embedded
    val message: Message,
    @Relation(
        entity = User::class,
        parentColumn = "from_user_id",
        entityColumn = "id"
    ) val user: User
) : Parcelable