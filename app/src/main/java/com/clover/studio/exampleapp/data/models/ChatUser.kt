package com.clover.studio.exampleapp.data.models

import androidx.room.Embedded
import androidx.room.Relation
import com.clover.studio.exampleapp.data.AppDatabase

class ChatUser {
    @Embedded
    var chat: Chat? = null

    @Relation(
        parentColumn = AppDatabase.TablesInfo.ID,
        entityColumn = AppDatabase.TablesInfo.ID,
        entity = User::class
    )
    var users: List<User>? = null
}
