package com.clover.studio.exampleapp.data.models.junction

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.clover.studio.exampleapp.data.AppDatabase

@Entity(primaryKeys = ["room_id", "id"], tableName = AppDatabase.TablesInfo.TABLE_ROOM_USER)
data class RoomUser(
    @ColumnInfo(name = "room_id")
    val roomId: Int,
    @ColumnInfo(name = "id")
    val userId: Int,
    val isAdmin: Boolean?
)