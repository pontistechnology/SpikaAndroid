package com.clover.studio.spikamessenger.data.models.junction

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.clover.studio.spikamessenger.data.AppDatabase

@Entity(primaryKeys = ["room_id", "user_id"], tableName = AppDatabase.TablesInfo.TABLE_ROOM_USER)
data class RoomUser(
    @ColumnInfo(name = "room_id")
    val roomId: Int,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    val isAdmin: Boolean?
)